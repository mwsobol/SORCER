/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class SOS {
    /**
     * General database prefix for the SORCER database schema.
     */
    protected static String dbPrefix = "SOS";
    protected static String sepChar = ".";

    /** Port for a code sever (webster) */
    protected static int port = 0;

    /**
     * Default key 'sorcer.env' for a file defining global environment
     * properties.
     */
    public static String SORCER_ENV_FILENAME = "sorcer.env";

    /**
     * Default key 'data.formats' for a file defining service context node
     * types.
     */
    protected static String CONTEXT_DATA_FORMATS = "data.formats";

    /**
     * Default key 'provider.properties' for a file defining provider
     * properties.
     */
    public static String PROVIDER_PROPERTIES_FILENAME = "provider.properties";

    /**
     * Default key 'servid.per' for a file storing a service registration ID.
     */
    protected static String serviceIdFilename = "servid.per";

    private static String sorcerVersion;

    private static final Logger logger = LoggerFactory.getLogger(SOS.class.getName());

    /**
     * <p>
     * Return the current version of the SORCER system.
     * </p>
     */
    public static String getSorcerVersion() {
        if(sorcerVersion==null) {
            Class clazz = SOS.class;
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();
             /* Make sure we are loaded from a JAR */
            if (classPath.startsWith("jar:file:")) {
                String jarPath =  classPath.substring("jar:file:".length(), classPath.lastIndexOf("!"));
                try {
                    JarFile jarFile = new JarFile(jarPath);
                    Manifest manifest = jarFile.getManifest();
                    Attributes attrs = manifest.getMainAttributes();
                    sorcerVersion = attrs.getValue("Implementation-Version");
                    logger.info("sorcerVersion: {}", sorcerVersion);
                } catch (Exception e) {
                    logger.warn("Unable to read MANIFEST {}: {}", e.getClass().getName(), e.getMessage());
                }
            }
        }
        return sorcerVersion==null?"unknown":sorcerVersion;
    }

    public static String deriveSorcerHome() {
        String sorcerHome = null;
        getSorcerVersion();
        if(sorcerVersion!=null) {
            Class clazz = SOS.class;
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();
            /* Make sure we are loaded from a JAR */
            if (classPath.startsWith("jar")) {
                String path = classPath.substring("jar:".length(), classPath.lastIndexOf("!"));
                logger.info("Loading " + path);
                File sorcerPlatformJar = new File(path);
                File directory = sorcerPlatformJar.getParentFile();
                while(directory!=null && !directory.getName().endsWith(sorcerVersion)) {
                    directory = directory.getParentFile();
                }
                sorcerHome = directory!=null?directory.getPath():null;
                logger.info("SORCER_HOME: "+sorcerHome);
            }
        }
        return sorcerHome;
    }

}
