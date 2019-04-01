package org.apache.camel.k.tooling.maven.processors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;

public abstract class AbstractCataloProcessorTest {

    protected CamelCatalog versionCamelCatalog(String version){
        return new DefaultCamelCatalog() {

            @Override
            public String getCatalogVersion() {
                return version;
            }

        };
    }
}
