package sorcer.core.provider.exertmonitor.db;

import java.io.IOException;

import sorcer.core.provider.MonitorManagementSession;
import sorcer.core.provider.exertmonitor.MonitorSession;
import sorcer.util.bdb.objects.MarshalledData;
import sorcer.util.bdb.objects.UuidKey;

import com.sleepycat.bind.EntityBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.SerialSerialBinding;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredValueSet;

/**
 * SessionDatabaseViews defines the data bindings and collection views for the
 * session database.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SessionDatabaseViews {
	
	private StoredMap<UuidKey, MonitorManagementSession>  sessionMap;

	/**
	 * Create the data bindings and collection views.
	 */
	public SessionDatabaseViews(SessionDatabase db) {
		// Create the data bindings.
		ClassCatalog catalog = db.getClassCatalog();
		SerialBinding sessionKeyBinding = new SerialBinding(catalog, UuidKey.class);
		EntityBinding sessionDataBinding = new SessionBinding(catalog,
				UuidKey.class, MarshalledData.class);
		
		sessionMap = new StoredMap(db.getSessionDatabase(),
				sessionKeyBinding, sessionDataBinding, true);
	}

	// The views returned below can be accessed using the java.util.Map or
	// java.util.Set interfaces, or using the StoredSortedMap and
	// StoredValueSet classes, which provide additional methods. The entity
	// sets could be obtained directly from the Map.values() method but
	// convenience methods are provided here to return them in order to avoid
	// down-casting elsewhere.
	/**
	 * Return a map view of the session storage container.
	 */
	public StoredMap<UuidKey, MonitorManagementSession>  getSessionMap() {
		return sessionMap;
	}
	
	/**
	 * Return an entity setValue view of the session storage container.
	 */
	public StoredValueSet<MonitorSession> getSessionSet() {
		return (StoredValueSet) sessionMap.values();
	}

	/**
	 * SessionBinding is used to bind the stored key/data entry pair to a
	 * combined data object (entity).
	 */
	public static class SessionBinding extends SerialSerialBinding {

		/**
		 * Construct the binding object.
		 */
		private SessionBinding(ClassCatalog classCatalog, Class keyClass, Class dataClass) {
			super(classCatalog, keyClass, dataClass);
		}
		
		/**
		 * Return the entity as the stored data. 
		 */
		public Object objectToData(Object object) {
			MarshalledData mSession = null;
			try {
				mSession = new MarshalledData(object);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return mSession;
		}

		/* (non-Javadoc)
		 * @see com.sleepycat.bind.serial.SerialSerialBinding#entryToObject(java.lang.Object, java.lang.Object)
		 */
		@Override
		public Object entryToObject(Object keyInput, Object object) {
			MonitorSession session = null;
			MarshalledData md = (MarshalledData)object;
			try {
				session = (MonitorSession)md.get();
				session.setCookie(((UuidKey)keyInput).getId());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return session;
		}

		/* (non-Javadoc)
		 * @see com.sleepycat.bind.serial.SerialSerialBinding#objectToKey(java.lang.Object)
		 */
		@Override
		public Object objectToKey(Object object) {
			return new UuidKey(((MonitorSession)object).getCookie());
		}
	}

}
