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
package org.apache.camel.component.knative;

public interface KnativeConstants {
    // Producer
    String OPERATION = "CamelKnativeOperation";
    String NAMESPACE_NAME = "CamelKnativeNamespaceName";

    String GITHUBSOURCE_NAME = "CamelKnativeGitHubSourceName";
    String GITHUBSOURCE_EVENT_TYPES = "CamelKnativeGitHubSourceEventType";
    String GITHUBSOURCE_OWNER_REPO = "CamelKnativeGitHubSourceOwnerRepo";
    String GITHUBSOURCE_SECRET = "CamelKnativeGitHubSourceSecret";
    String GITHUBSOURCE_ACCESS_TOKEN_KEY = "CamelKnativeGitHubSourceAccessTokenKey";
    String GITHUBSOURCE_SECRET_TOKEN_KEY = "CamelKnativeGitHubSourceSecretTokenKey";
    String GITHUBSOURCE_API_URL = "CamelKnativeGitHubSourceApiUrl";
    String GITHUBSOURCE_SINK_API_VERSION = "CamelKnativeGitHubSourceApiVersion";
    String GITHUBSOURCE_SINK_KIND = "CamelKnativeGitHubSourceSinkKind";
    String GITHUBSOURCE_SINK_NAME = "CamelKnativeGitHubSourceSinkName";

    String EVENT_ACTION = "CamelKnativeEventAction";
    String EVENT_TIMESTAMP = "CamelKnativeEventTimestamp";
}
