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
import sorcer.core.exertion.AltMogram;
import sorcer.core.exertion.LoopMogram;
import sorcer.core.exertion.OptMogram;
import sorcer.core.monitor.MonitorUtil;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.provider.Provider;
import sorcer.service.*;
import sorcer.service.modeling.Model;

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
        block.getDataContext().append(block.getScope());
    }


    @Override
    protected void doExec() throws MogramException, SignatureException {
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
                if (exertion.getDataContext().getScope() != null)
                    exertion.getDataContext().getScope().append(xrt.getDataContext());
                else {
//                  exertion.getDataContext().setScope(xrt.getContext());
                    exertion.getDataContext().setScope(new ParModel());
                    exertion.getDataContext().getScope().append(xrt.getContext());
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
            Condition.cleanupScripts(result);
            if (result instanceof ConditionalMogram) {
                Mogram target = ((ConditionalMogram)result).getTarget();
                if (target instanceof Model) {
                    xrt.getContext().append((Context)((Model)target).getResult());
                }
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
        } catch (Exception e) {
            throw new ExertionException(e);
        /*} catch (MonitorException e) {
            throw new ExertionException(e);*/
        }
    }

    private void preUpdate(Exertion exertion) throws ContextException, RemoteException {
		if (exertion instanceof AltMogram) {
			for (OptMogram oe : ((AltMogram)exertion).getOptExertions()) {
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
		} else if (exertion instanceof OptMogram) {
			Context pc = ((OptMogram)exertion).getCondition().getConditionalContext();
			((OptMogram)exertion).getCondition().setStatus(null);
			if (pc == null) {
				pc = new ParModel(exertion.getName());
				((OptMogram)exertion).getCondition().setConditionalContext(pc);
            }
            pc.append(xrt.getContext());
		} else if (exertion instanceof LoopMogram && ((LoopMogram)exertion).getCondition() != null) {
            ((LoopMogram)exertion).getCondition().setStatus(null);
			Context pc = ((LoopMogram)exertion).getCondition().getConditionalContext();
			if (pc == null) {
				pc = new ParModel(exertion.getName());
				((LoopMogram)exertion).getCondition().setConditionalContext(pc);
			}
			pc.append(xrt.getContext());
		} else {
            exertion.getDataContext().updateEntries(xrt.getContext());
        }
	}
	
	private void postUpdate(Exertion exertion) throws ContextException, RemoteException {
        if (exertion instanceof Job) {
            xrt.getDataContext().append(exertion.getDataContext());
        } else if (exertion instanceof AltMogram) {
			xrt.getContext().append(((AltMogram)exertion).getActiveOptExertion().getDataContext());
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
		} else if (exertion instanceof OptMogram) {
			xrt.getContext().append(exertion.getDataContext());
		}

//		if (exertion instanceof AltExertion) {
//			((ParModel)((Block)xrt).getContext()).appendNew(((AltExertion)exertion).getActiveOptExertion().getContext());
//		} else if (exertion instanceof OptExertion) {
//			((ParModel)((Block)xrt).getContext()).appendNew(((OptExertion)exertion).getContext());
//		}
		
		ServiceContext cxt = (ServiceContext)xrt.getDataContext();
		if (exertion.getDataContext().getReturnPath() != null)
            cxt.putValue(exertion.getContext().getReturnPath().path,
                    exertion.getDataContext().getReturnValue());
		else
             cxt.updateEntries(exertion.getDataContext());

        if (! (exertion instanceof Block))
		    ((ServiceContext)exertion.getDataContext()).setScope(null);
//		if (cxt.getReturnPath() != null)
//			cxt.putValue(cxt.getReturnPath().path, cxt.getReturnValue());
	}

    protected List<Mogram> getInputExertions() {
        return xrt.getMograms();
	}

}
