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

import java.util.concurrent.ExecutorService;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.knative.AbstractKnativeEndpoint;
import org.apache.camel.component.knative.KnativeConstants;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnativeEventingGitHubSourcesConsumer extends DefaultConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KnativeEventingGitHubSourcesConsumer.class);
    private static CustomResourceDefinitionContext gitHubSourceContext;

    private final Processor processor;
    private ExecutorService executor;
    private GitHubSourcesConsumerTask gitHubSourcesWatcher;

    public KnativeEventingGitHubSourcesConsumer(AbstractKnativeEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.processor = processor;
    }

    @Override
    public AbstractKnativeEndpoint getEndpoint() {
        return (AbstractKnativeEndpoint)super.getEndpoint();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        executor = getEndpoint().createExecutor();

        gitHubSourcesWatcher = new GitHubSourcesConsumerTask();
        executor.submit(gitHubSourcesWatcher);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        LOG.debug("Stopping Knative GitHubSources Consumer");
        if (executor != null) {
            if (getEndpoint() != null && getEndpoint().getCamelContext() != null) {
                if (gitHubSourcesWatcher != null) {
                    gitHubSourcesWatcher.getWatch().close();
                }
                getEndpoint().getCamelContext().getExecutorServiceManager().shutdownNow(executor);
            } else {
                if (gitHubSourcesWatcher != null) {
                    gitHubSourcesWatcher.getWatch().close();
                }
                executor.shutdownNow();
            }
        }
        executor = null;
    }

    class GitHubSourcesConsumerTask implements Runnable {

        private Watch watch;

        @Override
        public void run() {
            RawCustomResourceOperationsImpl w = getEndpoint().getKubernetesClient().customResource(getGitHubSourceContext());
            if (ObjectHelper.isNotEmpty(getEndpoint().getKnativeConfiguration().getNamespace())) {
                LOG.error("namespace is not specified.");
            }
            String namespace = getEndpoint().getKnativeConfiguration().getNamespace();
            try {
                w.watch(namespace, new Watcher<String>() {

                @Override
                public void eventReceived(Action action, String resource) {
                    Exchange exchange = getEndpoint().createExchange();
                    exchange.getIn().setBody(resource);
                    exchange.getIn().setHeader(KnativeConstants.EVENT_ACTION, action);
                    exchange.getIn().setHeader(KnativeConstants.EVENT_TIMESTAMP, System.currentTimeMillis());
                    try {
                        processor.process(exchange);
                    } catch (Exception e) {
                        getExceptionHandler().handleException("Error during processing", exchange, e);
                    }
                }

                @Override
                public void onClose(KubernetesClientException cause) {
                    if (cause != null) {
                        LOG.error(cause.getMessage(), cause);
                    }

                }
            });
            } catch (Exception e){
                LOG.error("Exception in handling githubsource instance change", e);
            }
        }

        public Watch getWatch() {
            return watch;
        }

        public void setWatch(Watch watch) {
            this.watch = watch;
        }
    }
    private CustomResourceDefinitionContext getGitHubSourceContext(){
        if (gitHubSourceContext == null) {
        gitHubSourceContext = new CustomResourceDefinitionContext.Builder()
            .withName("githubsources.sources.knative.dev")
            .withGroup("sources.knative.dev")
            .withScope("Namespaced")
            .withVersion("v1alpha1")
            .withPlural("githubsources")
            .build();
        }
        return gitHubSourceContext;
    }
}
