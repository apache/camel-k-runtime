/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.k.http;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.spi.PropertyConfigurerGetter;
import org.apache.camel.util.CaseInsensitiveMap;
import org.apache.camel.k.http.PlatformHttpServiceContextCustomizer;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@SuppressWarnings("unchecked")
public class PlatformHttpServiceContextCustomizerConfigurer extends org.apache.camel.support.component.PropertyConfigurerSupport implements GeneratedPropertyConfigurer, PropertyConfigurerGetter {

    private static final Map<String, Object> ALL_OPTIONS;
    static {
        Map<String, Object> map = new CaseInsensitiveMap();
        map.put("BindHost", java.lang.String.class);
        map.put("BindPort", int.class);
        map.put("BodyHandler", org.apache.camel.component.platform.http.vertx.VertxPlatformHttpServerConfiguration.BodyHandler.class);
        map.put("Cors", org.apache.camel.component.platform.http.vertx.VertxPlatformHttpServerConfiguration.Cors.class);
        map.put("MaxBodySize", java.math.BigInteger.class);
        map.put("Path", java.lang.String.class);
        map.put("SslContextParameters", org.apache.camel.support.jsse.SSLContextParameters.class);
        map.put("UseGlobalSslContextParameters", boolean.class);
        ALL_OPTIONS = map;
    }

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        org.apache.camel.k.http.PlatformHttpServiceContextCustomizer target = (org.apache.camel.k.http.PlatformHttpServiceContextCustomizer) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "bindhost":
        case "BindHost": target.setBindHost(property(camelContext, java.lang.String.class, value)); return true;
        case "bindport":
        case "BindPort": target.setBindPort(property(camelContext, int.class, value)); return true;
        case "bodyhandler":
        case "BodyHandler": target.setBodyHandler(property(camelContext, org.apache.camel.component.platform.http.vertx.VertxPlatformHttpServerConfiguration.BodyHandler.class, value)); return true;
        case "cors":
        case "Cors": target.setCors(property(camelContext, org.apache.camel.component.platform.http.vertx.VertxPlatformHttpServerConfiguration.Cors.class, value)); return true;
        case "maxbodysize":
        case "MaxBodySize": target.setMaxBodySize(property(camelContext, java.math.BigInteger.class, value)); return true;
        case "path":
        case "Path": target.setPath(property(camelContext, java.lang.String.class, value)); return true;
        case "sslcontextparameters":
        case "SslContextParameters": target.setSslContextParameters(property(camelContext, org.apache.camel.support.jsse.SSLContextParameters.class, value)); return true;
        case "useglobalsslcontextparameters":
        case "UseGlobalSslContextParameters": target.setUseGlobalSslContextParameters(property(camelContext, boolean.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Map<String, Object> getAllOptions(Object target) {
        return ALL_OPTIONS;
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        org.apache.camel.k.http.PlatformHttpServiceContextCustomizer target = (org.apache.camel.k.http.PlatformHttpServiceContextCustomizer) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "bindhost":
        case "BindHost": return target.getBindHost();
        case "bindport":
        case "BindPort": return target.getBindPort();
        case "bodyhandler":
        case "BodyHandler": return target.getBodyHandler();
        case "cors":
        case "Cors": return target.getCors();
        case "maxbodysize":
        case "MaxBodySize": return target.getMaxBodySize();
        case "path":
        case "Path": return target.getPath();
        case "sslcontextparameters":
        case "SslContextParameters": return target.getSslContextParameters();
        case "useglobalsslcontextparameters":
        case "UseGlobalSslContextParameters": return target.isUseGlobalSslContextParameters();
        default: return null;
        }
    }
}
