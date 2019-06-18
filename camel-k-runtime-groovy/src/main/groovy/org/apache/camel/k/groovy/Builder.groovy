package org.apache.camel.k.groovy

import org.apache.camel.CamelContext
import org.apache.camel.Endpoint
import org.apache.camel.Expression
import org.apache.camel.NoSuchEndpointException
import org.apache.camel.builder.*
import org.apache.camel.model.*
import org.apache.camel.model.rest.RestConfigurationDefinition
import org.apache.camel.model.rest.RestDefinition
import org.apache.camel.model.rest.RestsDefinition
import org.apache.camel.support.builder.Namespaces

class Builder {

    static RouteBuilder delegate

    static void configure() throws Exception {
        delegate.configure();
    }

    static void bindToRegistry(String id, Object bean) {
        delegate.bindToRegistry(id, bean);
    }

    static void bindToRegistry(String id, Class<?> type, Object bean) {
        delegate.bindToRegistry(id, type, bean);
    }

    static RestConfigurationDefinition restConfiguration() {
        return delegate.restConfiguration();
    }

    static RestConfigurationDefinition restConfiguration(String component) {
        return delegate.restConfiguration(component);
    }

    static RestDefinition rest() {
        return delegate.rest();
    }

    static RestDefinition rest(String path) {
        return delegate.rest(path);
    }

    static TransformerBuilder transformer() {
        return delegate.transformer();
    }

    static ValidatorBuilder validator() {
        return delegate.validator();
    }

    static RouteDefinition from(String uri) {
        return delegate.from(uri);
    }

    static RouteDefinition fromF(String uri, Object... args) {
        return delegate.fromF(uri, args);
    }

    static RouteDefinition from(Endpoint endpoint) {
        return delegate.from(endpoint);
    }

    static void errorHandler(ErrorHandlerBuilder errorHandlerBuilder) {
        delegate.errorHandler(errorHandlerBuilder);
    }

    static <T> T propertyInject(String key, Class<T> type) throws Exception {
        return delegate.propertyInject(key, type);
    }

    static InterceptDefinition intercept() {
        return delegate.intercept();
    }

    static InterceptFromDefinition interceptFrom() {
        return delegate.interceptFrom();
    }

    static InterceptFromDefinition interceptFrom(String uri) {
        return delegate.interceptFrom(uri);
    }

    static InterceptSendToEndpointDefinition interceptSendToEndpoint(String uri) {
        return delegate.interceptSendToEndpoint(uri);
    }

    static OnExceptionDefinition onException(Class<? extends Throwable> exception) {
        return delegate.onException(exception);
    }

    static OnExceptionDefinition onException(Class<? extends Throwable>... exceptions) {
        return delegate.onException(exceptions);
    }

    static OnCompletionDefinition onCompletion() {
        return delegate.onCompletion();
    }

    static void addRoutesToCamelContext(CamelContext context) throws Exception {
        delegate.addRoutesToCamelContext(context);
    }

    static RoutesDefinition configureRoutes(CamelContext context) throws Exception {
        return delegate.configureRoutes(context);
    }

    static RestsDefinition configureRests(CamelContext context) throws Exception {
        return delegate.configureRests(context);
    }

    static void setErrorHandlerBuilder(ErrorHandlerBuilder errorHandlerBuilder) {
        delegate.setErrorHandlerBuilder(errorHandlerBuilder);
    }

    static RestsDefinition getRestCollection() {
        return delegate.getRestCollection();
    }

    static Map<String, RestConfigurationDefinition> getRestConfigurations() {
        return delegate.getRestConfigurations();
    }

    static void setRestCollection(RestsDefinition restCollection) {
        delegate.setRestCollection(restCollection);
    }

    static void setRouteCollection(RoutesDefinition routeCollection) {
        delegate.setRouteCollection(routeCollection);
    }

    static RoutesDefinition getRouteCollection() {
        return delegate.getRouteCollection();
    }

    static ValueBuilder header(String name) {
        return delegate.header(name);
    }

    static ValueBuilder exchangeProperty(String name) {
        return delegate.exchangeProperty(name);
    }

    static ValueBuilder body() {
        return delegate.body();
    }

    static <T> ValueBuilder bodyAs(Class<T> type) {
        return delegate.bodyAs(type);
    }

    static ValueBuilder systemProperty(String name) {
        return delegate.systemProperty(name);
    }

    static ValueBuilder systemProperty(String name, String defaultValue) {
        return delegate.systemProperty(name, defaultValue);
    }

    static ValueBuilder constant(Object value) {
        return delegate.constant(value);
    }

    static ValueBuilder jsonpath(String value) {
        return delegate.jsonpath(value);
    }

    static ValueBuilder jsonpath(String value, Class<?> resultType) {
        return delegate.jsonpath(value, resultType);
    }

    static ValueBuilder language(String language, String expression) {
        return delegate.language(language, expression);
    }

    static SimpleBuilder simple(String value) {
        return delegate.simple(value);
    }

    static SimpleBuilder simple(String value, Class<?> resultType) {
        return delegate.simple(value, resultType);
    }

    static SimpleBuilder simpleF(String format, Object... values) {
        return delegate.simpleF(format, values);
    }

    static SimpleBuilder simpleF(String format, Class<?> resultType, Object... values) {
        return delegate.simpleF(format, resultType, values);
    }

    static ValueBuilder xpath(String value) {
        return delegate.xpath(value);
    }

    static ValueBuilder xpath(String value, Class<?> resultType) {
        return delegate.xpath(value, resultType);
    }

    static ValueBuilder xpath(String value, Namespaces namespaces) {
        return delegate.xpath(value, namespaces);
    }

    static ValueBuilder xpath(String value, Class<?> resultType, Namespaces namespaces) {
        return delegate.xpath(value, resultType, namespaces);
    }

    static ValueBuilder method(Object beanOrBeanRef) {
        return delegate.method(beanOrBeanRef);
    }

    static ValueBuilder method(Object beanOrBeanRef, String method) {
        return delegate.method(beanOrBeanRef, method);
    }

    static ValueBuilder method(Class<?> beanType) {
        return delegate.method(beanType);
    }

    static ValueBuilder method(Class<?> beanType, String method) {
        return delegate.method(beanType, method);
    }

    static ValueBuilder regexReplaceAll(Expression content, String regex, String replacement) {
        return delegate.regexReplaceAll(content, regex, replacement);
    }

    static ValueBuilder regexReplaceAll(Expression content, String regex, Expression replacement) {
        return delegate.regexReplaceAll(content, regex, replacement);
    }

    static ValueBuilder exceptionMessage() {
        return delegate.exceptionMessage();
    }

    static Endpoint endpoint(String uri) throws NoSuchEndpointException {
        return delegate.endpoint(uri);
    }

    static <T extends Endpoint> T endpoint(String uri, Class<T> type) throws NoSuchEndpointException {
        return delegate.endpoint(uri, type);
    }

    static List<Endpoint> endpoints(String... uris) throws NoSuchEndpointException {
        return delegate.endpoints(uris);
    }

    static List<Endpoint> endpoints(Endpoint... endpoints) {
        return delegate.endpoints(endpoints);
    }

    static DefaultErrorHandlerBuilder defaultErrorHandler() {
        return delegate.defaultErrorHandler();
    }

    static NoErrorHandlerBuilder noErrorHandler() {
        return delegate.noErrorHandler();
    }

    static DeadLetterChannelBuilder deadLetterChannel(String deadLetterUri) {
        return delegate.deadLetterChannel(deadLetterUri);
    }

    static DeadLetterChannelBuilder deadLetterChannel(Endpoint deadLetterEndpoint) {
        return delegate.deadLetterChannel(deadLetterEndpoint);
    }

    static CamelContext getContext() {
        return delegate.getContext();
    }

    static void setContext(CamelContext context) {
        delegate.setContext(context);
    }

    static ErrorHandlerBuilder getErrorHandlerBuilder() {
        return delegate.getErrorHandlerBuilder();
    }

}
