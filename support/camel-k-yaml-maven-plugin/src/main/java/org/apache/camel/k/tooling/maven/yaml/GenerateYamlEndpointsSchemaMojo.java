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
package org.apache.camel.k.tooling.maven.yaml;


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.util.StringHelper;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "generate-yaml-endpoints-schema",
    inheritByDefault = false,
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true,
    requiresProject = false)
public class GenerateYamlEndpointsSchemaMojo extends GenerateYamlSupportMojo {
    @Parameter(property = "camel.k.yaml.schema", defaultValue = "${project.build.directory}/yaml-${project.version}.json")
    private File outputFile;

    private ObjectNode definitions;

    @Override
    public void execute() throws MojoFailureException {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode root = mapper.createObjectNode();

        // Schema
        root.put("$schema", "http://json-schema.org/draft-04/schema#");
        root.put("type", "object");

        // Schema sections
        this.definitions = root.putObject("definitions");

        final CamelCatalog catalog = new DefaultCamelCatalog();
        for (String componentName : catalog.findComponentNames()) {
            ComponentModel component = catalog.componentModel(componentName);
            if (!definitions.has(component.getScheme())) {
                ObjectNode node = definitions.putObject(component.getScheme());
                node.put("type", "object");

                processEndpointOption(node, component.getEndpointPathOptions());
                processEndpointOption(node, component.getEndpointParameterOptions());
            }

            if (component.getAlternativeSchemes() != null) {
                for (String scheme: component.getAlternativeSchemes().split(",")) {
                    if (!definitions.has(scheme)) {
                        definitions.putObject(scheme)
                            .put("type", "object")
                            .put("$ref", "#/definitions/" + component.getScheme());
                    }
                }
            }
        }

        if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
            throw new MojoFailureException("Unable to create directory " + outputFile.getParentFile());
        }

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, root);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void processEndpointOption(ObjectNode root, Collection<ComponentModel.EndpointOptionModel> options) {
        options.stream()
            .sorted(Comparator.comparing(ComponentModel.EndpointOptionModel::getName))
            .forEach(option -> {
                if (option.isRequired()) {
                    root.withArray("required").add(option.getName());
                }

                String name = StringHelper.camelCaseToDash(option.getName());
                ObjectNode node = root.with("properties").putObject(name);

                processEndpointOption(node, option);
            });
    }

    private void processEndpointOption(ObjectNode root, ComponentModel.EndpointOptionModel option) {
        if (option.getDescription() != null) {
            root.put("description", option.getDescription());
        }
        if (option.getDefaultValue() != null) {
            root.put("default", Objects.toString(option.getDefaultValue()));
        }
        if (option.getEnums() != null) {
            option.getEnums().forEach(value -> root.withArray("enum").add(value));
        }

        switch (option.getType()) {
            case "string":
            case "object":
            case "array":
            case "duration":
                root.put("type", "string");
                break;
            case "boolean":
                root.put("type", "boolean");
                break;
            case "integer":
                root.put("type", "integer");
                break;
            case "number":
                root.put("type", "number");
                break;
            default:
                throw new IllegalArgumentException(
                    "Unable to determine type for name: " + option.getName() + ", type: " + option.getType());

        }
    }
}
