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

/**
 * Generic SORCER Logging Utility defines SORCER loggers for your providers. <br>
 * Here is an example: <br>
 * <code>Log.getProviderLog().info("some informal message");</code><br>
 * <code>Log.getProviderLog().error(jgapp.util.Debug.stackTraceToString(ex));</code>
 * <br>
 * <p>
 * The loggers can be configured via the standard java logger configuration
 * interface, if the system property <code>java.util.logging.config.file</code>
 * is set. It should be set to <code>${SORCER_HOME}/configs/sorcer.logging</code>
 * . This already has a good example configuration.
 * <p>
 * You should generally consider using a logger for each of your packages or
 * classes, following the standard of setting the logger key to be the same as
 * the fully qualified key of the package or class respectively. This allows
 * for finer-grained control of logging due to ability to execEnt logging on and
 * off based on namespaces. The Log class predefines the top-level namespace for
 * SORCER logging. You can use one of five predefined logs, for example:
 * 
 * <pre>
 * private static Logger logger = Log.getStarterLog();
 * 
 * private static Logger logger = Log.getProviderLog();
 * 
 * private static Logger logger = Log.getCoreProviderLog();
 * 
 * private static Logger logger = Log.getSorcerLog();
 * 
 * private static Logger logger = Log.getSorcerCoreLog();
 * 
 * private static Logger logger = Log.getSecurityLog();
 * 
 * private static Logger logger = Log.getTestLog();
 * 
 * private static Logger logger = Log.getRandomLog();
 * 
 * private static Logger logger = Log.getTrustLog();
 * 
 * private static Logger logger = Log.getIntegrityLog();
 * 
 * private static Logger logger = Log.getPolicyyLog();
 * </pre>
 * 
 * Otherwise you can define your provider's logger as follows:
 * 
 * <pre>
 * private static Logger logger = Logger
 * 		.getLogger(&quot;sorcer.core.provider.myProvider&quot;);
 * static {
 * 	FileHandler logFile = new FileHandler(&quot;myProvider.log&quot;);
 * 	logFile.setFormatter(new SimpleFormatter());
 * 	logger.addHandler(logFile);
 * }
 * </pre>
 * 
 * @author Mike Sobolewski
 * @author Max Berger
 * @version $Id: Log.java,v 1.3 2007/07/27 18:35:06 sobolemw Exp $
 * @see java.util.logging.Logger

 */

public class Log {

	/** Our main SORCER logger */
	private static Logger sorcer = LoggerFactory.getLogger("sorcer");

	/** For all sorcer.core.* core packages except core providers */
	private static Logger sorcerCore = LoggerFactory.getLogger("sorcer.core");

	/** Used by service providers except core providers */
	private static Logger provider = LoggerFactory.getLogger("sorcer.core.provider");

	/** Used by service dispatchers */
	private static Logger dispatch = LoggerFactory.getLogger("sorcer.core.dispatch");

	/** Used by SORCER core service providers */
	private static Logger coreProvider = LoggerFactory.getLogger("sorcer.core.provider.ServiceExerter");

	/** Logger solely for testing purposes */
	private static Logger test = LoggerFactory.getLogger("sorcer.test");

	/** SORCER security logging */
	private static Logger securityLogger = LoggerFactory.getLogger("sorcer.core.security");

	/** Everything that doesn't fit anywhere else */
	private static Logger random = LoggerFactory.getLogger("sorcer.random");

	/** Loggers for security related logging */
	private static final String TRUST_LOG = "net.jini.security.trust";

	private static final String INTEGRITY_LOG = "net.jini.security.integrity";

	private static final String POLICY_LOG = "net.jini.security.policy";

	private static Logger trustLogger = LoggerFactory.getLogger(TRUST_LOG);

	private static Logger integrityLogger = LoggerFactory.getLogger(INTEGRITY_LOG);

	private static Logger policyLogger = LoggerFactory.getLogger(POLICY_LOG);

	/**
	 * This can be used to test the logging configuration. Prc this class with
	 * your logging config file as system parameter: <br>
	 * <code>-Djava.util.logging.config.file=sorcer.logging</code><br>
	 * and it will try all loggers and send all kinds of messages.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		sendLogMessages(sorcer);
		sendLogMessages(sorcerCore);
		sendLogMessages(provider);
		sendLogMessages(coreProvider);
		sendLogMessages(securityLogger);
		sendLogMessages(test);
		sendLogMessages(random);
		sendLogMessages(dispatch);
	}

	/**
	 * Sends a "Hello world" message for this logger. Only used for testing from
	 * main function.
	 * 
	 * @param logger
	 */
	private static void sendLogMessages(Logger logger) {
		logger.trace("Trace");
		logger.debug("Debug");
		logger.info("Info");
		logger.warn("Warn");
		logger.error("Error");
	}

	/**
	 * Returns a logger used for all sorcer.* packages except providers.
	 * 
	 * @return the main sorcer logger
	 */
	public static Logger getSorcerLog() {
		return sorcer;
	}

	/**
	 * Returns a logger all sorcer.core.* core packages except core providers.
	 * 
	 * @return the SORCER core logger
	 */
	public static Logger getSorcerCoreLog() {
		return sorcerCore;
	}

	/**
	 * Returns a logger used by any service providers.
	 * 
	 * @return a service provider logger
	 */
	public static Logger getProviderLog() {
		return provider;
	}

	/**
	 * Returns a logger used by any service providers.
	 * 
	 * @return a service provider logger
	 */
	public static Logger getDispatchLog() {
		return dispatch;
	}

	/**
	 * Returns a logger used by SORCER core service providers.
	 * 
	 * @return the SORCER core provider logger
	 */
	public static Logger getCoreProviderLog() {
		return coreProvider;
	}

	/**
	 * Returns SORCER security logger for all service providers and requestors.
	 * 
	 * @return the SORCER security logger
	 */
	public static Logger getSecurityLog() {
		return securityLogger;
	}

	/**
	 * Returns a logger used for everything that does not fit into other
	 * loggers.
	 * 
	 * @return a random logger
	 */
	public static Logger getRandomLog() {
		return random;
	}

	/**
	 * This logger is to facilitate testing. Add your extensive debug messages
	 * here.
	 * 
	 * @return a test logger
	 */
	public static Logger getTestLog() {
		return test;
	}

	/**
	 * Returns a a trust logger - net.jini.security.trust.
	 * 
	 * @return a trust logger
	 */
	public static Logger getTrustLog() {
		return trustLogger;
	}

	/**
	 * Returns a a integrity logger - net.jini.security.integrity.
	 * 
	 * @return a integrity logger
	 */
	public static Logger getIntegrityLog() {
		return integrityLogger;
	}

	/**
	 * Returns a a policy logger - net.jini.security.policy.
	 * 
	 * @return a policy logger
	 */
	public static Logger getPolicyLog() {
		return policyLogger;
	}

}
