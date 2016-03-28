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
package sorcer.core.provider.exertmonitor;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.impl.service.LandlordLessor;
import org.rioproject.impl.service.LeasedListManager;
import org.rioproject.impl.service.ServiceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.monitor.MonitorEvent;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.ServiceExertion;

import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dennis Reedy
 */
public class ExertMonitorEventHandler {
    private LeasedListManager resourceMgr;
    private LandlordLessor landlord;
    private final AtomicInteger sequenceNumber = new AtomicInteger(1);
    private static final SecureRandom idGen = new SecureRandom();
    private ProxyPreparer listenerPreparer = new BasicProxyPreparer();
    private static Logger logger = LoggerFactory.getLogger(ExertMonitorEventHandler.class);

    public ExertMonitorEventHandler(Configuration config) throws RemoteException {
        resourceMgr = new LeasedListManager();
        landlord = new LandlordLessor(config);
        landlord.addLeaseListener(resourceMgr);
        try {
            listenerPreparer = (ProxyPreparer) config.getEntry("sorcer.core.provider.exertmonitor",
                                                               "eventListenerPreparer",
                                                               ProxyPreparer.class,
                                                               listenerPreparer);
        } catch (ConfigurationException e) {
            logger.warn("Failed getting the sorcer.core.provider.exertmonitor.eventListenerPreparer " +
                        "from configuration, use default");
        }
    }

    /**
     * Registers an EventRegistration.
     *
     * @param eventSource The event source
     * @param listener RemoteEventListener
     * @param principal The Principal to filter on
     * @param duration Requested EventRegistration lease <br>
     * @return EventRegistration <br>
     *
     * @throws LeaseDeniedException If the lease manager denies the lease
     */
    public EventRegistration register(Object eventSource,
                                      RemoteEventListener listener,
                                      SorcerPrincipal principal,
                                      long duration) throws LeaseDeniedException, RemoteException {
        RemoteEventListener preparedListener = (RemoteEventListener)listenerPreparer.prepareProxy(listener);
        EventRegistrationResource resource = new EventRegistrationResource(preparedListener, principal);
        ServiceResource sr = new ServiceResource(resource);
        Lease lease = landlord.newLease(sr, duration);
        EventRegistration registration = new EventRegistration(nextID(),
                                                               eventSource,
                                                               lease,
                                                               sequenceNumber.getAndIncrement());
        if(logger.isInfoEnabled())
            logger.info("Created EventRegistration for {}, total registrations now {}",
                        (principal==null?"null":principal.getId()), getRegistrantCount());
        return (registration);
    }

    public void fire(MonitorEvent monitorEvent) {
        if(resourceMgr.getServiceResources().length==0)
            return;
        logger.info("Fire a MonitorEvent, num registrations: {}", getRegistrantCount());
        for(ServiceResource sr : resourceMgr.getServiceResources()) {
            EventRegistrationResource er = (EventRegistrationResource) sr.getResource();
            SorcerPrincipal principal = er.getPrincipal();
            ServiceExertion xrt = (ServiceExertion) monitorEvent.getExertion();
            if (principal == null || xrt.getPrincipal().getId().equals(principal.getId())) {
                RemoteEventListener listener = er.getListener();
                try {
                    listener.notify(monitorEvent);
                } catch (UnknownEventException | RemoteException e) {
                    logger.warn("Failed notifying listener for {}, {}", (principal==null?"null":principal.getId()),
                                e);
                    try {
                        resourceMgr.removeResource(sr);
                        landlord.cancel(sr.getCookie());
                    } catch (Exception ex) {
                        logger.warn("Removing/Cancelling an EventConsumer from UnknownEventException", ex);
                    }
                }
            } else {
                logger.info("Failed to match principal {}, filtered with {}", xrt.getPrincipal().getId(), principal.getId());
            }
        }
    }

    /**
     * Terminates this EventHandler. This causes all event registrant leases to
     * be cancelled , and if any watches have been created those watches will be
     * destroyed
     */
    public void terminate() {
        landlord.removeAll();
        landlord.stop(true);
    }

    int getRegistrantCount() {
        return (landlord.total());
    }

    /**
     * Generate a new ID. IDs are generated from a
     * <code>SecureRandom</code> object so that it is next to
     * impossible to forge an ID and so we don't have
     * to remember/restore a counter cross restarts.
     */
    static long nextID() {
        return idGen.nextLong();
    }

    /**
     * Container class for event registration objects that are created and
     * behave as the resource that is being leased and controlled by the
     * ServiceResource
     */
    static class EventRegistrationResource {
        private RemoteEventListener listener;
        private SorcerPrincipal principal;

        public EventRegistrationResource(RemoteEventListener listener,
                                         SorcerPrincipal principal) {
            this.listener = listener;
            this.principal = principal;
        }
        public RemoteEventListener getListener() {
            return (listener);
        }

        public SorcerPrincipal getPrincipal() {
            return principal;
        }
    }
}
