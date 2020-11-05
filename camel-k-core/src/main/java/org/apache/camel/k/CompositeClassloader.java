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
package org.apache.camel.k;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeClassloader extends ClassLoader {
    private final List<ClassLoader> loaders = new CopyOnWriteArrayList<>();

    public CompositeClassloader() {
        // no parent
    }

    public CompositeClassloader(ClassLoader parent) {
        super(parent);
    }

    public void addClassLoader(ClassLoader loader) {
        loaders.add(loader);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (ClassLoader loader: loaders) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
                // ignored
            }
        }

        return super.loadClass(name);
    }

    public static CompositeClassloader wrap(ClassLoader parent) {
        return parent != null ? new CompositeClassloader(parent) : new CompositeClassloader();
    }
}
