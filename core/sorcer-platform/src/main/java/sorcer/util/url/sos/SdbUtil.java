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

package sorcer.util.url.sos;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.co.operator;
import sorcer.core.context.ServiceContext;
import sorcer.core.provider.DatabaseStorer;
import sorcer.core.provider.DatabaseStorer.Store;
import sorcer.core.provider.DataspaceStorer;
import sorcer.core.provider.ProviderName;
import sorcer.core.provider.StorageManagement;
import sorcer.service.*;
import sorcer.util.Sorcer;
import sorcer.util.bdb.objects.SorcerDatabaseViews;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static sorcer.so.operator.*;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" } )
public class SdbUtil {
	private static final Logger logger = LoggerFactory.getLogger(SdbUtil.class.getName());

	public static boolean isSosURL(Object url) {
		if (url instanceof URL && ((URL)url).getProtocol().equals("sos"))
			return true;
		else
			return false;
	}

	public static boolean isSosIdURL(Object url) {
		if (isSosURL(url) && ((URL) url).getRef() != null)
			return true;
		else
			return false;
	}

	public static Uuid getUuid(URL url) {
		String urlRef = url.getRef();
		int index = urlRef.indexOf('=');
		// storeType = SorcerDatabaseViews.getStoreType(reference.substring(0,
		// index));
		return UuidFactory.create(urlRef.substring(index + 1));
	}

	public static Store getStoreType(URL url) {
		String urlRef = url.getRef();
		int index = urlRef.indexOf('=');
		return SorcerDatabaseViews.getStoreType(urlRef.substring(0, index));
	}

	public static String getProviderName(URL url) {
		if (url == null)
			return null;
		else
			return url.getPath().substring(1);
	}

	public static String getServiceType(URL url) {
		return url.getHost();
	}

	/**
	 * Returns a context to be used with
	 * {@link StorageManagement#contextStore(Context)}
	 *
	 * @param object
	 *            to be stored
	 * @return storage {@link Context}
	 * @throws ContextException
	 */
	static public Context getStoreContext(Object object)
			throws ContextException {
		ServiceContext cxt = new ServiceContext("store context");
		cxt.putInValue(StorageManagement.object_stored, object);
		cxt.putInValue(StorageManagement.object_uuid,
				((Identifiable) object).getId());
		cxt.setReturnPath(StorageManagement.object_url);
		return cxt;
	}

	/**
	 * Returns a context to be used with
	 *
	 * @param uuid
	 *            {@link Uuid}
	 * @param type
	 *            one of: exertion, context, var, dataTable, varModel, object
	 * @return retrieval {@link Context}
	 * @throws ContextException
	 */
	static public Context getRetrieveContext(Uuid uuid, Store type)
			throws ContextException {
		ServiceContext cxt = new ServiceContext("retrieve context");
		cxt.putInValue(StorageManagement.object_type, type);
		cxt.putInValue(StorageManagement.object_uuid, uuid);
		cxt.setReturnPath(StorageManagement.object_retrieved);
		return cxt;
	}

	static public Context getUpdateContext(Object object, URL url)
			throws ContextException {
		return getUpdateContext(object, getUuid(url));
	}

	/**
	 * Returns a context to be used with
	 * {@link StorageManagement#contextUpdate(Context)}
	 * 
	 * @param object
	 *            to be updated
	 * @param uuid
	 *            {@link Uuid} og the updated object
	 * @return update {@link Context}
	 * @throws ContextException
	 */
	static public Context getUpdateContext(Object object, Uuid uuid)
			throws ContextException {
		ServiceContext cxt = new ServiceContext("update context");
		cxt.putInValue(StorageManagement.object_uuid, uuid);
		cxt.putInValue(StorageManagement.object_updated, object);
		cxt.setReturnPath(StorageManagement.object_url);
		return cxt;
	}

	static public Context getListContext(Store storeType)
			throws ContextException {
		ServiceContext cxt = new ServiceContext("storage list context");
		cxt.putInValue(StorageManagement.store_type, storeType);
		cxt.setReturnPath(StorageManagement.store_content_list);
		return cxt;
	}

	static public URL update(Object value) throws MogramException,
			SignatureException, ContextException {
		if (!(value instanceof Identifiable)
				|| !(((Identifiable) value).getId() instanceof Uuid)) {
			throw new ContextException("Object is not Uuid Identifiable: "
					+ value);
		}
		return SdbUtil.update((Uuid) ((Identifiable) value).getId(), value);
	}

	static public URL update(URL storedURL, Object value)
			throws MogramException, SignatureException, ContextException {
		return SdbUtil.update(SdbUtil.getUuid(storedURL), value);
	}

	static public URL update(Uuid storeUuid, Object value)
			throws MogramException, SignatureException, ContextException {
		Task objectUpdateTask = task(
				"update",
				sig("contextUpdate", DatabaseStorer.class,
						prvName(Sorcer.getActualDatabaseStorerName())),
				SdbUtil.getUpdateContext(value, storeUuid));

		objectUpdateTask = exert(objectUpdateTask);
		return (URL) get(context(objectUpdateTask),
				StorageManagement.object_url);
	}

	public static int clear(Store type) throws ServiceException, SignatureException {
		String storageName = Sorcer.getActualName(Sorcer
				.getDatabaseStorerName());
		Task objectStoreTask = task(
				"clearSessions",
				sig("contextClear", DatabaseStorer.class, prvName(storageName)),
				context("clearSessions", operator.inVal(StorageManagement.store_type, type),
						result(StorageManagement.store_size)));
		return (Integer) exec(objectStoreTask);
	}

	public static int size(Store type) throws ServiceException, SignatureException {
		String storageName = Sorcer.getActualName(Sorcer
				.getDatabaseStorerName());
		Task objectStoreTask = task(
				"size",
				sig("contextSize", DatabaseStorer.class, new ProviderName(storageName)),
				context("size", operator.inVal(StorageManagement.store_type, type),
						result(StorageManagement.store_size)));
		return (Integer) exec(objectStoreTask);
	}
	
	public static URL delete(Object object) throws ServiceException, SignatureException {
		if (object instanceof URL) {
			return deleteURL((URL) object);
		} else {
			return deleteObject(object);
		}
	}

	public static URL deleteObject(Object object) throws ServiceException, SignatureException {
		String storageName = Sorcer.getActualName(Sorcer
				.getDatabaseStorerName());
		Task objectStoreTask = task(
				"delete",
				sig("contextDelete", DatabaseStorer.class, prvName(storageName)),
				context("delete", operator.inVal(StorageManagement.object_deleted, object),
						result(StorageManagement.object_url)));
		return (URL) exec(objectStoreTask);
	}

	public static URL deleteURL(URL url) throws ServiceException {
		String serviceTypeName = getServiceType(url);
		String storageName = getProviderName(url);
		Task objectStoreTask = null;
		try {
			objectStoreTask = task(
					"delete",
					sig("contextDelete", Class.forName(serviceTypeName),
							prvName(storageName)),
					context("delete",
							operator.inVal(StorageManagement.object_deleted, url),
							result(StorageManagement.object_url)));
		} catch (ClassNotFoundException | SignatureException e) {
			throw new MogramException("No such service fiType: "
					+ serviceTypeName, e);
		}
		return (URL) exec(objectStoreTask);
	}

	public static URL store(Object object) throws MogramException, SignatureException {
		String storageName = Sorcer.getDatabaseStorerName();
		Task objectStoreTask = task(
				"store",
				sig("contextStore", DatabaseStorer.class, prvName(storageName)),
				context("store", operator.inVal(StorageManagement.object_stored, object),
						result(StorageManagement.object_url)));

		Task out = exert(objectStoreTask);
		try {
			return (URL) out.getDataContext().getValue(StorageManagement.object_url);
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
	}

	public static URL write(Object object) throws ServiceException,
			SignatureException {
		String storageName = Sorcer.getActualName(Sorcer.getSpacerName());
		Task objectStoreTask = task(
				"write",
				sig("contextWrite", DataspaceStorer.class, prvName(storageName)),
				context("stored", operator.inVal(StorageManagement.object_stored, object),
						result("stored/object/url")));

		return (URL) exec(objectStoreTask);
	}

	static public Object retrieve(URL url) throws ServiceException, SignatureException {
		return retrieve(SdbUtil.getUuid(url), getStoreType(url));
	}

	static public Object retrieve(Uuid storeUuid, Store storeType)
			throws ServiceException, SignatureException {
		Task objectRetrieveTask = task(
				"retrieve",
				sig("contextRetrieve", DatabaseStorer.class,
						prvName(Sorcer.getActualDatabaseStorerName())),
				SdbUtil.getRetrieveContext(storeUuid, storeType));
		try {
			return get((Context) exec(objectRetrieveTask));
		} catch (RemoteException e) {
			throw new ExertionException(e);
		}
	}

	static public List<String> list(URL url) throws ServiceException, SignatureException {
		return list(url, null);

	}

	static public List<String> list(URL url, Store storeType)
			throws ServiceException, SignatureException {
		Store type = storeType;
		String providerName = getProviderName(url);
		if (providerName == null)
			providerName = Sorcer.getActualDatabaseStorerName();

		if (type == null) {
			type = getStoreType(url);
			if (type == null) {
				type = Store.object;
			}
		}
		Task listTask = task("list",
				sig("contextList", DatabaseStorer.class, prvName(providerName)),
				SdbUtil.getListContext(type));

		return (List<String>) exec(listTask);
	}

	static public List<String> list(Store storeType) throws ServiceException, SignatureException {
		String storageName = Sorcer.getActualName(Sorcer
				.getDatabaseStorerName());
	
		Task listTask = task("contextList",
				sig("contextList", DatabaseStorer.class, prvName(storageName)),
				SdbUtil.getListContext(storeType));

		return (List<String>) exec(listTask);
	}
}
