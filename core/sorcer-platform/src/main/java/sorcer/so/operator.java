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
import sorcer.co.tuple.SignatureEntry;
import sorcer.core.context.ContextSelector;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.ThrowableTrace;
import sorcer.core.context.model.DataContext;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.EntModel;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.plexus.FidelityManager;
import sorcer.core.plexus.MultiFiMogram;
import sorcer.core.provider.Exerter;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.service.*;
import sorcer.service.modeling.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static sorcer.co.operator.get;
import static sorcer.mo.operator.value;

/**
 * Created by Mike Sobolewski on 9/10/20.
 */
public class operator extends Operator {

    public static Entry execEnt(Service service, Arg... args) throws ServiceException {
        try {
            ContextSelector contextSelector = selectContextSelector(args);
            Object result = service.execute(args);
            if (result instanceof Context && contextSelector != null) {
                try {
                    result = contextSelector.doSelect(result);
                } catch (ContextException e) {
                    throw new ServiceException(e);
                }
            }
            return new Entry(((Identifiable) service).getName(), result);
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    public static ContextSelector selectContextSelector(Arg[] args) {
        for (Arg arg : args) {
            if (arg instanceof ContextSelector)
                return (ContextSelector) arg;
        }
        return null;
    }

    public static Entry execEnt(Service service, String selector, Arg... args) throws ServiceException {
        try {
            return new Entry(selector, service.execute(args));
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    public static <T> T exec(Entry<T> entry, Arg... args)
        throws EvaluationException {
        try {
            synchronized (entry) {
                if (entry instanceof Valuation) {
                    return (T) ((Entry) entry).valuate(args);
                } else if (entry instanceof Entry && ((Entry) entry).getOut() instanceof ServiceContext) {
                    return (T) ((ServiceContext) ((Entry) entry).getOut()).getValue(entry.getName(), args);
                } else if (entry instanceof Incrementor) {
                    return ((Incrementor<T>) entry).next();
                } else if (entry instanceof Exertion) {
                    return (T) ((Exertion) entry).exert(args).getContext();
                } else if (entry instanceof Functionality) {
                    if (entry instanceof Srv && entry.getImpl() instanceof SignatureEntry) {
                        return (T) entry.execute(args);
                    } else {
                        return (T) ((Functionality) entry).getValue(args);
                    }
                } else if (entry instanceof Evaluation) {
                    return (T) ((Entry) entry).evaluate(args);
                } else {
                    return (T) entry.execute(args);
                }
            }
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }

    public static <T> T eval(evr<T> invoker, Arg... args)
        throws EvaluationException {
        try {
            if (invoker instanceof Incrementor) {
                return ((Incrementor<T>) invoker).next();
            } else {
                return invoker.compute(args);
            }
        } catch (RemoteException e) {
            throw new EvaluationException(e);
        }
    }

    public static Object exec(Mogram domain, String path, Arg... args) throws ContextException {
        if (domain instanceof Model) {
            try {
                return ((Model) domain).getValue(path, args);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        } else {
            // includes exertions
            return value(domain.getContext(), path, args);
        }
    }

    public static Object exec(Domain domain, String path, Arg... args) throws ContextException {
        if (path.indexOf("$") > 0) {
            String pn = null;
            String dn = null;
            int ind = path.indexOf("$");
            pn = path.substring(0, ind);
            dn = path.substring(ind + 1);
            return response(domain, pn, dn);
        }
        if (domain instanceof Model) {
            try {
                return domain.getValue(path, args);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        } else {
            return value((Context) domain, path, args);
        }
    }

    public static void clear(Discipline discipline) throws MogramException {
        discipline.clear();
    }

    public static Context eval(Discipline discipline, Arg... args)
        throws ContextException {
        try {
            return (Context) discipline.execute(args);
        } catch (ServiceException | RemoteException e) {
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

    public static Response query(Mogram mogram, Arg... args) throws ContextException {
        try {
            synchronized (mogram) {
                if (mogram instanceof Exertion) {
                    return mogram.exert(args).getContext();
                } else {
                    return (Response) ((EntModel) mogram).getValue(args);
                }
            }
        } catch (RemoteException | ServiceException e) {
            throw new ContextException(e);
        }
    }

    public static Fidelity<Path> response(String... paths) {
        Fidelity resp = new Fidelity("RESPONSE");
        resp.setSelects(Path.getPathList(paths));
        resp.setType(Fi.Type.RESPONSE);
        return resp;
    }

    public static Object resp(Mogram model, String path) throws ContextException {
        return response(model, path);
    }

    public static Context resp(Mogram model) throws ContextException {
        return response(model);
    }

    public static Object response(Exertion exertion, String path) throws ContextException {
        try {
            return ((ServiceContext)exertion.exert().getContext()).getResponseAt(path);
        } catch (RemoteException | MogramException e) {
            throw new ContextException(e);
        }
    }

    public static Object exec(Domain model, String path$domain) throws ContextException {
        if (model instanceof DataContext) {
            return value((Context)model, path$domain);
        } else {
            return response(model, path$domain);
        }
    }

    public static Object response(Domain model, String path$domain) throws ContextException {
        try {
            String path = null;
            String domain = null;
            if (path$domain.indexOf("$") > 0) {
                int ind = path$domain.indexOf("$");
                path = path$domain.substring(0, ind);
                domain = path$domain.substring(ind + 1);
                return response(model, path, domain);
            } else {
                return ((ServiceContext) model).getResponseAt(path$domain);
            }
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Object exec(Domain model, String path, String domain) throws ContextException {
        return response(model, path, domain);
    }

    public static Object response(Domain model, String path, String domain) throws ContextException {
        if (model.isEvaluated() && ((ServiceMogram)model).getMdaFi() == null) {
            return ((ServiceContext)model).getDomain(domain).getEvaluatedValue(path);
        } else {
            try {
                return ((Context)((ServiceContext)model).getDomain(domain)).getValue(path);
            } catch (RemoteException ex) {
                throw new ContextException(ex);
            }
        }
    }

    public static ServiceContext eval(Mogram mogram, Object... items) throws ContextException {
        return response(mogram, items);
    }

    public static ServiceContext response(Mogram mogram, Object... items) throws ContextException {
        if (mogram instanceof Exertion) {
            return exertionResponse((Exertion) mogram, items);
        } else if (mogram instanceof Domain &&  ((ServiceMogram)mogram).getType().equals(Functionality.Type.MADO)) {
            if (mogram.isEvaluated()) {
                return (ServiceContext) ((ServiceContext) mogram).getDomain((String) items[0]).getEvaluatedValue((String) items[1]);
            } else {
                return (ServiceContext) ((ServiceContext) ((ServiceContext) mogram).getDomain((String) items[0])).getValue((String) items[1]);
            }
        } else {
            return modelResponse((Domain) mogram, items);
        }
    }

    public static ServiceContext modelResponse(Domain model, Object... items) throws ContextException {
        try {
            List<Arg> argl = new ArrayList();
            List<Path> paths = new ArrayList();;
            for (Object item : items) {
                if (item instanceof Path) {
                    paths.add((Path) item);
                } if (item instanceof String) {
                    paths.add(new Path((String) item));
                } else if (item instanceof FidelityList) {
                    argl.addAll((Collection<? extends Arg>) item);
                } else if (item instanceof List
                        && ((List) item).size() > 0
                        && ((List) item).get(0) instanceof Path) {
                    paths.addAll((List<Path>) item);
                } else if (item instanceof Arg) {
                    argl.add((Arg) item);
                }
            }
            if (paths != null && paths.size() > 0) {
                model.getMogramStrategy().setResponsePaths(paths);
            }
            Arg[] args = new Arg[argl.size()];
            argl.toArray(args);
            if (model.getFidelityManager() != null) {
                ((FidelityManager) model.getFidelityManager()).reconfigure(Arg.selectFidelities(args));
            }
            model.substitute(args);
            return (ServiceContext) model.getResponse(args);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static ServiceContext exertionResponse(Exertion exertion, Object... items) throws ContextException {
        try {
            List<Arg> argl = new ArrayList();
            List<Path> paths = new ArrayList();;
            for (Object item : items) {
                if (item instanceof Path) {
                    paths.add((Path) item);
                } if (item instanceof String) {
                    paths.add(new Path((String) item));
                } else if (item instanceof FidelityList) {
                    argl.addAll((Collection<? extends Arg>) item);
                } else if (item instanceof List
                        && ((List) item).size() > 0
                        && ((List) item).get(0) instanceof Path) {
                    paths.addAll((List<Path>) item);
                } else if (item instanceof Arg) {
                    argl.add((Arg) item);
                }
            }
            if (paths != null && paths.size() > 0) {
                exertion.getMogramStrategy().setResponsePaths(paths);
            }
            Arg[] args = new Arg[argl.size()];
            argl.toArray(args);
            if (exertion.getFidelityManager() != null) {
                ((FidelityManager) exertion.getFidelityManager()).reconfigure(Arg.selectFidelities(args));
            }
            return (ServiceContext) exertion.exert(args).getContext();
        } catch (RemoteException | MogramException e) {
            throw new ContextException(e);
        }
    }

    public static Object eval(Exertion exertion, String selector,
                              Arg... args) throws EvaluationException {
        try {
            exertion.getDataContext().setReturnPath(new Signature.ReturnPath(selector));
            return exec((Service)exertion, args);
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

    public static Object execItem(Request item, Arg... args) throws ServiceException {
        try {
            return item.execute(args);
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    public static Object execMogram(Mogram mogram, Arg... args) throws ContextException {
        try {
            Object out = null;
            synchronized (mogram) {
                if (mogram instanceof Exertion) {
                    out = new ServiceShell().evaluate(mogram, args);
                } else {
                    out = ((ServiceContext) mogram).getValue(args);
                }
                ((ServiceMogram)mogram).setChanged(true);
                return out;
            }
        } catch (RemoteException | ServiceException e) {
            throw new ContextException(e);
        }
    }

    public static Object exec(Service service, Arg... args) throws ServiceException {
        try {
            if (service instanceof Entry || service instanceof Signature ) {
                return service.execute(args);
            } else if (service instanceof Mogram) {
                if (service instanceof DataContext || service instanceof MultiFiMogram) {
                    return new sorcer.core.provider.exerter.ServiceShell().exec(service, args);
                } else if (service instanceof Discipline) {
                    return service.execute(args);
                } else {
                    return execMogram((Mogram) service, args);
                }
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
            } else {
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