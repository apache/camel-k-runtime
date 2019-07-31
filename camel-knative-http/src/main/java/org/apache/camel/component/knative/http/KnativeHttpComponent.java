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
package org.apache.camel.component.knative.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.IntrospectionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("knative-http")
public class KnativeHttpComponent extends DefaultComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeHttpComponent.class);
    private final Map<KnativeHttp.HostKey, KnativeHttpDispatcher> registry;

    @Metadata(label = "advanced")
    private KnativeHttp.HostOptions hostOptions;

    public KnativeHttpComponent() {
        this.registry = new ConcurrentHashMap<>();
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        final Pattern pattern = Pattern.compile("([0-9a-zA-Z][\\w\\.-]+):(\\d+)\\/?(.*)");
        final Matcher matcher = pattern.matcher(remaining);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Bad URI: " + remaining);
        }

        final String host;
        final int port;
        final String path;

        switch (matcher.groupCount()) {
        case 1:
            host = matcher.group(1);
            port = 8080;
            path = "/";
            break;
        case 2:
            host = matcher.group(1);
            port = Integer.parseInt(matcher.group(2));
            path = "/";
            break;
        case 3:
            host = matcher.group(1);
            port = Integer.parseInt(matcher.group(2));
            path = "/" + matcher.group(3);
            break;
        default:
            throw new IllegalArgumentException("Bad URI: " + remaining);
        }

        KnativeHttpEndpoint ep = new KnativeHttpEndpoint(uri, this);
        ep.setHost(host);
        ep.setPort(port);
        ep.setPath(path);
        ep.setHeaderFilter(IntrospectionSupport.extractProperties(parameters, "filter.", true));

        setProperties(ep, parameters);

        return ep;
    }

    public KnativeHttp.HostOptions getHostOptions() {
        return hostOptions;
    }

    public void setHostOptions(KnativeHttp.HostOptions hostOptions) {
        this.hostOptions = hostOptions;
    }

    public void bind(KnativeHttp.HostKey key, HttpHandler handler, Predicate predicate) {
        getUndertow(key).bind(handler, predicate);
    }

    public void unbind(KnativeHttp.HostKey key, HttpHandler handler) {
        getUndertow(key).unbind(handler);

    }

    private KnativeHttpDispatcher getUndertow(KnativeHttp.HostKey key) {
        return registry.computeIfAbsent(key, k -> new KnativeHttpDispatcher(k, hostOptions));
    }
}
