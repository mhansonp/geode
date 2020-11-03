/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.cache;


import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.geode.test.dunit.VM.getHostName;
import static org.apache.geode.test.dunit.VM.getVM;
import static org.apache.geode.test.dunit.internal.JUnit4DistributedTestCase.getBlackboard;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.distributed.internal.ClusterDistributionManager;
import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.distributed.internal.DistributionMessageObserver;
import org.apache.geode.logging.internal.log4j.api.LogService;
import org.apache.geode.test.awaitility.GeodeAwaitility;
import org.apache.geode.test.dunit.AsyncInvocation;
import org.apache.geode.test.dunit.VM;
import org.apache.geode.test.dunit.rules.CacheRule;
import org.apache.geode.test.dunit.rules.ClientCacheRule;
import org.apache.geode.test.dunit.rules.DistributedRestoreSystemProperties;
import org.apache.geode.test.dunit.rules.DistributedRule;
import org.apache.geode.util.internal.GeodeGlossary;

public class ThreadDumpWithWaitingMemberDUnitTest implements Serializable {

  @Rule
  public DistributedRule distributedRule = new DistributedRule();

  @Rule
  public CacheRule cacheRule = new CacheRule();

  @Rule
  public ClientCacheRule clientCacheRule = new ClientCacheRule();

  @Rule
  public DistributedRestoreSystemProperties restoreSystemProperties =
      new DistributedRestoreSystemProperties();

  private final String regionName = "testRegion";

  private final Integer ACK_WAIT_THRESHOLD = 2;

  @Before
  public void setUp() {
  }

  @Test
  public void testThreadDumpHasWaitingMemberWithSharedReader() throws Exception {
    VM vm0 = getVM(0);
    VM vm1 = getVM(1);

    getBlackboard().initBlackboard();

    // Create region in one member
    vm0.invoke(() -> {

      System.setProperty(GeodeGlossary.GEMFIRE_PREFIX +
          DistributionConfig.ACK_WAIT_THRESHOLD_NAME, ACK_WAIT_THRESHOLD.toString());

      cacheRule.createCache();
      createRegion();
      DistributionMessageObserver.setInstance(new ThreadMessageObserver("VM0"));
    });

    vm1.invoke(() -> {
      cacheRule.createCache();
      createRegion();
      DistributionMessageObserver.setInstance(new ThreadMessageObserver("VM1"));
    });

    AsyncInvocation<Object> vm0put = vm0.invokeAsync(() -> {
      String oldName = Thread.currentThread().getName();
      try {
        Thread.currentThread().setName(oldName + " PUT THREAD ");
        Region<Object, Object> region = cacheRule.getCache().getRegion(regionName);
        LogService.getLogger().info("#### adding data into region.");
        region.put("KEY-1", "VALUE-1");
      } finally {
        Thread.currentThread().setName(oldName);
      }
    });

    vm0.invoke(() -> {
      getBlackboard().waitForGate("PutReceivedOnVM1", 30, SECONDS);
      takeThreadDump();
      getBlackboard().signalGate("ThreadDumpTakenOnVM0");
    });

    vm0put.await();

  }

  @Test
  public void testThreadDumpHasWaitingMemberWithClientOp() throws Exception {
    VM vm0 = getVM(0);
    VM vm1 = getVM(1);
    VM vm2 = getVM(2);
    
    String hostName = getHostName();

    getBlackboard().initBlackboard();

    // Create region in one member
    vm0.invoke(() -> {
      System.setProperty(GeodeGlossary.GEMFIRE_PREFIX +
          DistributionConfig.ACK_WAIT_THRESHOLD_NAME, ACK_WAIT_THRESHOLD.toString());

      cacheRule.createCache();
      createRegion();
      DistributionMessageObserver.setInstance(new ThreadMessageObserver("VM0"));
    });

    int port = vm0.invoke(this::startServer);

    vm1.invoke(() -> {
      System.setProperty(GeodeGlossary.GEMFIRE_PREFIX +
          DistributionConfig.ACK_WAIT_THRESHOLD_NAME, ACK_WAIT_THRESHOLD.toString());

      cacheRule.createCache();
      createRegion();
      DistributionMessageObserver.setInstance(new ThreadMessageObserver("VM1"));
    });

    AsyncInvocation<Object> vm2put = vm2.invokeAsync(() -> {
      clientCacheRule.createClientCache();
      createClientRegion(hostName, port);
      Region<Object, Object> region = clientCacheRule.getClientCache().getRegion(regionName);
      LogService.getLogger().info("#### adding data into region.");
      region.put("KEY-1", "VALUE-1");
    });

    AsyncInvocation<Object> vm0put = vm0.invokeAsync(() -> {
      GeodeAwaitility.await().until(() ->
          getBlackboard().isGateSignaled("PutReceivedOnVM1") ||
              getBlackboard().isGateSignaled("PutReceivedOnVM0"));

      if (getBlackboard().isGateSignaled("PutReceivedOnVM1")) {
        takeThreadDump();
        getBlackboard().signalGate("ThreadDumpTakenOnVM0");
      }

    });

    AsyncInvocation<Object> vm1put = vm1.invokeAsync(() -> {
      GeodeAwaitility.await().until(() ->
          getBlackboard().isGateSignaled("PutReceivedOnVM1") ||
              getBlackboard().isGateSignaled("PutReceivedOnVM0"));

      if (getBlackboard().isGateSignaled("PutReceivedOnVM0")) {
        takeThreadDump();
        getBlackboard().signalGate("ThreadDumpTakenOnVM1");
      }
    });

    vm0put.await();
    vm1put.await();
    vm2put.await();
  }

  @Test
  public void testThreadDumpHasWaitingMemberWithDirectAck() throws Exception {
    VM vm0 = getVM(0);
    VM vm1 = getVM(1);

    getBlackboard().initBlackboard();

    // Create region in one member
    vm0.invoke(() -> {
      System.setProperty(GeodeGlossary.GEMFIRE_PREFIX +
          DistributionConfig.CONSERVE_SOCKETS_NAME, "false");

      System.setProperty(GeodeGlossary.GEMFIRE_PREFIX +
          DistributionConfig.ACK_WAIT_THRESHOLD_NAME, ACK_WAIT_THRESHOLD.toString());
      cacheRule.createCache();
      createRegion();
      DistributionMessageObserver.setInstance(new ThreadMessageObserver("VM0"));
    });

    vm1.invoke(() -> {
      System.setProperty(GeodeGlossary.GEMFIRE_PREFIX +
          DistributionConfig.CONSERVE_SOCKETS_NAME, "false");

      cacheRule.createCache();
      createRegion();
      DistributionMessageObserver.setInstance(new ThreadMessageObserver("VM1"));
    });

    AsyncInvocation<Object> vm0put = vm0.invokeAsync(() -> {
      String oldName = Thread.currentThread().getName();
      try {
        Thread.currentThread().setName("#### PUT THREAD");
        Region<Object, Object> region = cacheRule.getCache().getRegion(regionName);
        LogService.getLogger().info("#### adding data into region.");
        region.put("KEY-1", "VALUE-1");
      } finally {
        Thread.currentThread().setName(oldName);
      }
    });

    vm0.invoke(() -> {
      getBlackboard().waitForGate("PutReceivedOnVM1", 30, SECONDS);
      takeThreadDump();
      getBlackboard().signalGate("ThreadDumpTakenOnVM0");
    });

    vm0put.await();

  }

  private int startServer() throws IOException {
    CacheServer server = cacheRule.getCache().addCacheServer();
    server.setPort(0);
    server.start();
    return server.getPort();
  }

  private void createRegion() {
    RegionFactory<Object, Object> rf = cacheRule.getCache().createRegionFactory(RegionShortcut.REPLICATE);
    rf.create("testRegion");
  }

  private void createClientRegion(String hostName, int port) {
    Pool pool = PoolManager.createFactory().addServer(hostName, port).create("clientPool");

    ClientRegionFactory<Object, Object> rf = clientCacheRule.getClientCache().createClientRegionFactory(
        ClientRegionShortcut.CACHING_PROXY);
    rf.setPoolName(pool.getName()).create("testRegion");
  }

  private void takeThreadDump() {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] infos = bean.dumpAllThreads(true, true);
    for (ThreadInfo info : infos) {
      System.out.println("\n" + dump(info));
    }
  }


  public String dump(ThreadInfo threadInfo) {
    StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\"" +
        " Id=" + threadInfo.getThreadId() + " " +
        threadInfo.getThreadState());
    if (threadInfo.getLockName() != null) {
      sb.append(" on ").append(threadInfo.getLockName());
    }
    if (threadInfo.getLockOwnerName() != null) {
      sb.append(" owned by \"").append(threadInfo.getLockOwnerName()).append("\" Id=")
          .append(threadInfo.getLockOwnerId());
    }
    if (threadInfo.isSuspended()) {
      sb.append(" (suspended)");
    }
    if (threadInfo.isInNative()) {
      sb.append(" (in native)");
    }
    sb.append('\n');
    int i = 0;
    StackTraceElement[] stackTrace = threadInfo.getStackTrace();
    for (; i < stackTrace.length; i++) {
      StackTraceElement ste = stackTrace[i];
      sb.append("\tat ").append(ste.toString());
      sb.append('\n');
      if (i == 0 && threadInfo.getLockInfo() != null) {
        Thread.State ts = threadInfo.getThreadState();
        switch (ts) {
          case BLOCKED:
            sb.append("\t-  blocked on ").append(threadInfo.getLockInfo());
            sb.append('\n');
            break;
          case WAITING:
          case TIMED_WAITING:
            sb.append("\t-  waiting on ").append(threadInfo.getLockInfo());
            sb.append('\n');
            break;
          default:
        }
      }

      for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
        if (mi.getLockedStackDepth() == i) {
          sb.append("\t-  locked ").append(mi);
          sb.append('\n');
        }
      }
    }
//    if (i < stackTrace.length) {
//      sb.append("\t...");
//      sb.append('\n');
//    }

    LockInfo[] locks = threadInfo.getLockedSynchronizers();
    if (locks.length > 0) {
      sb.append("\n\tNumber of locked synchronizers = ").append(locks.length);
      sb.append('\n');
      for (LockInfo li : locks) {
        sb.append("\t- ").append(li);
        sb.append('\n');
      }
    }
    sb.append('\n');
    return sb.toString();
  }

  private class ThreadMessageObserver extends DistributionMessageObserver {

    final String vmName;

    ThreadMessageObserver(String vmName) {
      this.vmName = vmName;
    }

    @Override
    public void beforeProcessMessage(ClusterDistributionManager dm, DistributionMessage message) {
      LogService.getLogger().info("#### Message is :" + message);
      if (message instanceof UpdateOperation.UpdateMessage) {
        LogService.getLogger().info("#### Processing UpdateOperation.UpdateMessage");
        try {
          Thread.sleep(ACK_WAIT_THRESHOLD * 1000);
          getBlackboard().signalGate("PutReceivedOn" + vmName);
          getBlackboard().waitForGate("ThreadDumpTakenOn" + (vmName.equals("VM0") ? "VM1" : "VM0"), 30, SECONDS);
        } catch (Exception ignore) {
        }
      }
    }
  }

}
