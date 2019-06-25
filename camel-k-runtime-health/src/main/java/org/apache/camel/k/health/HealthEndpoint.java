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
package org.apache.camel.k.health;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;

public class HealthEndpoint extends HttpServlet {
    private final CamelContext context;

    public HealthEndpoint(CamelContext context) {
        this.context = context;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (context.getStatus() == ServiceStatus.Started) {
            resp.setContentType("text/plain");
            resp.setContentLength(2);
            resp.setStatus(HttpServletResponse.SC_OK);

            try(PrintWriter writer = resp.getWriter()) {
                writer.write("OK");
            }

        } else {
            resp.setContentType("text/plain");
            resp.setContentLength(2);
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            try(PrintWriter writer = resp.getWriter()) {
                writer.write("KO");
            }
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
