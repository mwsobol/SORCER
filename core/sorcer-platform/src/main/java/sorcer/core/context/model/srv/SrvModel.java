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
import java.util.Arrays;
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

    public SrvModel() {
        super();
        isRevaluable = true;
        setSignature();
    }

    public SrvModel(String name)  {
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
            subjectValue = sig("service", ServiceModeler.class);
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
            if (s.getType() != Signature.Type.SRV)
                return false;
        }
        return true;
    }

    public void selectedServiceFidelity(String name) {
        Fidelity<Signature> fidelity = serviceFidelities.get(name);
        namedServiceFidelity = name;
        this.serviceFidelity = fidelity;
    }

    public void addServiceFidelity(Fidelity fidelity) {
        putServiceFidelity(fidelity.getName(), fidelity);
        namedServiceFidelity = fidelity.getName();
        this.serviceFidelity = fidelity;
    }

    public void setServiceFidelity(Fidelity fidelity) {
        this.serviceFidelity = fidelity;
        putServiceFidelity(fidelity.getName(), fidelity);
        namedServiceFidelity = fidelity.getName();
    }

    public void setServiceFidelity(String name, Fidelity fidelity) {
        this.serviceFidelity = fidelity;
        putServiceFidelity(name, fidelity);
        namedServiceFidelity = name;
    }

    public void putServiceFidelity(Fidelity fidelity) {
        if (serviceFidelities == null)
            serviceFidelities = new HashMap<String, Fidelity<Signature>>();
        serviceFidelities.put(fidelity.getName(), fidelity);
    }

    public void putServiceFidelity(String name, Fidelity fidelity) {
        if (serviceFidelities == null)
            serviceFidelities = new HashMap<String, Fidelity<Signature>>();
        serviceFidelities.put(name, fidelity);
    }

    public Signature getProcessSignature() {
        for (Signature s : serviceFidelity.getSelects()) {
            if (s.getType() == Signature.Type.SRV)
                return s;
        }
        return null;
    }

    public void selectServiceFidelity(String selector) throws ExertionException {
        if (selector != null && serviceFidelities != null
                && serviceFidelities.containsKey(selector)) {
            Fidelity sf = serviceFidelities.get(selector);

            if (sf == null)
                throw new ExertionException("no such service fidelity: " + selector + " at: " + this);
            serviceFidelity = sf;
            namedServiceFidelity = selector;
        }
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

            if (val instanceof Srv) {
                if (((Srv) val).asis() instanceof SignatureEntry) {
                    ServiceSignature sig = (ServiceSignature) ((SignatureEntry) ((Srv) val).asis()).value();
                    Context out = execSignature(sig);
                    if (sig.getReturnPath() != null) {
                        Object obj = out.getValue(sig.getReturnPath().path);
                        if (obj == null)
                            obj = out.getValue(path);
                        if (obj != null)
                            return obj;
                        else {
                            logger.warn("no value for return path: {} in: {}", sig.getReturnPath().path, out);
                            return out;
                        }
                    } else {
                        return out;
                    }
                } else {
                    if (((Srv) val).getValue() == Context.none) {
                        return getValue(((Srv) val).getName());
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

    public Context execSignature(Signature sig) throws Exception {
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

    protected void execDependencies(Signature sig, Arg... args) throws ContextException {
        Map<String, List<String>> dpm = runtime.getDependentPaths();
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
        serviceFidelity.getSelects().add(signature);
    }

    public Fidelity getServiceFidelity() {
        return serviceFidelity;
    }

    public void addSignatures(Signature... signatures) {
        if (this.serviceFidelity != null)
            this.serviceFidelity.getSelects().addAll(Arrays.asList(signatures));
        else {
            this.serviceFidelity = new Fidelity();
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
                getValue(entries);
                return this;
            }
        } catch (Exception e) {
            throw new ExertionException(e);
        }
    }

}
