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

import java.util.List;
import java.util.Optional;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.util.ObjectHelper;

public interface SourceLoader {
    /**
     * Provides a list of the languages supported by this loader.
     *
     * @return the supported languages.
     */
    List<String> getSupportedLanguages();

    /**
     * Creates a camel {@link RoutesBuilder} from the given resource.
     *
     * @param runtime the runtime.
     * @param source the source to load.
     * @return the RoutesBuilder.
     * @throws Exception
     */
    Result load(Runtime runtime, Source source) throws Exception;

    /**
     * Represent the result of the process of loading a {@link Source}.
     */
    interface Result {
        /**
         * A {@RoutesBuilder} containing routes to be added to the {@link org.apache.camel.CamelContext}.
         */
        Optional<RoutesBuilder> builder();

        /**
         * A configuration class that can be used to customize the {@link org.apache.camel.CamelContext}.
         */
        Optional<Object> configuration();

        /**
         * Construct an instance of {@link Result} for the given {@link RoutesBuilder}.
         */
        static Result on(RoutesBuilder target) {
            ObjectHelper.notNull(target, "target");

            return new Result() {
                @Override
                public Optional<RoutesBuilder> builder() {
                    return Optional.of(target);
                }

                @Override
                public Optional<Object> configuration() {
                    return Optional.empty();
                }
            };
        }

        /**
         * Construct an instance of {@link Result} by determining the type of hte given target object..
         */
        static Result on(Object target) {
            ObjectHelper.notNull(target, "target");

            return new Result() {
                @Override
                public Optional<RoutesBuilder> builder() {
                    return target instanceof RoutesBuilder
                        ? Optional.of((RoutesBuilder)target)
                        : Optional.empty();
                }

                @Override
                public Optional<Object> configuration() {
                    return target instanceof RoutesBuilder
                        ? Optional.empty()
                        : Optional.of(target);
                }
            };
        }
    }

    interface Interceptor {
        /**
         * Invoked before the source is materialized top a RoutesBuilder.
         */
        void beforeLoad(SourceLoader loader, Source source);

        /**
         * Invoked after the source is materialized and before is added to the runtime.
         */
        Result afterLoad(SourceLoader loader, Source source, Result result);
    }
}
