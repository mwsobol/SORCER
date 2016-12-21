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
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.id.UuidFactory;
import net.jini.lease.LeaseRenewalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.analytics.MethodAnalytics;
import sorcer.util.Sorcer;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Dennis Reedy
 */
public class MonitorAgent {
    private static final LookupDiscoveryManager discoveryManager;
    private static final Logger logger = LoggerFactory.getLogger(MonitorAgent.class);
    private static boolean monitoringEnabled = Boolean.parseBoolean(System.getProperty("monitoring.enabled", "true"));
    private static MonitorListener monitorListener;
    private final static ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final static BlockingQueue<Request> requests = new LinkedBlockingQueue<>();
    private final static CountDownLatch discoveryLatch = new CountDownLatch(1);
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
                executor.submit(new MonitorNotificationHandler());
            } catch (IOException e) {
                throw new RuntimeException("Could not create instance of DiscoveryManagement", e);
            }
        } else {
            discoveryManager = null;
        }
    }

    private MonitorRegistration monitorRegistration;
    private LeaseRenewalManager leaseManager;

    public void register(String identifier, String owner) {
        register(identifier, owner, Lease.ANY);
    }

    public void register(String identifier, String owner, long duration) {
        if(!monitoringEnabled) {
            monitorRegistration = new MonitorRegistration(null, UuidFactory.generate(), identifier, owner, null);
            return;
        }
        requests.add(new Request(new RegistrationRequest()
                                     .identifier(identifier)
                                     .owner(owner)
                                     .duration(duration).listener(new MonitorRegistrationNotifier())));
    }

    public MonitorRegistration getMonitorRegistration() {
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
    }

    public void completed(MethodAnalytics analytics) {
        update(Monitor.Status.COMPLETED, analytics);
    }

    public void failed() {
        update(Monitor.Status.FAILED);
    }

    public void update(Monitor.Status status) {
        update(status, null);
    }

    public void update(Monitor.Status status, MethodAnalytics analytics) {
        if(!monitoringEnabled) {
            return;
        }
        if(monitorRegistration==null) {
            if(analytics!=null) {
                if (logger.isDebugEnabled())
                    logger.debug("No MonitorRegistration, unable to update status for method {} {}",
                                 analytics.getMethodName(), status);
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("No MonitorRegistration, unable to update status {}", status);
            }
            return;
        }
        if(requests.add(new Request(new Update(copy()).status(status).analytics(analytics)))) {
            if (logger.isDebugEnabled())
                logger.debug("ADDED: {} {}", monitorRegistration.getIdentifier(), status);
        } else {
            logger.warn("FAILED ADDING: {}", analytics);
        }
    }

    public void terminate() {
        if(!monitoringEnabled)
            return;
        if(monitorRegistration!=null) {
            try {
                leaseManager.cancel(monitorRegistration.getLease());
            } catch (UnknownLeaseException | RemoteException e) {
                logger.warn("Problem cancelling lease, benign issue, {}: {}", e.getClass().getName(), e.getMessage());
            }
            leaseManager.clear();
            monitorRegistration = null;
        }
    }

    private class MonitorRegistrationNotifier implements MonitorRegistrationListener {

        @Override public void notify(MonitorRegistration registration) {
            monitorRegistration = registration;
            leaseManager = new LeaseRenewalManager(monitorRegistration.getLease(), Lease.FOREVER, null);
            logger.info("Successful registration to a Monitor for {}, {}",
                        monitorRegistration.getIdentifier(), monitorRegistration.getOwner());
        }

        @Override public void failed(String identifier, String owner, Exception e) {
            monitorRegistration = new MonitorRegistration(null, UuidFactory.generate(), identifier, owner, null);
            monitoringEnabled = false;
            requests.clear();
            if(e!=null) {
                logger.warn("Unable to obtain a MonitorRegistration for {}, {}, setting monitoringEnabled to false",
                            identifier, owner, e);
            } else {
                logger.warn("Unable to obtain a MonitorRegistration for {}, {}, setting monitoringEnabled to false, {}",
                            identifier, owner, monitorRegistration);
            }
        }
    }

    private static class MonitorListener implements DiscoveryListener {
        private final List<ServiceRegistrar> lookups = new ArrayList<>();
        private final ServiceTemplate template = new ServiceTemplate(null, new Class[]{Monitor.class}, null);

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
            logger.debug("Discovered {} Lookup(s)", discoveryEvent.getRegistrars().length);
            Collections.addAll(lookups, discoveryEvent.getRegistrars());
            discoveryLatch.countDown();
        }

        @Override public void discarded(DiscoveryEvent discoveryEvent) {
            for(ServiceRegistrar r : discoveryEvent.getRegistrars())
                lookups.remove(r);
        }
    }

    private class RegistrationRequest {
        String identifier;
        String owner;
        long duration;
        MonitorRegistrationListener registrationListener;

        RegistrationRequest identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }
        RegistrationRequest owner(String owner) {
            this.owner = owner;
            return this;
        }
        RegistrationRequest duration(long duration) {
            this.duration = duration;
            return this;
        }
        RegistrationRequest listener(MonitorRegistrationListener registrationListener) {
            this.registrationListener = registrationListener;
            return this;
        }
    }

    private class Update {
        Monitor.Status status;
        MethodAnalytics analytics;
        MonitorRegistration registration;

        Update(MonitorRegistration registration) {
            this.registration = registration;
        }

        Update status(Monitor.Status status) {
            this.status = status;
            return this;
        }

        Update analytics(MethodAnalytics analytics) {
            this.analytics = analytics;
            return this;
        }
    }

    private MonitorRegistration copy() {
        return new MonitorRegistration(monitorRegistration.getLease(),
                                       monitorRegistration.getUuid(),
                                       monitorRegistration.getIdentifier(),
                                       monitorRegistration.getOwner(),
                                       monitorRegistration.getMonitor());
    }

    private class Request {
        Update update;
        RegistrationRequest registrationRequest;

        Request(Update update) {
            this.update = update;
        }

        Request(RegistrationRequest registrationRequest) {
            this.registrationRequest = registrationRequest;
        }
    }

    private static class MonitorNotificationHandler implements Runnable {

        @Override public void run() {
            logger.debug("[{}] Started MonitorNotificationHandler", Thread.currentThread().getId());
            while(true) {
                try {
                    Request request = requests.take();
                    if(request.registrationRequest!=null) {
                        RegistrationRequest r = request.registrationRequest;
                        if (logger.isDebugEnabled())
                            logger.debug("Processing registration request for {}, {}", r.identifier, r.owner);
                        try {
                            long timeout = Long.parseLong(System.getProperty("monitor.discovery.timeout", "3"));
                            if(timeout>0)
                                discoveryLatch.await(timeout, TimeUnit.SECONDS);
                            Monitor monitor = monitorListener.getMonitor();
                            if(monitor==null) {
                                if(r.registrationListener!=null)
                                    r.registrationListener.failed(r.identifier, r.owner, null);
                            } else {
                                MonitorRegistration monitorRegistration = monitor.register(r.identifier, r.owner, r.duration);
                                if (r.registrationListener != null)
                                    r.registrationListener.notify(monitorRegistration);
                            }
                        } catch (IOException | MonitorException e) {
                            if(r.registrationListener!=null)
                                r.registrationListener.failed(r.identifier, r.owner, e);
                        }
                    } else {
                        Update update = request.update;
                        MonitorRegistration monitorRegistration = update.registration;
                        if (logger.isDebugEnabled())
                            logger.debug("HANDLE: {} {}", monitorRegistration.getIdentifier(), update.status);
                        try {
                            monitorRegistration.getMonitor().update(monitorRegistration, update.status, update.analytics);
                            if (logger.isDebugEnabled())
                                logger.debug("HANDLED: {} {}", monitorRegistration.getIdentifier(), update.status);
                        } catch (IOException | MonitorException e) {
                            if (update.analytics != null) {
                                logger.warn("Unable to update status for method {} {}, {}: {}",
                                            update.analytics.getMethodName(),
                                            update.status,
                                            e.getClass().getName(),
                                            e.getMessage());
                            } else {
                                logger.warn("Unable to update status {}, {}: {}",
                                            update.status, e.getClass().getName(), e.getMessage());
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error("Interrupted", e);
                    break;
                }
            }
        }
    }
}
