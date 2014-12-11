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

import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscovery;
import net.jini.lookup.entry.Name;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.Provider;
import sorcer.core.signature.NetSignature;
import sorcer.service.Accessor;
import sorcer.service.DynamicAccessor;
import sorcer.service.Service;
import sorcer.service.Signature;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

/**
 * ProviderLoactor is a simple wrapper class over Jini's LookupDiscover. It
 * which returns the first matching instance of a service either via unicast or
 * multicast discovery
 */

public class ProviderLocator implements DynamicAccessor, SorcerConstants {

	static final long WAIT_FOR = Sorcer.getLookupWaitTime();

	static final int MAX_TRIES = 5;

	static final private Logger logger = Log.getTestLog();

	private int tries = 0;

	static {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
	}

	private Object _proxy;
	private Object _lock = new Object();
	private ServiceTemplate _template;

	public static void init() {
		Accessor.setAccessor(new ProviderLocator());
	}

	/**
	 * Locates a service via Unicast discovery
	 * 
	 * @param lusHost
	 *            The name of the host where a Jini lookup service is running
	 * @param serviceClass
	 *            The class object representing the interface of the service
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @return The proxy to the discovered service
	 */
	public static Object getService(String lusHost, Class serviceClass)
			throws java.net.MalformedURLException, java.io.IOException,
			ClassNotFoundException {

		LookupLocator loc = new LookupLocator("jini://" + lusHost);
		ServiceRegistrar reggie = loc.getRegistrar();
		ServiceTemplate tmpl = new ServiceTemplate(null,
				new Class[] { serviceClass }, null);
		return reggie.lookup(tmpl);

	}

	/**
	 * Locates a service via Unicast discovery
	 * 
	 * @param lusHost
	 * @param serviceClass
	 * @param serviceName
	 * @return proxy or <code>null</code>
	 * @throws java.net.MalformedURLException
	 * @throws java.io.IOException
	 * @throws ClassNotFoundException
	 */
	public static Object getService(String lusHost, Class serviceClass,
			String serviceName) throws java.net.MalformedURLException,
			java.io.IOException, ClassNotFoundException {

		Class[] types = new Class[] { serviceClass };
		Entry[] entry = null;

		if (serviceName != null) {
			entry = new Entry[] { new Name(serviceName) };
		}

		ServiceTemplate _template = new ServiceTemplate(null, types, entry);
		LookupLocator loc = new LookupLocator("jini://" + lusHost);
		ServiceRegistrar reggie = loc.getRegistrar();

		return reggie.lookup(_template);
	}

	/**
	 * Locates the first matching service via multicast discovery
	 * 
	 * @param serviceClass
	 *            The class object representing the interface of the service
	 * @throws IOException
	 * @throws InterruptedException
	 * @return
	 */
	public static Object getService(Class serviceClass)
			throws java.io.IOException, InterruptedException {

		return getService(serviceClass, null, Long.MAX_VALUE);
	}

	/**
	 * Locates the first matching service via multicast discovery;
	 * for compatibility with other provider accessors.
	 * 
	 * @param serviceClass
	 *            The class object representing the interface of the service
	 * @throws IOException
	 * @throws InterruptedException
	 * @return
	 */
	public static Provider getProvider(Class serviceClass)
			throws java.io.IOException, InterruptedException {

		return (Provider)getService(serviceClass, null, Long.MAX_VALUE);
	}

	/**
	 * Locates the first matching service via multicast discovery
	 * 
	 * @param serviceClass
	 *            The class object representing the interface of the service
	 * @param waitTime
	 *            How to wait for the service to be discovered
	 * @throws IOException
	 * @throws InterruptedException
	 * @return
	 */
	public static Object getService(Class serviceClass, long waitTime)
			throws java.io.IOException, InterruptedException {

		return getService(serviceClass, null, waitTime);
	}

	/**
	 * Locates the first matching service via multicast discovery
	 * 
	 * @param serviceClass
	 *            The class object representing the interface of the service
	 * @param serviceName
	 *            The Name attribute of the service
	 * @throws IOException
	 * @throws InterruptedException
	 * @return
	 */
	public static Object getService(Class serviceClass, String serviceName,
			long waitTime) throws java.io.IOException, InterruptedException {

		ProviderLocator sl = new ProviderLocator();
		return sl.getServiceImpl(serviceClass, serviceName, waitTime);
	}

	private Object getServiceImpl(Class serviceClass, String serviceName,
			long waitTime) throws java.io.IOException, InterruptedException {

		Class[] types = new Class[] { serviceClass };
		Entry[] entry = null;

		if (serviceName != null) {
			entry = new Entry[] { new Name(serviceName) };
		}

		_template = new ServiceTemplate(null, types, entry);

		LookupDiscovery disco = new LookupDiscovery(LookupDiscovery.ALL_GROUPS);

		disco.addDiscoveryListener(new Listener());

		synchronized (_lock) {
			_lock.wait(waitTime);
		}

		disco.terminate();
		if (_proxy == null) {
			throw new InterruptedException("Service not found within wait time");
		}
		return _proxy;

	}

	class Listener implements DiscoveryListener {
		// invoked when a LUS is discovered
		public void discovered(DiscoveryEvent ev) {
			ServiceRegistrar[] reg = ev.getRegistrars();
			for (int i = 0; i < reg.length && _proxy == null; i++) {
				findService(reg[i]);
			}
		}

		public void discarded(DiscoveryEvent ev) {
		}
	}

	private void findService(ServiceRegistrar lus) {

		try {
			synchronized (_lock) {
				_proxy = lus.lookup(_template);
				if (_proxy != null) {
					_lock.notifyAll();
				}
			}
		} catch (RemoteException ex) {
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * Returns a list of groups as defined in the SORCER environment
	 * configuration, the sorcer.env file.
	 * 
	 * @return a list of group names
	 * @see Sorcer
	 */
	protected static String[] getGroups() {
		return Sorcer.getLookupGroups();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * sorcer.service.DynamicAccessor#getServiceItem(sorcer.service.Signature)
	 */
	@Override
	public ServiceItem getServiceItem(Signature signature) {
			//throws SignatureException {
		return null;
		//throw new SignatureException("Not implemented by this service accessor");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.DynamicAccessor#getService(sorcer.service.Signature)
	 */
	@Override
	public Service getService(Signature signature) {
		return getProvider(signature);
	}
	
	public static Provider getProvider(Signature signature) {
		Object proxy = null;
		try {
			if (((NetSignature)signature).isUnicast()) {
				String[] locators = Sorcer.getLookupLocators();
				for (String locator : locators) {
					proxy = (Service) ProviderLocator.getService(locator,
							signature.getServiceType(), signature
									.getProviderName());
					if (proxy != null && proxy instanceof Service)
						break;
					else
						continue;
				}
			} else {
				proxy = ProviderLocator.getService(signature.getServiceType(),
						signature.getProviderName().equals(ANY) 
						? null : signature.getProviderName(), WAIT_FOR);
			}
		} catch (Exception ioe) {
			//throw new SignatureException(ioe);
			return null;
		} 
		if (proxy == null || !(proxy instanceof Service)) {
			return null;
			//throw new SignatureException("Cannot find service for: "
			//		+ signature);
		} else
			return (Provider) proxy;
	}
}
