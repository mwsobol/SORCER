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

import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Mike Sobolewski on 6/14/15.
 */
public class FidelityManager<T extends Arg> implements FidelityManagement<T>, Observer, Identifiable, Serializable {

    // sequence number for unnamed instances
    protected static int count = 0;

    private String name;

    Uuid id = UuidFactory.generate();

    // fidelities for signatures
    protected Map<String, Fidelity<T>> fidelities = new ConcurrentHashMap<>();

    // fidelities for fidelites
    protected Map<String, Fidelity<Fidelity>> metafidelities = new ConcurrentHashMap<>();

    protected Mogram mogram;

    protected Map<Long, Session> sessions;

    public FidelityManager() {
        name = "fiManager" +  count++;
    }

    public FidelityManager(String name) {
        this.name = name;
    }

    public FidelityManager(Mogram mogram) {
        this.mogram = mogram;
        name = "fiManager" +  count++;
    }

    public Map<String, Fidelity<Fidelity>> getMetafidelities() {
        return metafidelities;
    }

    public void setMetafidelities(Map<String, Fidelity<Fidelity>> metafidelities) {
        this.metafidelities = metafidelities;
    }

    @Override
    public Map<String, Fidelity<T>> getFidelities() {
        return fidelities;
    }

    public void setFidelities(Map<String, Fidelity<T>> fidelities) {
        this.fidelities = fidelities;
    }

    public void addFidelity(String path, Fidelity<T> fi) {
            this.fidelities.put(path, fi);
    }

    public void addMetafidelity(String path, Fidelity<Fidelity> fi) {
        this.metafidelities.put(path, fi);
    }

    public Mogram getMogram() {
        return mogram;
    }

    public void setMogram(Mogram mogram) {
        this.mogram = mogram;
    }

    @Override
    public <M extends Mogram> M service(M mogram, Transaction txn) throws TransactionException, MogramException, RemoteException {
        this.mogram = mogram;
        return (M) mogram.exert(txn);
    }

    public <T extends Mogram> T service(T mogram) throws TransactionException, MogramException, RemoteException {
        return service(mogram, null);
    }

    public void initialize() {
       // implement is subclasses
    }

    public void init(List<Fidelity<Fidelity>> fidelities) {
        if (fidelities == null || fidelities.size() == 0) {
            initialize();
            return;
        }
        for (Fidelity fi : fidelities) {
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
            Fidelity mFi = metafidelities.get(fiName);
            List<Fidelity> fis = mFi.getSelects();
            String name = null;
            String path = null;
            for (Fidelity fi : fis) {
                name = fi.getName();
                path = fi.getPath();
                Iterator<Map.Entry<String, Fidelity<T>>> i = fidelities.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry<String, Fidelity<T>> fiEnt = i.next();
                    if (fiEnt.getKey().equals(path)) {
                        fiEnt.getValue().setFidelitySelection(name);
                    }
                }
            }
        }
    }

    public void add(Fidelity<Fidelity>... sysFis) {
        for (Fidelity<Fidelity> sysFi : sysFis){
            metafidelities.put(sysFi.getName(), sysFi);
        }
    }

    public void put(String sysFiName, Fidelity<Fidelity> sysFi) {
        metafidelities.put(sysFiName, sysFi);
    }

    public void put(Entry<Fidelity<Fidelity>>... entries) {
        try {
            for(Entry<Fidelity<Fidelity>> e : entries) {
                metafidelities.put(e.getName(), e.getValue());
            }
        } catch (EvaluationException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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
        // implement in subclasses and use this morphers provided by MultiFidelities (observables)

        MultiFidelity mFi = (MultiFidelity)observable;
        Morpher morpher = mFi.getMorpher();
        if (morpher != null)
            morpher.morph(this, mFi, obj);
    }

    @Override
    public <T extends Servicer> Object exec(T srv, Arg... entries) throws MogramException, RemoteException {
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
