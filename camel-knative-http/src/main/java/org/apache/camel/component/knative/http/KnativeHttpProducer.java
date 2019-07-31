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

import java.net.URI;
import java.nio.ByteBuffer;

import io.undertow.client.ClientRequest;
import io.undertow.client.UndertowClient;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverter;
import org.apache.camel.support.DefaultAsyncProducer;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.ssl.XnioSsl;

public class KnativeHttpProducer extends DefaultAsyncProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeHttpProducer.class);

    private final OptionMap options;
    private final KnativeHttpBinding binding;

    private UndertowClient client;
    private DefaultByteBufferPool pool;
    private XnioSsl ssl;
    private XnioWorker worker;

    public KnativeHttpProducer(KnativeHttpEndpoint endpoint, OptionMap options) {
        super(endpoint);
        this.options = options;
        this.binding = new KnativeHttpBinding(endpoint.getHeaderFilterStrategy());
    }

    @Override
    public KnativeHttpEndpoint getEndpoint() {
        return (KnativeHttpEndpoint)super.getEndpoint();
    }

    @Override
    public boolean process(final Exchange camelExchange, final AsyncCallback callback) {
        final KnativeHttpEndpoint endpoint = getEndpoint();
        final URI uri = endpoint.getHttpURI();
        final String pathAndQuery = URISupport.pathAndQueryOf(uri);

        final ClientRequest request = new ClientRequest();
        request.setMethod(Methods.POST);
        request.setPath(pathAndQuery);
        request.getRequestHeaders().put(Headers.HOST, uri.getHost());

        final Object body = binding.toHttpRequest(request, camelExchange.getIn());
        final TypeConverter tc = endpoint.getCamelContext().getTypeConverter();
        final ByteBuffer bodyAsByte = tc.tryConvertTo(ByteBuffer.class, body);

        // As tryConvertTo is used to convert the body, we should do null check
        // or the call bodyAsByte.remaining() may throw an NPE
        if (body != null && bodyAsByte != null) {
            request.getRequestHeaders().put(Headers.CONTENT_LENGTH, bodyAsByte.remaining());
        }

        // when connect succeeds or fails UndertowClientCallback will
        // get notified on a I/O thread run by Xnio worker. The writing
        // of request and reading of response is performed also in the
        // callback
        client.connect(
            new KnativeHttpClientCallback(camelExchange, callback, getEndpoint(), request, bodyAsByte),
            uri,
            worker,
            ssl,
            pool,
            options);

        // the call above will proceed on Xnio I/O thread we will
        // notify the exchange asynchronously when the HTTP exchange
        // ends with success or failure from UndertowClientCallback
        return false;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final Xnio xnio = Xnio.getInstance();

        pool = new DefaultByteBufferPool(true, 17 * 1024);
        worker = xnio.createWorker(options);

        SSLContextParameters sslContext = getEndpoint().getSslContextParameters();
        if (sslContext != null) {
            ssl = new UndertowXnioSsl(xnio, options, sslContext.createSSLContext(getEndpoint().getCamelContext()));
        }

        client = UndertowClient.getInstance();

        LOGGER.debug("Created worker: {} with options: {}", worker, options);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (worker != null && !worker.isShutdown()) {
            LOGGER.debug("Shutting down worker: {}", worker);
            worker.shutdown();
        }
    }
}
