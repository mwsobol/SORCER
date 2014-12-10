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

import java.util.ArrayList;
import java.util.List;

import net.jini.core.entry.Entry;
import net.jini.lookup.entry.ServiceInfo;

/**
 * @author Mike Sobolewski
 * 
 */
public class VersionInfo {
	public static final String PRODUCT_NAME = "SORCER Runtime Environment";
	public static final String EMAIL_CONTACT = "sobol@sorcersoft.org";
	public static final String SUPPLIER_NAME = "SORCERsoft.org";
	public static final String VERSION = "13.7.0";
	public static final String MODEL = "MSTC/AFRL/WPAFB";

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

}
