/*
 * Copyright to the original author or authors.
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
package sorcer.core.deploy;

import net.jini.admin.Administrable;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.lookup.ServiceDiscoveryManager;
import org.rioproject.config.Constants;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.impl.client.JiniClient;
import sorcer.util.Sorcer;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility to provide a cache of known {@link org.rioproject.monitor.ProvisionMonitor}s. This will be used by
 * classes that require a reference to available {@code ProvisionMonitor} instances, but do no want to go
 * through the overhead of discovery.
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorCache {
    private final net.jini.lookup.LookupCache cache;
    private static final ProvisionMonitorCache instance = new ProvisionMonitorCache();
    private final Map<String, String> discoveryInfo = new HashMap<>();
    private static Listener listener;

    private ProvisionMonitorCache() {
        try {
            LookupLocator[] locators = null;
            if(System.getProperty(Constants.LOCATOR_PROPERTY_NAME)!=null) {
                discoveryInfo.put("locators", System.getProperty(Constants.LOCATOR_PROPERTY_NAME));
                locators = JiniClient.parseLocators(System.getProperty(Constants.LOCATOR_PROPERTY_NAME));
            }
            StringBuilder g = new StringBuilder();
            for(String group : Sorcer.getLookupGroups()) {
                if(g.length()>0)
                    g.append(", ");
                g.append(group);
            }
            discoveryInfo.put("groups", g.toString());
            Class cl = org.rioproject.deploy.ProvisionManager.class;
            listener = new Listener();
            ServiceDiscoveryManager lookupMgr =
                    new ServiceDiscoveryManager(new LookupDiscoveryManager(Sorcer.getLookupGroups(),
                                                                           locators,
                                                                           listener), // DiscoveryListener
                                                null);

            cache = lookupMgr.createLookupCache(new ServiceTemplate(null, new Class[]{cl}, null),
                                                null,
                                                null);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static ProvisionMonitorCache getInstance() {
        return instance;
    }

    public String getGroups() {
        return discoveryInfo.get("groups");
    }

    public String getLocators() {
        return discoveryInfo.get("locators");
    }

    public DeployAdmin getDeployAdmin() {
        if(listener.monitor.get()!=null) {
            return listener.monitor.get();
        }
        DeployAdmin dAdmin = null;
        int waited = 0;
        int timeout = 60; /* We will timeout after 30 seconds */
        while(dAdmin==null && waited < timeout) {
            ServiceItem item = cache.lookup(null);
            if(item!=null) {
                try {
                    dAdmin = (DeployAdmin)((Administrable)item.service).getAdmin();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Thread.sleep(500);
                    waited++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return dAdmin;
    }

    static class Listener implements DiscoveryListener {
        final AtomicReference<DeployAdmin> monitor = new AtomicReference<>();
        private Class<?> serviceType = org.rioproject.deploy.ProvisionManager.class;
        private final List<ServiceRegistrar> lookups = new ArrayList<>();

        void lookup() {
            if(monitor.get()!=null)
                return;
            final ServiceTemplate template = new ServiceTemplate(null, new Class[]{serviceType}, null);
            for(ServiceRegistrar registrar : lookups) {
                try {
                    Object service = registrar.lookup(template);
                    if(service!=null) {
                        monitor.set((DeployAdmin)((Administrable)service).getAdmin());
                        break;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        public void discovered(DiscoveryEvent discoveryEvent) {
            Collections.addAll(lookups, discoveryEvent.getRegistrars());
            lookup();
        }

        public void discarded(DiscoveryEvent discoveryEvent) {
        }
    }

}
