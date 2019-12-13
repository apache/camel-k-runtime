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
package org.apache.camel.component.knative;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.component.knative.ce.CloudEventProcessor;
import org.apache.camel.component.knative.spi.CloudEvent;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.support.processor.DelegateAsyncProcessor;

/**
 * The KnativeReplyProcessor handles the processing of replies returned by the consumer.
 */
public class KnativeReplyProcessor extends DelegateAsyncProcessor {

    private final boolean cloudEventEnabled;

    private final CloudEventProcessor cloudEventProcessor;

    public KnativeReplyProcessor(KnativeEndpoint endpoint, KnativeEnvironment.KnativeServiceDefinition service, CloudEventProcessor cloudEventProcessor, boolean cloudEventEnabled) {
        super(cloudEventEnabled ? cloudEventProcessor.producer(endpoint, service) : null);

        this.cloudEventEnabled = cloudEventEnabled;
        this.cloudEventProcessor = cloudEventProcessor;
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (cloudEventEnabled) {
            // Delegate to CloudEvent processor
            return processor.process(exchange, callback);
        }

        // remove CloudEvent headers
        for (CloudEvent.Attribute attr : cloudEventProcessor.cloudEvent().attributes()) {
            exchange.getMessage().removeHeader(attr.http());
        }
        callback.done(true);
        return true;
    }

}
