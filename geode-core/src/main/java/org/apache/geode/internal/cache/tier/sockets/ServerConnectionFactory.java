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
package org.apache.geode.internal.cache.tier.sockets;


import java.io.IOException;
import java.net.Socket;

import org.apache.geode.StatisticsFactory;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.client.protocol.ClientProtocolService;
import org.apache.geode.internal.cache.client.protocol.ClientProtocolServiceLoader;
import org.apache.geode.internal.cache.tier.Acceptor;
import org.apache.geode.internal.cache.tier.CachedRegionHelper;
import org.apache.geode.internal.security.SecurityService;

/**
 * Creates instances of ServerConnection based on the connection mode provided.
 */
public class ServerConnectionFactory {

  private final ClientProtocolServiceLoader clientProtocolServiceLoader;
  private volatile ClientProtocolService clientProtocolService;

  public ServerConnectionFactory() {
    clientProtocolServiceLoader = new ClientProtocolServiceLoader();
  }

  private synchronized ClientProtocolService getClientProtocolService(
      StatisticsFactory statisticsFactory, String serverName) {
    if (clientProtocolService == null) {
      clientProtocolService = clientProtocolServiceLoader.lookupService();
      clientProtocolService.initializeStatistics(serverName, statisticsFactory);
    }
    return clientProtocolService;
  }

  ServerConnection makeServerConnection(final Socket socket, final InternalCache cache,
      final CachedRegionHelper cachedRegionHelper, final CacheServerStats stats,
      final int hsTimeout, final int socketBufferSize, final String communicationModeStr,
      final byte communicationMode, final Acceptor acceptor, final SecurityService securityService)
      throws IOException {

    return new OriginalServerConnection(socket, cache, cachedRegionHelper, stats, hsTimeout,
        socketBufferSize, communicationModeStr, communicationMode, acceptor, securityService);
  }
}
