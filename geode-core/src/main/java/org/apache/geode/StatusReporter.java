package org.apache.geode;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import org.apache.geode.logging.internal.log4j.api.LogService;

public class StatusReporter {
  protected static Logger logger = LogService.getLogger();

  public static boolean isComponentRegistered(ComponentStatus componentStatus) {
    synchronized (componentStatusSet) {
      return componentStatusSet.contains(componentStatus);
    }
  }

  enum State {
    SR_RUNNING, SR_NOT_STARTED
  }

  private static final Set<ComponentStatus> componentStatusSet = new HashSet<>();
  private static State status = State.SR_NOT_STARTED;
  private static final Semaphore stopSem = new Semaphore(1);

  static final Thread statusReporterThread = new Thread(() -> {
    status = State.SR_RUNNING;
    logger.info("StatusReporter Started thread.");
    try {
      stopSem.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    while (true) {
      try {
        if (stopSem.tryAcquire(5000, TimeUnit.MILLISECONDS)) {
          break;
        }
        StatusReporter.getStatusReport();
      } catch (Exception e) {
        return;
      }
    }
    logger.info("StatusReporter Stopping thread.");
    status = State.SR_NOT_STARTED;
  });

  public static State getStatus() {
    State tempState;
    synchronized (statusReporterThread) {
      if(status != State.SR_RUNNING) {
        StatusReporter.start();
      }
      tempState = status;
    }
    return tempState;
  }

  public static void start() {
    synchronized (statusReporterThread) {
      statusReporterThread.start();
    }
  }

  public static void stop() throws InterruptedException {
    synchronized (statusReporterThread) {
      stopSem.release();
    }
    statusReporterThread.join();
  }

  public static boolean registerComponent(ComponentStatus componentStatus) {
    synchronized (componentStatusSet) {
      return componentStatusSet.add(componentStatus);
    }
  }

  public static boolean removeComponent(ComponentStatus componentStatus) {
    synchronized (componentStatusSet) {
      return componentStatusSet.remove(componentStatus);
    }
  }

  public static boolean getStatusReport() {
    synchronized (statusReporterThread) {
      if (componentStatusSet.isEmpty()) {
        return false;
      }
      for (ComponentStatus componentStatus : componentStatusSet) {
        componentStatus.print();
      }
    }
    return true;
  }

}
