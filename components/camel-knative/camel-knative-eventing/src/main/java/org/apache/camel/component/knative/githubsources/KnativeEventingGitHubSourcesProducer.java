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

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.knative.AbstractKnativeEndpoint;
import org.apache.camel.component.knative.KnativeConstants;
import org.apache.camel.component.knative.KnativeOperations;
import org.apache.camel.support.DefaultProducer;
import org.apache.camel.support.MessageHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class KnativeEventingGitHubSourcesProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(KnativeEventingGitHubSourcesProducer.class);

    private static CustomResourceDefinitionContext gitHubSourceContext;

    private String gitHubSourceTemplate = "{" +

    "\"apiVersion\": \"sources.knative.dev/v1alpha1\"," +
    "\"kind\": \"GitHubSource\"," +
    "\"metadata\": {" +
    "   \"name\": \"%s\"}," +
    "\"spec\": {" +
    "   \"eventTypes\": [%s]," +
    "   \"ownerAndRepository\": \"%s\"," +
    "   \"accessToken\": {" +
    "       \"secretKeyRef\": {" +
    "           \"name\": \"%s\"," +
    "           \"key\": \"%s\"}}," +
    "   \"secretToken\": {" +
    "       \"secretKeyRef\": {" +
    "           \"name\": \"%s\"," +
    "           \"key\": \"%s\"}}}," +
    "\"githubAPIURL\": \"%s\"," +
    "\"sink\": {" +
    "    \"ref\": {" +
    "       \"apiVersion\": \"%s\"," +
    "       \"kind\": \"%s\"," +
    "       \"name\": \"%s\"}}" +
    "}";

    public KnativeEventingGitHubSourcesProducer(AbstractKnativeEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public AbstractKnativeEndpoint getEndpoint() {
        return (AbstractKnativeEndpoint)super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String operation;
        if (ObjectHelper.isEmpty(getEndpoint().getKnativeConfiguration().getOperation())) {
            operation = exchange.getIn().getHeader(KnativeConstants.OPERATION, String.class);
        } else {
            operation = getEndpoint().getKnativeConfiguration().getOperation();
        }

        switch (operation) {

            case KnativeOperations.LIST_GITHUBSOURCES:
                doList(exchange, operation);
                break;

            case KnativeOperations.GET_GITHUBSOURCE:
                doGetGitHubSource(exchange, operation);
                break;

            case KnativeOperations.DELETE_GITHUBSOURCE:
                doDeleteGitHubSource(exchange, operation);
                break;

            case KnativeOperations.CREATE_GITHUBSOURCE:
                doCreateGitHubSource(exchange, operation);
                break;

            default:
                throw new IllegalArgumentException("Unsupported operation " + operation);
        }
    }

    protected void doList(Exchange exchange, String operation) throws Exception {
        String namespaceName = exchange.getIn().getHeader(KnativeConstants.NAMESPACE_NAME, String.class);
        JsonObject gitHubSourcesListJSON = new JsonObject(getEndpoint().getKubernetesClient().customResource(getGitHubSourceContext()).list(namespaceName));
        LOG.info(gitHubSourcesListJSON.toString());
        JsonArray gitHubSourcesListItems = new JsonArray(gitHubSourcesListJSON.getCollection("items"));

        MessageHelper.copyHeaders(exchange.getIn(), exchange.getOut(), true);
        exchange.getOut().setBody(gitHubSourcesListItems);
    }

    protected void doGetGitHubSource(Exchange exchange, String operation) throws Exception {
        String gitHubSourceName = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_NAME, String.class);
        String namespaceName = exchange.getIn().getHeader(KnativeConstants.NAMESPACE_NAME, String.class);
        if (ObjectHelper.isEmpty(gitHubSourceName)) {
            LOG.error("Get a specific Deployment require specify a Deployment name");
            throw new IllegalArgumentException("Get a specific Deployment require specify a Deployment name");
        }
        JsonObject gitHubSourceJSON = new JsonObject();
        try {
            gitHubSourceJSON = new JsonObject(getEndpoint().getKubernetesClient().customResource(getGitHubSourceContext()).get(namespaceName, gitHubSourceName));
        } catch(KubernetesClientException e) {
            if (e.getCode() == 404) {
                LOG.info("GitHubSource instance not found", e);
            } else {
                throw e;
            }
        }
        LOG.info(gitHubSourceJSON.toString());

        MessageHelper.copyHeaders(exchange.getIn(), exchange.getOut(), true);
        exchange.getOut().setBody(gitHubSourceJSON);
    }

    protected void doDeleteGitHubSource(Exchange exchange, String operation) throws Exception {
        String gitHubSourceName = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_NAME, String.class);
        String namespaceName = exchange.getIn().getHeader(KnativeConstants.NAMESPACE_NAME, String.class);
        if (ObjectHelper.isEmpty(gitHubSourceName)) {
            LOG.error("Delete a specific deployment require specify a deployment name");
            throw new IllegalArgumentException("Delete a specific deployment require specify a deployment name");
        }
        if (ObjectHelper.isEmpty(namespaceName)) {
            LOG.error("Delete a specific deployment require specify a namespace name");
            throw new IllegalArgumentException("Delete a specific deployment require specify a namespace name");
        }

        JsonObject gitHubSourceJSON = new JsonObject();
        try {
            gitHubSourceJSON = new JsonObject(getEndpoint().getKubernetesClient().customResource(getGitHubSourceContext()).delete(namespaceName, gitHubSourceName));
        } catch(KubernetesClientException e) {
            if (e.getCode() == 404) {
                LOG.info("GitHubSource instance not found", e);
            } else {
                throw e;
            }
        }

        MessageHelper.copyHeaders(exchange.getIn(), exchange.getOut(), true);
        exchange.getOut().setBody(gitHubSourceJSON);
    }

    protected void doCreateGitHubSource(Exchange exchange, String operation) throws Exception {
        String gitHubSourceName = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_NAME, String.class);
        String namespaceName = exchange.getIn().getHeader(KnativeConstants.NAMESPACE_NAME, String.class);
        String eventType = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_EVENT_TYPES, String.class);
        String ownerRepo = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_OWNER_REPO, String.class);
        String secret = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_SECRET, String.class);
        String accessTokenKey = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_ACCESS_TOKEN_KEY, String.class);
        String secretTokenKey = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_SECRET_TOKEN_KEY, String.class);
        String apiUrl = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_API_URL, String.class);
        String sinkApiVersion = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_SINK_API_VERSION, String.class);
        String sinkKind = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_SINK_KIND, String.class);
        String sinkName = exchange.getIn().getHeader(KnativeConstants.GITHUBSOURCE_SINK_NAME, String.class);
        if (ObjectHelper.isEmpty(gitHubSourceName)) {
            LOG.error("Create a specific Deployment require specify a Deployment name");
            throw new IllegalArgumentException("Create a specific Deployment require specify a pod name");
        }
        if (ObjectHelper.isEmpty(namespaceName)) {
            LOG.error("Create a specific Deployment require specify a namespace name");
            throw new IllegalArgumentException("Create a specific Deployment require specify a namespace name");
        }
        if (ObjectHelper.isEmpty(eventType) || 
            ObjectHelper.isEmpty(ownerRepo) || 
            ObjectHelper.isEmpty(secret) ||
            ObjectHelper.isEmpty(accessTokenKey) ||
            ObjectHelper.isEmpty(secret) ||
            ObjectHelper.isEmpty(secretTokenKey)) {
            LOG.error("One or more parameters are missing to create a GitHubSource.");
            throw new IllegalArgumentException("One or more parameters are missing to create a GitHubSource");
        }
        if (ObjectHelper.isEmpty(apiUrl) ||
            ObjectHelper.isEmpty(sinkApiVersion) ||
            ObjectHelper.isEmpty(sinkKind) ||
            ObjectHelper.isEmpty(sinkName)) {
            LOG.error("One or more parameters are missing to create a GitHubSource.");
            throw new IllegalArgumentException("One or more parameters are missing to create a GitHubSource");
        }
        JsonObject gitHubSourceJSON = new JsonObject();
        try {
            gitHubSourceJSON = new JsonObject(getEndpoint().getKubernetesClient().customResource(getGitHubSourceContext()).create(namespaceName,
                String.format(gitHubSourceTemplate,
                              gitHubSourceName,
                              "\"".concat(eventType.replaceAll(" ","").replaceAll(",","\",\"")).concat("\""),
                              ownerRepo,
                              secret,
                              accessTokenKey,
                              secret,
                              secretTokenKey,
                              apiUrl,
                              sinkApiVersion,
                              sinkKind,
                              sinkName)));
        } catch(KubernetesClientException e) {
            if (e.getCode() == 409) {
                LOG.info("GitHubSource instance already exists", e);
            } else {
                throw e;
            }
        }
        MessageHelper.copyHeaders(exchange.getIn(), exchange.getOut(), true);
        exchange.getOut().setBody(gitHubSourceJSON);
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
