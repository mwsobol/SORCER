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
import net.jini.core.transaction.TransactionException;
import sorcer.core.context.ThrowableTrace;

import java.rmi.RemoteException;
import java.util.List;

/**
 * A free mogram is a mogram that has a name only to be bound at runtime.
 *
 * @see Mogram
 *
 * @author Mike Sobolewski
 */
public class FreeMogram extends ServiceMogram {

    Mogram mogram;

    public FreeMogram(String name) {
        this.key = name;
    }


    public Mogram getMogram() {
        return mogram;
    }

    public void setMogram(Mogram mogram) {
        this.mogram = mogram;
    }

    @Override
    public <T extends Mogram> T exert(Transaction txn, Arg... entries) throws TransactionException, MogramException, RemoteException {
        return null;
    }

    @Override
    public <T extends Mogram> T exert(Arg... entries) throws TransactionException, MogramException, RemoteException {
        return null;
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
    public void reportException(Throwable t) {

    }

    @Override
    public List<ThrowableTrace> getExceptions() throws RemoteException {
        return null;
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
    public boolean isMonitorable() throws RemoteException {
        return false;
    }

    @Override
    public Context getDataContext() throws ContextException {
        return null;
    }

    @Override
    public String describe() {
        return null;
    }

    @Override
    public <T extends Mogram> T exert(T mogram, Transaction txn, Arg... entries) throws TransactionException, MogramException, RemoteException {
        return null;
    }

    @Override
    public void substitute(Arg... entries) throws SetterException, RemoteException {

    }
}
