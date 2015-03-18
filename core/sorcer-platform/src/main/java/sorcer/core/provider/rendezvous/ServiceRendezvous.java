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
import sorcer.core.context.ControlContext;
import sorcer.core.provider.Concatenator;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.Rendezvous;
import sorcer.core.provider.Spacer;
import sorcer.service.*;
import sorcer.service.Strategy.Access;

import java.rmi.RemoteException;
import java.util.logging.Logger;

/**
 * ServiceJobber - The SORCER rendezvous service provider that provides
 * coordination for executing exertions using directly (PUSH) service providers.
 * 
 * @author Mike Sobolewski
 */
public class ServiceRendezvous extends RendezvousBean implements Rendezvous, Spacer, Jobber, Concatenator {
	private Logger logger = Logger.getLogger(ServiceRendezvous.class.getName());

	private boolean isConfigured = false;
	
	public ServiceRendezvous() throws RemoteException {
	}
	
	public Mogram execute(Mogram mogram, Transaction txn)
			throws TransactionException, ExertionException, RemoteException {
		Exertion exertion = (Exertion) mogram;
		if (!isConfigured)
			try {
				configure();
			} catch (ConfigurationException ex) {
				throw new ExertionException(ex);
			}
		
		logger.info("*********************************************ServiceRendezvous.execute, exertion = " + exertion);
		if (exertion.isTask()) {
			ServiceSpacer spacer = (ServiceSpacer) delegate
					.getBean(Spacer.class);
			return spacer.execute(exertion, txn);
		} else if (exertion instanceof Job) {
			if (((ControlContext) exertion.getControlContext()).getAccessType() == Access.PUSH) {
				ServiceJobber jobber = (ServiceJobber) delegate
						.getBean(Jobber.class);
				return jobber.execute(exertion, txn);
			} else if (((ControlContext) exertion.getControlContext())
					.getAccessType() == Access.PULL) {
				ServiceSpacer spacer = (ServiceSpacer) delegate
						.getBean(Spacer.class);
				return spacer.execute(exertion, txn);
			}
		} else if (exertion instanceof Block) {
			ServiceConcatenator concatenator = (ServiceConcatenator) delegate
					.getBean(Concatenator.class);
			return concatenator.execute(exertion, txn);
		}
		throw new ExertionException("now rendevous service available for exertion of this type: " + exertion.getClass());
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
