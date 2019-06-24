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

import net.jini.config.ConfigurationException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.core.provider.*;
import sorcer.service.*;
import sorcer.service.Strategy.Access;

import java.rmi.RemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceRendezvous - The SORCER rendezvous service provider that provides
 * coordination for all of SORCER service types: blocks, jobs, and modesls
 * 
 * @author Mike Sobolewski
 */
public class ServiceRendezvous extends SorcerExerterBean implements Rendezvous, Spacer, Jobber, Concatenator, Modeler {
	private Logger logger = LoggerFactory.getLogger(ServiceRendezvous.class.getName());

	private boolean isConfigured = false;
	
	public ServiceRendezvous() throws RemoteException {
	}
	
	public Mogram localExert(Mogram mogram, Transaction txn, Arg... args)
			throws TransactionException, RoutineException, RemoteException {
		Routine exertion = (Routine) mogram;
		if (!isConfigured)
			try {
				configure();
			} catch (ConfigurationException ex) {
				throw new RoutineException(ex);
			}
		
		logger.info("*********************************************ServiceRendezvous.exert, exertion = " + exertion);
		if (exertion.isTask()) {
			ServiceSpacer spacer = (ServiceSpacer) delegate
					.getBean(Spacer.class);
			return spacer.localExert(exertion, txn, args);
		} else if (exertion instanceof Job) {
			if ((exertion.getControlContext()).getAccessType() == Access.PUSH) {
				ServiceJobber jobber = (ServiceJobber) delegate
						.getBean(Jobber.class);
				return jobber.exert(exertion, txn, args);
			} else if ((exertion.getControlContext())
					.getAccessType() == Access.PULL) {
				ServiceSpacer spacer = (ServiceSpacer) delegate
						.getBean(Spacer.class);
				return spacer.localExert(exertion, txn, args);
			}
		} else if (exertion instanceof Block) {
			ServiceConcatenator concatenator = (ServiceConcatenator) delegate
					.getBean(Concatenator.class);
			return concatenator.exert(exertion, txn, args);
		}
		throw new RoutineException("now rendevous service available for exertion of this fiType: " + exertion.getClass());
	}

	@SuppressWarnings("unchecked")
	private void configure() throws RemoteException, ConfigurationException {
		delegate.getServiceComponents().put(Jobber.class, new ServiceJobber());
		delegate.getServiceComponents().put(Concatenator.class, new ServiceConcatenator());
		delegate.getServiceComponents().put(Spacer.class, new ServiceSpacer());
		//if (!delegate.spaceEnabled())
				//provider.initSpaceSupport();
		isConfigured = true;
	}
}
