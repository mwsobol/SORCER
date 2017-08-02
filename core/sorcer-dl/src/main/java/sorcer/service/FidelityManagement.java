/*
 * Copyright 2014 the original author or authors.
 * Copyright 2014 SorcerSoft.org.
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

import net.jini.core.entry.Entry;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Created by Mike Sobolewski on 6/14/15.
 */
public interface FidelityManagement<T extends Arg> extends Service, RemoteEventListener, Serializable {

    public Map<String, ServiceFidelity<T>> getFidelities() throws RemoteException;

    public Map<String, ServiceFidelity<Fidelity>> getMetafidelities() throws RemoteException;

    public void morph(String... fiNames) throws ContextException, RemoteException;

    public void reconfigure(String... fiNames) throws ContextException, RemoteException;

    public void reconfigure(Fidelity... fidelities) throws ContextException, RemoteException;

    public List<Fidelity> getDefaultFidelities() throws RemoteException;

    public Mogram getMogram() throws RemoteException;

    public List<Fidelity> getFiTrace() throws RemoteException;

    public void addTrace(ServiceFidelity fi);

    public void publish(Entry entry) throws RemoteException, ContextException;

    public EventRegistration register(long eventID, String path,
                                      RemoteEventListener toInform, long leaseLenght)
            throws UnknownEventException, RemoteException;

    public void deregister(long eventID) throws UnknownEventException,
            RemoteException;

}
