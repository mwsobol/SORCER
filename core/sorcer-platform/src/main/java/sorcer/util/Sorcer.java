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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.DatabaseStorer;
import sorcer.core.provider.DataspaceStorer;
import sorcer.service.Exerter;
import sorcer.service.Accessor;
import sorcer.service.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * The Sorcer utility class provides the global environment configuration for
 * the SORCER environment. The class is initialized only once by a static
 * initializer when the Sorcer class is loaded.
 * <p>
 * This includes all the information that is specific to the SORCER environment
 * and is shared among all provider components or even across multiple
 * providers.
 * <p>
 * The information is collected from iGrid/config/sorcer.env mainly and can be
 * updated by specific values from a provider Jini configuration file, provider
 * properties file, and JVM system properties.
 * <p>
 * A sorcer.env file is searched for in the <code>Sorcer.class</code> directory
 * (sorcer/util/sorcer.env), or in the requestReturn given by the JVM system property
 * <code>sorcer.env.file</code>. In development the last option is recommended.
 * <p>
 * The priorities for loading properties are as follows:
 * <ol>
 * <li>First, SORCER environment properties (sorcer.env) are read by the
 * {@code ServiceExerter}
 * <li>Second, provider configuration defined in Jini configuration file is
 * loaded and it can override any relevant settings in the existing Sorcer
 * object. Provider specific configuration is collected in ProviderConfig
 * {@link sorcer.core.provider.ProviderDelegate}.
 * <li>Third, application-specific provider properties are loaded if specified
 * by attribute <code>properties</code> in the Jini configuration file and they
 * can override relevant sorcer.env properties. While a collection of Jini
 * configuration properties is predefined, in the provider properties file,
 * custom properties can be defined and accessed via
 * {@code ServiceExerter.getProperty(String key)}.
 * <li>Finally, JVM system properties (<code>sorcer.env.file</code>), if
 * specified, can override settings in the existing Env object.
 * </ol>
 * <p>
 * The SORCER environment includes context data types. These types are similar
 * to MIME types and are loaded like the environment properties
 * <code>sorcer.env</code> described above. They associate applications to a
 * format of data contained in context data nodes. Data types can be either
 * loaded from a file (default key <code>data.formats</code>) or database. A
 * JVM system property <code>sorcer.formats.file</code> can be used to indicate
 * the location and key of a data multitype file. Data types are defined in service
 * contexts by a particular composite attribute
 * <code>dnt|application|modifiers</code>, see examples in
 * <code>iGrid/data.formats</code>. Data multitype associations (for example
 * <code>dnt|etds|object|Hashtable.output</code>) can be used to lookup data
 * nodes in service contexts {@code Contexts.getMarkedPaths}.
 */
@SuppressWarnings("rawtypes")
public class Sorcer extends SorcerEnv implements SorcerConstants {
	final static Logger logger = LoggerFactory.getLogger(Sorcer.class.getName());
	protected Sorcer() {
		super();
	}
	
	/**
	 * Loads data node (eval) types from the SORCER data store or file. Data
	 * 
	 * node types specify application types of data nodes in service contexts.
	 * It is analogous to MIME types in SORCER. Each multitype has a format
	 * 'cnt/application/format/modifiers' or in the association format
	 * 'cnt|application|format|modifiers' when used with
	 * {@code Context.getMarkedPaths}.
	 * 
	 * @param filename
	 *            key of file containing service context node multitype definitions.
	 */
	private static void loadDataNodeTypes(String filename) {
		try {
			// Try in local directory first
			props.load((new FileInputStream(new File(filename))));
		} catch (Throwable t1) {
			try {
				// Can not access "filename" give try as resource
				// sorcer/util/data.formats
				InputStream stream = Sorcer.class.getResourceAsStream(filename);
				if (stream != null)
					props.load(stream);
				else
					logger.error("could not load data node types from: "+ filename);
			} catch (Throwable t2) {
				logger.error("could not load data node types: \n"+ t2.getMessage());
			}

		}
	}

	/**
	 * Load context node (eval) types from default 'node.types'. SORCER node
	 * types specify application types of data nodes in SORCER service contexts.
	 * It is an analog of MIME types in SORCER. Each multitype has a format
	 * 'cnt/application/format/modifiers'.
	 */
	public static void loadContextNodeTypes(Hashtable<?, ?> map) {
		if (map != null && !map.isEmpty()) {
			String idName = null, cntName = null;
			String[] tokens;
			for (Enumeration<?> e = map.keys(); e.hasMoreElements();) {
				idName = (String) e.nextElement();
				tokens = toArray(idName);
				cntName = ("".equals(tokens[1])) ? tokens[0] : tokens[1];
				props.put(cntName,
						Context.DATA_NODE_TYPE + APS + map.get(idName));
			}
		}
	}

	public static void setCodeBase(String[] jars) {
		String  url = SorcerEnv.getWebsterUrl();
		SorcerEnv.setCodeBase(jars, url);
	}
	
	public static String getDatabaseStorerUrl() {
		return "sos://" + DatabaseStorer.class.getName() + '/'
				+ getActualDatabaseStorerName();
	}

	public static String getDataspaceStorerUrl() {
		return "sos://" + DataspaceStorer.class.getName() + '/'
				+ getActualSpacerName();
	}

	public static void destroy(String providerName, Class serviceType) {
		Exerter prv = (Exerter) Accessor.get().getService(providerName, serviceType);
		if (prv != null)
			try {
				prv.destroy();
			} catch (Throwable t) {
				// a dead provider will be not responding anymore
				// t.printStackTrace();
			}
	}

	public static void destroyNode(String providerName, Class serviceType) {
		Exerter prv = (Exerter) Accessor.get().getService(providerName, serviceType);
		if (prv != null)
			try {
				prv.destroyNode();
			} catch (Throwable t) {
				// a dead provider will be not responding anymore
				// t.printStackTrace();
			}
	}

}
