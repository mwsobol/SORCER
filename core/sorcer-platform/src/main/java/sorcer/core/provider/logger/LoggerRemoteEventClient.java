package sorcer.core.provider.logger;

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SORCER class
 * User: prubach
 * Date: 05.06.14
 */
public class LoggerRemoteEventClient {


        private static final int MIN_LEASE = 30000;
        private RemoteEventListener proxy = null;
        private Exporter exporter = null;
        private boolean running = true;
        private EventRegistration eventRegistration = null;
        private List<Map<String,String>> filterMapList;
        private List<RemoteLogger> loggers = new ArrayList<RemoteLogger>();

        private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LoggerRemoteEventClient.class);

		/*
		 * The arguments should be passed as proxies such that they
		 * can be used directly by client.
		 *
		 */

        public void register(List<Map<String,String>> filterMapList, RemoteEventListener remoteEventListener) throws LoggerRemoteException {
            ServiceItem[] sItems = Accessor.getServiceItems(RemoteLogger.class, null);
            List<RemoteLogger> lrs = new ArrayList<RemoteLogger>();
            for (ServiceItem sItem : sItems) {
                lrs.add((RemoteLogger) sItem.service);
            }
            register(lrs, filterMapList, remoteEventListener);
        }


        public void register(List<RemoteLogger> loggers, List<Map<String, String>> filterMapList, RemoteEventListener remoteEventListener) throws LoggerRemoteException {
            try {
                if (loggers.isEmpty()) throw new LoggerRemoteException("No remoteLoggers found");
                this.loggers = loggers;
                this.filterMapList = filterMapList;
                //Make a proxy of myself to pass to the server/filter
                exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(Sorcer.getHostAddress(), 0),
                        new BasicILFactory());
                proxy = (RemoteEventListener)exporter.export(remoteEventListener);

                //register as listener with server and passing the
                //event registration to the filter while registering there.
                for (RemoteLogger remoteLogger : loggers) {
                    logger.debug("Registering with event server: " + remoteLogger);
                    eventRegistration = remoteLogger.registerLogListener(proxy, null, Lease.FOREVER, filterMapList);
                    logger.debug("Got registration " + eventRegistration.getID() + " " + eventRegistration.getSource() + " " + eventRegistration.toString());
                    Lease providersEventLease = eventRegistration.getLease();
                    LeaseRenewalManager lrm = new LeaseRenewalManager();
                    providersEventLease.renew(Lease.ANY);
                    lrm.renewUntil(providersEventLease, Lease.FOREVER, MIN_LEASE, null);
                }
            } catch (Exception e){
                throw new LoggerRemoteException("Exception while initializing Listener client " ,e);
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
                throw new LoggerRemoteException("Problem destroying LoggerEventClient: " ,re);
            }
        }
}
