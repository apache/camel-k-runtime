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
package org.apache.camel.k.tooling.maven.processors;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.apache.camel.k.tooling.maven.GenerateYamlLoaderSupportClasses;
import org.apache.camel.k.tooling.maven.GenerateYamlParserSupportClasses;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GenerateYamlSupportClassesTest {
    @Test
    public void testGenerateHasDataFormat() {
        final TypeSpec spec = new GenerateYamlParserSupportClasses().generateHasDataFormat();
        final JavaFile file = JavaFile.builder("org.apache.camel.k.loader.yaml.parser", spec).build();

        assertThat(file.packageName).isEqualTo("org.apache.camel.k.loader.yaml.parser");
        assertThat(spec.name).isEqualTo("HasDataFormat");
        assertThat(spec.methodSpecs).isNotEmpty();
    }

    @Test
    public void testGenerateHasExpression() {
        final TypeSpec spec = new GenerateYamlParserSupportClasses().generateHasExpression();
        final JavaFile file = JavaFile.builder("org.apache.camel.k.loader.yaml.parser", spec).build();

        assertThat(file.packageName).isEqualTo("org.apache.camel.k.loader.yaml.parser");
        assertThat(spec.name).isEqualTo("HasExpression");
        assertThat(spec.methodSpecs).isNotEmpty();
    }

    @Test
    public void testGenerateJacksonModule() {
        final TypeSpec spec = new GenerateYamlLoaderSupportClasses().generateJacksonModule();
        final JavaFile file = JavaFile.builder("org.apache.camel.k.loader.yaml", spec).build();

        assertThat(file.packageName).isEqualTo("org.apache.camel.k.loader.yaml");
        assertThat(spec.name).isEqualTo("YamlModule");
        assertThat(spec.methodSpecs).isNotEmpty();
    }
}
