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
package org.apache.geode.logging.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import org.apache.geode.logging.internal.log4j.api.LogService;

/**
 * SelectiveLogger
 * This class' purpose is to enable one to log a bunch of data that will
 * be helpful in some event. In the case that the event occurs, one uses the
 * print routine, and the log statements will be added to the log. Otherwise,
 * just use the clear option to clear the list. This is best thought of as a
 * debug tool. Using this as a static or something like that would be a bad
 * idea.
 */

public class SelectiveLogger {
  final List<String> logMessages = new ArrayList<>();
  Logger logger = LogService.getLogger();
  Supplier<String> prependSupplier;

  public SelectiveLogger setPrepend(Supplier<String> stringSupplier) {
    prependSupplier = stringSupplier;
    return this;
  }

  /**
   * log
   * The purpose of this method is to add a log message that maybe printed out to the
   * list of messages.
   *
   * @param message - the string to be output into the logs
   */
  public SelectiveLogger log(String message) {
    logger.info(prependSupplier.get() + " " + message);
    return this;
  }

  /**
   * This method will print the log messages to log service and clear the list
   */
  public SelectiveLogger print() {
    logger.info(prependSupplier.get() + " Informational stack dump",
        new Exception("Showing stack trace"));
    logMessages.clear();
    return this;
  }

  /**
   * The method clears the list of log messages
   */
  public void flush() {
    logMessages.clear();
  }
}
