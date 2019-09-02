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

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.camel.AsyncCallback;
import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Message;
import org.apache.camel.support.DefaultAsyncProducer;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.support.MessageHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnativeHttpProducer extends DefaultAsyncProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeHttpProducer.class);

    private final Vertx vertx;
    private final WebClientOptions clientOptions;
    private WebClient client;

    public KnativeHttpProducer(KnativeHttpEndpoint endpoint, Vertx vertx, WebClientOptions clientOptions) {
        super(endpoint);

        this.vertx = ObjectHelper.notNull(vertx, "vertx");
        this.clientOptions = ObjectHelper.supplyIfEmpty(clientOptions, WebClientOptions::new);
    }

    @Override
    public KnativeHttpEndpoint getEndpoint() {
        return (KnativeHttpEndpoint)super.getEndpoint();
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        final byte[] payload;

        try {
            payload = exchange.getMessage().getMandatoryBody(byte[].class);
        } catch (InvalidPayloadException e) {
            exchange.setException(e);
            callback.done(true);

            return true;
        }

        KnativeHttpEndpoint endpoint = getEndpoint();
        Message message = exchange.getMessage();

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.HOST, endpoint.getHost());
        headers.add(HttpHeaders.CONTENT_LENGTH, Integer.toString(payload.length));

        String contentType = MessageHelper.getContentType(message);
        if (contentType != null) {
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        }

        for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
            if (!endpoint.getHeaderFilterStrategy().applyFilterToCamelHeaders(entry.getKey(), entry.getValue(), exchange)) {
                headers.add(entry.getKey(), entry.getValue().toString());
            }
        }

        client.post(endpoint.getPort(), endpoint.getHost(), endpoint.getPath())
            .putHeaders(headers)
            .sendBuffer(Buffer.buffer(payload), response -> {
                HttpResponse<Buffer> result = response.result();

                Message answer = new DefaultMessage(exchange.getContext());
                answer.setHeader(Exchange.HTTP_RESPONSE_CODE, result.statusCode());

                for (Map.Entry<String, String> entry : result.headers().entries()) {
                    if (!endpoint.getHeaderFilterStrategy().applyFilterToExternalHeaders(entry.getKey(), entry.getValue(), exchange)) {
                        KnativeHttpSupport.appendHeader(message.getHeaders(), entry.getKey(), entry.getValue());
                    }
                }

                exchange.setMessage(answer);

                if (response.succeeded()) {
                    answer.setBody(result.body().getBytes());
                } else if (response.failed() && endpoint.getThrowExceptionOnFailure()) {
                    Exception cause = new CamelException(
                        "HTTP operation failed invoking " + URISupport.sanitizeUri(getURI()) + " with statusCode: " + result.statusCode()
                    );

                    exchange.setException(cause);
                }

                callback.done(false);
            });

        return false;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();

        this.client = WebClient.create(vertx, clientOptions);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (this.client != null) {
            LOGGER.debug("Shutting down client: {}", client);
            this.client.close();
            this.client = null;
        }
    }

    private String getURI() {
        String p = getEndpoint().getPath();

        if (p == null) {
            p = KnativeHttp.DEFAULT_PATH;
        } else if (!p.startsWith("/")) {
            p = "/" + p;
        }

        return String.format("http://%s:%d%s", getEndpoint().getHost(), getEndpoint().getPort(), p);
    }
}
