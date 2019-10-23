package org.apache.camel.k.main;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.webhook.WebhookCapableEndpoint;
import org.apache.camel.component.webhook.WebhookConfiguration;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.support.DefaultEndpoint;

public class DummyWebhookComponent extends DefaultComponent {

    private final Runnable onRegister;

    private final Runnable onUnregister;

    public DummyWebhookComponent(Runnable onRegister, Runnable onUnregister) {
        this.onRegister = onRegister;
        this.onUnregister = onUnregister;
    }



    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        return new DummyWebhookComponent.DummyWebhookEndpoint();
    }

    class DummyWebhookEndpoint extends DefaultEndpoint implements WebhookCapableEndpoint {

        @Override
        protected String createEndpointUri() {
            return "dummy";
        }

        @Override
        public Producer createProducer() throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return new DummyWebhookConsumer(this, processor);
        }

        @Override
        public Processor createWebhookHandler(Processor next) {
            return next;
        }

        @Override
        public void registerWebhook() throws Exception {
            onRegister.run();
        }

        @Override
        public void unregisterWebhook() throws Exception {
            onUnregister.run();
        }

        @Override
        public void setWebhookConfiguration(WebhookConfiguration webhookConfiguration) {

        }

        @Override
        public List<String> getWebhookMethods() {
            return Collections.singletonList("POST");
        }

        class DummyWebhookConsumer extends DefaultConsumer {
            public DummyWebhookConsumer(Endpoint endpoint, Processor processor) {
                super(endpoint, processor);
            }

            @Override
            protected void doStart() throws Exception {
                throw new IllegalStateException("Webhook consumer must never be started");
            }
        }
    }
}
