/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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
package sorcer.core.provider.rendezvous;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.core.dispatch.JobThread;
import sorcer.core.exertion.NetJob;
import sorcer.core.provider.Jobber;
import sorcer.service.Executor;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;

/**
 * ServiceJobber - The SORCER rendezvous service provider that provides
 * coordination for executing exertions using directly (PUSH) service providers.
 * 
 * @author Mike Sobolewski
 */
public class ServiceJobber extends RendezvousBean implements Jobber {
	private Logger logger = Logger.getLogger(ServiceJobber.class.getName());

	public ServiceJobber() throws RemoteException {
		// do nothing
	}
			
	public Exertion execute(Exertion exertion, Transaction txn)
			throws TransactionException, ExertionException, RemoteException {
		//logger.info("*********************************************ServiceJobber.exert(), exertion = " + exertion);
				setServiceID(exertion);
				Exertion result = null;
				try {
					if (((ServiceExertion)exertion).getControlContext().isMonitorable()
							&& !(((NetJob)exertion).getControlContext()).isWaitable()) {
						replaceNullExertionIDs(exertion);
						notifyViaEmail(exertion);
						new JobThread((Job) exertion, provider).start();
						return exertion;
					} else {
						JobThread jobThread = new JobThread((Job) exertion, provider);
						jobThread.start();
						jobThread.join();
						result = jobThread.getResult();
						logger.finest("<==== Result: " + result);
					}
				} catch (Throwable e) {
					throw new ExertionException(e);
				}
				//logger.info("*********************************************ServiceJobber.exert(), ex = " + ex);

		return result;
	}

}
