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
def source  = new File(basedir, "catalog.yaml")
def catalog = new org.yaml.snakeyaml.Yaml().load(new FileInputStream(source))

assert catalog.spec.runtime.version == runtimeVersion
assert catalog.spec.runtime.applicationClass == 'org.apache.camel.k.main.Application'

assert catalog.spec.runtime.capabilities['health'].dependencies[0].groupId == 'org.apache.camel.k'
assert catalog.spec.runtime.capabilities['health'].dependencies[0].artifactId == 'camel-k-runtime-health'
assert catalog.spec.runtime.capabilities['rest'].dependencies[0].groupId == 'org.apache.camel'
assert catalog.spec.runtime.capabilities['rest'].dependencies[0].artifactId == 'camel-rest'
assert catalog.spec.runtime.capabilities['rest'].dependencies[1].groupId == 'org.apache.camel.k'
assert catalog.spec.runtime.capabilities['rest'].dependencies[1].artifactId == 'camel-k-runtime-http'

assert catalog.metadata.labels['camel.apache.org/runtime.version'] == runtimeVersion

assert catalog.spec.artifacts['camel-knative'].dependencies.size == 3
assert catalog.spec.artifacts['camel-knative'].dependencies.find { it.groupId == 'org.apache.camel.k' && it.artifactId == 'camel-knative-api'}
assert catalog.spec.artifacts['camel-knative'].dependencies.find { it.groupId == 'org.apache.camel.k' && it.artifactId == 'camel-knative'}
assert catalog.spec.artifacts['camel-knative'].dependencies.find { it.groupId == 'org.apache.camel.k' && it.artifactId == 'camel-knative-http'}

assert catalog.spec.artifacts['camel-knative'].schemes.size == 1
assert catalog.spec.artifacts['camel-knative'].schemes[0].id == 'knative'
