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

package sorcer.core.provider.dbp;

import java.io.File;
import java.io.InvalidObjectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.jini.config.Configuration;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.provider.DatabaseStorer;
import sorcer.core.provider.ServiceProvider;
import sorcer.core.provider.StorageManagement;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.Identifiable;
import sorcer.service.modeling.Variability;
import sorcer.service.modeling.VariabilityModeling;
import sorcer.util.ModelTable;
import sorcer.util.bdb.objects.SorcerDatabase;
import sorcer.util.bdb.objects.SorcerDatabaseViews;
import sorcer.util.bdb.objects.UuidKey;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.Handler;
import sorcer.util.url.sos.SdbUtil;

import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredValueSet;
import com.sleepycat.je.DatabaseException;
import com.sun.jini.start.LifeCycle;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class DatabaseProvider extends ServiceProvider implements DatabaseStorer {

	static {
		Handler.register();
	}

	private SorcerDatabase db;

	private SorcerDatabaseViews views;
	
	public DatabaseProvider() throws RemoteException {
		// do nothing
	}

	/**
	 * Constructs an instance of the SORCER Object Store implementing
	 * EvaluationRemote. This constructor is required by Jini 2 life cycle
	 * management.
	 * 
	 * @param args
	 * @param lifeCycle
	 * @throws Exception
	 */
	public DatabaseProvider(String[] args, LifeCycle lifeCycle) throws Exception {
		super(args, lifeCycle);
		setupDatabase();
	}
	
	public Uuid store(Object object) {
		Object obj = object;
		if (!(object instanceof Identifiable)) {
			obj = new UuidObject(object);
		}
		PersistThread pt = new PersistThread(obj);
		pt.start();
		return pt.getUuid();	
	}
	
	public Uuid update(Uuid uuid, Object object) throws InvalidObjectException {
		Object uuidObject = object;
		if (!(object instanceof Identifiable)) {
			uuidObject = new UuidObject(uuid, object);
		}
			UpdateThread ut = new UpdateThread(uuid, uuidObject);
			ut.start();
			return ut.getUuid();
	}
	
	public Uuid update(URL url, Object object) throws InvalidObjectException {
		Object uuidObject = object;
		if (!(object instanceof Identifiable)) {
			uuidObject = new UuidObject(SdbUtil.getUuid(url), object);
		}
		UpdateThread ut = new UpdateThread(url, uuidObject);
		ut.start();
		return ut.getUuid();
	}
	
	public Object getObject(Uuid uuid) {
		StoredMap<UuidKey, UuidObject> uuidObjectMap = views.getUuidObjectMap();
		UuidObject uuidObj = uuidObjectMap.get(new UuidKey(uuid));
		return uuidObj.getObject();
	}
	
	public Context getContext(Uuid uuid) {
		StoredMap<UuidKey, Context> cxtMap = views.getContextMap();
		return cxtMap.get(new UuidKey(uuid));
	}
	
	public ModelTable getTable(Uuid uuid) {
		StoredMap<UuidKey, ModelTable> tableMap = views.getTableMap();
		return tableMap.get(new UuidKey(uuid));
	}
	
	public Exertion getExertion(Uuid uuid) {
		StoredMap<UuidKey, Exertion> xrtMap = views.getExertionMap();
		return xrtMap.get(new UuidKey(uuid));
	}
	
	protected class PersistThread extends Thread {

		Object object;
		Uuid uuid;

		public PersistThread(Object object) {
			this.object = object;
			this.uuid = (Uuid)((Identifiable)object).getId();
		}

		public void run() {
			StoredValueSet storedSet = null;
			if (object instanceof Context) {
				storedSet = views.getContextSet();
				storedSet.add(object);
			} else if (object instanceof Exertion) {
				storedSet = views.getExertionSet();
				storedSet.add(object);
			} else if (object instanceof ModelTable) {
				storedSet = views.getTableSet();
				storedSet.add(object);
			} else if (object instanceof UuidObject) {
				storedSet = views.getUuidObjectSet();
				storedSet.add(object);
			}
		}
		
		public Uuid getUuid() {
			return uuid;
		}
	}

	protected class UpdateThread extends Thread {

		Object object;
		Uuid uuid;
		
		public UpdateThread(Uuid uuid, Object object) throws InvalidObjectException {
			this.uuid = uuid;
			this.object = object;
		}

		public UpdateThread(URL url, Object object) throws InvalidObjectException {
			this.object = object;
			this.uuid = SdbUtil.getUuid(url);
		}
		
		public void run() {
			StoredMap storedMap = null;
			if (object instanceof Context) {
				storedMap = views.getContextMap();
				storedMap.replace(new UuidKey(uuid), object);
			} else if (object instanceof Exertion) {
				storedMap = views.getExertionMap();
				storedMap.replace(new UuidKey(uuid), object);
			} else if (object instanceof ModelTable) {
				storedMap = views.getTableMap();
				storedMap.replace(new UuidKey(uuid), object);
			} else if (object instanceof Object) {
				storedMap = views.getUuidObjectMap();
				storedMap.replace(new UuidKey(uuid), object);
			}
		}
		
		public Uuid getUuid() {
			return uuid;
		}
	}
	
	protected class DeleteThread extends Thread {

		Uuid uuid;
		Store storeType;
		
		public DeleteThread(Uuid uuid, Store storeType) {
			this.uuid = uuid;
			this.storeType = storeType;
		}

		public void run() {
			StoredMap storedMap = getStoredMap(storeType);
			storedMap.remove(new UuidKey(uuid));
		}
		
		public Uuid getUuid() {
			return uuid;
		}
	}
	
	public Context contextStore(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Object object = context.getValue(object_stored);		
		Uuid uuid = store(object);
		Store type = getStoreType(object);
		URL sdbUrl = getDatabaseURL(type, uuid);
		if (((ServiceContext)context).getReturnPath() != null)
			context.putOutValue(((ServiceContext)context).getReturnPath().path, sdbUrl);

		context.putOutValue(object_url, sdbUrl);
		context.putOutValue(store_size, getStoreSize(type));
		
		return context;
	}

	public URL getDatabaseURL(Store storeType, Uuid uuid) throws MalformedURLException {
		String pn = null;
		try {
			pn = getProviderName();
		} catch (RemoteException e) {
			// ignore it, local call
		}
		if (pn == null || pn.length() == 0 || pn.equals("*"))
			pn = "";
		else
			pn = "/" + pn;
		return new URL("sos://" + delegate.getPublishedServiceTypes()[0].getName() + pn + "#"
				+ storeType + "=" + uuid);
	}
	
	public URL getSdbUrl() throws MalformedURLException, RemoteException {
		String pn = getProviderName();
		if (pn == null || pn.length() == 0 || pn.equals("*"))
			pn = "";
		else
			pn = "/" + pn;
		return new URL("sos://" + delegate.getPublishedServiceTypes()[0].getName() + pn);
	}
	
	public int size(Store storeType) {
		StoredValueSet storedSet = getStoredSet(storeType);
		return storedSet.size();
	}
	
	public Uuid deleteURL(URL url) {
		Store storeType = SdbUtil.getStoreType(url);
		Uuid id = SdbUtil.getUuid(url);
		DeleteThread dt = new DeleteThread(id, storeType);
		dt.start();
		id = dt.getUuid();
		return id;
	}

	public Object retrieve(URL url) {
		Store storeType = SdbUtil.getStoreType(url);
		Uuid uuid = SdbUtil.getUuid(url);
		return retrieve(uuid, storeType);
	}
	
	public Object retrieve(Uuid uuid, Store storeType) {
		Object obj = null;
		if (storeType == Store.context)
			obj = getContext(uuid);
		else if (storeType == Store.exertion)
			obj = getExertion(uuid);
		else if (storeType == Store.table)
			obj = getTable(uuid);
		else if (storeType == Store.object)
			obj = getObject(uuid);
		
		return obj;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.core.StorageMangement#rertieve(sorcer.service.Context)
	 */
	@Override
	public Context contextRetrieve(Context context) throws RemoteException,
			ContextException {
		Store storeType = (Store) context.getValue(object_type);
		Uuid uuid = null;
		Object id = context.getValue(object_uuid);
			if (id instanceof String) {
				uuid = UuidFactory.create((String)id);
			} else if (id instanceof Uuid) {
				uuid = (Uuid)id;
			} else {
				throw new ContextException("No valid stored object Uuid: " + id);
			}
				
		Object obj = retrieve(uuid, storeType);
		if (((ServiceContext)context).getReturnPath() != null)
			context.putOutValue(((ServiceContext)context).getReturnPath().path, obj);
		
		// default returned path
		context.putOutValue(object_retrieved, obj);
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.StorageManagement#update(sorcer.service.Context)
	 */
	@Override
	public Context contextUpdate(Context context) throws RemoteException,
			ContextException, MalformedURLException, InvalidObjectException {
		Object object = context.getValue(object_updated);
		Object id = context.getValue(object_uuid);
		Uuid uuid = null;
		if (id instanceof String) {
			uuid = UuidFactory.create((String)id);
		} else if (id instanceof Uuid) {
			uuid = (Uuid)id;
		} else {
			throw new ContextException("Wrong update object Uuid: " + id);
		}
		uuid = update(uuid, object);
		Store type = getStoreType(object);
		URL sdbUrl = getDatabaseURL(type, uuid);
		if (((ServiceContext)context).getReturnPath() != null)
			context.putOutValue(((ServiceContext)context).getReturnPath().path, sdbUrl);

		context.putOutValue(object_url, sdbUrl);
		context.remove(object_updated);
		context.putOutValue(store_size, getStoreSize(type));
		return context;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.core.StorageManagement#contextList(sorcer.service.Context)
	 */
	@Override
	public Context contextList(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		List<String> content = list((Store)context.getValue(StorageManagement.store_type));
		context.putValue(StorageManagement.store_content_list, content);
		return context;
	}
	
	public List<String> list(Store storeType) {
		StoredValueSet storedSet = getStoredSet(storeType);
		List<String> contents = new ArrayList<String>(storedSet.size());
		Iterator it = storedSet.iterator();
		while(it.hasNext()) {
			contents.add(it.next().toString());
		}
		return contents;
	}
	
	public List<String> list(URL url) {
		return list(SdbUtil.getStoreType(url));
	}

	/* (non-Javadoc)
	 * @see sorcer.core.StorageManagement#contextClear(sorcer.service.Context)
	 */
	@Override
	public Context contextClear(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Store type = (Store)context.getValue(StorageManagement.store_type);
		context.putValue(store_size, clear(type));
		return context;
	}
	
	public int clear(Store type) throws RemoteException,
			ContextException, MalformedURLException {
		StoredValueSet storedSet = getStoredSet(type);
		int size = storedSet.size();
		storedSet.clear();
		return size;
	}
	
	protected void setupDatabase() throws DatabaseException, RemoteException {
		Configuration config = delegate.getDeploymentConfig();
		String dbHome = null;
		try {
			dbHome = (String) config.getEntry(ServiceProvider.COMPONENT,
					DB_HOME, String.class);
		} catch (Exception e) {
			// do nothing, default value is used
		}
		logger.info("dbHome: " + dbHome);
		if (dbHome == null || dbHome.length() == 0) {
			logger.info("No provider's database created");
			destroy();
			return;
		}

		File dbHomeFile = null;
		dbHomeFile = new File(dbHome);
		if (!dbHomeFile.isDirectory() && !dbHomeFile.exists()) {
			boolean done = dbHomeFile.mkdirs();
			if (!done) {
				logger.severe("Not able to create session database home: "
						+ dbHomeFile.getAbsolutePath());
				destroy();
				return;
			}
		}
		System.out.println("Opening provider's BDBJE in: "
				+ dbHomeFile.getAbsolutePath());
		db = new SorcerDatabase(dbHome);
		views = new SorcerDatabaseViews(db);
	}
	
	/**
	 * Destroy the service, if possible, including its persistent storage.
	 * 
	 * @see sorcer.core.provider.base.Provider#destroy()
	 */
	@Override
	public void destroy() throws RemoteException {
		try {
			if (db != null) {
				db.close();
			}
		} catch (DatabaseException e) {
			logger.severe("Failed to close provider's databse: "
					+ e.getMessage());
		}
		super.destroy();
	}

	/* (non-Javadoc)
	 * @see sorcer.core.StorageManagement#delete(sorcer.service.Context)
	 */
	@Override
	public Context contextDelete(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Object deletedObject = context
				.getValue(StorageManagement.object_deleted);
		if (deletedObject instanceof URL) {
			context.putValue(StorageManagement.object_url, (URL)deletedObject);
		} else {
			Uuid id = (Uuid) ((Identifiable) deletedObject).getId();
			context.putValue(StorageManagement.object_url,
					getDatabaseURL(getStoreType(deletedObject), id));
		}
		delete(deletedObject);
		return context;
	}
	
	public StoredMap getStoredMap(Store storeType) {
		StoredMap storedMap = null;
		if (storeType == Store.context) {
			storedMap = views.getContextMap();
		} else if (storeType == Store.exertion) {
			storedMap = views.getExertionMap();
		} else if (storeType == Store.table) {
			storedMap = views.getTableMap();
		} else if (storeType == Store.object) {
			storedMap = views.getUuidObjectMap();
		}
		return storedMap;
	}
	
	public StoredValueSet getStoredSet(Store storeType) {
		StoredValueSet storedSet = null;
		if (storeType == Store.context) {
			storedSet = views.getContextSet();
		} else if (storeType == Store.exertion) {
			storedSet = views.getExertionSet();
		} else if (storeType == Store.table) {
			storedSet = views.getTableSet();
		} else if (storeType == Store.object) {
			storedSet = views.getUuidObjectSet();
		}
		return storedSet;
	}
	
	public Uuid delete(Object object) {
		if (object instanceof URL) {
			return deleteURL((URL)object);
		} else if (object instanceof Identifiable) 
			return deleteIdentifiable(object);
		return null;
	}
	
	public Uuid deleteIdentifiable(Object object) {
		Uuid id = (Uuid) ((Identifiable) object).getId();
		DeleteThread dt = null;
		if (object instanceof Context) {
			dt = new DeleteThread(id, Store.context);
		} else if (object instanceof Exertion) {
			dt = new DeleteThread(id, Store.exertion);
		} else if (object instanceof Variability) {
			dt = new DeleteThread(id, Store.var);
		} else if (object instanceof VariabilityModeling) {
			dt = new DeleteThread(id, Store.varmodel);
		} else if (object instanceof ModelTable) {
			dt = new DeleteThread(id, Store.table);
		} else {
			dt = new DeleteThread(id, Store.object);			
		}
		dt.start();
		id = dt.getUuid();
		return id;
	}
	
	private int getStoreSize(Store type) {
		if (type == Store.context) {
			return views.getContextSet().size();
		} else if (type == Store.exertion) {
			return views.getExertionSet().size();
		} else if (type == Store.table) {
			return views.getTableSet().size();
		} else {
			return views.getUuidObjectSet().size();
		}
	}
	
	private Store getStoreType(Object object) {
		Store type = Store.object;
		if (object instanceof Context) {
			type = Store.context;
		} else if (object instanceof Exertion) {
			type = Store.exertion;
		} else if (object instanceof Variability) {
			type = Store.var;
		} else if (object instanceof VariabilityModeling) {
			type = Store.varmodel;
		} else if (object instanceof ModelTable) {
			type = Store.table;
		}
		return type;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.StorageManagement#contextSize(sorcer.service.Context)
	 */
	@Override
	public Context contextSize(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Store type = (Store)context.getValue(StorageManagement.store_type);
		if (((ServiceContext)context).getReturnPath() != null)
			context.putOutValue(((ServiceContext)context).getReturnPath().path, getStoreSize(type));
		context.putOutValue(store_size, getStoreSize(type));
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.StorageManagement#contextRecords(sorcer.service.Context)
	 */
	@Override
	public Context contextRecords(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		// TODO Auto-generated method stub
		return null;
	}

}
