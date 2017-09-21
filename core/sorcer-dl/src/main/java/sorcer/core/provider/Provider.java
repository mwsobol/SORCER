/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

import net.jini.admin.Administrable;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import org.slf4j.Logger;
import sorcer.core.analytics.AnalyticsProducer;
import sorcer.service.Activity;
import sorcer.service.Monitorable;
import sorcer.service.Service;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.Policy;
import java.util.List;
import java.util.Properties;

/**
 * This is an interface that defines how a provider interacts with other code 
 * the through the methods that are exposed. It extends {@link Service},
 * {@link sorcer.service.Monitorable}, and {@link Remote}.
 * @see Service
 * @see Monitorable
 * @see Remote
 */
public interface Provider extends Exerter, Monitorable, AnalyticsProducer, Administrable, Remote {

	public ServiceID getProviderID() throws RemoteException;

	public String getProviderName() throws RemoteException;
	
	public Entry[] getAttributes() throws RemoteException;

	public List<Object> getProperties() throws RemoteException;

	public String getProperty(String property) throws RemoteException;

	public Properties getJavaSystemProperties() throws RemoteException;

	public boolean mutualExclusion() throws RemoteException;
	
	public String[] getGroups() throws RemoteException;

	public String getInfo() throws RemoteException;

	public String getDescription() throws RemoteException;

	public boolean isBusy() throws RemoteException;

	/**
	 * Destroy the service, if possible, including its persistent storage.
	 * 
	 * @see sorcer.core.provider.Provider#destroy()
	 */
	public void destroy() throws RemoteException;

	/**
	 * Destroy all services in this node (virtual machine) by calling each
	 * destroy().
	 * 
	 * @see sorcer.core.provider.Provider#destroy()
	 */
	public void destroyNode() throws RemoteException;
	
	public void fireEvent() throws RemoteException;

//	public void notifyInformation(Exertion task, String message)
//			throws RemoteException;
//
//	public void notifyException(Exertion task, String message, Exception e)
//			throws RemoteException;
//
//	public void notifyExceptionWithStackTrace(Exertion task, Exception e)
//			throws RemoteException;
//
//	public void notifyException(Exertion task, Exception e)
//			throws RemoteException;
//
//	public void notifyWarning(Exertion task, String message)
//			throws RemoteException;
//
//	public void notifyFailure(Exertion task, Exception e)
//			throws RemoteException;
//
//	public void notifyFailure(Exertion task, String message)
//			throws RemoteException;

	public Object getProxy() throws RemoteException;

	public void updatePolicy(Policy policy) throws RemoteException;

	public Logger getContextLogger() throws RemoteException;

	public Logger getProviderLogger() throws RemoteException;

	public Logger getRemoteLogger() throws RemoteException;
	
}
