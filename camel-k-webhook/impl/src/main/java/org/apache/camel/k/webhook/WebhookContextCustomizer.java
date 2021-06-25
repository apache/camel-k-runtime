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
package org.apache.camel.k.webhook;

import org.apache.camel.CamelContext;
import org.apache.camel.component.webhook.WebhookAction;
import org.apache.camel.component.webhook.WebhookRoutePolicyFactory;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.annotation.Customizer;
import org.apache.camel.spi.Configurer;

@Configurer
@Customizer("webhook")
public class WebhookContextCustomizer implements ContextCustomizer {
    private WebhookAction action;

    public WebhookAction getAction() {
        return action;
    }

    public void setAction(WebhookAction action) {
        this.action = action;
    }

    @Override
    public void apply(CamelContext camelContext) {
        if (action != null) {
            camelContext.addRoutePolicyFactory(new WebhookRoutePolicyFactory(action));
        }
    }
}
