/*
 * Copyright to the original author or authors.
 * Copyright 2015 Sorcersoft.com
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
package sorcer.netlet.util;

import edu.emory.mathcs.util.classloader.URIClassLoader;
import net.jini.loader.ClassAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

/**
 * The ServiceClassLoader overrides getURLs(), ensuring all classes that need to
 * be annotated with specific location(s) are returned appropriately
 *
 * @author Rafał Krupiński
 */
public class NetletClassLoader extends URIClassLoader implements ClassAnnotation {
    private URI[] searchPath;
    /**
     * The ClassAnnotator to use
     */
    private ClassAnnotator annotator;
    /**
     * Meta data associated with the classloader
     */
    private Properties metaData = new Properties();

    protected URL[] codebase;

    /**
     * Constructs a new ServiceClassLoader for the specified URLs having the
     * given parent. The constructor takes two sets of URLs. The first setValue is
     * where the class loader loads classes from, the second setValue is what it
     * returns when getURLs() is called.
     *
     * @param searchPath Array of URIs to search for classes
     * @param parent     Parent ClassLoader to delegate to
     * @param metaData   Optional meta data associated with the classloader
     */
    public NetletClassLoader(URI[] searchPath,
                             URL[] codebase,
                             ClassLoader parent,
                             Properties metaData) {
        super(searchPath, parent);
        this.codebase = codebase;
        this.annotator = new ClassAnnotator();
        this.searchPath = searchPath;
        if (metaData != null)
            this.metaData.putAll(metaData);
    }

    /**
     * Get the {@link ClassAnnotator} created at construction
     * time
     *
     * @return The ClassAnnotator
     */
    public ClassAnnotation getClassAnnotator() {
        return annotator;
    }

    /**
     * Get the meta data associated with this classloader
     *
     * @return A Properties object representing any meta data associated with
     * this classloader. A new Properties object is created each time
     */
    public Properties getMetaData() {
        return new Properties(metaData);
    }

    /**
     * Add meta data associated with the classloader
     *
     * @param metaData Properties to associate to this classloader. If the
     *                 property already exists in the managed metaData, it will be replaced.
     *                 New properties will be added. A null parameter will be ignored.
     */
    public void addMetaData(Properties metaData) {
        if (metaData == null)
            return;
        this.metaData.putAll(metaData);
    }

    /**
     * Get the URLs to be used for class annotations as determined by the
     * {@link ClassAnnotator}
     */
    public URL[] getURLs() {
        return codebase;
    }

    /**
     * Get the search path of URLs for loading classes and resources
     *
     * @return The array of <code>URL[]</code> which corresponds to the search
     * path for the class loader; that is, the array elements are the locations
     * from which the class loader will load requested classes.
     * @throws MalformedURLException If any of the URis cannot be transformed
     *                               to URLs
     */
    public URL[] getSearchPath() throws MalformedURLException {
        URL[] urls;
        if (searchPath != null) {
            urls = new URL[searchPath.length];
            for (int i = 0; i < urls.length; i++)
                urls[i] = searchPath[i].toURL();
        } else {
            urls = new URL[0];
        }
        return (urls);
    }

    /**
     * Appends the specified URLs to the list of URLs to search for classes and
     * resources.
     *
     * @param urls The URLs to add
     */
    public void addURLs(URL[] urls) {
        URI[] uris = new URI[0];
        try {
            uris = getURIs(urls);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        for (URI uri : uris)
            super.addURI(uri);
    }

    public void setCodebase(URL[] codebase) {
        this.codebase = codebase;
        annotator.setAnnotationURLs();
    }

    /**
     * Get the class annotations as determined by the
     * {@link ClassAnnotator}
     *
     * @see ClassAnnotation#getClassAnnotation
     */
    public String getClassAnnotation() {
        return (annotator.getClassAnnotation());
    }

    /**
     * Returns a String representation of this class loader.
     */
    public String toString() {
        return (NetletClassLoader.class.getName() + " " +
                "ClassPath : [" + urisToPath(searchPath) + "] " +
                "Codebase : [" + getClassAnnotation() + "]");
    }

    /**
     * Convert a <code>URL[]</code> into a <code>URI[]</code>
     *
     * @param urls Array of URLs to convert
     * @return Converted array of URIs
     * @throws URISyntaxException If there are errors converting the URLs to
     *                            URIs
     */
    public static URI[] getURIs(URL[] urls) throws URISyntaxException {
        if (urls == null)
            throw new IllegalArgumentException("urls array must not be null");
        URI[] uris = new URI[urls.length];
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].getProtocol().equals("file")) {
                File f = new File(urls[i].getFile());
                if (f.getAbsolutePath().contains("%20")) {
                    String path = f.getAbsolutePath().replaceAll("%20", " ");
                    f = new File(path);
                }
                uris[i] = f.toURI();
            } else {
                uris[i] = urls[i].toURI();
            }
        }
        return (uris);
    }

    /**
     * Provides support to annotate classes required for dynamic class loading
     * in RMI
     *
     * @author Dennis Reedy
     * @see net.jini.loader.ClassAnnotation
     */
    public class ClassAnnotator implements ClassAnnotation {
        private final Logger logger = LoggerFactory.getLogger(ClassAnnotator.class);

        /**
         * Codebase annotation
         */
        private String exportAnnotation;

        public ClassAnnotator() {
            setAnnotationURLs();
        }

        /**
         * Get the codebase URLs used for class annotations.
         *
         * @return The codebase URLs required for class annotations
         */
        public URL[] getURLs() {
            URL[] urls = null;
            if (codebase != null) {
                urls = new URL[codebase.length];
                System.arraycopy(codebase, 0, urls, 0, urls.length);
                if (logger.isTraceEnabled()) {
                    logger.trace("URLs: {}", Arrays.toString(urls));
                }
            }
            return (urls);
        }

        /**
         * Replace the URLs used for class annotations.
         */
        public void setAnnotationURLs() {
            this.exportAnnotation = urlsToPath(codebase);
        }

        /**
         * @see net.jini.loader.ClassAnnotation#getClassAnnotation
         */
        public String getClassAnnotation() {
            if (logger.isTraceEnabled()) {
                logger.info("Annotation: {}", exportAnnotation);
            }
            return exportAnnotation;
        }

        /**
         * Utility method that converts a <code>URL[]</code> into a corresponding,
         * space-separated string with the same array elements. Note that if the
         * array has zero elements, the return eval is the empty string.
         *
         * @param urls An array of URLs that are to be converted
         * @return A space-separated string of each URL provided
         */
        public String urlsToPath(URL[] urls) {
            if (urls.length == 0) {
                return ("");
            } else if (urls.length == 1) {
                return (urls[0].toExternalForm());
            } else {
                StringBuilder path = new StringBuilder(urls[0].toExternalForm());
                for (int i = 1; i < urls.length; i++) {
                    path.append(' ');
                    path.append(urls[i].toExternalForm());
                }
                return (path.toString());
            }
        }


    }

    /**
     * Utility method that converts a <code>URI[]</code> into a corresponding,
     * space-separated string with the same array elements. Note that if the
     * array has zero elements, the return eval is the empty string.
     *
     * @param uris An array of URIs that are to be converted
     * @return A space-separated string of each URI provided
     */
    protected static String urisToPath(URI[] uris) {
        if (uris.length == 0) {
            return ("");
        } else if (uris.length == 1) {
            return (uris[0].toString());
        } else {
            StringBuilder path = new StringBuilder(uris[0].toString());
            for (int i = 1; i < uris.length; i++) {
                path.append(' ');
                path.append(uris[i].toString());
            }
            return (path.toString());
        }
    }
}
