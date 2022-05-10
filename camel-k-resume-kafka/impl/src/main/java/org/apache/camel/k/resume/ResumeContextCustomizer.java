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

package org.apache.camel.k.resume;

import org.apache.camel.CamelContext;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.annotation.Customizer;
import org.apache.camel.k.resume.kafka.KafkaResumeFactory;
import org.apache.camel.resume.ResumeStrategy;
import org.apache.camel.resume.cache.ResumeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Customizer("resume")
public class ResumeContextCustomizer implements ContextCustomizer {
    private static final Logger LOG = LoggerFactory.getLogger(ResumeContextCustomizer.class);
    private String resumeStrategy;
    private String resumeServer;
    private String resumePath;
    private String cacheFillPolicy;


    @Override
    public void apply(CamelContext camelContext) {
        LOG.debug("Receiving context for customization");
        LOG.debug("Resume strategy: {}", resumeStrategy);
        LOG.debug("Resume server: {}", resumeServer);
        LOG.debug("Resume path: {}", resumePath);
        LOG.debug("Cache fill policy: {}", cacheFillPolicy);

        ResumeCache<?> resumeCache = (ResumeCache<?>) camelContext.getRegistry().lookupByName("cache");
        LOG.debug("Values from the registry (cache): {}", resumeCache);

        try {
            ResumeStrategy resumeStrategyInstance = KafkaResumeFactory.build(resumeStrategy, resumeServer, resumePath, cacheFillPolicy);

            LOG.debug("Created resume strategy instance: {}", resumeStrategyInstance.getClass());
            camelContext.getRegistry().bind("resumeStrategy", resumeStrategyInstance);
        } catch (Exception e) {
            LOG.error("Exception: {}", e.getMessage(), e);
        }
    }

    public String getResumeStrategy() {
        return resumeStrategy;
    }

    public void setResumeStrategy(String resumeStrategy) {
        this.resumeStrategy = resumeStrategy;
    }

    public String getResumePath() {
        return resumePath;
    }

    public void setResumePath(String resumePath) {
        this.resumePath = resumePath;
    }

    public String getResumeServer() {
        return resumeServer;
    }

    public void setResumeServer(String resumeServer) {
        this.resumeServer = resumeServer;
    }

    public String getCacheFillPolicy() {
        return cacheFillPolicy;
    }

    public void setCacheFillPolicy(String cacheFillPolicy) {
        this.cacheFillPolicy = cacheFillPolicy;
    }
}
