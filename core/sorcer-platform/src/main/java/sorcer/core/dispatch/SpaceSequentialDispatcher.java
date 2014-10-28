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

import sorcer.core.exertion.ExertionEnvelop;
import sorcer.core.exertion.NetJob;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.core.provider.Provider;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;
import sorcer.util.ProviderAccessor;

public class SpaceSequentialDispatcher extends SpaceExertDispatcher {

	public SpaceSequentialDispatcher(Job job, 
            Set<Context> sharedContexts,
            boolean isSpawned, 
            LokiMemberUtil myMemberUtil, 
            Provider provider,
            ProvisionManager provisionManager) throws ExertionException, ContextException  {
		super(job, sharedContexts, isSpawned, myMemberUtil, provider, provisionManager);
	}

	public void dispatchExertions() throws ExertionException,
			SignatureException {
        checkAndDispatchExertions();
		try {
			reconcileInputExertions(xrt);
		} catch (ContextException ex) {
			throw new ExertionException(ex);
		}

		logger.finer("exertion count: " + inputXrts.size());
		for (int i = 0; i < inputXrts.size(); i++) {
			ServiceExertion exertion = (ServiceExertion) inputXrts.get(i);
			logger.finer("exertion #: " + i + ", exertion:\n" + exertion);
			try {
				writeEnvelop(exertion);
				logger.finer("generateTasks ==> SPACE SEQUENIAL EXECUTE EXERTION: "
								+ exertion.getName());
			} catch (RemoteException re) {
				re.printStackTrace();
				logger.severe("Space not reachable....resetting space");
				space = ProviderAccessor.getSpace();
				if (space == null) {
					xrt.setStatus(FAILED);
					throw new ExertionException("NO exertion space available!");
				}
				if (masterXrt != null) {
					try {
						writeEnvelop(masterXrt);
					} catch (Exception e) {
						e.printStackTrace();
						xrt.setStatus(FAILED);
						throw new ExertionException(
								"Wrting master exertion into exertion space failed!",
								e);
					}
				}
			}
			logger.finer("waiting for exertion " + i + ", id="
					+ exertion.getId() + ".............");
			waitForExertion(exertion.getIndex());
		}
		dThread.stop = true;
	}

	public void collectResults() throws ExertionException, SignatureException {
		int count = 0;
		ExertionEnvelop temp;
		temp = ExertionEnvelop.getTemplate();
		temp.parentID = xrt.getId();
		temp.state = new Integer(DONE);

		logger.finer("<===================== collect exertions for template: \n"
						+ temp.describe());
		while (count < inputXrts.size() && state != FAILED) {
			ExertionEnvelop resultEnvelop = (ExertionEnvelop) takeEnvelop(temp);
			logger.finer("collected result envelope  <===================== \n"
					+ resultEnvelop.describe());

			if (resultEnvelop != null && resultEnvelop.exertion != null) {
				ServiceExertion input = (ServiceExertion) ((NetJob)xrt)
						.get(((ServiceExertion) resultEnvelop.exertion)
								.getIndex());
				ServiceExertion result = (ServiceExertion) resultEnvelop.exertion;
				postExecExertion(input, result);
				count++;
			}
		}
		executeMasterExertion();
		dispatchers.remove(xrt.getId());
		state = DONE;
	}
}
