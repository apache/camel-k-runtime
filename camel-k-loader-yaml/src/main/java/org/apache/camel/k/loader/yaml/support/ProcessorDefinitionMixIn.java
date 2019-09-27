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
package org.apache.camel.k.loader.yaml.support;

import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.camel.Exchange;
import org.apache.camel.model.Block;
import org.apache.camel.model.OptionalIdentifiedDefinition;
import org.apache.camel.model.OtherAttributesAware;
import org.apache.camel.model.ProcessorDefinition;

/**
 * ProcessorDefinition declares multiple methods for setBody and Jackson get confused
 * about what method to use so to hide such fields from the deserialization process
 * without having to change the original class, a MixIn is required.
 */
public abstract class ProcessorDefinitionMixIn<Type extends ProcessorDefinition<Type>>
    extends OptionalIdentifiedDefinition<Type>
    implements Block, OtherAttributesAware {

    @JsonIgnore
    public abstract <Result> Type setBody(Supplier<Result> supplier);

    @JsonIgnore
    public abstract <Result> Type setBody(Function<Exchange, Result> function);
}
