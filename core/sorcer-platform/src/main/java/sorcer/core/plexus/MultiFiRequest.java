/*
* Copyright 2016 SORCERsoft.org.
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
import sorcer.core.context.ServiceContext;
import sorcer.core.context.ThrowableTrace;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.List;

/**
 * A metasystem is represented by a mogram with multiple projections of its
 * subsystems so it's a system of systems.
 *
 * Each projection of the system can be treated as a fidelity selectable
 * at runtime for a group of multiple subsystems available in the metasystem.
 * A fidelity is associated with the result of executing a its own and/or other
 * subsystems and related services. The result of a metasystem is a merged
 * service context of all contexts received foe its current fidelity.
 *
 * Created by Mike Sobolewski
 */
public class MultiFiRequest extends ServiceMogram {

    protected ServiceFidelity<Request> serviceFidelity;

    protected MorphedFidelity<Request> morphedFidelity;

    public MultiFiRequest() {
    }

    public MultiFiRequest(String name) throws SignatureException {
        super(name);
    }

    @Override
    public Mogram clearScope() throws MogramException {
        return scope.clearScope();
    }

    public MultiFiRequest(MorphedFidelity<Request> fidelity) {
        this(fidelity.getName(), fidelity);
    }

    public MultiFiRequest(String name, MorphedFidelity<Request> fidelity)  {
        super(name);
        morphedFidelity = fidelity;
        if (fiManager == null)
            fiManager = new FidelityManager(morphedFidelity.getName());

        ((FidelityManager)fiManager).init(morphedFidelity.getFidelity());
        ((FidelityManager)fiManager).setMogram(this);
        ((FidelityManager)fiManager).addFidelity(morphedFidelity.getName(), morphedFidelity.getFidelity());
        morphedFidelity.addObserver((FidelityManager)fiManager);
    }

    public MultiFiRequest(String name, ServiceFidelity<Request> fidelity) {
        super(name);
        serviceFidelity = fidelity;
    }

    public MultiFiRequest(Context context, MorphedFidelity<Request> fidelity)  {
        this(context.getName(), fidelity);
        scope = context;
    }

    public MultiFiRequest(Context context, ServiceFidelity<Request> fidelity) {
        this(context.getName(), fidelity);
        scope = context;
    }

    @Override
    public <T extends Mogram> T exert(Transaction txn, Arg... entries) throws TransactionException, MogramException, RemoteException {
        Mogram mogram = (Mogram)morphedFidelity.getSelect();
        mogram.getContext().setScope(scope);
        T out = mogram.exert(txn, entries);
        morphedFidelity.setChanged();
        morphedFidelity.notifyObservers(out);
        return out;
    }

    @Override
    public <T extends Mogram> T exert(Arg... entries) throws TransactionException, MogramException, RemoteException {
        return exert(null, entries);
    }

    @Override
    public Context getContext() throws ContextException {
//        return ((Mogram)morphedFidelity.getSelect()).getContext();
        return scope;
    }

    public void setDataContext(ServiceContext dataContext) {
        ((ServiceExertion)morphedFidelity.getSelect()).setContext(dataContext);
    }

    @Override
    public void reportException(Throwable t) {

    }

    @Override
    public ServiceFidelity selectFidelity(String selector) {
        if (serviceFidelity != null) {
            serviceFidelity.setSelect(selector);
            return serviceFidelity;
        } else {
            morphedFidelity.getFidelity().setSelect(selector);
            return morphedFidelity.getFidelity();
        }
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
    public void substitute(Arg... entries) throws SetterException {
    }

    @Override
    public Context getDataContext() throws ContextException {
        return null;
    }

    @Override
    public String describe() {
        return toString();
    }

    public ServiceFidelity<Request> getServiceFidelity() {
        return serviceFidelity;
    }

    public void setServiceFidelity(ServiceFidelity<Request> serviceFidelity) {
        this.serviceFidelity = serviceFidelity;
    }

    public MorphedFidelity getMorphedFidelity() {
        return morphedFidelity;
    }

    public void setMorphedFidelity(MorphedFidelity morphedFidelity) {
        this.morphedFidelity = morphedFidelity;
    }

    @Override
    public <T extends Mogram> T exert(T mogram, Transaction txn, Arg... args) throws TransactionException, MogramException, RemoteException {
        return null;
    }

    public <T extends Mogram> T exert(T mogram) throws TransactionException, MogramException, RemoteException {
        return null;
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
