package org.apache.camel.k.tooling.maven.processors;

import org.apache.camel.catalog.CamelCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CataloProcessor2Test extends AbstractCataloProcessorTest {

    @Test
    public void testAcceptHyphen(){
        CatalogProcessor_2_x cp2 = new CatalogProcessor_2_x();

        CamelCatalog catalog = versionCamelCatalog("2.21.0.fuse-730049");

        assertThat(cp2.accepts(catalog)).isTrue();
    }

    @Test
    public void testAcceptEqualToLower(){
        CatalogProcessor_2_x cp2 = new CatalogProcessor_2_x();

        CamelCatalog catalog = versionCamelCatalog("2.18.0");

        assertThat(cp2.accepts(catalog)).isTrue();
    }

    @Test
    public void testAcceptLessThanLower(){
        CatalogProcessor_2_x cp2 = new CatalogProcessor_2_x();

        CamelCatalog catalog = versionCamelCatalog("2.17.0");

        assertThat(cp2.accepts(catalog)).isFalse();
    }

    @Test
    public void testAcceptEqualToHigher(){
        CatalogProcessor_2_x cp2 = new CatalogProcessor_2_x();

        CamelCatalog catalog = versionCamelCatalog("3.0.0");

        assertThat(cp2.accepts(catalog)).isFalse();
    }

    @Test
    public void testAcceptMoreThanHigher(){
        CatalogProcessor_2_x cp2 = new CatalogProcessor_2_x();

        CamelCatalog catalog = versionCamelCatalog("4.0.0");

        assertThat(cp2.accepts(catalog)).isFalse();
    }
}
