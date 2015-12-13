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
import sorcer.core.dispatch.*;
import sorcer.core.exertion.NetTask;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.core.provider.Provider;
import sorcer.core.provider.Spacer;
import sorcer.service.*;

import java.rmi.RemoteException;

import static sorcer.util.StringUtils.tName;

/**
 * ServiceSpacer - The SORCER rendezvous service provider that provides
 * coordination for executing exertions using JavaSpace from which provides PULL
 * exertions to be executed.
 * 
 * @author Mike Sobolewski
 */
public class ServiceSpacer extends RendezvousBean implements Spacer {
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

    @Override
    public Mogram localExert(Mogram mogram, Transaction txn, Arg... args)
            throws TransactionException, ExertionException, RemoteException {
         if (mogram instanceof Exertion && ((Exertion)mogram).isCompound())
            return doCompound(mogram, txn);
        else
            return doTask((Task)mogram);
    }

    public Mogram doCompound(Mogram mogram, Transaction txn, Arg... args)
            throws TransactionException, ExertionException, RemoteException {
        setServiceID(mogram);
        try {
            MogramThread mogramThread = new MogramThread(mogram, provider, getDispatcherFactory((Exertion)mogram));
            if (((Exertion)mogram).getControlContext().isMonitorable()
                    && !((Exertion)mogram).getControlContext().isWaitable()) {
                replaceNullExertionIDs((Exertion)mogram);
                notifyViaEmail((Exertion)mogram);
                new Thread(mogramThread, ((Job)mogram).getContextName()).start();
                return mogram;
            } else {
                mogramThread.run();
                Mogram result = mogramThread.getResult();
                logger.debug("<== Result: " + result);
                return result;
            }
        } catch (Exception e) {
            ((ServiceExertion)mogram).reportException(e);
            logger.warn("Error: " + e.getMessage());
            return mogram;
        }
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
                    task.getControlContext().appendTrace("spacer: "
                            +(provider != null ? provider.getProviderName() + " " : "")
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

    protected DispatcherFactory getDispatcherFactory(Exertion exertion) {
        if (exertion.isSpacable())
            return MogramDispatcherFactory.getFactory(myMemberUtil);
        else
            return getDispatcherFactory(exertion);
    }
}
