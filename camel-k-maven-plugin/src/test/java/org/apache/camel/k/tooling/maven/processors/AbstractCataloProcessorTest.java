package org.apache.camel.k.tooling.maven.processors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.catalog.JSonSchemaResolver;
import org.apache.camel.catalog.LanguageValidationResult;
import org.apache.camel.catalog.RuntimeProvider;
import org.apache.camel.catalog.SimpleValidationResult;
import org.apache.camel.catalog.SuggestionStrategy;
import org.apache.camel.catalog.VersionManager;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCataloProcessorTest {
    protected CamelCatalog versionCamelCatalog(String version){
        return new CamelCatalog() {
            @Override
            public JSonSchemaResolver getJSonSchemaResolver() {
                return null;
            }

            @Override
            public void setJSonSchemaResolver(JSonSchemaResolver resolver) {

            }

            @Override
            public void setRuntimeProvider(RuntimeProvider provider) {

            }

            @Override
            public RuntimeProvider getRuntimeProvider() {
                return null;
            }

            @Override
            public void enableCache() {

            }

            @Override
            public boolean isCaching() {
                return false;
            }

            @Override
            public void setSuggestionStrategy(SuggestionStrategy suggestionStrategy) {

            }

            @Override
            public SuggestionStrategy getSuggestionStrategy() {
                return null;
            }

            @Override
            public void setVersionManager(VersionManager versionManager) {

            }

            @Override
            public VersionManager getVersionManager() {
                return null;
            }

            @Override
            public void addComponent(String name, String className) {

            }

            @Override
            public void addComponent(String name, String className, String jsonSchema) {

            }

            @Override
            public void addDataFormat(String name, String className) {

            }

            @Override
            public void addDataFormat(String name, String className, String jsonSchema) {

            }

            @Override
            public String getCatalogVersion() {
                return version;
            }

            @Override
            public boolean loadVersion(String version) {
                return false;
            }

            @Override
            public String getLoadedVersion() {
                return null;
            }

            @Override
            public String getRuntimeProviderLoadedVersion() {
                return null;
            }

            @Override
            public boolean loadRuntimeProviderVersion(String groupId, String artifactId, String version) {
                return false;
            }

            @Override
            public List<String> findComponentNames() {
                return null;
            }

            @Override
            public List<String> findDataFormatNames() {
                return null;
            }

            @Override
            public List<String> findLanguageNames() {
                return null;
            }

            @Override
            public List<String> findModelNames() {
                return null;
            }

            @Override
            public List<String> findOtherNames() {
                return null;
            }

            @Override
            public List<String> findComponentNames(String filter) {
                return null;
            }

            @Override
            public List<String> findDataFormatNames(String filter) {
                return null;
            }

            @Override
            public List<String> findLanguageNames(String filter) {
                return null;
            }

            @Override
            public List<String> findModelNames(String filter) {
                return null;
            }

            @Override
            public List<String> findOtherNames(String filter) {
                return null;
            }

            @Override
            public String componentJSonSchema(String name) {
                return null;
            }

            @Override
            public String dataFormatJSonSchema(String name) {
                return null;
            }

            @Override
            public String languageJSonSchema(String name) {
                return null;
            }

            @Override
            public String otherJSonSchema(String name) {
                return null;
            }

            @Override
            public String modelJSonSchema(String name) {
                return null;
            }

            @Override
            public String componentAsciiDoc(String name) {
                return null;
            }

            @Override
            public String componentHtmlDoc(String name) {
                return null;
            }

            @Override
            public String dataFormatAsciiDoc(String name) {
                return null;
            }

            @Override
            public String dataFormatHtmlDoc(String name) {
                return null;
            }

            @Override
            public String languageAsciiDoc(String name) {
                return null;
            }

            @Override
            public String languageHtmlDoc(String name) {
                return null;
            }

            @Override
            public String otherAsciiDoc(String name) {
                return null;
            }

            @Override
            public String otherHtmlDoc(String name) {
                return null;
            }

            @Override
            public Set<String> findComponentLabels() {
                return null;
            }

            @Override
            public Set<String> findDataFormatLabels() {
                return null;
            }

            @Override
            public Set<String> findLanguageLabels() {
                return null;
            }

            @Override
            public Set<String> findModelLabels() {
                return null;
            }

            @Override
            public Set<String> findOtherLabels() {
                return null;
            }

            @Override
            public String archetypeCatalogAsXml() {
                return null;
            }

            @Override
            public String springSchemaAsXml() {
                return null;
            }

            @Override
            public String blueprintSchemaAsXml() {
                return null;
            }

            @Override
            public Map<String, String> endpointProperties(String uri) throws URISyntaxException {
                return null;
            }

            @Override
            public Map<String, String> endpointLenientProperties(String uri) throws URISyntaxException {
                return null;
            }

            @Override
            public boolean validateTimePattern(String pattern) {
                return false;
            }

            @Override
            public EndpointValidationResult validateEndpointProperties(String uri) {
                return null;
            }

            @Override
            public EndpointValidationResult validateEndpointProperties(String uri, boolean ignoreLenientProperties) {
                return null;
            }

            @Override
            public EndpointValidationResult validateEndpointProperties(String uri, boolean ignoreLenientProperties, boolean consumerOnly, boolean producerOnly) {
                return null;
            }

            @Override
            public SimpleValidationResult validateSimpleExpression(String simple) {
                return null;
            }

            @Override
            public SimpleValidationResult validateSimpleExpression(ClassLoader classLoader, String simple) {
                return null;
            }

            @Override
            public SimpleValidationResult validateSimplePredicate(String simple) {
                return null;
            }

            @Override
            public SimpleValidationResult validateSimplePredicate(ClassLoader classLoader, String simple) {
                return null;
            }

            @Override
            public LanguageValidationResult validateLanguagePredicate(ClassLoader classLoader, String language, String text) {
                return null;
            }

            @Override
            public LanguageValidationResult validateLanguageExpression(ClassLoader classLoader, String language, String text) {
                return null;
            }

            @Override
            public String endpointComponentName(String uri) {
                return null;
            }

            @Override
            public String asEndpointUri(String scheme, String json, boolean encode) throws URISyntaxException {
                return null;
            }

            @Override
            public String asEndpointUriXml(String scheme, String json, boolean encode) throws URISyntaxException {
                return null;
            }

            @Override
            public String asEndpointUri(String scheme, Map<String, String> properties, boolean encode) throws URISyntaxException {
                return null;
            }

            @Override
            public String asEndpointUriXml(String scheme, Map<String, String> properties, boolean encode) throws URISyntaxException {
                return null;
            }

            @Override
            public String listComponentsAsJson() {
                return null;
            }

            @Override
            public String listDataFormatsAsJson() {
                return null;
            }

            @Override
            public String listLanguagesAsJson() {
                return null;
            }

            @Override
            public String listModelsAsJson() {
                return null;
            }

            @Override
            public String listOthersAsJson() {
                return null;
            }

            @Override
            public String summaryAsJson() {
                return null;
            }
        };
    }
}
