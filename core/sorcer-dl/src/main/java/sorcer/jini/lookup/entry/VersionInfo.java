/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

package sorcer.jini.lookup.entry;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import net.jini.core.entry.Entry;
import net.jini.lookup.entry.ServiceInfo;
import sorcer.core.SorcerConstants;


/**
 * @author Mike Sobolewski
 * 
 */
public class VersionInfo {
	public static final String PRODUCT_NAME = "SORCER Runtime Environment";
	public static final String EMAIL_CONTACT = "sobol@sorcersoft.org";
	public static final String SUPPLIER_NAME = "SORCERsoft.org";
	public static final String VERSION = getSorcerVersion();
	public static final String MODEL = "MSTC/AFRL/WPAFB";
    private static String sorcerVersion;

    public static String getVersion() {
        return PRODUCT_NAME + ", " + EMAIL_CONTACT + ", " + SUPPLIER_NAME
                + ", " + VERSION + ", "
                + System.getProperty("java.runtime.name") + " v."
                + System.getProperty("java.runtime.version");
    }

    /**
     * Create the service owned attributes for a SORCER service provider
     */
    public static List<Entry> productAttributesFor(String serviceName) {
        String name = serviceName == null || serviceName.length() == 0 ? PRODUCT_NAME : serviceName;
        final Entry info = new ServiceInfo(name,
                VersionInfo.EMAIL_CONTACT, VersionInfo.SUPPLIER_NAME,
                VersionInfo.VERSION, MODEL, "");

        final Entry type = new com.sun.jini.lookup.entry.BasicServiceType(name);

        List<Entry> al = new ArrayList<Entry>(2);
        al.add(info);
        al.add(type);
        return al;
	}

    public static String getSorcerVersion() {
        if(sorcerVersion==null) {
            Class clazz = VersionInfo.class;
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();
             /* Make sure we are loaded from a JAR */
            if (classPath.startsWith("jar")) {
                 /* Replace the class name with the MANIFEST */
                String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
                        "/META-INF/MANIFEST.MF";
                try {
                    URL url = new URL(manifestPath);
                    Manifest manifest = new Manifest(url.openStream());
                    Attributes attrs = manifest.getMainAttributes();
                    sorcerVersion = attrs.getValue("Implementation-Version");
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read MANIFEST", e);
                }
            }
        }
        return sorcerVersion==null?"unknown":sorcerVersion;
    }

}
