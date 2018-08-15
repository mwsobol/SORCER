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

package sorcer.mo;

import sorcer.co.tuple.ExecDependency;
import sorcer.co.tuple.InoutValue;
import sorcer.co.tuple.InputValue;
import sorcer.co.tuple.OutputValue;
import sorcer.core.context.MapContext;
import sorcer.core.context.ModelStrategy;
import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.*;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.context.model.srv.SrvModel;
import sorcer.core.dispatch.DispatcherException;
import sorcer.core.dispatch.ProvisionManager;
import sorcer.core.dispatch.SortingException;
import sorcer.core.dispatch.SrvModelAutoDeps;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.plexus.FidelityManager;
import sorcer.core.plexus.MorphFidelity;
import sorcer.core.plexus.Morpher;
import sorcer.service.*;
import sorcer.service.Domain;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Model;
import sorcer.service.Signature.ReturnPath;
import sorcer.util.url.sos.SdbUtil;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.*;


/**
 * Created by Mike Sobolewski on 4/26/15.
 */
public class operator {

    protected static int count = 0;

    public static ServiceFidelity mdlFi(Service... models) {
        ServiceFidelity fi = new ServiceFidelity(models);
        fi.fiType = ServiceFidelity.Type.MODEL;
        return fi;
    }

    public static ServiceFidelity mdlFi(String fiName, Service... models) {
        ServiceFidelity fi = new ServiceFidelity(fiName, models);
        fi.fiType = ServiceFidelity.Type.MODEL;
        return fi;
    }

    public static <T> T putValue(Context<T> context, String path, T value) throws ContextException {
        context.putValue(path, value);
        ((ServiceMogram)context).setChanged(true);
        return value;
    }

    public static Context setValues(Context model, Context context) throws ContextException {
        ServiceContext cxt = (ServiceContext)context;
        String path;
        Object obj;
        Object v;
        Iterator i = cxt.keyIterator();
        while (i.hasNext()) {
            path = (String) i.next();
            obj =  cxt.get(path);
            v = model.get(path);
            if (v instanceof Entry) {
                try {
                    ((Entry)v).setValue(obj);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            } else {
                model.putValue(path, obj);
            }
            ((ServiceContext)model).setChanged(true);

        }
        return model;
    }

    public static Domain setValues(Domain model, Entry... entries) throws ContextException {
        for (Entry e : entries) {
            Object v = model.asis(e.getName());
            Object nv = e.asis();
            String en = e.getName();
            try {
                if (v instanceof Setter) {
                    ((Setter) v).setValue(nv);
                } else if (SdbUtil.isSosURL(v)) {
                    SdbUtil.update((URL) v, e.asis());
                } else {
                    ((Context)model).putValue(e.getName(), e.asis());
                }
            } catch (RemoteException | MogramException | SignatureException re) {
                throw new ContextException(re);
            }
            ((ServiceContext)model).setChanged(true);
        }
        return model;
    }

    public static Domain setValue(Domain model, String entName, Object value)
        throws ContextException {
        Object entry = model.get(entName);
        try {
            if (entry == null) {
                model.add(sorcer.po.operator.ent(entName, value));
            } else if (entry instanceof Entry) {
                ((Entry) entry).setValue(value);
            } else if (entry instanceof Setter) {
                ((Setter) entry).setValue(value);
            } else {
                ((ServiceContext)model).put(entName, value);
            }
        } catch (RemoteException e) {
            throw new ContextException(e);
        }

        if (entry instanceof Proc) {
            Proc proc = (Proc) entry;
            if (proc.getScope() != null)
                proc.getScope().putValue(proc.getName(), value);
        }
        ((ServiceMogram)model).setChanged(true);
        return model;
    }

    public static Model setValue(Model model, String entName, String path, Object value)
        throws ContextException {
        Object entry = model.asis(entName);
        if (entry instanceof Setup) {
            ((Setup) entry).setEntry(path, value);
        } else {
            throw new ContextException("A Setup is required with: " + path);
        }
        return model;
    }

    public static Model setValue(Model model, String entName, Subroutine... entries)
            throws ContextException {
        Object entry = model.asis(entName);
        if (entry != null) {
            if (entry instanceof Setup) {
                for (Subroutine e : entries) {
                    ((Setup) entry).getContext().putValue(e.getName(), e.get());
                }
            }
            ((Setup)entry).isValid(false);
//            ((Setup)entry).getEvaluation().setValueIsCurrent(false);
        }
        return model;
    }

    public static Model setValue(Model model, Slot... entries) throws ContextException {
        for(Slot slot :entries) {
            setValue(model, slot.getName(), slot.get());
        }
        return model;
    }

    public static Model setValue(Model model, Entry... entries) throws ContextException {
        for(Entry ent :entries) {
            setValue(model, ent.getName(), ent.get());
        }
        return model;
    }

    public static EntryModel procModel(String name, Signature builder) throws SignatureException {
        EntryModel model = (EntryModel) instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static EntryModel parModel(String name, Signature builder) throws SignatureException {
        EntryModel model = (EntryModel) instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static SrvModel srvModel(String name, Signature builder) throws SignatureException {
        SrvModel model = (SrvModel) instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static EntryModel procModel(String name, Identifiable... objects)
            throws ContextException, RemoteException {
        EntryModel entModel = new EntryModel(objects);
        entModel.setName(name);
        return entModel;

    }

    public static EntryModel procModel(Identifiable... objects)
            throws ContextException, RemoteException {
        return new EntryModel(objects);
    }

    public static EntryModel procModel(Object... entries)
            throws ContextException {
        if (entries != null && entries.length == 1 && entries[0] instanceof Context) {
            ((Context)entries[0]).setModeling(true);
            try {
                return new EntryModel((Context)entries[0]);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        }
        EntryModel model = new EntryModel();
        Object[] dest = new Object[entries.length+1];
        System.arraycopy(entries,  0, dest,  1, entries.length);
        dest[0] = model;
        return (EntryModel) context(dest);
    }

    public static Model inConn(Model model, Context inConnector) {
        ((ServiceContext)model).getMogramStrategy().setInConnector(inConnector);
        if (inConnector instanceof MapContext)
            ((MapContext)inConnector).direction =  MapContext.Direction.IN;
        return model;
    }

    public static Model outConn(Model model, Context outConnector) {
        ((ServiceContext) model).getMogramStrategy().setOutConnector(outConnector);
        if (outConnector instanceof MapContext)
            ((MapContext)outConnector).direction = MapContext.Direction.OUT;
        return model;
    }

    public static Model responseClear(Model model) throws ContextException {
            ((ServiceContext)model).getMogramStrategy().getResponsePaths().clear();
        return model;
    }

    public static Mogram responseUp(Mogram mogram, String... responsePaths) throws ContextException {
        if (responsePaths == null || responsePaths.length == 0) {
            ((ServiceContext) mogram).getMogramStrategy().getResponsePaths().clear();
            ((ServiceContext) mogram).getMogramStrategy().getResponsePaths().addAll(((ServiceContext) mogram).getOutPaths());
        } else {
            for (String path : responsePaths) {
                ((ServiceContext) mogram).getMogramStrategy().getResponsePaths().add(new Path(path));
            }
        }
        return mogram;
    }

    public static Domain clearResponse(Domain model) throws ContextException {
        ((ServiceContext) model).getMogramStrategy().getResponsePaths().clear();
        return model;
    }

    public static Mogram responseDown(Mogram mogram, String... responsePaths) throws ContextException {
        if (responsePaths == null || responsePaths.length == 0) {
            ((ServiceContext) mogram).getMogramStrategy().getResponsePaths().clear();
        } else {
            for (String path : responsePaths) {
                ((ServiceContext) mogram).getMogramStrategy().getResponsePaths().remove(new Path(path));
            }
        }
        return mogram;
    }

    public static Entry result(Entry entry) throws ContextException {
        Entry out = null;

        if (entry.asis() instanceof ServiceContext) {
            out = new Entry(entry.getName(), ((ServiceContext)entry.asis()).getValue(entry.getName()));
            return out;
        } else {
            out = new Entry(entry.getName(), entry.getImpl());
        }
        return out;
    }

    public static Context result(Domain model) throws ContextException {
        return ((ServiceContext)model).getMogramStrategy().getOutcome();
    }

    public static Object result(Domain model, String path) throws ContextException {
        return ((ServiceContext)model).getMogramStrategy().getOutcome().asis(path);
    }

    public static Object get(Domain model, String path) throws ContextException {
        return model.get(path);
    }

    public static  ServiceContext substitute(ServiceContext model, Subroutine... entries) throws ContextException {
        model.substitute(entries);
        return model;
    }

    public static Context ins(Domain model) throws ContextException {
        return inputs(model);
    }

    public static Context allInputs(Domain model) throws ContextException {
        try {
            return model.getAllInputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context inputs(Domain model) throws ContextException {
        try {
            return model.getInputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context outs(Domain model) throws ContextException {
        return outputs(model);
    }

    public static Context outputs(Domain model) throws ContextException {
        try {
            return model.getOutputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Mogram setResponse(Mogram mogram, Path... mogramPaths) throws ContextException {
        List<Path> paths = Arrays.asList(mogramPaths);
        mogram.getMogramStrategy().setResponsePaths(paths);
        return mogram;
    }

    public static Mogram setResponse(Mogram mogram, String... mogramPaths) throws ContextException {
        List<Path> paths = new ArrayList();
        for (String ps : mogramPaths) {
            paths.add(new Path(ps));
        }
        mogram.getMogramStrategy().setResponsePaths(paths);
        return mogram;
    }

    public static void init(Domain model, Arg... args) throws ContextException {
        // initialize a model
        Map<String, List<ExecDependency>> depMap = ((ModelStrategy)model.getMogramStrategy()).getDependentPaths();
        Signature.Paths paths = Arg.selectPaths(args);
        if (paths != null) {
            model.getDependers().add(new ExecDependency(paths));
        }
        if (depMap != null && model instanceof Model) {
            model.execDependencies("_init_", args);
        }
    }

    public static Mogram clear(Mogram mogram) throws MogramException {
         mogram.clear();
         return mogram;
    }

    public static ServiceContext out(Mogram mogram) {
            return (ServiceContext) mogram.getMogramStrategy().getOutcome();
    }

    public static void traced(Model model, boolean isTraced) throws ContextException {
        ((FidelityManager)model.getFidelityManager()).setTraced(isTraced);
    }

    public static Context inConn(List<Entry> entries) throws ContextException {
        MapContext map = new MapContext();
        map.direction = MapContext.Direction.IN;
        sorcer.eo.operator.populteContext(map, entries);
        return map;
    }

    public static Context inConn(boolean isRedundant, Value... entries) throws ContextException {
        MapContext map = new MapContext();
        map.direction = MapContext.Direction.IN;
        map.isRedundant = isRedundant;
        List<Entry> items = Arrays.asList(entries);
        sorcer.eo.operator.populteContext(map, items);
        return map;
    }
    public static Context inConn(Value... entries) throws ContextException {
        return inConn(false, entries);
    }

    public static Context outConn(List<Entry> entries) throws ContextException {
        MapContext map = new MapContext();
        map.direction = MapContext.Direction.OUT;
        sorcer.eo.operator.populteContext(map, entries);
        return map;
    }

    public static Context outConn(Entry... entries) throws ContextException {
        MapContext map = new MapContext();
        map.direction = MapContext.Direction.OUT;
        List<Entry> items = Arrays.asList(entries);
        sorcer.eo.operator.populteContext(map, items);
        return map;
    }

    public static ReturnPath returnPath(String path) {
        return  new ReturnPath<>(path);
    }

    public static Paradigmatic modeling(Paradigmatic paradigm) {
        paradigm.setModeling(true);
        return paradigm;
    }

    public static Paradigmatic modeling(Paradigmatic paradigm, boolean modeling) {
        paradigm.setModeling(modeling);
        return paradigm;
    }

    public static Mogram addFidelities(Mogram mogram, Fidelity... fidelities) {
        for (Fidelity fi : fidelities) {
            ((FidelityManager)mogram.getFidelityManager()).put(fi.getName(), fi);
        }
        return mogram;
    }

    public static Mogram reconfigure(Mogram mogram, Fidelity... fidelities) throws ContextException {
        FidelityList fis = new FidelityList();
        List<String> metaFis = new ArrayList<>();
        try {
            for (Fidelity fi : fidelities) {
                if (fi instanceof Metafidelity) {
                    metaFis.add(fi.getName());
                } else if (fi instanceof Fidelity) {
                    fis.add(fi);
                }
            }
            if (metaFis.size() > 0) {
                ((FidelityManager)mogram.getFidelityManager()).morph(metaFis);
            }
            if (fis.size() > 0) {
                ((FidelityManager)mogram.getFidelityManager()).reconfigure(fis);
            }
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
        return mogram;
    }

    public static Mogram reconfigure(Mogram model, List fiList) throws ContextException {
        try {
            if (fiList instanceof FidelityList) {
                ((FidelityManager) model.getFidelityManager()).reconfigure((FidelityList) fiList);
            } else {
                throw new ContextException("A list of fidelities is required for reconfigurartion");
            }
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
        return model;
    }

    public static Mogram morph(Mogram model, String... fiNames) throws ContextException {
//        ((FidelityManager)model.getFidelityManager()).morph(fiNames);
        try {
            model.morph(fiNames);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
        return model;
    }

    public static Model model(Object... items) throws ContextException {
        String name = "unknown" + count++;
        boolean hasEntry = false;
        boolean aneType = false;
        boolean procType = false;
        boolean srvType = false;
        boolean hasExertion = false;
        boolean hasSignature = false;
        boolean isFidelity = false;
        boolean autoDeps = true;
        for (Object i : items) {
            if (i instanceof String) {
                name = (String) i;
            } else if (i instanceof Exertion) {
                hasExertion = true;
            } else if (i instanceof Signature) {
                hasSignature = true;
            } else if (i instanceof Entry) {
                try {
                    hasEntry = true;
                    if (i instanceof Proc)
                        procType = true;
                    else if (i instanceof Srv || i instanceof Ane) {
                        srvType = true;
                    }
                } catch (Exception e) {
                    throw new ModelException(e);
                }
            } else if (i.equals(Strategy.Flow.EXPLICIT)) {
                autoDeps = false;
            } else if (i instanceof Fidelity) {

            }
        }

        if ((hasEntry|| hasSignature && hasEntry) && !hasExertion) {
            Model mo = null;
            if (srvType) {
                mo = srvModel(items);
            } else if (procType) {
                if (isFidelity) {
                    mo = srvModel(procModel(items));
                } else {
                    mo = procModel(items);
                }
            }
            // default model
            if (mo == null) {
                mo = procModel(items);
            }
            mo.setName(name);
            if (mo instanceof SrvModel && autoDeps) {
                try {
                    mo = new SrvModelAutoDeps((SrvModel) mo).get();
                } catch (SortingException e) {
                    throw new ContextException(e);
                }
            }
            ((ModelStrategy)mo.getMogramStrategy()).setOutcome(new ServiceContext(name + "-Output)"));
            return mo;
        }
        throw new ModelException("do not know what model to create");
    }

    public static Context add(Domain model, Identifiable... objects)
            throws ContextException, RemoteException {
        return add((Context)model, objects);
    }

    public static Context add(Context context, Identifiable... objects)
            throws RemoteException, ContextException {
        if (context instanceof Model) {
            return (Context) context.add(objects);
        }
        boolean isReactive = false;
        for (Identifiable i : objects) {
            if (i instanceof Reactive && ((Reactive) i).isReactive()) {
                isReactive = true;
            }
            if (i instanceof Mogram) {
                ((Mogram) i).setScope(context);
                i = srv(i);
            }
            if (context instanceof PositionalContext) {
                PositionalContext pc = (PositionalContext) context;
                if (i instanceof InputValue) {
                    if (isReactive) {
                        pc.putInValueAt(i.getName(), i, pc.getTally() + 1);
                    } else {
                        pc.putInValueAt(i.getName(), ((Entry) i).getImpl(), pc.getTally() + 1);
                    }
                } else if (i instanceof OutputValue) {
                    if (isReactive) {
                        pc.putOutValueAt(i.getName(), i, pc.getTally() + 1);
                    } else {
                        pc.putOutValueAt(i.getName(), ((Entry) i).getImpl(), pc.getTally() + 1);
                    }
                } else if (i instanceof InoutValue) {
                    if (isReactive) {
                        pc.putInoutValueAt(i.getName(), i, pc.getTally() + 1);
                    } else {
                        pc.putInoutValueAt(i.getName(), ((Entry) i).getImpl(), pc.getTally() + 1);
                    }
                } else {
                    if (i instanceof Value) {
                        pc.putValueAt(i.getName(), ((Entry) i).getOut(), pc.getTally() + 1);
                    } else {
                        if (context instanceof EntryModel || isReactive) {
                            pc.putValueAt(i.getName(), i, pc.getTally() + 1);
                        } else {
                            pc.putValueAt(i.getName(), ((Entry) i).getImpl(), pc.getTally() + 1);
                        }
                    }
                }
            } else if (context instanceof ServiceContext) {
                if (i instanceof InputValue) {
                    if (i instanceof Reactive) {
                        context.putInValue(i.getName(), i);
                    } else {
                        context.putInValue(i.getName(), ((Subroutine) i).getImpl());
                    }
                } else if (i instanceof OutputValue) {
                    if (isReactive) {
                        context.putOutValue(i.getName(), i);
                    } else {
                        context.putOutValue(i.getName(), ((Subroutine) i).getImpl());
                    }
                } else if (i instanceof InoutValue) {
                    if (isReactive) {
                        context.putInoutValue(i.getName(), i);
                    } else {
                        context.putInoutValue(i.getName(), ((Subroutine) i).getImpl());
                    }
                } else {
                    if (context instanceof EntryModel || isReactive) {
                        context.putValue(i.getName(), i);
                    } else {
                        context.putValue(i.getName(), ((Entry) i).getImpl());
                    }
                }
            }

            if (i instanceof Entry) {
                Entry e = (Entry) i;
                if (e.getAnnotation() != null) {
                    context.mark(e.getName(), e.annotation().toString());
                }
                if (e.asis() instanceof Scopable) {
                    ((Scopable) e.asis()).setScope(context);
                }
            }

            if (i instanceof Proc && ((Proc)i).getImpl() instanceof Invocation) {
                ((Proc)i).setScope(context);
                ((ServiceInvoker)((Proc)i).getImpl()).setScope(context);
            }
        }
        context.isChanged();
        return context;
    }

    public static Model aneModel(String name, Object... objects)
            throws ContextException, RemoteException {
        return srvModel(name, objects);
    }

    public static EntryModel procModel(String name, Object... objects)
            throws RemoteException, ContextException {
        EntryModel pm = new EntryModel(name);
        for (Object o : objects) {
            if (o instanceof Identifiable)
                pm.add((Identifiable)o);
        }
        return pm;
    }

    public static Object get(EntryModel pm, String parname, Arg... parametrs)
            throws ContextException, RemoteException {
        Object obj = pm.asis(parname);
        if (obj instanceof Proc)
            obj = ((Proc)obj).evaluate(parametrs);
        return obj;
    }

    public static Model srvModel(Object... items) throws ContextException {
        sorcer.eo.operator.Complement complement = null;
        Fidelity<Path> responsePaths = null;
        SrvModel model = null;
        FidelityManager fiManager = null;
        List<Metafidelity> metaFis = new ArrayList<>();
        List<Srv> morphFiEnts = new ArrayList();
        List<Fidelity> fis = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof sorcer.eo.operator.Complement) {
                complement = (sorcer.eo.operator.Complement)item;
            } else if (item instanceof Model) {
                model = ((SrvModel)item);
            } else if (item instanceof FidelityManager) {
                fiManager = ((FidelityManager)item);
            } else if (item instanceof Srv && ((Srv)item).getImpl() instanceof MorphFidelity) {
                morphFiEnts.add((Srv)item);
            } else if (item instanceof Fidelity) {
                if (item instanceof Metafidelity) {
                    metaFis.add((Metafidelity) item);
                } else {
                    if (((Fidelity)item).getFiType() == Fi.Type.RESPONSE){
                        responsePaths = (Fidelity<Path>) item;
                    }
                }
            } else if (item instanceof Entry && ((Entry)item).getMultiFi() != null) {
                Fidelity fi = (Fidelity) ((Entry)item).getMultiFi();
                fi.setName(((Entry)item).getName());
                fi.setPath(((Entry)item).getName());
                fis.add(fi);
            }
        }
        if (model == null)
            model = new SrvModel();

        if (morphFiEnts != null || metaFis != null || fis != null) {
           if (fiManager == null)
               fiManager = new FidelityManager(model);
        }
        if (fiManager != null) {
            fiManager.setMogram(model);
            model.setFidelityManager(fiManager);
            fiManager.init(metaFis);
            fiManager.add(fis);
            MorphFidelity mFi = null;
            if ((morphFiEnts.size() > 0)) {
                for (Srv morphFiEnt : morphFiEnts) {
                    mFi = (MorphFidelity) morphFiEnt.getImpl() ;
                    fiManager.addMorphedFidelity(morphFiEnt.getName(), mFi);
                    fiManager.addFidelity(morphFiEnt.getName(), mFi.getFidelity());
                    mFi.setPath(morphFiEnt.getName());
                    mFi.setSelect(mFi.getSelects().get(0));
                    mFi.addObserver(fiManager);
                    if (mFi.getMorpherFidelity() != null) {
                        // set the default morpher
                        mFi.setMorpher((Morpher) ((Subroutine)mFi.getMorpherFidelity().get(0)).getImpl());
                    }
                }
            }
        }

        if (responsePaths != null) {
            model.getMogramStrategy().setResponsePaths(responsePaths.getSelects());
        }
        if (complement != null) {
            model.setSubject(complement.getName(), complement.getId());
        }

        Object[] dest = new Object[items.length+1];
        System.arraycopy(items,  0, dest,  1, items.length);
        dest[0] = model;
        return (Model)context(dest);
    }

    public static void update(Mogram mogram, Setup... entries) throws ContextException {
        try {
            mogram.update(entries);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static void run(sorcer.util.Runner runner, Arg... args) throws SignatureException, MogramException {
        runner.exec(args);
    }

    public static String printDeps(Mogram model) throws SortingException, ContextException {
        return new SrvModelAutoDeps((SrvModel)model).printDeps();
    }

    public static boolean provision(Signature... signatures) throws  DispatcherException {
        ProvisionManager provisionManager = new ProvisionManager(Arrays.asList(signatures));
        return provisionManager.deployServices();
    }
}
