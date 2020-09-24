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
package org.apache.camel.component.wrap;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.URISupport;
import org.apache.camel.util.UnsafeUriCharactersEncoder;

@Component(Wrap.SCHEME)
public class WrapComponent extends DefaultComponent {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    public WrapComponent() {
        this(null);
    }

    public WrapComponent(CamelContext context) {
        super(context);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        final String query;

        int idx = uri.indexOf('?');
        if (idx > -1) {
            query = uri.substring(idx + 1);
        } else {
            final String encoded = UnsafeUriCharactersEncoder.encode(uri);
            final URI u = new URI(encoded);

            query = u.getRawQuery();
        }

        final Map<String, Object> queryParams = URISupport.parseQuery(query, true);
        final String scheme = StringHelper.before(remaining, ":");
        final String path =  StringHelper.after(remaining, ":");
        final String newScheme = scheme + "-" + COUNTER.getAndIncrement();
        final org.apache.camel.Component newComponent = Wrap.newComponentInstance(getCamelContext(), scheme);

        for (Map.Entry<String, Object> entry: parameters.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            boolean bound = PropertyBindingSupport.build()
                .withConfigurer(newComponent.getComponentPropertyConfigurer())
                .withReference(true)
                .withRemoveParameters(true)
                .bind(getCamelContext(), newComponent, key, val);

            if (bound) {
                queryParams.remove(key);
            }
        }

        getCamelContext().addComponent(newScheme, newComponent);

        return new WrapEndpoint(
            uri,
            this,
            URISupport.appendParametersToURI(newScheme + ":" + path, queryParams));
    }

    @Override
    public boolean useRawUri() {
        return true;
    }
}
