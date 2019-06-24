/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.util;

import net.jini.config.Configuration;
import net.jini.config.EmptyConfiguration;
import sorcer.service.Exerter;
import sorcer.service.Service;
import sorcer.service.Signature;
import sorcer.service.SignatureException;

/**
 * A class which supports a simple Jini multicast lookup. It doesn't register
 * with any ServiceRegistrars it simply interrogates each one that's discovered
 * for a ServiceItem associated with the passed multitype/signature of a provider.
 */
public class ProviderLookup extends ServiceAccessor {

	public ProviderLookup() {
		this(EmptyConfiguration.INSTANCE);
	}

    public ProviderLookup(Configuration config) {
        super(config);
    }

    /**
     * Returns a SORCER service provider with the specified signature, using
     * a Cataloger if available, otherwise using Jini lookup services.
     *
     * @param signature a provider signature
     * @return a SORCER service provider
     */
    public Exerter getProvider(Signature signature) throws SignatureException {
        return (Exerter) getService(signature);
    }

	/**
	 * Returns a service provider with the specified service multitype.
	 * 
	 * @param serviceType
	 *            a provider service multitype (interface)
	 * @return a service provider
	 */
	public Object getService(Class serviceType) {
		return getService(null, serviceType);
	}

	/**
	 * Returns a SORCER service provider with the specified key and service
	 * multitype.
	 * 
	 * @param providerName
	 *            the key of service provider
	 * @param serviceType
	 *            a provider service multitype (interface)
	 * @return a SORCER service provider
	 */
	public Service getProvider(String providerName, String serviceType) {
		return (Service) getService(providerName, serviceType);
	}

	/**
	 * Returns a service provider with the specified key and service multitype.
	 *
	 * @param providerName
	 *            The key of the provider to search for
	 * @param serviceType
	 *            The interface to look for
	 *
	 * @return a service provider
	 */
	public Object getService(String providerName, String serviceType) {
		Class type;
		try {
			type = Class.forName(serviceType);
		} catch (ClassNotFoundException cnfe) {
			//logger.error("ProviderLookup", "selectService", cnfe);
			return null;
		}
		return getService(providerName, type);
	}

	/**
	 * Returns a SORCER service provider with the specified service multitype, using
	 * a Cataloger if availabe, otherwise using Jini lookup services.
	 * 
	 * @param serviceType
	 *            a provider service multitype (interface)
	 * @return a SORCER service provider
	 */
	public Service getProvider(String serviceType) {
		return getProvider(null, serviceType);
	}

    public <T> T getProvider (String name, Class<T> type) {
        return getService(name, type);
	}

}
