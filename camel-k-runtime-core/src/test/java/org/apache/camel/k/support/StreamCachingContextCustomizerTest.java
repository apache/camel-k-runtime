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

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.customizer.StreamCachingContextCustomizer;
import org.apache.camel.spi.StreamCachingStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StreamCachingContextCustomizerTest {

    @Test
    public void testClasspathHandler() {
        StreamCachingContextCustomizer scccc = new StreamCachingContextCustomizer();

        scccc.setAnySpoolRules(true);
        scccc.setBufferSize(9);
        scccc.setEnabled(true);
        scccc.setRemoveSpoolDirectoryWhenStopping(true);
        scccc.setSpoolChiper("sha");
        scccc.setSpoolDirectory("./xxx");
        scccc.setSpoolThreshold(9);
        scccc.setSpoolUsedHeapMemoryLimit("Committed");
        scccc.setSpoolUsedHeapMemoryThreshold(9);

        CamelContext context = new DefaultCamelContext();
        scccc.apply(context);

        assertThat(context.getStreamCachingStrategy().isAnySpoolRules()).isTrue();
        assertThat(context.getStreamCachingStrategy().getBufferSize()).isEqualTo(9);
        assertThat(context.isStreamCaching()).isTrue();
        assertThat(context.getStreamCachingStrategy().isRemoveSpoolDirectoryWhenStopping()).isTrue();
        assertThat(context.getStreamCachingStrategy().getSpoolChiper()).isEqualTo("sha");
        assertThat(context.getStreamCachingStrategy().getSpoolDirectory()).isNull();
        assertThat(context.getStreamCachingStrategy().getSpoolThreshold()).isEqualTo(9L);
        assertThat(context.getStreamCachingStrategy().getSpoolUsedHeapMemoryLimit()).isEqualTo(StreamCachingStrategy.SpoolUsedHeapMemoryLimit.Committed);
        assertThat(context.getStreamCachingStrategy().getSpoolUsedHeapMemoryThreshold()).isEqualTo(9);

        scccc.setSpoolUsedHeapMemoryLimit("Max");

        scccc.apply(context);
        assertThat(context.getStreamCachingStrategy().getSpoolUsedHeapMemoryLimit()).isEqualTo(StreamCachingStrategy.SpoolUsedHeapMemoryLimit.Max);
    }

    @Test
    public void testUnsupportedStreamCachingSpoolUsedHeapMemoryLimit() {
        StreamCachingContextCustomizer scccc = new StreamCachingContextCustomizer();

        scccc.setSpoolUsedHeapMemoryLimit("Unsupported");

        CamelContext context = new DefaultCamelContext();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> scccc.apply(context));

        assertThat(exception.getMessage()).isEqualTo("Invalid option Unsupported must either be Committed or Max");
    }
}
