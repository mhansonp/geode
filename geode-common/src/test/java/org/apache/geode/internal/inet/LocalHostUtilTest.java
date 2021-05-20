package org.apache.geode.internal.inet;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Set;

import javax.naming.spi.Resolver;

import org.junit.Before;
import org.junit.Test;


public class LocalHostUtilTest {

  @Before
  public void before() {

  }

  @Test
  public void testGetMyAddresses() throws SocketException {
    Set<InetAddress> myAddresses = LocalHostUtil.getMyAddresses();
    for (InetAddress address : myAddresses) {
      assertThat(address.isAnyLocalAddress()).isFalse();
      assertThat(address.isLinkLocalAddress()).isFalse();
      assertThat(address.isLoopbackAddress()).isFalse();
      if (address instanceof Inet6Address) {
        Inet6Address inet6Address = (Inet6Address) address;
        assertThat(inet6Address.getScopedInterface().isLoopback()).isFalse();
        assertThat(inet6Address.getHostAddress().contains("%lo")).isFalse();
      }
    }
  }
  @Test
  public void testIsLocalHost() throws SocketException, UnknownHostException {

    InetAddress inet6Address = InetAddress.getByAddress("", new byte[]{0,0,0,0,0,0,0,0,0,0,
        (byte) 0xff,
        (byte) 0xff,0x7f,00,0,1});
    InetSocketAddress inet6Address1 = new InetSocketAddress("0:0:0:0:0:ffff:7f00:1%lo", 29298);
    assertThat(LocalHostUtil.isLocalHost(inet6Address)).isTrue();
    assertThat(LocalHostUtil.isLocalHost(inet6Address1)).isTrue();
    assertThat(inet6Address1.getAddress().isLoopbackAddress()).isTrue();
      assertThat(inet6Address.isLoopbackAddress()).isTrue();
////        InetSocketAddress inetSocketAddress =  new InetSocketAddress(, 29298));
//    Set<InetAddress> myAddresses = LocalHostUtil.getMyAddresses();
//    for (InetAddress address : myAddresses) {
//      assertThat(address.isAnyLocalAddress()).isFalse();
//      assertThat(address.isLinkLocalAddress()).isFalse();
//      assertThat(address.isLoopbackAddress()).isFalse();
//      if (address instanceof Inet6Address) {
//        Inet6Address inet6Address = (Inet6Address) address;
//        assertThat(inet6Address.getScopedInterface().isLoopback()).isFalse();
//        assertThat(inet6Address.getHostAddress().contains("%lo")).isFalse();
//      }
//    }
  }
}
