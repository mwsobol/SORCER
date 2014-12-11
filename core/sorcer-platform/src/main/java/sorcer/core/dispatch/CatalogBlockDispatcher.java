/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

import java.rmi.RemoteException;
import java.util.Set;

import sorcer.core.SorcerConstants;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.exertion.AltExertion;
import sorcer.core.exertion.LoopExertion;
import sorcer.core.exertion.OptExertion;
import sorcer.core.provider.Provider;
import sorcer.service.Block;
import sorcer.service.Condition;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;
import sorcer.service.Task;

/**
 * A dispatching class for exertion blocks in the PUSH mode.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"rawtypes", "unchecked" })

public class CatalogBlockDispatcher extends CatalogExertDispatcher implements
		SorcerConstants {

	public CatalogBlockDispatcher(Block block, Set<Context> sharedContext,
			boolean isSpawned, Provider provider,
			ProvisionManager provisionManager) throws Throwable {
		super(block, sharedContext, isSpawned, provider, provisionManager);
	}
	
	public void dispatchExertions() throws ExertionException,
			SignatureException {
        checkAndDispatchExertions();
		inputXrts = xrt.getExertions();
		reset();
		// reconcileInputExertions(xrt);
		collectResults();
		
		try {
			Condition.cleanupScripts(xrt);
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
	}

	public void collectResults() throws ExertionException, SignatureException {
		try {
			String pn = null;
			if (inputXrts == null || inputXrts.size() == 0) {
				xrt.setStatus(FAILED);
				state = FAILED;
				try {
					pn = provider.getProviderName();
					if (pn == null)
						pn = provider.getClass().getName();
					ExertionException fe = new ExertionException(pn + " received invalid job: "
							+ xrt.getName(), xrt);

					xrt.reportException(fe);
					dispatchers.remove(xrt.getId());
					throw fe;
				} catch (RemoteException e) {
					// ignore it, local call
				}
			}

			ServiceExertion se = null;
			xrt.startExecTime();
			for (int i = 0; i < inputXrts.size(); i++) {
				se = (ServiceExertion) inputXrts.get(i);
				try {
					((ServiceContext)se.getContext()).setBlockScope(xrt.getContext());
				} catch (ContextException ce) {
					throw new ExertionException(ce);
				}
				// Provider is expecting exertion to be in context
				try {
					se.getContext().setExertion(se);
				
				// support for continuous pre and post execution of task
				// signatures
				if (i > 0 && se.isTask() && ((Task) se).isContinous())
					se.setContext(inputXrts.get(i - 1).getContext());
				} catch (ContextException ex) {
					throw new ExertionException(ex);
				}
				if (isInterupted(se)) {
					se.stopExecTime();
					dispatchers.remove(xrt.getId());
					return;
				}

				try {
					preUpdate(se);
					se = (ServiceExertion) execExertion(se);
				} catch (ContextException ce) {
					throw new ExertionException(ce);
				}
				
				if (se.getStatus() <= FAILED) {
					xrt.setStatus(FAILED);
					state = FAILED;
					try {
						pn = provider.getProviderName();
						if (pn == null)
							pn = provider.getClass().getName();
						ExertionException fe = new ExertionException(pn
								+ " received failed task: " + se.getName(), se);
						xrt.reportException(fe);
						dispatchers.remove(xrt.getId());
						throw fe;
					} catch (RemoteException e) {
						// ignore it, local call
					}
				} else if (se.getStatus() == SUSPENDED
						|| xrt.getControlContext().isReview(se)) {
					xrt.setStatus(SUSPENDED);
					ExertionException ex = new ExertionException(
							"exertion suspended", se);
					se.reportException(ex);
					dispatchers.remove(xrt.getId());
					throw ex;
				}
				try {
					postUpdate(se);
				} catch (Exception e) {
					throw new ExertionException(e);
				}
			}

			state = DONE;
			dispatchers.remove(xrt.getId());
			xrt.stopExecTime();
			xrt.setStatus(DONE);
		} finally {
			dThread.stop = true;
		}
	}

	private void preUpdate(ServiceExertion exertion) throws ContextException {
		if (exertion instanceof AltExertion) {
			for (OptExertion oe : ((AltExertion)exertion).getOptExertions()) {
				oe.getCondition().getConditionalContext().append(xrt.getContext());
				((OptExertion)oe).getCondition().setStatus(null);
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
			Context pc = ((LoopExertion)exertion).getCondition().getConditionalContext();
			((Condition)((LoopExertion)exertion).getCondition()).setStatus(null);
			if (pc == null) {
				pc = new ParModel(exertion.getName());
				((LoopExertion)exertion).getCondition().setConditionalContext(pc);
			}
			pc.append(xrt.getContext());
		}
	}
	
	private void postUpdate(ServiceExertion exertion) throws ContextException, RemoteException {
		if (exertion instanceof AltExertion) {
			((Block)xrt).getContext().append(((AltExertion)exertion).getActiveOptExertion().getContext());
		} else if (exertion instanceof OptExertion) {
			((Block)xrt).getContext().append(((OptExertion)exertion).getContext());
		}
		
//		if (exertion instanceof AltExertion) {
//			((ParModel)((Block)xrt).getContext()).appendNew(((AltExertion)exertion).getActiveOptExertion().getContext());
//		} else if (exertion instanceof OptExertion) {
//			((ParModel)((Block)xrt).getContext()).appendNew(((OptExertion)exertion).getContext());
//		}
		
		ServiceContext cxt = (ServiceContext)xrt.getContext();
		if (((ServiceContext)exertion.getContext()).getReturnPath() != null)
			cxt.putOutValue(((ServiceContext)exertion.getContext()).getReturnPath().path, 
					exertion.getContext().getReturnValue()); 
		else 
			cxt.appendNewEntries(exertion.getContext());
		
		((ServiceContext)exertion.getContext()).setBlockScope(null);
	}
	
	private void reset() {
		xrt.setStatus(INITIAL);
		for (Exertion e : ((Block)xrt).getExertions())
			((ServiceExertion)e).reset(INITIAL);
	}
}
