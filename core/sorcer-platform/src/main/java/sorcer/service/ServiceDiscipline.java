/*
 * Copyright 2018 the original author or authors.
 * Copyright 2018 SorcerSoft.org.
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
import sorcer.service.modeling.Discipline;
import sorcer.service.modeling.Getter;

import java.rmi.RemoteException;
import java.util.List;

/**
 *  Implements a service discipline as service-fidelity-dispatch
 */
public class ServiceDiscipline implements Discipline, Getter<Service> {

    protected String  name;

    protected ServiceFidelity dispatchMultiFi;

    protected ServiceFidelity serviceMultiFi;

    protected Context input;

    protected Context output;

    protected Context inConnector;

    protected Context outConnector;

    // the executed service
    protected Service out;

    // the service context of the executed service
    protected Mogram result;

    protected Task precondition;

    protected Task postcondition;

    protected Signature builder;

    public ServiceDiscipline() {
        // do nothing
    }

    public ServiceDiscipline(Exertion... services) {
        serviceMultiFi = new ServiceFidelity(services);
    }

    public ServiceDiscipline(Exertion dispatch, Service service) {
        serviceMultiFi = new ServiceFidelity(new Exertion[] { dispatch });
        dispatchMultiFi = new ServiceFidelity(new Service[] { service });
    }

    public ServiceDiscipline(Exertion[] dispatchs, Service[] services) {
        serviceMultiFi = new ServiceFidelity(dispatchs);
        dispatchMultiFi = new ServiceFidelity(services);
    }

    public ServiceDiscipline(List<Exertion> dispatchs, List<Service> services) {
        Exertion[] cArray = new Exertion[dispatchs.size()];
        Service[] pArray = new Exertion[services.size()];
        serviceMultiFi = new ServiceFidelity(dispatchs.toArray(cArray));
        dispatchMultiFi = new ServiceFidelity(services.toArray(pArray));
    }

    public void add(Exertion dispatch, Service service) {
        serviceMultiFi.getSelects().add(dispatch);
        dispatchMultiFi.getSelects().add(service);
    }

    @Override
    public void add(Fidelity dispatchFi, Fidelity serviceFi) {
        Exertion dispatch = (Exertion) dispatchFi.getSelect();
        dispatch.setName(dispatchFi.getName());
        Object service = serviceFi.getSelect();
        if (service instanceof Signature) {
            ((ServiceSignature)service).setName(serviceFi.getName());
        } else if (service instanceof Request) {
            ((Request)service).setName(serviceFi.getName());
        }
        serviceMultiFi.getSelects().add(dispatch);
        dispatchMultiFi.getSelects().add((Service)service);
    }

    @Override
    public Service getService() throws MogramException {
        // if no service then dispatch is standalone
        if (dispatchMultiFi == null || dispatchMultiFi.getSelect() == null) {
            return  serviceMultiFi.getSelect();
        }
        return dispatchMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getDispatchMultiFi() throws MogramException {
        return dispatchMultiFi;
    }

    @Override
    public Exertion getDispatch() throws ExertionException {
        return (Exertion) serviceMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getServiceMultiFi() throws MogramException {
        return serviceMultiFi;
    }

    @Override
    public Context getInput() throws ContextException, ExertionException {
        return getDispatch().getContext();
    }

    @Override
    public Context getOutput(Arg... args) throws ServiceException {
        if (result == null || ! result.isValid()) {
            execute(args);
        }
        if (outConnector != null) {
            if (result instanceof Context) {
                return ((ServiceContext) result).updateContextWith(outConnector);
            } else if (result instanceof Mogram) {
                if (outConnector != null)
                    return ((ServiceContext) result.getContext()).updateContextWith(outConnector);
            }
        } else {
            if (result instanceof Context) {
                return (Context) result;
            } else if (result instanceof Mogram) {
                return result.getContext();
            }
        }
        return output;
    }

    public Mogram getResult() {
        return result;
    }

    @Override
    public Signature getBuilder() {
        return builder;
    }

    public void setBuilder(Signature builder) {
        this.builder = builder;
    }

    @Override
    public Object execute(Arg... args) throws ServiceException {
        try {
            List<Fidelity> fis = Arg.selectFidelities(args);
            if (fis != null && fis.size() > 0) {
                selectFi(fis.get(0));
            }
            Exertion xrt = getDispatch();
            if (input != null) {
                if (inConnector != null) {
                    xrt.setContext(((ServiceContext) input).updateContextWith(inConnector));
                } else {
                    xrt.setContext(input);
                }
            }
            xrt.setProvider(getService());
            return result = xrt.exert();
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    private void selectFi(Fidelity fi) {
        serviceMultiFi.selectSelect(fi.getName());
        dispatchMultiFi.selectSelect(fi.getPath());
    }


    public Task getPrecondition() {
        return precondition;
    }

    public void setPrecondition(Task precondition) {
        this.precondition = precondition;
    }

    public Task getPostcondition() {
        return postcondition;
    }

    public void setPostcondition(Task postcondition) {
        this.postcondition = postcondition;
    }

    @Override
    public Service get(Arg... args) throws ContextException {
        return out;
    }

    @Override
    public Fi getMultiFi() {
        return serviceMultiFi;
    }


    @Override
    public Object getId() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

}
