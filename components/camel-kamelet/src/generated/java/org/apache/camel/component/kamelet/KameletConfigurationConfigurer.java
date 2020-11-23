/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.kamelet;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.spi.PropertyConfigurerGetter;
import org.apache.camel.util.CaseInsensitiveMap;
import org.apache.camel.component.kamelet.KameletConfiguration;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@SuppressWarnings("unchecked")
public class KameletConfigurationConfigurer extends org.apache.camel.support.component.PropertyConfigurerSupport implements GeneratedPropertyConfigurer, PropertyConfigurerGetter {

    private static final Map<String, Object> ALL_OPTIONS;
    static {
        Map<String, Object> map = new CaseInsensitiveMap();
        map.put("RouteProperties", java.util.Map.class);
        map.put("TemplateProperties", java.util.Map.class);
        ALL_OPTIONS = map;
    }

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        org.apache.camel.component.kamelet.KameletConfiguration target = (org.apache.camel.component.kamelet.KameletConfiguration) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "routeproperties":
        case "RouteProperties": target.setRouteProperties(property(camelContext, java.util.Map.class, value)); return true;
        case "templateproperties":
        case "TemplateProperties": target.setTemplateProperties(property(camelContext, java.util.Map.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Map<String, Object> getAllOptions(Object target) {
        return ALL_OPTIONS;
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        org.apache.camel.component.kamelet.KameletConfiguration target = (org.apache.camel.component.kamelet.KameletConfiguration) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "routeproperties":
        case "RouteProperties": return target.getRouteProperties();
        case "templateproperties":
        case "TemplateProperties": return target.getTemplateProperties();
        default: return null;
        }
    }

    @Override
    public Object getCollectionValueType(Object target, String name, boolean ignoreCase) {
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "routeproperties":
        case "RouteProperties": return java.util.Properties.class;
        case "templateproperties":
        case "TemplateProperties": return java.util.Properties.class;
        default: return null;
        }
    }
}

