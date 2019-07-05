/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.DispatchResult;
import sorcer.core.Dispatcher;
import sorcer.service.Exerter;
import sorcer.service.*;

import java.rmi.RemoteException;

public class MogramThread implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(MogramThread.class
			.getName());

	private static final int SLEEP_TIME = 250;
	// doMogram method calls internally
	private Mogram job;

	private Mogram result;

	Exerter provider;

    private DispatcherFactory dispatcherFactory;

	public MogramThread(Mogram job, Exerter provider, DispatcherFactory dispatcherFactory) {
		this.job = job;
		this.provider = provider;
        this.dispatcherFactory = dispatcherFactory;
	}

	public void run() {
		logger.debug("*** Subroutine explorer started with control context ***\n"
				+ ((Subroutine)job).getControlContext());
		try {
            Dispatcher dispatcher = null;
            if (job instanceof Job)
                dispatcher = dispatcherFactory.createDispatcher((Job)job, provider);
            else
                dispatcher = dispatcherFactory.createDispatcher((Task)job, provider);
			try {
				((Subroutine)job).getControlContext().appendTrace((
						provider.getProviderName() != null ? provider.getProviderName() + " " : "") +
						"run: " + job.getName() + " explorer: " + dispatcher.getClass().getName());
			} catch (RemoteException e) {
                logger.error("exception in explorer: " + e);
				// ignore it, locall prc
			}
/*			 int COUNT = 1000;
			 int count = COUNT;
			while (explorer.getState() != Exec.DONE
					&& explorer.getState() != Exec.FAILED
					&& explorer.getState() != Exec.SUSPENDED) {
				 count--;
				 if (count < 0) {
				 logger.debug("*** Mogramber's Subroutine Dispatcher waiting in state: "
				 + explorer.getState());
				 count = COUNT;
				 }
				Thread.sleep(SLEEP_TIME);
			}*/
            dispatcher.exec();
            DispatchResult dispatchResult = dispatcher.getResult();
            logger.debug("*** Dispatcher exit state = " + dispatcher.getClass().getName()  + " state: " + dispatchResult.state
                    + " for job***\n" + ((Subroutine)job).getControlContext());
            result = (Mogram) dispatchResult.exertion;
		} catch (DispatcherException de) {
			de.printStackTrace();
		}
	}

	public Mogram getMogram() {
		return job;
	}

	public Mogram getResult() throws ContextException {
		return result;
	}
}
