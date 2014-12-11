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

import sorcer.core.provider.Provider;
import sorcer.service.Context;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.SignatureException;
import sorcer.service.Task;

public class CatalogSingletonDispatcher extends CatalogExertDispatcher {

	public CatalogSingletonDispatcher(Job job, 
            Set<Context> sharedContexts,
            boolean isSpawned, 
            Provider provider,
            ProvisionManager provisionManager) throws Throwable {
		super(job, sharedContexts, isSpawned, provider, provisionManager);
	}


	public void dispatchExertions() throws SignatureException,
			ExertionException {
        checkAndDispatchExertions();
		// boolean isPersisted = (job.getStatus() != INITIAL)?false:true;
		xrt.setStatus(RUNNING);
		collectResults();
	}

	public void collectResults() throws ExertionException, SignatureException {
		Task result = null;
		Task exertion = (Task) ((Job) xrt).get(0);
		exertion.startExecTime();
		// Provider is expecting exertion field in Context to be set.
		try {
			exertion.getContext().setExertion(exertion);
			result = execTask(exertion);
			if (result.getStatus() <= FAILED) {
				xrt.setStatus(FAILED);
				state = FAILED;
				ExertionException fe = null;
				fe = new ExertionException(provider.getProviderName()
						+ " received failed task", result);
				result.reportException(fe);
				dispatchers.remove(xrt.getId());
				throw fe;
			} else {
				((Job) xrt).setExertionAt(result, 0);
				state = DONE;
				notifyExertionExecution(exertion, result);
				xrt.setStatus(DONE);
				dispatchers.remove(xrt.getId());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ExertionException(e);
		} finally {
			dThread.stop = true;
		}
	}

}
