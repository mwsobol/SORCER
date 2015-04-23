/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
 * Copyright 2014 SorcerSoft.com S.A.
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

package sorcer.core.dispatch;

import net.jini.core.lease.Lease;
import net.jini.lease.LeaseRenewalManager;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.exertion.AltExertion;
import sorcer.core.exertion.LoopExertion;
import sorcer.core.exertion.OptExertion;
import sorcer.core.monitor.MonitorUtil;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.provider.Provider;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

/**
 * A dispatching class for exertion blocks in the PUSH mode.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"rawtypes", "unchecked" })

public class CatalogBlockDispatcher extends CatalogSequentialDispatcher {

	public CatalogBlockDispatcher(Exertion block, Set<Context> sharedContext,
			boolean isSpawned, Provider provider,
            ProvisionManager provisionManager) throws ContextException, RemoteException {
		super(block, sharedContext, isSpawned, provider, provisionManager);
        block.getDataContext().append((Context) block.getScope());
    }


    @Override
    protected void doExec() throws ExertionException, SignatureException {
        super.doExec();
		try {
			Condition.cleanupScripts(xrt);
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
	}

    @Override
    protected void beforeExec(Exertion exertion) throws ExertionException, SignatureException {
        super.beforeExec(exertion);
        try {
            preUpdate(exertion);
            if (exertion.getDataContext() != null) {
                if (exertion.getScope() != null)
                    ((Context) exertion.getScope()).append(xrt.getContext());
                else {
//                    exertion.setScope(xrt.getContext());
                    exertion.setScope(new ParModel());
                    ((Context)exertion.getScope()).append(xrt.getContext());
                }
            }
        } catch (Exception ex) {
            throw new ExertionException(ex);
        }
    }

    @Override
    protected void afterExec(Exertion result) throws ContextException, ExertionException {
        super.afterExec(result);
        try {
            postUpdate(result);
            if (result.getName().equals("t3")) {
                System.out.println("ZZZZZZZZZZZZZZ context: " + result.getContext());
                System.out.println("ZZZZZZZZZZZZZZ scope: " + result.getScope());
            }
            Condition.cleanupScripts(result);

            if (result.getName().equals("t3")) {
                System.out.println("ZZZZZZZZZZZZZZ after clean context: " + result.getContext());
                System.out.println("ZZZZZZZZZZZZZZ after clean scope: " + result.getScope());
            }

            //TODO Not very nice
            /*MonitoringSession monSession = MonitorUtil.getMonitoringSession(result);
            if (result.isBlock() && result.isMonitorable() && monSession!=null) {
                boolean isFailed = false;
                for (Exertion xrt : result.getAllMograms()) {
                    if (xrt.getStatus()==Exec.FAILED || xrt.getStatus()==Exec.ERROR) {
                        isFailed = true;
                        break;
                    }
                }
                monSession.changed(result.getContext(), (isFailed ? Exec.FAILED : Exec.DONE));
            }*/
        } catch (RemoteException e) {
            throw new ExertionException(e);
        /*} catch (MonitorException e) {
            throw new ExertionException(e);*/
        }
    }

    private void preUpdate(Exertion exertion) throws ContextException {
		if (exertion instanceof AltExertion) {
			for (OptExertion oe : ((AltExertion)exertion).getOptExertions()) {
                oe.getCondition().getConditionalContext().append(xrt.getContext());
				oe.getCondition().setStatus(null);
			}
            MonitoringSession monSession = MonitorUtil.getMonitoringSession(exertion);
            if (exertion.isMonitorable() && monSession!=null) {
                try {
                    monSession.init((Monitorable) provider.getProxy(), ExertionDispatcherFactory.LEASE_RENEWAL_PERIOD,
                            ExertionDispatcherFactory.DEFAULT_TIMEOUT_PERIOD);
                    if (getLrm()==null) setLrm(new LeaseRenewalManager());
                    getLrm().renewUntil(monSession.getLease(), Lease.FOREVER, ExertionDispatcherFactory.LEASE_RENEWAL_PERIOD, null);
                } catch (RemoteException re) {
                    logger.error("Problem initializing Monitor Session for: " + exertion.getName(), re);
                } catch (MonitorException e) {
                    logger.error("Problem initializing Monitor Session for: " + exertion.getName(), e);
                }
            }
		} else if (exertion instanceof OptExertion) {
			Context pc = ((OptExertion)exertion).getCondition().getConditionalContext();
			((OptExertion)exertion).getCondition().setStatus(null);
			if (pc == null) {
				pc = new ParModel(exertion.getName());
				((OptExertion)exertion).getCondition().setConditionalContext(pc);
            }
            pc.append(xrt.getContext());
		} else if (exertion instanceof LoopExertion) {
			((LoopExertion)exertion).getCondition().setStatus(null);
			Context pc = ((LoopExertion)exertion).getCondition().getConditionalContext();			
			if (pc == null) {
				pc = new ParModel(exertion.getName());
				((LoopExertion)exertion).getCondition().setConditionalContext(pc);
			}
			pc.append(xrt.getContext());
		}
	}
	
	private void postUpdate(Exertion exertion) throws ContextException, RemoteException {
		if (exertion instanceof AltExertion) {
			xrt.getContext().append(((AltExertion)exertion).getActiveOptExertion().getContext());
            /*MonitoringSession monSession = MonitorUtil.getMonitoringSession(exertion);
            if (exertion.isMonitorable() && monSession!=null) {
                try {
                    monSession.changed(exertion.getContext(), exertion.getStatus());
                    getLrm().remove(monSession.getLease());
                } catch (RemoteException re) {
                    logger.error("Problem initializing Monitor Session for: " + exertion.getName(), re);
                } catch (MonitorException e) {
                    logger.error("Problem initializing Monitor Session for: " + exertion.getName(), e);
                } catch (UnknownLeaseException e) {
                    logger.error("Problem removing monitoring lease for: " + exertion.getName(), e);
                }
            } */
		} else if (exertion instanceof OptExertion) {
			xrt.getContext().append(exertion.getContext());
		}

//		if (exertion instanceof AltExertion) {
//			((ParModel)((Block)xrt).getContext()).appendNew(((AltExertion)exertion).getActiveOptExertion().getContext());
//		} else if (exertion instanceof OptExertion) {
//			((ParModel)((Block)xrt).getContext()).appendNew(((OptExertion)exertion).getContext());
//		}
		
		ServiceContext cxt = (ServiceContext)xrt.getContext();
		if (exertion.getContext().getReturnPath() != null)
            cxt.putInValue(exertion.getContext().getReturnPath().path,
                    exertion.getContext().getReturnValue());
		else
             cxt.updateEntries(exertion.getContext());

        if (! (exertion instanceof Block))
		    ((ServiceContext)exertion.getContext()).setScope(null);
//		if (cxt.getReturnPath() != null)
//			cxt.putValue(cxt.getReturnPath().path, cxt.getReturnValue());
	}

    protected List<Mogram> getInputExertions() {
        return xrt.getMograms();
	}

}
