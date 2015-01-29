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

package sorcer.service;

import sorcer.core.context.ServiceContext;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.modeling.Model;

import java.util.HashMap;
import java.util.Map;

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
public class ServiceModel extends ServiceContext implements Model {

    // service fidelities for this exertions
    protected Map<String, ServiceFidelity> fidelities;

    protected ServiceFidelity fidelity = new ServiceFidelity();

    // the current fidelity alias, as it is named in 'fidelities'
    // its original name is different if aliasing is used for already
    // existing names 
    protected String selectedFidelitySelector;
    
    public ServiceModel() {
        super();
        isModeling = true;
    }

    public ServiceModel(String name) {
        super(name);
        isModeling = true;
    }

    public ServiceModel(Signature signature) {
        addSignature(signature);
    }

    public ServiceModel(String name, Signature signature) {
        this(name);
        addSignature(signature);
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

}
