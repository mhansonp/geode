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
 *
 */

package org.apache.geode.management.internal.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.management.api.ClusterManagementOperationResult;
import org.apache.geode.management.api.ClusterManagementService;
import org.apache.geode.management.client.ClusterManagementServiceBuilder;
import org.apache.geode.management.operation.BlogCodeRequest;
import org.apache.geode.management.runtime.BlogCodeResponse;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.rules.MemberStarterRule;

/**
 * This class borrows very heavily from the RestoreRedundancyCommandDUnitTest
 *
 */

public class BlogCodeDUnitTest {

  @Rule
  public ClusterStartupRule cluster = new ClusterStartupRule();

  private MemberVM locator;
  private List<MemberVM> servers;
  private static final int SERVERS_TO_START = 1;

  private ClusterManagementService client1;

  @Before
  public void setup() {
    locator = cluster.startLocatorVM(0, MemberStarterRule::withHttpService);
    servers = new ArrayList<>();
    int locatorPort = locator.getPort();
    IntStream.range(0, SERVERS_TO_START)
        .forEach(i -> servers.add(cluster.startServerVM(i + 1, locatorPort)));

    client1 = new ClusterManagementServiceBuilder()
        .setHost("localhost")
        .setPort(locator.getHttpPort())
        .build();
  }

  @After
  public void tearDown() {
    client1.close();
  }

  @Test
  public void testBlogCode()
      throws ExecutionException, InterruptedException {

    BlogCodeRequest blogCodeRequest = new BlogCodeRequest();

    ClusterManagementOperationResult<BlogCodeRequest, BlogCodeResponse> startResult =
        client1.start(blogCodeRequest);

    assertThat(startResult.isSuccessful()).isTrue();

    ClusterManagementOperationResult<BlogCodeRequest, BlogCodeResponse> endResult =
        client1.getFuture(blogCodeRequest, startResult.getOperationId()).get();

    BlogCodeResponse blogCodeResponse = endResult.getOperationResult();

    assertThat(blogCodeResponse.getSuccess()).isTrue();

    assertThat(blogCodeResponse.getStatusMessage()).isEqualTo("Hello, World!");
  }
}
