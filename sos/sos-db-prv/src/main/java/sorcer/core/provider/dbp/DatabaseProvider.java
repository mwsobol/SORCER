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

import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredValueSet;
import com.sleepycat.je.DatabaseException;
import com.sun.jini.start.LifeCycle;
import net.jini.config.Configuration;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.provider.DatabaseStorer;
import sorcer.core.provider.ServiceExerter;
import sorcer.core.provider.StorageManagement;
import sorcer.service.*;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Variability;
import sorcer.util.ModelTable;
import sorcer.util.bdb.objects.SorcerDatabase;
import sorcer.util.bdb.objects.SorcerDatabaseViews;
import sorcer.util.bdb.objects.UuidKey;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.Handler;
import sorcer.util.url.sos.SdbUtil;

import java.io.File;
import java.io.InvalidObjectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DatabaseProvider extends ServiceExerter implements DatabaseStorer {
    static final Logger logger = LoggerFactory.getLogger(DatabaseProvider.class);
	static {
		Handler.register();
	}

	private SorcerDatabase db;

	private SorcerDatabaseViews views;

	public DatabaseProvider() throws RemoteException {
		super();
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

    private Set<Uuid> objectsQueue = Collections.synchronizedSet(new HashSet<Uuid>());

	public Uuid store(Object object) {
		Object obj = object;
//		if (!(object instanceof Identifiable)) {
			obj = new UuidObject(object);
//		}
        PersistThread pt = new PersistThread(obj);
		Uuid id = pt.getUuid();
        pt.start();
        return id;
	}

	public Uuid update(Uuid uuid, Object object) throws InvalidObjectException {
//		logger.info("Updating uuid object: " + uuid);
		Object uuidObject = object;
		if (!(object instanceof Identifiable)) {
			uuidObject = new UuidObject(uuid, object);
		}
		UpdateThread ut = new UpdateThread(uuid, uuidObject);
        Uuid id = ut.getUuid();
        ut.start();
        return id;
	}

	public Uuid updateObject(URL url, Object object) throws InvalidObjectException {
//		logger.info("Updating url object: " + url);
		Object uuidObject = object;
		if (!(object instanceof Identifiable)) {
			uuidObject = new UuidObject(SdbUtil.getUuid(url), object);
		}
		UpdateThread ut = new UpdateThread(url, uuidObject);
        Uuid id = ut.getUuid();
        ut.start();
		return id;
	}

    private void append(Uuid id, Object object) {
        waitIfBusy(id);
        objectsQueue.add(id);
//		logger.info("new waiting: " + object + " id: " + id + " size: " + objectsQueue.size());
	}

    private void waitIfBusy(Uuid uuid) {
        while (objectsQueue.contains(uuid)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                logger.warn("Interrupted while waiting for retrieved object: " + uuid);
            }
        }
    }

    private synchronized Set<Uuid> getCurrentBusy(){
        return new HashSet<Uuid>(objectsQueue);
    }

    public void waitIfBusy() {
        Set<Uuid> currentlyBusy = getCurrentBusy();
        while (!currentlyBusy.isEmpty()) {
            try {
                currentlyBusy.retainAll(objectsQueue);
//                logger.info("currentlyBusy size: " + currentlyBusy.size());
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                logger.warn("Interrupted while busy :" + currentlyBusy.size());
            }
        }
    }

	public Object getObject(Uuid uuid) {
//		logger.info("Getting object: " + uuid);
		waitIfBusy(uuid);
		StoredMap<UuidKey, UuidObject> uuidObjectMap = views.getUuidObjectMap();

		int tries = 0;
		while (uuidObjectMap == null && tries < 100) {
			logger.info("Didn't getValue UuidObjectMap, trying: " + tries);
			try {
				Thread.sleep(50);
			} catch (InterruptedException ie) {
			}
			views.getUuidObjectMap();
			tries++;
		}

		UuidObject uuidObj = uuidObjectMap.get(new UuidKey(uuid));
		return uuidObj != null ? uuidObj.getObject() : null;
	}

	public Context getContext(Uuid uuid) {
        try {
            append(uuid, "context");
            StoredMap<UuidKey, Context> cxtMap = views.getContextMap();
            return cxtMap.get(new UuidKey(uuid));
        } finally {
            objectsQueue.remove(uuid);
        }
	}

	public Routine getExertion(Uuid uuid) {
        try {
            append(uuid, "exertion");
            StoredMap<UuidKey, Routine> xrtMap = views.getRoutineMap();
		    return xrtMap.get(new UuidKey(uuid));
        } finally {
            objectsQueue.remove(uuid);
        }
	}

    public ModelTable getTable(Uuid uuid) {
        try {
            append(uuid, "dataTable");
            StoredMap<UuidKey, ModelTable> xrtMap = views.getTableMap();
            return xrtMap.get(new UuidKey(uuid));
        } finally {
            objectsQueue.remove(uuid);
        }
    }

	protected class PersistThread extends Thread {

		Object object;
		Uuid uuid;

		public PersistThread(Object object) {
            super("PersistThread-" + ((Identifiable)object).getId());
			this.object = object;
			this.uuid = ((UuidObject)object).getId();
			append(uuid, object);
		}

		@SuppressWarnings("unchecked")
		public void run() {
			try {
//				logger.info("persisting: " + object);
				StoredValueSet storedSet = views.getUuidObjectSet();
				storedSet.add(object);
//				TODO
//				Object inner = ((UuidObject)object).getObject();
//				if (inner instanceof Context) {
//					storedSet = views.getContextSet();
//					storedSet.add(object);
//				} else if (inner instanceof Routine) {
//					storedSet = views.getRoutineSet();
//					storedSet.add(object);
//				} else if (inner instanceof ModelTable) {
//					storedSet = views.getTableSet();
//					storedSet.add(object);
//				} else if (inner instanceof UuidObject) {
//					storedSet = views.getUuidObjectSet();
//					storedSet.add(object);
//				}
			} finally {
				objectsQueue.remove(this.uuid);
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
            super("UpdateThread-" + url);
			this.object = object;
			this.uuid = SdbUtil.getUuid(url);
            append(uuid, object);
		}

		public void run() {
            StoredMap storedMap = null;
            UuidKey key = null;
			try {
                key = new UuidKey(uuid);
                if (object instanceof Context) {
                    storedMap = views.getContextMap();
                    storedMap.replace(key, object);
                } else if (object instanceof Routine) {
                    storedMap = views.getRoutineMap();
                    storedMap.replace(key, object);
                } else if (object instanceof ModelTable) {
                    storedMap = views.getTableMap();
                    storedMap.replace(key, object);
                } else if (object instanceof Object) {
                    storedMap = views.getUuidObjectMap();
                    storedMap.replace(key, object);
                }
            } catch (IllegalArgumentException ie) {
                logger.warn("Problem updating object with key: " + key.toString()
						+ "\n" + storedMap.get(key).toString());
                objectsQueue.remove(this.uuid);
                throw (ie);
            } finally {
                objectsQueue.remove(this.uuid);
            }
		}

		public Uuid getUuid() {
			return uuid;
		}
	}

	protected class DeleteThread extends Thread {

		Uuid uuid;
		Store storeType;
        StoredMap storedMap;

        public DeleteThread(Uuid uuid, Store storeType) {
            super("DeleteThread-" + uuid);
            this.uuid = uuid;
			this.storeType = storeType;
            storedMap = getStoredMap(storeType);
            append(uuid, "deleteThread");
		}

		public void run() {
            try {
                storedMap.remove(new UuidKey(uuid));
            } finally {
                objectsQueue.remove(this.uuid);
            }
		}

		public Uuid getUuid() {
			return uuid;
		}
	}

	public Context contextStore(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Object object = context.asis(object_stored);
		Uuid uuid = store(object);
		Store type = getStoreType(object);
		URL sdbUrl = getDatabaseURL(type, uuid);
		if (((ServiceContext)context).getContextReturn() != null)
			context.putOutValue(((ServiceContext)context).getContextReturn().returnPath, sdbUrl);

		context.putOutValue(object_url, sdbUrl);
		context.putOutValue(store_size, getStoreSize(type));

		return context;
	}

	public URL getDatabaseURL(Store storeType, Uuid uuid) throws MalformedURLException, RemoteException {
		String pn = getProviderName();
		if (pn == null || pn.length() == 0 || pn.equals("*"))
			pn = "";
		else
			pn = "/" + pn;
		return new URL("sos://" + DatabaseStorer.class.getName() + pn + "#"
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

	public Uuid deleteObject(URL url) {
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
//		return retrieve(uuid, storeType);
		//TODO
		return retrieve(uuid, Store.object);

	}
	
	public Object retrieve(Uuid uuid, Store storeType) {
		Object obj = null;
//		TODO
//		if (storeType == Store.context)
//			obj = getContext(uuid);
//		else if (storeType == Store.exertion)
//			obj = getMogram(uuid);
//        else if (storeType == Store.dataTable)
//            obj = getTable(uuid);
//        else if (storeType == Store.object)
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
//		TODO
//		Object obj = retrieve(uuid, storeType);
		Object obj = retrieve(uuid, Store.object);
		if (((ServiceContext)context).getContextReturn() != null)
			context.putOutValue(((ServiceContext)context).getContextReturn().returnPath, obj);
		
		// default returned reqestPath
		context.putOutValue(object_retrieved, obj);
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.StorageManagement#update(sorcer.service.Context)
	 */
	@Override
	public Context contextUpdate(Context context) throws RemoteException,
			ContextException {
		Object object = context.getValue(object_updated);
		Object id = context.getValue(object_uuid);
		Uuid uuid = null;
		if (id instanceof String) {
			uuid = UuidFactory.create((String)id);
		} else if (id instanceof Uuid) {
			uuid = (Uuid)id;
		} else {
			throw new ContextException("Wrong object Uuid: " + id);
		}
		URL sdbUrl = null;
		Store type = getStoreType(object);
		try {
            uuid = update(uuid, object);
		 	sdbUrl = getDatabaseURL(type, uuid);
		} catch (Exception e) {
			throw new ContextException(e);
		}
		if (((ServiceContext)context).getContextReturn() != null)
			context.putOutValue(((ServiceContext)context).getContextReturn().returnPath, sdbUrl);

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
		List<String> content = list((Store) context.getValue(StorageManagement.store_type));
		context.putValue(StorageManagement.store_content_list, content);
		return context;
	}
	
	public List<String> list(Store storeType) {
		StoredValueSet storedSet = getStoredSet(storeType);
        logger.info("list got storedSet size: " + storedSet.size());
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
			dbHome = (String) config.getEntry(ServiceExerter.COMPONENT,
					DB_HOME, String.class);
		} catch (Exception e) {
			// do nothing, default eval is used
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
				logger.warn("Not able to create session database home: "
                         + dbHomeFile.getAbsolutePath());
				destroy();
				return;
			}
		}
		logger.info("Opening provider's BDBJE in: " + dbHomeFile.getAbsolutePath());
		db = new SorcerDatabase(dbHome);
		views = new SorcerDatabaseViews(db);
	}
	
	/**
	 * Destroy the service, if possible, including its persistent storage.
	 * 
	 * @see Exerter#destroy()
	 */
	public void destroy() {
		try {
            int tries=0;
            try {
                while (objectsQueue.size()>0 && tries<80) {
                    Thread.sleep(50);
                    tries++;
                }
                if (tries==80) logger.warn("Interrupted while objects where still being used; size: "
                + objectsQueue.size());
            } catch (InterruptedException ie) {}
			if (db != null) {
				db.close();
			}
		} catch (DatabaseException e) {
			logger.error("Failed to close provider's database: " +
                    e.getMessage());
		}
		super.destroy();
	}

	/* (non-Javadoc)
	 * @see sorcer.core.provider.StorageManagement#deleteObject(sorcer.service.Context)
	 */
	@Override
	public Context contextDelete(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Object deletedObject = context
				.getValue(StorageManagement.object_deleted);
		if (deletedObject instanceof URL) {
			context.putValue(StorageManagement.object_url, deletedObject);
		} else {
			Uuid id = (Uuid) ((Identifiable) deletedObject).getId();
			context.putValue(StorageManagement.object_url,
					getDatabaseURL(getStoreType(deletedObject), id));
		}
		deleteObject(deletedObject);
		return context;
	}
	
	public StoredMap getStoredMap(Store storeType) {
        waitIfBusy();
		StoredMap storedMap = null;
		if (storeType == Store.context) {
			storedMap = views.getContextMap();
		} else if (storeType == Store.exertion) {
			storedMap = views.getRoutineMap();
		} else if (storeType == Store.table) {
            storedMap = views.getTableMap();
        } else if (storeType == Store.object) {
			storedMap = views.getUuidObjectMap();
		}
		return storedMap;
	}
	
	public StoredValueSet getStoredSet(Store storeType) {
        waitIfBusy();
		StoredValueSet storedSet = null;
		if (storeType == Store.context) {
			storedSet = views.getContextSet();
		} else if (storeType == Store.exertion) {
			storedSet = views.getRoutineSet();
		} else if (storeType == Store.table) {
            storedSet = views.getTableSet();
        } else if (storeType == Store.object) {
			storedSet = views.getUuidObjectSet();
		}
		return storedSet;
	}
	
	public Uuid deleteObject(Object object) {
		if (object instanceof URL) {
			return deleteObject((URL)object);
		} else if (object instanceof Identifiable) 
			return deleteIdentifiable(object);
		return null;
	}
	
	public Uuid deleteIdentifiable(Object object) {
		Uuid id = (Uuid) ((Identifiable) object).getId();
		DeleteThread dt = null;
		if (object instanceof Context) {
			dt = new DeleteThread(id, Store.context);
		} else if (object instanceof Routine) {
			dt = new DeleteThread(id, Store.exertion);
		} else if (object instanceof Functionality) {
			dt = new DeleteThread(id, Store.var);
		} else if (object instanceof Variability) {
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
        waitIfBusy();
		if (type == Store.context) {
			return views.getContextSet().size();
		} else if (type == Store.exertion) {
			return views.getRoutineSet().size();
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
		} else if (object instanceof Routine) {
			type = Store.exertion;
		} else if (object instanceof Functionality) {
			type = Store.var;
		} else if (object instanceof Variability) {
			type = Store.varmodel;
		} else if (object instanceof ModelTable) {
            type = Store.table;
        }
        return type;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.provider.StorageManagement#contextSize(sorcer.service.Context)
	 */
	@Override
	public Context contextSize(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Store type = (Store)context.getValue(StorageManagement.store_type);
		if (((ServiceContext)context).getContextReturn() != null)
			context.putOutValue(((ServiceContext)context).getContextReturn().returnPath, getStoreSize(type));
		context.putOutValue(store_size, getStoreSize(type));
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.provider.StorageManagement#contextRecords(sorcer.service.Context)
	 */
	@Override
	public Context contextRecords(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		return null;
	}

    @Override
    public URL storeObject(Object object) throws RemoteException{
        Uuid uuid = store(object);
        Store type = getStoreType(object);
        try {
            return getDatabaseURL(type, uuid);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Couldn't compute object URL", e);
        }
    }

}

