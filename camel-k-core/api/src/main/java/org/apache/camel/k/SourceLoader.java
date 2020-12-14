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
package org.apache.camel.k;

import java.util.Collection;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;

public interface SourceLoader {
    /**
     * Provides a list of the languages supported by this loader.
     *
     * @return the supported languages.
     */
    Collection<String> getSupportedLanguages();

    /**
     * Creates a camel {@link RoutesBuilder} from the given resource.
     *
     * @param camelContext the runtime.
     * @param source the source to load.
     * @return the RoutesBuilder.
     */
    RoutesBuilder load(CamelContext camelContext, Source source);

    /**
     * Define some entry point to intercept the creation fo routes from a {@link Source}
     */
    interface Interceptor {
        /**
         * Invoked before the source is materialized top a RoutesBuilder.
         */
        default void beforeLoad(SourceLoader loader, Source source) {
        }

        /**
         * Invoked after the source is materialized and before is added to the runtime.
         */
        default RoutesBuilder afterLoad(SourceLoader loader, Source source, RoutesBuilder builder) {
            return builder;
        }
    }
}
