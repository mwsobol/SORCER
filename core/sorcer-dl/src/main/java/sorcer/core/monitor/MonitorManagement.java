/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

import net.jini.core.event.RemoteEventListener;
import sorcer.core.provider.MonitorManagementSession;
import sorcer.service.Routine;
import sorcer.service.MonitorException;
import sorcer.service.Monitorable;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * 
 * An interface to be implemented by a monitoring service. It must give
 * registrations to the client for an Routine and Lease the Server side
 * resource maintained for this registration. Also it must return the exertions
 * with MonitorSessions inside it corresponding to each exertion. These monitor
 * session are sub resources for the controlling the session in the server.
 * 
 * MonitorManagement is implicitly a Remote object as MonitorSessionManagement 
 * is also a remote object
 * 
 **/

public interface MonitorManagement extends Monitorable {

    Routine register(RemoteEventListener lstnr, Routine ex, long duration) throws MonitorException,
                                                                                    RemoteException;

	boolean persist(MonitorManagementSession session) throws IOException;

}
