/**
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
package org.apache.camel.k.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.URISupport;

public class PlatformStreamHandler extends URLStreamHandler {
    public static void configure() {
        URL.setURLStreamHandlerFactory(protocol -> "platform".equals(protocol) ? new PlatformStreamHandler() : null);
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new URLConnection(url) {
            @Override
            public void connect() throws IOException {
            }

            @Override
            public InputStream getInputStream() throws IOException {
                String path = url.getPath();
                String type = StringHelper.before(path, ":");
                String query = StringHelper.after(path, "?");

                if (ObjectHelper.isNotEmpty(type)) {
                    path = StringHelper.after(path, ":");
                }
                if (ObjectHelper.isNotEmpty(query)) {
                    path = StringHelper.before(path, "?");
                }

                boolean compression = hasCompression(query);

                InputStream is;

                if (type != null) {
                    switch (type) {
                    case "env":
                        is = resolveEnv(path);
                        break;
                    case "file":
                        is = resolveFile(path);
                        break;
                    case "classpath":
                        is = resolveClasspath(path);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported delegated resolver: " + type);
                    }
                } else {
                    is = resolveEnv(path);

                    if (is == null) {
                        is = resolveFile(path);
                    }
                    if (is == null) {
                        is = resolveClasspath(path);
                    }
                }

                if (is != null && compression) {
                    is = new GZIPInputStream(Base64.getDecoder().wrap(is));
                }

                return is;
            }
        };
    }

    private static InputStream resolveEnv(String path) {
        String name = path.toUpperCase();
        name = name.replace(" ", "_");
        name = name.replace(".", "_");
        name = name.replace("-", "_");

        String ref = System.getenv(name);

        if (ref == null) {
            return null;
        }

        String refType = StringHelper.before(ref, ":");
        String refName = StringHelper.after(ref, ":");
        String refQuery = StringHelper.after(refName, "?");
        boolean compression = hasCompression(refQuery);

        if (ObjectHelper.isNotEmpty(refQuery)) {
            refName = StringHelper.before(refName, "?");
        }

        InputStream is;

        switch (refType) {
        case "env":
            String content = System.getenv(refName);
            is = new ByteArrayInputStream(content.getBytes());
            break;
        case "file":
            is = resolveFile(refName);
            break;
        case "classpath":
            is = resolveClasspath(refName);
            break;
        default:
            throw new IllegalArgumentException("Unsupported delegated resolver: " + refName);
        }

        if (is != null && compression) {
            try {
                is = new GZIPInputStream(Base64.getDecoder().wrap(is));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return is;
    }

    private static InputStream resolveFile(String path) {
        Path data = Paths.get(path);

        if (!Files.exists(data)) {
            return null;
        }

        try {
            return Files.newInputStream(data);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static InputStream resolveClasspath(String path) {
        return ObjectHelper.loadResourceAsStream(path);
    }

    private static boolean hasCompression(String query) {
        try {
            Map<String, Object> params = URISupport.parseQuery(query);
            return Boolean.valueOf((String) params.get("compression"));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
