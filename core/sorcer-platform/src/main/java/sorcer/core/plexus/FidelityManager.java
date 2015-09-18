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
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.core.context.ModelStrategy;
import sorcer.service.Fidelity;
import sorcer.service.FidelityManagement;
import sorcer.service.Mogram;
import sorcer.service.MogramException;

import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Mike Sobolewski on 6/14/15.
 */
public class FidelityManager<T> implements FidelityManagement<T>, Serializable {

    // fidelities for this service
    protected Map<String, Fidelity<T>> fidelities = new ConcurrentHashMap<String, Fidelity<T>>();

    protected Fidelity<T> selectedFidelity;

    protected ModelStrategy runtime;

    protected Map<Long, Session> sessions;

    public FidelityManager(Mogram mogram) {
        this.runtime = new ModelStrategy(mogram);
    }

    public void addFidelity(Fidelity<T>... fidelities) {
        for (Fidelity f : fidelities)
            this.fidelities.put(f.getName(), f);
    }

    public void setSelectedFidelity(String name) {
        selectedFidelity = fidelities.get(name);
    }

    @Override
    public Map<String, Fidelity<T>> getFidelities() {
        return fidelities;
    }

    @Override
    public Fidelity<T> getSelectedFidelity() {
        return selectedFidelity;
    }

    @Override
    public <T extends Mogram> T service(T mogram, Transaction txn) throws TransactionException, MogramException, RemoteException {
        runtime.setTarget(mogram);
        return (T) runtime.exert(txn);
    }

    @Override
    public <T extends Mogram> T service(T mogram) throws TransactionException, MogramException, RemoteException {
        runtime.setTarget(mogram);
        return (T) runtime.exert();
    }

    public ModelStrategy getRuntime() {
        return runtime;
    }

    public void setRuntime(ModelStrategy runtime) {
        this.runtime = runtime;
    }

    @Override
    public void notify(RemoteEvent remoteEvent) throws UnknownEventException, RemoteException {

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
