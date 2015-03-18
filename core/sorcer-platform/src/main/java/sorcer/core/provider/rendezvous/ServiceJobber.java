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

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.core.dispatch.DispatcherFactory;
import sorcer.core.dispatch.ExertionDispatcherFactory;
import sorcer.core.dispatch.JobThread;
import sorcer.core.provider.Jobber;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.Mogram;

import java.rmi.RemoteException;
import java.util.logging.Logger;

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

	public Mogram execute(Mogram mogram, Transaction txn)
			throws TransactionException, ExertionException, RemoteException {

            setServiceID(mogram);
            try {
                JobThread mogramThread = new JobThread((Job)mogram, provider, getDispatcherFactory((Exertion)mogram));
                if (((Exertion)mogram).getControlContext().isMonitorable()
                        && !((Exertion)mogram).getControlContext().isWaitable()) {
                    replaceNullExertionIDs((Exertion)mogram);
                    notifyViaEmail((Exertion)mogram);
                    new Thread(mogramThread, ((Job)mogram).getContextName()).start();
                    return mogram;
                } else {
                    mogramThread.run();
                    Job result = mogramThread.getResult();
                    logger.fine("<== Result: " + result);
                    return result;
                }
            } catch (Exception e) {
                ((Job)mogram).reportException(e);
                logger.warning("Error: " + e.getMessage());
                return mogram;
            }
	}

    protected DispatcherFactory getDispatcherFactory(Exertion exertion) {
        return ExertionDispatcherFactory.getFactory();
    }

}
