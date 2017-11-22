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
package sorcer.so;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.Operator;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.ThrowableTrace;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.ProcModel;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.plexus.MultiFiMogram;
import sorcer.core.provider.Exerter;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Modeling;
import sorcer.service.modeling.Valuation;
import sorcer.service.modeling.ent;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Mike Sobolewski on 9/10/20.
 */
public class operator extends Operator {

    public static Entry act(Service service, Arg... args) throws ServiceException {
        try {
            return new Entry(((Identifiable)service).getName(), service.execute(args));
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    public static Entry act(Service service, String selector, Arg... args) throws ServiceException {
        try {
            return new Entry(selector, service.execute(args));
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    public static <T> T eval(ent<T> entry, Arg... args)
            throws EvaluationException {
        try {
            synchronized (entry) {
                if (entry instanceof Valuation) {
                    return (T) ((Entry) entry).get(args);
                } else if (((Entry) entry).asis() instanceof ServiceContext) {
                    return (T) ((ServiceContext) ((Entry) entry).asis()).getValue(entry.getName());
                } else if (entry instanceof Incrementor) {
                    return ((Incrementor<T>) entry).next();
                } else if (entry instanceof Evaluation) {
                    return (T) ((Evaluation) entry).evaluate(args);
                } else {
                    return (T) ((Entry) entry).get(args);
                }
            }
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }

    public static Object eval(Mogram mogram, Arg... args) throws MogramException {
        try {
            synchronized (mogram) {
                if (mogram instanceof Exertion) {
                    return exec(mogram, args);
                } else if (mogram instanceof Model) {
                    return ((Model)mogram).getResponse(args);
                }
            }
        } catch (RemoteException | ServiceException e) {
            throw new MogramException(e);
        }
        return null;
    }

    public static <T> T eval(ServiceInvoker<T> invoker, Arg... args)
            throws EvaluationException {
        try {
            if (invoker instanceof Incrementor){
                return ((Incrementor<T>) invoker).next();
            } else {
                return invoker.invoke(args);
            }
        } catch (RemoteException e) {
            throw new EvaluationException(e);
        }
    }

    public static Object eval(Model model, String selector,
                              Arg... args) throws ContextException {
        try {
            return model.getValue(selector, args);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context eval(Model model, Context context)
            throws ContextException {
        Context rc = null;
        try {
            rc = model.evaluate(context);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
        return rc;
    }

    public static Object eval(Model model, Arg... args)
            throws ContextException {
        try {
            synchronized (model) {
                if (model instanceof ProcModel) {
                    return ((ProcModel) model).getValue(args);
                } else {
                    return ((ServiceContext) model).getValue(args);
                }
            }
        } catch (Exception e) {
            throw new ContextException(e);
        }
    }

    public static Object eval(Exertion exertion, String selector,
                              Arg... args) throws EvaluationException {
        try {
            exertion.getDataContext().setReturnPath(new Signature.ReturnPath(selector));
            return exec(exertion, args);
        } catch (Exception e) {
            e.printStackTrace();
            throw new EvaluationException(e);
        }
    }

    /**
     * Assigns the tag for this context, for example "triplet|one|two|three" is a
     * tag (relation) named 'triplet' as a product of three "places" one, two, three.
     *
     * @param context
     * @param association
     * @throws ContextException
     */
    public static Context tagAssociation(Context context, String association)
            throws ContextException {
        context.setAttribute(association);
        return context;
    }

    public static Object execItem(Activity item, Arg... args) throws ServiceException {
        try {
            return item.execute(args);
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    public static Object exec(Service service, Arg... args) throws ServiceException, RemoteException {
        try {
            if (service instanceof Entry || service instanceof Signature ) {
                    return service.execute(args);
            } else if (service instanceof Context || service instanceof MultiFiMogram) {
                if (service instanceof Model) {
                    return ((Model)service).getResponse(args);
                } else {
                    return new sorcer.core.provider.exerter.ServiceShell().exec(service, args);
                }
            } else if (service instanceof Exertion) {
                return new ServiceShell().evaluate((Mogram) service, args);
            } else if (service instanceof Evaluation) {
                return ((Evaluation) service).evaluate(args);
            } else if (service instanceof Modeling) {
                Domain cxt = Arg.selectDomain(args);
                if (cxt != null) {
                    return ((Modeling) service).evaluate((ServiceContext)cxt);
                } else {
                    ((Context)service).substitute(args);
                    ((Modeling) service).evaluate();
                }
                return ((Model)service).getResult();
            }else {
                return service.execute(args);
            }
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    public static List<ThrowableTrace> exceptions(Exertion exertion) throws RemoteException {
        return exertion.getExceptions();
    }

    public static <T extends Mogram> T exert(T mogram, Arg... args) throws MogramException {
        try {
            return mogram.exert(null, args);
        } catch (Exception e) {
            throw new ExertionException(e);
        }
    }

    public static <T extends Mogram> T exert(T input,
                                             Transaction transaction,
                                             Arg... entries) throws ExertionException {
        return new sorcer.core.provider.exerter.ServiceShell().exert(input, transaction, entries);
    }

    public static <T extends Mogram> T exert(Exerter service, T mogram, Arg... entries)
            throws TransactionException, MogramException, RemoteException {
        return service.exert(mogram, null, entries);
    }

    public static <T extends Mogram> T exert(Mogram service, T mogram, Arg... entries)
            throws TransactionException, MogramException, RemoteException {
        return service.exert(mogram, null, entries);
    }

    public static <T extends Mogram> T exert(Mogram service, T mogram, Transaction txn, Arg... entries)
            throws TransactionException, MogramException, RemoteException {
        return service.exert(mogram, txn, entries);
    }

}