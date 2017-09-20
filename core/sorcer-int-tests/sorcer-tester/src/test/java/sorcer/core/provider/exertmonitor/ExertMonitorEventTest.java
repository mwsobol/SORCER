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

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lease.LeaseRenewalManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.co.operator;
import sorcer.core.monitor.MonitorEvent;
import sorcer.core.monitor.MonitoringManagement;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.so.operator.exert;

/**
 * @author Dennis Reedy
 */
public class ExertMonitorEventTest {
    LeaseRenewalManager leaseRenewalManager;
    ExertMonitorListener monitorListener;
    @Before
    public void init() {
        Accessor.create();
        leaseRenewalManager = new LeaseRenewalManager();
    }

    @After
    public void cleanUp() throws RemoteException, UnknownLeaseException {
        leaseRenewalManager.cancel(monitorListener.eventRegistration.getLease());
        monitorListener.unexport();
    }

    @Test
    public void testEventNotification() throws MogramException, SignatureException, RemoteException, LeaseDeniedException {
        SorcerPrincipal principal = new SorcerPrincipal(System.getProperty("user.name"));
        principal.setId(System.getProperty("user.name"));
        MonitoringManagement mm = Accessor.get().getService("Exert Monitor", MonitoringManagement.class);
        assertNotNull(mm);
        monitorListener = new ExertMonitorListener();
        EventRegistration registration = mm.register(principal,
                                                     monitorListener.getRemoteEventListener(),
                                                     Lease.ANY);
        monitorListener.eventRegistration = registration;
        leaseRenewalManager.renewUntil(registration.getLease(), Lease.FOREVER, TimeUnit.MINUTES.toMillis(1), null);
        Task t5 = task("t5",
                       sig("add", Adder.class),
                       context("add", operator.inVal("arg/x1", 20.0),
                               operator.inVal("arg/x2", 80.0), outVal("result/y")),
                       strategy(Strategy.Access.PULL, Strategy.Wait.YES, Strategy.Monitor.YES));

        t5 = exert(t5);
        assertNotNull(context(t5).asis("context/checkpoint/time"));
        assertTrue(monitorListener.states.size()==4);
        assertTrue(monitorListener.states.get(0).equals("INSPACE"));
        assertTrue(monitorListener.states.get(1).equals("RUNNING"));
        assertTrue(monitorListener.states.get(2).equals("UPDATED"));
        assertTrue(monitorListener.states.get(3).equals("DONE"));
    }

    class ExertMonitorListener implements RemoteEventListener {
        Exporter exporter;
        RemoteEventListener remoteEventListener;
        EventRegistration eventRegistration;
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
            System.out.println(monitorEvent.getExertion().getProcessSignature() +
                               " " +
                               Exec.State.name(monitorEvent.getCause()));
            notified++;
            states.add(Exec.State.name(monitorEvent.getCause()));
        }
    }
}
