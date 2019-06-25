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

import javax.servlet.http.HttpServlet;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import org.apache.camel.CamelContext;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletEndpoint extends ServiceSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServletEndpoint.class);

    private final CamelContext context;
    private final String bindHost;
    private final int bindPort;
    private final String path;

    private Undertow server;
    private DeploymentManager manager;

    public ServletEndpoint(CamelContext context, String bindHost, int bindPort, String path) {
        this.context = context;
        this.bindHost = bindHost;
        this.bindPort = bindPort;
        this.path = path;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doStart() throws Exception {
        DeploymentInfo servletBuilder = Servlets.deployment()
            .setClassLoader(ServletEndpoint.class.getClassLoader())
            .setContextPath(path)
            .setDeploymentName("camel-k.war");

        context.getRegistry().findByType(ServletRegistration.class).forEach( r -> {
                LOGGER.info("Registering servlet: {}", r);

                servletBuilder.addServlet(
                    Servlets.servlet(r.getName(), HttpServlet.class, () -> new ImmediateInstanceHandle(r.getServlet())).addMappings(r.getMappings())
                );
            }
        );

        this.manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        this.manager.deploy();

        PathHandler path = Handlers.path(Handlers.redirect(this.path)).addPrefixPath(this.path, manager.start());

        LOGGER.info("Starting servlet engine on {}:{}{}", this.bindHost, this.bindPort, this.path);

        this.server = Undertow.builder().addHttpListener(this.bindPort, this.bindHost).setHandler(path).build();
        this.server.start();
    }

    @Override
    protected void doStop() throws Exception {
        if (this.server != null) {
            LOGGER.info("Stopping servlet engine");
            this.server.stop();
        }
    }
}
