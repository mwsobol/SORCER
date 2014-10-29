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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import sorcer.core.exertion.Jobs;
import sorcer.core.provider.Provider;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;

public class CatalogParallelDispatcher extends CatalogExertDispatcher {
	List<ExertionThread> workers;

	public CatalogParallelDispatcher(Job job, 
            Set<Context> sharedContexts,
            boolean isSpawned, 
            Provider provider,
            ProvisionManager provisionManager) throws Throwable {
		super(job, sharedContexts, isSpawned, provider, provisionManager);
	}

	public void dispatchExertions() throws ExertionException,
			SignatureException {
        checkAndDispatchExertions();
		workers = new ArrayList<ExertionThread>();
		try {
			inputXrts = Jobs.getInputExertions(((Job)xrt));
			reconcileInputExertions(xrt);
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
		for (int i = 0; i < inputXrts.size(); i++) {
			ServiceExertion exertion = (ServiceExertion) inputXrts.get(i);
			workers.add(runExertion(exertion));
		}
		collectResults();
		dThread.stop = true;
	}

	public void collectResults() throws ExertionException, SignatureException {
		boolean isFailed = false;
		boolean isSuspended = false;
		Exertion result = null;
		while (workers.size() > 0) {
			for (int i = 0; i < workers.size(); i++) {
				result = workers.get(i).getResult();
				if (result != null) {
					ServiceExertion se = (ServiceExertion) result;
					se.stopExecTime();
					if (se.getStatus() == FAILED)
						isFailed = true;
					else if (se.getStatus() == SUSPENDED)
						isSuspended = true;
					workers.remove(i);
				}
			}
		}

		if (isFailed) {
			xrt.setStatus(FAILED);
			state = FAILED;
			ExertionException fe = new ExertionException(this.getClass().getName() 
					+ " failed job", xrt);
			xrt.reportException(fe);
			dispatchers.remove(xrt.getId());
			throw fe;
		}
		else if (isSuspended) {
			xrt.setStatus(SUSPENDED);
			state = SUSPENDED;
			ExertionException fe = new ExertionException(this.getClass().getName() 
					+ " suspended job", xrt);
			xrt.reportException(fe);
			dispatchers.remove(xrt.getId());
			throw fe;
		}
		
		if (masterXrt != null) {
			if (isInterupted(masterXrt)) {
				masterXrt.stopExecTime();
				dispatchers.remove(xrt.getId());
				return;
			}
			// finally execute Master Exertion
			masterXrt = (ServiceExertion) execExertion(masterXrt);
			masterXrt.stopExecTime();
			if (masterXrt.getStatus() <= FAILED)
				xrt.setStatus(FAILED);
			else
				xrt.setStatus(DONE);
		}
		xrt.setStatus(DONE);
		dispatchers.remove(xrt.getId());
		state = DONE;
	}
}
