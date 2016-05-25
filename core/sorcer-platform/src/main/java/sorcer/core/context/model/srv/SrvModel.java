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

package sorcer.core.context.model.srv;

import groovy.lang.Closure;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.co.tuple.MogramEntry;
import sorcer.co.tuple.SignatureEntry;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.plexus.MorphedFidelity;
import sorcer.core.provider.rendezvous.ServiceModeler;
import sorcer.core.signature.ServiceSignature;
import sorcer.eo.operator;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Variability;
import sorcer.service.Signature.ReturnPath;

import java.rmi.RemoteException;
import java.util.*;

import static sorcer.eo.operator.*;

/**
 * A ServiceModel is a schematic description or representation of something, especially a system, 
 * phenomenon, or service, that accounts for its properties and is used to study its characteristics.
 * Properties of a service model are represented by path of Context with values that depend
 * on other properties and can be evaluated as specified by ths model. Evaluations of the service 
 * model entries of the Srv type results in exerting a dynamic federation of services as specified by
 * these entries. A rendezvous service provider orchestrating a choreography of the model
 * is a local or remote one specified by a service signature of the model.
 *   
 * Created by Mike Sobolewski on 1/29/15.
 */
public class SrvModel extends ParModel<Object> implements Model, Invocation<Object> {
    private static final Logger logger = LoggerFactory.getLogger(SrvModel.class);

    public static SrvModel instance(Signature builder) throws SignatureException {
        SrvModel model = (SrvModel) sorcer.co.operator.instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public SrvModel() {
        super();
        isRevaluable = true;
        setSignature();
    }

    public SrvModel(String name) {
        super(name);
        isRevaluable = true;
        setSignature();
    }

    public SrvModel(Signature signature) {
        super();
//        setSignature(signature.getName(), signature);
        addSignature(signature);
    }

    public SrvModel(String name, Signature signature) {
        this(name);
//        setSignature(signature.getName(), signature);
        addSignature(signature);
    }

    private void setSignature() {
        subjectPath = "service/model";
        try {
            subjectValue = sig("exert", ServiceModeler.class);
        } catch (SignatureException e) {
            // ignore it;
        }
    }

    private void setSignature(Signature signature) {
        setSignature(null, signature);
    }

    private void setSignature(String path, Signature signature) {
        if (path == null)
            subjectPath = "service/model";
        else
            subjectPath = path;
        subjectValue = signature;
    }

    public boolean isBatch() {
        for (Signature s : serviceFidelity.getSelects()) {
            if (s.getType() != Signature.Type.PROC)
                return false;
        }
        return true;
    }

    public void selectedServiceFidelity(String name) {
        ServiceFidelity<Signature> fidelity = serviceFidelities.get(name);
        serviceFidelitySelector = name;
        this.serviceFidelity = fidelity;
    }

    public void addServiceFidelity(ServiceFidelity fidelity) {
        putServiceFidelity(fidelity.getName(), fidelity);
        serviceFidelitySelector = fidelity.getName();
        this.serviceFidelity = fidelity;
    }

    public void setServiceFidelity(ServiceFidelity fidelity) {
        this.serviceFidelity = fidelity;
        putServiceFidelity(fidelity.getName(), fidelity);
        serviceFidelitySelector = fidelity.getName();
    }

    public void setServiceFidelity(String name, ServiceFidelity fidelity) {
        this.serviceFidelity = fidelity;
        putServiceFidelity(name, fidelity);
        serviceFidelitySelector = name;
    }

    public void putServiceFidelity(ServiceFidelity fidelity) {
        if (serviceFidelities == null)
            serviceFidelities = new HashMap<String, ServiceFidelity>();
        serviceFidelities.put(fidelity.getName(), fidelity);
    }

    public void putServiceFidelity(String name, ServiceFidelity fidelity) {
        if (serviceFidelities == null)
            serviceFidelities = new HashMap<String, ServiceFidelity>();
        serviceFidelities.put(name, fidelity);
    }

    public Signature getProcessSignature() {
        for (Signature s : serviceFidelity.getSelects()) {
            if (s.getType() == Signature.Type.PROC)
                return s;
        }
        return null;
    }

    public void selectServiceFidelity(String selector) throws ExertionException {
        if (selector != null && serviceFidelities != null
                && serviceFidelities.containsKey(selector)) {
            ServiceFidelity sf = serviceFidelities.get(selector);

            if (sf == null)
                throw new ExertionException("no such service fidelity: " + selector + " at: " + this);
            serviceFidelity = sf;
            serviceFidelitySelector = selector;
        }
    }

    // call from VarModels
    public Object getSrvValue(String path, Srv srv, Arg... args) throws EvaluationException {
        try {
            putValue(path, srv);
        } catch (ContextException e) {
            data.remove(path);
            return new EvaluationException(e);
        }
        Object out = getSrvValue(path, args);
        data.remove(path);
        return out;
    }

    public Object getValue(String path, Arg... args) throws EvaluationException {
           return getSrvValue(path, args);
    }

    public Object getSrvValue(String path, Arg... args) throws EvaluationException {
        Object val = null;
        try {
            append(args);
            if (path != null) {
                execDependencies(path, args);
                val = get(path);
            } else {
                ReturnPath rp = returnPath(args);
                if (rp != null)
                    val = getReturnValue(rp);
                else
                    val = super.getValue(path, args);
            }

            if (val instanceof Srv) {
                if (isChanged())
                    ((Srv) val).isValid(false);
                Object val2 = ((Srv) val).asis();
                if (val2 instanceof SignatureEntry) {
                    // return the calculated value
                    if (((Srv) val).getSrvValue() != null && ((Srv) val).isValueCurrent())
                        return ((Srv) val).getSrvValue();
                    else {
                        Signature sig = ((SignatureEntry) ((Srv) val).asis()).value();
                        return evalSignature(sig, path, args);
                    }
                } else if (val2 instanceof ServiceFidelity) {
                    Object selection = getFi((ServiceFidelity) val2, args, path);
                    if (selection instanceof Signature) {
                        return evalSignature((Signature) selection, path, args);
                    } else if (selection instanceof Evaluation) {
                        return ((Evaluation)selection).getValue(args);
                    } else {
                        return selection;
                    }
                } else if (val2 instanceof MorphedFidelity) {
                    Object obj = getFi(((MorphedFidelity) val2).getFidelity(), args, path);
                    Object out = null;
                    if (obj instanceof Signature)
                        out = evalSignature((Signature)obj, path);
                    else if (obj instanceof Entry) {
                        Arg[] nargs = Arrays.copyOf(args, args.length+1);
                        args[args.length] = this;
                        out = ((Entry) obj).getValue(nargs);
                    }
                    ((MorphedFidelity) val2).setChanged();
                    ((MorphedFidelity) val2).notifyObservers(out);
                    return out;
                } else if (val2 instanceof MogramEntry) {
                    return evalMogram((MogramEntry)val2, path, args);
                } else if (val2 instanceof ValueCallable && ((Srv) val).getType() == Variability.Type.LAMBDA) {
                    ReturnPath rp = ((Srv) val).getReturnPath();
                    Object obj = null;
                    if (rp != null && rp.inPaths != null) {
                        Context cxt = getEvaluatedSubcontext(rp.inPaths, args);
                        obj = ((ValueCallable)val2).call(cxt);
                    } else {
                        obj = ((ValueCallable) val2).call(this);
                    }
                    ((Srv) get(path)).setSrvValue(obj);
                    if (rp != null && rp.path != null)
                        putValue(((Srv) val).getReturnPath().path, obj);
                    return obj;
                }  else if (val2 instanceof Requestor && ((Srv) val).getType() == Variability.Type.LAMBDA) {
                    String entryPath = ((Entry)val).getName();
                    Object out = ((Requestor)val2).exec((Service) this.asis(entryPath), this, args);
                    ((Srv) get(path)).setSrvValue(out);
                    return out;
                } else if (val2 instanceof EntryCollable && ((Srv) val).getType() == Variability.Type.LAMBDA) {
                    Entry entry = ((EntryCollable)val2).call(this);
                    ((Srv) get(path)).setSrvValue(entry.value());
                    if (path != entry.getName())
                        putValue(entry.getName(), entry.value());
                    else if (asis(entry.getName()) instanceof Srv) {
                        ((Srv)asis(entry.getName())).setSrvValue(entry.value());
                    }
                    return entry;
                } else if (val2 instanceof Closure) {
                    Entry entry = (Entry) ((Closure)val2).call(this);
                    ((Srv) get(path)).setSrvValue(entry.value());
                    putValue(path, entry.value());
                    if (path != entry.getName())
                        putValue(entry.getName(), entry.value());
                    return entry;
                } else if (val2 instanceof ServiceInvoker) {
                    val =  ((ServiceInvoker)val2).invoke(args);
                    ((Srv) get(path)).setSrvValue(val);
                    return val;
                } else if (val2 instanceof Service && ((Srv) val).getType() == Variability.Type.LAMBDA) {
                    String entryPath = ((Entry)val).getName();
                    String[] paths = ((Srv)val).getPaths();
                    Arg[] nargs = null;
                    if (paths == null || paths.length == 0) {
                        nargs = new Arg[]{this};
                    } else {
                        nargs = new Arg[paths.length];
                        for (int i = 0; i < paths.length; i++) {
                            if (!(asis(paths[i]) instanceof Arg))
                                nargs[i] = new Entry(paths[i], asis(paths[i]));
                            else
                                nargs[i] = (Arg) asis(paths[i]);
                        }
                    }
                    Object out = ((Service)val2).exec(nargs);
                    ((Srv) get(path)).setSrvValue(out);
                    return out;
                } else {
                    if (val2 == Context.none) {
                        return getValue(((Srv) val).getName());
                    }
                }
            } else {
                return super.getValue(path, args);
            }
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
        // the same entry in entry
        if (val instanceof Entry && ((Entry) val).name().equals(path)) {
            return ((Entry) val).value();
        }
        if (val instanceof ServiceFidelity) {
            try {
                return ((Entry)((ServiceFidelity)val).getSelect()).getValue();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return val;
    }

    public Object evalSignature(Signature sig, String path, Arg... args) throws MogramException {
        Context out = execSignature(sig, args);
        if (sig.getReturnPath() != null) {
            Object obj = out.getValue(((ReturnPath)sig.getReturnPath()).path);
            if (obj == null)
                obj = out.getValue(path);
            if (obj != null) {
                ((Srv)get(path)).setSrvValue(obj);
                return obj;
            } else {
                logger.warn("no value for return path: {} in: {}", ((ReturnPath)sig.getReturnPath()).path, out);
                return out;
            }
        } else {
            return out;
        }
    }

    private Object evalMogram(MogramEntry mogramEntry, String path, Arg... entries)
            throws MogramException, RemoteException, TransactionException {
        Mogram out = mogramEntry.asis().exert(entries);
        if (out instanceof Exertion){
            Context outCxt = ((Exertion) out).getContext();
            if (outCxt.getReturnPath() != null) {
                Object obj = outCxt.getReturnValue();
                ((Srv)get(path)).setSrvValue(obj);
                return obj;
            }
            else {
                ((Srv) get(path)).setSrvValue(outCxt);
                return outCxt;
            }
        } else if (out instanceof Model) {
            Context outCxt = (Context) ((Model)out).getResponse(entries);
            append(outCxt);
            return outCxt;
        }
        return null;
    }

    protected <T extends Arg> T getFi(ServiceFidelity<T> fi, Arg[] entries, String path) throws ContextException {
        ServiceFidelity selected = null;
        FidelityList fiList = FidelityList.selectFidelities(entries);
        for (ServiceFidelity sfi : fiList) {
            if (sfi.getPath().equals(path)) {
                selected = sfi;
                ((Entry) asis(path)).isValid(false);
                isChanged();
                break;
            }
        }

        List<T> choices = fi.getSelects();
        for (T s : choices) {
            if (selected == null && fi.getSelect() != null)
                return fi.getSelect();
            else {
                String selectName = null;
                if (selected != null) {
                    selectName = selected.getName();
                } else {
                    selectName = choices.get(0).getName();
                }
                if (s.getName().equals(selectName)) {
                    fi.setSelect(s);
                    return s;
                }
            }
        }
        return null;
    }

    public Context execSignature(Signature sig, Arg... items) throws MogramException {
        execDependencies(sig, items);
        return  super.execSignature(sig, items);
    }

    protected void execDependencies(String path, Arg... args) throws ContextException {
        Map<String, List<Path>> dpm = mogramStrategy.getDependentPaths();
        List<Path> dpl = dpm.get(path);
        if (dpl != null && dpl.size() > 0) {
            for (Path p : dpl) {
                getValue(p.path, args);
            }

        }
    }

    protected void execDependencies(Signature sig, Arg... args) throws ContextException {
        execDependencies(sig.getName(), args);
    }

    /**
     * Appends a signature <code>signature</code> for this model.
     **/
    public void addSignature(Signature signature) {
        if (signature == null)
            return;
        ((ServiceSignature) signature).setOwnerId(getOwnerId());
        serviceFidelity.getSelects().add(signature);
    }

    public ServiceFidelity getServiceFidelity() {
        return serviceFidelity;
    }

    public void addSignatures(Signature... signatures) {
        if (this.serviceFidelity != null)
            this.serviceFidelity.getSelects().addAll(Arrays.asList(signatures));
        else {
            this.serviceFidelity = new ServiceFidelity();
            this.serviceFidelity.getSelects().addAll(Arrays.asList(signatures));
        }
    }

    @Override
    public Model exert(Transaction txn, Arg... entries) throws TransactionException,
            ExertionException, RemoteException {
        Signature signature = null;
        try {
            if (serviceFidelity != null) {
                signature = getProcessSignature();
            } else if (subjectValue != null && subjectValue instanceof Signature) {
                signature = (Signature)subjectValue;
            }
            if (signature != null) {
                Exertion out = operator.exertion(name, subjectValue, this).exert(txn, entries);
                Exertion xrt = out.exert();
                return xrt.getDataContext();
            } else {
                // evaluate model responses
                getResponse(entries);
                return this;
            }
        } catch (Exception e) {
            throw new ExertionException(e);
        }
    }

    public SrvModel clearOutputs() throws EvaluationException, RemoteException {
        Iterator<Map.Entry<String, Object>> i = entryIterator();
        while (i.hasNext()) {
            Map.Entry e = i.next();
            if (e.getValue() instanceof Srv) {
                ((Srv) e.getValue()).srvValue = null;
            } else if (e.getValue() instanceof Entry && ((Entry)e.getValue()).asis() instanceof Evaluation) {
                ((Entry)e.getValue()).isValid(false);
            }
        }
        return this;
    }

    public SrvModel getInoutSubcontext(String... paths) throws ContextException {
        // bare-bones subcontext
        SrvModel subcntxt = new SrvModel();
        subcntxt.setSubject(subjectPath, subjectValue);
        subcntxt.setName(getName() + "-subcontext");
        subcntxt.setDomainId(getDomainId());
        subcntxt.setSubdomainId(getSubdomainId());
        if  (paths != null && paths.length > 0) {
            for (int i = 0; i < paths.length; i++)
                subcntxt.putInoutValueAt(paths[i], getValue(paths[i]), tally + 1);
        }
        return subcntxt;
    }
}
