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

package sorcer.util;

import java.rmi.RemoteException;

/**
 * This interface defines the remote method client can exert on the remote
 * object via a Mandate argument.
 */
public interface Mandator extends java.rmi.Remote {

	public Mandate execMandate(Mandate mandate) throws RemoteException;

	// This is Just a test Method to check the validity of this RemoteObject
	// by othe clients.
	public String getName() throws RemoteException;
}
