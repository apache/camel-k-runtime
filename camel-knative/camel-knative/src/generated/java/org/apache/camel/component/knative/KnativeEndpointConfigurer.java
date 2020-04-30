/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.knative;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.spi.PropertyConfigurerGetter;
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
        case "basicpropertybinding":
        case "basicPropertyBinding": target.setBasicPropertyBinding(property(camelContext, boolean.class, value)); return true;
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
        case "replywithcloudevent":
        case "replyWithCloudEvent": target.getConfiguration().setReplyWithCloudEvent(property(camelContext, boolean.class, value)); return true;
        case "servicename":
        case "serviceName": target.getConfiguration().setServiceName(property(camelContext, java.lang.String.class, value)); return true;
        case "synchronous": target.setSynchronous(property(camelContext, boolean.class, value)); return true;
        case "transportoptions":
        case "transportOptions": target.getConfiguration().setTransportOptions(property(camelContext, java.util.Map.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Map<String, Object> getAllOptions(Object target) {
        Map<String, Object> answer = new CaseInsensitiveMap();
        answer.put("apiVersion", java.lang.String.class);
        answer.put("basicPropertyBinding", boolean.class);
        answer.put("bridgeErrorHandler", boolean.class);
        answer.put("ceOverride", java.util.Map.class);
        answer.put("cloudEventsSpecVersion", java.lang.String.class);
        answer.put("cloudEventsType", java.lang.String.class);
        answer.put("environment", org.apache.camel.component.knative.spi.KnativeEnvironment.class);
        answer.put("exceptionHandler", org.apache.camel.spi.ExceptionHandler.class);
        answer.put("exchangePattern", org.apache.camel.ExchangePattern.class);
        answer.put("filters", java.util.Map.class);
        answer.put("kind", java.lang.String.class);
        answer.put("lazyStartProducer", boolean.class);
        answer.put("replyWithCloudEvent", boolean.class);
        answer.put("serviceName", java.lang.String.class);
        answer.put("synchronous", boolean.class);
        answer.put("transportOptions", java.util.Map.class);
        return answer;
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        KnativeEndpoint target = (KnativeEndpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "apiversion":
        case "apiVersion": return target.getConfiguration().getApiVersion();
        case "basicpropertybinding":
        case "basicPropertyBinding": return target.isBasicPropertyBinding();
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
        case "replywithcloudevent":
        case "replyWithCloudEvent": return target.getConfiguration().isReplyWithCloudEvent();
        case "servicename":
        case "serviceName": return target.getConfiguration().getServiceName();
        case "synchronous": return target.isSynchronous();
        case "transportoptions":
        case "transportOptions": return target.getConfiguration().getTransportOptions();
        default: return null;
        }
    }
}

