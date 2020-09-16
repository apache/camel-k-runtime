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
package org.apache.camel.k.health;

import java.util.Collection;
import java.util.Map;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.component.platform.http.vertx.VertxPlatformHttpRouter;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckFilter;
import org.apache.camel.health.HealthCheckHelper;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.impl.health.AbstractHealthCheck;
import org.apache.camel.impl.health.ContextHealthCheck;
import org.apache.camel.impl.health.RoutesHealthCheckRepository;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.annotation.Customizer;
import org.apache.camel.spi.Configurer;

@Configurer
@Customizer("health")
public class HealthContextCustomizer implements ContextCustomizer {
    public static final String DEFAULT_PATH = "/health";

    private String path;
    private String healthGroupFilterExclude;
    private boolean includeRoutes;
    private boolean includeContext;

    public HealthContextCustomizer() {
        this.path = DEFAULT_PATH;
        this.includeRoutes = true;
        this.includeContext = true;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHealthGroupFilterExclude() {
        return healthGroupFilterExclude;
    }

    public void setHealthGroupFilterExclude(String healthGroupFilterExclude) {
        this.healthGroupFilterExclude = healthGroupFilterExclude;
    }

    public boolean isIncludeRoutes() {
        return includeRoutes;
    }

    public void setIncludeRoutes(boolean includeRoutes) {
        this.includeRoutes = includeRoutes;
    }

    public boolean isIncludeContext() {
        return includeContext;
    }

    public void setIncludeContext(boolean includeContext) {
        this.includeContext = includeContext;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST;
    }

    @Override
    public void apply(CamelContext camelContext) {
        try {
            HealthCheckRegistry reg =  HealthCheckRegistry.get(camelContext);
            if (includeRoutes) {
                reg.register(new RoutesHealthCheckRepository());
            }
            if (includeContext) {
                ContextHealthCheck contextHealthCheck = new ContextHealthCheck();
                contextHealthCheck.getConfiguration().setEnabled(true);

                reg.register(contextHealthCheck);
            }

            camelContext.addService(reg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // add health route
        addRoute(
            camelContext,
            VertxPlatformHttpRouter.lookup(camelContext)
        );
    }

    private Route addRoute(CamelContext camelContext, VertxPlatformHttpRouter router) {
        Route route = router.route(HttpMethod.GET, path);

        // add body handler
        route.handler(router.bodyHandler());

        // add health endpoint handler
        route.handler(routingContext -> {
            int code = 200;

            Collection<HealthCheck.Result> results = HealthCheckHelper.invoke(
                camelContext,
                (HealthCheckFilter) check -> check.getGroup() != null && check.getGroup().equals(getHealthGroupFilterExclude()));

            JsonObject response = new JsonObject();
            response.put("status", "UP");

            JsonArray checks = new JsonArray();

            for (HealthCheck.Result result: results) {
                Map<String, Object> details = result.getDetails();
                boolean enabled = true;

                if (details.containsKey(AbstractHealthCheck.CHECK_ENABLED)) {
                    enabled = (boolean) details.get(AbstractHealthCheck.CHECK_ENABLED);
                }

                if (enabled) {
                    JsonObject check = new JsonObject();
                    check.put("name", result.getCheck().getId());
                    check.put("status", result.getState().name());

                    if (result.getState() == HealthCheck.State.DOWN) {
                        response.put("status", "DOWN");
                        code = 503;
                    }

                    checks.add(check);
                }
            }

            if (!checks.isEmpty()) {
                response.put("checks", checks);
            }

            routingContext.response()
                .putHeader("content-type", "application/json")
                .setStatusCode(code)
                .end(Json.encodePrettily(response));
        });

        return route;
    }
}
