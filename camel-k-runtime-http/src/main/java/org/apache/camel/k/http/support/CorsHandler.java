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
package org.apache.camel.k.http.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.k.http.PlatformHttpServiceConfiguration;
import org.apache.camel.util.ObjectHelper;

public class CorsHandler implements Handler<RoutingContext> {

    private static final Pattern COMMA_SEPARATED_SPLIT_REGEX = Pattern.compile("\\s*,\\s*");

    // This is set in the recorder at runtime.
    // Must be static because the filter is created(deployed) at build time and runtime config is still not available
    final PlatformHttpServiceConfiguration.CorsConfiguration corsConfig;

    public CorsHandler(PlatformHttpServiceConfiguration configuration) {
        this.corsConfig = ObjectHelper.notNull(configuration.getCors(), "config");
    }

    private void processRequestedHeaders(HttpServerResponse response, String allowHeadersValue) {
        if (ObjectHelper.isEmpty(corsConfig.getHeaders())) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowHeadersValue);
        } else {
            List<String> requestedHeaders = new ArrayList<>();
            for (String requestedHeader : COMMA_SEPARATED_SPLIT_REGEX.split(allowHeadersValue)) {
                requestedHeaders.add(requestedHeader.toLowerCase());
            }

            List<String> validRequestedHeaders = new ArrayList<>();
            for (String configHeader : corsConfig.getHeaders()) {
                if (requestedHeaders.contains(configHeader.toLowerCase())) {
                    validRequestedHeaders.add(configHeader);
                }
            }

            if (!validRequestedHeaders.isEmpty()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", validRequestedHeaders));
            }
        }
    }

    private void processMethods(HttpServerResponse response, String allowMethodsValue) {
        if (ObjectHelper.isEmpty(corsConfig.getMethods())) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethodsValue);
        } else {
            List<String> requestedMethods = new ArrayList<>();
            for (String requestedMethod : COMMA_SEPARATED_SPLIT_REGEX.split(allowMethodsValue)) {
                requestedMethods.add(requestedMethod.toLowerCase());
            }

            List<String> validRequestedMethods = new ArrayList<>();
            for (String configMethod : corsConfig.getMethods()) {
                if (requestedMethods.contains(configMethod.toLowerCase())) {
                    validRequestedMethods.add(configMethod);
                }
            }

            if (!validRequestedMethods.isEmpty()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, String.join(",", validRequestedMethods));
            }
        }
    }

    @Override
    public void handle(RoutingContext event) {
        final HttpServerRequest request = event.request();
        final HttpServerResponse response = event.response();
        final String origin = request.getHeader(HttpHeaders.ORIGIN);

        if (origin == null) {
            event.next();
        } else {
            final String requestedMethods = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);

            if (requestedMethods != null) {
                processMethods(response, requestedMethods);
            }

            final String requestedHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

            if (requestedHeaders != null) {
                processRequestedHeaders(response, requestedHeaders);
            }

            boolean allowsOrigin = ObjectHelper.isEmpty(corsConfig.getOrigins()) || corsConfig.getOrigins().contains(origin);

            if (allowsOrigin) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }

            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");


            if (ObjectHelper.isNotEmpty(corsConfig.getExposedHeaders())) {
                response.headers().set(
                    HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                    String.join(",", corsConfig.getExposedHeaders()));
            }

            if (request.method().equals(HttpMethod.OPTIONS)) {
                if ((requestedHeaders != null || requestedMethods != null) && corsConfig.getAccessControlMaxAge() != null) {
                    response.putHeader(
                        HttpHeaders.ACCESS_CONTROL_MAX_AGE,
                        String.valueOf(corsConfig.getAccessControlMaxAge().getSeconds()));
                }
                response.end();
            } else {
                event.next();
            }
        }
    }
}