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

package sorcer.core.provider;

import java.io.InvalidObjectException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

@SuppressWarnings("rawtypes")
public interface StorageManagement {

	// paths used in storage contexts
	final static String object_stored = "object/stored";
	final static String object_retrieved = "object/retrieved";
	final static String object_updated = "object/updated";
	final static String object_deleted = "object/deleted";
	final static String object_url = "object/url";
	final static String object_uuid = "object/uuid";
	final static String object_type = "object/type";
	final static String store_type = "store/type";
	final static String store_size = "store/size";
	final static String store_content_list = "store/content/list";

	public Context contextRetrieve(Context context) throws RemoteException,
			ContextException;

	public Context contextStore(Context context) throws RemoteException,
			ContextException, MalformedURLException;

	public Context contextUpdate(Context context) throws RemoteException,
			ContextException, MalformedURLException, InvalidObjectException;

	public Context contextDelete(Context context) throws RemoteException,
			ContextException, MalformedURLException;

	public Context contextClear(Context context) throws RemoteException,
			ContextException, MalformedURLException;

	public Context contextList(Context context) throws RemoteException,
			ContextException, MalformedURLException;

	public Context contextRecords(Context context) throws RemoteException,
			ContextException, MalformedURLException;

	public Context contextSize(Context context) throws RemoteException,
			ContextException, MalformedURLException;

}
