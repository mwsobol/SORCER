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

import java.rmi.RemoteException;


/**
 * @author Mike Sobolewski
 */
public interface SpaceContext {
	final static String spacePrefix = "_JS_";
	
	public Object writeValue(String path, Object value) throws ContextException, RemoteException;

	public Object readValue(String path) throws ContextException, RemoteException;
	
	public Object takeValue(String path) throws ContextException, RemoteException;
	
	public Object aliasValue(String path, String alias) throws ContextException, RemoteException;
	
	/**
	 * Makes JavaSpace resources unavailable for using in this ServiceContext.
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public void unshare() throws ContextException, RemoteException;
	
	/**
	 * Makes JavaSpace resources available for using in this ServiceContext.
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public void share() throws ContextException, RemoteException;
}
