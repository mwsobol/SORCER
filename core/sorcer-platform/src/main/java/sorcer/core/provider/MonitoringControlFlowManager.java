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
import sorcer.core.provider.rendezvous.SorcerExerterBean;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

import static sorcer.core.dispatch.MogramDispatcherFactory.DEFAULT_LEASE_PERIOD;
import static sorcer.core.dispatch.MogramDispatcherFactory.DEFAULT_TIMEOUT_PERIOD;
import static sorcer.core.monitor.MonitorUtil.getMonitoringSession;
import static sorcer.service.Exec.FAILED;

/**
 * @author Rafał Krupiński
 */
public class MonitoringControlFlowManager extends ControlFlowManager {
    final private static Logger log = LoggerFactory.getLogger(MonitoringControlFlowManager.class);
    private MonitorSessionManagement sessionMonitor;
    private LeaseRenewalManager lrm;

    public MonitoringControlFlowManager(Routine exertion,
                                        ProviderDelegate delegate) throws RemoteException, ConfigurationException {
        super(exertion, delegate);
        sessionMonitor = Accessor.get().getService(null, MonitoringManagement.class);
        lrm = new LeaseRenewalManager(delegate.getProviderConfiguration());
    }

    public MonitoringControlFlowManager(Routine exertion,
                                        ProviderDelegate delegate,
                                        SorcerExerterBean serviceBean) throws RemoteException, ConfigurationException {
        super(exertion, delegate, serviceBean);
        sessionMonitor = Accessor.get().getService(null, MonitoringManagement.class);
        lrm = new LeaseRenewalManager(delegate.getProviderConfiguration());
    }

    @Override
    public Routine process() throws RoutineException {
        MonitoringSession monSession = getMonitoringSession(exertion);
        if (sessionMonitor==null) {
            logger.error("Monitoring enabled but ExertMonitor service could not be found!");
            return (Routine) super.process();
        }
        try {
            if (monSession==null) {
                logger.info("No Monitor Session, registering for: " + exertion.getName());
                exertion = register(exertion);
            }
        } catch (RemoteException | MonitorException e) {
            logger.error("Failed registering exertion into monitoring session", e);
        }
        monSession = getMonitoringSession(exertion);
        ServiceRoutine result;
        if (!(exertion instanceof Transroutine) && !exertion.isSpacable()) {
            try {
                monSession.init((Monitorable) delegate.getProxy(), DEFAULT_LEASE_PERIOD, DEFAULT_TIMEOUT_PERIOD);
                lrm.renewUntil(monSession.getLease(), Lease.FOREVER, TimeUnit.MINUTES.toMillis(1), null);
                logger.info("Lease: {}", monSession.getLease());
            } catch (RemoteException | MonitorException e) {
                log.error("Failed issuing monitoring calls", e);
            }

            result = (ServiceRoutine) super.process();
            logger.info("Got result: {}", result);
            Exec.State resultState = result.getStatus() <= FAILED ? Exec.State.FAILED : Exec.State.DONE;

            try {
                if (sessionMonitor != null &&
                    monSession.getState() != resultState.ordinal() &&
                    !(result instanceof Transroutine))
                    monSession.changed(result.getContext(), result.getControlContext(), resultState.ordinal());
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
            } catch (RemoteException | ContextException | MonitorException e) {
                log.warn("Error while executing monitorable exertion", e);
            } finally {
                try {
                    if (sessionMonitor != null && !(exertion instanceof Transroutine))
                        lrm.remove(monSession.getLease());
                } catch (UnknownLeaseException e) {
                    log.warn("Error while removing lease for {}", exertion.getName(), e);
                }
            }
        } else {
            result = (ServiceRoutine) super.process();
        }
        return result;
    }

    private Routine register(Routine exertion) throws RemoteException, MonitorException {

        ServiceRoutine registeredExertion = (ServiceRoutine) (sessionMonitor.register(null,
                                                                                        exertion,
                                                                                        DEFAULT_LEASE_PERIOD));
        MonitoringSession session = getMonitoringSession(registeredExertion);
        log.info("Session for the exertion = {}", session);
        log.info("Lease to be renewed for duration = {}",
                 (session.getLease().getExpiration() - System.currentTimeMillis()));
        lrm.renewUntil(session.getLease(), Lease.FOREVER, TimeUnit.MINUTES.toMillis(1), null);
        return registeredExertion;
    }
}
