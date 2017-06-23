/*
 * Copyright 2015 the original author or authors.
 * Copyright 2015 SorcerSoft.org.
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
import sorcer.core.context.ThrowableTrace;
import sorcer.core.provider.Exerter;

import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * An top-level common interface for all mograms in SORCER.
 *
 * @author Mike Sobolewski
 */
public interface Mogram extends Identifiable, Exerter, Scopable, Substitutable, Request {

    /**
     * Exerts this mogram by the assigned service provider if it is set. If a service
     * provider is not set then at runtime it bounds to any available provider
     * that matches this mogram's signature of the <code>PROCESS</code> fiType.
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
            MogramException, RemoteException;

    public <T extends Mogram> T exert(Arg... entries) throws TransactionException, MogramException,
            RemoteException;;

    /**
     * Returns an ID of this mogram.
     *
     * @return a unique identifier
     */
    public Uuid getId();

    public int getIndex();

    public void setIndex(int i);

    public Mogram getParent();

    public void setParentId(Uuid parentId);

    public Signature getProcessSignature();

    public Mogram deploy(List<Signature> builders) throws MogramException, ConfigurationException;
    /**
     * Returns a status of this mogram.
     *
     * @return a status
     */
    public int getStatus();

    public void setStatus(int value);

    public Context getContext() throws ContextException;

    public Mogram clearScope() throws MogramException;

    public void reportException(Throwable t);

    /**
     * Returns the list of traces of thrown exceptions from this mogram.
     * @return ThrowableTrace list
     */
    public List<ThrowableTrace> getExceptions() throws RemoteException;

    /**
     * Returns the list of traces left by collborating services.
     * @return ThrowableTrace list
     */
    public List<String> getTrace() throws RemoteException;

    /**
     * Appends a trace info to a trace list of this mogram.
     */
    public void appendTrace(String info) throws RemoteException;

    /**
     * Returns the list of all traces of thrown exceptions with exceptions of
     * component mograms.
     *
     * @return ThrowableTrace list
     */
    public List<ThrowableTrace> getAllExceptions() throws RemoteException;


    /**
     * Returns a service fidelity of this exertion that consists of process
     * signature, all pre-processing, post-processing, and append signatures.
     * There is only one process signature defining late binding to the service
     * provider processing this exertion.
     *
     * @return a service fidelity
     * @param selection
     *            The service fidelity name.
     */
    public ServiceFidelity selectFidelity(String selection);

    /**
     * Returns a service fidelity of this exertion that consists of process
     * signature, all pre-processing, post-processing, and append signatures.
     * There is only one process signature defining late binding to the service
     * provider processing this exertion.
     *
     * @return a collection of all service signatures
     * @see #getProcessSignature
     */
    public ServiceFidelity<Signature> getSelectedFidelity();

    /**
     * Returns a map of all available service fidelities of this exertion.
     */
    public Map<String, ServiceFidelity> getFidelities();

    /**
     * Returns a fdelity manager for of this exertion.
     */
    public FidelityManagement getFidelityManager();

    /**
     * Returns <code>true</code> if this exertion should be monitored for its
     * execution, otherwise <code>false</code>.
     *
     * @return <code>true</code> if this exertion requires its execution to be
     *         monitored.
     */
    public boolean isMonitorable() throws RemoteException;

    /**
     * The exertion format for thin exertions (no RMI and Jini classes)
     */
    public static final int THIN = 0;

    public Uuid getParentId();

    /**
     * Return date when exertion was created
     * @return
     */
    public Date getCreationDate();

    /**
	 */
	public Date getGoodUntilDate();

	/**
	 * @param date
	 *            The goodUntilDate to set.
	 */
	public void setGoodUntilDate(Date date);

	/**
	 */
	public String getDomainId();

	/**
	 * @param id
	 *            The domainID to set.
	 */
	public void setDomainId(String id);

	/**
	 */
	public String getSubdomainId();

	/**
	 * @param id
	 *            The subdomainID to set.
	 */
	public void setSubdomainId(String id);

	/**
	 */
	public String getDomainName();

	/**
	 * @param name
	 *            The domain name to set.
	 */
	public void setDomainName(String name);

	/**
	 */
	public String getSubdomainName();

    /**
     * @param name
     *            The subdomainName to set.
     */
    public void setSubdomainName(String name);

    /**
     * Returns a principal using this service context.
     *
     * @return a Principal
     */
    public Principal getPrincipal();

    /**
     */
    public Date getLastUpdateDate();

    /**
     * @param date
     *            The lastUpdateDate to set.
     */
    public void setLastUpdateDate(Date date);

    /**
     * @param description
     *            The description to set.
     */
    public void setDescription(String description);

    /** 
     *
     * Assigns a name for this service context. 
     *
     * @param name 
     *       a context name to set.
     */

    public void setName(String name);

    /**
     */
    public String getDescription();

    /**
     */
    public String getOwnerId();

    /**
     */
    public String getSubjectId();

    /**
     * @param projectName
     *            The project to set.
     */
    public void setProjectName(String projectName);

    /**
     */
    public String getProjectName();

    public boolean isValid();

    public void isValid(boolean state);

    /**
     * Returns a data service context (service data) of this mogram.
     *
     * @return a service context
     * @throws ContextException
     */
    public Context getDataContext() throws ContextException;

    /**
     * Reconfigure this model with given fudelities.
     *
     * @param fidelities
     */
    public void reconfigure(Fidelity... fidelities) throws ContextException, RemoteException;

    /**
     * Reconfigure this model with given names of metafidelities.
     *
     * @param metaFiNames
     */
    public void morph(String... metaFiNames) throws RemoteException;

    /**
     * Check if this context is export controlled, accessible to principals from
     * export controlled countries.
     *
     * @return true if is export controlled
     */
    public boolean isExportControlled();

    /**
     *  Returns a signature builder that returns instances of this model.
     *  A inConnector specifies a map of an input context as needed by another collaborating service.
     *
     * @param args  optional configuration arguments
     * @return  a signature for the builder of this model                                                                                s
     * @throws ContextException
     * @throws RemoteException
     */
    public Signature getBuilder(Arg... args) throws MogramException;

    public void applyFidelity(String name);

    public MogramStrategy getMogramStrategy();

    public void setBuilder(Signature builder) throws MogramException;

    Object get(String component);

    public String describe();
}
