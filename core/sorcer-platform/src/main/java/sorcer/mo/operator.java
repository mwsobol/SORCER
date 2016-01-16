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

import sorcer.co.tuple.Tuple2;
import sorcer.core.Name;
import sorcer.core.context.MapContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.EntModel;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.context.model.srv.SrvModel;
import sorcer.core.dispatch.SortingException;
import sorcer.core.dispatch.SrvModelAutoDeps;
import sorcer.core.plexus.FidelityManager;
import sorcer.core.plexus.MultiFidelity;
import sorcer.core.plexus.MultiFidelityMogram;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.Signature.ReturnPath;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mike Sobolewski on 4/26/15.
 */
public class operator {

    public static EntModel entModel(String name, Signature builder) throws SignatureException {
        EntModel model = (EntModel) sorcer.co.operator.instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static ParModel parModel(String name, Signature builder) throws SignatureException {
        ParModel model = (ParModel) sorcer.co.operator.instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static SrvModel srvModel(String name, Signature builder) throws SignatureException {
        SrvModel model = (SrvModel) sorcer.co.operator.instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static Context entModel(Object... entries)
            throws ContextException {
        if (entries != null && entries.length == 1 && entries[0] instanceof Context) {
            ((Context)entries[0]).setModeling(true);
            try {
                return new EntModel((Context)entries[0]);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        }
        EntModel model = new EntModel();
        Object[] dest = new Object[entries.length+1];
        System.arraycopy(entries,  0, dest,  1, entries.length);
        dest[0] = model;
        return sorcer.eo.operator.context(dest);
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

    public static Model responseUp(Model model, String... responsePaths) throws ContextException {
        for (String path : responsePaths)
            ((ServiceContext)model).getMogramStrategy().getResponsePaths().add(new Name(path));
        return model;
    }

    public static Model responseDown(Model model, String... responsePaths) throws ContextException {
        for (String path : responsePaths)
            ((ServiceContext)model).getMogramStrategy().getResponsePaths().remove(new Name(path));
        return model;
    }

    public static Context result(Model model) throws ContextException {
        return ((ServiceContext)model).getMogramStrategy().getOutcome();
    }

    public static Object resultAt(Model model, String path) throws ContextException {
        return ((ServiceContext)((ServiceContext)model).getMogramStrategy().getOutcome()).get(path);
    }

    public static  ServiceContext substitute(ServiceContext model, Entry... entries) throws ContextException {
        model.substitute(entries);
        return model;
    }

    public static Context ins(Model model) throws ContextException {
        return inputs(model);
    }

    public static Context allInputs(Model model) throws ContextException {
        try {
            return model.getAllInputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context inputs(Model model) throws ContextException {
        try {
            return model.getInputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context outs(Model model) throws ContextException {
        return outputs(model);
    }

    public static Context outputs(Model model) throws ContextException {
        try {
            return model.getOutputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Object resp(Model model, String path) throws ContextException {
        return response(model, path);
    }

    public static Object response(Model model, String path) throws ContextException {
        try {
            return ((ServiceContext)model).getResponseAt(path);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context resp(Model model) throws ContextException {
        return response(model);
    }

    public static Context response(Model model, Arg... args) throws ContextException {
        try {
            return (Context) model.getResponse(args);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Entry entry(Model model, String path) throws ContextException {
        return new Entry(path, ((Context)model).asis(path));
    }

    public static Context inConn(List<Tuple2<String, ?>> entries) throws ContextException {
        MapContext map = new MapContext();
        map.direction = MapContext.Direction.IN;
        sorcer.eo.operator.populteContext(map, entries);
        return map;
    }

    public static Context inConn(Tuple2<String, ?>... entries) throws ContextException {
        MapContext map = new MapContext();
        map.direction = MapContext.Direction.IN;
        List<Tuple2<String, ?>> items = Arrays.asList(entries);
        sorcer.eo.operator.populteContext(map, items);
        return map;
    }

    public static Context outConn(List<Tuple2<String, ?>> entries) throws ContextException {
        MapContext map = new MapContext();
        map.direction = MapContext.Direction.OUT;
        sorcer.eo.operator.populteContext(map, entries);
        return map;
    }

    public static Context outConn(Tuple2<String, ?>... entries) throws ContextException {
        MapContext map = new MapContext();
        map.direction = MapContext.Direction.OUT;
        List<Tuple2<String, ?>> items = Arrays.asList(entries);
        sorcer.eo.operator.populteContext(map, items);
        return map;
    }

    public static ReturnPath returnPath(String path) {
        return  new ReturnPath<>(path);
    }

    public static Fidelity<Arg> response(String... paths) {
        return  new Fidelity(paths);
    }

    public static Paradigmatic modeling(Paradigmatic paradigm) {
        paradigm.setModeling(true);
        return paradigm;
    }

    public static Paradigmatic modeling(Paradigmatic paradigm, boolean modeling) {
        paradigm.setModeling(modeling);
        return paradigm;
    }

    public static Model mfiModel(Object... items) throws ContextException {
        List<Fidelity<Arg>> fidelities = new ArrayList<Fidelity<Arg>>();
        for (Object item : items) {
            if (item instanceof Fidelity) {
                fidelities.add((Fidelity)item);
            }
        }
        MultiFidelityMogram model = new MultiFidelityMogram();
        model.addSelectionFidelities(fidelities);
        return srvModel(items);
    }

    public static Model srvModel(Object... items) throws ContextException {
        sorcer.eo.operator.Complement complement = null;
        List<Signature> sigs = new ArrayList<Signature>();
        Fidelity<Arg> responsePaths = null;
        SrvModel model = null;
        FidelityManager fiManager = null;
        List<Fidelity<Fidelity>> metaFis = new ArrayList<Fidelity<Fidelity>>();
        List<Srv> metaFiEnts = new ArrayList<Srv>();
        for (Object item : items) {
            if (item instanceof Signature) {
                sigs.add((Signature)item);
            } else if (item instanceof sorcer.eo.operator.Complement) {
                complement = (sorcer.eo.operator.Complement)item;
            } else if (item instanceof Model) {
                model = ((SrvModel)item);
            } else if (item instanceof FidelityManager) {
                fiManager = ((FidelityManager)item);
            } else if (item instanceof Srv && ((Entry)item)._2 instanceof MultiFidelity) {
                metaFiEnts.add((Srv)item);
            } else if (item instanceof Fidelity) {
                if (((Fidelity) item).getSelects().get(0) instanceof Fidelity) {
                    metaFis.add((Fidelity<Fidelity>) item);
                } else if (((Fidelity) item).getSelects().get(0) instanceof Name) {
                    responsePaths = ((Fidelity<Arg>) item);
                }
            }
        }
        if (model == null)
            model = new SrvModel();

        if (metaFiEnts != null || metaFis != null) {
           if (fiManager == null)
               fiManager = new FidelityManager(model.getName());
        }
        if (fiManager != null) {
            model.setFiManager(fiManager);
            fiManager.init(metaFis);
            fiManager.setMogram(model);
            MultiFidelity mFi = null;
            if ((metaFiEnts.size() > 0)) {
                for (Srv metaFiEnt : metaFiEnts) {
                    mFi = (MultiFidelity) metaFiEnt._2;
                    fiManager.addFidelity(metaFiEnt._1, mFi.getFidelity());
                    mFi.setPath(metaFiEnt._1);
                    mFi.setSelection((Arg) mFi.getSelects().get(0));
                    mFi.addObserver(fiManager);
                }
            }
        }

        if (responsePaths != null) {
            model.getMogramStrategy().setResponsePaths(((Fidelity) responsePaths).getSelects());
        }
        if (complement != null) {
            model.setSubject(complement.path(), complement.value());
        }

        Object[] dest = new Object[items.length+1];
        System.arraycopy(items,  0, dest,  1, items.length);
        dest[0] = model;
        return sorcer.eo.operator.context(dest);
    }

    public static void run(sorcer.util.Runner runner, Arg... args) throws SignatureException, MogramException {
        runner.exec(args);
    }

    public static String printDeps(Mogram model) throws SortingException {
        return new SrvModelAutoDeps((SrvModel)model).printDeps();
    }

}
