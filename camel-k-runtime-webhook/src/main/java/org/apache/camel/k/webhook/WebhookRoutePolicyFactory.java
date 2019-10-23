package org.apache.camel.k.webhook;

import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.Route;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.webhook.WebhookCapableEndpoint;
import org.apache.camel.component.webhook.WebhookEndpoint;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.support.RoutePolicySupport;

/**
 * A RoutePolicyFactory that does not start any route but only registers/unregisters the webhook endpoints when enabled.
 */
public class WebhookRoutePolicyFactory implements RoutePolicyFactory {

    private final WebhookAction action;

    public WebhookRoutePolicyFactory(WebhookAction action) {
        this.action = action;
    }

    @Override
    public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode route) {
        if (action != null) {
            return new WebhookRoutePolicy(camelContext, action);
        }
        return null;
    }

    private class WebhookRoutePolicy extends RoutePolicySupport {

        private final CamelContext context;

        private final WebhookAction action;

        public WebhookRoutePolicy(CamelContext context, WebhookAction action) {
            this.context = context;
            this.action = action;
        }

        @Override
        public void onInit(Route route) {
            super.onInit(route);
            route.getRouteContext().setAutoStartup(false);

            if (route.getEndpoint() instanceof WebhookEndpoint) {
                WebhookEndpoint webhook = (WebhookEndpoint) route.getEndpoint();
                if (webhook.getConfiguration() != null && webhook.getConfiguration().isWebhookAutoRegister()) {
                    throw new IllegalStateException("Webhook auto-register is enabled on endpoint " + webhook + ": it must be disabled when the WebhookRoutePolicy is active");
                }
                executeWebhookAction(webhook.getEndpoint());
            }
        }

        @Override
        protected void doStart() throws Exception {
            super.doStart();
            context.getExecutorServiceManager().newThread("terminator", context::stop).start();
        }

        private void executeWebhookAction(WebhookCapableEndpoint endpoint) {
            switch (this.action) {
                case REGISTER:
                    try {
                        endpoint.registerWebhook();
                    } catch (Exception ex) {
                        throw new RuntimeCamelException("Unable to register webhook for endpoint " + endpoint, ex);
                    }
                    return;
                case UNREGISTER:
                    try {
                        endpoint.unregisterWebhook();
                    } catch (Exception ex) {
                        throw new RuntimeCamelException("Unable to unregister webhook for endpoint " + endpoint, ex);
                    }
                    return;
                default:
                    throw new UnsupportedOperationException("Unsupported webhook action type: " + this.action);
            }
        }
    }
}
