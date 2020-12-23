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
package org.apache.camel.k.loader.yaml;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeserializationContextTest {
    @Disabled
    @Test
    void withAttributeOnDeserializationConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().withAttribute("message", "test");

        Foo foo = mapper.readValue(
            "{ \"bar\": { \"id\": \"1\"} }",
            Foo.class);

        assertThat(foo).isNotNull();
        assertThat(foo.message).isEqualTo("test");
        assertThat(foo.bar).isNotNull();
        assertThat(foo.bar.id).isEqualTo("1");
        assertThat(foo.bar.message).isEqualTo("test");
    }

    @Disabled
    @Test
    void withAttributeOnReader() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Foo foo = mapper.readerFor(Foo.class)
            .withAttribute("message", "test")
            .readValue("{ \"bar\": { \"id\": \"1\"} }");

        assertThat(foo).isNotNull();
        assertThat(foo.message).isEqualTo("test");
        assertThat(foo.bar).isNotNull();
        assertThat(foo.bar.id).isEqualTo("1");
        assertThat(foo.bar.message).isEqualTo("test");
    }

    @JsonDeserialize(using = Foo.Deserializer.class)
    public static class Foo {
        Bar bar;
        String message;

        public static class Deserializer extends StdDeserializer<Foo> {
            public Deserializer() {
                super(Foo.class);
            }

            @Override
            public Foo deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                Foo foo = new Foo();
                foo.message = (String)ctx.getAttribute("message");

                JsonNode node = p.getCodec().readTree(p);
                JsonNode bar = node.get("bar");

                if (bar != null) {
                    foo.bar = p.getCodec().treeToValue(bar, Bar.class);
                }

                return foo;
            }
        }
    }

    @JsonDeserialize(using = Bar.Deserializer.class)
    public static class Bar {
        String id;
        String message;

        public static class Deserializer extends StdDeserializer<Bar> {
            public Deserializer() {
                super(Bar.class);
            }

            @Override
            public Bar deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                Bar bar = new Bar();
                bar.message = (String)ctx.getAttribute("message");

                JsonNode node = p.getCodec().readTree(p);
                JsonNode id = node.get("id");

                if (id != null) {
                    bar.id = id.textValue();
                }

                return bar;
            }
        }
    }
}
