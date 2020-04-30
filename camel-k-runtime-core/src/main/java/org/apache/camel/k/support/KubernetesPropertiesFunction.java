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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.util.StringHelper;

public class KubernetesPropertiesFunction implements PropertiesFunction {
    private final String name;
    private final Path root;

    public KubernetesPropertiesFunction(String path, String name) {
        this.root = path != null ? Paths.get(path) : null;
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String apply(String remainder) {
        if (this.root == null) {
            return remainder;
        }

        final String name = StringHelper.before(remainder, "/");
        final String property = StringHelper.after(remainder, "/");

        if (name == null || property == null) {
            return remainder;
        }

        Path file = this.root.resolve(name.toLowerCase()).resolve(property);
        if (Files.exists(file) && !Files.isDirectory(file)) {
            try {
                return Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return remainder;
    }
}
