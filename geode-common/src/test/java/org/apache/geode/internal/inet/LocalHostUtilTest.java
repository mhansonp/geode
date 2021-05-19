package org.apache.geode.internal.inet;

import org.junit.Before;
import org.junit.Test;


public class LocalHostUtilTest {

  @Before
  public void before() {

  }

  @Test
  public void testGetMyAddresses() {
    System.out.println("My addresses = " + LocalHostUtil.getMyAddresses());


  }

}
