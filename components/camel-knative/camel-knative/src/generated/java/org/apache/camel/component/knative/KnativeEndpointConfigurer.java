/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.knative;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ExtendedPropertyConfigurerGetter;
import org.apache.camel.spi.PropertyConfigurerGetter;
import org.apache.camel.spi.ConfigurerStrategy;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.util.CaseInsensitiveMap;
import org.apache.camel.support.component.PropertyConfigurerSupport;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@SuppressWarnings("unchecked")
public class KnativeEndpointConfigurer extends PropertyConfigurerSupport implements GeneratedPropertyConfigurer, PropertyConfigurerGetter {

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        KnativeEndpoint target = (KnativeEndpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "apiversion":
        case "apiVersion": target.getConfiguration().setApiVersion(property(camelContext, java.lang.String.class, value)); return true;
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": target.setBridgeErrorHandler(property(camelContext, boolean.class, value)); return true;
        case "ceoverride":
        case "ceOverride": target.getConfiguration().setCeOverride(property(camelContext, java.util.Map.class, value)); return true;
        case "cloudeventsspecversion":
        case "cloudEventsSpecVersion": target.getConfiguration().setCloudEventsSpecVersion(property(camelContext, java.lang.String.class, value)); return true;
        case "cloudeventstype":
        case "cloudEventsType": target.getConfiguration().setCloudEventsType(property(camelContext, java.lang.String.class, value)); return true;
        case "environment": target.getConfiguration().setEnvironment(property(camelContext, org.apache.camel.component.knative.spi.KnativeEnvironment.class, value)); return true;
        case "exceptionhandler":
        case "exceptionHandler": target.setExceptionHandler(property(camelContext, org.apache.camel.spi.ExceptionHandler.class, value)); return true;
        case "exchangepattern":
        case "exchangePattern": target.setExchangePattern(property(camelContext, org.apache.camel.ExchangePattern.class, value)); return true;
        case "filters": target.getConfiguration().setFilters(property(camelContext, java.util.Map.class, value)); return true;
        case "kind": target.getConfiguration().setKind(property(camelContext, java.lang.String.class, value)); return true;
        case "lazystartproducer":
        case "lazyStartProducer": target.setLazyStartProducer(property(camelContext, boolean.class, value)); return true;
        case "name": target.getConfiguration().setName(property(camelContext, java.lang.String.class, value)); return true;
        case "reply": target.getConfiguration().setReply(property(camelContext, java.lang.Boolean.class, value)); return true;
        case "replywithcloudevent":
        case "replyWithCloudEvent": target.getConfiguration().setReplyWithCloudEvent(property(camelContext, boolean.class, value)); return true;
        case "transportoptions":
        case "transportOptions": target.getConfiguration().setTransportOptions(property(camelContext, java.util.Map.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Class<?> getOptionType(String name, boolean ignoreCase) {
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "apiversion":
        case "apiVersion": return java.lang.String.class;
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": return boolean.class;
        case "ceoverride":
        case "ceOverride": return java.util.Map.class;
        case "cloudeventsspecversion":
        case "cloudEventsSpecVersion": return java.lang.String.class;
        case "cloudeventstype":
        case "cloudEventsType": return java.lang.String.class;
        case "environment": return org.apache.camel.component.knative.spi.KnativeEnvironment.class;
        case "exceptionhandler":
        case "exceptionHandler": return org.apache.camel.spi.ExceptionHandler.class;
        case "exchangepattern":
        case "exchangePattern": return org.apache.camel.ExchangePattern.class;
        case "filters": return java.util.Map.class;
        case "kind": return java.lang.String.class;
        case "lazystartproducer":
        case "lazyStartProducer": return boolean.class;
        case "name": return java.lang.String.class;
        case "reply": return java.lang.Boolean.class;
        case "replywithcloudevent":
        case "replyWithCloudEvent": return boolean.class;
        case "transportoptions":
        case "transportOptions": return java.util.Map.class;
        default: return null;
        }
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        KnativeEndpoint target = (KnativeEndpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "apiversion":
        case "apiVersion": return target.getConfiguration().getApiVersion();
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": return target.isBridgeErrorHandler();
        case "ceoverride":
        case "ceOverride": return target.getConfiguration().getCeOverride();
        case "cloudeventsspecversion":
        case "cloudEventsSpecVersion": return target.getConfiguration().getCloudEventsSpecVersion();
        case "cloudeventstype":
        case "cloudEventsType": return target.getConfiguration().getCloudEventsType();
        case "environment": return target.getConfiguration().getEnvironment();
        case "exceptionhandler":
        case "exceptionHandler": return target.getExceptionHandler();
        case "exchangepattern":
        case "exchangePattern": return target.getExchangePattern();
        case "filters": return target.getConfiguration().getFilters();
        case "kind": return target.getConfiguration().getKind();
        case "lazystartproducer":
        case "lazyStartProducer": return target.isLazyStartProducer();
        case "name": return target.getConfiguration().getName();
        case "reply": return target.getConfiguration().getReply();
        case "replywithcloudevent":
        case "replyWithCloudEvent": return target.getConfiguration().isReplyWithCloudEvent();
        case "transportoptions":
        case "transportOptions": return target.getConfiguration().getTransportOptions();
        default: return null;
        }
    }

    @Override
    public Object getCollectionValueType(Object target, String name, boolean ignoreCase) {
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "ceoverride":
        case "ceOverride": return java.lang.String.class;
        case "filters": return java.lang.String.class;
        case "transportoptions":
        case "transportOptions": return java.lang.Object.class;
        default: return null;
        }
    }
}

