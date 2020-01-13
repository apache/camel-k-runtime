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
package org.apache.camel.k.cron;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.Route;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.k.Runtime;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.support.RoutePolicySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RoutePolicyFactory that shuts the context down after the first exchange has been done.
 */
public class CronRoutePolicyFactory implements RoutePolicyFactory {

    private Runtime runtime;

    public CronRoutePolicyFactory(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode route) {
        return new CronRoutePolicy(camelContext);
    }

    private class CronRoutePolicy extends RoutePolicySupport {

        private final Logger logger = LoggerFactory.getLogger(CronRoutePolicy.class);

        private final CamelContext context;

        public CronRoutePolicy(CamelContext context) {
            this.context = context;
        }

        @Override
        public void onExchangeDone(Route route, Exchange exchange) {
            logger.info("Context shutdown started by cron policy");
            context.getExecutorServiceManager().newThread("terminator", this::stopRuntime).start();
        }

        private void stopRuntime() {
            try {
                runtime.stop();
            } catch (Exception e) {
                throw new RuntimeCamelException(e);
            }
        }

    }
}
