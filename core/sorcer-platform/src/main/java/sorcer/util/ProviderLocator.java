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

import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscovery;
import net.jini.lookup.ServiceItemFilter;
import net.jini.lookup.entry.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ProviderName;
import sorcer.core.provider.ServiceName;
import sorcer.core.signature.NetSignature;
import sorcer.eo.operator;
import sorcer.service.Service;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.eo.operator.ParTypes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ProviderLoactor is a simple wrapper class over Jini's LookupDiscovery. It
 * which returns the first matching instance of a service either via unicast or
 * multicast discovery with support for SORCER signatures
 */

public class ProviderLocator {

	static final long WAIT_FOR = SorcerEnv.getLookupWaitTime();

	static final int MAX_TRIES = 20;

    final private static Logger log = LoggerFactory.getLogger(ProviderLocator.class);

	private Object proxy;

	private final Object lock = new Object();

	private ServiceTemplate template;

	/**
	 * Locates a service via Unicast discovery
	 * 
	 * @param lusHost
	 *            The key of the host where a Jini lookup service is running
	 * @param serviceClass
	 *            The class object representing the interface of the service
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @return The proxy to the discovered service
	 */
	public static Object getService(String lusHost, Class serviceClass)
			throws java.io.IOException,
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
	public static Object getService(String lusHost, Class serviceClass, Class[] matchTypes,
									String serviceName) throws
			java.io.IOException, ClassNotFoundException {

		Class[] types =  new Class[] { serviceClass };
		if (matchTypes != null && matchTypes.length > 0) {
			operator.ParTypes allTypes = new ParTypes(serviceClass, matchTypes);
			types = allTypes.parameterTypes;
		}

		Entry[] entry = null;

		if (serviceName != null) {
			entry = new Entry[] { new Name(serviceName) };
		}

		ServiceTemplate template = new ServiceTemplate(null, types, entry);
		LookupLocator loc = new LookupLocator("jini://" + lusHost);
		ServiceRegistrar reggie = loc.getRegistrar();

		return reggie.lookup(template);
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

		return getService(serviceClass, null, null, null, Long.MAX_VALUE);
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

        return (Provider)getService(serviceClass, null, null, null, Long.MAX_VALUE);
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

		return getService(serviceClass, null, null, null, waitTime);
	}

	/**
	 * Locates the first matching service via multicast discovery
	 *
	 * @param serviceClass
	 *            The class object representing the interface of the service
	 * @param serviceName
	 *            The Tag attribute of the service
	 * @throws IOException
	 * @throws InterruptedException
	 * @return
	 */
	public static Object getService(Class serviceClass, Class[] matchTypes, String serviceName, String[] groups,
			long waitTime) throws java.io.IOException, InterruptedException {

		ProviderLocator sl = new ProviderLocator();
		return sl.getServiceImpl(serviceClass, matchTypes, serviceName, groups, waitTime);
	}

	private Object getServiceImpl(Class serviceClass, Class[] matchTypes, String serviceName, String[] groups,
			long waitTime) throws java.io.IOException, InterruptedException {

		Class[] types =  new Class[] { serviceClass };
		if (matchTypes != null && matchTypes.length > 0) {
			ParTypes allTypes = new ParTypes(serviceClass, matchTypes);
			types = allTypes.parameterTypes;
		}
		Entry[] entry = null;

		if (serviceName != null) {
			entry = new Entry[] { new Name(serviceName) };
		}

		template = new ServiceTemplate(null, types, entry);

		LookupDiscovery disco = null;
        // no groups then use ALL_GROUPS
		if (groups != null && groups.length > 0) {
			disco = new LookupDiscovery(groups);
		} else {
			disco = new LookupDiscovery(LookupDiscovery.ALL_GROUPS);
		}

		disco.addDiscoveryListener(new Listener());

		synchronized (lock) {
			lock.wait(waitTime);
		}

		disco.terminate();
		if (proxy == null) {
			throw new InterruptedException("Service not found within wait time");
		}
		return proxy;

	}

    public ServiceItem[] getServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter, String[] groups) {
        String[] locators = SorcerEnv.getLookupLocators();
        List<ServiceItem> result = new ArrayList<ServiceItem>();
        for (String locator : locators) {
            try {
                LookupLocator loc = new LookupLocator("jini://" + locator);
                ServiceRegistrar reggie = loc.getRegistrar();
                ServiceMatches matches = reggie.lookup(template, maxMatches);
                result.addAll(Arrays.asList(matches.items));
                if (result.size() >= maxMatches) break;
            } catch (MalformedURLException e) {
                log.warn("Malformed URL", e);
            } catch (ClassNotFoundException e) {
                log.warn("ClassNotFoundException URL", e);
            } catch (RemoteException e) {
                log.debug("Remote exception", e);
            } catch (IOException e) {
                log.debug("Communication error", e);
            }
        }
        if (result.size() < minMatches) {
            LookupDiscovery disco = null;
            try {
                disco = new LookupDiscovery(groups);
                //SorcerDiscoveryListener listener = new SorcerDiscoveryListener(template, minMatches, maxMatches, filter);
                //disco.addDiscoveryListener(listener);
                //result.addAll(listener.get(WAIT_FOR, TimeUnit.MILLISECONDS));
                Thread.sleep(WAIT_FOR*MAX_TRIES);
                for (ServiceRegistrar registrar : disco.getRegistrars()) {
                    ServiceMatches matches = registrar.lookup(template, maxMatches);
                    result.addAll(Arrays.asList(matches.items));
                    if (result.size() >= maxMatches) break;
                }
            } catch (IOException e) {
                log.debug("Communication error", e);
            } catch (InterruptedException ignored) {
                //ignored
            } finally {
                disco.terminate();
            }
        }

        return result.toArray(new ServiceItem[result.size()]);
    }

    class Listener implements DiscoveryListener {
		// invoked when a LUS is discovered
		public void discovered(DiscoveryEvent ev) {
			ServiceRegistrar[] reg = ev.getRegistrars();
			for (int i = 0; i < reg.length && proxy == null; i++) {
				findService(reg[i]);
			}
		}

		public void discarded(DiscoveryEvent ev) {
		}
	}

	private void findService(ServiceRegistrar lus) {

		try {
			synchronized (lock) {
				proxy = lus.lookup(template);
				if (proxy != null) {
					lock.notifyAll();
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
		return SorcerEnv.getLookupGroups();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * sorcer.service.DynamicAccessor#getServiceItem(sorcer.service.Signature)
	 */
	public ServiceItem getServiceItem(Signature signature)
			throws SignatureException {
		throw new SignatureException("Not implemented by this service accessor");
	}

	public static Service getService(Signature signature) throws SignatureException {
		Object proxy = null;
		try {
			String[] groups = null;
			String[] locators = null;
			if (signature.getProviderName() instanceof ServiceName) {
				groups = ((ServiceName)signature.getProviderName()).getGroups();
			}
			ProviderName pn = signature.getProviderName();
			if (pn instanceof  ServiceName) {
				locators = ((ServiceName) pn).getLocators();
			}
			if (((NetSignature)signature).isUnicast() || locators != null) {
				if (locators == null)
					locators = SorcerEnv.getLookupLocators();
				for (String locator : locators) {
					proxy = getService(locator,
                            signature.getServiceType(), signature.getMatchTypes(), signature
                            .getProviderName().getName());
					if (proxy != null && proxy instanceof Service)
						break;
                }
			} else {
				proxy = getService(signature.getServiceType(), signature.getMatchTypes(),
                        signature.getProviderName().getName(), groups, WAIT_FOR);
			}
		} catch (Exception ioe) {
			throw new SignatureException(ioe);
		} 
		if (proxy == null || !(proxy instanceof Service)) {
			throw new SignatureException("Cannot find service for: "
					+ signature);
		} else
			return (Service) proxy;
	}

    public static Provider getProvider(Signature signature) throws SignatureException {
        return (Provider)getService(signature);
    }

    /*
 	* (non-Javadoc)
 	*
 	* @see sorcer.service.DynamicAccessor#selectService(sorcer.service.Signature)
 	*/
    public <T> T getProvider(String serviceName, Class<T> serviceType) {
        try {
            return (T)getServiceImpl(serviceType, null, serviceName, null, WAIT_FOR);
        } catch (Exception e) {
            return null;
        }
    }
}
