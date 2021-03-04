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
package org.apache.geode.distributed.internal;

import java.io.DataInputStream;
import java.net.Socket;

import org.apache.logging.log4j.Logger;

import org.apache.geode.distributed.internal.tcpserver.ProtocolChecker;
import org.apache.geode.internal.cache.client.protocol.ClientProtocolServiceLoader;
import org.apache.geode.logging.internal.log4j.api.LogService;

public class ProtocolCheckerImpl implements ProtocolChecker {
  private static final Logger logger = LogService.getLogger();
  public final InternalLocator internalLocator;
  public final ClientProtocolServiceLoader clientProtocolServiceLoader;

  public ProtocolCheckerImpl(
      final InternalLocator internalLocator,
      final ClientProtocolServiceLoader clientProtocolServiceLoader) {
    this.internalLocator = internalLocator;
    this.clientProtocolServiceLoader = clientProtocolServiceLoader;
  }


  @Override
  public boolean checkProtocol(final Socket socket, final DataInputStream input,
      final int firstByte) throws Exception {
    return false;
  }
}
