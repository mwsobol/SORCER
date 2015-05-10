/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

import sorcer.core.context.model.ent.EntModel;
import sorcer.core.exertion.Mograms;
import sorcer.core.provider.Provider;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import static sorcer.service.Exec.*;

public class CatalogSequentialDispatcher extends CatalogExertDispatcher {

	@SuppressWarnings("rawtypes")
    public CatalogSequentialDispatcher(Exertion job,
                                       Set<Context> sharedContext,
                                       boolean isSpawned,
                                       Provider provider,
                                       ProvisionManager provisionManager) {
        super(job, sharedContext, isSpawned, provider, provisionManager);
    }

    protected void doExec() throws ExertionException,
            SignatureException {

        String pn;
        if (inputXrts == null) {
            xrt.setStatus(FAILED);
            state = FAILED;
            try {
                pn = provider.getProviderName();
                if (pn == null)
                    pn = provider.getClass().getName();
                ExertionException fe = new ExertionException(pn + " received invalid job: "
                        + xrt.getName(), xrt);

                xrt.reportException(fe);
                dispatchers.remove(xrt.getId());
                throw fe;
            } catch (RemoteException e) {
                logger.warn("Error during local call", e);
            }
        }

        xrt.startExecTime();
        Context previous = null;
        for (Mogram mogram: inputXrts) {
            if (xrt.isBlock()) {
                try {
                    if (mogram instanceof Exertion) {
                        if (((Exertion) mogram).getScope() != null) {
                            ((Exertion) mogram).getScope().append(xrt.getContext());
                        } else {
                            ((Exertion) mogram).setScope(xrt.getContext());
//                        ((Exertion) mogram).setScope(new ParModel());
//                        ((Exertion) mogram).getScope()).append(xrt.getContext());
                        }
                    } else {
                        if (mogram.getScope() != null)
                            mogram.getScope().append(xrt.getContext());
                        else {
                            mogram.setScope(xrt.getContext());
                        }
                    }
                } catch (Exception ce) {
                    throw new ExertionException(ce);
                }
            }

            try {
                if (mogram instanceof Exertion) {
                    ServiceExertion se = (ServiceExertion) mogram;
                    // support for continuous pre and post execution of task
                    // signatures
                    if (previous != null && se.isTask() && ((Task) se).isContinous())
                        se.setContext(previous);
                    dispatchExertion(se);
                    previous = se.getContext();
                } else if (mogram instanceof EntModel) {
                    ((EntModel)mogram).updateEntries(xrt.getContext());
                    xrt.getDataContext().append(((Model) mogram).getResponses());
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExertionException(e);
            }

        }

        if (masterXrt != null) {
            masterXrt = (ServiceExertion) execExertion(masterXrt); // executeMasterExertion();
            if (masterXrt.getStatus() <= FAILED) {
                state = FAILED;
                xrt.setStatus(FAILED);
            } else {
                state = DONE;
                xrt.setStatus(DONE);
            }
        } else
            state = DONE;
        dispatchers.remove(xrt.getId());
        xrt.stopExecTime();
        xrt.setStatus(DONE);
    }

    protected void dispatchExertion(ServiceExertion se) throws SignatureException, ExertionException {
        se = (ServiceExertion) execExertion(se);
        if (se.getStatus() <= FAILED) {
            xrt.setStatus(FAILED);
            state = FAILED;
            try {
                String pn = provider.getProviderName();
                if (pn == null)
                    pn = provider.getClass().getName();
                ExertionException fe = new ExertionException(pn
                        + " received failed task: " + se.getName(), se);
                xrt.reportException(fe);
                dispatchers.remove(xrt.getId());
                throw fe;
            } catch (RemoteException e) {
                logger.warn("Exception during local call");
            }
        } else if (se.getStatus() == SUSPENDED
                || xrt.getControlContext().isReview(se)) {
            xrt.setStatus(SUSPENDED);
            ExertionException ex = new ExertionException(
                    "exertion suspended", se);
            se.reportException(ex);
            dispatchers.remove(xrt.getId());
            throw ex;
        }
    }

    protected List<Mogram> getInputExertions() throws ContextException {
        return Mograms.getInputExertions(((Job) xrt));
    }
}
