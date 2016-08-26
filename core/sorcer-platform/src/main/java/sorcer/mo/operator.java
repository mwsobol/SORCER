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

import sorcer.core.Name;
import sorcer.core.context.MapContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.Proc;
import sorcer.core.context.model.ent.ProcModel;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.context.model.srv.SrvModel;
import sorcer.core.dispatch.SortingException;
import sorcer.core.dispatch.SrvModelAutoDeps;
import sorcer.core.plexus.FidelityManager;
import sorcer.core.plexus.MorphFidelity;
import sorcer.core.plexus.Morpher;
import sorcer.service.*;
import sorcer.service.modeling.ServiceModel;
import sorcer.service.modeling.Model;
import sorcer.service.Signature.ReturnPath;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static sorcer.eo.operator.context;

/**
 * Created by Mike Sobolewski on 4/26/15.
 */
public class operator {

    public static <T> T putValue(Context<T> context, String path, T value) throws ContextException {
        context.putValue(path, value);
        return value;
    }

    public static Object setValue(Model model, String entName, Object value)
        throws ContextException {
        Object entry = model.asis(entName);
        if (entry == null)
            try {
                model.add(sorcer.po.operator.ent(entName, value));
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        else if (entry instanceof Setter) {
            try {
                ((Setter) entry).setValue(value);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            ((ServiceContext)model).putValue(entName, value);
        }

        if (entry instanceof Proc) {
            Proc proc = (Proc) entry;
            if (proc.getScope() != null && proc.getContextable() == null)
                proc.getScope().putValue(proc.getName(), value);
        }

        ((ServiceMogram)model).setIsChanged(true);
        return value;
    }

    public static ProcModel procModel(String name, Signature builder) throws SignatureException {
        ProcModel model = (ProcModel) sorcer.co.operator.instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static ProcModel parModel(String name, Signature builder) throws SignatureException {
        ProcModel model = (ProcModel) sorcer.co.operator.instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static SrvModel srvModel(String name, Signature builder) throws SignatureException {
        SrvModel model = (SrvModel) sorcer.co.operator.instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static ProcModel procModel(Object... entries)
            throws ContextException {
        if (entries != null && entries.length == 1 && entries[0] instanceof Context) {
            ((Context)entries[0]).setModeling(true);
            try {
                return new ProcModel((Context)entries[0]);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        }
        ProcModel model = new ProcModel();
        Object[] dest = new Object[entries.length+1];
        System.arraycopy(entries,  0, dest,  1, entries.length);
        dest[0] = model;
        return (ProcModel) context(dest);
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

    public static ServiceModel responseUp(ServiceModel model, String... responsePaths) throws ContextException {
        for (String path : responsePaths)
            ((ServiceContext)model).getMogramStrategy().getResponsePaths().add(new Name(path));
        return model;
    }

    public static ServiceModel responseDown(ServiceModel model, String... responsePaths) throws ContextException {
        for (String path : responsePaths)
            ((ServiceContext)model).getMogramStrategy().getResponsePaths().remove(new Name(path));
        return model;
    }

    public static Context result(ServiceModel model) throws ContextException {
        return ((ServiceContext)model).getMogramStrategy().getOutcome();
    }

    public static Object result(ServiceModel model, String path) throws ContextException {
        return ((ServiceContext)model).getMogramStrategy().getOutcome().asis(path);
    }

    public static Object get(ServiceModel model, String path) throws ContextException {
        return ((ServiceContext)((ServiceContext)model).getMogramStrategy().getOutcome()).get(path);
    }

    public static  ServiceContext substitute(ServiceContext model, Entry... entries) throws ContextException {
        model.substitute(entries);
        return model;
    }

    public static Context ins(ServiceModel model) throws ContextException {
        return inputs(model);
    }

    public static Context allInputs(ServiceModel model) throws ContextException {
        try {
            return model.getAllInputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context inputs(ServiceModel model) throws ContextException {
        try {
            return model.getInputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context outs(ServiceModel model) throws ContextException {
        return outputs(model);
    }

    public static Context outputs(ServiceModel model) throws ContextException {
        try {
            return model.getOutputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Object resp(ServiceModel model, String path) throws ContextException {
        return response(model, path);
    }

    public static Object response(ServiceModel model, String path) throws ContextException {
        try {
            return ((ServiceContext)model).getResponseAt(path);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context resp(ServiceModel model) throws ContextException {
        return response(model);
    }

    public static Context response(ServiceModel model, Arg... args) throws ContextException {
        try {
            return (Context) model.getResponse(args);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static void traced(Model model, boolean isTraced) throws ContextException {
        ((FidelityManager)model.getFidelityManager()).setTraced(isTraced);
    }

    public static Entry entry(Model model, String path) throws ContextException {
        return new Entry(path, model.asis(path));
    }

    public static Context inConn(List<Entry> entries) throws ContextException {
        MapContext map = new MapContext();
        map.direction = MapContext.Direction.IN;
        sorcer.eo.operator.populteContext(map, entries);
        return map;
    }

    public static Context inConn(Entry... entries) throws ContextException {
        MapContext map = new MapContext();
        map.direction = MapContext.Direction.IN;
        List<Entry> items = Arrays.asList(entries);
        sorcer.eo.operator.populteContext(map, items);
        return map;
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

    public static ServiceFidelity<Arg> response(String... paths) {
        return  new ServiceFidelity(paths);
    }

    public static Paradigmatic modeling(Paradigmatic paradigm) {
        paradigm.setModeling(true);
        return paradigm;
    }

    public static Paradigmatic modeling(Paradigmatic paradigm, boolean modeling) {
        paradigm.setModeling(modeling);
        return paradigm;
    }

    public static Model srvModel(Object... items) throws ContextException {
        sorcer.eo.operator.Complement complement = null;
        List<Signature> sigs = new ArrayList<>();
        Fidelity responsePaths = null;
        SrvModel model = null;
        FidelityManager fiManager = null;
        List<ServiceFidelity<Fidelity>> metaFis = new ArrayList<>();
        List<Srv> morphFiEnts = new ArrayList();
        for (Object item : items) {
            if (item instanceof Signature) {
                sigs.add((Signature)item);
            } else if (item instanceof sorcer.eo.operator.Complement) {
                complement = (sorcer.eo.operator.Complement)item;
            } else if (item instanceof Model) {
                model = ((SrvModel)item);
            } else if (item instanceof FidelityManager) {
                fiManager = ((FidelityManager)item);
            } else if (item instanceof Srv && ((Entry)item)._2 instanceof MorphFidelity) {
                morphFiEnts.add((Srv)item);
            } else if (item instanceof Fidelity) {
                if (((Fidelity) item).getType().equals(Fidelity.Type.META)) {
                    metaFis.add((ServiceFidelity<Fidelity>) item);
                } else {
                    responsePaths = ((Fidelity) item);
                }
            }
        }
        if (model == null)
            model = new SrvModel();

        if (morphFiEnts != null || metaFis != null) {
           if (fiManager == null)
               fiManager = new FidelityManager(model);
        }
        if (fiManager != null) {
            model.setFidelityManager(fiManager);
            fiManager.init(metaFis);
            fiManager.setMogram(model);
            MorphFidelity mFi = null;
            if ((morphFiEnts.size() > 0)) {
                for (Srv morphFiEnt : morphFiEnts) {
                    mFi = (MorphFidelity) morphFiEnt._2;
                    fiManager.addMorphedFidelity(morphFiEnt._1, mFi);
                    fiManager.addFidelity(morphFiEnt._1, mFi.getFidelity());
                    mFi.setPath(morphFiEnt._1);
                    mFi.setSelect((Arg) mFi.getSelects().get(0));
                    mFi.addObserver(fiManager);
                    if (mFi.getMorpherFidelity() != null) {
                        // setValue the default morpher
                        mFi.setMorpher((Morpher) ((Entry)mFi.getMorpherFidelity().get(0))._2);
                    }
                }
            }
        }

        if (responsePaths != null) {
            model.getMogramStrategy().setResponsePaths(((ServiceFidelity) responsePaths).getSelects());
        }
        if (complement != null) {
            model.setSubject(complement.path(), complement.value());
        }

        Object[] dest = new Object[items.length+1];
        System.arraycopy(items,  0, dest,  1, items.length);
        dest[0] = model;
        return (Model)context(dest);
    }

    public static void run(sorcer.util.Runner runner, Arg... args) throws SignatureException, MogramException {
        runner.exec(args);
    }

    public static String printDeps(Mogram model) throws SortingException {
        return new SrvModelAutoDeps((SrvModel)model).printDeps();
    }

}
