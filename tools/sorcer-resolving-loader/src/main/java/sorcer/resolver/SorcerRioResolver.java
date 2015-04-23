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

import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.url.artifact.ArtifactURLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * @author Paweł Rubach
 */
public class SorcerRioResolver extends SorcerResolver {

    private static final Logger logger = LoggerFactory.getLogger(SorcerRioResolver.class);

    private final Resolver resolver;

    public SorcerRioResolver() {
        try {
            resolver = ResolverHelper.getResolver();
        } catch (ResolverException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String[] doResolve(String artifact) throws SorcerResolverException {
        String[] cp = null;

        if (artifact.startsWith("artifact:")) {
            cp = resolveUrl(artifact);
        } else if (artifact.indexOf(':') >= 0) {
            cp = resolveCoords(artifact);
        }
        if (cp == null || cp.length == 0)
            throw new SorcerResolverException("Failed to resolve: " + artifact + " after 5 attempts");
        return cp;
    }

    private String[] resolveUrl(String artifact) {
        String[] cp = null;
        String path = artifact.substring(artifact.indexOf(":") + 1);
        ArtifactURLConfiguration artifactURLConfiguration = new ArtifactURLConfiguration(path);
        int tries = 0;
        while (tries < 5 && (cp == null || cp.length == 0)) {
            try {
                cp = resolver.getClassPathFor(artifactURLConfiguration.getArtifact(),
                        artifactURLConfiguration.getRepositories());
            } catch (Exception e) {
                logger.warn("Failed to resolve at {} attempt: {}", tries, artifactURLConfiguration.getArtifact());
                logger.debug("Resolver error", e);
            }
            tries++;
        }
        return cp;
    }

    private String[] resolveCoords(String coords) throws SorcerResolverException {
        String[] cp = null;
        int tries = 0;
        while (tries < 5 && (cp == null || cp.length == 0)) {
            try {
                cp = resolver.getClassPathFor(coords);
            } catch (Exception e) {
                logger.warn("Failed to resolve at {} attempt: {}", tries, coords);
                logger.debug("Resolver error", e);
            }
            tries++;
        }
        return cp;
    }

    @Override
    public URL getLocation(String path) throws SorcerResolverException {
        throw new SorcerResolverException("NOT IMPLEMENTED in this Resolver");
    }
}
