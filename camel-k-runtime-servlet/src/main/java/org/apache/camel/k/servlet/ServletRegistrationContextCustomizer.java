package org.apache.camel.k.servlet;

import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.Runtime;

public class ServletRegistrationContextCustomizer implements ContextCustomizer {
    public static final String DEFAULT_PATH = "/camel/*";
    public static final String DEFAULT_CAMEL_SERVLET_NAME = "CamelServlet";

    private String path;
    private String camelServletName;

    public ServletRegistrationContextCustomizer(){
        this(DEFAULT_PATH, DEFAULT_CAMEL_SERVLET_NAME);
    }

    public ServletRegistrationContextCustomizer(String path, String camelServletName){
        this.path = path;
        this.camelServletName = camelServletName;
    }

    @Override
    public void apply(CamelContext camelContext, Runtime.Registry registry) {
        registry.bind(
                camelServletName,
                new ServletRegistration(camelServletName, new CamelHttpTransportServlet(), path)
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCamelServletName() {
        return camelServletName;
    }

    public void setCamelServletName(String camelServletName) {
        this.camelServletName = camelServletName;
    }
}