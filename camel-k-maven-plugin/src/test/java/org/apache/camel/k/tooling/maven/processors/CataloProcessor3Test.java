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

package org.apache.camel.k.tooling.maven.processors;

import org.apache.camel.catalog.CamelCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CataloProcessor3Test extends AbstractCataloProcessorTest {

    @Test
    public void testAcceptHyphen(){
        CatalogProcessor_3_x cp3 = new CatalogProcessor_3_x();

        CamelCatalog catalog = versionCamelCatalog("3.0.0.acme-123456");

        assertThat(cp3.accepts(catalog)).isTrue();
    }

    @Test
    public void testAcceptEqualToLower(){
        CatalogProcessor_3_x cp3 = new CatalogProcessor_3_x();

        CamelCatalog catalog = versionCamelCatalog("3.0.0");

        assertThat(cp3.accepts(catalog)).isTrue();
    }

    @Test
    public void testAcceptLessThanLower(){
        CatalogProcessor_3_x cp3 = new CatalogProcessor_3_x();

        CamelCatalog catalog = versionCamelCatalog("2.17.0");

        assertThat(cp3.accepts(catalog)).isFalse();
    }

    @Test
    public void testAcceptEqualToHigher(){
        CatalogProcessor_3_x cp3 = new CatalogProcessor_3_x();

        CamelCatalog catalog = versionCamelCatalog("4.0.0");

        assertThat(cp3.accepts(catalog)).isFalse();
    }

    @Test
    public void testAcceptMoreThanHigher(){
        CatalogProcessor_3_x cp3 = new CatalogProcessor_3_x();

        CamelCatalog catalog = versionCamelCatalog("5.0.0");

        assertThat(cp3.accepts(catalog)).isFalse();
    }
}
