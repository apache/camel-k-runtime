package org.apache.camel.k.tooling.maven.processors;

import org.apache.camel.catalog.CamelCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CataloProcessor3Test extends AbstractCataloProcessorTest {

    @Test
    public void testAcceptHyphen(){
        CatalogProcessor_3_x cp3 = new CatalogProcessor_3_x();

        CamelCatalog catalog = versionCamelCatalog("3.0.0.fuse-730049");

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
