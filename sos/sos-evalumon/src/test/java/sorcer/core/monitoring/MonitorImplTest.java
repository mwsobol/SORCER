package sorcer.core.monitoring;

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rioproject.cybernode.StaticCybernode;
import org.rioproject.deploy.ServiceBeanInstantiationException;

import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dennis Reedy
 */
public class MonitorImplTest {
    private static MonitorImpl monitor;

    @BeforeClass
    public static void create() throws ServiceBeanInstantiationException {
        StaticCybernode cybernode = new StaticCybernode();
        monitor = (MonitorImpl) cybernode.activate(MonitorImpl.class.getName());
    }

    @Test
    public void testInjection() throws ServiceBeanInstantiationException {
        assertNotNull(monitor);
        assertNotNull(monitor.getProxy());
        assertNotNull(monitor.getConfig());
    }

    @Test
    public void testRegistration() throws MonitorException, ExportException {
        MonitorRegistration registration = monitor.register("spacely-sprockets",
                                                            System.getProperty("user.name"),
                                                            TimeUnit.MINUTES.toMillis(5));
        assertNotNull(registration.getMonitor());
        assertNotNull(registration);
        MonitorListener monitorListener = new MonitorListener();
        EventRegistration eventRegistration = monitor.register(new MonitorEventFilter(System.getProperty("user.name")),
                                                               monitorListener.getRemoteEventListener(),
                                                               TimeUnit.MINUTES.toMillis(5));
        assertNotNull(eventRegistration);
        monitor.update(registration, Monitor.Status.SUBMITTED, null);
        monitor.update(registration, Monitor.Status.COMPLETED, null);

        registration = monitor.register("spacely-sprockets",
                                        System.getProperty("user.name"),
                                        TimeUnit.MINUTES.toMillis(5));
        monitor.update(registration, Monitor.Status.SUBMITTED, null);
        monitor.update(registration, Monitor.Status.FAILED, null);
        assertTrue(monitorListener.states.size()==4);
    }

    @Test(expected=MonitorException.class)
    public void testRegistrationSendFailure() throws MonitorException, RemoteException, UnknownLeaseException {
        MonitorRegistration registration = monitor.register("spacely-sprockets",
                                                            System.getProperty("user.name"),
                                                            TimeUnit.MINUTES.toMillis(5));
        assertNotNull(registration.getMonitor());
        assertNotNull(registration);
        MonitorListener monitorListener = new MonitorListener();
        EventRegistration eventRegistration = monitor.register(new MonitorEventFilter(),
                                                               monitorListener.getRemoteEventListener(),
                                                               TimeUnit.MINUTES.toMillis(5));
        assertNotNull(eventRegistration);
        monitor.update(registration, Monitor.Status.SUBMITTED, null);
        monitor.update(registration, Monitor.Status.COMPLETED, null);
        registration.getLease().cancel();
        monitor.update(registration, Monitor.Status.SUBMITTED, null);
    }

    @Test
    public void testRegistrationWithNames() throws MonitorException, ExportException {
        MonitorRegistration registration = monitor.register("spacely-sprockets",
                                                            System.getProperty("user.name"),
                                                            TimeUnit.MINUTES.toMillis(5));
        assertNotNull(registration.getMonitor());
        assertNotNull(registration);
        MonitorListener monitorListener = new MonitorListener();
        List<String> names = new ArrayList<>();
        names.add(registration.getIdentifier());
        EventRegistration eventRegistration = monitor.register(new MonitorEventFilter(null, names),
                                                               monitorListener.getRemoteEventListener(),
                                                               TimeUnit.MINUTES.toMillis(5));
        assertNotNull(eventRegistration);
        monitor.update(registration, Monitor.Status.SUBMITTED, null);
        monitor.update(registration, Monitor.Status.COMPLETED, null);

        monitor.update(registration, Monitor.Status.SUBMITTED, null);
        monitor.update(registration, Monitor.Status.FAILED, null);
        assertTrue(monitorListener.states.size()==4);
    }

    @Test
    public void testRegistrationWithBadNames() throws MonitorException, ExportException {
        MonitorRegistration registration = monitor.register("spacely-sprockets",
                                                            System.getProperty("user.name"),
                                                            TimeUnit.MINUTES.toMillis(5));
        assertNotNull(registration.getMonitor());
        assertNotNull(registration);
        MonitorListener monitorListener = new MonitorListener();
        List<String> names = new ArrayList<>();
        names.add(registration.getIdentifier()+"foo");
        EventRegistration eventRegistration = monitor.register(new MonitorEventFilter(System.getProperty("user.name"), names),
                                                               monitorListener.getRemoteEventListener(),
                                                               TimeUnit.MINUTES.toMillis(5));
        assertNotNull(eventRegistration);
        monitor.update(registration, Monitor.Status.SUBMITTED, null);
        monitor.update(registration, Monitor.Status.COMPLETED, null);

        monitor.update(registration, Monitor.Status.SUBMITTED, null);
        monitor.update(registration, Monitor.Status.FAILED, null);
        assertTrue(monitorListener.states.size()==0);
    }

    class MonitorListener implements RemoteEventListener {
        Exporter exporter;
        RemoteEventListener remoteEventListener;
        int notified = 0;
        List<String> states = new LinkedList<>();

        RemoteEventListener getRemoteEventListener() throws ExportException {
            if (remoteEventListener == null) {
                if (exporter == null)
                    exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                                     new BasicILFactory());

                remoteEventListener = (RemoteEventListener) exporter.export(this);
            }
            return remoteEventListener;
        }

        void unexport() {
            if (exporter != null) {
                exporter.unexport(true);
            }
        }

        @Override public void notify(RemoteEvent remoteEvent) throws UnknownEventException, RemoteException {
            MonitorEvent monitorEvent = (MonitorEvent) remoteEvent;
            System.out.println(monitorEvent.getIdentifier() +" " +monitorEvent.getStatus().name());
            notified++;
            states.add(monitorEvent.getStatus().name());
        }
    }
}