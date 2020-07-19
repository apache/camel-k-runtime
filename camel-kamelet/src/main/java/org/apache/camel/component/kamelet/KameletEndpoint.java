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
package org.apache.camel.component.kamelet;

import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.support.DefaultProducer;
import org.apache.camel.support.service.ServiceHelper;

@UriEndpoint(
    firstVersion = "3.5.0",
    scheme = "kamelet",
    syntax = "kamelet:templateId/routeId",
    title = "Kamelet",
    label = "camel-k")
public class KameletEndpoint extends DefaultEndpoint {
    @Metadata(required = true)
    @UriPath(description = "The Route Template ID")
    private final String templateId;

    @Metadata(required = false)
    @UriPath(description = "The Route ID", defaultValueNote = "The ID will be auto-generated if not provided")
    private final String routeId;

    private final Map<String, Object> kameletProperties;
    private final String kameletUri;

    public KameletEndpoint(
            String uri,
            KameletComponent component,
            String templateId,
            String routeId,
            Map<String, Object> kameletProperties) {

        super(uri, component);

        this.templateId = templateId;
        this.routeId = routeId;
        this.kameletProperties = kameletProperties;
        this.kameletUri = "direct:" + routeId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getRouteId() {
        return routeId;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new KameletProducer();
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        Consumer answer = new KemeletConsumer(processor);
        configureConsumer(answer);

        return answer;
    }

    @Override
    protected void doStart() throws Exception {
        try {
            // Add a route to the camel context from the given template
            // TODO: add validation (requires: https://issues.apache.org/jira/browse/CAMEL-15312)
            getCamelContext().addRouteFromTemplate(routeId, templateId, kameletProperties);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        super.doStart();
    }

    // *********************************
    //
    // Helpers
    //
    // *********************************

    private class KemeletConsumer extends DefaultConsumer {
        private volatile Endpoint endpoint;
        private volatile Consumer consumer;

        public KemeletConsumer(Processor processor) {
            super(KameletEndpoint.this, processor);
        }

        @Override
        protected void doStart() throws Exception {
            endpoint = getCamelContext().getEndpoint(kameletUri);
            consumer = endpoint.createConsumer(getProcessor());

            ServiceHelper.startService(endpoint);
            ServiceHelper.startService(consumer);

            super.doStart();
        }

        @Override
        protected void doStop() throws Exception {
            ServiceHelper.stopService(endpoint);
            ServiceHelper.stopService(consumer);

            super.doStop();
        }
    }

    private class KameletProducer extends DefaultProducer {
        private volatile Endpoint endpoint;
        private volatile Producer producer;

        public KameletProducer() {
            super(KameletEndpoint.this);
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            if (producer != null) {
                producer.process(exchange);
            }
        }

        @Override
        protected void doStart() throws Exception {
            endpoint = getCamelContext().getEndpoint(kameletUri);
            producer = endpoint.createProducer();

            ServiceHelper.startService(endpoint);
            ServiceHelper.startService(producer);

            super.doStart();
        }

        @Override
        protected void doStop() throws Exception {
            ServiceHelper.stopService(endpoint);
            ServiceHelper.stopService(producer);

            super.doStop();
        }
    }
}