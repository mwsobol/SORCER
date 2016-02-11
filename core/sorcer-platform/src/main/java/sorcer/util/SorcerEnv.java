/*
 * Copyright to the original author or authors.
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

import net.jini.discovery.DiscoveryGroupManagement;
import org.rioproject.net.HostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.data.DataService;
import sorcer.service.ConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.util.*;

import static sorcer.core.SorcerConstants.*;

/**
 * The Sorcer utility class provides the global environment configuration for
 * the SORCER environment. The class is initialized only once by a static
 * initializer when the Sorcer class is loaded.
 * <p/>
 * This includes all the information that is specific to the SORCER environment
 * and is shared among all provider components or even across multiple
 * providers.
 * <p/>
 * The information is collected from iGrid/config/sorcer.env mainly and can be
 * updated by specific values from a provider Jini configuration file, provider
 * properties file, and JVM system properties.
 * <p/>
 * A sorcer.env file is searched for in the <code>SorcerEnv.class</code>
 * directory (sorcer/util/sorcer.env), or in the path given by the JVM system
 * property <code>sorcer.env.file</code>. In development the last option is
 * recommended.
 * <p/>
 * The priorities for loading properties are as follows:
 * <ol>
 * <li>First, SORCER environment properties (sorcer.env) are read by the
 * {@code ServiceProvider}
 * <li>Second, provider configuration defined in Jini configuration file is
 * loaded and it can override any relevant settings in the existing Sorcer
 * object. Provider specific configuration is collected in ProviderConfig
 * {@link sorcer.core.provider.ProviderDelegate}.
 * <li>Third, application-specific provider properties are loaded if specified
 * by attribute <code>properties</code> in the Jini configuration file and they
 * can override relevant sorcer.env properties. While a collection of Jini
 * configuration properties is predefined, in the provider properties file,
 * custom properties can be defined and accessed via
 * {@code ServiceProvider.getProperty(String key)}.
 * <li>Finally, JVM system properties (<code>sorcer.env.file</code>), if
 * specified, can override settings in the existing Env object.
 * </ol>
 * <p/>
 * The SORCER environment includes context data types. These types are similar
 * to MIME types and are loaded like the environment properties
 * <code>sorcer.env</code> described above. They associate applications to a
 * format of data contained in context data nodes. Data types can be either
 * loaded from a file (default name <code>data.formats</code>) or database. A
 * JVM system property <code>sorcer.formats.file</code> can be used to indicate
 * the location and name of a data type file. Data types are defined in service
 * contexts by a particular composite attribute
 * <code>dnt|application|modifiers</code>, see examples in
 * <code>iGrid/data.formats</code>. Data type associations (for example
 * <code>dnt|etds|object|Hashtable.output</code>) can be used to lookup data
 * nodes in service contexts {@code Contexts.getMarkedPaths}.
 * 
 * @author M. W. Sobolewski
 */
public class SorcerEnv extends SOS {
	final static Logger logger = LoggerFactory.getLogger(SorcerEnv.class.getName());

	/**
	 * Collects all the properties from sorcer.env, related properties from a
	 * provider properties file, provider Jini configuration file, and JVM
	 * system properties.
	 */
	protected static Properties props;

	protected static Properties expandingProps;

	static String SCRATCH_DIR_ORIG;

	/**
	 * Loads the environment from the SORCER file configuration sorcer.env.
	 */
	static {
		try {
			String home = getHome();
			if (home != null) {
				System.setProperty("sorcer.home", getHome());
				loadEnvironment();
                DataService.getPlatformDataService();
			}
		} catch (ConfigurationException e) {
			logger.warn("Failed to load the SORCER environment configuration");
		}
	}

	protected SorcerEnv() {
	}

	public static String getHome() {
        File homeDir = getHomeDir();
		return homeDir==null?null:homeDir.toString();
	}

	/**
	 * Returns the home directory of the iGrid environment.
	 * 
	 * @return a path of the home directory
	 */
	public static File getHomeDir() {
        try {
            String hd = System.getenv("SORCER_HOME");
            if (hd != null)
                hd = hd.trim();

            if (hd != null && hd.length() > 0) {
                System.setProperty(SORCER_HOME, hd);
                return new File(hd);
            }

            hd = System.getProperty(SORCER_HOME);
            if (hd == null)
                hd = SOS.deriveSorcerHome();
            if (hd != null && hd.length() > 0) {
                return new File(hd);
            }

            if (props != null) {
                hd = props.getProperty(SORCER_HOME);
                if (hd != null && hd.length() > 0) {
                    return new File(hd);
                }
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
		return null;
	}

    public static URL getDataURL(File file) throws IOException {
        return DataService.getPlatformDataService().getDataURL(file);
    }

	/**
	 * Returns the hostname of a SORCER class server.
	 * 
	 * @return a webster host name.
	 */
	public static String getWebsterInterface() {
		String hn = System.getenv("SORCER_WEBSTER_INTERFACE");

		if (hn != null && hn.length() > 0) {
			return hn;
		}

		hn = System.getProperty(P_WEBSTER_INTERFACE);
		if (hn != null && hn.length() > 0) {
			return hn;
		}

        if(props!=null)
            hn = props.getProperty(P_WEBSTER_INTERFACE);
		if (hn != null && hn.length() > 0) {
			return hn;
		}

		try {
			hn = getHostName();
		} catch (UnknownHostException e) {
			logger.error("Cannot determine the webster hostname.");
		}

		return hn;
	}

	/**
	 * Checks which port to use for a SORCER class server.
	 * 
	 * @return a port number
	 */
	public static int getWebsterPort() {
		if (port != 0)
			return port;

		String wp = System.getenv("IGRID_WEBSTER_PORT");
		if (wp != null && wp.length() > 0) {
			return new Integer(wp);
		}

		wp = System.getProperty(P_WEBSTER_PORT);
		if (wp != null && wp.length() > 0) {
			return new Integer(wp);
		}

        if(props!=null)
            wp = props.getProperty(P_WEBSTER_PORT);
		if (wp != null && wp.length() > 0) {
			return new Integer(wp);
		}

		try {
			port = getAnonymousPort();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return port;
	}

	/**
	 * Returns the start port to use for a SORCER code server.
	 * 
	 * @return a port number
	 */
	public static int getWebsterStartPort() {
		String hp = System.getenv("IGRID_WEBSTER_START_PORT");
		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}

		hp = System.getProperty(P_WEBSTER_START_PORT);
		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}

		hp = props.getProperty(P_WEBSTER_START_PORT);
		if (hp != null && hp.length() > 0) {
			return new Integer(hp);

		}

		return 0;
	}

	/**
	 * Returns the end port to use for a SORCER code server.
	 * 
	 * @return a port number
	 */
	public static int getWebsterEndPort() {
		String hp = System.getenv("IGRID_WEBSTER_END_PORT");

		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}

		hp = System.getProperty(P_WEBSTER_END_PORT);
		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}

		hp = props.getProperty(P_WEBSTER_END_PORT);
		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}

		return 0;
	}

	/**
	 * Returns a registered service ID received previously from a LUS and
	 * persisted in a file.
	 * 
	 * @return service ID filename
	 */
	public static String getServiceIdFilename() {
		return serviceIdFilename;
	}

	public static Properties getEnvironment() throws ConfigurationException {
		loadEnvironment();
		return props;
	}

	/**
	 * Loads the environment properties from the default filename (sorcer.env)
	 * or as given by the system property <code>sorcer.env.file</code> and
	 * service context types from the default filename (node.types) or the
	 * system property <code>sorcer.formats.file</code>.
	 * 
	 * @throws ConfigurationException
	 */
	protected static void loadEnvironment() throws ConfigurationException {
		// Try and load from path given in system properties
		props = new Properties();
		String envFile = System.getProperty("sorcer.env.file");
		if (envFile != null) {
            logger.info("Loading from sorcer.env.file system property: "+envFile);
			String[] parts = envFile.split(";");
			Properties fp;
			for (String part : parts) {
				fp = loadProperties(part);
				props.putAll(fp);
			}
		} else {
			try {
				envFile = SORCER_ENV_FILENAME;
				props = loadProperties(envFile);
			} catch (Exception e) {
				envFile = getHome() + "/configs/"+ SORCER_ENV_FILENAME;
				props = loadProperties(envFile);
			}
		}
		// update(props, false);
		reconcileProperties(props, false);
		logger.info("*** loaded env properties:" + envFile + "\n"+ GenericUtil.getPropertiesString(props));
		updateCodebase();
		logger.trace("java.rmi.server.codebase: " + System.getProperty("java.rmi.server.codebase"));
	}

	/**
	 * Loads properties from a <code>filename</code>, and overwrites them with
	 * ${SORCER_HOME}/configs/sorcer.env and then with given properties.
	 * 
	 * @param filenames properties
	 * @return expended given Properties instance
	 * @throws ConfigurationException
	 */
	public static Properties loadProperties(String... filenames) throws ConfigurationException {
		Properties properties = new Properties();
		Properties fp = null;
		if (filenames != null && filenames.length == 1) {
			if (props == null)
				loadEnvironment();
			expandingProps = props;
			return loadFileProperties(filenames[0], true);
		} else {
			expandingProps = properties;
			expandingProps = loadFileProperties(filenames[0], false);
			properties.putAll(expandingProps);
			for (int i = 1; i < filenames.length; i++) {
				fp = loadFileProperties(filenames[i], true);
				properties.putAll(fp);
			}
			// update(fp, false);
			reconcileProperties(properties, false);
			logger.info("loaded properties fp:" + Arrays.toString(filenames)
					+ "\n" + GenericUtil.getPropertiesString(properties));
			return properties;
		}
	}

	/**
	 * Tries to load properties from a <code>filename</code>, first in a local
	 * directory. If there is no file in the local directory then load the file
	 * from the classpath at sorcer/util/sorcer.env.
	 *
	 * @throws ConfigurationException
	 */
	public static Properties loadFileProperties(String filename, boolean expending) throws ConfigurationException {
        logger.info("* Loading properties: "+filename);
		Properties properties = new Properties();
		try {
			File pf = new File(filename);
			if (pf.exists()) {
				// Try the provided filename first
				InputStream is = new FileInputStream(filename);
				properties.load(is);
				logger.info("* loaded properties from file: " + filename);
				is.close();
			} else {
				// No file, try as resource /configs/<filename>
				File file = new File(filename);
                String fn = "configs/" + file.getName();
                ClassLoader resourceLoader = Thread.currentThread().getContextClassLoader();
				if(logger.isTraceEnabled()) {
					logger.trace("Using {}: {}", resourceLoader.getClass().getName(), resourceLoader);
				}
                if(logger.isDebugEnabled()) {
                    Enumeration<URL> resources = resourceLoader.getResources(fn);
                    StringBuilder sb = new StringBuilder();
                    while(resources.hasMoreElements()) {
                        if(sb.length()>0) {
                            sb.append("\n");
                        }
                        sb.append("\t").append(resources.nextElement().toExternalForm());
                    }
                    logger.debug("Found Resources\n"+(sb.length()==0?"<No configs/sorcer.env found>":sb.toString()));
                }
                URL resourceURL = resourceLoader.getResource(fn);
                if(resourceURL!=null) {
                    logger.info("Loaded from "+resourceURL.toExternalForm());
                    InputStream rs = resourceURL.openStream();
                    logger.info("* loaded properties from resource as stream: "+ fn);
                    properties.load(rs);
                    rs.close();
                    logger.info("* Loading properties from using: "+ rs);
                } else {
                    throw new ConfigurationException("No resource as stream available: " + fn);
                }
			}
		} catch (Exception le) {
			logger.warn("* could not load properties: " + filename);
			throw new ConfigurationException(le);
		}

//		logger.info("** loaded properties:" + filename + "\n"
//				+ GenericUtil.getPropertiesString(properties));

		// update(properties, expending);
		reconcileProperties(properties, expending);
		// env properties are printed in loadEnvironment()
		if (!filename.contains("sorcer.env")) {
			logger.info("*** loaded properties:" + filename + "\n"
					+ GenericUtil.getPropertiesString(properties));
		}
		return properties;
	}

	/**
	 * Loads data node (value) types from the SORCER data store or file. Data
	 * node types specify application types of data nodes in service contexts.
	 * It is analogous to MIME types in SORCER. Each type has a format
	 * 'cnt/application/format/modifiers' or in the association format
	 * 'cnt|application|format|modifiers' when used with
	 * {@code Context.getMarkedPaths}.
	 * 
	 * @param filename
	 *            name of file containing service context node type definitions.
	 * @throws ConfigurationException
	 */
	protected static void loadDataFormatTypes(String filename)
			throws ConfigurationException {
		try {
			// Try the provided filename first
			File pf = new File(filename);
			if (pf.exists()) {
				InputStream is = new FileInputStream(new File(filename));
				props.load(is);
				logger.info("* loaded data formats from: " + filename);
				is.close();
			} else {
				// no file" give try as resource
				File file = new File(filename);
				String fn = "/configs/" + file.getName();
				InputStream stream = SorcerEnv.class.getResourceAsStream(fn);
				if (stream != null) {
					props.load(stream);
					stream.close();
					logger.info("* loaded data formats from resource as stream: "+ fn);
				} else {
					throw new ConfigurationException("No resource as stream available: " + fn);
				}
			}
		} catch (Exception le) {
			logger.warn("* could not load data formats: " + filename);
			throw new ConfigurationException(le);
		}
	}

	public static Properties loadProperties(InputStream inputStream)
			throws ConfigurationException {
		Properties properties = new Properties();
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}
		expandingProps = props;
		reconcileProperties(properties, true);
		return properties;
	}

	public static Properties loadPropertiesNoException(String filename) {
		logger.info("loading properties from " + filename);
		logger.info("before loading, props = " + props);
		try {
			return loadProperties(filename);
		} catch (ConfigurationException e) {
			logger.warn(e.toString());
			// e.printStackTrace();
		}
		logger.info("after loading, props = " + props);
		return props;
	}

	protected static void reconcileProperties(Properties properties, boolean expanding)
        throws ConfigurationException {
		update(properties, expanding);
		// set the document root for HTTP server either for provider ormstc.data.dir
		// requestor
        //rootDir = properties.getProperty(P_DATA_ROOT_DIR);
		String rootDir = DataService.getDataDir();
        String dataDir = dataDir = properties.getProperty(P_DATA_DIR);

        if(logger.isTraceEnabled())
            logger.trace("\n1. rootDir = {}\ndataDir = {}" + rootDir, dataDir);

		if (dataDir != null) {
			System.setProperty(DOC_ROOT_DIR, rootDir + File.separator + dataDir);
            if(logger.isTraceEnabled())
                logger.trace("1. DOC_ROOT_DIR = {}", System.getProperty(DOC_ROOT_DIR));
		} else {
			//rootDir = properties.getProperty(R_DATA_ROOT_DIR);
			dataDir = properties.getProperty(R_DATA_DIR);
			if (rootDir != null && dataDir != null) {
				System.setProperty(DOC_ROOT_DIR, rootDir + File.separator+ dataDir);
            }
            if(logger.isTraceEnabled()) {
                logger.trace("\n2 .rootDir = {}\ndataDir = {}", rootDir, dataDir);
                logger.trace("2. DOC_ROOT_DIR = {}", System.getProperty(DOC_ROOT_DIR));
            }
        }
        dataDir = properties.getProperty(P_SCRATCH_DIR);
		// logger.debug("\n3. dataDir = " + dataDir);
		if (dataDir != null) {
			System.setProperty(SCRATCH_DIR, dataDir);
			// logger.info("3. SCRATCH_DIR = " +
			// System.getProperty(SCRATCH_DIR));
		} else {
			dataDir = properties.getProperty(R_SCRATCH_DIR);
			// logger.debug("\n4. dataDir = " + dataDir);
			if (dataDir != null) {
				System.setProperty(SCRATCH_DIR, dataDir);
				// logger.info("4. SCRATCH_DIR = " +
				// System.getProperty(SCRATCH_DIR));
			}
		}

		String httpInterface = null, httpPort = null;
		httpInterface = properties.getProperty(P_DATA_SERVER_INTERFACE);
		httpPort = properties.getProperty(P_DATA_SERVER_PORT);
		if (httpInterface != null) {
			System.setProperty(DATA_SERVER_INTERFACE, httpInterface);
			System.setProperty(DATA_SERVER_PORT, httpPort);
		} else {
			httpInterface = properties.getProperty(R_DATA_SERVER_INTERFACE);
			httpPort = properties.getProperty(R_DATA_SERVER_PORT);
			if (httpInterface != null) {
				System.setProperty(DATA_SERVER_INTERFACE, httpInterface);
				System.setProperty(DATA_SERVER_PORT, httpPort);
			}
		}
		SCRATCH_DIR_ORIG = System.getProperty(SCRATCH_DIR);
	}

	public static void updateCodebase() {
		String codebase = System.getProperty("java.rmi.server.codebase");
		if (codebase == null)
			return;
		String pattern = "${localhost}";
		if (codebase.indexOf(pattern) >= 0) {
			try {
				String val = codebase.replace(pattern, getHostAddress());
				System.setProperty("java.rmi.server.codebase", val);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Overwrites defined properties in sorcer.env (sorcer.home,
	 * provider.webster.interface, provider.webster.port) with those defined as
	 * JVM system properties.
	 * 
	 * @param properties
	 * @throws ConfigurationException
	 */
	private static void update(Properties properties, boolean expanding)
			throws ConfigurationException {
		Enumeration<?> e = properties.propertyNames();
		String key, value, evalue = null;
		String pattern = "${localhost}";
		// first substitute for this localhost
		while (e.hasMoreElements()) {
			key = (String) e.nextElement();
			value = properties.getProperty(key);
			if (value.equals(pattern)) {
				try {
					value = getHostAddress();
				} catch (UnknownHostException ex) {
					ex.printStackTrace();
				}
				properties.put(key, value);
			}
		}
		// now substitute matching entries accordingly
		e = properties.propertyNames();
		while (e.hasMoreElements()) {
			key = (String) e.nextElement();
			value = properties.getProperty(key);
			// logger.info("key = " + key);
			// logger.info("value = " + value);
			evalue = expandStringProperties(value, properties, expanding);
			if (evalue != null)
				properties.put(key, evalue);
			if (value.equals(pattern)) {
				try {
					evalue = getHostAddress();
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
				properties.put(key, evalue);
			}
		}
	}

	/**
	 * Return
	 * <code>true<code> if multicast for lookup discovery is enabled, otherwise <code>false<code>.
	 * 
	 * @return true if multicast is enabled, default is true.
	 */
	public static boolean isMulticastEnabled() {
		return props.getProperty(MULTICAST_ENABLED, "true").equals("true");
	}

	/**
	 * Does a ServiceDiscoveryManager use a lookup cache?
	 * 
	 * @return true if ServiceDiscoveryManager use a lookup cache.
	 */
	public static boolean isLookupCacheEnabled() {
		return props.getProperty(LOOKUP_CACHE_ENABLED, "false").equals("true");
	}

	/**
	 * Returns required wait duration for a ServiceDiscoveryManager.
	 * 
	 * @return wait duration
	 */
	public static Long getLookupWaitTime() {
		return Long.parseLong(getProperty(LOOKUP_WAIT, "3000"));
	}

	/**
	 * Returns a number of min lookup matched for a ServiceDiscoveryManager.
	 * 
	 * @return number of min lookup matches
	 */
	public static int getLookupMinMatches() {
		return Integer.parseInt(getProperty(LOOKUP_MIN_MATCHES, "1"));
	}
	
	/**
	 * Returns a number of max lookup matched for a ServiceDiscoveryManager.
	 * 
	 * @return number of max lookup matches
	 */
	public static int getLookupMaxMatches() {
		return Integer.parseInt(getProperty(LOOKUP_MAX_MATCHES, "999"));
	}

	/**
	 * Returns the properties. Implementers can use this method instead of the
	 * access methods to cache the environment and optimize performance. Name of
	 * properties are defined in sorcer.util.SORCER.java
	 * 
	 * @return the props
	 */
	public static Properties getSorcerProperites() {
		return props;
	}

	/**
	 * Returns a URL for the SORCER class server.
	 * 
	 * @return the current URL for the SORCER class server.
	 */
	public static String getWebsterUrl() {
		return "http://" + getWebsterInterface() + ':' + getWebsterPort();
	}

	/**
	 * Returns a URL for the SORCER data server.
	 * 
	 * @return the current URL for the SORCER data server.
	 */
	public static String getDataServerUrl() {
		return "http://" + getDataServerInterface() + ':' + getDataServerPort();
	}

	/**
	 * Returns the hostname of a data server.
	 * 
	 * @return a data server name.
	 */
	public static String getDataServerInterface() {
		String hn = System.getenv("DATA_SERVER_INTERFACE");

		if (hn != null && hn.length() > 0) {
			logger.debug("data server hostname as the system environment value: "
					+ hn);
			return hn;
		}

		hn = System.getProperty(DATA_SERVER_INTERFACE);
		if (hn != null && hn.length() > 0) {
			logger.debug("data server hostname as 'data.server.interface' system property value: "
					+ hn);
			return hn;
		}

		hn = props.getProperty(DATA_SERVER_INTERFACE);
		if (hn != null && hn.length() > 0) {
			logger.debug("data server hostname as 'data.server.interface' provider property value: "
					+ hn);
			return hn;
		}

		try {
			hn = getHostName();
			logger.debug("data.server.interface hostname as the local host value: "
					+ hn);
		} catch (UnknownHostException e) {
			logger.error("Cannot determine the data.server.interface hostname.");
		}

		return hn;
	}

	/**
	 * Returns the port of a provider data server.
	 * 
	 * @return a data server port.
	 */
	public static int getDataServerPort() {
		String wp = System.getenv("DATA_SERVER_PORT");
		if (wp != null && wp.length() > 0) {
			// logger.debug("data server port as 'DATA_SERVER_PORT': " + wp);
			return new Integer(wp);
		}

		wp = System.getProperty(DATA_SERVER_PORT);
		if (wp != null && wp.length() > 0) {
			// logger.debug("data server port as System 'data.server.port': "
			// + wp);
			return new Integer(wp);
		}

		wp = props.getProperty(DATA_SERVER_PORT);
		if (wp != null && wp.length() > 0) {
			logger.info("data server port as Sorcer 'data.server.port': " + wp);
			return new Integer(wp);
		}

		// logger.error("Cannot determine the 'data.server.port'.");
		throw new RuntimeException("Cannot determine the 'data.server.port'.");
	}

	/**
	 * Returns the hostname of a provider data server.
	 * 
	 * @return a data server name.
	 */
	public String getProviderDataServerInterface() {
		return System.getProperty(P_DATA_SERVER_INTERFACE);
	}

	/**
	 * Returns the port of a provider data server.
	 * 
	 * @return a data server port.
	 */
	public String getProviderDataServerPort() {
		return System.getProperty(P_DATA_SERVER_PORT);
	}

	/**
	 * Specify a URL for the SORCER application server; default is
	 * http://127.0.0.1:8080/
	 * 
	 * @return the current URL for the SORCER application server.
	 */
	public static String getPortalUrl() {
		return props.getProperty("http://" + P_PORTAL_HOST) + ':'+ props.getProperty(P_PORTAL_PORT);
	}

	/**
	 * Gets the service locators for unicast discovery.
	 * 
	 * @return and array of strings as locator URLs
	 */
	public static String[] getLookupLocators() {
		String locs = props.getProperty(P_LOCATORS, System.getProperty(P_LOCATORS));
		return (locs != null && locs.length() != 0) ? toArray(locs) : new String[] {};
	}

	/**
	 * Returns the Jini Lookup Service groups for this environment.
	 * 
	 * @return an array of group names
	 */
	public static String[] getLookupGroups() {
        String groups = null;
        if(props!=null)
            groups = props.getProperty(P_GROUPS);
        if (groups == null || groups.length() == 0)
            return DiscoveryGroupManagement.ALL_GROUPS;
        return toArray(groups);
	}

    public static String getLookupGroupsAsString() {
        String[] sa = getLookupGroups();
        StringBuilder sb = new StringBuilder();
        for (String group : sa) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(group);
        }
        return sb.toString();
    }

    /**
     * Gets a system Cataloger name for this environment.
     *
     * @return a name of the system Cataloger
     */
    public static String getCatalogerName() {
        return props.getProperty(P_CATALOGER_NAME, "Cataloger");
	}

	/**
	 * Returns an the actual Cataloger name, eventually suffixed, to use with
	 * this environment.
	 * 
	 * @return a Cataloger actual name
	 */
	public static String getActualCatalogerName() {
		return getActualName(getCatalogerName());
	}

	/**
	 * Gets an exertion space group name to use with this environment.
	 * 
	 * @return a of space group name
	 * @see #getSpaceName
	 */
	public static String getSpaceGroup() {
		return props.getProperty(P_SPACE_GROUP, getLookupGroups()[0]);
	}

	/**
	 * Returns an exertion space name to use with this environment.
	 * 
	 * @return a not suffixed space name
	 * @see #getSpaceName
	 */

	public static String getSpaceName() {
		return props.getProperty(P_SPACE_NAME, "Exert Space");
	}

	/**
	 * Returns an the actual space name, eventually suffixed, to use with this
	 * environment.
	 * 
	 * @return a space actual name
	 * @see #getSpaceName
	 */
	public static String getActualSpaceName() {
		return getActualName(getSpaceName());
	}

	/**
	 * Returns whether this cache store should be in a database or local file.
	 * 
	 * @return true if cached to file
	 */
	public static boolean getPersisterType() {
		if ((props.getProperty(S_PERSISTER_IS_DB_TYPE)).equals("true"))
			return true;
		else
			return false;
	}

	/**
	 * Checks which host to use for RMI.
	 * 
	 * @return a name of ExertMonitor provider
	 */
	public static String getExertMonitorName() {
		return props.getProperty(EXERT_MONITOR_NAME, "Exert Monitor");
	}

	public static String getActualExertMonitorName() {
		return getActualName(getExertMonitorName());
	}

	public static String getDatabaseStorerName() {
		return props.getProperty(DATABASE_STORER_NAME, "Database Storage");
	}

	public static String getActualDatabaseStorerName() {
		return getActualName(getDatabaseStorerName());
	}

	public static String getDataspaceStorerName() {
		return props.getProperty(DATASPACE_STORER_NAME, "Dataspace Storage");
	}

	public static String getActualDataspaceStorerName() {
		return getActualName(getDataspaceStorerName());
	}

	public static String getSpacerName() {
		return props.getProperty(SPACER_NAME, "Rendezvous");
	}

	public static String getActualSpacerName() {
		return getActualName(getSpacerName());
	}

	/**
	 * Checks which host to use for RMI.
	 * 
	 * @return a hostname
	 */
	public static String getRmiHost() {
		return props.getProperty(S_RMI_HOST);
	}

	/**
	 * Checks which port to use for RMI.
	 * 
	 * @return a port number
	 */
	public static String getRmiPort() {
		return props.getProperty(S_RMI_HOST, "1099");
	}

	/**
	 * Specifies a host to be used for the SORCER SORCER application server. A
	 * default host name is localhost.
	 * 
	 * @return a hostname
	 */
	public static String getPortalHost() {
		return props.getProperty(P_PORTAL_HOST);
	}

	/**
	 * Specifies a port to be used for the SORCER application server. A default
	 * port is 8080.
	 * 
	 * @return a port number
	 */
	public static String getPortalPort() {
		return props.getProperty(P_PORTAL_PORT);
	}

	/**
	 * Checks whether a certain boolean property is set.
	 * 
	 * @param property
	 * @return true if property is set
	 */
	public static boolean isOn(String property) {
		return props.getProperty(property, "false").equals("true");
	}

	/**
	 * Should we use the Oracle DB for the Persister service provider?
	 * 
	 * @return true if we should
	 */
	public static boolean isDbOracle() {
		return props.getProperty(S_IS_DB_ORACLE, "false").equals("true");
	}

	/**
	 * Return true if a modified name is used.
	 * 
	 * @return true if name is suffixed
	 */
	public static boolean nameSuffixed() {
		return props.getProperty(S_IS_NAME_SUFFIXED, "false").equals("true");
	}

	/**
	 * Gets the value of a certain property.
	 * 
	 * @param property the property to get
	 * @return the string value of that property
	 */
	public static String getProperty(String property) {
        String p = null;
        if(props!=null) {
            p = props.getProperty(property);
        } else {
            logger.warn("No loaded properties");
        }
        return p;
	}

	/**
	 * Gets the value for a certain property or the default value if property is
	 * not set.
	 * 
	 * @param property
	 * @param defaultValue
	 * @return the string value of that property
	 */
	public static String getProperty(String property, String defaultValue) {
		return props.getProperty(property, defaultValue);
	}

	/**
	 * All database table names start with this schema prefix.
	 * 
	 * @return
	 */
	public static String getDBPrefix() {
		return dbPrefix;
	}

	public static void setDBPrefix(String prefix) {
		dbPrefix = prefix;
	}

	public static String getSepChar() {
		return sepChar;
	}

	public static void setSepChar(String character) {
		sepChar = character;
	}

	/**
	 * @param tableBaseName
	 * @return
	 */
	public static String seqIdPath(String tableBaseName) {
		return dbPrefix + "_" + tableBaseName + "." + tableBaseName + "_Seq_Id";
	}

	/**
	 * @param tableBaseName
	 * @return
	 */
	public static String seqName(String tableBaseName) {
		return dbPrefix + "_" + tableBaseName + "_seq";
	}

	/**
	 * @param tableBaseName
	 * @return
	 */
	public static String seqIdName(String tableBaseName) {
		return tableBaseName + "_Seq_Id";
	}

	/**
	 * @param tableBaseName
	 * @return
	 */
	public static String tableName(String tableBaseName) {
		return dbPrefix + "_" + tableBaseName;
	}

	/**
	 * Uses getRMIHost and getRMIPort to return the RMI registry URL.
	 * 
	 * @return
	 */
	public static String getRmiUrl() {
		return "rmi://" + getRmiHost() + ":" + getRmiPort() + "/";
	}

	/**
	 * Returns the name of the JNDI context factory.
	 * 
	 * @return a fully qualified name of class of context factory
	 */
	public static String getContextFactory() {
		return "com.sun.jndi.rmi.registry.RegistryContextFactory";
	}

	/**
	 * Returns the JNDI context provider URL string.
	 * 
	 * @return URL string of the JNDI context provider
	 */
	public static String getContextProviderUrl() {
		return getRmiUrl();
	}

	/**
	 * Returns the properties. Implementers can use this method instead of the
	 * access methods to cache the environment and optimize performance.
	 * 
	 * @return the instance of Properties
	 */
	public static Properties getEnvProperties() {
		return props;
	}

	/**
	 * Returns the filename of SORCER environment configuration.
	 * 
	 * @return environment configuration filename
	 */
	public static String getEnvFilename() {
		return SORCER_ENV_FILENAME;
	}

	/**
	 * Returns the default configuration filename of SORCER provider.
	 * 
	 * @return default configuration filename
	 */
	public static String getConfigFilename() {
		return PROVIDER_PROPERTIES_FILENAME;
	}

	/**
	 * Appends properties from the parameter properties <code>properties</code>
	 * and makes them available as the global SORCER environment properties.
	 * 
	 * @param properties
	 *            the additional properties used to update the
	 *            <code>Sorcer<code>
	 *                   properties
	 */
	public static void appendProperties(Properties properties) {
		props.putAll(properties);

	}

	/**
	 * Updates this environment properties from the provider properties
	 * <code>properties</code> and makes them available as the global SORCER
	 * environment properties.
	 * 
	 * @param properties
	 *            the additional properties used to update the
	 *            <code>Sorcer<code>
	 *                   properties
	 */
	public static void updateFromProperties(Properties properties) {

		try {
			String val = null;

			val = properties.getProperty(S_HOME);
			if (val != null && val.length() != 0)
				props.put(S_HOME, val);

			val = properties.getProperty(S_JOBBER_NAME);
			if (val != null && val.length() != 0)
				props.put(S_JOBBER_NAME, val);

			val = properties.getProperty(S_CATALOGER_NAME);
			if (val != null && val.length() != 0)
				props.put(S_CATALOGER_NAME, val);

			val = properties.getProperty(S_COMMANDER_NAME);
			if (val != null && val.length() != 0)
				props.put(S_COMMANDER_NAME, val);

			val = properties.getProperty(SORCER_HOME);
			if (val != null && val.length() != 0)
				props.put(SORCER_HOME, val);

			val = properties.getProperty(S_RMI_HOST);
			if (val != null && val.length() != 0)
				props.put(S_RMI_HOST, val);
			val = properties.getProperty(S_RMI_PORT);
			if (val != null && val.length() != 0)
				props.put(S_RMI_PORT, val);

			val = properties.getProperty(P_WEBSTER_INTERFACE);
			if (val != null && val.length() != 0)
				props.put(P_WEBSTER_INTERFACE, val);
			val = properties.getProperty(P_WEBSTER_PORT);
			if (val != null && val.length() != 0)
				props.put(P_WEBSTER_PORT, val);

			val = properties.getProperty(P_PORTAL_HOST);
			if (val != null && val.length() != 0)
				props.put("P_PORTAL_HOST", val);
			val = properties.getProperty(P_PORTAL_PORT);
			if (val != null && val.length() != 0)
				props.put(P_PORTAL_PORT, val);

			// provider data
			val = properties.getProperty(DATA_SERVER_INTERFACE);
			if (val != null && val.length() != 0)
				props.put(DATA_SERVER_INTERFACE, val);
			val = properties.getProperty(DATA_SERVER_PORT);
			if (val != null && val.length() != 0)
				props.put(DATA_SERVER_PORT, val);
			val = properties.getProperty(P_DATA_DIR);
			if (val != null && val.length() != 0)
				props.put(P_DATA_DIR, val);

			val = properties.getProperty(P_SCRATCH_DIR);
			if (val != null && val.length() != 0)
				props.put(P_SCRATCH_DIR, val);

			val = properties.getProperty(LOOKUP_WAIT);
			if (val != null && val.length() != 0)
				props.put(LOOKUP_WAIT, val);

			val = properties.getProperty(LOOKUP_CACHE_ENABLED);
			if (val != null && val.length() != 0)
				props.put(LOOKUP_CACHE_ENABLED, val);

			val = properties.getProperty(P_SERVICE_ID_PERSISTENT);
			if (val != null && val.length() != 0)
				props.put(P_SERVICE_ID_PERSISTENT, val);

			val = properties.getProperty(P_SPACE_GROUP);
			if (val != null && val.length() != 0)
				props.put(P_SPACE_GROUP, val);

			val = properties.getProperty(P_SPACE_NAME);
			if (val != null && val.length() != 0)
				props.put(P_SPACE_NAME, val);

		} catch (AccessControlException ae) {
			ae.printStackTrace();
		}
	}

	/**
	 * Returns the provider's data root directory.
	 * 
	 * @return a provider data root directory
	 */
	/*public File getDataRootDir() {
		return new File(getProperty(P_DATA_ROOT_DIR));
	}*/

	/**
	 * Returns the provider's data directory.
	 * 
	 * @return a provider data directory
	 */
	public static File getDataDir() {
		return getDocRootDir();
	}

	/**
	 * Returns a directory for provider's HTTP document root directory.
	 * 
	 * @return a HTTP document root directory
	 */
	public static File getDocRootDir() {
		return new File(DataService.getDataDir());
	}

	/*public static File getDataFile(String filename) throws IOException {
        return dataService.getDataFile(new File(filename).toURI().toURL());
	}*/

	/**
	 * Returns a directory for providers's scratch files
	 * 
	 * @return a scratch directory
	 */
	/*static public File getUserHomeDir() {
		return new File(System.getProperty("user.home"));
	}*/

	/**
	 * Returns a directory for providers's scratch files
	 * 
	 * @return a scratch directory
	 */
/*	static public File getSorcerHomeDir() {
		return new File(System.getProperty(SORCER_HOME));
	}*/

	/**
	 * Returns a directory for providers's scratch files
	 * 
	 * @return a scratch directory
	 */
	/*static public File getScratchDir() {
		return getNewScratchDir();
	}*/


	public static File[] getSharedDirs() {
		String filePath = props.getProperty(S_SHARED_DIRS_FILE);
		String[] paths;

		if (filePath != null) {
			try {
				List<String> pathList = IOUtils.readLines(new File(filePath));
				paths = pathList.toArray(new String[pathList.size()]);
			} catch (IOException e) {
				logger.warn("Error while reading " + filePath  + " " + e.getMessage());
				paths = new String[]{};
			}
			props.put(S_SHARED_DIRS, StringUtils.join(paths, File.pathSeparator));
		} else {
			String sharedDirs = props.getProperty(S_SHARED_DIRS);
			if (sharedDirs != null) {
				paths = sharedDirs.split(File.pathSeparator);
			} else {
				paths = new String[0];
			}
		}

		File[] result = new File[paths.length];
		for (int i = 0; i < paths.length; i++) {
			result[i] = new File(paths[i]);
		}
		return result;
	}

	/**
	 * Get an anonymous port.
	 * 
	 * @return An anonymous port created by invoking {@code getPortAvailable()}.
	 *         Once this method is called the return value is set statically for
	 *         future reference
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 */
	public static int getAnonymousPort() throws IOException {
		if (port == 0)
			port = getPortAvailable();
		return port;
	}

	/**
	 * Get an anonymous port
	 * 
	 * @return An available port created by instantiating a
	 *         <code>java.net.ServerSocket</code> with a port of 0
	 * @throws IOException
	 *             If an available port cannot be obtained
	 */
	public static int getPortAvailable() throws java.io.IOException {
		java.net.ServerSocket socket = new java.net.ServerSocket(0);
		int port = socket.getLocalPort();
		socket.close();
		return port;
	}

	/**
	 * Return the local host address using
	 * <code>java.net.InetAddress.getLocalHost().getHostAddress()</code>
	 * 
	 * @return The local host address
	 * @throws java.net.UnknownHostException
	 *             if no IP address for the local host could be found.
	 */
	public static String getHostAddress() throws java.net.UnknownHostException {
		return getLocalHost().getHostAddress();
	}

	/**
	 * Return the local host address for a passed in host using
	 * {@link java.net.InetAddress#getByName(String)}
	 * 
	 * @param name
	 *            The name of the host to return
	 * @return The local host address
	 * @throws java.net.UnknownHostException
	 *             if no IP address for the host name could be found.
	 */
	public static String getHostAddress(String name)
			throws java.net.UnknownHostException {
		return java.net.InetAddress.getByName(name).getHostAddress();
	}

	/**
	 * Return the local host name
	 * <code>java.net.InetAddress.getLocalHost().getHostName()</code>
	 * 
	 * @return The local host name
	 * @throws java.net.UnknownHostException
	 *             if no hostname for the local host could be found.
	 */
	public static String getHostName() throws java.net.UnknownHostException {
		return getLocalHost().getCanonicalHostName();
	}

    public static InetAddress getLocalHost() throws UnknownHostException {
        String hostnameProp = getProperty(JavaSystemProperties.RMI_SERVER_HOSTNAME);
        if (hostnameProp != null && !hostnameProp.isEmpty())
            return InetAddress.getByName(hostnameProp);
        else
            return HostUtil.getInetAddress();
    }

	/**
	 * Return the SORCER environment properties loaded by default from the
	 * 'sorcer.env' file.
	 * 
	 * @return The SORCER environment properties
	 */
	public static Properties getProperties() {
		return props;
	}

	/**
	 * Expands properties embedded in a string with ${some.property.name}. Also
	 * treats ${/} as ${file.separator}.
	 */
	synchronized public static String expandStringProperties(String value,
			Properties properties, boolean expanding)
			throws ConfigurationException {
		int p = value.indexOf("${", 0);
		if (p == -1) {
			// logger.info("returning value = " + value);
			return value;
		}
		int max = value.length();
		StringBuffer sb = new StringBuffer(max);
		int i = 0; /* Index of last character we copied */
		while (true) {
			if (p > i) {
				/* Copy in anything before the special stuff */
				sb.append(value.substring(i, p));
				i = p;
			}
			int pe = value.indexOf('}', p + 2);
			if (pe == -1) {
				/* No matching '}' found, just add in as normal text */
				sb.append(value.substring(p, max));
				break;
			}
			String prop = value.substring(p + 2, pe);
			if (prop.equals("/")) {
				sb.append(File.separatorChar);
			} else {
				try {
					String val = null;
					// try system property
					val = prop.length() == 0 ? null : System.getProperty(prop);
					// try environment variable
					if (val == null) {
						// try System env
						val = prop.length() == 0 ? null : System.getenv(prop);
					}
					// try this Env existing properties
					if (val == null && expandingProps != null) {
						val = prop.length() == 0 ? null : expandingProps
								.getProperty(prop);
					}
					// try the currently loaded properties
					if (val == null && properties != null)
						val = prop.length() == 0 ? null : properties
								.getProperty(prop);

					if (val != null) {
						sb.append(val);
					} else {
						// logger.info("returning null");
						return null;
					}
				} catch (Exception e) {
					throw new ConfigurationException(e);
				}
			}
			i = pe + 1;
			p = value.indexOf("${", i);
			if (p == -1) {
				/* No more to expand -- copy in any extra. */
				if (i < max) {
					sb.append(value.substring(i, max));
				}
				break;
			}
		}
		// logger.info("returning string = " + sb.toString());
		return sb.toString();
	}

	public static void setCodeBase(String[] jars, String codebseUrl) {
        String codebase = "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < jars.length - 1; i++) {
            if (jars[i].startsWith("artifact:"))
                sb.append(jars[i]).append(" ");
            else
                sb.append(codebseUrl).append("/").append(jars[i]).append(" ");
        }
        if (jars[jars.length - 1].startsWith("artifact:"))
            sb.append(jars[jars.length - 1]);
        else
            sb.append(codebseUrl).append("/").append(jars[jars.length - 1]);
        codebase = sb.toString();
        System.setProperty("java.rmi.server.codebase", codebase);
        if (logger.isDebugEnabled())
            logger.debug("Setting codebase 'java.rmi.server.codebase': "
                    + codebase);
	}
	
	/**
	 * Convert a comma, space, and '|' delimited String to array of Strings
	 * 
	 * @param arg
	 *            The String to convert
	 * @return An array of Strings
	 */
	public static String[] toArray(String arg) {
		StringTokenizer token = new StringTokenizer(arg, " ," + APS);
		String[] array = new String[token.countTokens()];
		int i = 0;
		while (token.hasMoreTokens()) {
			array[i] = token.nextToken();
			i++;
		}
		return (array);
	}

	public static String getNameSuffix() {
		String suffix = props.getProperty(S_NAME_SUFFIX);
		if (suffix == null)
			suffix = getDefaultNameSuffix(3);
		return suffix;
	}

	public static String getDefaultNameSuffix(int suffixLength) {
		return System.getProperty("user.name").substring(0, suffixLength)
				.toUpperCase();
	}

	public static String getSuffixedName(String name) {
		String suffix = props.getProperty(S_NAME_SUFFIX,
				getDefaultNameSuffix(3));
		return name + "-" + suffix;
	}

	public static String getActualName(String name) {
		if (nameSuffixed()) {
			String suffix = props.getProperty(S_NAME_SUFFIX, getDefaultNameSuffix(3));
			if (name.endsWith(suffix))
				return name;
			else
				return name + "-" + suffix;
		}
		return name;
	}
}
