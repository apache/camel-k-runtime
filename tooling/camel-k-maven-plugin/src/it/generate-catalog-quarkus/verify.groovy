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

new File(basedir, "catalog.yaml").withReader {
    def catalog = new groovy.yaml.YamlSlurper().parse(it)

    assert catalog.spec.runtime.version == runtimeVersion
    assert catalog.spec.runtime.applicationClass == 'io.quarkus.runner.GeneratedMain'
    assert catalog.spec.runtime.metadata['camel.version'] == camelVersion
    assert catalog.spec.runtime.metadata['quarkus.version'] == quarkusVersion
    assert catalog.spec.runtime.metadata['camel-quarkus.version'] == camelQuarkusVersion


    assert catalog.spec.runtime.capabilities['cron'].dependencies[0].groupId == 'org.apache.camel.k'
    assert catalog.spec.runtime.capabilities['cron'].dependencies[0].artifactId == 'camel-k-quarkus-cron'
    assert catalog.spec.runtime.capabilities['health'].dependencies[0].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.runtime.capabilities['health'].dependencies[0].artifactId == 'camel-quarkus-microprofile-health'
    assert catalog.spec.runtime.capabilities['rest'].dependencies.any { it.groupId == 'org.apache.camel.quarkus' && it.artifactId == 'camel-quarkus-rest' }
    assert catalog.spec.runtime.capabilities['rest'].dependencies.any { it.groupId == 'org.apache.camel.quarkus' && it.artifactId == 'camel-quarkus-platform-http' }
    assert catalog.spec.runtime.capabilities['platform-http'].dependencies[0].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.runtime.capabilities['platform-http'].dependencies[0].artifactId == 'camel-quarkus-platform-http'
    assert catalog.spec.runtime.capabilities['circuit-breaker'].dependencies[0].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.runtime.capabilities['circuit-breaker'].dependencies[0].artifactId == 'camel-quarkus-microprofile-fault-tolerance'
    assert catalog.spec.runtime.capabilities['tracing'].dependencies[0].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.runtime.capabilities['tracing'].dependencies[0].artifactId == 'camel-quarkus-opentracing'

    assert catalog.metadata.labels['camel.apache.org/runtime.version'] == runtimeVersion

    catalog.spec.artifacts['camel-knative'].with {
        assert dependencies.size() == 1
        assert dependencies[0].groupId == 'org.apache.camel.k'
        assert dependencies[0].artifactId == 'camel-k-quarkus-knative'
        assert schemes.size() == 1
        assert schemes[0].id == 'knative'
    }
}