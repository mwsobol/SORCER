/**
 *
 * Copyright 2013 the original author or authors.
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
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.ServiceItemFilter;
import net.jini.lookup.entry.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.Cataloger;
import sorcer.core.provider.Provider;
import sorcer.core.signature.NetSignature;
import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.river.Filters;
import sorcer.service.Accessor;
import sorcer.service.Signature;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static sorcer.core.SorcerConstants.ANY;

/**
 * A utility class that provides access to SORCER services and some
 * infrastructure services. It extends the <code>ServiceAccessor</code>
 * functionality.
 *
 * The <code>getService</code> methods directly use ServiceAccessor calls while
 * the <code>getService</code> methods use a SORCER Cataloger's cached services
 * with round robin load balancing.
 *
 * The individual SORCER services should be accessed using this utility since it
 * uses local cached proxies for frequently used SORCER infrastructure services,
 * for example: Cataloger, JavaSpace, Jobber. ProviderAccessor normally uses
 * Cataloger if available, otherwise it uses Jini lookup services as implemented
 * by <code>ServiceAccessor</code>.
 *
 * @see ServiceAccessor
 */

public class ProviderAccessor extends ServiceAccessor {
	static Logger logger = LoggerFactory.getLogger(ProviderAccessor.class.getName());

    /**
	 * Used for local caching to speed up getting frequently needed service
	 * providers. Calls to discover JavaSpace takes a lot of time.
	 */
	protected static Cataloger cataloger;

    protected static ProviderNameUtil providerNameUtil = new SorcerProviderNameUtil();

    /* Zero-arg constructor for testing purposes, should not be used without a configuration */
    public ProviderAccessor() {
        this(EmptyConfiguration.INSTANCE);
    }

	public ProviderAccessor(Configuration configuration) {
		super(configuration);
	}

    /**
     * Returns a SORCER service provider with the specified signature,
     * Added for compatibility with AFRL SORCER
     *
     * @param signature
     *            the signature of service provider
     * @return a SORCER provider service
     */
    public Provider getProvider(Signature signature) {
        return (Provider)getService(signature);
    }

    /**
     * Returns a SORCER service provider with the specified service type, using
     * a Cataloger if available, otherwise using Jini lookup services.
     *
     * @param serviceType
     *            a provider service type (interface)
     * @return a SORCER provider service
     */
    public Provider getProvider(Class serviceType) {
        return getProvider(new NetSignature(serviceType));
    }

    /**
	 * Returns a SORCER service provider registered with the most significant
	 * and the least significant bits.
	 *
	 * @param mostSig
	 *            most significant bits
	 * @param leastSig
	 *            least significant bits
	 * @return a SORCER provider service
	 */
	public Object getService(long mostSig, long leastSig) {
		ServiceID serviceID = new ServiceID(mostSig, leastSig);
		return getService(serviceID, null, null);
	}

	/**
	 * Returns a SORCER service provider with the specified name and service
	 * type, using a Cataloger if available, otherwise using Jini lookup
	 * services.
	 *
	 * @param providerName
	 *            the name of service provider
	 * @param serviceType
	 *            a provider service type (interface)
	 * @return a SORCER provider service
	 */
    @SuppressWarnings("unchecked")
	public <T> T getProvider(String providerName, Class<T> serviceType) {
		Provider servicer = null;
		if (providerName != null) {
            if (providerName.equals(SorcerConstants.ANY))
                providerName = null;
            if(SorcerConstants.NAME_DEFAULT.equals(providerName)){
                providerName = providerNameUtil.getName(serviceType);
            }
        }

		try {
			//servicer = (Service)ProviderLookup.getService(providerName, serviceType);
			cataloger = getLocalCataloger();
			if (cataloger != null) {
                long t0 = System.currentTimeMillis();
                logger.info("Use Cataloger to discover {} {}", providerName, serviceType.getName());
				int tryNo = 0;
				while (tryNo < ServiceAccessor.LUS_REPEAT) {
					servicer = cataloger.lookup(providerName, serviceType);
					//servicer = (Service)cataloger.lookupItem(providerName, serviceType).service;
					if (servicer != null)
						break;

					Thread.sleep(ServiceAccessor.WAIT_FOR);
					tryNo++;
				}
                logger.info("Waited {} millis to discover {} {}", (System.currentTimeMillis()-t0), providerName, serviceType.getName());
			}
            // fall back on Jini LUS
            if (servicer == null) {
                if(cataloger!=null)
                    logger.info("Could not find {} {} in Cataloger, discover using Lookup service",
                                providerName, serviceType.getName());
                else
                    logger.info("No Cataloger, discover {} {} using Lookup service",
                                providerName, serviceType==null?"<null>" : serviceType.getName());
                servicer = (Provider) super.getService(providerName, serviceType);
            }
		} catch (Throwable ex) {
			logger.error("getService {} {}", providerName, serviceType==null?"<null>": serviceType.getName(), ex);
		}
		return (T) servicer;
	}


    /**
	 * Returns a SORCER service provider with the specified service ID using a
	 * Cataloger if available, otherwise using Jini lookup services.
	 *
	 * @param serviceID
	 *            serviceID of the desired service
	 * @return a SORCER provider service
	 */
	public Object getService(ServiceID serviceID) {
		try {
			cataloger = getLocalCataloger();
			if (cataloger != null)
				return cataloger.lookup(serviceID);
			else
				return super.getService(serviceID);
		} catch (Exception ex) {
			logger.error(ProviderAccessor.class.getName(), "getService", ex);
			return null;
		}
	}

    /**
	 * Returns a SORCER service provider matching a given attributes.
	 *
	 * @param attributes
	 *            attribute set to match
	 * @return a SORCER provider
	 */
	public Provider getProvider(Entry[] attributes) {
		return (Provider) getService(null, null, attributes);
	}

	/**
	 * Returns a SORCER service provider matching a given list of implemented
	 * service types (interfaces).
	 *
	 * @param serviceTypes
	 *            a set of service types to match
	 * @return a SORCER provider
	 */
	public Provider getProvider(Class[] serviceTypes) {
		return (Provider) getService(null, serviceTypes, null);
	}

	/**
	 * Returns a SORCER Cataloger Service.
	 *
	 * This method searches for either a JINI or a RMI Cataloger service.
	 *
	 * @return a Cataloger service proxy
     * @see sorcer.core.provider.Cataloger
	 */
	protected Cataloger getLocalCataloger() {
        return getCataloger(providerNameUtil.getName(Cataloger.class)) ;
	}

	/**
	 * Returns a SORCER Cataloger service provider using JINI discovery.
	 *
	 * @return a SORCER Cataloger
	 */
    protected Cataloger getCataloger(String serviceName) {
        boolean catIsOk;
		try {
            catIsOk = Accessor.isAlive((Provider) cataloger);
            if (catIsOk) {
				return cataloger;
			} else {
                ServiceItem[] serviceItems = getServiceItems(getServiceTemplate(null, serviceName, new Class[]{Cataloger.class}, null),
                                                             1,
                                                             1,
                                                             Filters.any());
                cataloger = serviceItems.length == 0 ? null : (Cataloger) serviceItems[0].service;
                if (Accessor.isAlive((Provider)cataloger))
                    return cataloger;
                else
                    return null;
            }
		} catch (Exception e) {
			logger.error("Problem getting Cataloger", e);
			return null;
		}
	}

    /**
	 * Returns a SORCER service using a cached Cataloger instance by this
	 * ProviderAccessor. However if it not possible uses a ServiceAccessor to
	 * get a requested service form Jini lookup services directly. This approach
	 * allows for SORCER requestors and providers to avoid continuous usage of
	 * lookup discovery for each needed service that is delegated to a SORCER
	 * Cataloger service.
	 *
	 * @param providerName
	 *            - a name of requested service
	 * @param primaryInterface
	 *            - service type of requested provider
	 * @return a requested service or null if a Cataloger is not available
	 */
	protected Provider lookup(String providerName, Class<Provider> primaryInterface) {
		try {
			// check if the cataloger is alive then return a requested service
			// provider
			if (Accessor.isAlive((Provider) cataloger))
				return cataloger.lookup(providerName, primaryInterface);
			else {
				// try to get a new cataloger and lookup again
				cataloger = getService(providerNameUtil.getName(Cataloger.class), Cataloger.class);
				if (cataloger != null) {
					logger.debug("Got service provider from Cataloger");
					return cataloger.lookup(providerName, primaryInterface);
				} else {
					// just get a provider without a Cataloger, use directly
					// LUSs
					logger.error("No SORCER cataloger available");
					return getService(providerName, primaryInterface);
				}
			}
		} catch (RemoteException ex) {
			logger.error(ProviderAccessor.class.getName(), "lookup", ex);
			return null;
		}
	}

    @Override
    public ServiceItem[] getServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter) {
        assert template != null;

        // cataloger throws NPE if attributeSetTemplates is null
        assert template.attributeSetTemplates != null;
        assert filter != null;
        assert minMatches <= maxMatches;

        if(!Arrays.asList(template.serviceTypes).contains(Cataloger.class)){
			Cataloger cataloger = getLocalCataloger();
            if (cataloger != null) {
			    try {
                    ServiceMatches matches = cataloger.lookup(template, maxMatches);
                    logger.debug("Cataloger returned {} matches for {}", matches.totalMatches, formatServiceTemplate(template));
			        ServiceItem[] matching = Filters.matching(matches.items, filter);
                    if (matching.length > 0) {
                        return matching;
                    } else {
                        return super.getServiceItems(template, minMatches, maxMatches, filter);
                    }
                } catch (RemoteException e) {
                    logger.error( "Problem with Cataloger, falling back", e);
                }
            }
        }
		return super.getServiceItems(template, minMatches, maxMatches, filter);
    }

    ServiceTemplate getServiceTemplate(ServiceID serviceID,
                                       String providerName,
                                       Class[] serviceTypes,
                                       String[] publishedServiceTypes) {
        Class[] types;
        List<Entry> attributes = new ArrayList<>(2);

        if (providerName != null && !providerName.isEmpty() && !ANY.equals(providerName))
            attributes.add(new Name(providerName));

        if (publishedServiceTypes != null) {
            SorcerServiceInfo st = new SorcerServiceInfo();
            st.publishedServices = publishedServiceTypes;
            attributes.add(st);
        }

        if (serviceTypes == null) {
            types = new Class[] { Provider.class };
        } else {
            types = serviceTypes;
        }

        logger.debug("getServiceTemplate >> \n serviceID: {}\nproviderName: {}\nserviceTypes: {}\npublishedServiceTypes: {}",
                     serviceID, providerName, StringUtils.arrayToString(serviceTypes), StringUtils.arrayToString(publishedServiceTypes));

        return new ServiceTemplate(serviceID, types, attributes.toArray(new Entry[attributes.size()]));
    }
}
