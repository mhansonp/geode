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

package org.apache.geode.management.internal.rest.controllers;

import static org.apache.geode.management.configuration.Links.URI_VERSION;
import static org.apache.geode.management.operation.BlogCodeRequest.BLOG_CODE_ENDPOINT;

import java.util.Optional;

import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.apache.geode.internal.security.SecurityService;
import org.apache.geode.management.api.ClusterManagementListOperationsResult;
import org.apache.geode.management.api.ClusterManagementOperationResult;
import org.apache.geode.management.operation.BlogCodeRequest;
import org.apache.geode.management.runtime.BlogCodeResponse;

@RestController("blogCodeOperation")
@RequestMapping(URI_VERSION)
public class BlogCodeOperationController extends AbstractManagementController {
  @ApiOperation(value = "start blogCode")
  @PreAuthorize("@securityService.authorize('DATA', 'MANAGE')")
  @PostMapping(BLOG_CODE_ENDPOINT)
  public ResponseEntity<ClusterManagementOperationResult<BlogCodeRequest, BlogCodeResponse>> startBlogCode(
      @RequestBody BlogCodeRequest operation) {
    operation.setOperator(
        Optional.ofNullable(securityService).map(SecurityService::getSubject).map(Object::toString)
            .orElse(null));
    ClusterManagementOperationResult<BlogCodeRequest, BlogCodeResponse> result =
        clusterManagementService
            .start(operation);
    return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
  }

  @ApiOperation(value = "list blogCode")
  @PreAuthorize("@securityService.authorize('DATA', 'MANAGE')")
  @GetMapping(BLOG_CODE_ENDPOINT)
  public ClusterManagementListOperationsResult<BlogCodeRequest, BlogCodeResponse> listBlogCode() {
    return clusterManagementService.list(new BlogCodeRequest());
  }

  @ApiOperation(value = "get blogCode")
  @PreAuthorize("@securityService.authorize('DATA', 'MANAGE')")
  @GetMapping(BLOG_CODE_ENDPOINT + "/{id:.+}")
  public ClusterManagementOperationResult<BlogCodeRequest, BlogCodeResponse> getBlogCode(
      @PathVariable String id) {
    return clusterManagementService.get(new BlogCodeRequest(), id);
  }
}
