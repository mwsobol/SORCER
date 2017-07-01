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

import java.io.IOException;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Dennis Reedy
 */
public class MonitorEventHandler {
    private LeasedListManager resourceMgr;
    private LandlordLessor landlord;
    private final AtomicInteger sequenceNumber = new AtomicInteger(1);
    private static final SecureRandom idGen = new SecureRandom();
    private ProxyPreparer listenerPreparer = new BasicProxyPreparer();
    private static Logger logger = LoggerFactory.getLogger(MonitorEventHandler.class);

    MonitorEventHandler(Configuration config) throws IOException {
        resourceMgr = new LeasedListManager();
        landlord = new LandlordLessor(config);
        landlord.addLeaseListener(resourceMgr);
        try {
            listenerPreparer = (ProxyPreparer) config.getEntry("sorcer.modeling.monitor",
                                                               "eventListenerPreparer",
                                                               ProxyPreparer.class,
                                                               listenerPreparer);
        } catch (ConfigurationException e) {
            logger.warn("Failed getting the sorcer.modeling.monitor.eventListenerPreparer " +
                        "from configuration, use default");
        }
    }

    /**
     * Registers an EventRegistration.
     *
     * @param eventSource The event source
     * @param listener RemoteEventListener
     * @param eventFilter The MonitorEventFilter to filter with
     * @param duration Requested EventRegistration lease <br>
     * @return EventRegistration <br>
     *
     * @throws LeaseDeniedException If the lease manager denies the lease
     */
    EventRegistration register(Object eventSource,
                               RemoteEventListener listener,
                               MonitorEventFilter eventFilter,
                               long duration) throws LeaseDeniedException, RemoteException {
        RemoteEventListener preparedListener = (RemoteEventListener)listenerPreparer.prepareProxy(listener);
        EventRegistrationResource resource = new EventRegistrationResource(preparedListener, eventFilter);
        ServiceResource sr = new ServiceResource(resource);
        Lease lease = landlord.newLease(sr, duration);
        EventRegistration registration = new EventRegistration(nextID(),
                                                               eventSource,
                                                               lease,
                                                               sequenceNumber.getAndIncrement());
        if(logger.isDebugEnabled())
            logger.debug("Created EventRegistration, total registrations now {}", getRegistrantCount());
        return (registration);
    }

    void fire(MonitorEvent monitorEvent) {
        if(resourceMgr.getServiceResources().length==0) {
            if(logger.isDebugEnabled())
                logger.debug("There are no leased registrations, cancel sending {} {}, status: {}",
                             monitorEvent.getIdentifier(), monitorEvent.getOwner(), monitorEvent.getStatus().name());
            return;
        }
		if(logger.isDebugEnabled())
            logger.debug("Fire a MonitorEvent, num registrations: {}", getRegistrantCount());
        for(ServiceResource sr : resourceMgr.getServiceResources()) {
            EventRegistrationResource er = (EventRegistrationResource) sr.getResource();
            MonitorEventFilter eventFilter = er.getEventFilter();
            if (eventFilter == null || eventFilter.accept(monitorEvent)) {
                RemoteEventListener listener = er.getListener();
                try {
                    monitorEvent.setSequenceNumber(er.sequenceNumber.incrementAndGet());
                    listener.notify(monitorEvent);
                } catch (UnknownEventException | RemoteException e) {
                    logger.warn("Failed notifying listener", e);
                    try {
                        resourceMgr.removeResource(sr);
                        landlord.cancel(sr.getCookie());
                    } catch (Exception ex) {
                        logger.warn("Removing/Cancelling an EventConsumer from UnknownEventException", ex);
                    }
                }
            } else {
                if(logger.isDebugEnabled())
                    logger.debug("Failed to match MonitorEvent, filtered with {}", eventFilter);
            }
        }
    }

    /**
     * Terminates this EventHandler. This causes all event registrant leases to
     * be cancelled , and if any watches have been created those watches will be
     * destroyed
     */
    @SuppressWarnings("unused")
    public void terminate() {
        landlord.removeAll();
        landlord.stop(true);
    }

    private int getRegistrantCount() {
        return (landlord.total());
    }

    /**
     * Generate a new ID. IDs are generated from a
     * <code>SecureRandom</code> object so that it is next to
     * impossible to forge an ID and so we don't have
     * to remember/restore a counter cross restarts.
     */
    private static long nextID() {
        return idGen.nextLong();
    }

    /**
     * Container class for event registration objects that are created and
     * behave as the resource that is being leased and controlled by the
     * ServiceResource
     */
    static class EventRegistrationResource {
        private RemoteEventListener listener;
        private MonitorEventFilter eventFilter;
        private AtomicLong sequenceNumber = new AtomicLong(0);

        EventRegistrationResource(RemoteEventListener listener,
                                         MonitorEventFilter eventFilter) {
            this.listener = listener;
            this.eventFilter = eventFilter;
        }
        RemoteEventListener getListener() {
            return (listener);
        }

        MonitorEventFilter getEventFilter() {
            return eventFilter;
        }
    }
}
