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

import java.util.Arrays;
import java.util.List;

import org.apache.camel.model.ProcessorDefinition;

public class StepParserException extends RuntimeException {
    private final String processor;
    private final List<String> properties;

    public StepParserException(String message, ProcessorDefinition<?> processor, String... properties) {
        super(message);

        this.processor = processor.getShortName();
        this.properties = Arrays.asList(properties);
    }
    public StepParserException(String message, String... properties) {
        super(message);

        this.processor = null;
        this.properties = Arrays.asList(properties);
    }

    public List<String> getProperties() {
        return properties;
    }
}
