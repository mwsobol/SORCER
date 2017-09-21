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
import sorcer.core.context.model.ent.Ref;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A metasystem is represented by a mogram with multiple projections of its
 * subsystems so it's a system of systems.
 *
 * Each projection of the system can be treated as a fidelity selectable
 * at runtime for a group of multiple subsystems available in the metasystem.
 * A fidelity is associated with the result of executing its own and/or other
 * subsystems and related services. The result of a metasystem is a merged
 * service context of all contexts received from the executed fidelity.
 *
 * Created by Mike Sobolewski
 */
public class MultiFiMogram extends ServiceMogram implements Fi<Activity> {

    protected Fidelity requestFidelity;

    protected MorphFidelity morphFidelity;

    protected String path = "";

    public MultiFiMogram() {
    }

    public MultiFiMogram(String name) throws SignatureException {
        super(name);
    }

    @Override
    public Mogram clearScope() throws MogramException {
        return scope.clearScope();
    }

    public MultiFiMogram(ServiceFidelity fidelity) {
        this(fidelity.getName(), fidelity);
    }

    public MultiFiMogram(String name, MorphFidelity fidelity)  {
        super(name);
        morphFidelity = fidelity;
        if (fiManager == null)
            fiManager = new FidelityManager(name);

        ((FidelityManager)fiManager).add((MetaFi) morphFidelity.getFidelity());
        ((FidelityManager)fiManager).setMogram(this);
        ((FidelityManager)fiManager).addMorphedFidelity(morphFidelity.getName(), morphFidelity);
        ((FidelityManager)fiManager).addFidelity(morphFidelity.getName(), morphFidelity.getFidelity());
        morphFidelity.addObserver((FidelityManager)fiManager);
    }

    public MultiFiMogram(String name, Metafidelity fidelity) {
        super(name);
        requestFidelity = fidelity;
    }

    public MultiFiMogram(String name, ServiceFidelity fidelity) {
        super(name);
        requestFidelity = fidelity;
    }

    public MultiFiMogram(Context context, MorphFidelity fidelity)  {
        this(context.getName(), fidelity);
        scope = context;
    }

    public MultiFiMogram(Context context, Metafidelity fidelity) {
        this(context.getName(), fidelity);
        scope = context;
    }

    @Override
    public <T extends Mogram> T exert(Transaction txn, Arg... entries) throws TransactionException, MogramException, RemoteException {
        Mogram mogram = (Mogram) morphFidelity.getSelect();
        mogram.getContext().setScope(scope);
        T out = mogram.exert(txn, entries);
        morphFidelity.setChanged();
        morphFidelity.notifyObservers(out);
        return out;
    }

    @Override
    public <T extends Mogram> T exert(Arg... entries) throws TransactionException, MogramException, RemoteException {
        return exert(null, entries);
    }

    @Override
    public Context getContext() throws ContextException {
//        return ((Mogram)morphFidelity.getSelect()).getContext();
        return scope;
    }

    public void setDataContext(ServiceContext dataContext) {
        ((ServiceExertion) morphFidelity.getSelect()).setContext(dataContext);
    }

    @Override
    public void reportException(Throwable t) {

    }

    @Override
    public Fidelity selectFidelity(String selector) {
        if (requestFidelity != null) {
            requestFidelity.setSelect(selector);
            return requestFidelity;
        } else {
            morphFidelity.getFidelity().setSelect(selector);
            return morphFidelity.getFidelity();
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

    public Fidelity getServiceFidelity() {
        if (requestFidelity == null && morphFidelity != null)
            return morphFidelity.getFidelity();
        else {
            return requestFidelity;
        }
    }

    public void setServiceFidelity(Metafidelity requestFidelity) {
        this.requestFidelity = requestFidelity;
    }

    public MorphFidelity getMorphFidelity() {
        return morphFidelity;
    }

    public void setMorphFidelity(MorphFidelity morphFidelity) {
        this.morphFidelity = morphFidelity;
    }

    @Override
    public <T extends Mogram> T exert(T mogram, Transaction txn, Arg... args) throws TransactionException, MogramException, RemoteException {
        return null;
    }

    public <T extends Mogram> T exert(T mogram) throws TransactionException, MogramException, RemoteException {
        return null;
    }

    public void setUnifiedName(String name) throws RemoteException {
        this.name = name;
        ((FidelityManager)fiManager).setName(name);
        Map<String, ServiceFidelity> fiMap = fiManager.getFidelities();
        Set<String> fiSet = fiMap.keySet();
        if (fiSet.size() == 1) {
            Iterator<String> i = fiSet.iterator();
            String sFiName = i.next();
            fiManager.getFidelities();
            ServiceFidelity sf = fiMap.get(sFiName);
            sf.setName(name);
            fiMap.put(name, sf);
            fiMap.remove(sFiName);
        }
    }

    @Override
    public void appendTrace(String info) throws RemoteException {
        fiManager.getMogram().appendTrace(info);
    }

    @Override
    public Object execute(Arg... entries) throws MogramException, RemoteException {
        return null;
    }

    @Override
    public Object get(String component) {
        return requestFidelity.getSelect(component);
    }

    private Fi getMultifidelity() {
        if (morphFidelity != null) {
            return morphFidelity;
        } else if (requestFidelity != null) {
            return requestFidelity;
        } else {
            return null;
        }

    }
    @Override
    public String getPath() {
        return getMultifidelity().getPath();
    }

    @Override
    public void setPath(String path) {

    }

    @Override
    public void setSelect(Activity select) {

    }

    @Override
    public Activity getSelect() {
        Activity req = null;
        Object select = getMultifidelity().getSelect();
        if (select instanceof Ref) {
            try {
                req=  (Activity) ((Ref) getMultifidelity().getSelect()).getValue();
            } catch (EvaluationException | RemoteException e) {
                e.printStackTrace();
            }
        } else{
            req = (Activity) getMultifidelity().getSelect();
        }
        return req;
    }

    @Override
    public void setSelect(String name) {
        getMultifidelity().setSelect(name);
    }

    @Override
    public void addSelect(Activity fidelity) {
        getMultifidelity().addSelect(fidelity);
    }

    @Override
    public List getSelects() {
        return  getMultifidelity().getSelects();
    }
}
