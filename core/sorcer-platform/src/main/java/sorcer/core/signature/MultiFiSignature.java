/*
 * Copyright 2019 the original author or authors.
 * Copyright 2019 SorcerSoft.org.
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

package sorcer.core.signature;

import sorcer.core.provider.ProviderName;
import sorcer.service.*;
import sorcer.service.modeling.Functionality;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Represents a multifidelity signature.
 *
 * Created by Mike Sobolewski
 */
public class MultiFiSignature extends MultiFiSlot<String, Signature> implements Signature {

    public MultiFiSignature(Signature... signatures) {
       multiFi = new ServiceFidelity(signatures);
       multiFi.setSelect( signatures[0]);
       impl = signatures[0];
    }

    @Override
    public String getSelector() {
        return ((Signature)impl).getSelector();
    }

    @Override
    public String getPrefix() {
        return ((Signature)impl).getPrefix();

    }

    @Override
    public ProviderName getProviderName() {
        return ((Signature)impl).getProviderName();

    }

    @Override
    public Object getProvider() throws SignatureException {
        return ((Signature)impl).getProvider();
    }

    @Override
    public Functionality getVariability() {
        return ((Signature)impl).getVariability();
    }

    @Override
    public void setProviderName(ProviderName providerName) {
        ((Signature)impl).setProviderName(providerName);
    }

    @Override
    public Class getServiceType() throws SignatureException {
        return ((Signature)impl).getServiceType();
    }

    @Override
    public void setServiceType(Class serviceType) {
        ((Signature)impl).setServiceType(serviceType);
    }

    @Override
    public Multitype getMultitype() throws SignatureException {
        return ((Signature)impl).getMultitype();
    }

    @Override
    public void setMultitype(Multitype multitype) {
        ((Signature)impl).setMultitype(multitype);
    }

    @Override
    public Class[] getMatchTypes() {
        return ((Signature)impl).getMatchTypes();
    }

    public Context.Return getContextReturn() {
        return contextReturn;
    }

    @Override
    public Type getExecType() {
        return ((Signature)impl).getExecType();
    }

    @Override
    public Context getInConnector() {
        return ((Signature)impl).getInConnector();
    }

    @Override
    public Signature setType(Type type) {
        return ((Signature)impl).setType(type);
    }

    @Override
    public String getCodebase() {
        return ((Signature)impl).getCodebase();
    }

    @Override
    public void setCodebase(String urls) {
        ((Signature)impl).setCodebase(urls);
    }

    @Override
    public void close() throws RemoteException, IOException {
        ((Signature)impl).close();
    }

    @Override
    public Deployment getDeployment() {
        return ((Signature)impl).getDeployment();
    }

    @Override
    public Strategy.Access getAccessType() {
        return ((Signature)impl).getAccessType();
    }

    @Override
    public int compareTo(Object o) {
         return ((Signature)impl).compareTo(o);
    }

    @Override
    public void addDependers(Evaluation... dependers) {
        ((Signature)impl).addDependers(dependers);
    }

    @Override
    public List<Evaluation> getDependers() {
        return ((Signature)impl).getDependers();
    }

    @Override
    public Object execute(Arg... args) throws MogramException {
        if (multiFi != null) {
            try {
                List<Fidelity> fis = Arg.selectFidelities(args);
                if (fis.size() > 0) {
                    multiFi.selectSelect(fis.get(0).getName());
                }
                impl = multiFi.getSelect();
                key = ((Signature)impl).getName();
                if (((Signature)impl).getContextReturn() == null && contextReturn != null) {
                    ((Signature)impl).setContextReturn(contextReturn);
                }
                return ((Signature)impl).execute(args);
            } catch (ServiceException | ConfigurationException | RemoteException e) {
                throw new MogramException(e);
            }
        } else {
            throw new MogramException("misconfigured MultiFiSignature with multiFi: " + multiFi);
        }
    }
}
