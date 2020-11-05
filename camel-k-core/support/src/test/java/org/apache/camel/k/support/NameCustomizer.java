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
package org.apache.camel.k.support;

import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.impl.engine.ExplicitCamelContextNameStrategy;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.model.ModelCamelContext;

public final class NameCustomizer implements ContextCustomizer {
    private String name;

    public NameCustomizer() {
        this("default");
    }

    public NameCustomizer(String name) {
        this.name = name;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST;
    }

    @Override
    public void apply(CamelContext camelContexty) {
        camelContexty.adapt(ModelCamelContext.class).setNameStrategy(new ExplicitCamelContextNameStrategy(name));
    }
}
