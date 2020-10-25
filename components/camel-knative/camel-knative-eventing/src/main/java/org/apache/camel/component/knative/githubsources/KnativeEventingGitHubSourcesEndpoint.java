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
package org.apache.camel.component.knative.githubsources;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.knative.AbstractKnativeEndpoint;
import org.apache.camel.component.knative.KnativeConfiguration;
import org.apache.camel.spi.UriEndpoint;

/**
 * Perform operations on Knative Deployments and get notified on Deployment changes.
 */
@UriEndpoint(firstVersion = "2.20.0", scheme = "knativeeventing-githubsources", title = "KnativeEventing GitHubSources", syntax = "knativeeventing-githubsources:masterUrl", category = {Category.CONTAINER, Category.CLOUD, Category.PAAS})
public class KnativeEventingGitHubSourcesEndpoint extends AbstractKnativeEndpoint {

    public KnativeEventingGitHubSourcesEndpoint(String uri, KnativeEventingGitHubSourcesComponent component, KnativeConfiguration config) {
        super(uri, component, config);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new KnativeEventingGitHubSourcesProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        Consumer consumer = new KnativeEventingGitHubSourcesConsumer(this, processor);
        configureConsumer(consumer);
        return consumer;
    }
}
