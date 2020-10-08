/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.k.main;

import org.apache.camel.k.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ApplicationSupportTest {

    @Test
    void configureLogging() {
        System.setProperty(Constants.PROPERTY_CAMEL_K_CONF, "src/test/resources/logger.properties");

        try {
            Assertions.assertFalse(LoggerFactory.getLogger("org.foo").isDebugEnabled());
            Assertions.assertTrue(LoggerFactory.getLogger("org.bar").isInfoEnabled());

            ApplicationSupport.configureLogging();

            Assertions.assertTrue(LoggerFactory.getLogger("org.foo").isDebugEnabled());
            Assertions.assertFalse(LoggerFactory.getLogger("org.bar").isInfoEnabled());
        } finally {
            System.getProperties().remove(Constants.PROPERTY_CAMEL_K_CONF);
        }
    }
}
