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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import io.undertow.util.MimeMappings;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.TypeConverter;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.util.ObjectHelper;

public class KnativeHttpConsumer extends DefaultConsumer implements HttpHandler {
    private final KnativeHttpBinding binding;

    public KnativeHttpConsumer(KnativeHttpEndpoint endpoint, Processor processor) {
        super(endpoint, processor);

        this.binding = new KnativeHttpBinding(endpoint.getHeaderFilterStrategy());
    }

    @Override
    public KnativeHttpEndpoint getEndpoint() {
        return (KnativeHttpEndpoint) super.getEndpoint();
    }

    @Override
    protected void doStart() throws Exception {
        final KnativeHttpEndpoint endpoint = getEndpoint();
        final KnativeHttpComponent component = endpoint.getComponent();
        final KnativeHttp.HostKey key = endpoint.getHostKey();

        component.bind(key, this, Predicates.and(
            Predicates.path(endpoint.getPath()),
            value -> {
                if (ObjectHelper.isEmpty(endpoint.getHeaderFilter())) {
                    return true;
                }

                HeaderMap hm = value.getRequestHeaders();

                for (Map.Entry<String, Object> entry: endpoint.getHeaderFilter().entrySet()) {
                    String ref = entry.getValue().toString();
                    String val = hm.getFirst(entry.getKey());
                    boolean matches = Objects.equals(ref, val) || val.matches(ref);

                    if (!matches) {
                        return false;
                    }
                }

                return true;
            }
        ));

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        final KnativeHttpEndpoint endpoint = getEndpoint();
        final KnativeHttpComponent component = endpoint.getComponent();
        final KnativeHttp.HostKey key = endpoint.getHostKey();

        component.unbind(key, this);

        super.doStop();
    }

    @Override
    public void handleRequest(HttpServerExchange httpExchange) throws Exception {
        //create new Exchange
        //binding is used to extract header and payload(if available)
        Exchange camelExchange = createExchange(httpExchange);

        //Unit of Work to process the Exchange
        createUoW(camelExchange);
        try {
            getProcessor().process(camelExchange);
        } catch (Exception e) {
            getExceptionHandler().handleException(e);
        } finally {
            doneUoW(camelExchange);
        }

        Object body = binding.toHttpResponse(httpExchange, camelExchange.getMessage());
        TypeConverter tc = getEndpoint().getCamelContext().getTypeConverter();

        if (body == null) {
            httpExchange.getResponseHeaders().put(new HttpString(Exchange.CONTENT_TYPE), MimeMappings.DEFAULT_MIME_MAPPINGS.get("txt"));
            httpExchange.getResponseSender().send("No response available");
        } else {
            ByteBuffer bodyAsByteBuffer = tc.mandatoryConvertTo(ByteBuffer.class, body);
            httpExchange.getResponseSender().send(bodyAsByteBuffer);
        }
    }

    public Exchange createExchange(HttpServerExchange httpExchange) throws Exception {
        Exchange exchange = getEndpoint().createExchange(ExchangePattern.InOut);
        Message in = binding.toCamelMessage(httpExchange, exchange);

        exchange.setProperty(Exchange.CHARSET_NAME, httpExchange.getRequestCharset());
        in.setHeader(Exchange.HTTP_CHARACTER_ENCODING, httpExchange.getRequestCharset());

        exchange.setIn(in);
        return exchange;
    }
}
