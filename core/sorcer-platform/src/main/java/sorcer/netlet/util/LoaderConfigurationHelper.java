package sorcer.netlet.util;
/**
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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

import org.rioproject.resolver.Artifact;
import sorcer.resolver.*;
import sorcer.resolver.SorcerResolver;
import sorcer.resolver.SorcerResolverException;
import sorcer.util.JavaSystemProperties;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used by LoaderConfiguration and ScriptThread to load jars from path
 * User: Pawel.Rubach
 * Date: 06.06.13
 * Time: 01:56
 * To change this template use File | Settings | File Templates.
 */
public class LoaderConfigurationHelper {

    private static final char WILDCARD = '*';
    public static final String LOAD_PREFIX = "load";
    public static final String CODEBASE_PREFIX = "codebase";
    static final Logger logger = LoggerFactory.getLogger(LoaderConfigurationHelper.class.getName());
    private static SorcerResolver sorcerResolver = SorcerResolver.getInstance();

    public static List<URL> load(String str) {
        List<URL> urlsList = new ArrayList<URL>();
        URI uri;
        str = assignProperties(str);

        //first check for simple artifact coordinates

        if (Artifact.isArtifact(str))
            try {
                URL[] classpath = SorcerRioResolver.toURLs(sorcerResolver.doResolve(str));
                Collections.addAll(urlsList, classpath);
            } catch (SorcerResolverException e) {
                logger.error("Could not resolve " + str, e);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        else {

        try {
            uri = new URI(str);
        } catch (URISyntaxException e) {
            logger.error( "Error while parsing URL " + str, e);
            return urlsList;
        }
        String scheme = uri.getScheme();
        if ("http".equals(scheme)) {
            try {
                urlsList.add(new URL(str));
            } catch (MalformedURLException e) {
                logger.error("Problem creating URL: " + str);
            }
        } else if ("artifact".equals(scheme)) {
                try {
                    urlsList.add(uri.toURL());
                    URL[] classpath = SorcerRioResolver.toURLs(sorcerResolver.doResolve(uri.toString()));
                    Collections.addAll(urlsList, classpath);
                } catch (SorcerResolverException e) {
                    logger.error( "Could not resolve " + str, e);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
        } else if ("file".equals(scheme) || scheme==null || (new File(str).exists())) {
            return getFilesFromFilteredPath(str);
        }
        }
        return urlsList;
    }

    public static List<URL> setCodebase(List<String> codebaseLines, PrintStream out) {
        String curCodebase = System.getProperty(JavaSystemProperties.RMI_SERVER_CODEBASE);
        StringBuilder codebaseSb = new StringBuilder();
        if (curCodebase!=null) codebaseSb.append(curCodebase);
        List<URL> codebaseUrls = new ArrayList<URL>();
        for (String codebaseStr : codebaseLines) {
            if (codebaseStr.startsWith(LoaderConfigurationHelper.CODEBASE_PREFIX))
                codebaseStr = codebaseStr.substring(LoaderConfigurationHelper.CODEBASE_PREFIX.length()).trim();
            if ((!codebaseStr.startsWith("http://")) && (!codebaseStr.startsWith("artifact:"))) {
                if (out!=null) out.println("Codebase can only be specified using http:// or artifact:");
                else logger.error("Codebase can only be specified using mvn://, http:// or artifact:");
                return null;
            }
            if (codebaseStr != null) {
                try {
                    codebaseUrls.add(new URL(codebaseStr));
                } catch (MalformedURLException me) {
                    if (out != null) out.println("Codebase url is malformed: " + me.getMessage());
                    else logger.error("Codebase url is malformed: " + me.getMessage());
                }
                codebaseSb.append(" ").append(codebaseStr);
            }
        }
        System.setProperty(JavaSystemProperties.RMI_SERVER_CODEBASE, codebaseSb.toString());
        return codebaseUrls;
    }

    public static boolean existRemoteFile(URL url) {
        try {
            HttpURLConnection huc =  ( HttpURLConnection ) url.openConnection();
            huc.setRequestMethod("HEAD");
            if (huc.getResponseCode() == HttpURLConnection.HTTP_OK)
                return true;
        } catch (ProtocolException e) {
            logger.error("Problem with protocol while loading URL to classpath: " + url.toString() + "\n" + e.getMessage());
        } catch (IOException e) {
            logger.error("Problem adding remote file to classpath, file does not exist: " + url.toString() + "\n" + e.getMessage());
        }
        return false;
    }

    /*
       * Expands the properties inside the given string to it's values.
       */
    public static String assignProperties(String str) {
        int propertyIndexStart = 0, propertyIndexEnd = 0;
        boolean requireProperty;
        String result = "";

        while (propertyIndexStart < str.length()) {
            {
                int i1 = str.indexOf("${", propertyIndexStart);
                int i2 = str.indexOf("!{", propertyIndexStart);
                if (i1 == -1) {
                    propertyIndexStart = i2;
                } else if (i2 == -1) {
                    propertyIndexStart = i1;
                } else {
                    propertyIndexStart = Math.min(i1, i2);
                }
                requireProperty = propertyIndexStart == i2;
            }
            if (propertyIndexStart == -1) break;
            result += str.substring(propertyIndexEnd, propertyIndexStart);

            propertyIndexEnd = str.indexOf("}", propertyIndexStart);
            if (propertyIndexEnd == -1) break;

            String propertyKey = str.substring(propertyIndexStart + 2, propertyIndexEnd);
            String propertyValue;

            propertyValue = System.getProperty(propertyKey);

            // assume properties contain paths
            if (propertyValue == null) {
                if (requireProperty) {
                    throw new IllegalArgumentException("Variable " + propertyKey + " in nsh.config references a non-existent System property! Try passing the property to the VM using -D" + propertyKey + "=myValue in JAVA_OPTS");
                } else {
                    return null;
                }
            }
            propertyValue = getSlashyPath(propertyValue);
            propertyValue = correctDoubleSlash(propertyValue,propertyIndexEnd,str);
            result += propertyValue;

            propertyIndexEnd++;
            propertyIndexStart = propertyIndexEnd;
        }

        if (propertyIndexStart == -1 || propertyIndexStart >= str.length()) {
            result += str.substring(propertyIndexEnd);
        } else if (propertyIndexEnd == -1) {
            result += str.substring(propertyIndexStart);
        }

        return result;
    }


    /**
     * Get files to load a possibly filtered path. Filters are defined
     * by using the * wildcard like in any shell.
     */
    public static List<URL> getFilesFromFilteredPath(String filter) {
        List<URL> filesToLoad = new ArrayList<URL>();
        if (filter == null) return null;
        filter = getSlashyPath(filter);
        int starIndex = filter.indexOf(WILDCARD);
        if (starIndex == -1) {
            try {
                filesToLoad.add(new File(filter).toURI().toURL());
            } catch (MalformedURLException e) {
                logger.error("Problem converting file to URL: " + e.getMessage());
            }
            //addFile(new File(filter));
            return filesToLoad;
        }

        return filesToLoad;
    }

    private static String correctDoubleSlash(String propertyValue, int propertyIndexEnd, String str) {
        int index = propertyIndexEnd+1;
        if ( index<str.length() && str.charAt(index)=='/' &&
                propertyValue.endsWith("/") &&
                propertyValue.length()>0)
        {
            propertyValue = propertyValue.substring(0,propertyValue.length()-1);
        }
        return propertyValue;
    }

    // change path representation to something more system independent.
    // This solution is based on an absolute path
    private static String getSlashyPath(final String path) {
        String changedPath = path;
        if (File.separatorChar != '/')
            changedPath = changedPath.replace(File.separatorChar, '/');

        return changedPath;
    }

}
