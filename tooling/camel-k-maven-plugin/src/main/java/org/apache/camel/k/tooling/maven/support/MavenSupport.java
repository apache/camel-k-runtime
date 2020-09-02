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
package org.apache.camel.k.tooling.maven.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.maven.project.MavenProject;

public final class MavenSupport {
    private MavenSupport() {
    }

    public static ClassLoader getClassLoader(MavenProject project) {
        if (project == null) {
            return IndexerSupport.class.getClassLoader();
        }

        try {
            List<String> elements = new ArrayList<>(project.getCompileClasspathElements());
            URL[] urls = new URL[elements.size()];
            for (int i = 0; i < elements.size(); ++i) {
                urls[i] = new File(elements.get(i)).toURI().toURL();
            }
            return new URLClassLoader(urls, IndexerSupport.class.getClassLoader());
        } catch (Exception e) {
            return IndexerSupport.class.getClassLoader();
        }
    }

    public static String getVersion(Class<?> clazz, String path) {
        String version = null;

        // try to load from maven properties first
        try (InputStream is = clazz.getResourceAsStream(path)) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                version = p.getProperty("version", "");
            }
        } catch (Exception ignored) {
        }

        // fallback to using Java API
        if (version == null) {
            Package aPackage = clazz.getPackage();
            if (aPackage != null) {
                version = getVersion(aPackage);
            }
        }

        if (version == null) {
            // we could not compute the version so use a blank
            throw new IllegalStateException("Unable to determine runtime version");
        }

        return version;
    }

    public static String getVersion(Package pkg) {
        String version = pkg.getImplementationVersion();
        if (version == null) {
            version = pkg.getSpecificationVersion();
        }

        return version;
    }

    public static void getVersion(Class<?> clazz, String path, Consumer<String> consumer) {
        consumer.accept(
            MavenSupport.getVersion(clazz, path)
        );
    }

    public static void getVersion(Class<?> clazz, String groupId, String artifactId, Consumer<String> consumer) {
        getVersion(
            clazz,
            String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId),
            consumer);
    }

    public static String getVersion(Class<?> clazz, String groupId, String artifactId) {
        return MavenSupport.getVersion(
            clazz,
            String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId));
    }

    public static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }

            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static String sha1Hex(InputStream is) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        byte[] buffer = new byte[1024];
        for(int read = is.read(buffer, 0, 1024); read > -1; read = is.read(buffer, 0, 1024)) {
            digest.update(buffer, 0, read);
        }

        return bytesToHex(digest.digest());
    }
}
