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
package sorcer.core.monitoring;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.analytics.MethodAnalytics;
import sorcer.util.Sorcer;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dennis Reedy
 */
public class MonitorAgent {
    private static final LookupDiscoveryManager discoveryManager;
    private static final Logger logger = LoggerFactory.getLogger(MonitorAgent.class);
    private static final boolean monitoringEnabled =
        Boolean.parseBoolean(System.getProperty("monitoring.enabled", "true"));
    private static MonitorListener monitorListener;
    static {
        if(monitoringEnabled) {
            try {
                LookupLocator[] lookupLocators = null;
                String[] locators = Sorcer.getLookupLocators();
                if (locators.length > 0) {
                    lookupLocators = new LookupLocator[locators.length];
                    for (int i = 0; i < locators.length; i++)
                        lookupLocators[i] = new LookupLocator(locators[i]);
                }
                discoveryManager = new LookupDiscoveryManager(Sorcer.getLookupGroups(),
                                                              lookupLocators,
                                                              null);
                monitorListener = new MonitorListener();
                discoveryManager.addDiscoveryListener(monitorListener);
                logger.debug("Discovery using groups: {}, locators: {}",
                             discoveryManager.getGroups(), discoveryManager.getLocators());
            } catch (IOException e) {
                throw new RuntimeException("Could not create instance of DiscoveryManagement", e);
            }
        } else {
            discoveryManager = null;
        }
    }


    private MonitorRegistration monitorRegistration;
    //private LeaseRenewalManager leaseManager;

    public MonitorRegistration register(String identifier, String owner) {
        return register(identifier, owner, Lease.ANY);
    }

    public MonitorRegistration register(String identifier, String owner, long duration) {
        if(!monitoringEnabled) {
            return new MonitorRegistration(null, UuidFactory.generate(), identifier, owner, null);
        }
        if(monitorRegistration!=null)
            return monitorRegistration;
        waitOnDiscover();
        Monitor monitor = monitorListener.getMonitor();
        if(monitor==null) {
            logger.error("No available Monitor");
            return null;
        }
        try {
            monitorRegistration = monitor.register(identifier, owner, duration);
            //leaseManager = new LeaseRenewalManager(monitorRegistration.getLease(), Lease.FOREVER, null);
        } catch (IOException | MonitorException e) {
            logger.warn("Unable to obtain a MonitorRegistration for {}, {}", identifier, owner, e);
        }
        return monitorRegistration;
    }

    public void started() {
        update(Monitor.Status.SUBMITTED);
    }

    public void inprocess(MethodAnalytics analytics) {
        update(Monitor.Status.ACTIVE, analytics);
    }

    public void completed() {
        update(Monitor.Status.COMPLETED);
        terminate();
    }

    public void failed() {
        update(Monitor.Status.FAILED);
        terminate();
    }

    public void update(Monitor.Status status) {
        update(status, null);
    }

    private void update(Monitor.Status status, MethodAnalytics analytics) {
        if(!monitoringEnabled) {
            return;
        }
        if(monitorRegistration==null) {
            logger.warn("No MonitorRegistration, unable to update status");
            return;
        }
        try {
            monitorRegistration.getMonitor().update(monitorRegistration, status, analytics);
            if(logger.isInfoEnabled())
                logger.info("ACTIVITY: {}", analytics);
        } catch (IOException | MonitorException e) {
            logger.error("Unable to update status", e);
        }
    }

    public void terminate() {
        if(!monitoringEnabled)
            return;
        discoveryManager.removeDiscoveryListener(monitorListener);
        if(monitorRegistration!=null) {
            /*try {
                leaseManager.cancel(monitorRegistration.getLease());
            } catch (UnknownLeaseException | RemoteException e) {
                logger.warn("Problem cancelling lease", e);
            }
            leaseManager.clear();*/
            monitorRegistration = null;
        }
    }

    private void waitOnDiscover() {
        int waitCount = 0;
        long timeout = 60;
        while(monitorListener.isDiscovering() && waitCount < timeout) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waitCount++;
        }
    }

    private static class MonitorListener implements DiscoveryListener {
        private final List<ServiceRegistrar> lookups = new ArrayList<>();
        private final ServiceTemplate template = new ServiceTemplate(null, new Class[]{Monitor.class}, null);

        boolean isDiscovering() {
            return lookups.isEmpty();
        }

        Monitor getMonitor() {
            Monitor monitor = null;
            for (ServiceRegistrar registrar : lookups) {
                try {
                    Object o = registrar.lookup(template);
                    if (o != null) {
                        monitor = (Monitor)o;
                        break;
                    }
                } catch (RemoteException e) {
                    logger.error("Problem discovering Monitors", e);
                }
            }
            return monitor;
        }

        @Override public void discovered(DiscoveryEvent discoveryEvent) {
            logger.info("Discovered {} Lookup(s)", discoveryEvent.getRegistrars().length);
            Collections.addAll(lookups, discoveryEvent.getRegistrars());
        }

        @Override public void discarded(DiscoveryEvent discoveryEvent) {
            for(ServiceRegistrar r : discoveryEvent.getRegistrars())
                lookups.remove(r);
        }
    }
}
