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
import java.util.Collections;
import java.util.List;

import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.k.Runtime;
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
    public List<String> getSupportedLanguages() {
        return Collections.singletonList("xml");
    }

    @Override
    public Result load(Runtime runtime, Source source) throws Exception {
        RouteBuilder builder = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final ExtendedCamelContext context = getContext().adapt(ExtendedCamelContext.class);
                final XMLRoutesDefinitionLoader loader = context.getXMLRoutesDefinitionLoader();

                try (InputStream is = source.resolveAsInputStream(getContext())) {
                    try {
                        Object result = loader.loadRoutesDefinition(getContext(), is);
                        if (result instanceof RoutesDefinition) {
                            RoutesDefinition definitions = (RoutesDefinition)result;
                            LOGGER.debug("Loaded {} routes from {}", definitions.getRoutes().size(), source);
                            setRouteCollection(definitions);
                        }
                    } catch (IllegalArgumentException e) {
                        // ignore
                    } catch (XmlPullParserException e) {
                        LOGGER.debug("Unable to load RoutesDefinition: {}", e.getMessage());
                    }
                }

                try (InputStream is = source.resolveAsInputStream(getContext())) {
                    try {
                        Object result = loader.loadRestsDefinition(getContext(), is);
                        if (result instanceof RestsDefinition) {
                            RestsDefinition definitions = (RestsDefinition)result;
                            LOGGER.debug("Loaded {} rests from {}", definitions.getRests().size(), source);
                            setRestCollection(definitions);
                        }
                    } catch (IllegalArgumentException e) {
                        // ignore
                    } catch (XmlPullParserException e) {
                        LOGGER.debug("Unable to load RestsDefinition: {}", e.getMessage());
                    }
                }
            }
        };

        return Result.on(builder);
    }
}
