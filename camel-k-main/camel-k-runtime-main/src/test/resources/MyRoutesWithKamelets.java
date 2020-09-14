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

import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.k.main.support.MyBean;

public class MyRoutesWithKamelets extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:k1")
            .routeId("k1")
            .to("kamelet:my-template/myKamelet1?message=my-message");
        from("direct:k2")
            .routeId("k2")
            .to("kamelet:my-template/myKamelet2");
    }
}