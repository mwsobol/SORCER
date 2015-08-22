package sorcer.core.provider.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceItem;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lease.LeaseRenewalManager;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.RemoteLogger;
import sorcer.service.Accessor;
import sorcer.util.Sorcer;

import java.io.PrintStream;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private EventRegistration eventRegistration = null;
    private List<Map<String,String>> filterMapList;
    private List<RemoteLogger> loggers = new ArrayList<RemoteLogger>();
    private PrintStream out;
    public RemoteLoggerListener(PrintStream out) {
        this.out = out;
    }

    /*
	* The arguments should be passed as proxies such that they
	* can be used directly by listener.
	*/
    public void register(List<Map<String, String>> filterMapList) throws LoggerRemoteException {
        ServiceItem[] sItems = Accessor.getServiceItems(RemoteLogger.class, null);
        List<RemoteLogger> lrs = new ArrayList<RemoteLogger>();
        for (ServiceItem sItem : sItems) {
            lrs.add((RemoteLogger) sItem.service);
        }
        register(lrs, filterMapList);
    }


    public void register(List<RemoteLogger> loggers, List<Map<String, String>> filterMapList) throws LoggerRemoteException {
        try {
            if (loggers.isEmpty()) throw new LoggerRemoteException("No remoteLoggers found");
            this.loggers = loggers;
            this.filterMapList = filterMapList;
            //Make a proxy of myself to pass to the server/filter
            exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(Sorcer.getHostAddress(), 0),
                    new BasicILFactory());
            proxy = (RemoteEventListener)exporter.export(this);
            //register as listener with server and passing the
            //event registration to the filter while registering there.
            for (RemoteLogger remoteLogger : loggers) {
                logger.debug("Registering with remote logger: " + remoteLogger);
                eventRegistration = remoteLogger.registerLogListener(proxy, null, Lease.FOREVER, filterMapList);
                logger.debug("Got registration " + eventRegistration.getID() + " " + eventRegistration.getSource() + " " + eventRegistration.toString());
                Lease providersEventLease = eventRegistration.getLease();
                LeaseRenewalManager lrm = new LeaseRenewalManager();
                providersEventLease.renew(Lease.ANY);
                lrm.renewUntil(providersEventLease, Lease.FOREVER, MIN_LEASE, null);
            }
        } catch (Exception e){
            throw new LoggerRemoteException("Exception while initializing Listener " ,e);
        }
    }

    public void destroy() throws LoggerRemoteException {
        try {
            if (eventRegistration != null) {
                for (RemoteLogger remoteLogger : loggers) {
                    remoteLogger.unregisterLogListener(eventRegistration);
                }
            } else {
                throw new LoggerRemoteException("EventRegistration is NULL - maybe never registered to Remote Logger");
            }
            if (exporter != null) exporter.unexport(true);
        } catch (RemoteException re) {
            throw new LoggerRemoteException("Problem destroying RemoteLoggerListener: " ,re);
        }
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
