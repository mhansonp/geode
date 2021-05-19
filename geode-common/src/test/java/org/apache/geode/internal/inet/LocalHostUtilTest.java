package org.apache.geode.internal.inet;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;


public class LocalHostUtilTest {

  @Before
  public void before() {

  }

  @Test
  public void testGetMyAddresses() {
    System.out.println("My addresses = " + LocalHostUtil.getMyAddresses());
    Set<InetAddress> myAddresses =  LocalHostUtil.getMyAddresses();
    for (InetAddress address : myAddresses) {
      System.out.println("address = " + address);
      assertThat(address.isAnyLocalAddress()).isFalse();
      assertThat(address.isLinkLocalAddress()).isFalse();
      assertThat(address.isLoopbackAddress()).isFalse();
//      assertThat(address.isSiteLocalAddress()).isFalse();
      if(address instanceof Inet6Address) {
        System.out.println("address " + address + " is an IPv6 address ");
        Inet6Address inet6Address = (Inet6Address) address;
        assertThat(inet6Address.isLoopbackAddress()).isFalse();
        assertThat(inet6Address.getHostAddress().contains("%lo")).isFalse();
      }
    }
  }

}
