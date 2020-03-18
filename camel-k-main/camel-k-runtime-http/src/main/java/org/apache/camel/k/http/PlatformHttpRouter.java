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
package org.apache.camel.k.http;

import io.vertx.ext.web.Router;
import org.apache.camel.CamelContext;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.support.CamelContextHelper;

public class PlatformHttpRouter {
    public static final String PLATFORM_HTTP_ROUTER_NAME = PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME + "-router";

    private final Router router;

    public PlatformHttpRouter(Router router) {
        this.router = router;
    }

    public Router get() {
        return router;
    }

    // **********************
    //
    // Helpers
    //
    // **********************

    public static PlatformHttpRouter lookup(CamelContext camelContext) {
        return CamelContextHelper.mandatoryLookup(
            camelContext,
            PlatformHttpRouter.PLATFORM_HTTP_ROUTER_NAME,
            PlatformHttpRouter.class
        );
    }
}
