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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.dispatch.DispatcherFactory;
import sorcer.core.dispatch.MogramDispatcherFactory;
import sorcer.core.dispatch.MogramThread;
import sorcer.core.provider.Jobber;
import sorcer.service.*;

import java.rmi.RemoteException;

/**
 * ServiceJobber - The SORCER rendezvous service provider that provides
 * coordination for executing mograms using directly (PUSH) service providers.
 * 
 * @author Mike Sobolewski
 */
public class ServiceJobber extends SystemServiceBean implements Jobber {
	private Logger logger = LoggerFactory.getLogger(ServiceJobber.class.getName());

	public ServiceJobber() throws RemoteException {
		// do nothing
	}

	public Mogram localExert(Mogram mogram, Transaction txn, Arg... args)
			throws TransactionException, ExertionException, RemoteException {

            setServiceID(mogram);
            try {
                MogramThread mogramThread = new MogramThread(mogram, provider, getDispatcherFactory((Routine)mogram));
                if (((Routine)mogram).getControlContext().isMonitorable()
                        && !((Routine)mogram).getControlContext().isWaitable()) {
                    replaceNullExertionIDs((Routine)mogram);
                    notifyViaEmail((Routine)mogram);
                    new Thread(mogramThread, ((Job)mogram).getContextName()).start();
                    return mogram;
                } else {
                    mogramThread.run();
                    Mogram result = mogramThread.getResult();
                    logger.debug("<== Result: " + result);
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
                mogram.reportException(e);
                logger.warn("Error: " + e.getMessage());
                return mogram;
            }
	}

    protected DispatcherFactory getDispatcherFactory(Routine exertion) {
        return MogramDispatcherFactory.getFactory();
    }

}
