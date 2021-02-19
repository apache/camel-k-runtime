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
package org.apache.camel.k.loader.xml;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.annotation.Loader;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.rest.RestsDefinition;
import org.apache.camel.spi.XMLRoutesDefinitionLoader;
import org.apache.camel.xml.io.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Loader("xml")
public class XmlSourceLoader implements SourceLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(XmlSourceLoader.class);

    @Override
    public Collection<String> getSupportedLanguages() {
        return Collections.singletonList("xml");
    }

    @SuppressWarnings("unchecked")
    @Override
    public RoutesBuilder load(CamelContext camelContext, Source source) {
        final ExtendedCamelContext context = camelContext.adapt(ExtendedCamelContext.class);
        final XMLRoutesDefinitionLoader loader = context.getXMLRoutesDefinitionLoader();

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                try (InputStream is = source.resolveAsInputStream(getContext())) {
                    try {
                        Optional<RoutesDefinition> result = (Optional<RoutesDefinition>)loader.loadRoutesDefinition(getContext(), is);
                        result.ifPresent(this::setRouteCollection);
                    } catch (IllegalArgumentException ignored) {
                        // ignore
                    } catch (XmlPullParserException e) {
                        LOGGER.debug("Unable to load RoutesDefinition: {}", e.getMessage());
                    }
                }

                try (InputStream is = source.resolveAsInputStream(getContext())) {
                    try {
                        Optional<RestsDefinition> result = (Optional<RestsDefinition>)loader.loadRestsDefinition(getContext(), is);
                        result.ifPresent(this::setRestCollection);
                    } catch (IllegalArgumentException ignored) {
                        // ignore
                    } catch (XmlPullParserException e) {
                        LOGGER.debug("Unable to load RestsDefinition: {}", e.getMessage());
                    }
                }

                LOGGER.debug("Loaded {} routes from {}", getRouteCollection().getRoutes().size(), source);
                LOGGER.debug("Loaded {} rests from {}", getRestCollection().getRests().size(), source);
            }
        };
    }
}
