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

import sorcer.service.Exerter;
import sorcer.core.provider.ServiceExerter;
import sorcer.service.Accessor;

import java.io.File;

/**
 * For compatibility with the version of sorcersoft.com Not needed class as the
 * methods of ProviderUtil that are in SORCEREnv/Sorcer define the system
 * functionality not a provider!
 */
public class ProviderUtil extends GenericUtil {
	private ProviderUtil() {
		super();
	}

    public static void destroy(String providerName, Class serviceType) {
        Exerter prv = (Exerter) Accessor.create().getService(providerName, serviceType);
        if (prv != null)
            try {
                prv.destroy();
            } catch (Throwable t) {
                // a dead provider will be not responding anymore
                // t.printStackTrace();
            }
    }

    public static void destroyNode(String providerName, Class serviceType) {
        Exerter prv = (Exerter) Accessor.create().getService(providerName, serviceType);
        if (prv != null)
            try {
                prv.destroyNode();
            } catch (Throwable t) {
                // a dead provider will be not responding anymore
                // t.printStackTrace();
            }
    }

    public static void checkFileExistsAndIsReadable(File file, ServiceExerter sp) {
        GenericUtil.checkFileExistsAndIsReadable(file, sp);
    }
}
