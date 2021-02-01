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


    assert catalog.spec.runtime.dependencies.any {
        it.groupId == 'org.apache.camel.k' && it.artifactId == 'camel-k-runtime'
    }

    assert catalog.spec.runtime.capabilities['cron'].dependencies[0].groupId == 'org.apache.camel.k'
    assert catalog.spec.runtime.capabilities['cron'].dependencies[0].artifactId == 'camel-k-cron'
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
    assert catalog.spec.runtime.capabilities['master'].dependencies[0].groupId == 'org.apache.camel.k'
    assert catalog.spec.runtime.capabilities['master'].dependencies[0].artifactId == 'camel-k-master'

    assert catalog.metadata.labels['camel.apache.org/runtime.version'] == runtimeVersion

    catalog.spec.artifacts['camel-k-master'].with {
        schemes == null
    }
    catalog.spec.artifacts['camel-k-cron'].with {
        schemes == null
    }
    catalog.spec.artifacts['camel-k-webhook'].with {
        schemes == null
    }

    def diff = org.apache.commons.collections4.CollectionUtils.disjunction(
            catalog.spec.artifacts.values()
                    .findAll { it.schemes != null }
                    .collect { it.schemes.collect { it.id } }
                    .flatten(),
            catalog.spec.artifacts.values()
                    .findAll { it.schemes != null }
                    .collect { it.schemes.collect { it.id } }
                    .flatten()
                    .unique()
    )

    assert diff.size() == 0 : "Duplicated schemes: ${diff}"

    catalog.spec.artifacts['camel-k-knative'].with {
        assert dependencies == null
        assert requiredCapabilities == null
        assert schemes.size() == 1

        schemes[0].with {
            assert id == 'knative'
            assert consumer.requiredCapabilities == null
            assert consumer.dependencies[0].groupId == 'org.apache.camel.k'
            assert consumer.dependencies[0].artifactId == 'camel-k-knative-consumer'

            assert producer.requiredCapabilities == null
            assert producer.dependencies[0].groupId == 'org.apache.camel.k'
            assert producer.dependencies[0].artifactId == 'camel-k-knative-producer'
        }
    }

    catalog.spec.artifacts['camel-k-kamelet'].with {
        assert dependencies == null
        assert requiredCapabilities == null

        assert schemes.size() == 1

        schemes[0].with {
            assert id == 'kamelet'
            assert passive == true
            assert http == false
        }
    }

    catalog.spec.artifacts['camel-k-kamelet-reify'].with {
        assert dependencies == null
        assert requiredCapabilities == null

        assert schemes.size() == 1

        schemes[0].with {
            assert id == 'kamelet-reify'
            assert passive == false
            assert http == false
        }
    }
}