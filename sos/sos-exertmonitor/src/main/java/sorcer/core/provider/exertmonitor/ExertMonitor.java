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

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.id.Uuid;
import sorcer.core.UEID;
import sorcer.core.context.StrategyContext;
import sorcer.core.monitor.MonitoringManagement;
import sorcer.core.provider.MonitorManagementSession;
import sorcer.core.provider.ServiceProvider;
import sorcer.core.provider.exertmonitor.db.SessionDatabase;
import sorcer.core.provider.exertmonitor.db.SessionDatabaseViews;
import sorcer.core.provider.exertmonitor.lease.MonitorLandlord;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.*;
import sorcer.util.bdb.objects.UuidKey;

import com.sleepycat.collections.StoredMap;
import com.sleepycat.je.DatabaseException;
import com.sun.jini.start.LifeCycle;

public class ExertMonitor extends ServiceProvider implements MonitoringManagement {

	static transient final Logger logger = LoggerFactory.getLogger(ExertMonitor.class.getName());

	private MonitorLandlord landlord;

	private SessionDatabase db;
	
	private StoredMap<UuidKey, MonitorManagementSession> resources;

    private Map<Uuid, UuidKey> cacheSessionKeyMap = new HashMap<Uuid, UuidKey>();

	public ExertMonitor(String[] args, LifeCycle lifeCycle) throws Exception {
		super(args, lifeCycle);
		initMonitor();
	}

	private void initMonitor() throws Exception {
		landlord = new MonitorLandlord();
		String dbHome = getProperty("monitor.database.home");
		File dbHomeFile = null;
		if (dbHome == null || dbHome.length() == 0) {
			logger.error("Session database home missing: " + dbHome);
			destroy();
		} else {
			dbHomeFile = new File(dbHome);
			if (!dbHomeFile.isDirectory() && !dbHomeFile.exists()) {			
				boolean done = dbHomeFile.mkdirs();
				if (!done) {
					logger.error("Not able to create session database home: "
                            + dbHomeFile.getAbsolutePath());
					destroy();
				}
			}
		}
        logger.debug("Opening BDBJE environment in: " + dbHomeFile);
		db = new SessionDatabase(dbHome);
		SessionDatabaseViews views = new SessionDatabaseViews(db);
		resources = views.getSessionMap();

		// statically initialize
		MonitorSession.mLandlord = landlord;
		MonitorSession.sessionManager = (MonitoringManagement) getServiceProxy();
	}

	final Object resourcesWriteLock = new Object();

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
					resource = getSession(next.getKey()).getSessionResource(cookie);
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

	private void update(int aspect, Uuid cookie, Context ctx, StrategyContext controlContext) throws RemoteException,
			MonitorException {
		// Get the SessionResource corresponding to this cookie
		MonitorSession resource = findSessionResource(cookie);
		if (resource == null)
			throw new MonitorException("There exists no such session for: "
					+ cookie);

		resource.update(ctx, controlContext, aspect);
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

	private void done(Uuid cookie, Context ctx, StrategyContext controlContext) throws RemoteException,
			MonitorException {
		// Get the SessionResource correspoding to this cookie
		MonitorSession resource = findSessionResource(cookie);

		if (resource == null)
			throw new MonitorException("There exists no such session");
		
		resource.done(ctx, controlContext);
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
	private void failed(Uuid cookie, Context ctx, StrategyContext controlContext) throws RemoteException,
			MonitorException {
		MonitorSession resource = findSessionResource(cookie);

		if (resource == null)
			throw new MonitorException("There exists no such session");

		resource.failed(ctx, controlContext);
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
	 * @throws MonitorException
	 * 
	 */
	public Map<Uuid, ExertionInfo> getMonitorableExertionInfo(
			Exec.State state, Principal principal) throws RemoteException,
			MonitorException {
        logger.debug("Trying to get exertionInfos for: " + state.toString() + " for: "  + principal);
		Map<Uuid, ExertionInfo> table = new HashMap<Uuid, ExertionInfo>();
		try {
			if (resources==null) return table;
			Iterator<UuidKey> ki = resources.keySet().iterator();
			UuidKey key;
			while (ki.hasNext()) {
				key = ki.next();
                MonitorSession monSession = getSession(key);
                table.putAll(getMonitorableExertionInfo(monSession, key, state, principal));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MonitorException(e);
		}
		return table;
	}

    private Map<Uuid, ExertionInfo> getMonitorableExertionInfo(MonitorSession monitorSession, UuidKey key, Exec.State state, Principal principal) throws RemoteException,MonitorException {
        Map<Uuid, ExertionInfo> table = new HashMap<Uuid, ExertionInfo>();
        logger.debug("Trying to get exertionInfos for: " + monitorSession + " state: " + state.toString() + " for: "  + principal);
        ServiceExertion xrt = (ServiceExertion) (monitorSession).getRuntimeExertion();
        if (xrt.getPrincipal().getId()
                .equals(((SorcerPrincipal) principal).getId())) {
            if (state == null || state.equals(Exec.State.NULL)
                    || xrt.getStatus() == state.ordinal()) {
                table.put(xrt.getId(), new ExertionInfo(xrt, key.getId()));
            }
        }
        for (MonitorSession internalSession : monitorSession) {
            table.putAll(getMonitorableExertionInfo(internalSession, key, state, principal));
        }
        return table;
    }




	public Exertion getMonitorableExertion(Uuid id, Principal principal)
			throws RemoteException, MonitorException {
			Exertion xrt = getSession(id).getRuntimeExertion();
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
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */
	public Exertion getMonitorableExertion(UEID cookie, Principal principal)
			throws RemoteException, MonitorException {
        UuidKey lkey = cacheSessionKeyMap.get(cookie.exertionID);
        Exertion ex;
        if (lkey!=null) {
            ex = (getSession(lkey)).getRuntimeExertion();
            if (ex!=null && ((ServiceExertion) ex).getPrincipal().getId()
                    .equals(((SorcerPrincipal) principal).getId()))
                return ex;
            else
                return null;
        }
		Iterator<UuidKey> ki = resources.keySet().iterator();
		while (ki.hasNext()) {
			lkey = ki.next();
			ex = (getSession(lkey)).getRuntimeExertion();
            if (ex!=null) cacheSessionKeyMap.put(ex.getId(), lkey);
            if (cookie.exertionID.equals(ex.getId().toString())
					&& ((ServiceExertion) ex).getPrincipal().getId()
							.equals(((SorcerPrincipal) principal).getId()))
				return ex;
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
	public void update(Uuid cookie, Context ctx, StrategyContext controlContext, int aspect)
			throws RemoteException, MonitorException {
		if (aspect==Exec.UPDATED || aspect==Exec.PROVISION) {
			update(aspect, cookie, ctx, controlContext);
		} else if (aspect==Exec.DONE) {
			done(cookie, ctx, controlContext);
		} else if (aspect== Exec.FAILED) {
			failed(cookie, ctx, controlContext);
		} else
            logger.warn("Got wrong aspect to update: " + aspect);

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
		resources.put(new UuidKey(((MonitorSession)session).getCookie()), session);
		return true;
	}
	
	public MonitorSession getSession(UuidKey key) throws MonitorException {
		try {
			return (MonitorSession) resources.get(key);
		} catch (Exception e) {
			throw new MonitorException(e);
		}
	}

	public MonitorSession getSession(Uuid key) throws MonitorException {
		try {
			return (MonitorSession) resources.get(new UuidKey(key));
		} catch (Exception e) {
			throw new MonitorException(e);
		}
	}
}
