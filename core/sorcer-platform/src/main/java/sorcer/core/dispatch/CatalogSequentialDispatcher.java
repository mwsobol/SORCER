/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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
import sorcer.core.exertion.Jobs;
import sorcer.core.provider.Provider;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;
import sorcer.service.Task;

public class CatalogSequentialDispatcher extends CatalogExertDispatcher
		implements SorcerConstants {

	@SuppressWarnings("rawtypes")
	public CatalogSequentialDispatcher(Job job, 
            Set<Context> sharedContext,
            boolean isSpawned, 
            Provider provider,
            ProvisionManager provisionManager) throws Throwable {
		super(job, sharedContext, isSpawned, provider, provisionManager);
	}

	public void dispatchExertions() throws ExertionException,
			SignatureException {
        checkAndDispatchExertions();
		try {
			inputXrts = Jobs.getInputExertions(((Job)xrt));
			reconcileInputExertions(xrt);
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
		collectResults();
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
				} catch (RemoteException e) {
					// ignore it, local call
				}
				ExertionException fe = new ExertionException(pn
						+ " received a job with no component exertions or alreday executed: "  
						+ xrt.getName(), xrt);
				xrt.reportException(fe);
				dispatchers.remove(xrt.getId());
				throw fe;
			}

			ServiceExertion se = null;
			xrt.startExecTime();
			for (int i = 0; i < inputXrts.size(); i++) {
				se = (ServiceExertion) inputXrts.get(i);
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
				se = (ServiceExertion) execExertion(se);
				if (se.getStatus() <= FAILED) {
					xrt.setStatus(FAILED);
					state = FAILED;
					try {
						pn = provider.getProviderName();
						if (pn == null) 
							pn = provider.getClass().getName();
					} catch (RemoteException e) {
						// ignore it, local call
					}
					ExertionException fe = new ExertionException(pn
							+ " received failed task: " + se.getName(), se);
					xrt.reportException(fe);
					dispatchers.remove(xrt.getId());
					throw fe;
				} else if (se.getStatus() == SUSPENDED
						|| xrt.getControlContext().isReview(se)) {
					xrt.setStatus(SUSPENDED);
					ExertionException ex = new ExertionException(
							"exertion suspended", se);
					se.reportException(ex);
					dispatchers.remove(xrt.getId());
					throw ex;
				}
			}
			if (isInterupted(masterXrt)) {
				masterXrt.stopExecTime();
				dispatchers.remove(xrt.getId());
				return;
			}

			if (masterXrt != null) {
				masterXrt = (ServiceExertion) execExertion(masterXrt); // executeMasterExertion();
				if (masterXrt.getStatus() <= FAILED) {
					state = FAILED;
					xrt.setStatus(FAILED);
				} else {
					state = DONE;
					xrt.setStatus(DONE);
				}
			} else
				state = DONE;
			dispatchers.remove(xrt.getId());
			xrt.stopExecTime();
			xrt.setStatus(DONE);
		} finally {
			dThread.stop = true;
		}
	}

}
