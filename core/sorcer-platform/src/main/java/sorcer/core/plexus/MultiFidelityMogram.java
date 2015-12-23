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
import sorcer.core.context.ThrowableTrace;
import sorcer.core.context.model.par.ParModel;
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
public class MultiFidelityMogram extends ServiceMogram {

    // subsystems of this system aggregated into systems via system fidelites
    protected Context<Mogram> subsystems = new ParModel<Mogram>();

    // service fidelities for this model
    protected Map<String, Fidelity<Arg>> selectionFidelities;

    protected Fidelity<Arg> selectedFidelity;

    public MultiFidelityMogram() {
    }

    public MultiFidelityMogram(String name) throws SignatureException {
        super(name);
    }

    public MultiFidelityMogram(String name, Signature signature) throws SignatureException {
        super(name, signature);
    }

    @Override
    public <T extends Mogram> T exert(Transaction txn, Arg... entries) throws TransactionException, MogramException, RemoteException {
        return (T) fiManager.getMogram().exert(txn, entries);
    }

    @Override
    public <T extends Mogram> T exert(Arg... entries) throws TransactionException, MogramException, RemoteException {
        return (T) fiManager.getMogram().exert(entries);
    }

    @Override
    public Context getContext() throws ContextException {
        return subsystems;
    }

    @Override
    public Mogram clearScope() throws MogramException {
        return subsystems.clearScope();
    }

    @Override
    public void reportException(Throwable t) {

    }

    @Override
    public List<ThrowableTrace> getExceptions() throws RemoteException {
        return fiManager.getMogram().getExceptions();
    }

    @Override
    public List<String> getTrace() throws RemoteException {
        return fiManager.getMogram().getTrace();
    }

    @Override
    public List<ThrowableTrace> getAllExceptions() throws RemoteException {
        return fiManager.getMogram().getAllExceptions();
    }

    @Override
    public boolean isMonitorable() throws RemoteException {
        return fiManager.getMogram().isMonitorable();
    }

    @Override
    public Mogram substitute(Arg... entries) throws SetterException {
        return null;
    }

    @Override
    public Context getDataContext() throws ContextException {
        return null;
    }

    @Override
    public String describe() {
        return toString();
    }

    /**
     * Returns the current values of atributes (fidelites) of a given projection.
     *
     * @return the projection values of this evaluation
     * @throws EvaluationException
     * @throws RemoteException
     */
//    public MultiFidelityService setProjection(String... fidelities) throws EvaluationException, RemoteException {
//        for (String path : fidelities)
//            fidelityManager.runtime.getResponsePaths().add(path);
//        return this;
//    }
//
//    public void setSelectionFidelities(String fidelityName) {
//        Fidelity fi = selectionFidelities.get(fidelityName);
//        fidelityManager.runtime.setCurrentSelector(fi.getName());
//    }
//
//    public void setSelectionFidelities(Fidelity<String> fidelity) {
//        selectionFidelities.put(fidelity.getName(), fidelity);
//        fidelityManager.runtime.setCurrentSelector(fidelity.getName());
//    }
//
//    public Fidelity<String> getSelectionFidelity() {
//        return selectionFidelities.get(fidelityManager.runtime.getCurrentSelector());
//    }

    public void addSelectionFidelities(List<Fidelity<Arg>> fidelities) {
        for (Fidelity<Arg> fi : fidelities)
            addSelectionFidelity(fi);
    }

    public void addSelectionFidelities(Fidelity<Arg>... fidelities) {
        for (Fidelity<Arg> fi : fidelities)
            addSelectionFidelity(fi);
    }

    public void addSelectionFidelity(Fidelity<Arg> fidelity) {
        if (selectionFidelities == null)
            selectionFidelities = new HashMap<String, Fidelity<Arg>>();
        selectionFidelities.put(fidelity.getName(), fidelity);
    }

//    public void selectSelectionFidelity(String fidelity) throws ExertionException {
//        if (fidelity != null && selectionFidelities != null
//                && selectionFidelities.containsKey(fidelity)) {
//            fidelityManager.runtime.setCurrentSelector(selectionFidelities.get(fidelity).getName());
//            fidelityManager.runtime.getResponsePaths().clear();
//            fidelityManager.runtime.getResponsePaths().add(fidelityManager.runtime.getCurrentSelector());
//        }
//    }

    public Mogram put(Mogram mogram) throws ContextException {
        subsystems.putValue(mogram.getName(), mogram);
        return mogram;
    }

    public Mogram put(final String path, Mogram value) throws ContextException {
        if (path == null)
            throw new IllegalArgumentException("path must not be null");

        subsystems.putValue(path, value);
        return (Mogram) value;
    }

//    public Context getValue(Arg... entries) throws EvaluationException {
//        try {
//            Mogram mogram = subsystems.getValue(fidelityManager.runtime.getResponsePaths().get(0));
//            mogram = mogram.exert(entries);
//            if (mogram instanceof Exertion)
//                return ((Exertion) mogram).getContext();
//            else
//                return fidelityManager.runtime.getOutcome();
//        } catch (Exception e) {
//            throw new EvaluationException(e);
//        }
//    }
//
//    public Object getResponse(String fidelity, Arg... entries) throws ContextException {
//        try {
//            selectSelectionFidelity(fidelity);
//        } catch (ExertionException e) {
//            throw new ContextException(e);
//        }
//        return getValue(entries);
//    }

    public Fidelity<Arg> getSelectedSelectionFidelity() {
        return selectedFidelity;
    }

    public void setSelectedSelectionFidelity(Fidelity<Arg> selectedFidelity) {
        this.selectedFidelity = selectedFidelity;
    }

    @Override
    public <T extends Mogram> T exert(T mogram, Transaction txn, Arg... args) throws TransactionException, MogramException, RemoteException {
        return null;
    }

    public <T extends Mogram> T exert(T mogram) throws TransactionException, MogramException, RemoteException {
        return null;
    }

    public Context<Mogram> getSubsystems() {
        return subsystems;
    }

    public void setSubsystems(Context<Mogram> subsystems) {
        this.subsystems = subsystems;
    }


    @Override
    public void appendTrace(String info) throws RemoteException {
        fiManager.getMogram().appendTrace(info);
    }

    @Override
    public Object exec(Arg... entries) throws MogramException, RemoteException {
        return null;
    }
}
