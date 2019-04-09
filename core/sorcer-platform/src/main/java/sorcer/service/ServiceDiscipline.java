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
import sorcer.core.provider.Provider;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.modeling.Discipline;
import sorcer.service.modeling.Getter;

import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Date;
import java.util.List;

/**
 *  Implements a service discipline as outGovernance-multiFi-dispatch
 */
public class ServiceDiscipline implements Discipline, Getter<Service> {

    protected Uuid disciplineId;

    protected String  name;

    protected ServiceFidelity dispatchMultiFi;

    protected ServiceFidelity governanceMultiFi;

    // the input of thsi discipline
    protected Context input;

    // the output of this discipline
    protected Context output;

    protected Context inConnector;

    protected Context outConnector;

    // the executed governance
    protected Service outGovernance;

    // the executed dispatcher
    protected Mogram outDispatcher;

    protected Task precondition;

    protected Task postcondition;

    protected Signature builder;

    protected Morpher morpher;

    public ServiceDiscipline() {
        disciplineId = UuidFactory.generate();
    }

    public ServiceDiscipline(Exertion... dispatchs) {
        governanceMultiFi = new ServiceFidelity(dispatchs);
    }

    public ServiceDiscipline(Exertion dispatch, Service service) {
        governanceMultiFi = new ServiceFidelity(new Exertion[] { dispatch });
        dispatchMultiFi = new ServiceFidelity(new Service[] { service });
    }

    public ServiceDiscipline(Exertion[] dispatchs, Service[] services) {
        governanceMultiFi = new ServiceFidelity(dispatchs);
        dispatchMultiFi = new ServiceFidelity(services);
    }

    public ServiceDiscipline(List<Exertion> dispatchs, List<Service> services) {
        Exertion[] cArray = new Exertion[dispatchs.size()];
        Service[] pArray = new Exertion[services.size()];
        governanceMultiFi = new ServiceFidelity(dispatchs.toArray(cArray));
        dispatchMultiFi = new ServiceFidelity(services.toArray(pArray));
    }

    public void add(Exertion dispatch, Service service) {
        governanceMultiFi.getSelects().add(dispatch);
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
        dispatchMultiFi.getSelects().add(dispatch);
        governanceMultiFi.getSelects().add((Service)service);
    }

    @Override
    public Service getGovernance() throws MogramException {
        // if no service then dispatch is standalone
        if (governanceMultiFi == null || governanceMultiFi.getSelect() == null) {
            return (Service) dispatchMultiFi.getSelect();
        }
        return (Service) governanceMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getGovernanceMultiFi() throws MogramException {
        return governanceMultiFi;
    }

    public Service getOutGovernance() {
        return outGovernance;
    }

    @Override
    public Exertion getDispatcher() throws ExertionException {
        return (Exertion) dispatchMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getDispatcherMultiFi() {
        return dispatchMultiFi;
    }

    @Override
    public Context getInput() throws ContextException, ExertionException {
        return getDispatcher().getContext();
    }

    @Override
    public Context getOutput(Arg... args) throws ServiceException {
        if (outDispatcher == null) {
            execute(args);
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

    public Mogram getOutDispatcher() {
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
    public String describe() {
        return null;
    }

    @Override
    public Object execute(Arg... args) throws ServiceException {
        try {
            List<Fidelity> fis = Arg.selectFidelities(args);
            if (fis != null && fis.size() > 0) {
                selectFi(fis.get(0));
            }
            Exertion xrt = getDispatcher();
            if (input != null) {
                if (inConnector != null) {
                    xrt.setContext(((ServiceContext) input).updateContextWith(inConnector));
                } else {
                    xrt.setContext(input);
                }
            }
            outGovernance = getGovernance();
            xrt.dispatch(outGovernance);
            outDispatcher = xrt.exert();

            return getOutput();
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    protected void selectFi(Fidelity fi) {
        dispatchMultiFi.selectSelect(fi.getName());
        governanceMultiFi.selectSelect(fi.getPath());
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
        return outGovernance;
    }

    @Override
    public Fi getMultiFi() {
        return dispatchMultiFi;
    }

    @Override
    public FidelityManagement getFidelityManager() {
        return null;
    }

    @Override
    public FidelityManagement getRemoteFidelityManager() throws RemoteException {
        return null;
    }

    @Override
    public boolean isMonitorable() throws RemoteException {
        return false;
    }

    @Override
    public Uuid getParentId() {
        return null;
    }

    @Override
    public Date getCreationDate() {
        return null;
    }

    @Override
    public Date getGoodUntilDate() {
        return null;
    }

    @Override
    public void setGoodUntilDate(Date date) {

    }

    @Override
    public String getDomainId() {
        return null;
    }

    @Override
    public void setDomainId(String id) {

    }

    @Override
    public String getSubdomainId() {
        return null;
    }

    @Override
    public void setSubdomainId(String id) {

    }

    @Override
    public String getDomainName() {
        return null;
    }

    @Override
    public void setDomainName(String name) {

    }

    @Override
    public String getSubdomainName() {
        return null;
    }

    @Override
    public Object getEvaluatedValue(String path) throws ContextException {
        return null;
    }

    @Override
    public boolean isEvaluated() {
        return false;
    }

    @Override
    public void setSubdomainName(String name) {

    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public Date getLastUpdateDate() {
        return null;
    }

    @Override
    public void setLastUpdateDate(Date date) {

    }

    @Override
    public void setDescription(String description) {

    }

    public Morpher getMorpher() {
        return morpher;
    }

    public void setMorpher(Morpher morpher) {
        this.morpher = morpher;
    }

    @Override
    public <T extends Mogram> T exert(Transaction txn, Arg... entries) throws MogramException, RemoteException {
        return null;
    }

    @Override
    public <T extends Mogram> T exert(Arg... entries) throws MogramException, RemoteException {
        return null;
    }

    @Override
    public Uuid getId() {
        return disciplineId;
    }

    @Override
    public void setId(Uuid id) {

    }

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public void setIndex(int i) {

    }

    @Override
    public Mogram getParent() {
        return null;
    }

    @Override
    public void setParentId(Uuid parentId) {

    }

    @Override
    public Signature getProcessSignature() {
        return null;
    }

    @Override
    public Mogram deploy(List<Signature> builders) throws MogramException, ConfigurationException {
        return null;
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public void setStatus(int value) {

    }

    @Override
    public Context getContext() throws ContextException {
        return null;
    }

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public Mogram clearScope() throws MogramException {
        return null;
    }

    @Override
    public Mogram clear() throws MogramException {
        outGovernance = null;
        outDispatcher = null;
        output = null;
        return this;
    }

    @Override
    public void reportException(Throwable t) {

    }

    @Override
    public List<ThrowableTrace> getExceptions() throws RemoteException {
        return null;
    }

    @Override
    public void reportException(String message, Throwable t) {

    }

    @Override
    public void reportException(String message, Throwable t, ProviderInfo info) {

    }

    @Override
    public void reportException(String message, Throwable t, Provider provider) {

    }

    @Override
    public void reportException(String message, Throwable t, Provider provider, ProviderInfo info) {

    }

    @Override
    public List<String> getTrace() throws RemoteException {
        return null;
    }

    @Override
    public void appendTrace(String info) throws RemoteException {

    }

    @Override
    public List<ThrowableTrace> getAllExceptions() throws RemoteException {
        return null;
    }

    @Override
    public Fidelity selectFidelity(String selection) {
        return null;
    }

    @Override
    public Fidelity getSelectedFidelity() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getOwnerId() {
        return null;
    }

    @Override
    public String getSubjectId() {
        return null;
    }

    @Override
    public void setProjectName(String projectName) {

    }

    @Override
    public String getProjectName() {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public void setValid(boolean state) {

    }

    @Override
    public Context getDataContext() throws ContextException {
        return null;
    }

    @Override
    public void reconfigure(Fidelity... fidelities) throws ContextException, RemoteException {

    }

    @Override
    public void morph(String... metaFiNames) throws ContextException, RemoteException {

    }

    @Override
    public void update(Setup... contextEntries) throws ContextException, RemoteException {

    }

    @Override
    public String getProjectionFi(String projectionName) throws ContextException, RemoteException {
        return null;
    }

    @Override
    public boolean isExportControlled() {
        return false;
    }

    @Override
    public Signature getBuilder(Arg... args) throws MogramException {
        return null;
    }

    @Override
    public void applyFidelity(String name) {

    }

    @Override
    public MogramStrategy getMogramStrategy() {
        return null;
    }

    @Override
    public Object getValue(String path, Arg... args) throws ContextException, RemoteException {
        return null;
    }

    @Override
    public Object asis(String path) throws ContextException {
        return null;
    }

    @Override
    public Object asis(Path path) throws ContextException {
        return null;
    }

    @Override
    public Object putValue(String path, Object value) throws ContextException {
        return null;
    }

    @Override
    public Object putValue(Path path, Object value) throws ContextException {
        return null;
    }

    @Override
    public <T extends Mogram> T exert(T mogram, Transaction txn, Arg... entries) throws MogramException, RemoteException {
        return null;
    }

    @Override
    public Context getScope() {
        return null;
    }

    @Override
    public void setScope(Context scope) {

    }

    @Override
    public void substitute(Arg... entries) throws SetterException, RemoteException {

    }
}
