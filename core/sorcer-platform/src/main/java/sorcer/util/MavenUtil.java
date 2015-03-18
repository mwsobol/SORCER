package sorcer.util;
/**
 * Copyright 2013 Sorcersoft.com S.A.
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

import java.io.*;
import java.net.URL;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author Rafał Krupiński
 */
public class MavenUtil {
    /**
     * Exception source indicator.
     */
    private static boolean inside;

    /**
     * Extracts version for jar archive
     *
     * @param serviceType service class
     * @return library version
     */
    public synchronized static String findVersion(Class<?> serviceType) {
        inside = false;
        URL jar = serviceType.getProtectionDomain().getCodeSource().getLocation();
        JarInputStream zip = null;
        try {
            zip = new JarInputStream(jar.openStream());
            return handleJar(zip);
        } catch (IOException e) {
            if (inside) {
                handleException(e, jar);
            } else {
                try {
                    String fileName = jar.getFile();
                    if (fileName.startsWith("file:") && fileName.endsWith("!/")) {
                        fileName = fileName.substring(0, fileName.length() - 2);
                        fileName = fileName.replace("file:", "");
                        File file = new File(fileName);
                        zip = new JarInputStream(new FileInputStream(file));
                        handleJar(zip);
                    } else {
                        handleException(e, jar);
                    }
                } catch (IOException ee) {
                    handleException(ee, jar);
                }
            }
        }
        return null;
    }

    /**
     * Handles exception
     *
     * @param e   exception to print as stack trace
     * @param jar jar file that causes the exception
     */
    private static void handleException(Exception e, URL jar) {
        System.err.println(String.format("Unable to open '%s' jar file", jar.toString()));
        e.printStackTrace();
    }

    /**
     * Handles jar version extraction.
     *
     * @param zip jar input stream
     * @return jar version
     * @throws IOException
     */
    private static String handleJar(JarInputStream zip) throws IOException {
        inside = true;
        JarEntry entry = null;
        while ((entry = zip.getNextJarEntry()) != null) {
            String name = entry.getName();
            if (name.startsWith("META-INF/") && name.endsWith("/pom.properties") && entry.getSize()>0) {
                byte[] buf = new byte[(int) entry.getSize()];
                zip.read(buf);
                InputStream is = new ByteArrayInputStream(buf);
                Properties properties = new Properties();
                properties.load(is);
                zip.close();
                return properties.getProperty("version");
            }
        }
        return null;
    }
}
