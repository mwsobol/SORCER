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

package sorcer.core.provider.exerter;

import sorcer.core.context.ControlContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.Subroutine;
import sorcer.service.*;

import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski on 4/30/15.
 */
public class Binder {

    private Mogram  mogram;

    public Binder(Mogram mogram) {
        this.mogram = mogram;
    }

    public Mogram bind(Arg... args) throws ContextException {
        try {
            mogram.substitute(args);

            if (mogram instanceof Exertion) {
                ((ServiceExertion)mogram).selectFidelity(args);
                initExecState(args);
            } else if (mogram instanceof Context) {
                mogram.substitute(args);
            }
        } catch (Exception e) {
            throw new ContextException(e);
        }

        return mogram;
    }


    private void initExecState(Arg... entries) throws ContextException, RemoteException {
        ServiceExertion exertion = (ServiceExertion)mogram;
        Context argCxt = null;
        if (entries!=null) {
            for (Arg arg : entries) {
                if (arg instanceof Context && ((Context)arg).size() > 0) {
                    argCxt = (Context)arg;
                }
            }
        }
        if (mogram instanceof Block) {
            resetContext(exertion, argCxt, entries);
        }
//		else if (exertion.getScope() != null) {
//			exertion.getDataContext().append((Context)exertion.getScope());
//		}
        Exec.State state = exertion.getControlContext().getExecState();
        if (state == Exec.State.INITIAL) {
            for (Mogram e : exertion.getAllMograms()) {
                if (e instanceof Exertion) {
                    if (((ControlContext) ((Exertion)e).getControlContext()).getExecState() == Exec.State.INITIAL) {
                        e.setStatus(Exec.INITIAL);
                    }
                }
                if (e instanceof Block) {
                    resetContext((Exertion)e, argCxt);
                }
            }
        }
    }

    private void resetContext(Exertion exertion, Context context, Arg... entries) throws ContextException, RemoteException {
        Context initContext = ((ServiceContext)exertion.getDataContext()).getInitContext();
        // overwrite initContext
        if (initContext != null) {
            exertion.getDataContext().append(initContext);
            if (entries != null) {
                for (Arg a : entries) {
                    if (a instanceof Entry) {
                        initContext.putValue(
                                a.getName(), ((Entry) a).get());
                    }
                }
            }
        }
        // overwrite dataContext
        if (entries != null) {
            for (Arg a : entries) {
                if (a instanceof Entry) {
                    exertion.getContext().putValue(
                            a.getName(), ((Entry) a).get());
                }
            }
        }
        if (context != null) {
            exertion.getDataContext().append(context);
        }
    }

    private void resetScope(Exertion exertion) throws ContextException, RemoteException {
        ((ServiceContext)exertion.getDataContext()).clearScope();
        exertion.getDataContext().append(((ServiceContext)exertion.getDataContext()).getInitContext());
    }

}

