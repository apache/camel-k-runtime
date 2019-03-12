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
package org.apache.camel.k.support;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.adapter.Resources;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PlatformStreamHandlerTest {
    static {
        PlatformStreamHandler.configure();
    }

    @Test
    public void testClasspathHandler() throws Exception {
        CamelContext context = new DefaultCamelContext();

        try (InputStream is = Resources.resolveResourceAsInputStream(context, "platform:my-cp-resource.txt")) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            assertThat(content).isEqualTo("my cp content");
        }

        try (InputStream is = Resources.resolveResourceAsInputStream(context, "platform:classpath:my-cp-resource.txt")) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            assertThat(content).isEqualTo("my cp content");
        }
    }

    @Test
    public void testFileHandler() throws Exception {
        CamelContext context = new DefaultCamelContext();
        String root = System.getProperty("root") + "/src/test/resources";

        try (InputStream is = Resources.resolveResourceAsInputStream(context, "platform:" + root + "/my-file-resource.txt")) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            assertThat(content).isEqualTo("my file content");
        }
        try (InputStream is = Resources.resolveResourceAsInputStream(context, "platform:file:" + root + "/my-file-resource.txt")) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            assertThat(content).isEqualTo("my file content");
        }
    }

    @Test
    public void testEnvHandler() throws Exception {
        CamelContext context = new DefaultCamelContext();

        try (InputStream is = Resources.resolveResourceAsInputStream(context, "platform:my-env-resource.txt")) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            assertThat(content).isEqualTo("my env content");
        }
        try (InputStream is = Resources.resolveResourceAsInputStream(context, "platform:env:my-env-resource.txt")) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            assertThat(content).isEqualTo("my env content");
        }
        try (InputStream is = Resources.resolveResourceAsInputStream(context, "platform:env:my-compressed-env-resource.txt")) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            assertThat(content.trim()).isEqualTo("my compressed env content");
        }
    }
}
