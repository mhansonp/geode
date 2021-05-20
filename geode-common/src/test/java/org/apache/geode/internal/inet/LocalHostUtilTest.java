package org.apache.geode.internal.inet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;


public class LocalHostUtilTest {

  @Before
  public void before() {

  }

  @Test
  public void testGetMyAddresses() throws SocketException {
    Set<InetAddress> myAddresses =  LocalHostUtil.getMyAddresses();
    for (InetAddress address : myAddresses) {
      assertThat(address.isAnyLocalAddress()).isFalse();
      assertThat(address.isLinkLocalAddress()).isFalse();
      assertThat(address.isLoopbackAddress()).isFalse();
      if(address instanceof Inet6Address) {
        Inet6Address inet6Address = (Inet6Address) address;
        assertThat(inet6Address.getScopedInterface().isLoopback()).isFalse();
        assertThat(inet6Address.getHostAddress().contains("%lo")).isFalse();
      }
    }
  }

}
