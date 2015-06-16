/*
* Copyright 2013 the original author or authors.
* Copyright 2013, 2014 Sorcersoft.com S.A.
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

package sorcer.core.plexus;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.core.SelectFidelity;
import sorcer.core.context.ServiceRuntime;
import sorcer.core.context.ThrowableTrace;
import sorcer.core.context.model.srv.Srv;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A system is a service (mogram) with multiple projections of its subsystems.
 * Each projection of the system can be treated as a service fidelity selectable
 * at runtime from multiple subsystems available. A fidelity is associated with
 * the result of executing a single subsystem or multiple subsystems in parallels
 * according to dependency management of its underlying subsystems. The result is
 * a merged service context of all contexts received from its fidelity subsystems.
 *
 * Created by Mike Sobolewski
 */
public class MultiFidelityService extends ServiceMogram {

    // subsystems of this system aggregated into systems via system fidelites
    protected Context<Mogram> subsystems;

    protected ServiceRuntime runtime = new ServiceRuntime((MultiFidelityService)this);

    // service fidelities for this model
    protected Map<String, Fidelity<String>> selectionFidelities;

    protected SelectFidelity selectedFidelity;

    public MultiFidelityService() {
    }

    public MultiFidelityService(String name) throws SignatureException {
        super(name);
    }

    public MultiFidelityService(String name, Signature signature) throws SignatureException {
        super(name, signature);
    }

    @Override
    public <T extends Mogram> T exert(Transaction txn, Arg... entries) throws TransactionException, MogramException, RemoteException {
        return (T) runtime.exert(txn, entries);
    }

    @Override
    public <T extends Mogram> T exert(Arg... entries) throws TransactionException, MogramException, RemoteException {
        return (T) runtime.exert(entries);
    }

    @Override
    public Mogram clearScope() throws MogramException {
        return subsystems.clearScope();
    }

    @Override
    public List<ThrowableTrace> getExceptions() {
        return runtime.getExceptions();
    }

    @Override
    public List<String> getTrace() {
        return runtime.getTraceList();
    }

    @Override
    public List<ThrowableTrace> getAllExceptions() {
        return runtime.getAllExceptions();
    }

    @Override
    public boolean isMonitorable() {
        return runtime.isMonitorable();
    }

    @Override
    public Mogram substitute(Arg... entries) throws SetterException {
        return null;
    }

    /**
     * Returns the current values of atributes (fidelites) of a given projection.
     *
     * @return the projection values of this evaluation
     * @throws EvaluationException
     * @throws RemoteException
     */
    public MultiFidelityService setProjection(String... fidelities) throws EvaluationException, RemoteException {
        for (String path : fidelities)
            runtime.getResponsePaths().add(path);
        return this;
    }

    public void setSelectionFidelities(String fidelityName) {
        Fidelity fi = selectionFidelities.get(fidelityName);
        runtime.setCurrentSelector(fi.getName());
    }

    public void setSelectionFidelities(Fidelity<String> fidelity) {
        selectionFidelities.put(fidelity.getName(), fidelity);
        runtime.setCurrentSelector(fidelity.getName());
    }

    public Fidelity<String> getSelectionFidelity() {

        return selectionFidelities.get(runtime.getCurrentSelector());
    }

    public void addSelectionFidelities(List<Fidelity<String>> fidelities) {
        for (Fidelity<String> fi : fidelities)
            addSelectionFidelity(fi);
    }

    public void addSelectionFidelities(Fidelity<String>... fidelities) {
        for (Fidelity<String> fi : fidelities)
            addSelectionFidelity(fi);
    }

    public void addSelectionFidelity(Fidelity<String> fidelity) {
        if (selectionFidelities == null)
            selectionFidelities = new HashMap<String, Fidelity<String>>();
        selectionFidelities.put(fidelity.getName(), fidelity);
    }

    public void selectSelectionFidelity(String fidelity) throws ExertionException {
        if (fidelity != null && selectionFidelities != null
                && selectionFidelities.containsKey(fidelity)) {
            runtime.setCurrentSelector(selectionFidelities.get(fidelity).getName());
            runtime.getResponsePaths().clear();
            runtime.getResponsePaths().add(runtime.getCurrentSelector());
        }
    }


    public Mogram putValue(final String path, Object value) throws ContextException {
        if (path == null)
            throw new IllegalArgumentException("path must not be null");
        if (value instanceof Srv) {
            subsystems.putValue(path, value);
        } else {
            throw new IllegalArgumentException("value must Service entry of Srv type");
        }
        return (Mogram) value;
    }

    public Context getValue(Arg... entries) throws EvaluationException {
        try {
            Mogram mogram = subsystems.getValue(runtime.getResponsePaths().get(0));
            mogram = mogram.exert(entries);
            if (mogram instanceof Exertion)
                return ((Exertion) mogram).getContext();
            else
                return runtime.getOutcome();
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }

    public Object getResponse(String fidelity, Arg... entries) throws ContextException {
        try {
            selectSelectionFidelity(fidelity);
        } catch (ExertionException e) {
            throw new ContextException(e);
        }
        return getValue(entries);
    }

    public SelectFidelity getSelectedSelectionFidelity() {
        return selectedFidelity;
    }

    public void setSelectedSelectionFidelity(SelectFidelity selectedFidelity) {
        this.selectedFidelity = selectedFidelity;
    }

    @Override
    public <T extends Mogram> T service(T mogram, Transaction txn) throws TransactionException, MogramException, RemoteException {
        return null;
    }

    @Override
    public <T extends Mogram> T service(T mogram) throws TransactionException, MogramException, RemoteException {
        return null;
    }

    public Context<Mogram> getSubsystems() {
        return subsystems;
    }

    public void setSubsystems(Context<Mogram> subsystems) {
        this.subsystems = subsystems;
    }


}
