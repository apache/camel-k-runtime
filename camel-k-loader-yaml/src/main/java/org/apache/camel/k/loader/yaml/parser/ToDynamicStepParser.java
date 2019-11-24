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
package org.apache.camel.k.loader.yaml.parser;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.camel.reifier.ProcessorReifier;
import org.apache.camel.reifier.ToDynamicReifier;
import org.apache.camel.util.URISupport;

@YAMLStepParser("tod")
public class ToDynamicStepParser implements ProcessorStepParser {
    static {
        ProcessorReifier.registerReifier(Definition.class, ToDynamicReifier::new);
    }

    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        return context.node(Definition.class);
    }

    public static final class Definition extends ToDynamicDefinition implements Step.Definition {
        public Map<String, Object> parameters;

        public Definition() {
        }

        public Definition(String uri) {
            super(uri);
        }

        @JsonIgnore
        public String getEndpointUri() {
            String answer = getUri();

            if (parameters != null) {
                try {
                    answer = URISupport.appendParametersToURI(answer, parameters);
                } catch (URISyntaxException | UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            return answer;
        }
    }
}

