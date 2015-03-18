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
import sorcer.core.DispatchResult;
import sorcer.core.dispatch.DispatcherException;
import sorcer.core.dispatch.DispatcherFactory;
import sorcer.core.dispatch.ExertionDispatcherFactory;
import sorcer.core.dispatch.SpaceTaskDispatcher;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.core.provider.Provider;
import sorcer.core.provider.Spacer;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.HashSet;

import static sorcer.util.StringUtils.tName;

/**
 * ServiceSpacer - The SORCER rendezvous service provider that provides
 * coordination for executing exertions using JavaSpace from which provides PULL
 * exertions to be executed.
 * 
 * @author Mike Sobolewski
 */
public class ServiceSpacer extends ServiceJobber implements Spacer, Executor {
    private Logger logger = LoggerFactory.getLogger(ServiceSpacer.class.getName());

    private LokiMemberUtil myMemberUtil;

    /**
     * ServiceSpacer - Default constructor
     *
     * @throws RemoteException
     */
    public ServiceSpacer() throws RemoteException {
        myMemberUtil = new LokiMemberUtil(ServiceSpacer.class.getName());
    }

    public Exertion execute(Exertion exertion, Transaction txn)
            throws TransactionException, RemoteException, ExertionException {
        if (exertion.isJob())
            return (Exertion)super.execute(exertion, txn);
        else
            return doTask(exertion);
    }

    protected class TaskThread extends Thread {

        // doJob method calls this internally
        private Task task;

        private Task result;

        private Provider provider;

        public TaskThread(Task task, Provider provider) {
            super(tName("Task-" + task.getName()));
            this.task = task;
            this.provider = provider;
        }

        public void run() {
            logger.trace("*** TaskThread Started ***");
            try {
                SpaceTaskDispatcher dispatcher = getDispatcherFactory(task).createDispatcher(task, provider);
                try {
                    task.getControlContext().appendTrace(provider.getProviderName() + " dispatcher: "
                            + dispatcher.getClass().getName());
                } catch (RemoteException e) {
                    //ignore it, local call
                }
                dispatcher.exec();
                DispatchResult dispatchResult = dispatcher.getResult();
                logger.debug("Dispatcher State: " + dispatchResult.state);
				result = (NetTask) dispatchResult.exertion;
            } catch (DispatcherException e) {
                logger.warn("Error while executing space task {}", task.getName(), e);
                task.reportException(e);
            }
        }

        public Task getResult() throws ContextException {
            return result;
        }
    }

    public Exertion doTask(Exertion task) throws RemoteException {
        setServiceID(task);
        try {
            if (task.isMonitorable()
                    && !task.isWaitable()) {
                replaceNullExertionIDs(task);
                notifyViaEmail(task);
                new TaskThread((Task) task, provider).start();
                return task;
            } else {
                TaskThread taskThread = new TaskThread((Task) task, provider);
                taskThread.start();
                taskThread.join();
                Task result = taskThread.getResult();
                logger.trace("Spacer result: " + result);
                return result;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

	@Override
    protected DispatcherFactory getDispatcherFactory(Exertion exertion) {
        if (exertion.isSpacable())
            return ExertionDispatcherFactory.getFactory(myMemberUtil);
        else
            return super.getDispatcherFactory(exertion);
    }
}
