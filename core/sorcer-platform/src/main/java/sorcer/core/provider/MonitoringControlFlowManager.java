/*
 * Copyright 2014 Sorcersoft.com S.A.
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

package sorcer.core.provider;

import net.jini.config.ConfigurationException;
import net.jini.core.lease.Lease;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.lease.LeaseRenewalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.monitor.MonitorSessionManagement;
import sorcer.core.monitor.MonitoringManagement;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.provider.rendezvous.RendezvousBean;
import sorcer.service.*;

import java.rmi.RemoteException;

import static sorcer.core.monitor.MonitorUtil.getMonitoringSession;
import static sorcer.service.Exec.FAILED;

/**
 * @author Rafał Krupiński
 */
public class MonitoringControlFlowManager extends ControlFlowManager {
    final private static Logger log = LoggerFactory.getLogger(MonitoringControlFlowManager.class);

    public static final long LEASE_RENEWAL_PERIOD = 1 * 1000 * 30L;
    public static final long DEFAULT_TIMEOUT_PERIOD = 1 * 1000 * 90L;

    private MonitorSessionManagement sessionMonitor;

    private LeaseRenewalManager lrm;

    public MonitoringControlFlowManager(Exertion exertion, ProviderDelegate delegate) throws RemoteException, ConfigurationException {
        super(exertion, delegate);
        sessionMonitor = Accessor.getService(MonitoringManagement.class);
        lrm = new LeaseRenewalManager();
    }

    public MonitoringControlFlowManager(Exertion exertion, ProviderDelegate delegate, RendezvousBean rendezvousBean) throws RemoteException, ConfigurationException {
        super(exertion, delegate, rendezvousBean);
        sessionMonitor = Accessor.getService(MonitoringManagement.class);
        lrm = new LeaseRenewalManager();
    }

    public MonitoringControlFlowManager(Exertion exertion, ProviderDelegate delegate, Jobber jobber) throws RemoteException, ConfigurationException {
        super(exertion, delegate, jobber);
        sessionMonitor = Accessor.getService(MonitoringManagement.class);
        lrm = new LeaseRenewalManager();
    }

    public MonitoringControlFlowManager(Exertion exertion, ProviderDelegate delegate, Concatenator concatenator) throws RemoteException, ConfigurationException {
        super(exertion, delegate, concatenator);
        sessionMonitor = Accessor.getService(MonitoringManagement.class);
        lrm = new LeaseRenewalManager();
    }

    @Override
    public Exertion process() throws ExertionException {
        MonitoringSession monSession = getMonitoringSession(exertion);
        if (sessionMonitor==null)
            logger.error("Monitoring enabled but ExertMonitor service could not be found!");
        try {
            if (monSession==null && sessionMonitor!=null) {
                logger.info("No Monitor Session, registering for: " + exertion.getName());
                exertion = register(exertion);
            }
        } catch (RemoteException e) {
            throw new ExertionException(e);
        }
        monSession = getMonitoringSession(exertion);

        try {
            if (sessionMonitor!=null && !(exertion instanceof CompoundExertion)) {
                monSession.init((Monitorable) delegate.getProvider().getProxy(), LEASE_RENEWAL_PERIOD,
                        DEFAULT_TIMEOUT_PERIOD);
                lrm.renewUntil(monSession.getLease(), Lease.ANY, null);
            }
            ServiceExertion result = (ServiceExertion) super.process();
            Exec.State resultState = result.getStatus() <= FAILED ? Exec.State.FAILED : Exec.State.DONE;

            try {
                if (sessionMonitor!=null && !(result instanceof CompoundExertion))
                    monSession.changed(result.getContext(), result.getControlContext(), resultState.ordinal());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Error while executing monitorable exertion", e);
                result.reportException(e);
                throw new ExertionException(e);
            }
            return result;
        } catch (RemoteException e) {
            String msg = "RemoteException from local call";
            log.error(msg,e);
            throw new IllegalStateException(msg, e);
        } catch (MonitorException e) {
            String msg = "RemoteException from local call";
            log.error(msg,e);
            throw new IllegalStateException(msg, e);
        } finally {
            try {
                if (sessionMonitor!=null && !(exertion instanceof CompoundExertion))
                    lrm.remove(monSession.getLease());
            } catch (UnknownLeaseException e) {
                log.warn("Error while removing lease for {}", exertion.getName(), e);
            }
        }
    }

    private Exertion register(Exertion exertion) throws RemoteException {

        ServiceExertion registeredExertion = (ServiceExertion) (sessionMonitor.register(null,
                exertion, DEFAULT_TIMEOUT_PERIOD));
        MonitoringSession session = getMonitoringSession(registeredExertion);
        log.debug("Session for the exertion = {}", session);
        log.debug("Lease to be renewed for duration = {}",
                (session.getLease().getExpiration() - System
                        .currentTimeMillis())
        );
        lrm.renewUntil(session.getLease(), Lease.ANY, null);
        return registeredExertion;
    }
}
