/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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

package sorcer.core.provider.exertmonitor;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.monitor.MonitorEvent;
import sorcer.core.monitor.MonitorableSession;
import sorcer.core.provider.MonitorManagementSession;
import sorcer.core.provider.MonitoringManagement;
import sorcer.core.provider.Provider;
import sorcer.service.Context;
import sorcer.service.Exec;
import sorcer.service.Exertion;
import sorcer.service.Job;
import sorcer.service.MonitorException;
import sorcer.service.Monitorable;
import sorcer.service.ServiceExertion;
import sorcer.util.ObjectCloner;

public class MonitorSession extends ArrayList<MonitorSession> implements
	MonitorManagementSession, MonitorLandlord.MonitorLeasedResource, Serializable {

	static final long serialVersionUID = -4427096084987355507L;

	// ThreadPool for event processing
	{
		try {
			eventPool = Executors.newFixedThreadPool(EVENT_TASK_POOL_MAX);
		} catch (Exception e) {
			logger.log(
					Level.SEVERE,
					"Error while instantiating eventPool... "
							+ e.getMessage());
		}
	}

	public transient static MonitorLandlord mLandlord;

	public transient static MonitoringManagement sessionManager;

	public transient static ExecutorService eventPool;

	static transient final String LOGGER = "sorcer.core.provider.monitor.SessionResource";

	static transient final Logger logger = Logger.getLogger(LOGGER);

	static transient final int EVENT_TASK_POOL_MIN = 1;

	static transient final int EVENT_TASK_POOL_MAX = 5;

	private Uuid cookie;

	private ServiceExertion initialExertion;

	private ServiceExertion runtimeExertion;

	private Monitorable provider;

	private MonitorSession parentResource;

	private RemoteEventListener listener;

	private long expiration;

	private long timeout;

	// The state which is sorcer.core.monitor.ExertionState
	// final int INITIAL = 1;
	// final int INSPACE = 2;
	// final int RUNNING = 3;
	// final int DONE = 4;
	// final int SUSPENDED = 5;
	// final int ERROR = 0;
	// final int FAILED = -1;
	// private int state;

	private Lease lease;

	public MonitorSession(Exertion ex, RemoteEventListener listener,
			long duration) throws IOException {
		super();
		if (ex == null)
			throw new NullPointerException(
					"Assertion Failed: initialExertion cannot be NULL");

		this.initialExertion = (ServiceExertion) ex;
		runtimeExertion = (ServiceExertion) ObjectCloner.cloneAnnotated(ex);
		this.listener = listener;
		init();
		runtimeExertion.setStatus(Exec.INITIAL);

		// Set the epiration for the root Resource
		// get the lease and stick it
		setTimeout(Long.MAX_VALUE);
		if (mLandlord != null) {
			setExpiration(mLandlord.getExpiration(duration));
			lease = mLandlord.newLease(this);
		}
		runtimeExertion.setMonitorSession(new MonitorableSession(
				sessionManager, cookie, lease));
	}

	private MonitorSession(Exertion xrt, Exertion runtimeXrt,
			MonitorSession parentSession) {

		super();

		if (xrt == null || runtimeXrt == null)
			throw new NullPointerException(
					"Assertion Failed: initialExertion cannot be NULL");

		this.initialExertion = (ServiceExertion) xrt;
		this.runtimeExertion = (ServiceExertion) runtimeXrt;
		this.parentResource = parentSession;
		this.listener = parentSession.getListener();
		init();

		((ServiceExertion) runtimeXrt)
				.setMonitorSession(new MonitorableSession(sessionManager,
						cookie));

	}

	public void restore() {

	}

	private void init() {
		cookie = UuidFactory.generate();
		if (((ServiceExertion) initialExertion).isJob())
			addSessions((Job) initialExertion, (Job) runtimeExertion, this);
	}

	private void addSessions(Job initial, Job runtime, MonitorSession parent) {
		for (int i = 0; i < initial.size(); i++)
			add(new MonitorSession(initial.get(i),
					runtime.get(i), parent));
	}

	public RemoteEventListener getListener() {
		return listener;
	}

	public Map<Uuid, MonitorManagementSession> getSessions() {
		HashMap<Uuid, MonitorManagementSession> map = new HashMap<Uuid, MonitorManagementSession>();
		collectSessions(map);
		map.put(cookie, this);
		return map;
	}

	private HashMap<Uuid, MonitorManagementSession> collectSessions(HashMap<Uuid, MonitorManagementSession> map) {
		MonitorSession resource;
		for (int i = 0; i < size(); i++) {
			resource = (MonitorSession) get(i);
			map.put(cookie, resource);
			collectSessions(map);
		}
		return map;
	}

	public Lease init(Monitorable executor, long duration, long timeout)
			throws MonitorException {

		if (executor == null)
			throw new NullPointerException(
					"Assertion Failed: executor cannot be NULL");

		if (isRunning() || isInSpace()) {
			logger.log(Level.SEVERE,
					"Trying to initialize a exertion already in space or is running"
							+ this);
			throw new MonitorException(
					"Session already active and is in state =" + getState());
		}

		runtimeExertion.setStatus(Exec.RUNNING);
		this.provider = executor;
		setExpiration(mLandlord.getExpiration(duration));
		setTimeout(System.currentTimeMillis() + timeout);
		persist();
		return mLandlord.newLease(this);
	}

	public void init(long duration, long timeout) throws MonitorException {

		if (isRunning() || isInSpace()) {
			logger.log(Level.SEVERE,
					"Trying to initialize a exertion already in space or is running"
							+ this);
			throw new MonitorException("Session already active state="
					+ getState());
		}

		setExpiration(mLandlord.getExpiration(duration));
		setTimeout(System.currentTimeMillis() + timeout);

		runtimeExertion.setStatus(Exec.INSPACE);
		persist();
		lease = mLandlord.newLease(this);
	}

	public Lease init(Monitorable executor) throws MonitorException {
		if (executor == null)
			throw new NullPointerException(
					"Assertion Failed: executor cannot be NULL");

		if (!isInSpace()) {
			logger.log(Level.SEVERE,
					"Trying to initialize a exertion not in space" + this);
			throw new MonitorException(
					"This session can be only activated without being picked from space current state="
							+ getState());
		}

		runtimeExertion.setStatus(Exec.RUNNING);
		this.provider = executor;
		persist();
		return lease;
	}

	public void update(Context<?> ctx) {
		if (ctx == null)
			throw new NullPointerException(
					"Assertion Failed: ctx cannot be NULL");

		if (runtimeExertion instanceof ServiceExertion)
			runtimeExertion.setContext(ctx);
		persist();
	}

	public void done(Context<?> ctx) throws MonitorException {
		if (ctx == null)
			new NullPointerException("Assertion Failed: ctx cannot be null");

		if (!isRunning()) {
			logger.log(Level.SEVERE,
					"Trying to call done on a non running resource" + this);
			throw new MonitorException("Exertion not running, state="
					+ getState());
		}

		logger.log(Level.INFO,
				" This exertion is completed " + runtimeExertion.getName());

		runtimeExertion.setStatus(Exec.DONE);
		if (runtimeExertion instanceof ServiceExertion)
			runtimeExertion.setContext(ctx);

		fireRemoteEvent();
		notifyParent();
		persist();
		mLandlord.remove(this);
	}

	public void failed(Context<?> ctx) throws MonitorException {
		if (ctx == null)
			throw new NullPointerException(
					"Assertion Failed: ctx cannot be NULL");

		if (!isRunning()) {
			logger.log(Level.SEVERE,
					"Trying to call failed on a non running resource" + this);
			throw new MonitorException("Exertion not running . state="
					+ getState());
		}

		runtimeExertion.setStatus(Exec.FAILED);
		if (runtimeExertion instanceof ServiceExertion)
			runtimeExertion.setContext(ctx);

		fireRemoteEvent();
		notifyParent();
		persist();
		mLandlord.remove(this);
	}

	private void notifyParent() {
		if (parentResource != null)
			parentResource.stateChanged();
		else {
			// so error has propogated to the top.
			// check if we are done. If so then remove yourself from
			// leasemanager
			if (getState() != Exec.RUNNING)
				mLandlord.remove(this);
		}
	}

	private void stateChanged() {
		int oldState = getState();
		// logger.log(Level.INFO,
		System.out.println("stateChanged called oldState =" + getState()
				+ " resetting state.....");
		resetState();
		// logger.log(Level.INFO,
		System.out.println("stateChanged called newState =" + getState());
		if (oldState != getState()) {
			fireRemoteEvent();
			notifyParent();
			persist();
		}
	}

	// Persist only the root session
	private void persist() {
		if (parentResource != null)
			return;
		try {
			sessionManager.persist(this);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				logger.log(Level.SEVERE, "Could not persist the session resource:\n"
						+ initialExertion + " at: " + ((Provider)sessionManager).getProviderName());
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Here's the algorithm to manage the states based on states of children
	 * Rule 1: If any one of the child state is FAILED and all others are DONE,
	 * marked ourself FAILED Rule 2: If all child are DONE, mark ourself as DONE
	 * Rule 3: If any child is SUSPENDED and all other child are DONE, then we
	 * are in USPENDED state
	 */
	private void resetState() {
		int failedCount = 0, suspendedCount = 0, doneCount = 0;
		for (int i = 0; i < size(); i++) {
			if (((MonitorSession) get(i)).isFailed())
				failedCount++;
			else if (((MonitorSession) get(i)).isSuspended())
				suspendedCount++;
			else if (((MonitorSession) get(i)).isDone())
				doneCount++;
			else
				// logger.log(Level.INFO,
				System.out
						.println("State not accounted for while resetting state"
								+ i
								+ " state="
								+ ((MonitorSession) get(i)).getState());

		}
		// logger.log(Level.SEVERE,
		System.out.println("failed count=" + failedCount + " suspended count="
				+ suspendedCount + " doneCount=" + doneCount);

		if (doneCount == size())
			runtimeExertion.setStatus(Exec.DONE);
		else if (failedCount != 0
				&& failedCount + doneCount + suspendedCount == size())
			runtimeExertion.setStatus(Exec.FAILED);
		else if (suspendedCount != 0 && doneCount + suspendedCount == size())
			runtimeExertion.setStatus(Exec.SUSPENDED);

	}

	public int getState() {
		return ((ServiceExertion) runtimeExertion).getStatus();
	}

	public boolean isInitial() {
		return (((ServiceExertion) runtimeExertion).getStatus() == Exec.INITIAL);
	}

	public boolean isInSpace() {
		return (runtimeExertion.getStatus() == Exec.INSPACE);
	}

	public boolean isRunning() {
		return (runtimeExertion.getStatus() == Exec.RUNNING);
	}

	public boolean isDone() {
		return (runtimeExertion.getStatus() == Exec.DONE);
	}

	public boolean isSuspended() {
		return (runtimeExertion.getStatus() == Exec.SUSPENDED);
	}

	public boolean isError() {
		return (runtimeExertion.getStatus() == Exec.ERROR);
	}

	public boolean isFailed() {
		return (runtimeExertion.getStatus() <= Exec.FAILED);
	}

	/**
	 * 
	 * Searches if any SessionResource exists with this parent session with a
	 * child session having the same value for the cookie.
	 * 
	 * @param The
	 *            cookie for which corresponding to a SessionResource contained
	 *            in this session resource
	 * 
	 * @returns null if no such SessionResource exists
	 * 
	 */
	public MonitorSession getSessionResource(Uuid cookie) {

		if (cookie.equals(this.cookie))
			return this;
		else {
			MonitorSession resource;
			for (int i = 0; i < size(); i++)
				if ((resource = ((MonitorSession) get(i))
						.getSessionResource(cookie)) != null)
					return resource;
		}
		return null;
	}

	/***************************************************************************
	 * 
	 * Start implementing the semantics of MonitorLandlord.MonitorLeasedResource
	 * 
	 **************************************************************************/

	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}

	public void setTimeout(long timeoutDuration) {
		timeout = timeoutDuration;
	}

	public long getTimeout() {
		return timeout;
	}

	// If the object is in space, the lease
	// never expires
	public long getExpiration() {
		if (runtimeExertion.getStatus() == Exec.INSPACE)
			return Long.MAX_VALUE;
		else
			return expiration;
	}

	public void leaseCancelled() {
		try {
			((ServiceExertion) runtimeExertion)
					.reportException(new Exception(
							"Lease was cancelled..The provider did not renew the lease"));
			runtimeExertion.setStatus(Exec.FAILED);

			fireRemoteEvent();
			notifyParent();
			persist();

		} catch (Exception e) {
			logger.log(Level.SEVERE,
					"Exception occured which calling leaseCancelled");
		}

	}

	public void timedOut() {
		try {
			((ServiceExertion) runtimeExertion).reportException(new Exception(
					"This exertion was timedout."));
			runtimeExertion.setStatus(Exec.FAILED);

			fireRemoteEvent();
			notifyParent();
			persist();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception occured which calling timedOut");
		}
	}

	public void setCookie(Uuid cookie) {
		this.cookie = cookie;
	}

	public Uuid getCookie() {
		return cookie;
	}

	public ServiceExertion getInitialExertion() {
		return initialExertion;
	}

	public Exertion getRuntimeExertion() {
		return runtimeExertion;
	}

	public String toString() {
		return "cookie:" + cookie + " exertion:" + runtimeExertion.getName();
	}

	// Event firing mechanism
	private void fireRemoteEvent() {
		try {			
			MonitorEvent event = new MonitorEvent(sessionManager,
					runtimeExertion, runtimeExertion.getStatus());
			eventPool.submit(new MonitorEventTask(event, listener));
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Dispatching Monitoring Event", e);
		}
	}

	static class MonitorEventTask implements Runnable {
		MonitorEvent event;

		RemoteEventListener listener;

		MonitorEventTask(MonitorEvent event, RemoteEventListener listener) {
			this.event = event;
			this.listener = listener;
		}

		public void run() {
			try {
				listener.notify(event);
			} catch (Exception e) {
				logger.log(Level.WARNING,
						"Exception notifying event consumers", e);
			}
		}
	}

}
