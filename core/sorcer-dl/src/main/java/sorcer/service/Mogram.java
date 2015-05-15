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
import sorcer.core.context.ThrowableTrace;

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
public interface Mogram extends Service, ServiceProjection, Scopable, Substitutable, Identifiable {

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

    public void setParentId(Uuid parentId);

    public Signature getProcessSignature();

    /**
     * Returns a status of this mogram.
     *
     * @return a status
     */
    public int getStatus();

    public void setStatus(int value);

    public Mogram clearScope() throws MogramException;

    /**
     * Returns the list of traces of thrown exceptions from this mogram.
     * @return ThrowableTrace list
     */
    public List<ThrowableTrace> getExceptions();

    /**
     * Returns the list of all traces of thrown exceptions with exceptions of
     * component mograms.
     *
     * @return ThrowableTrace list
     */
    public List<ThrowableTrace> getAllExceptions();

    /**
     * Returns a service fidelity of this exertion that consists of process
     * signature, all pre-processing, post-processing, and append signatures.
     * There is only one process signature defining late binding to the service
     * provider processing this exertion.
     *
     * @return a collection of all service signatures
     * @see #getProcessSignature
     */
    public Fidelity<Signature> getFidelity();

    /**
     * Returns a map of all available service fidelities of this exertion.
     */
    public Map<String, Fidelity<Signature>> getFidelities();

    /**
     * Returns <code>true</code> if this exertion should be monitored for its
     * execution, otherwise <code>false</code>.
     *
     * @return <code>true</code> if this exertion requires its execution to be
     *         monitored.
     */
    public boolean isMonitorable();

    public Mogram substitute(Arg... entries) throws SetterException;

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
	 *            The domainName to set.
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
    public Integer getScopeCode();

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

    /**
     * Check if this context is export controlled, accessible to principals from
     * export controlled countries.
     *
     * @return true if is export controlled
     */
    public boolean isExportControlled();

}
