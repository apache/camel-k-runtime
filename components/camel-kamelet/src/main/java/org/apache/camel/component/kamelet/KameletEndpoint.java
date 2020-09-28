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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UriEndpoint(
    firstVersion = "3.5.0",
    scheme = "kamelet",
    syntax = "kamelet:templateId/routeId",
    title = "Kamelet",
    lenientProperties = true,
    label = "camel-k")
public class KameletEndpoint extends DefaultEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(KameletEndpoint.class);

    @Metadata(required = true)
    @UriPath(description = "The Route Template ID")
    private final String templateId;
    @Metadata(required = false)
    @UriPath(description = "The Route ID", defaultValueNote = "The ID will be auto-generated if not provided")
    private final String routeId;

    @UriParam(label = "producer", defaultValue = "true")
    private boolean block = true;
    @UriParam(label = "producer", defaultValue = "30000")
    private long timeout = 30000L;
    @UriParam(label = "producer", defaultValue = "true")

    private final Map<String, Object> kameletProperties;
    private final Map<String, KameletConsumer> consumers;
    private final String key;

    public KameletEndpoint(
            String uri,
            KameletComponent component,
            String templateId,
            String routeId,
            Map<String, KameletConsumer> consumers) {

        super(uri, component);

        ObjectHelper.notNull(templateId, "template id");
        ObjectHelper.notNull(routeId, "route id");

        this.templateId = templateId;
        this.routeId = routeId;
        this.key = templateId + "/" + routeId;
        this.kameletProperties = new HashMap<>();
        this.consumers = consumers;
    }

    public boolean isBlock() {
        return block;
    }

    /**
     * If sending a message to a direct endpoint which has no active consumer, then we can tell the producer to block
     * and wait for the consumer to become active.
     */
    public void setBlock(boolean block) {
        this.block = block;
    }

    public long getTimeout() {
        return timeout;
    }

    /**
     * The timeout value to use if block is enabled.
     *
     * @param timeout the timeout value
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public KameletComponent getComponent() {
        return (KameletComponent) super.getComponent();
    }

    @Override
    public boolean isLenientProperties() {
        return true;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getRouteId() {
        return routeId;
    }

    /**
     * Custom properties for kamelet
     */
    public void setKameletProperties(Map<String, Object> kameletProperties) {
        if (kameletProperties != null) {
            this.kameletProperties.clear();
            this.kameletProperties.putAll(kameletProperties);
        }
    }

    public Map<String, Object> getKameletProperties() {
        return Collections.unmodifiableMap(kameletProperties);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new KameletProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        Consumer answer = new KameletConsumer(this, processor);
        configureConsumer(answer);
        return answer;
    }

    // *********************************
    //
    // Helpers
    //
    // *********************************

    void addConsumer(KameletConsumer consumer) {
        synchronized (consumers) {
            if (consumers.putIfAbsent(key, consumer) != null) {
                throw new IllegalArgumentException(
                    "Cannot add a 2nd consumer to the same endpoint. Endpoint " + this + " only allows one consumer.");
            }
            consumers.notifyAll();
        }
    }

    void removeConsumer(KameletConsumer consumer) {
        synchronized (consumers) {
            consumers.remove(key, consumer);
            consumers.notifyAll();
        }
    }

    KameletConsumer getConsumer() throws InterruptedException {
        synchronized (consumers) {
            KameletConsumer answer = consumers.get(key);
            if (answer == null && block) {
                StopWatch watch = new StopWatch();
                for (; ; ) {
                    answer =consumers.get(key);
                    if (answer != null) {
                        break;
                    }
                    long rem = timeout - watch.taken();
                    if (rem <= 0) {
                        break;
                    }
                    consumers.wait(rem);
                }
            }

            return answer;
        }
    }
}