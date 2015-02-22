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

import com.sleepycat.collections.StoredMap;
import com.sleepycat.je.DatabaseException;
import com.sun.jini.start.LifeCycle;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.id.Uuid;
import sorcer.core.UEID;
import sorcer.core.provider.MonitorManagementSession;
import sorcer.core.provider.MonitoringManagement;
import sorcer.core.provider.ServiceProvider;
import sorcer.core.provider.exertmonitor.db.SessionDatabase;
import sorcer.core.provider.exertmonitor.db.SessionDatabaseViews;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.*;
import sorcer.service.Exec.State;
import sorcer.util.bdb.objects.UuidKey;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;

public class ExertMonitor extends ServiceProvider implements MonitoringManagement {

	static transient final String LOGGER = "sorcer.core.provider.monitor.MonitororImpl";

	static transient final Logger logger = Logger.getLogger(LOGGER);

	private MonitorLandlord landlord;

	private SessionDatabase db;
	
	private StoredMap<UuidKey, MonitorManagementSession> resources;

	public ExertMonitor(String[] args, LifeCycle lifeCycle) throws Exception {
		super(args, lifeCycle);
		initMonitor();
	}

	private void initMonitor() throws Exception {
		landlord = new MonitorLandlord();
		String dbHome = getProperty("monitor.database.home");
		File dbHomeFile = null;
		if (dbHome == null || dbHome.length() == 0) {
			logger.severe("Session database home missing: " + dbHome);
			destroy();
		} else {
			dbHomeFile = new File(dbHome);
			if (!dbHomeFile.isDirectory() && !dbHomeFile.exists()) {			
				boolean done = dbHomeFile.mkdirs();
				if (!done) {
					logger.severe("Not able to create session database home: " 
							+ dbHomeFile.getAbsolutePath());
					destroy();
				}
			}
		}
        System.out.println("Opening BDBJE environment in: " + dbHomeFile.getAbsolutePath());
		db = new SessionDatabase(dbHome);
		SessionDatabaseViews views = new SessionDatabaseViews(db);
		resources = views.getSessionMap();

		// statically initialize
		MonitorSession.mLandlord = landlord;
		MonitorSession.sessionManager = (MonitoringManagement) getServiceProxy();
	}

	public void stop(UEID ref, SorcerPrincipal principal)
			throws RemoteException, UnknownExertionException,
			AccessDeniedException {

	}

	public void suspend(UEID ref, SorcerPrincipal principal)
			throws RemoteException, UnknownExertionException,
			AccessDeniedException {

	}

	public void resume(UEID ref, SorcerPrincipal principal)
			throws RemoteException, UnknownExertionException,
			AccessDeniedException {

	}

	public void step(UEID ref, SorcerPrincipal principal)
			throws RemoteException, UnknownExertionException,
			AccessDeniedException {

	}

	Object resourcesWriteLock = new Object();

	public Exertion register(RemoteEventListener lstnr, Exertion ex,
			long duration) throws RemoteException {

		MonitorSession resource;
		try {
			resource = new MonitorSession(ex, lstnr, duration);
		} catch (IOException ioe) {
			throw new RemoteException(ioe.getMessage());
		}

		synchronized (resourcesWriteLock) {
			try {
				persist(resource);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return resource.getRuntimeExertion();
	}

	/**
	 * Makes this an active session. The jobber decides the lease duration and
	 * the timeout after which the monitor will call on monitorables that the
	 * job is failed and report back to the Listener that the exertion of this
	 * session has failed.
	 * 
	 * @param mntrbl
	 *            The monitorable to which this task is dispatched.
	 * @param duration
	 *            Requested lease duration for the session.
	 * @param timeout
	 *            Timeout for execution of this task.
	 * 
	 * @throws MonitorException
	 *             1) If there is no such session 2) If this session is already
	 *             active
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */

	public Lease init(Uuid cookie, Monitorable mntrbl, long duration,
			long timeout) throws RemoteException, MonitorException {

		// Get the SessionResource correspoding to this cookie
		MonitorSession resource = findSessionResource(cookie);

		if (resource == null)
			throw new MonitorException("There exists no such session");

		return resource.init(mntrbl, duration, timeout);
	}
	
	private MonitorSession findSessionResource(Uuid cookie)
			throws MonitorException {

		MonitorSession resource;

		// Check if landlord is keeping it in memory
		Hashtable lresources = landlord.getResources();
		if (lresources.get(cookie) != null)
			return (MonitorSession) lresources.get(cookie);

		Uuid key;
		for (Enumeration e = lresources.keys(); e.hasMoreElements();) {
			key = (Uuid) e.nextElement();
			resource = ((MonitorSession) lresources.get(key))
			.getSessionResource(cookie);
			if (resource != null)
				return resource;
		}

		// if (landlord.getResource(cookie)!=null) return
		// (SessionResource)landlord.getResource(cookie);

		// Ok it's not with landlord. So we retrieve it from the database
		synchronized (resourcesWriteLock) {
			Iterator<Map.Entry<UuidKey, MonitorManagementSession>> si = resources.entrySet().iterator();
			Map.Entry<UuidKey, MonitorManagementSession> next;
			while (si.hasNext()) {
				next = si.next();
				try {
					resource = ((MonitorSession)getSession(next.getKey())).getSessionResource(cookie);
				} catch (Exception e) {
					throw new MonitorException(e);
				} 
				if (resource != null)
					return resource;
			}
		}
			return null;
	}

	/**
	 * 
	 * If the Broker wants to drop the exertion to space, then the Broker has no
	 * idea who will pick up this exertion. In that case, it doesn't make sense
	 * for the broker to force leasing. However, it may activate the the session
	 * with the timeout marked and the lease duration specified so that if no
	 * provider picks out and the task gets timed out, then we can clean up the
	 * entry from space and notify the broker.
	 * 
	 * If the provider picks up before it timesout, then the provider must
	 * initialize this session by calling init(Monitorable) so that the monitor
	 * will now make sure that the leases are renewed properly for this session.
	 * 
	 * @param duration
	 *            Requested lease duration for the session.
	 * @param timeout
	 *            Timeout for execution of this task wich includes idle time in
	 *            space.
	 * 
	 * @throws MonitorException
	 *             1) If this session is already active 2) If there is no such
	 *             session
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */

	public void init(Uuid cookie, long duration, long timeout)
			throws RemoteException, MonitorException {
		// Get the SessionResource correspoding to this cookie
		MonitorSession resource = findSessionResource(cookie);

		if (resource == null)
			throw new MonitorException("There exists no such session");

		resource.init(duration, timeout);
	}

	/**
	 * 
	 * If the Broker wants to drop the exertion to space, then the Broker has no
	 * idea who will pick up this exertion. In that case, the broker would have
	 * already set the lease duration and timeout.
	 * 
	 * The provider who picks up the entry must initialize this session by
	 * calling init(Monitorable) so that the we will now know that the task with
	 * the monitorable and also will make sure that the leases are renewed
	 * properly for this session.
	 * 
	 * @param mntrbl
	 *            The monitorable who picked this up.
	 * 
	 * @throws MonitorException
	 *             1) If there is no such session 2) The execution has been
	 *             inited by some one else.
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */

	public Lease init(Uuid cookie, Monitorable mntrbl) throws RemoteException,
			MonitorException {
		// Get the SessionResource correspoding to this cookie
		MonitorSession resource = findSessionResource(cookie);

		if (resource == null)
			throw new MonitorException("There exists no such session");

		return resource.init(mntrbl);
	}

	/**
	 * Providers use this method to update their current status of the executed
	 * tasks
	 *
     * @param cookie A Uuid
	 * @param ctx The current state of data of this task.
	 * 
	 * @throws MonitorException
	 *             1) If there is no such session 2) The session is not valid
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 */

	private void update(Uuid cookie, Context ctx) throws RemoteException,
			MonitorException {
		// Get the SessionResource corresponding to this cookie
		MonitorSession resource = findSessionResource(cookie);
		if (resource == null)
			throw new MonitorException("There exists no such session for: "
					+ cookie);

		resource.update(ctx);
	}

	/**
	 * Providers use this method to notify that the exertion has been executed.
	 * 
	 * @param ctx
	 *            The monitorable who picked this up.
	 * 
	 * @throws MonitorException
	 *             1) If there is no such session 2) The exertion does not
	 *             belong to this session
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 */

	private void done(Uuid cookie, Context ctx) throws RemoteException,
			MonitorException {
		// Get the SessionResource correspoding to this cookie
		MonitorSession resource = findSessionResource(cookie);

		if (resource == null)
			throw new MonitorException("There exists no such session");
		
		resource.done(ctx);
	}

	/**
	 * Providers use this method to notify that the exertion was failed
	 * 
	 * @param ctx
	 *            The monitorable who picked this up.
	 * 
	 * @throws MonitorException
	 *             1) If there is no such session 2) The exertion does not
	 *             belong to this session
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 */
	private void failed(Uuid cookie, Context ctx) throws RemoteException,
			MonitorException {
		MonitorSession resource = findSessionResource(cookie);

		if (resource == null)
			throw new MonitorException("There exists no such session");

		resource.failed(ctx);
	}

	public int getState(Uuid cookie) throws RemoteException, MonitorException {

		MonitorSession resource = findSessionResource(cookie);

		if (resource == null)
			throw new MonitorException("There exists no such session");

		return resource.getState();
	}

	/**
	 * The spec requires that this method gets all the monitorable exertion
	 * infos from all the monitor managers and return a Hashtable where
	 * 
	 * key -> ExertionReferenceID value -> Some info regarding this exertion
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * @throws ExertionException
	 * 
	 */
	public Map<Uuid, ExertionInfo> getMonitorableExertionInfo(
			State category, Principal principal) throws RemoteException,
			MonitorException {
		Map<Uuid, ExertionInfo> table = new HashMap<Uuid, ExertionInfo>();
		try {
			Iterator<Map.Entry<UuidKey, MonitorManagementSession>> si = resources.entrySet().iterator();
//			Map.Entry<Uuid, MonitorSession> next;
//			while (si.hasNext()) {
//				next = si.next();
//				System.out.println("session cookie: " + next.getKey());
//				System.out.println("session info: " + next.getValue());
//			}
			Iterator<UuidKey> ki = resources.keySet().iterator();
			UuidKey key;
			while (ki.hasNext()) {
				key = ki.next();
				ServiceExertion xrt = (ServiceExertion) ((MonitorSession)getSession(key)).getRuntimeExertion();						
				if (((ServiceExertion) xrt).getPrincipal().getId()
						.equals(((SorcerPrincipal) principal).getId())) {
					if (category == null || category.equals(State.NULL)
							|| xrt.getStatus() == category.ordinal()) {
						table.put(xrt.getId(), new ExertionInfo(xrt, key.getId()));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MonitorException(e);
		}
		return table;
	}

	public Exertion getMonitorableExertion(Uuid id, Principal principal)
			throws RemoteException, MonitorException {
			Exertion xrt = ((ServiceExertion) ((MonitorSession)getSession(id)).getRuntimeExertion());
			if (((ServiceExertion) xrt).getPrincipal().getId()
					.equals(((SorcerPrincipal) principal).getId()))
				return xrt;
			else
				return null;
	}

	/**
	 * For this reference ID, which references a exertion in a monitor, get the
	 * exertion if the client has enough credentials.
	 * 
	 * @throws AccessDeniedException
	 *             if the client does not have enough credentials.
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */
	public Exertion getMonitorableExertion(UEID cookie, Principal principal)
			throws RemoteException, MonitorException {
		UuidKey key;
		Exertion ex;
		Iterator<UuidKey> ki = resources.keySet().iterator();
		while (ki.hasNext()) {
			key = ki.next();
			ex = ((MonitorSession)getSession(key)).getRuntimeExertion();

			if (cookie.exertionID.equals(((ServiceExertion) ex).getId())
					&& ((ServiceExertion) ex).getPrincipal().getId()
							.equals(((SorcerPrincipal) principal).getId()))
				return ex;
			else
				return null;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * sorcer.core.monitor.MonitorSessionManagement#update(net.jini.id.Uuid,
	 * sorcer.service.Context,
	 * sorcer.core.monitor.MonitorSessionManagement.Aspect)
	 */
	@Override
	public void update(Uuid cookie, Context ctx, Object aspect)
			throws RemoteException, MonitorException {
		if (aspect.equals(State.UPDATED)) {
			update(cookie, ctx);
		} else if (aspect.equals(State.DONE)) {
			done(cookie, ctx);
		} else if (aspect.equals(State.FAILED)) {
			failed(cookie, ctx);
		}

	}

	public void destroy() {
		try {
			db.close();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
		landlord.terminate();
		super.destroy();
	}

	/* (non-Javadoc)
	 * @see sorcer.core.monitor.MonitorManagement#persist(sorcer.core.provider.exertmonitor.MonitorSession)
	 */
	@Override
	public boolean persist(MonitorManagementSession session) throws IOException {
		resources.put(new UuidKey(((MonitorSession)session).getCookie()), (MonitorSession)session);
		return true;
	}
	
	public MonitorManagementSession getSession(UuidKey key) throws MonitorException {
		try {
			return resources.get(key);
		} catch (Exception e) {
			throw new MonitorException(e);
		}
	}

	public MonitorManagementSession getSession(Uuid key) throws MonitorException {
		try {
			return resources.get(new UuidKey(key));
		} catch (Exception e) {
			throw new MonitorException(e);
		}
	}
	
	private void printSessions() throws IOException, ClassNotFoundException {
		// testing
		Iterator<Map.Entry<UuidKey, MonitorManagementSession>> mei = resources
				.entrySet().iterator();
		Map.Entry<UuidKey, MonitorManagementSession> entry = null;
		while (mei.hasNext()) {
			entry = mei.next();
			System.out.println("session cookie: " + entry.getKey().getId()
					+ ":" + ((MonitorSession)entry.getValue()).getInitialExertion().getName());
		}
	}
}
