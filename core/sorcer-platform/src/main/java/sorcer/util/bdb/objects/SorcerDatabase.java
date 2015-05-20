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

package sorcer.util.bdb.objects;

import java.io.File;
import java.util.concurrent.TimeUnit;

import sorcer.core.provider.ProviderRuntime;
import sorcer.service.ServiceExertion;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.serial.TupleSerialKeyCreator;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.ForeignKeyDeleteAction;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;

/**
 * SorcerDatabase defines the storage containers, indices and foreign keys for
 * the service provider database persisting common SORCER objects.
 * 
 * @author Mike Sobolewski
 */
public class SorcerDatabase {

    private static final long LOCK_TIMEOUT = 1000L;
    private static final String CLASS_CATALOG = "java_class_catalog";
    private static final String RUNTIME_STORE = "runtime_store";
    private static final String EXERTION_STORE = "exertion_store";
    private static final String CONTEXT_STORE = "context_store";
    private static final String TABLE_STORE = "table_store";
    private static final String VAR_STORE = "var_store";
    private static final String VAR_MODEL_STORE = "var_model_store";
    private static final String OBJECT_STORE = "uuid_object_store";

    private static final String RUNTIME_PROVIDER_NAME_INDEX =
        "runtime_provider_name_index";

    private Environment env;
    private Database exertionDb;
    private Database runtimeDb;
    private Database contextDb;
    private Database tableDb;
    private Database varDb;
    private Database varModelDb;
    private Database uuidObjectDb;

    private SecondaryDatabase runtimeByProviderNameDb;
    private StoredClassCatalog javaCatalog;

    /**
     * Open all storage containers, indices, and catalogs.
     */
    public SorcerDatabase(String homeDirectory)
        throws DatabaseException {
        // Open the Berkeley DB environment in transactional mode.
        //System.out.println("Opening environment in: " + homeDirectory);
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setLockTimeout(LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        envConfig.setSharedCache(true);
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

        // Open the Berkeley DB database, the stores are opened
        // with no duplicate keys allowed.

        runtimeDb = env.openDatabase(null, RUNTIME_STORE, dbConfig);

        exertionDb = env.openDatabase(null, EXERTION_STORE, dbConfig);

        contextDb = env.openDatabase(null, CONTEXT_STORE, dbConfig);

        tableDb = env.openDatabase(null, TABLE_STORE, dbConfig);

        varDb = env.openDatabase(null, VAR_STORE, dbConfig);

        varModelDb = env.openDatabase(null, VAR_MODEL_STORE, dbConfig);
        
        uuidObjectDb = env.openDatabase(null, OBJECT_STORE, dbConfig);

        // Open the SecondaryDatabase for the name index of the provider in the runtime store.
        // Duplicate keys are allowed since more than one provider may be in
        // the same exertion.  A foreign key constraint is defined for the
        // name indices to ensure that a runtime only refers to
        // existing provider keys.  The CASCADE delete action means
        // that shipments will be deleted if their associated part or supplier
        // is deleted.
        //
        SecondaryConfig secConfig = new SecondaryConfig();
        secConfig.setTransactional(true);
        secConfig.setAllowCreate(true);
        secConfig.setSortedDuplicates(true);

        secConfig.setKeyCreator(new RuntimeByProviderNameKeyCreator(javaCatalog,
        		ProviderRuntime.class));
        runtimeByProviderNameDb = env.openSecondaryDatabase(null, RUNTIME_PROVIDER_NAME_INDEX,
                                                     runtimeDb, secConfig);
        secConfig.setForeignKeyDatabase(runtimeDb);
        secConfig.setForeignKeyDeleteAction(ForeignKeyDeleteAction.CASCADE);
        secConfig.setKeyCreator(new ExertionByRuntimeKeyCreator(javaCatalog,
                                                             ServiceExertion.class));
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
     * Return the exertion storage container.
     */
    public final Database getExertionDatabase() {
        return exertionDb;
    }

    /**
     * Return the runtime storage container.
     */
    public final Database getRuntimeDatabase() {
        return runtimeDb;
    }

    /**
     * Return the {@link sorcer.service.Context} storage container.
     */
    public final Database getContextDatabase() {
        return contextDb;
    }
    
    /**
     * Return the {@link sorcer.vfe.util.Table} storage container.
     */
    public final Database getTableDatabase() {
        return tableDb;
    }
    
    /**
     * Return the {@link sorcer.vfe.Var} storage container.
     */
    public final Database getVarDatabase() {
        return varDb;
    }
    
    /**
     * Return the {@link sorcer.core.context.model.var.VarModel} storage container.
     */
    public final Database getVarModelDatabase() {
        return varModelDb;
    }
    
    /**
     * Return the Uuid object storage container.
     */
    public final Database getUuidObjectDatabase() {
        return uuidObjectDb;
    }
    
    /**
     * Return the shipment-by-part index.
     */
    public final SecondaryDatabase getRuntimeByProviderNameDatabase() {
        return runtimeByProviderNameDb;
    }
    
    /**
     * Close all stores (closing a store automatically closes its indices).
     */
    public void close()
        throws DatabaseException {
        // Close secondary databases, then primary databases.
        exertionDb.close();
        runtimeDb.close();
        runtimeByProviderNameDb.close();
        contextDb.close();
        tableDb.close();
        varDb.close();
        varModelDb.close();
        uuidObjectDb.close();
        
        // close the catalog and the environment.
        javaCatalog.close();
        env.sync();
        env.close();
    }

    /**
     * The SecondaryKeyCreator for the RuntimeByProviderName index.  This is an
     * extension of the abstract class TupleSerialKeyCreator, which implements
     * SecondaryKeyCreator for the case where the data keys are of the format
     * TupleFormat and the data values are of the format SerialFormat.
     */
    private static class RuntimeByProviderNameKeyCreator
        extends TupleSerialKeyCreator {

        /**
         * Construct the city key extractor.
         * @param catalog is the class catalog.
         * @param valueClass is the supplier value class.
         */
        private RuntimeByProviderNameKeyCreator(ClassCatalog catalog,
                                         Class valueClass) {
            super(catalog, valueClass);
        }

        /**
         * Extract the city key from a supplier key/value pair.  The city key
         * is stored in the supplier value, so the supplier key is not used.
         */
		public boolean createSecondaryKey(TupleInput primaryKeyInput,
				Object valueInput, TupleOutput indexKeyOutput) {
			sorcer.core.provider.ProviderRuntime runtime 
				= (sorcer.core.provider.ProviderRuntime) valueInput;
			String providerName;
				providerName = runtime.getProviderName();

				if (providerName != null) {
					indexKeyOutput.writeString(runtime.getProviderName());
					return true;
				} else {
					return false;
				}
		}
	}

    /**
     * The SecondaryKeyCreator for the ExertionByRuntime index.  This is an
     * extension of the abstract class TupleSerialKeyCreator, which implements
     * SecondaryKeyCreator for the case where the data keys are of the format
     * TupleFormat and the data values are of the format SerialFormat.
     */
    private static class ExertionByRuntimeKeyCreator
        extends TupleSerialKeyCreator {

        /**
         * Construct the part key extractor.
         * @param catalog is the class catalog.
         * @param valueClass is the shipment value class.
         */
		private ExertionByRuntimeKeyCreator(ClassCatalog catalog,
				Class valueClass) {
			super(catalog, valueClass);
		}

        /**
         * Extract the part key from a shipment key/value pair.  The part key
         * is stored in the shipment key, so the shipment value is not used.
         */
		public boolean createSecondaryKey(TupleInput primaryKeyInput,
				Object valueInput, TupleOutput indexKeyOutput) {
			String providerName = primaryKeyInput.readString();
			// don't bother reading the supplierNumber
			indexKeyOutput.writeString(providerName);
			return true;
		}
    }

}
