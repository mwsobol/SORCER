/*
 * Copyright 2014 Sorcersoft.com S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.resolver;

import org.rioproject.resolver.RemoteRepository;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * @author Rafał Krupiński
 */
public class SorcerResolver implements Resolver {
    final protected static Logger log = LoggerFactory.getLogger(SorcerResolver.class);

    private static SorcerResolver inst;

    final Resolver resolver;

    public static synchronized Resolver getResolver() throws ResolverException {
        if (inst == null)
            inst = new SorcerResolver(ResolverHelper.getResolver());
        return inst;
    }

    public SorcerResolver(Resolver resolver) {
        this.resolver = resolver;
    }

    public static URL[] toURLs(String[] filePaths) throws MalformedURLException {
        URL[] result = new URL[filePaths.length];
        for (int i = 0; i < filePaths.length; i++) {
            result[i] = toURI(filePaths[i]).toURL();
        }
        return result;
    }

    public static URI toURI(String filePath) {
        return new File(filePath).toURI();
    }

    public static URI[] toURIs(String[] filePaths) throws URISyntaxException {
        URI[] result = new URI[filePaths.length];
        for (int i = 0; i < filePaths.length; i++) {
            result[i] = toURI(filePaths[i]);
        }
        return result;
    }

    @Override
    public String[] getClassPathFor(String artifact) throws ResolverException {
        return resolver.getClassPathFor(artifact);
    }

    @Override
    public String[] getClassPathFor(String artifact, RemoteRepository[] repositories) throws ResolverException {
        return resolver.getClassPathFor(artifact, repositories);
    }

    @Override
    public URL getLocation(String artifact, String artifactType) throws ResolverException {
        return resolver.getLocation(artifact, artifactType);
    }

    @Override
    public URL getLocation(String artifact, String artifactType, RemoteRepository[] repositories) throws ResolverException {
        return resolver.getLocation(artifact, artifactType, repositories);
    }

    @Override
    public Collection<RemoteRepository> getRemoteRepositories() {
        return resolver.getRemoteRepositories();
    }

    @Override
    @Deprecated
    public String[] getClassPathFor(String artifact, File pom, boolean download) throws ResolverException {
        return resolver.getClassPathFor(artifact, pom, download);
    }
}
