/*
 * Copyright 2015 the original author or authors.
 * Copyright 2015 SorcerSoft.org.
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
import sorcer.core.context.model.srv.SrvModel;
import sorcer.core.provider.Modeler;
import sorcer.service.Arg;
import sorcer.service.ExertionException;
import sorcer.service.Mogram;
import sorcer.service.Task;
import sorcer.service.modeling.Model;

import java.rmi.RemoteException;

import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.task;

/**
 * ServiceModler - The SORCER rendezvous service provider that manages
 * coordination for executing service models using its service signatures
 * to create a dynamic federation of collaborating providers.
 *
 * @author Mike Sobolewski
 */
public class ServiceModeler extends RendezvousBean implements Modeler {
    private Logger logger = LoggerFactory.getLogger(ServiceModeler.class.getName());

    public ServiceModeler() throws RemoteException {
        // do nothing
    }

    public Mogram localExert(Mogram mogram, Transaction txn, Arg... args)
            throws TransactionException, ExertionException, RemoteException {
        //logger.info("*********************************************ServiceModeler.exert, model = " + mogram);
        setServiceID(mogram);
        SrvModel model = (SrvModel) mogram;
        Model result = null;
        try {
            if (model.getSubjectValue() instanceof Class) {
                Task task = task(model.getName(), sig(model.getSubjectPath(), model.getSubjectValue()), model);
                Task out = task.exert();
                logger.trace("<==== Result: " + out);
                result = out.getDataContext();
            } else {
                Task task = task(model.getName(), model);
                Task out = task.exert();
                logger.trace("<==== Result: " + out);
                result = out.getDataContext();
            }
        } catch (Throwable e) {
            throw new ExertionException(e);
        }
        //logger.info("*********************************************ServiceModeler.exert(), model = " + result);
        return result;
    }

}
