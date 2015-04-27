/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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
import net.jini.core.transaction.TransactionException;
import net.jini.id.Uuid;

import java.rmi.RemoteException;

/**
 * An top-level common interface for all mograms in SORCER.
 *
 * @author Mike Sobolewski
 */
public interface Mogram extends Service, Scopable, Identifiable {

    /**
     * Exerts this mogram by the assigned service provider if it is set. If a service
     * provider is not set then at runtime it bounds to any available provider
     * that matches this mogram's signature of the <code>PROCESS</code> type.
     * Service exertions and models are instances of mograms.
     *
     * @param txn
     *            The transaction (if any) under which to exert.
     * @return a resulting exertion
     * @throws net.jini.core.transaction.TransactionException
     *             if a transaction error occurs
     * @throws ExertionException
     *             if processing this exertion causes an error
     */
    public <T extends Mogram> T exert(Transaction txn, Arg... entries) throws TransactionException,
            ExertionException, RemoteException;

    public <T extends Mogram> T exert(Arg... entries) throws TransactionException, ExertionException,
            RemoteException;

    public void setIndex(int i);

    public void setParentId(Uuid parentId);

    public Signature getProcessSignature();

    public int getStatus();

    public void setStatus(int value);
}
