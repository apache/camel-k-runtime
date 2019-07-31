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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.http.common.HttpHelper;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.support.ExchangeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelExceptionHandler;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;


class KnativeHttpClientCallback implements ClientCallback<ClientConnection> {
    private static final Logger LOG = LoggerFactory.getLogger(KnativeHttpClientCallback.class);

    private final ByteBuffer body;
    private final AsyncCallback callback;
    private final BlockingDeque<Closeable> closeables;
    private final KnativeHttpEndpoint endpoint;
    private final Exchange exchange;
    private final ClientRequest request;

    KnativeHttpClientCallback(Exchange exchange, AsyncCallback callback, KnativeHttpEndpoint endpoint, ClientRequest request, ByteBuffer body) {
        this.closeables = new LinkedBlockingDeque<>();
        this.exchange = exchange;
        this.callback = callback;
        this.endpoint = endpoint;
        this.request = request;
        this.body = body;
    }

    @Override
    public void completed(final ClientConnection connection) {
        // we have established connection, make sure we close it
        deferClose(connection);

        // now we can send the request and perform the exchange: writing the
        // request and reading the response
        connection.sendRequest(request, on(this::performClientExchange));
    }

    @Override
    public void failed(final IOException e) {
        hasFailedWith(e);
    }

    private ChannelListener<StreamSinkChannel> asyncWriter(final ByteBuffer body) {
        return channel -> {
            try {
                write(channel, body);

                if (body.hasRemaining()) {
                    channel.resumeWrites();
                } else {
                    flush(channel);
                }
            } catch (final IOException e) {
                hasFailedWith(e);
            }
        };
    }

    private void deferClose(final Closeable closeable) {
        try {
            closeables.putFirst(closeable);
        } catch (final InterruptedException e) {
            hasFailedWith(e);
        }
    }

    private void finish(final Message result) {
        for (final Closeable closeable : closeables) {
            IoUtils.safeClose(closeable);
        }

        if (result != null) {
            if (ExchangeHelper.isOutCapable(exchange)) {
                exchange.setOut(result);
            } else {
                exchange.setIn(result);
            }
        }

        callback.done(false);
    }

    private void hasFailedWith(final Throwable e) {
        LOG.trace("Exchange has failed with", e);
        if (Boolean.TRUE.equals(endpoint.getThrowExceptionOnFailure())) {
            exchange.setException(e);
        }

        finish(null);
    }

    private <T> ClientCallback<T> on(final Consumer<T> completed) {
        return on(completed, this::hasFailedWith);
    }

    private <T> ClientCallback<T> on(Consumer<T> completed, Consumer<IOException> failed) {
        return new ClientCallback<T>() {
            @Override
            public void completed(final T result) {
                completed.accept(result);
            }

            @Override
            public void failed(final IOException e) {
                failed(e);
            }
        };
    }

    private void performClientExchange(final ClientExchange clientExchange) {
        // add response listener to the exchange, we could receive the response
        // at any time (async)
        setupResponseListener(clientExchange);

        // write the request
        writeRequest(clientExchange, body);
    }

    private void setupResponseListener(final ClientExchange clientExchange) {
        clientExchange.setResponseListener(on(response -> {
            try {
                final KnativeHttpBinding binding = new KnativeHttpBinding(endpoint.getHeaderFilterStrategy());
                final Message result = binding.toCamelMessage(clientExchange, exchange);
                final int code = clientExchange.getResponse().getResponseCode();

                if (!HttpHelper.isStatusCodeOk(code, "200-299") && endpoint.getThrowExceptionOnFailure()) {
                    // operation failed so populate exception to throw
                    final String uri = endpoint.getHttpURI().toString();
                    final String statusText = clientExchange.getResponse().getStatus();

                    // Convert Message headers (Map<String, Object>) to Map<String, String> as expected by
                    // HttpOperationsFailedException using Message versus clientExchange as its header values
                    // have extra formatting
                    final Map<String, String> headers = result.getHeaders().entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, (entry) -> entry.getValue().toString()));

                    // Since result (Message) isn't associated with an Exchange yet, you can not use result.getBody(String.class)
                    final String bodyText = ExchangeHelper.convertToType(exchange, String.class, result.getBody());
                    final Exception cause = new HttpOperationFailedException(uri, code, statusText, null, headers, bodyText);

                    if (ExchangeHelper.isOutCapable(exchange)) {
                        exchange.setOut(result);
                    } else {
                        exchange.setIn(result);
                    }

                    // make sure to fail with HttpOperationFailedException
                    hasFailedWith(cause);
                } else {
                    // we end Camel exchange here
                    finish(result);
                }
            } catch (Throwable e) {
                hasFailedWith(e);
            }
        }));
    }

    private void writeRequest(final ClientExchange clientExchange, final ByteBuffer body) {
        final StreamSinkChannel requestChannel = clientExchange.getRequestChannel();
        if (body != null) {
            try {
                // try writing, we could be on IO thread and ready to write to
                // the socket (or not)
                write(requestChannel, body);

                if (body.hasRemaining()) {
                    // we did not write all of body (or at all) register a write
                    // listener to write asynchronously
                    requestChannel.getWriteSetter().set(asyncWriter(body));
                    requestChannel.resumeWrites();
                } else {
                    // we are done, we need to flush the request
                    flush(requestChannel);
                }
            } catch (final IOException e) {
                hasFailedWith(e);
            }
        }
    }

    private static void flush(final StreamSinkChannel channel) throws IOException {
        // the canonical way of flushing Xnio channels
        channel.shutdownWrites();
        if (!channel.flush()) {
            final ChannelListener<StreamSinkChannel> safeClose = IoUtils::safeClose;
            final ChannelExceptionHandler<Channel> closingChannelExceptionHandler = ChannelListeners
                .closingChannelExceptionHandler();
            final ChannelListener<StreamSinkChannel> flushingChannelListener = ChannelListeners
                .flushingChannelListener(safeClose, closingChannelExceptionHandler);
            channel.getWriteSetter().set(flushingChannelListener);
            channel.resumeWrites();
        }
    }

    private static void write(final StreamSinkChannel channel, final ByteBuffer body) throws IOException {
        int written = 1;
        while (body.hasRemaining() && written > 0) {
            written = channel.write(body);
        }
    }
}
