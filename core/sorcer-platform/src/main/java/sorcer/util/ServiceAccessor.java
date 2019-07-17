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

import net.jini.config.Configuration;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.lookup.ServiceItemFilter;
import net.jini.lookup.entry.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.service.DynamicAccessor;
import sorcer.service.Signature;
import sorcer.service.SignatureException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.Collections;

import static sorcer.core.SorcerConstants.ANY;

/**
 * A service discovery and management utility allowing to access services by
 * matching service templates and passing filters provided by clients. The
 * optional usage of a service lookup cache is provided for clients that require
 * lookup for multiple services frequently.
 * <p>
 * A similar service discovery and management functionality is provided by a
 * SORCER Cataloger service provider with round robin load balancing.
 * <p>
 * The continuous discovery of SORCER services is usually delegated to
 * Catalogers while other SORCER service providers can query effectively
 * Cataloger's cached services with round robin load balancing.
 * <p>
 * The individual SORCER services should be accessed using the
 * {@link sorcer.util.ProviderAccessor} subclass, that uses local cached proxies
 * for frequently used SORCER infrastructure services. ProviderAccessor normally
 * uses Cataloger if available, otherwise uses Jini lookup services as
 * implemented by the ServiceAccessor.
 *
 * @see sorcer.util.ProviderAccessor
 *
 * @author Mike Sobolewski
 */
public class ServiceAccessor implements DynamicAccessor {
	static Logger logger = LoggerFactory.getLogger(ServiceAccessor.class);
	static long WAIT_FOR = Sorcer.getLookupWaitTime();
    static boolean cacheEnabled = Sorcer.isLookupCacheEnabled();
	
	// wait for cataloger 2 sec  = LUS_REPEAT x 200
	// since then falls back on LUSs managed by ServiceAccessor
	// wait for service accessor 5 sec  = LUS_REPEAT x 500
	final static int LUS_REPEAT = 10;
	private static LeaseRenewalManager lrm = null;
	private static ServiceDiscoveryManager sdManager = null;
	private static LookupCache lookupCache = null;
	protected static String[] lookupGroups = Sorcer.getLookupGroups();
	private static int MIN_MATCHES = Sorcer.getLookupMinMatches();
	private static int MAX_MATCHES = Sorcer.getLookupMaxMatches();
    protected ProviderNameUtil providerNameUtil = new SorcerProviderNameUtil();

    public ServiceAccessor(Configuration config) {
        openDiscoveryManagement(config);
    }

	static public ServiceDiscoveryManager getServiceDiscoveryManager() {
		return sdManager;
	}

	/**
	 * Returns a service impl containing a service matching providerName and
	 * serviceInfo using Jini lookup service.
	 *
	 * @param providerName key
	 * @param serviceType fiType
	 * @return A ServiceItem
	 */
	public ServiceItem getServiceItem(String providerName, Class serviceType) {
		String name = overrideName(providerName, serviceType);
		Class[] serviceTypes = new Class[] { serviceType };
		Entry[] attrSets = new Entry[] { new Name(name) };
		ServiceTemplate template = new ServiceTemplate(null, serviceTypes, attrSets);
		return getServiceItem(template, null);
	}

	public ServiceItem getServiceItem(String providerName, Class[] serviceTypes) {
		String name = overrideName(providerName, serviceTypes[serviceTypes.length-1]);
		Entry[] attrSets = new Entry[] { new Name(name) };
		ServiceTemplate template = new ServiceTemplate(null, serviceTypes, attrSets);
		return getServiceItem(template, null);
	}

	/**
	 * Returns a service impl matching a service template and passing a filter
	 * with Jini registration groups. Note that template matching is a remote
	 * operation - matching is done by lookup services while passing a filter is
	 * done on the client side. Clients should provide a service filter, usually
	 * as an object of an inner class. A filter narrows the template matching by
	 * applying more precise, for example boolean select as required by the
	 * client. No lookup cache is used by this <code>getServiceItem</code>
	 * method.
	 *
	 * @param template
	 *            template to match remotely.
	 * @param filter
	 *            filer to use or null.
	 * @return a ServiceItem that matches the template and filter or null if
	 *         none is found.
	 */
    public ServiceItem getServiceItem(ServiceTemplate template, ServiceItemFilter filter) {
        checkNullName(template);
		ServiceItem si = null;
        logger.info("Lookup {}, timeout: {}, filter: {}", formatServiceTemplate(template), WAIT_FOR, filter);
		try {
            int tryNo = 0;
            while (tryNo < LUS_REPEAT) {
                si = sdManager.lookup(template, filter, WAIT_FOR);
                logger.info("Found [{}] instances of {}", si == null ?
                                                          "0" :
                                                          "1", formatServiceTemplate(template), WAIT_FOR);
                if (si != null)
                    break;
                tryNo++;
            }
		} catch (IOException | InterruptedException ie) {
			logger.error("getServiceItem", ie);
		}
		return si;
	}

	/**
	 * Creates a service lookup and discovery manager with a provided service
	 * template, lookup cache filter, and list of jini groups.
	 *
	 * @param config Configuration to pass on to underlying discovery
	 */
	protected void openDiscoveryManagement(Configuration config) {
		if (sdManager == null) {
			LookupLocator[] locators = getLookupLocators();
            Set<String> groupsSet = new HashSet<>();
            String[] groups = DiscoveryGroupManagement.ALL_GROUPS;
            if (SorcerEnv.getLookupGroups() != null) {
                Collections.addAll(groupsSet, SorcerEnv.getLookupGroups());
                groupsSet.add(Sorcer.getSpaceGroup());
                groups = groupsSet.toArray(new String[groupsSet.size()]);
            }
            try {
                logger.info("[openDiscoveryManagement] SORCER Group(s): {}, Locators: {}",
                             SorcerUtil.arrayToString(groups), SorcerUtil.arrayToString(locators));

				lrm = new LeaseRenewalManager(config);
                DiscoveryManagement ldManager = new LookupDiscoveryManager(groups, locators, null, config);
				sdManager = new ServiceDiscoveryManager(ldManager, lrm, config);
			} catch (Exception e) {
				logger.error("openDiscoveryManagement", e);
			}
		}
		// Opening a lookup cache
		openCache();
	}

	public static LeaseRenewalManager getLeaseRenewalManager() {
		return lrm;
	}

	public static void clearLeaseRenewalManager() {
		lrm.clear();
	}

	/**
	 * Creates a lookup cache for the existing service discovery manager
	 */
	private void openCache() {
		if (cacheEnabled && lookupCache == null) {
			try {
				lookupCache = sdManager.createLookupCache(null, null, null);
			} catch (RemoteException e) {
				closeLookupCache();
			}
		}
	}

	/**
	 * Terminates a lookup cache used by this ServiceAccessor.
	 */
	static private void closeLookupCache() {
		if (lookupCache != null) {
			lookupCache.terminate();
			lookupCache = null;
		}
	}

	/**
	 * Returns a service matching multitype, service attributes (args), and
	 * passes a provided filter.
	 *
	 * @param attributes   attributes of the requested provider
	 * @param serviceType fiType of the requested provider
     *
	 * @return The discovered service or null
	 */
    @SuppressWarnings("unchecked")
	public <T>T getService(Class<T> serviceType, Entry[] attributes, ServiceItemFilter filter) {
		if (serviceType == null) {
			throw new IllegalArgumentException("Missing service fiType for a ServiceTemplate");
		}
        ServiceTemplate tmpl = new ServiceTemplate(null, new Class[] { serviceType }, attributes);
		ServiceItem si = getServiceItem(tmpl, filter);
		if (si != null)
			return (T)si.service;
		else
			return null;
	}

	@Override
	public <T> T getService(final String serviceName, Class<T> serviceType) {
		T proxy = null;
        String name = overrideName(serviceName, serviceType);

		int tryNo = 0;
		while (tryNo < LUS_REPEAT) {
			logger.info("trying to getValue service: {}: {}; attempt: {}...", serviceType, name, tryNo);
			try {
				tryNo++;
				proxy = getService(serviceType, new Entry[] { new Name(name) }, null);
				if (proxy != null)
					break;

				Thread.sleep(WAIT_FOR);
			} catch (Exception e) {
				logger.error("Failed trying to getValue {} {}", name, serviceType.getName(), e);
			}
		}
		logger.info("got LUS service [fiType={} key={}]: {}", serviceType.getName(), name, proxy);
		return proxy;
	}

    /**
	 * Returns a service matching a given template filter, and Jini lookup
	 * service.
	 *
	 * @param template service template
	 * @param filter   service filter
     *
	 * @return a service provider
	 */
	public Object getService(ServiceTemplate template, ServiceItemFilter filter) {
        ServiceItem si = getServiceItem(template, filter);
        return si == null ? null : si.service;
    }

	/**
	 * Returns a list of lookup locators with the URLs defined in the SORCER
	 * environment
	 *
	 * @see sorcer.util.Sorcer
	 *
	 * @return a list of locators for unicast lookup discovery
	 */
	private LookupLocator[] getLookupLocators() {
		String[] locURLs = SorcerEnv.getLookupLocators();
        if (locURLs == null || locURLs.length == 0) {
            return null;
        }
        List<LookupLocator> locators = new ArrayList<>(locURLs.length);
        logger.debug("ProviderAccessor Locators: {}", Arrays.toString(locURLs));

		for (String locURL : locURLs)
			try {
				locators.add(new LookupLocator(locURL));
			} catch (Exception e) {
				logger.warn("Invalid Lookup URL: {}", locURL, e);
			}

		if (locators.isEmpty())
			return null;
        return locators.toArray(new LookupLocator[locators.size()]);
	}

    static public void terminateDiscovery() {
		closeLookupCache();
		sdManager.terminate();
		sdManager = null;
	}

    public ServiceItem getServiceItem(Signature signature) throws SignatureException {
		if (signature.getMatchTypes() != null) {
			return getServiceItem(signature.getProviderName().getName(), signature.getMatchTypes());
		} else {
			return getServiceItem(signature.getProviderName().getName(), signature.getServiceType());
		}
    }

    public  Object getService(Signature signature) throws SignatureException {
        ServiceItem serviceItem = getServiceItem(signature);
        return serviceItem == null ? null : serviceItem.service;
    }


	public Object getService(ServiceID serviceID) {
        return getService(serviceID, null, null);
    }

    public Object getService(ServiceID serviceID, Class[] serviceTypes, Entry[] attrSets) {
        ServiceTemplate st = new ServiceTemplate(serviceID, serviceTypes, attrSets);
        return getService(st, null);
    }

    public ServiceItem[] getServiceItems(ServiceTemplate template, ServiceItemFilter filter) {
        return doGetServiceItems(template, MIN_MATCHES, MAX_MATCHES, filter);
    }

	public ServiceItem[] getServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter) {
		return getServiceItems(template, minMatches, maxMatches, filter, LUS_REPEAT);
	}

    public ServiceItem[] getServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter, int lusRepeat) {
        logger.info("Lookup {}, lusRepeat: {}, timeout: {}", formatServiceTemplate(template), lusRepeat, WAIT_FOR);
        for (int tryNo = 0; tryNo < lusRepeat; tryNo++) {
            ServiceItem[] result = doGetServiceItems(template, minMatches, maxMatches, filter);
            if (result != null && result.length > 0)
                return result;
        }
        return new ServiceItem[0];
    }

	private ServiceItem[] doGetServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter) {
        try {
            return sdManager.lookup(template, minMatches, maxMatches, filter, WAIT_FOR);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error while getting service", e);
            return null;
        }
    }

	static String formatServiceTemplate(ServiceTemplate template) {
		return String.format("key: %s, fiType: %s",  getNames(template.attributeSetTemplates), getTypes(template.serviceTypes));
    }

    static String getNames(Entry[] entries) {
        StringBuilder sb = new StringBuilder();
        if(entries!=null) {
            for (Entry e : entries) {
                if (e instanceof Name) {
                    if (sb.length() > 0)
                        sb.append(", ");
                    sb.append(((Name) e).name);
                }
            }
        } else {
            sb.append("<null>");
        }
        return sb.toString();
    }

    static String getTypes(Class<?>[] classes) {
        StringBuilder sb = new StringBuilder();
        if(classes!=null) {
            for (Class<?> c : classes) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(c.getName());
            }
        } else {
            sb.append("<null>");
        }
        return sb.toString();
    }

    private String overrideName(String providerName, Class serviceType) {
        if (providerName == null || "*".equals(providerName))
            return null;
        if (SorcerConstants.NAME_DEFAULT.equals(providerName))
            providerName = providerNameUtil.getName(serviceType);
        return Sorcer.getActualName(providerName);
    }

    private void checkNullName(ServiceTemplate template) {
        if (template.attributeSetTemplates == null)
            return;
        for (Entry attr : template.attributeSetTemplates) {
            if (attr instanceof Name) {
                Name name = (Name) attr;
                if (ANY.equals(name.name)) {
                    name.name = null;
                    logger.warn("Requested service with key '*'", new IllegalArgumentException());
                }
            } else if (attr instanceof SorcerServiceInfo) {
                SorcerServiceInfo info = (SorcerServiceInfo) attr;
                if (ANY.equals(info.providerName)) {
                    info.providerName = null;
                    logger.warn("Requested service with key '*'", new IllegalArgumentException());
                }
            }
        }
    }
}
