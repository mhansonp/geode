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
package org.apache.geode.management.operation;


import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.management.api.ClusterManagementOperation;
import org.apache.geode.management.runtime.BlogCodeResponse;

/**
 * Defines a distributed system request to optimize bucket allocation across members.
 * RestoreRedundancyRequest - is part of an experimental API for performing operations on GEODE
 * using a REST interface. This API is experimental and may change.
 *
 */

@Experimental
public class BlogCodeRequest
    implements ClusterManagementOperation<BlogCodeResponse> {

  /**
   * see {@link #getEndpoint()}
   */
  public static final String BLOG_CODE_ENDPOINT = "/operations/blogCode";
  private static final long serialVersionUID = -2004467697819428456L;
  private String operator = "start blogCode";

  @Override
  @JsonIgnore
  public String getEndpoint() {
    return BLOG_CODE_ENDPOINT;
  }

  @Override
  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }


  @Override
  public String toString() {
    return "BlogCodeRequest{" +
        "operator='" + operator + '\'' +
        '}';
  }
}
