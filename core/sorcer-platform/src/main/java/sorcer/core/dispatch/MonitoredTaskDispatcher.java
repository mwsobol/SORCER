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

import java.util.Set;

import sorcer.core.SorcerConstants;
import sorcer.core.exertion.NetTask;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Task;

public class MonitoredTaskDispatcher extends MonitoredExertDispatcher
		implements SorcerConstants {

	/**
	 * @param exertion
	 * @param sharedContext
	 * @param isSpawned
	 * @param provider
	 * @throws Throwable
	 */
	public MonitoredTaskDispatcher(Exertion exertion,
			Set<Context> sharedContext, boolean isSpawned, Provider provider)
			throws Throwable {
		super(exertion, sharedContext, isSpawned, provider);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.core.dispatch.ExertionDispatcher#dispatchExertions()
	 */
	@Override
	public void dispatchExertions() throws ExertionException {
        checkAndDispatchExertions();
		preExecExertion(xrt);
		collectResults();
	}

	/* (non-Javadoc)
	 * @see sorcer.core.dispatch.ExertionDispatcher#collectResults()
	 */
	@Override
	public void collectResults() throws ExertionException {
		NetTask result = null;
		try {
//			logger.finer("\n*** getting result... ***\n");
			result = (NetTask) ((ServiceProvider) provider).getDelegate()
					.doTask((Task) xrt, null);
			result.getControlContext().appendTrace(provider.getProviderName() 
					+ " dispatcher: " + getClass().getName());
//			logger.finer("\n*** got result: ***\n" + result);

			if (result.getStatus() <= FAILED) {
				xrt.setStatus(FAILED);
				state = FAILED;
				xrt.getMonitorSession().changed(result.getContext(),
						State.FAILED);
				ExertionException fe = new ExertionException(this.getClass()
						.getName() + " received failed task", result);
				result.reportException(fe);
				throw fe;
			} else {
				notifyExertionExecution(xrt, result);
				state = DONE;
				xrt.setStatus(DONE);
				xrt.getMonitorSession().changed(result.getContext(),
						State.DONE);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ExertionException ne = new ExertionException(e);
			result.reportException(ne);
			throw ne;
		}
		postExecExertion(xrt);
	}

}
