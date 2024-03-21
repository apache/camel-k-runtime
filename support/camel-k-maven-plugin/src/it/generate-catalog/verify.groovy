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
    assert catalog.spec.runtime.applicationClass == 'io.quarkus.bootstrap.runner.QuarkusEntryPoint'
    assert catalog.spec.runtime.metadata['camel.version'] == camelVersion
    // Re-enabled this when the version will be the same again
    //assert catalog.spec.runtime.metadata['quarkus.version'] == quarkusVersion
    assert catalog.spec.runtime.metadata['camel-quarkus.version'] == camelQuarkusVersion
    assert catalog.spec.runtime.metadata['quarkus.native-builder-image'] == quarkusNativeBuilderImage
    assert catalog.spec.runtime.metadata['jib.maven-plugin.version'] == jibMavenPluginVersion
    assert catalog.spec.runtime.metadata['jib.layer-filter-extension-maven.version'] == jibLayerFilterExtensionMavenVersion

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
    assert catalog.spec.runtime.capabilities['telemetry'].dependencies[0].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.runtime.capabilities['telemetry'].dependencies[0].artifactId == 'camel-quarkus-opentelemetry'
    assert catalog.spec.runtime.capabilities['master'].dependencies[0].groupId == 'org.apache.camel.k'
    assert catalog.spec.runtime.capabilities['master'].dependencies[0].artifactId == 'camel-k-master'
    // Logging properties
    assert catalog.spec.runtime.capabilities['logging'].runtimeProperties[0].key == 'quarkus.console.color'
    assert catalog.spec.runtime.capabilities['logging'].runtimeProperties[0].value == '${camel.k.logging.color}'
    assert catalog.spec.runtime.capabilities['logging'].runtimeProperties[1].key == 'quarkus.log.console.format'
    assert catalog.spec.runtime.capabilities['logging'].runtimeProperties[1].value == '${camel.k.logging.format}'
    assert catalog.spec.runtime.capabilities['logging'].runtimeProperties[2].key == 'quarkus.log.console.json'
    assert catalog.spec.runtime.capabilities['logging'].runtimeProperties[2].value == '${camel.k.logging.json}'
    assert catalog.spec.runtime.capabilities['logging'].runtimeProperties[3].key == 'quarkus.log.console.json.pretty-print'
    assert catalog.spec.runtime.capabilities['logging'].runtimeProperties[3].value == '${camel.k.logging.jsonPrettyPrint}'
    assert catalog.spec.runtime.capabilities['logging'].runtimeProperties[4].key == 'quarkus.log.level'
    assert catalog.spec.runtime.capabilities['logging'].runtimeProperties[4].value == '${camel.k.logging.level}'
    // Master properties
    assert catalog.spec.runtime.capabilities['master'].runtimeProperties[0].key == 'quarkus.camel.cluster.kubernetes.labels."${camel.k.master.labelKey}"'
    assert catalog.spec.runtime.capabilities['master'].runtimeProperties[0].value == '${camel.k.master.labelValue}'
    assert catalog.spec.runtime.capabilities['master'].runtimeProperties[1].key == 'quarkus.camel.cluster.kubernetes.resource-name'
    assert catalog.spec.runtime.capabilities['master'].runtimeProperties[1].value == '${camel.k.master.resourceName}'
    assert catalog.spec.runtime.capabilities['master'].runtimeProperties[2].key == 'quarkus.camel.cluster.kubernetes.resource-type'
    assert catalog.spec.runtime.capabilities['master'].runtimeProperties[2].value == '${camel.k.master.resourceType}'
    assert catalog.spec.runtime.capabilities['master'].buildTimeProperties[0].key == 'quarkus.camel.cluster.kubernetes.enabled'
    assert catalog.spec.runtime.capabilities['master'].buildTimeProperties[0].value == '${camel.k.master.enabled}'
    // Telemetry properties
    assert catalog.spec.runtime.capabilities['telemetry'].runtimeProperties[0].key == 'quarkus.opentelemetry.tracer.exporter.otlp.endpoint'
    assert catalog.spec.runtime.capabilities['telemetry'].runtimeProperties[0].value == '${camel.k.telemetry.endpoint}'
    assert catalog.spec.runtime.capabilities['telemetry'].runtimeProperties[1].key == 'quarkus.opentelemetry.tracer.resource-attributes'
    assert catalog.spec.runtime.capabilities['telemetry'].runtimeProperties[1].value == '${camel.k.telemetry.serviceName}'
    assert catalog.spec.runtime.capabilities['telemetry'].runtimeProperties[2].key == 'quarkus.opentelemetry.tracer.sampler'
    assert catalog.spec.runtime.capabilities['telemetry'].runtimeProperties[2].value == '${camel.k.telemetry.sampler}'
    assert catalog.spec.runtime.capabilities['telemetry'].runtimeProperties[3].key == 'quarkus.opentelemetry.tracer.sampler.parent-based'
    assert catalog.spec.runtime.capabilities['telemetry'].runtimeProperties[3].value == '${camel.k.telemetry.samplerParentBased}'
    assert catalog.spec.runtime.capabilities['telemetry'].runtimeProperties[4].key == 'quarkus.opentelemetry.tracer.sampler.ratio'
    assert catalog.spec.runtime.capabilities['telemetry'].runtimeProperties[4].value == '${camel.k.telemetry.samplerRatio}'

    // Service Binding properties
    assert catalog.spec.runtime.capabilities['service-binding'].runtimeProperties[0].key == 'quarkus.kubernetes-service-binding.enabled'
    assert catalog.spec.runtime.capabilities['service-binding'].runtimeProperties[0].value == '${camel.k.serviceBinding.enabled}'

    // Health properties
    assert catalog.spec.runtime.capabilities['health'].metadata.defaultLivenessProbePath == '/q/health/live'
    assert catalog.spec.runtime.capabilities['health'].metadata.defaultReadinessProbePath == '/q/health/ready'
    assert catalog.spec.runtime.capabilities['health'].metadata.defaultStartupProbePath == '/q/health/started'

    assert catalog.spec.loaders['groovy'].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.loaders['groovy'].artifactId == 'camel-quarkus-groovy-dsl'
    assert catalog.spec.loaders['groovy'].languages[0] == 'groovy'
    assert catalog.spec.loaders['groovy'].metadata['native'] == 'true'
    assert catalog.spec.loaders['groovy'].metadata['sources-required-at-build-time'] == 'true'
    assert catalog.spec.loaders['java'].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.loaders['java'].artifactId == 'camel-quarkus-java-joor-dsl'
    assert catalog.spec.loaders['java'].languages[0] == 'java'
    assert catalog.spec.loaders['java'].metadata['native'] == 'true'
    assert catalog.spec.loaders['java'].metadata['sources-required-at-build-time'] == 'true'
    assert catalog.spec.loaders['jsh'].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.loaders['jsh'].artifactId == 'camel-quarkus-jsh-dsl'
    assert catalog.spec.loaders['jsh'].languages[0] == 'jsh'
    assert catalog.spec.loaders['jsh'].metadata['native'] == 'false'
    assert catalog.spec.loaders['jsh'].metadata['sources-required-at-build-time'] == 'true'
    assert catalog.spec.loaders['kts'].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.loaders['kts'].artifactId == 'camel-quarkus-kotlin-dsl'
    assert catalog.spec.loaders['kts'].languages[0] == 'kts'
    assert catalog.spec.loaders['kts'].metadata['native'] == 'true'
    assert catalog.spec.loaders['kts'].metadata['sources-required-at-build-time'] == 'true'
    assert catalog.spec.loaders['js'].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.loaders['js'].artifactId == 'camel-quarkus-js-dsl'
    assert catalog.spec.loaders['js'].languages[0] == 'js'
    assert catalog.spec.loaders['js'].metadata['native'] == 'false'
    assert catalog.spec.loaders['js'].metadata['sources-required-at-build-time'] == null
    assert catalog.spec.loaders['xml'].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.loaders['xml'].artifactId == 'camel-quarkus-xml-io-dsl'
    assert catalog.spec.loaders['xml'].languages[0] == 'xml'
    assert catalog.spec.loaders['xml'].metadata['native'] == 'true'
    assert catalog.spec.loaders['xml'].metadata['sources-required-at-build-time'] == null
    assert catalog.spec.loaders['yaml'].groupId == 'org.apache.camel.quarkus'
    assert catalog.spec.loaders['yaml'].artifactId == 'camel-quarkus-yaml-dsl'
    assert catalog.spec.loaders['yaml'].languages[0] == 'yaml'
    assert catalog.spec.loaders['yaml'].metadata['native'] == 'true'
    assert catalog.spec.loaders['yaml'].metadata['sources-required-at-build-time'] == null

    assert catalog.metadata.labels['camel.apache.org/runtime.version'] == runtimeVersion

    catalog.spec.artifacts['camel-k-master'].with {
        schemes == null
    }
    catalog.spec.artifacts['camel-k-cron'].with {
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

    catalog.spec.artifacts.each { k,v ->
        assert k != null
        assert v.groupId != null
        assert v.artifactId != null
        assert v.version == null
    }

    catalog.spec.artifacts['camel-quarkus-knative'].with {
        assert dependencies == null
        assert requiredCapabilities == null
        assert schemes.size() == 1
    }
}
