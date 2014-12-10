/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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
import sorcer.core.exertion.NetTask;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.SignatureException;
import sorcer.util.ProviderAccessor;

public class SpaceTaskDispatcher extends SpaceExertDispatcher {

	public SpaceTaskDispatcher(final NetTask task, 
            final Set<Context> sharedContexts,
            final boolean isSpawned, 
            final LokiMemberUtil myMemberUtil,
            final ProvisionManager provisionManager) throws ExertionException, SignatureException {

		this.xrt = task;
		subject = task.getSubject();
		this.sharedContexts = sharedContexts;
		this.isSpawned = isSpawned;
		isMonitored = task.isMonitorable();
		state = RUNNING;
		dispatchers.put(task.getId(), this);
		this.provisionManager = provisionManager;
		dispatchExertions();
		
		disatchGroup = new ThreadGroup("task-"+ task.getId());
		disatchGroup.setDaemon(true);
		disatchGroup.setMaxPriority(Thread.NORM_PRIORITY - 1);

		CollectResultThread crThread = new CollectResultThread(disatchGroup);
		crThread.start();

		CollectFailThread cfThread = new CollectFailThread(disatchGroup);
		cfThread.start();

		CollectErrorThread efThread = new CollectErrorThread(disatchGroup);
		efThread.start();

		this.myMemberUtil = myMemberUtil;
	}

	public void dispatchExertions() throws ExertionException,
			SignatureException {
		checkAndDispatchExertions();
		try {
			reconcileInputExertions(xrt);
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
		logger.finer("space task: " + xrt);
		try {
			writeEnvelop(xrt);
			logger.finer("written task ==> SPACE EXECUTE TASK: "
					+ xrt.getName());
		} catch (RemoteException re) {
			re.printStackTrace();
			logger.severe("Space not reachable... resetting space");
			space = ProviderAccessor.getSpace();
			if (space == null) {
				xrt.setStatus(FAILED);
				throw new ExertionException("NO exertion space available!");
			}
		}
	}
	
	public void collectResults() throws ExertionException, SignatureException {
		ExertionEnvelop temp;
		temp = ExertionEnvelop.getTemplate();
		temp.exertionID = xrt.getId();
		temp.state = new Integer(DONE);

		logger.finer("<===================== template for space task to be collected: \n"
				+ temp.describe());

		ExertionEnvelop resultEnvelop = takeEnvelop(temp);
		
		logger.info("\n\n\n\n\n\n**********************************************SpaceTaskDispatcher.collectResults(): resultEnvelop = " + resultEnvelop);

		if (resultEnvelop != null) {
			logger.finer("collected result envelope  <===================== \n"
					+ resultEnvelop.describe());
			
			NetTask result = (NetTask) resultEnvelop.exertion;
			state = DONE;
			try {
				notifyExertionExecution(xrt, xrt, result);
			} catch (ContextException e) {
				throw new ExertionException(e);
			}
			result.setStatus(DONE);
			xrt = result;
		}
		dispatchers.remove(xrt.getId());
	}
	
	public void collectFails() throws ExertionException {
		ExertionEnvelop template;
		template = ExertionEnvelop.getTemplate();
		template.exertionID = xrt.getId();
		template.state = new Integer(FAILED);

		logger.finer("<===================== template for failed task to be collected: \n"
				+ template.describe());

		ExertionEnvelop resultEnvelop = takeEnvelop(template);
		if (resultEnvelop != null) {
			NetTask result = (NetTask) resultEnvelop.exertion;
			state = FAILED;
			try {
				notifyExertionExecution(xrt, xrt, result);
			} catch (ContextException e) {
				throw new ExertionException(e);
			}
			result.setStatus(FAILED);
			xrt = result;
		}
		dispatchers.remove(xrt.getId());
	}
	
	public void collectErrors() throws ExertionException {
		ExertionEnvelop template;
		template = ExertionEnvelop.getTemplate();
		template.exertionID = xrt.getId();
		template.state = new Integer(ERROR);

		logger.finer("<===================== template for error task to be collected: \n"
				+ template.describe());

		ExertionEnvelop resultEnvelop = takeEnvelop(template);
		if (resultEnvelop != null) {
			NetTask result = (NetTask) resultEnvelop.exertion;
			state = ERROR;
			try {
				notifyExertionExecution(xrt, xrt, result);
			} catch (ContextException e) {
				throw new ExertionException(e);
			}
			result.setStatus(ERROR);
			xrt = result;
		}
		dispatchers.remove(xrt.getId());
	}

}
