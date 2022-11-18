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

package org.apache.camel.k.resume.kafka;

import org.apache.camel.processor.resume.kafka.KafkaResumeStrategyConfiguration;
import org.apache.camel.processor.resume.kafka.KafkaResumeStrategyConfigurationBuilder;
import org.apache.camel.processor.resume.kafka.SingleNodeKafkaResumeStrategy;
import org.apache.camel.resume.Cacheable;
import org.apache.camel.resume.ResumeStrategy;
import org.apache.camel.util.ObjectHelper;

public final class KafkaResumeFactory {

    private KafkaResumeFactory() {

    }

    public static ResumeStrategy build(String name, String resumeServer, String topic, String cacheFillPolicy) {
        Cacheable.FillPolicy policy = extracted(cacheFillPolicy);

        final KafkaResumeStrategyConfiguration resumeStrategyConfiguration = KafkaResumeStrategyConfigurationBuilder.newBuilder()
                .withBootstrapServers(resumeServer)
                .withCacheFillPolicy(policy)
                .withTopic(topic)
                .build();

        switch (name) {
            case "org.apache.camel.processor.resume.kafka.SingleNodeKafkaResumeStrategy": {
                return new SingleNodeKafkaResumeStrategy<>(resumeStrategyConfiguration);
            }
            default: {
                throw new UnsupportedOperationException(String.format("The strategy %s is not a valid strategy", name));
            }
        }
    }

    private static Cacheable.FillPolicy extracted(String cacheFillPolicy) {
        if (!ObjectHelper.isEmpty(cacheFillPolicy) && cacheFillPolicy.equals("minimizing")) {
            return Cacheable.FillPolicy.MINIMIZING;
        }

        return Cacheable.FillPolicy.MAXIMIZING;
    }
}
