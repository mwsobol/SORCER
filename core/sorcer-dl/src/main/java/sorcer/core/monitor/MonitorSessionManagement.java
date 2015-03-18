/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 Sorcersoft.com S.A.
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
package sorcer.core.monitor;

import net.jini.core.lease.Lease;
import net.jini.core.transaction.server.TransactionConstants;
import net.jini.id.Uuid;
import sorcer.core.context.IControlContext;
import sorcer.service.Monitorable;
import sorcer.service.Context;
import sorcer.service.MonitorException;

import java.rmi.RemoteException;


/**
 * A server side session manager. Clients call on session manager to manage
 * individual sessions data.
 * 
 * The session is activated by the manager when init(monitorable, duration,
 * timeout) is called by broker. From this moment, the Manager will require that
 * the monitorable renew the leases for the specified duration. If the task is
 * not complete for the specified duration, the monitor will send stop signal to
 * the monitorable and notify the Broker via RemoteEventListener.notify().
 * 
 * For Space based programming, since we don't know who will execute the job,
 * broker calls init(space, timeout, duration). This means that the monitor is
 * going to mark the timeout and duration of this session. Note that from this
 * moment, the session is not activated. It waits till the timeout period for
 * some monitorable provider to pick up and call init(monitorable). From this
 * moment on, the session is activated and leases have to be renewed.
 * 
 * Once providers revieves the session (which is normally sent inside the
 * exertion) They may call update() stop() getLease() or getState(). The
 * getState is actually delegated to the SessionManager remotely.
 * 
 **/

public interface MonitorSessionManagement extends MonitorManagement {
	
	/**
	 * Makes this an active session. The rendezvous decides the lease duration and
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
	 *             1) If this session is already active 2) If there is no such
	 *             session
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 * @see TransactionConstants
	 */

	public Lease init(Uuid cookie, Monitorable mntrbl, long duration,
			long timeout) throws RemoteException, MonitorException;

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
	 * */

	public void init(Uuid cookie, long duration, long timeout)
			throws RemoteException, MonitorException;

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
	 * */

	public Lease init(Uuid cookie, Monitorable mntrbl) throws RemoteException,
			MonitorException;

	/**
	 * Providers use this method to update the state of monitorable exertion.
	 * 
	 * @param ctx
	 *            The service dataContext of monitorable exertion.
	 *            
	 * @param aspect
	 *            The aspect of dataContext change.
	 * 
	 * @throws MonitorException
	 *             1) If there is no such session 2) The dataContext does not
	 *             belong to this session
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 **/
	
	public void update(Uuid cookie, Context ctx, IControlContext controlContext, int aspect)
			throws RemoteException, MonitorException;
	
	public int getState(Uuid cookie) throws RemoteException,
			MonitorException;

}
