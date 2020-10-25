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
package org.apache.camel.component.knative.producer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext.Builder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.knative.KnativeConstants;
import org.apache.camel.component.knative.KnativeTestSupport;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

public class KnativeEventingGitHubSourcesProducerTest extends KnativeTestSupport {
    @Rule
    public KubernetesServer server = new KubernetesServer();

    private String gitHubSourceString = "{\"metadata\":{\"generation\":1,\"finalizers\":[\"githubsources.sources.knative.dev\"],\"uid\":\"7d660fbc-d42a-4acf-9d56-583b19c4025c\","+
        "\"resourceVersion\":\"5579297\",\"creationTimestamp\":\"2020-09-03T13:44:27Z\",\"name\":\"test\",\"namespace\":\"test\",\"selfLink\":\"/apis/sources.knative.dev/v1alpha1/namespaces/katamari/githubsources/test\"},"+
        "\"apiVersion\":\"sources.knative.dev/v1alpha1\",\"githubAPIURL\":\"https://api.github.com/\",\"kind\":\"GitHubSource\",\"sink\":{\"ref\":{\"name\":\"github\","+
        "\"kind\":\"Channel\",\"apiVersion\":\"messaging.knative.dev/v1beta1\"}},\"spec\":{\"accessToken\":{\"secretKeyRef\":{\"name\":\"githubsecret\",\"key\":\"accessToken\"}},"+
        "\"eventTypes\":[\"issues\",\"repository\"],\"ownerAndRepository\":\"akihikokuroda/sample\",\"secretToken\":{\"secretKeyRef\":{\"name\":\"githubsecret\",\"key\":\"secretToken\"}}}}}";
    private CustomResourceDefinitionContext getGitHubSourceContext(){
            return new CustomResourceDefinitionContext.Builder()
            .withName("githubsources.sources.knative.dev")
            .withGroup("sources.knative.dev")
            .withScope("Namespaced")
            .withVersion("v1alpha1")
            .withPlural("githubsources")
            .build();
     }

    @BindToRegistry("kubernetesClient")
    public KubernetesClient getClient() throws Exception {
        return server.getClient();
    }

    @Test
    public void listTest() throws Exception {
        JSONObject instance = new JSONObject(getClient().customResource(getGitHubSourceContext()).load(gitHubSourceString));        
        JSONObject gitHubSourceList = new JSONObject();
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        list.add(instance); 
        gitHubSourceList.put("items", list);

        server.expect().get().withPath("/apis/sources.knative.dev/v1alpha1/namespaces/test/githubsources")
        .andReturn(200, gitHubSourceList.toString()).once();    

        Exchange ex = template.request("direct:listGitHubSources", exchange -> {
            exchange.getIn().setHeader(KnativeConstants.NAMESPACE_NAME, "test");
        });

        List<Map<String, Object>> result = ex.getMessage().getBody(List.class);
    
        //assertEquals(1, result.size());
    }

    @Test
    public void createAndDeleteTest() throws Exception {
        JSONObject instance = new JSONObject(getClient().customResource(getGitHubSourceContext()).load(gitHubSourceString));        
        JSONObject gitHubSourceList = new JSONObject();
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        list.add(instance); 
        gitHubSourceList.put("items", list);

        server.expect().post().withPath("/apis/sources.knative.dev/v1alpha1/namespaces/test/githubsources")
        .andReturn(200, gitHubSourceList.toString()).once();    
        server.expect().get().withPath("/apis/sources.knative.dev/v1alpha1/namespaces/test/githubsources")
        .andReturn(200, gitHubSourceList.toString()).once();    
        server.expect().delete().withPath("/apis/sources.knative.dev/v1alpha1/namespaces/test/githubsources/createtest")
        .andReturn(200, gitHubSourceList.toString()).once();

        Exchange ex = template.request("direct:createGitHubSource", exchange -> {
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_NAME, "createtest");
            exchange.getIn().setHeader(KnativeConstants.NAMESPACE_NAME, "test");
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_EVENT_TYPES, "issues, repository");
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_OWNER_REPO, "akihikokuroda/sample");
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_SECRET, "githubsecret");
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_ACCESS_TOKEN_KEY, "accessToken");
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_SECRET_TOKEN_KEY, "secretToken");
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_API_URL, "https://api.github.com/");
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_SINK_API_VERSION, "messaging.knative.dev/v1beta1");
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_SINK_KIND, "Channel");
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_SINK_NAME, "github");
        });

        Exchange ex2 = template.request("direct:listGitHubSources", exchange -> {
            exchange.getIn().setHeader(KnativeConstants.NAMESPACE_NAME, "test");
        });
        List<Map<String, Object>> result = ex2.getMessage().getBody(List.class);

        Exchange ex3 = template.request("direct:deleteGitHubSource", exchange -> {
            exchange.getIn().setHeader(KnativeConstants.GITHUBSOURCE_NAME, "createtest");
            exchange.getIn().setHeader(KnativeConstants.NAMESPACE_NAME, "test");
        });

        server.expect().get().withPath("/apis/sources.knative.dev/v1alpha1/namespaces/test/githubsources")
        .andReturn(200, "").once();    
        Exchange ex4 = template.request("direct:listGitHubSources", exchange -> {
            exchange.getIn().setHeader(KnativeConstants.NAMESPACE_NAME, "test");
        });

        List<Map<String, Object>> result1 = ex4.getMessage().getBody(List.class);
    
        assertEquals(null, result1);
    }
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:getGitHubSource").toF("knativeeventing-githubsources:///?kubernetesClient=#kubernetesClient&operation=getGitHubSource");
                from("direct:listGitHubSources").toF("knativeeventing-githubsources:///?kubernetesClient=#kubernetesClient&operation=listGitHubSources");
                from("direct:deleteGitHubSource").toF("knativeeventing-githubsources:///?kubernetesClient=#kubernetesClient&operation=deleteGitHubSource");
                from("direct:createGitHubSource").toF("knativeeventing-githubsources:///?kubernetesClient=#kubernetesClient&operation=createGitHubSource");
            }
        };
    }

}
