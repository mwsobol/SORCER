package sorcer.core.provider.exertmonitor.db;

import java.io.File;

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

/**
 * SessionDatabase defines the storage containers for the ExertMonitor database.
 * 
 * @author Mike Sobolewski
 */
public class SessionDatabase {

    private static final String CLASS_CATALOG = "java_class_catalog";
    private static final String SESSION_STORE = "sesion_store";

    private Environment env;
    private Database sessionDb;
    private StoredClassCatalog javaCatalog;

    /**
     * Open all storage containers and catalogs.
     */
    public SessionDatabase(String homeDirectory)
        throws DatabaseException {
        // Open the Berkeley DB environment in transactional mode.
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        env = new Environment(new File(homeDirectory), envConfig);

        // Set the Berkeley DB config for opening all stores.
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);

        // Create the Serial class catalog.  This holds the serialized class
        // format for all database records of serial format.
        //
        Database catalogDb = env.openDatabase(null, CLASS_CATALOG, dbConfig);
        javaCatalog = new StoredClassCatalog(catalogDb);

        // Open the Berkeley DB database for the monitor session
        // store.  The store is opened with no duplicate keys allowed.
        sessionDb = env.openDatabase(null, SESSION_STORE, dbConfig);
    }

    /**
     * Return the storage environment for the database.
     */
    public final Environment getEnvironment() {
        return env;
    }

    /**
     * Return the class catalog.
     */
    public final StoredClassCatalog getClassCatalog() {
        return javaCatalog;
    }

    /**
     * Return the monitor session storage container.
     */
    public final Database getSessionDatabase() {
        return sessionDb;
    }
    
    /**
     * Close all stores (closing a store automatically closes its indices).
     */
    public void close()
        throws DatabaseException {
        // Close secondary databases, then primary databases.
        sessionDb.close();
        // And don't forget to close the catalog and the environment.
        javaCatalog.close();
        env.close();
    }

}
