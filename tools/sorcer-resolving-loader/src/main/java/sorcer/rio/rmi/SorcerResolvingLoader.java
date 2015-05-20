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

package sorcer.rio.rmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.resolver.SorcerResolver;
import sorcer.resolver.SorcerResolverException;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SORCER class
 * User: prubach
 * Date: 13.03.14
 */
public class SorcerResolvingLoader extends RMIClassLoaderSpi {
    public static final String CODEBASE_SEPARATOR = " ";
    /**
     * A table of artifacts to derived codebases. This improves performance by resolving the classpath once per
     * artifact.
     */
    private final Map<String, Set<String>> artifactToCodebase = new ConcurrentHashMap<String, Set<String>>();
    /**
     * A table of classes to artifact: codebase. This will ensure that if the annotation is requested for a class that
     * has it's classpath resolved from an artifact, that the artifact URL is passed back instead of the resolved
     * (local) classpath.
     */
    private static final Logger logger = LoggerFactory.getLogger(SorcerResolvingLoader.class);
    private static SorcerResolver sorcerResolver;
    static {
        sorcerResolver = SorcerResolver.getInstance();
    }
    private static final RMIClassLoaderSpi loader = RMIClassLoader.getDefaultProviderInstance();

    @Override
    public Class<?> loadClass(final String codebase,
                              final String name,
                              final ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        if (logger.isTraceEnabled()) {
            logger.trace("codebase: {}, name: {}, defaultLoader: {}",
                    codebase, name, defaultLoader == null ? "NULL" : defaultLoader.getClass().getName());
        }
        String resolvedCodebase = resolveCodebase(codebase);
        if (logger.isTraceEnabled()) {
            logger.trace("Load class {} using codebase {}, resolved to {}", name, codebase, resolvedCodebase);
        }
        Class<?> cl = loader.loadClass(resolvedCodebase, name, defaultLoader);
        if(logger.isDebugEnabled()) {
            logger.debug("Class {} loaded by {}", name, cl.getClassLoader());
        }
        return cl;
    }

    @Override
    public Class<?> loadProxyClass(final String codebase,
                                   final String[] interfaces,
                                   final ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        if(logger.isTraceEnabled()) {
            logger.trace("Load proxy classes {}, with codebase {}, defaultLoader {}",
                    Arrays.toString(interfaces), codebase, defaultLoader==null?"NULL":defaultLoader.getClass().getName());
        }
        String resolvedCodebase = resolveCodebase(codebase);
        Class<?> proxyClass = loader.loadProxyClass(resolvedCodebase, interfaces, defaultLoader);
        if(logger.isDebugEnabled()) {
            logger.debug("Proxy classes {} loaded by {}", Arrays.toString(interfaces), proxyClass.getClassLoader());
        }
        return proxyClass;
    }

    @Override
    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        String resolvedCodebase = resolveCodebase(codebase);
        ClassLoader classLoader = loader.getClassLoader(resolvedCodebase);
        if(logger.isTraceEnabled()) {
            logger.trace("ClassLoader for codebase {}, resolved as {} is {}", codebase, resolvedCodebase, classLoader);
        }
        return classLoader;
    }

    @Override
    public String getClassAnnotation(final Class<?> aClass) {
        String loaderAnnotation = loader.getClassAnnotation(aClass);
        String artifact = null;
        if(loaderAnnotation!=null) {
            for(Map.Entry<String, Set<String>> entry : artifactToCodebase.entrySet()) {
                String resolvedCodebase = join(entry.getValue(), CODEBASE_SEPARATOR);
                if(resolvedCodebase.equals(loaderAnnotation)) {
                    artifact = entry.getKey();
                    break;
                }
            }
        }
        String annotation = artifact==null?loaderAnnotation:artifact;
        if(logger.isDebugEnabled())
            logger.debug("Annotation for {} is {}", aClass.getName(), annotation);
        return annotation;
    }

    private String resolveCodebase(final String codebase) {
        String adaptedCodebase;
        if(codebase!=null && codebase.startsWith("artifact:")) {
            String[] artifacts = codebase.split(CODEBASE_SEPARATOR);
            Set<String> jarsSet = new HashSet<String>();
            for (String artf : artifacts) {
                if (artf != null) {
                    Set<String> adaptedCodebaseSet;
                    synchronized (artf.intern()) {
                        adaptedCodebaseSet = artifactToCodebase.get(artf);
                        if (adaptedCodebaseSet == null)
                            try {
                                adaptedCodebaseSet = new HashSet<String>();
                                for (String path : sorcerResolver.doResolve(artf)) {
                                    // ignore pom files
                                    if (path.endsWith(".pom"))
                                        continue;
                                    adaptedCodebaseSet.add(new File(path).toURI().toURL().toExternalForm());
                                }
                                artifactToCodebase.put(artf, adaptedCodebaseSet);
                                logger.debug("Resolved {} to {}", artf, adaptedCodebaseSet);
                            } catch (SorcerResolverException e) {
                                logger.warn("Unable to resolve {}", artf, e);
                            } catch (MalformedURLException e) {
                                logger.warn("The codebase {} is malformed", artf, e);
                            }
                    }
                    jarsSet.addAll(adaptedCodebaseSet);
                }
            }
            adaptedCodebase = join(jarsSet, CODEBASE_SEPARATOR);
        }  else {
            adaptedCodebase = codebase;
        }
        return adaptedCodebase;
    }


    /**
     * Copied from StringUtils to avoid dependency on sorcer-platform
     */
    public static String join(Iterable<?> iterable, String separator) {
        Iterator<?> iterator = iterable.iterator();
        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return "";
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return toString(first);
        }

        // two or more elements
        StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
    }

    public static String toString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

}
