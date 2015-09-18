package sorcer.util;

import net.jini.config.Configuration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.impl.event.AbstractEventHandler;
import org.rioproject.impl.service.ServiceResource;
import org.rioproject.impl.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;

import static java.lang.String.format;

/**
 * SORCER class
 * User: prubach
 * Date: 09.09.14
 */
public class SenderEventHandler extends AbstractEventHandler {
        static Logger logger = LoggerFactory.getLogger(SenderEventHandler.class);

        /**
         * Construct a DispatchEventHandler with an EventDescriptor and default
         * lease maximum and time allocation
         *
         * @param descriptor The EventDescriptor
         *
         * @throws java.io.IOException If a landlord lease manager cannot be created
         */
        public SenderEventHandler(EventDescriptor descriptor) throws IOException {
            super(descriptor);
        }

        /**
         * Construct a DispatchEventHandler with an EventDescriptor and a
         * Configuration object
         *
         * @param descriptor The EventDescriptor
         * @param config The configuration object
         *
         * @throws IOException If a landlord lease manager cannot be created
         */
        public SenderEventHandler(EventDescriptor descriptor, Configuration config) throws IOException {
            super(descriptor, config);
        }

        /**
         * Implement the <code>fire</code> method from <code>EventHandler</code>
         */
        public void fire(RemoteServiceEvent event) {
            event.setEventID(descriptor.eventID);
            event.setSequenceNumber(sequenceNumber);
            ServiceResource[] resources = resourceMgr.getServiceResources();

            if (resources.length==0) {
                try {
                    Thread.sleep(10);
                    resources = resourceMgr.getServiceResources();

                    if (resources.length==0) logger.warn(format("STILL 0!!! SenderEventHandler: notify [%d] listeners " +
                                    "with event [%s]", landlord.getLeasedResources().length,
                            event.getClass().getName()));
                    else logger.trace(format("SenderEventHandler: notify [%d] listeners " +
                                    "with event [%s]", resources.length,
                            event.getClass().getName()));
                } catch (InterruptedException ie) {
                }
            } else if(logger.isTraceEnabled())
                logger.trace(format("SenderEventHandler: notify [%d] listeners " +
                                "with event [%s]", resources.length,
                        event.getClass().getName()));
            for (ServiceResource sr : resources) {
                EventRegistrationResource er =
                        (EventRegistrationResource) sr.getResource();
                if (!landlord.ensure(sr)) {
                    if (logger.isTraceEnabled())
                        logger.trace(format("SenderEventHandler.fire() Could not ensure " +
                                        "lease for ServiceResource " +
                                        "[%s] resources count now : %d",
                                er.getListener().getClass().getName(),
                                resourceMgr.getServiceResources().length));
                    try {
                        resourceMgr.removeResource(sr);
                        landlord.remove(sr);
                    } catch (Exception e) {
                        if (logger.isTraceEnabled())
                            logger.trace("Removing Resource and Cancelling Lease", e);
                    }
                    continue;
                }
                RemoteEventListener listener = null;
                try {
                    listener = er.getListener();
                    MarshalledObject handback = er.getHandback();
                    event.setHandback(handback);
                    t0 = System.currentTimeMillis();
                    listener.notify(event);
                    t1 = System.currentTimeMillis();
                    sendTime = t1 - t0;
                    if (responseWatch != null)
                        responseWatch.setElapsedTime(sendTime, t1);
                    sent++;
                    printStats();
                } catch (UnknownEventException uee) {
                    if (logger.isTraceEnabled())
                        logger.trace(format("UnknownEventException for EventDescriptor [%s]", descriptor.toString()), uee);
                /* We are allowed to cancel the lease here */
                    try {
                        resourceMgr.removeResource(sr);
                        landlord.cancel(sr.getCookie());
                    } catch (Exception e) {
                        if (logger.isTraceEnabled())
                            logger.warn("Removing resource and cancelling Lease", e);
                    }
                } catch (RemoteException re) {
                    if (logger.isTraceEnabled())
                        logger.trace(format("fire() for EventDescriptor [%s]", descriptor.toString()), re);

                    try {
                        Thread.sleep(10);
                        listener.notify(event);
                        t1 = System.currentTimeMillis();
                        sendTime = t1 - t0;
                        if (responseWatch != null)
                            responseWatch.setElapsedTime(sendTime, t1);
                        sent++;
                        printStats();
                    } catch (Exception eee) {
                        logger.trace(format("Still problem with fire() for EventDescriptor [%s]", descriptor.toString()), eee);
                /* Cancel the Lease if the EventConsumer is unreachable */
                        if(!ThrowableUtil.isRetryable(re)) {
                            try {
                                resourceMgr.removeResource(sr);
                                landlord.cancel(sr.getCookie());
                            } catch (Exception e) {
                                if (logger.isTraceEnabled())
                                    logger.trace("Removing resource and cancelling Lease", e);
                            }
                        }
                    }
                }
            }
            sequenceNumber++;
        }
}
