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
import static org.apache.geode.test.dunit.VM.getVM;
import static org.apache.geode.test.dunit.internal.JUnit4DistributedTestCase.getBlackboard;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Serializable;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import sun.jvm.hotspot.StackTrace;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.distributed.internal.ClusterDistributionManager;
import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.distributed.internal.DistributionMessageObserver;
import org.apache.geode.internal.cache.partitioned.PutMessage;
import org.apache.geode.internal.logging.LogService;
import org.apache.geode.test.dunit.AsyncInvocation;
import org.apache.geode.test.dunit.VM;
import org.apache.geode.test.dunit.rules.CacheRule;
import org.apache.geode.test.dunit.rules.ClientCacheRule;
import org.apache.geode.test.dunit.rules.DistributedRestoreSystemProperties;
import org.apache.geode.test.dunit.rules.DistributedRule;

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

  private String regionName = "testRegion";

  private final Integer ACK_WAIT_THRESHOLD = new Integer(2);

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

      System.setProperty(DistributionConfig.GEMFIRE_PREFIX +
          DistributionConfig.ACK_WAIT_THRESHOLD_NAME, ACK_WAIT_THRESHOLD.toString());

      cacheRule.createCache();
      createRegion(regionName, false);
      DistributionMessageObserver.setInstance(new ThreadMessageObserver());
    });

    vm1.invoke(() -> {
      cacheRule.createCache();
      createRegion(regionName, false);
      DistributionMessageObserver.setInstance(new ThreadMessageObserver());
    });

    AsyncInvocation vm0put = vm0.invokeAsync(() -> {
      String oldName = Thread.currentThread().getName();
      try {
        Thread.currentThread().setName(oldName + " PUT THREAD ");
        Region region = cacheRule.getCache().getRegion(regionName);
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

    vm0put.join();

  }

  @Test
  public void testThreadDumpHasWaitingMemberWithDirectAck() throws Exception {
    VM vm0 = getVM(0);
    VM vm1 = getVM(1);

    getBlackboard().initBlackboard();

    // Create region in one member
    vm0.invoke(() -> {
      System.setProperty(DistributionConfig.GEMFIRE_PREFIX +
          DistributionConfig.CONSERVE_SOCKETS_NAME, "false");

      System.setProperty(DistributionConfig.GEMFIRE_PREFIX +
          DistributionConfig.ACK_WAIT_THRESHOLD_NAME, ACK_WAIT_THRESHOLD.toString());
      cacheRule.createCache();
      createRegion(regionName, false);
      DistributionMessageObserver.setInstance(new ThreadMessageObserver());
    });

    vm1.invoke(() -> {
      System.setProperty(DistributionConfig.GEMFIRE_PREFIX +
          DistributionConfig.CONSERVE_SOCKETS_NAME, "false");

      cacheRule.createCache();
      createRegion(regionName, false);
      DistributionMessageObserver.setInstance(new ThreadMessageObserver());
    });

    AsyncInvocation vm0put = vm0.invokeAsync(() -> {
      String oldName = Thread.currentThread().getName();
      try {
        Thread.currentThread().setName("#### PUT THREAD");
        Region region = cacheRule.getCache().getRegion(regionName);
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

    vm0put.join();

  }

  private void createRegion(String regionName, boolean addAsyncEventQueueId) {
    RegionFactory rf = cacheRule.getCache().createRegionFactory(RegionShortcut.REPLICATE);
    if (addAsyncEventQueueId) {
      rf.addAsyncEventQueueId("aeqId");
    }
    rf.create(regionName);
  }

  private void createRegion(String regionName, Class exception) {
    RegionFactory rf = cacheRule.getCache().createRegionFactory(RegionShortcut.REPLICATE)
        .addAsyncEventQueueId("aeqId");
    assertThatThrownBy(() -> rf.create(regionName)).isInstanceOf(exception);
  }

  private void takeThreadDump() {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] infos = bean.dumpAllThreads(true, true);
    for (ThreadInfo info : infos) {
      LogService.getLogger().info("\n" + dump(info));
    }
  }


  public String dump(ThreadInfo threadInfo) {
    StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\"" +
        " Id=" + threadInfo.getThreadId() + " " +
        threadInfo.getThreadState());
    if (threadInfo.getLockName() != null) {
      sb.append(" on " + threadInfo.getLockName());
    }
    if (threadInfo.getLockOwnerName() != null) {
      sb.append(" owned by \"" + threadInfo.getLockOwnerName() +
          "\" Id=" + threadInfo.getLockOwnerId());
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
      sb.append("\tat " + ste.toString());
      sb.append('\n');
      if (i == 0 && threadInfo.getLockInfo() != null) {
        Thread.State ts = threadInfo.getThreadState();
        switch (ts) {
          case BLOCKED:
            sb.append("\t-  blocked on " + threadInfo.getLockInfo());
            sb.append('\n');
            break;
          case WAITING:
            sb.append("\t-  waiting on " + threadInfo.getLockInfo());
            sb.append('\n');
            break;
          case TIMED_WAITING:
            sb.append("\t-  waiting on " + threadInfo.getLockInfo());
            sb.append('\n');
            break;
          default:
        }
      }

      for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
        if (mi.getLockedStackDepth() == i) {
          sb.append("\t-  locked " + mi);
          sb.append('\n');
        }
      }
    }
    if (i < stackTrace.length) {
      sb.append("\t...");
      sb.append('\n');
    }

    LockInfo[] locks = threadInfo.getLockedSynchronizers();
    if (locks.length > 0) {
      sb.append("\n\tNumber of locked synchronizers = " + locks.length);
      sb.append('\n');
      for (LockInfo li : locks) {
        sb.append("\t- " + li);
        sb.append('\n');
      }
    }
    sb.append('\n');
    return sb.toString();
  }

  private class ThreadMessageObserver extends DistributionMessageObserver {

    @Override
    public void beforeProcessMessage(ClusterDistributionManager dm, DistributionMessage message) {
      LogService.getLogger().info("#### Message is :" + message);
      if (message instanceof UpdateOperation.UpdateMessage) {
        try {
          Thread.sleep(ACK_WAIT_THRESHOLD.intValue() * 1000);
          getBlackboard().signalGate("PutReceivedOnVM1");
          getBlackboard().waitForGate("ThreadDumpTakenOnVM0", 30, SECONDS);
        } catch (Exception ex) {
        }
      }
    }
  }

}
