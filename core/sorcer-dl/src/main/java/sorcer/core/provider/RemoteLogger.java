/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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
package sorcer.core.provider;

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.LeaseDeniedException;
import sorcer.core.provider.logger.LoggingConfig;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.LoggingEventVO;

public interface RemoteLogger extends Remote {

    String LOGGER_CONTEXT_KEY = "SORCER-REMOTE-CALL";
    String KEY_HOSTNAME = "hostname";
    String KEY_MOGRAM_ID = "mogId";
    String KEY_PROVIDER_ID = "prvId";

    public void publish(List<LoggingEventVO> record) throws RemoteException;

    public List<LoggingConfig> getLoggers() throws IOException;

    public String[] getLogNames() throws RemoteException;

    public List<String> getLog(String fileName) throws RemoteException;

    public void deleteLog(String logName) throws RemoteException;


    public EventRegistration registerLogListener(RemoteEventListener listener,
                                      MarshalledObject handback, long duration, List<Map<String, String>> filterMapList)
            throws LeaseDeniedException, RemoteException;

    public void unregisterLogListener(EventRegistration evReg) throws RemoteException;

}