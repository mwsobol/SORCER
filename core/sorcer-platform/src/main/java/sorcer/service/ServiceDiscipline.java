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

import net.jini.core.transaction.Transaction;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.ThrowableTrace;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.modeling.Discipline;
import sorcer.service.modeling.Getter;

import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Implements a service discipline as out-multiFi-dispatch
 */
public class ServiceDiscipline implements Discipline, Getter<Service> {

    protected Uuid disciplineId;

    protected String  name;

    protected Map<String, DisciplineFidelity> disciplineFidelities = new HashMap<>();

    protected ServiceFidelity contextMultiFi;

    protected ServiceFidelity dispatchMultiFi;

    protected ServiceFidelity governanceMultiFi;

    // the input of this discipline
    protected Context input;

    // the output of this discipline
    protected Context output;

    protected Context inConnector;

    protected Context outConnector;

    // the executed governance
    protected Service out;

    // the executed dispatcher
    protected Routine outDispatcher;

    protected Task precondition;

    protected Task postcondition;

    protected Signature builder;

    protected Morpher morpher;

    protected Discipline parent;

    // default instance new Return(Context.RETURN);
    protected Context.Return contextReturn;

    public ServiceDiscipline() {
        disciplineId = UuidFactory.generate();
    }

    public ServiceDiscipline(Routine... dispatchs) {
        governanceMultiFi = new ServiceFidelity(dispatchs);
    }

    public ServiceDiscipline(Service service, Routine dispatch) {
        governanceMultiFi = new ServiceFidelity(new Service[] { service });
        dispatchMultiFi = new ServiceFidelity(new Service[] { dispatch });
    }

    public ServiceDiscipline(Service[] services, Routine[] dispatchs) {
        governanceMultiFi = new ServiceFidelity(services);
        dispatchMultiFi = new ServiceFidelity(dispatchs);
    }

    public ServiceDiscipline(List<Service> services, List<Routine> dispatchs) {
        Routine[] cArray = new Routine[dispatchs.size()];
        Service[] pArray = new Routine[services.size()];
        governanceMultiFi = new ServiceFidelity(services.toArray(cArray));
        dispatchMultiFi = new ServiceFidelity(dispatchs.toArray(pArray));
    }

    public void add(Service service, Routine dispatch) {
        add(service, dispatch, null);
    }

    public void add(Service service, Routine dispatch, Context context) {
        governanceMultiFi.getSelects().add(service);
        dispatchMultiFi.getSelects().add(dispatch);
        if (context != null) {
            contextMultiFi.getSelects().add(context);
        }
    }

    public void add(Fidelity serviceFi, Fidelity dispatchFi) {
        add(serviceFi, dispatchFi, null);
    }
    @Override
    public void add(Fidelity serviceFi, Fidelity dispatchFi, Fidelity contextFi) {
        Routine dispatch = (Routine) dispatchFi.getSelect();
        dispatch.setName(dispatchFi.getName());
        Service service = null;
        if (serviceFi != null) {
            service = (Service)serviceFi.getSelect();
        }
        if (service instanceof Signature) {
            ((ServiceSignature)service).setName(serviceFi.getName());
        } else if (service instanceof Request) {
            ((Request)service).setName(serviceFi.getName());
        }
        dispatchMultiFi.getSelects().add(dispatch);
        governanceMultiFi.getSelects().add((Service)service);
        if (contextFi != null) {
            contextFi.getSelects().add(contextFi.getSelect());
        }
    }

    @Override
    public Service getGovernance() {
        // if no governance then dispatch is standalone
        if (governanceMultiFi == null || governanceMultiFi.returnSelect() == null) {
            return null;
        }
        return governanceMultiFi.getSelect();
    }

    @Override
    public Context getInput() throws ContextException {
        // if no contextMultiFi then return direct input
        if (contextMultiFi == null || contextMultiFi.getSelect() == null) {
            return input;
        }
        input = (Context) contextMultiFi.getSelect();
        return input;
    }

    @Override
    public ServiceFidelity getContextMultiFi() {
        return contextMultiFi;
    }

    public void setContextMultiFi(ServiceFidelity contextMultiFi) {
        this.contextMultiFi = contextMultiFi;
    }

    @Override
    public ServiceFidelity getGovernanceMultiFi() {
        return governanceMultiFi;
    }

    public Service getout() {
        return out;
    }

    @Override
    public Routine getDispatcher() {
        return (Routine) dispatchMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getDispatcherMultiFi() {
        return dispatchMultiFi;
    }

    public Context setInput(Context input) throws ContextException {
        return this.input = input;
    }

    public void setParent(Discipline parent) {
        this.parent = parent;
    }

    @Override
    public Context getOutput(Arg... args) throws ContextException {
        if (outDispatcher == null) {
            try {
                execute(args);
            } catch (ServiceException e) {
                throw new ContextException(e);
            }
        }
        Context out = null;
        if (outConnector != null) {
            if (outDispatcher instanceof Context) {
                out = ((ServiceContext) outDispatcher).updateContextWith(outConnector);
            } else if (outDispatcher instanceof Mogram) {
                if (outConnector != null)
                    out = ((ServiceContext) outDispatcher.getContext()).updateContextWith(outConnector);
            }
        } else {
            if (outDispatcher instanceof Context) {
                out = (Context) outDispatcher;
            } else if (outDispatcher instanceof Mogram) {
                out = outDispatcher.getContext();
            }
        }
        if (output == null) {
            output = out;
        } else if (out != null) {
            output.append(out);
        }

        return output;
    }

    public Routine getOutDispatcher() {
        return outDispatcher;
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
            if (out != null) {
                clear();
            }
            List<Fidelity> fis = Arg.selectFidelities(args);
            if (fis != null && fis.size() > 0) {
                try {
                    selectFi(fis.get(0));
                } catch (ConfigurationException e) {
                    throw new ServiceException(e);
                }
            }
            Routine xrt = (Routine) getDispatcher();
            Context cxt = null;
            if (contextMultiFi != null) {
                cxt = (Context) contextMultiFi.getSelect();
            }
            if (cxt != null) {
                xrt.setContext(cxt);
            }
            if (input != null) {
                if (inConnector != null) {
                    xrt.setContext(((ServiceContext) input).updateContextWith(inConnector));
                } else {
                    xrt.setContext(input);
                }
            }
            out = getGovernance();
            if (out != null) {
                xrt.dispatch(out);
            }
            outDispatcher = xrt.exert();

            return getOutput();
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    protected void selectFi(Fidelity fi) throws ConfigurationException {
        if (fi.getPath().isEmpty()) {
            DisciplineFidelity discFi = disciplineFidelities.get(fi.getName());
            dispatchMultiFi.selectSelect(discFi.getDispatcherFi().getName());
            if (governanceMultiFi != null && discFi.getGovernanceFi() != null) {
                governanceMultiFi.findSelect(discFi.getGovernanceFi().getName());
            } else {
                governanceMultiFi.setSelect(null);
            }
            if (contextMultiFi != null) {
                if (discFi.getContextFi() != null) {
                    contextMultiFi.findSelect(discFi.getContextFi().getName());
                } else {
                    contextMultiFi.setSelect(null);
                }
            }
        } else {
            dispatchMultiFi.selectSelect(fi.getPath());
            if (governanceMultiFi != null) {
                governanceMultiFi.selectSelect(fi.getName());
            }
            if (contextMultiFi != null) {
                contextMultiFi.selectSelect((String) fi.getSelect());
            }
        }
    }
    @Override
    public Context.Return getContextReturn() {
        return contextReturn;
    }

    public void setContextReturn() {
        this.contextReturn = new Context.Return();
    }

    public void setContextReturn(String returnPath) {
        this.contextReturn = new Context.Return(returnPath);
    }

    public void setContextReturn(Context.Return contextReturn) {
        this.contextReturn = contextReturn;
    }

    public void setContextReturn(String path, Signature.Direction direction) {
        contextReturn = new Context.Return(path, direction);
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
    public Service getValue(Arg... args) throws ContextException {
        return out;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Fi getMultiFi() {
        return dispatchMultiFi;
    }


    public Morpher getMorpher() {
        return morpher;
    }

    public void setMorpher(Morpher morpher) {
        this.morpher = morpher;
    }

    @Override
    public Context<Object> getContext(String path) throws ContextException, RemoteException {
        return ((Mogram)governanceMultiFi.getSelect(path)).getContext();
    }

    @Override
    public Context evaluate(Context context, Arg... args) throws EvaluationException, RemoteException {
        try {
            input.substitute(context);
            return ((ServiceDiscipline)execute(args)).getOutput();
        } catch (ServiceException e) {
            throw new EvaluationException(e);
        }
    }

    @Override
    public Context getContext() throws ContextException {
        return output;
    }

    @Override
    public void setContext(Context input) throws ContextException {
        this.input = input;
    }

    public Map<String, DisciplineFidelity> getDisciplineFidelities() {
        return disciplineFidelities;
    }

    @Override
    public Context appendContext(Context context) throws ContextException, RemoteException {
        return null;
    }

    @Override
    public Context getContext(Context contextTemplate) throws RemoteException, ContextException {
        return null;
    }

    @Override
    public Context appendContext(Context context, String path) throws ContextException, RemoteException {
        return null;
    }

    @Override
    public Object getId() {
        return disciplineId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void clear() throws MogramException {
        outDispatcher.clear();
    }
}
