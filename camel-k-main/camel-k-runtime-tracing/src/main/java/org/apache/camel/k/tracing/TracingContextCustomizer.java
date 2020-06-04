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
package org.apache.camel.k.tracing;

import java.util.Collections;
import java.util.Map;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import org.apache.camel.CamelContext;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.annotation.Customizer;
import org.apache.camel.opentracing.OpenTracingTracer;
import org.apache.camel.util.ObjectHelper;

@Customizer("tracing")
public class TracingContextCustomizer implements ContextCustomizer {
    private String serviceName;
    private Map<String, String> tags;
    private Configuration.ReporterConfiguration reporter = new Configuration.ReporterConfiguration();
    private Configuration.SamplerConfiguration sampler = new Configuration.SamplerConfiguration();

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Configuration.ReporterConfiguration getReporter() {
        return reporter;
    }

    public void setReporter(Configuration.ReporterConfiguration reporter) {
        this.reporter = reporter;
    }

    public Configuration.SamplerConfiguration getSampler() {
        return sampler;
    }

    public void setSampler(Configuration.SamplerConfiguration sampler) {
        this.sampler = sampler;
    }

    @Override
    public void apply(CamelContext camelContext) {
        ObjectHelper.notNull(serviceName, "service-name");

        Tracer tracer = new Configuration(camelContext.getName())
            .withServiceName(serviceName)
            .withTracerTags(tags != null ? tags : Collections.emptyMap())
            .withReporter(reporter)
            .withSampler(sampler)
            .getTracer();

        OpenTracingTracer openTracingTracer = new OpenTracingTracer();
        openTracingTracer.setTracer(tracer);
        openTracingTracer.init(camelContext);
    }
}
