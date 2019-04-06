/**
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
package org.apache.camel.k.adapter;


import java.util.Collections;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

public class Main extends org.apache.camel.main.MainSupport {
    private CamelContext context;

    public Main(CamelContext context) {
        this.context = context;
    }

    @Override
    protected ProducerTemplate findOrCreateCamelTemplate() {
        return context.createProducerTemplate();
    }

    @Override
    protected Map<String, CamelContext> getCamelContextMap() {
        return Collections.singletonMap(context.getName(), context);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        postProcessContext();

        try {
            context.start();
        } finally {
            if (context.isVetoStarted()) {
                completed();
            }
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (!getCamelContexts().isEmpty()) {
            context.stop();
        }
    }
}