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
import org.apache.camel.Endpoint;
import org.apache.camel.component.quartz.QuartzEndpoint;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.support.LifecycleStrategySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CronQuartzContextCustomizer implements ContextCustomizer {

    public CronQuartzContextCustomizer() {
    }

    @Override
    public void apply(CamelContext camelContext) {
        camelContext.addLifecycleStrategy(new CronQuartzLifecycleStrategy());
        camelContext.addRoutePolicyFactory(new CronRoutePolicyFactory());
    }

    static class CronQuartzLifecycleStrategy extends LifecycleStrategySupport {

        private static final Logger LOG = LoggerFactory.getLogger(CronQuartzLifecycleStrategy.class);

        @Override
        public void onEndpointAdd(Endpoint endpoint) {
            if (endpoint instanceof QuartzEndpoint) {
                LOG.info("Cron policy is configuring the quartz endpoint to startup immediately");
                QuartzEndpoint qe = (QuartzEndpoint) endpoint;
                qe.setCron(null);
                qe.setFireNow(true);
                qe.setAutoStartScheduler(true);
                qe.setCustomCalendar(null);
                qe.setStartDelayedSeconds(0);
            }
        }
    }
}
