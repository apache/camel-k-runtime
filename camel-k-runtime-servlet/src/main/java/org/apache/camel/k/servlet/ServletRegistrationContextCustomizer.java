/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.k.servlet;

import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.k.ContextCustomizer;

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
    public void apply(CamelContext camelContext) {
        camelContext.getRegistry().bind(
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