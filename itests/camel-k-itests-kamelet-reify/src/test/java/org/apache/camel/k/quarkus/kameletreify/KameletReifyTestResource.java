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
package org.apache.camel.k.quarkus.kameletreify;

import java.util.Arrays;
import java.util.Map;

import com.github.dockerjava.api.model.Ulimit;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.test.infra.artemis.services.ArtemisContainer;
import org.apache.camel.test.infra.messaging.services.MessagingLocalContainerService;
import org.apache.camel.test.infra.messaging.services.MessagingService;
import org.apache.camel.test.infra.messaging.services.MessagingServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KameletReifyTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(KameletReifyTestResource.class);

    private static MessagingService messagingService = MessagingServiceFactory
            .builder()
            .addLocalMapping(KameletReifyTestResource::createLocalService)
            .build();

    public static MessagingLocalContainerService<ArtemisContainer> createLocalService() {
        ArtemisContainer artemisContainer = new ArtemisContainer();

        artemisContainer.withCreateContainerCmdModifier( c -> c.getHostConfig()
                .withUlimits(Arrays.asList(new Ulimit("nofile", 5000L, 5000L))));

        return new MessagingLocalContainerService<>(artemisContainer, c -> c.defaultEndpoint());
    }

    @Override
    public Map<String, String> start() {
        try {
            System.out.println("Starting ...");
            messagingService.initialize();

            System.out.println("Using endpoint: " + messagingService.defaultEndpoint());

            return Map.of(
                "amqBrokerUrl", messagingService.defaultEndpoint(),
                "amqQueueName", "my-queue"
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {

    }
}

