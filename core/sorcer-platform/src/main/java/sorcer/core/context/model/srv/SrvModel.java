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

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.co.tuple.SignatureEntry;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.provider.rendezvous.ServiceModeler;
import sorcer.core.signature.ServiceSignature;
import sorcer.eo.operator;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sorcer.eo.operator.*;

/**
 * A ServiceModel is a schematic description or representation of something, especially a system, 
 * phenomenon, or service, that accounts for its properties and is used to study its characteristics.
 * Properties of a service model are represented by path of ServiceContext with values that depend
 * on other properties and can be evaluated as specified by ths model. Evaluations of the service 
 * model is the result of exerting a dynamic federation of services as specified by entries 
 * (variables) of the model. A rendezvous service provider orchestrating a choreography of the model
 * is a local or remote Modeler specified by a service signature of the model.
 *   
 * Created by Mike Sobolewski on 1/29/15.
 */
public class SrvModel extends ParModel<Object> implements Model {

    // service fidelities for this model
    protected Map<String, ServiceFidelity> fidelities;

    protected ServiceFidelity fidelity = new ServiceFidelity();

    // the current fidelity alias, as it is named in 'fidelities'
    // its original name is different if aliasing is used for already
    // existing names 
    protected String selectedFidelitySelector;

    private ParModel modelScope = new ParModel();

    public SrvModel() throws SignatureException {
        super();
        isModeling = true;
        addSignature(sig("service", ServiceModeler.class));
    }

    public SrvModel(String name) throws SignatureException {
        super(name);
        isModeling = true;
        subjectPath = "service/model";
        addSignature(sig("service", ServiceModeler.class));
    }

    public SrvModel(Signature signature) {
        super();
        getFidelity().clear();
        addSignature(signature);
    }

    public SrvModel(String name, Signature signature) throws SignatureException {
        this(name);
        getFidelity().clear();
        addSignature(signature);
    }

    public Object getValue(String path, Arg... entries) throws ContextException {
        Object val = null;
        try {
            append(entries);

            if (path != null) {
                val = get(path);
            } else {
                Signature.ReturnPath rp = returnPath(entries);
                if (rp != null)
                    val = getReturnValue(rp);
                else
                    val = super.getValue(path, entries);
            }

            if (val instanceof SrvEntry) {
                if (((SrvEntry) val).asis() instanceof SignatureEntry) {
                    ServiceSignature sig = (ServiceSignature) ((SignatureEntry) ((SrvEntry) val).asis()).value();
                    Context out = execSignature(sig);
                    if (sig.getReturnPath() != null && sig.getReturnPath().path != null) {
                        return getValue(sig.getReturnPath().path);
                    } else {
                        return out;
                    }
                } else {
                    if (((SrvEntry) val).getValue() == Context.none) {
                        return getValue(((SrvEntry) val).getName());
                    }
                }
            } else {
                return super.getValue(path, entries);
            }
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
        return val;
    }

    private Context execSignature(Signature sig) throws Exception {
        String[] ips = sig.getReturnPath().inPaths;
        String[] ops = sig.getReturnPath().outPaths;
        execDependencies(sig);
        Context incxt = this;
        if (ips != null && ips.length > 0) {
            incxt = this.getEvaluatedSubcontext(ips);
        }
        if (sig.getReturnPath() != null) {
            incxt.setReturnPath(sig.getReturnPath());
        }
        Context outcxt = ((Task) task(sig, incxt).exert()).getContext();
        if (ops != null && ops.length > 0) {
            outcxt = outcxt.getSubcontext(ops);
        }
        this.appendInout(outcxt);
        return outcxt;
    }

    private void execDependencies(Signature sig, Arg... args) throws ContextException {
        Map<String, List<String>> dpm = getDependentPaths();
        List<String> dpl = dpm.get(sig.getName());
        if (dpl != null && dpl.size() > 0) {
            for (String p : dpl) {
                getValue(p, args);
            }
        }
    }

    /**
     * Appends a signature <code>signature</code> for this model.
     **/
    public void addSignature(Signature signature) {
        if (signature == null)
            return;
        ((ServiceSignature) signature).setOwnerId(getOwnerId());
        fidelity.add(signature);
    }

    public ServiceFidelity getFidelity() {
        return fidelity;
    }

    public void addSignatures(ServiceFidelity signatures) {
        if (this.fidelity != null)
            this.fidelity.addAll(signatures);
        else {
            this.fidelity = new ServiceFidelity();
            this.fidelity.addAll(signatures);
        }
    }

    public boolean isBatch() {
        for (Signature s : fidelity) {
            if (s.getType() != Signature.Type.SRV)
                return false;
        }
        return true;
    }

    public void setFidelity(ServiceFidelity fidelity) {
        this.fidelity = fidelity;
    }

    public void putFidelity(ServiceFidelity fidelity) {
        if (fidelities == null)
            fidelities = new HashMap<String, ServiceFidelity>();
        fidelities.put(fidelity.getName(), fidelity);
    }

    public void addFidelity(ServiceFidelity fidelity) {
        putFidelity(fidelity.getName(), fidelity);
        selectedFidelitySelector = name;
        this.fidelity = fidelity;
    }

    public void setFidelity(String name, ServiceFidelity fidelity) {
        this.fidelity = new ServiceFidelity(name, fidelity);
        putFidelity(name, fidelity);
        selectedFidelitySelector = name;
    }

    public void putFidelity(String name, ServiceFidelity fidelity) {
        if (fidelities == null)
            fidelities = new HashMap<String, ServiceFidelity>();
        fidelities.put(name, new ServiceFidelity(name, fidelity));
    }

    public void addFidelity(String name, ServiceFidelity fidelity) {
        ServiceFidelity nf = new ServiceFidelity(name, fidelity);
        putFidelity(name, nf);
        selectedFidelitySelector = name;
        fidelity = nf;
    }

    public Signature getProcessSignature() {
        for (Signature s : fidelity) {
            if (s.getType() == Signature.Type.SRV)
                return s;
        }
        return null;
    }
    
    public void selectFidelity(String selector) throws ExertionException {
        if (selector != null && fidelities != null
                && fidelities.containsKey(selector)) {
            ServiceFidelity sf = fidelities.get(selector);

            if (sf == null)
                throw new ExertionException("no such service fidelity: " + selector + " at: " + this);
            fidelity = sf;
            selectedFidelitySelector = selector;
        }
    }

    @Override
    public Model exert(Transaction txn, Arg... entries) throws TransactionException,
            ExertionException, RemoteException {
        Signature signature = null;
        try {
            if (fidelity != null) {
                signature = getProcessSignature();
                Exertion out = operator.exertion(name, signature, this).exert(txn, entries);
                Exertion xrt = out.exert();
                return xrt.getDataContext();
            } else {
                // evaluate model responses
                getValue(entries);
                return this;
            }
        } catch (Exception e) {
            throw new ExertionException(e);
        }
    }

}
