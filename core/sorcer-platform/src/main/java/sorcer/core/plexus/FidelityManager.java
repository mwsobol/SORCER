/*
* Copyright 2015 SORCERsoft.org.
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

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.invoker.Observable;
import sorcer.core.invoker.Observer;
import sorcer.service.*;

import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static sorcer.eo.operator.fi;

/**
 * Created by Mike Sobolewski on 6/14/15.
 */
public class FidelityManager<T extends Arg> implements FidelityManagement<T>, Observer, Identifiable {

    // sequence number for unnamed instances
    protected static int count = 0;

    private String name;

    Uuid id = UuidFactory.generate();

    // fidelities for signatures and other selection of T
    protected Map<String, ServiceFidelity<T>> fidelities = new ConcurrentHashMap<>();

    // fidelities for fidelites
    protected Map<String, ServiceFidelity<ServiceFidelity>> metafidelities = new ConcurrentHashMap<>();

    // fidelities for signatures and other selection of T
    protected Map<String, MorphFidelity> morphFidelities = new ConcurrentHashMap<>();

    // changed fidelities by morphers
    protected List<ServiceFidelity> fiTrace = new ArrayList();

    protected Mogram mogram;

    protected boolean isTraced = false;

    protected Map<Long, Session> sessions;


    public FidelityManager() {
        name = "fiManager" +  count++;
    }

    public FidelityManager(String name) {
        this.name = name;
    }

    public FidelityManager(Mogram mogram) {
        this(mogram.getName());
        this.mogram = mogram;
    }

    public Map<String, ServiceFidelity<ServiceFidelity>> getMetafidelities() {
        return metafidelities;
    }

    public void setMetafidelities(Map<String, ServiceFidelity<ServiceFidelity>> metafidelities) {
        this.metafidelities = metafidelities;
    }

    @Override
    public Map<String, ServiceFidelity<T>> getFidelities() {
        return fidelities;
    }


    public Map<String, MorphFidelity> getMorphFidelities() {
        return morphFidelities;
    }

    public void setMorphFidelities(Map<String, MorphFidelity> morphFidelities) {
        this.morphFidelities = morphFidelities;
    }

    public void setFidelities(Map<String, ServiceFidelity<T>> fidelities) {
        this.fidelities = fidelities;
    }

    public void addFidelity(String path, ServiceFidelity<T> fi) {
        if (fi != null)
            this.fidelities.put(path, fi);
    }

    public void addMorphedFidelity(String path, MorphFidelity mFi) {
        if (mFi != null)
            this.morphFidelities.put(path, mFi);
    }

    public List<ServiceFidelity> getFiTrace() {
        return fiTrace;
    }

    public void setFiTrace(List<ServiceFidelity> fiTrace) {
        this.fiTrace = fiTrace;
    }

    public void addTrace(ServiceFidelity fi) {
        if (fi != null)
            this.fiTrace.add(fi);
    }

    public void addMetafidelity(String path, ServiceFidelity<ServiceFidelity> fi) {
        this.metafidelities.put(path, fi);
    }

    public Mogram getMogram() {
        return mogram;
    }

    public void setMogram(Mogram mogram) {
        this.mogram = mogram;
    }

    public <M extends Mogram> M exert(M mogram, Transaction txn, Arg... args) throws TransactionException, MogramException, RemoteException {
        this.mogram = mogram;
        return (M) mogram.exert(txn);
    }

    public <T extends Mogram> T exert(T mogram) throws TransactionException, MogramException, RemoteException {
        return exert(mogram, null);
    }

    public void initialize() {
       // implement is subclasses
    }

    public void init(ServiceFidelity<ServiceFidelity> fidelity) {
        put(fidelity.getName(), fidelity);
    }

    public void init(List<ServiceFidelity<ServiceFidelity>> fidelities) {
        if (fidelities == null || fidelities.size() == 0) {
            initialize();
            return;
        }
        for (ServiceFidelity fi : fidelities) {
            put(fi.getName(), fi);
        }
    }

    public EventRegistration register(long eventID, MarshalledObject handback,
                                      RemoteEventListener toInform, long leaseLenght)
            throws UnknownEventException, RemoteException {
        return registerModel(eventID, handback, toInform, leaseLenght);
    }

    public EventRegistration registerModel(long eventID, MarshalledObject handback,
                                           RemoteEventListener toInform, long leaseLenght)
            throws UnknownEventException, RemoteException {
        if (sessions == null) {
            sessions = new HashMap<Long, Session>();
        }
        String source = getClass().getName() + "-" + UUID.randomUUID();
        Session session = new Session(eventID, source, handback, toInform,
                leaseLenght);
        sessions.put(eventID, session);
        EventRegistration er = new EventRegistration(eventID, source, null,
                session.seqNum);
        return er;
    }

    public void deregister(long eventID) throws UnknownEventException,
            RemoteException {
        if (sessions.containsKey(eventID)) {
            sessions.remove(eventID);
        } else
            throw new UnknownEventException("No registration for eventID: "
                    + eventID);
    }

    public Map<Long, Session> getSessions() {
        return sessions;
    }

    public void morph(String... fiNames) {
        for (String fiName : fiNames) {
            ServiceFidelity mFi = metafidelities.get(fiName);
            List<ServiceFidelity> fis = mFi.getSelects();
            String name = null;
            String path = null;
            if (isTraced) {
                fiTrace.add(new ServiceFidelity(fiName, fis));
            }
            for (ServiceFidelity fi : fis) {
                name = fi.getName();
                path = fi.getPath();
                Iterator<Map.Entry<String, ServiceFidelity<T>>> i = fidelities.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry<String, ServiceFidelity<T>> fiEnt = i.next();
                    if (fiEnt.getKey().equals(path)) {
                        if (morphFidelities.get(path) != null) {
                            morphFidelities.get(path).setMorpherSelect(name);
                        }
                        fiEnt.getValue().setSelect(name);
                    }
                }
            }
        }
    }

    public FidelityList resetFidelities() throws ContextException, RemoteException {
        FidelityList fl = new FidelityList();
        Collection<ServiceFidelity<T>> fc = fidelities.values();
        for (ServiceFidelity sf : fc) {
            sf.setSelect(sf.get(0));
            fl.add(fi(sf.getPath(), sf.getSelect().getName()));
        }
        return fl;
    }

    public FidelityList getCurrentFidelities() throws ContextException, RemoteException {
        FidelityList fl = new FidelityList();
		Iterator<Map.Entry<String, ServiceFidelity<T>>> it = fidelities.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, ServiceFidelity<T>> me = it.next();
			fl.add(fi(me.getKey(), me.getValue().getSelect().getName()));
		}
        return fl;
    }

    public FidelityList getDefaultFidelities() throws ContextException, RemoteException {
        FidelityList fl = new FidelityList();
		Iterator<Map.Entry<String, ServiceFidelity<T>>> it = fidelities.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, ServiceFidelity<T>> me = it.next();
			fl.add(fi(me.getKey(), me.getValue().get(0).getName()));
		}
        return fl;
    }

    @Override
    public void reconfigure(String... fiNames) throws RemoteException {
        // applies to MultiFiRequests
        if (fidelities.size() == 1 && fiNames.length == 1) {
            ServiceFidelity fi = fidelities.get(name);
            if (fi == null) {
                fi = selectFidelity(fiNames[0]);
            }
            fi.setSelect(fiNames[0]);
            if (isTraced) {
                ServiceFidelity nsf = new ServiceFidelity(fiNames[0]);
                nsf.setPath(name);
                fiTrace.add(nsf);
            }
        }
    }


    private ServiceFidelity selectFidelity(String name) {
        ServiceFidelity fi = fidelities.get(name);
        if (fi == null) {
            ServiceFidelity sf = fidelities.get(0);
            List<ServiceFidelity> sfl = sf.getSelects();
            for (ServiceFidelity f : sfl) {
                if (f.getName().equals(name))
                    return sf;
            }
        }
        return null;
    }

    @Override
    public void reconfigure(ServiceFidelity... fidelities) throws ContextException, RemoteException {
        if (fidelities == null || fidelities.length == 0) {
            return;
        }
        if (this.fidelities.size() > 0) {
            for (ServiceFidelity fi : fidelities) {
                ServiceFidelity sFi = this.fidelities.get(fi.getPath());
                if (sFi != null) {
                    sFi.setSelect(fi.getName());
                }
                if (isTraced)
                    fiTrace.add(fi);
            }
        }
    }

    public void add(ServiceFidelity<ServiceFidelity>... sysFis) {
        for (ServiceFidelity<ServiceFidelity> sysFi : sysFis){
            metafidelities.put(sysFi.getName(), sysFi);
        }
    }

    public void put(String sysFiName, ServiceFidelity<ServiceFidelity> sysFi) {
        metafidelities.put(sysFiName, sysFi);
    }

    public void put(Entry<ServiceFidelity<ServiceFidelity>>... entries) {
        try {
            for(Entry<ServiceFidelity<ServiceFidelity>> e : entries) {
                metafidelities.put(e.getName(), e.getValue());
            }
        } catch (EvaluationException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean isTraced() {
        return isTraced;
    }

    public void setTraced(boolean traced) {
        isTraced = traced;
    }


    @Override
    public Uuid getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void update(Observable observable, Object obj) throws EvaluationException, RemoteException {
        // implement in subclasses and use the morphers provided by MorphedFidelities (observables)

        MorphFidelity mFi = (MorphFidelity)observable;
        Morpher morpher = mFi.getMorpher();
        if (morpher != null)
            try {
                morpher.morph(this, mFi, obj);
            } catch (ServiceException e) {
                throw new EvaluationException(e);
            }
    }

    @Override
    public Object exec(Arg... args) throws MogramException, RemoteException {
        return null;
    }

    static class Session {
        long eventID;
        Object source;
        long leaseLenght;
        long seqNum = 0;
        RemoteEventListener listener;
        MarshalledObject handback;

        Session(long eventID, Object source, MarshalledObject handback,
                RemoteEventListener toInform, long leaseLenght) {
            this.eventID = eventID;
            this.source = source;
            this.leaseLenght = leaseLenght;
            this.listener = toInform;
            this.handback = handback;
        }
    }

}
