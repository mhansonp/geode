package org.apache.geode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class StatusReporterTest {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testStart() throws InterruptedException {
    StatusReporter.start();
    while (StatusReporter.getStatus() != StatusReporter.State.SR_RUNNING) {
      Thread.sleep(1000);
    }
    assertThat(StatusReporter.getStatus()).isEqualTo(StatusReporter.State.SR_RUNNING);
  }

  @Test
  public void testRegisterComponent() {
    PrintStream printStream = Mockito.mock(PrintStream.class);
    ComponentStatus componentStatus = new ComponentStatus() {
      @Override
      public String name() {
        return "Test Component";
      }

      @Override
      public String getStatusString() {
        return "Everything is fine and happy.";
      }

      @Override
      public void print() {
        printStream.println(getStatusString());
      }
    };

    StatusReporter.registerComponent(componentStatus);
    assertThat(StatusReporter.isComponentRegistered(componentStatus)).isTrue();
    StatusReporter.removeComponent(componentStatus);
  }

  @Test
  public void testGetStatusReport() {
    PrintStream printStream = Mockito.mock(PrintStream.class);
    ComponentStatus componentStatus = new ComponentStatus() {
      @Override
      public String name() {
        return "Test Component";
      }

      @Override
      public String getStatusString() {
        return "Everything is fine and happy.";
      }

      @Override
      public void print() {
        printStream.println(getStatusString());
      }
    };

    StatusReporter.registerComponent(componentStatus);
    assertThat(StatusReporter.getStatusReport()).isTrue();
    Mockito.verify(printStream).println(componentStatus.getStatusString());
    StatusReporter.removeComponent(componentStatus);
  }

  @Test
  public void testPeriodicReports() throws InterruptedException {
    PrintStream printStream = Mockito.mock(PrintStream.class);
    ComponentStatus componentStatus = new ComponentStatus() {
      @Override
      public String name() {
        return "Test Component";
      }

      @Override
      public String getStatusString() {
        return "Everything is fine and happy.";
      }

      @Override
      public void print() {
        printStream.println(getStatusString());
      }
    };

    StatusReporter.registerComponent(componentStatus);
    Thread.sleep(5000);
    Mockito.verify(printStream).println(componentStatus.getStatusString());
    StatusReporter.removeComponent(componentStatus);
  }
}
