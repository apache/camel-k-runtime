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
def catalogVersion = '3.0.0-RC1'
def runtimeVersion = '1.0.2-SNAPSHOT'

def source  = new File(basedir, "camel-catalog-${catalogVersion}-${runtimeVersion}.yaml")
def catalog = new org.yaml.snakeyaml.Yaml().load(new FileInputStream(source))

assert catalog.spec.version == catalogVersion
assert catalog.spec.runtimeVersion == runtimeVersion
assert catalog.metadata.labels['camel.apache.org/catalog.version'] == catalogVersion
assert catalog.metadata.labels['camel.apache.org/runtime.version'] == runtimeVersion