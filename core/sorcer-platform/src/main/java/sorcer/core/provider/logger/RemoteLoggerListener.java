package sorcer.core.provider.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import org.rioproject.impl.client.ServiceDiscoveryAdapter;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.RemoteLogger;
import sorcer.service.Accessor;
import sorcer.util.ServiceAccessor;
import sorcer.util.Sorcer;

import java.io.PrintStream;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SORCER class
 * User: prubach
 * Date: 05.06.14
 */
public class RemoteLoggerListener implements RemoteEventListener {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RemoteLoggerListener.class);

    private static final int MIN_LEASE = 30000;
    private RemoteEventListener proxy = null;
    private Exporter exporter = null;
    private final Map<RemoteLogger, EventRegistration> eventRegistrations = new HashMap<>();
    private final List<Map<String,String>> filterMapList;
    private final PrintStream out;
    private LookupCache lookupCache;

    public RemoteLoggerListener(List<Map<String, String>> filterMapList) throws LoggerRemoteException {
        this(filterMapList, System.err);
    }

    public RemoteLoggerListener(List<Map<String, String>> filterMapList, PrintStream out) throws LoggerRemoteException {
        this.out = out;
        this.filterMapList = filterMapList;
        ServiceAccessor serviceAccessor = (ServiceAccessor) Accessor.get();
        ServiceTemplate serviceTemplate = new ServiceTemplate(null, new Class[] { RemoteLogger.class }, null);
        try {
            export();
            lookupCache = serviceAccessor.getServiceDiscoveryManager().createLookupCache(serviceTemplate, null, new RemoteLoggerDiscoveryListener());
        } catch (UnknownHostException | RemoteException e) {
            throw new LoggerRemoteException("Exception while initializing Listener ", e);
        }
    }

    class RemoteLoggerDiscoveryListener extends ServiceDiscoveryAdapter {

        @Override public void serviceAdded(ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPostEventServiceItem();
            logger.info("Discovered {}", item);
            register((RemoteLogger) item.service);
        }

        @Override public void serviceRemoved(ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPreEventServiceItem();
            if(eventRegistrations.remove((RemoteLogger)item.service)!=null)
                logger.debug("Removed {}", item.service);
        }
    }


    private void export() throws ExportException, UnknownHostException {
        if(exporter==null)
            exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(Sorcer.getHostAddress(), 0), new BasicILFactory());
        if(proxy==null)
            proxy = (RemoteEventListener)exporter.export(this);
    }

    void register(RemoteLogger... loggers)  {
        if (loggers.length==0) {
            logger.warn("No remoteLoggers found");
            return;
        }

        for (RemoteLogger remoteLogger : loggers) {
            try {
                logger.debug("Registering with remote logger: {}", remoteLogger);
                EventRegistration eventRegistration = remoteLogger.registerLogListener(proxy, null, Lease.FOREVER, filterMapList);
                logger.debug("Got registration {} {} {}",
                             eventRegistration.getID(), eventRegistration.getSource(), eventRegistration.toString());
                Lease providersEventLease = eventRegistration.getLease();
                LeaseRenewalManager lrm = new LeaseRenewalManager();
                providersEventLease.renew(Lease.ANY);
                lrm.renewUntil(providersEventLease, Lease.FOREVER, MIN_LEASE, null);
                eventRegistrations.put(remoteLogger, eventRegistration);
            } catch(LeaseDeniedException | UnknownLeaseException | RemoteException e) {
                logger.warn("Unable to register to remoteLogger {}", remoteLogger, e);
            }
        }
    }

    public void destroy() {
        for (Map.Entry<RemoteLogger, EventRegistration> entry : eventRegistrations.entrySet()) {
            try {
                entry.getKey().unregisterLogListener(entry.getValue());
            } catch (RemoteException e) {
                logger.warn("Error unregistering from {}", entry.getKey(), e);
            }
        }
        if (exporter != null)
            exporter.unexport(true);
    }

    @Override
    public void notify(RemoteEvent remoteEvent) throws UnknownEventException, RemoteException {
        LoggerRemoteEvent logEvent = (LoggerRemoteEvent)remoteEvent;
        ILoggingEvent le = logEvent.getLoggingEvent();
        // Print everything to the out stream as if it was a local log
        String mogId = le.getMDCPropertyMap().get(RemoteLogger.KEY_MOGRAM_ID);
        String prvId = le.getMDCPropertyMap().get(RemoteLogger.KEY_PROVIDER_ID);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        out.print(le.getLevel() + "  " + sdf.format(le.getTimeStamp()) +
                " [" + (mogId != null ? mogId.substring(0, 8) : "NO MOGRAM ID") + "@" + (prvId != null ? prvId.substring(0, 8) : "NO PRV ID") + "] ");
        out.print(" " + le.getLoggerName() + " -");
        out.println(" " + le.getFormattedMessage());
        if (le.getCallerData() != null)
            for (StackTraceElement ste : le.getCallerData()) {
                out.println(ste.toString());
            }
//        logger.info(mogId);
//        ((ch.qos.logback.classic.Logger)logger).callAppenders(logEvent.getLoggingEvent());
    }
}
