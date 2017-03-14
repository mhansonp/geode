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

package org.apache.geode.management.internal.cli.commands;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.geode.distributed.ConfigurationProperties;
import org.apache.geode.internal.AvailablePortHelper;
import org.apache.geode.test.dunit.rules.GfshShellConnectionRule;
import org.apache.geode.test.dunit.rules.Locator;
import org.apache.geode.test.dunit.rules.LocatorStarterRule;
import org.apache.geode.test.junit.categories.IntegrationTest;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Properties;

@Category(IntegrationTest.class)
public class ExportLogsIntegrationTest {

  @ClassRule
  public static LocatorStarterRule locatorStarterRule = new LocatorStarterRule();

  private static Locator locator;

  @Rule
  public GfshShellConnectionRule gfsh = new GfshShellConnectionRule();

  private static int[] ports = AvailablePortHelper.getRandomAvailableTCPPorts(2);
  protected static int httpPort = ports[0];
  protected static int jmxPort = ports[1];

  @BeforeClass
  public static void before() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(ConfigurationProperties.HTTP_SERVICE_PORT, httpPort + "");
    properties.setProperty(ConfigurationProperties.JMX_MANAGER_PORT, jmxPort + "");
    locator = locatorStarterRule.startLocator(properties);
  }

  protected void connect() throws Exception {
    gfsh.connectAndVerify(locator);
  }

  @Test
  public void testInvalidMember() throws Exception {
    connect();
    gfsh.executeCommand("export logs --member=member1,member2");
    assertThat(gfsh.getGfshOutput()).contains("No Members Found");
  }

  @Test
  public void testNothingToExport() throws Exception {
    connect();
    gfsh.executeCommand("export logs --stats-only");
    assertThat(gfsh.getGfshOutput()).contains("No files to be exported.");
  }
}
