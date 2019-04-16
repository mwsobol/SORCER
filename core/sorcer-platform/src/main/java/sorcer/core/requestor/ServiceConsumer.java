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

package sorcer.core.requestor;

import groovy.lang.GroovyShell;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.RemoteLogger;
import sorcer.core.provider.logger.LoggerRemoteException;
import sorcer.core.provider.logger.RemoteLoggerListener;
import sorcer.netlet.ServiceScripter;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.tools.webster.InternalWebster;
import sorcer.tools.webster.Webster;
import sorcer.util.Sorcer;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

import static sorcer.eo.operator.prvName;

/**
 * @author Mike Sobolewski
 */
public class ServiceConsumer implements Consumer, Requestor, SorcerConstants {
	/** Logger for logging information about this instance */
	protected static final Logger logger = LoggerFactory.getLogger(ServiceConsumer.class.getName());

    protected String name;
	protected Properties props;
    static protected Class target;
    static protected String[] args;
	protected Mogram mogram;
	protected String jobberName;
	protected GroovyShell shell;
	protected RemoteLoggerListener listener;
	protected static ServiceConsumer requestor = null;
	final static String REQUESTOR_PROPERTIES_FILENAME = "consumer.properties";

	// Consumer inputs
    protected List<Service> services;
    protected Context inContext;
    static protected Arg[] consumerArgs;

	public ServiceConsumer() {
		// do nothing
	}

    public ServiceConsumer(Class consumerType, String... args) {
        target = consumerType;
        ServiceConsumer.args = args;
    }

	public ServiceConsumer(Class consumerType, Context inContext, Arg... args) {
        target = consumerType;
        this.inContext = inContext;
        ServiceConsumer.consumerArgs = args;
    }

	public static void main(String... args) throws Exception {
		prepareToRun(args);
		requestor.preprocess(args);
		requestor.process(args);
		requestor.postprocess();
	}

	public Object execute(Arg... args) throws MogramException, RemoteException {
		prepareToRun();
		requestor.preprocess(Arg.asStrings(args));
		try {
			if (args.length == 1 && args[0] instanceof Signature) {
				// consumer services
				Signature rs = (Signature) args[0];
				return rs.execute(rs);
			} else if (requestor.jobberName != null) {
				Arg[] ext = new Arg[args.length+1];
				System.arraycopy(args,  0, ext,  1, args.length);
				ext[0] = prvName(requestor.jobberName);
				mogram = requestor.mogram.exert(requestor.getTransaction(), ext);
			} else {
				if (args == null)
					mogram = requestor.mogram.exert(requestor.getTransaction());
				else
					mogram = requestor.mogram.exert(requestor.getTransaction(), args);
			}

		} catch (Exception e) {
			throw new RoutineException(e);
		}
		return mogram.getContext();
	}

	public static void prepareToRun(String... args) {
		System.setSecurityManager(new SecurityManager());
		Accessor.create();

		// Initialize system properties: configs/sorcer.env
		Sorcer.getEnvProperties();

		String requestorType = null;
		if (args.length == 0 && target == null) {
			System.err
					.println("Usage: java sorcer.core.consumer.ExertRequestor  <requestorType>");
			System.exit(1);
		}
		try {
			if (target != null) {
				requestor = (ServiceConsumer) target.newInstance();
			} else {
				requestorType = args[0];
				requestor = (ServiceConsumer) Class.forName(requestorType)
						.newInstance();
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("Not able to create service consumer: " + requestorType);
			System.exit(1);
		}
		String str = System.getProperty(REQUESTOR_PROPERTIES_FILENAME);
		logger.info(REQUESTOR_PROPERTIES_FILENAME + " = " + str);
		if (str != null) {
			requestor.loadProperties(str); // search the provider package
		} else {
			requestor.loadProperties(REQUESTOR_PROPERTIES_FILENAME);
		}
		boolean isWebsterInt = false;
		String val = System.getProperty(SORCER_WEBSTER_INTERNAL);
		if (val != null && val.length() != 0) {
			isWebsterInt = val.equals("true");
		}
		if (isWebsterInt) {
			String roots = System.getProperty(SorcerConstants.WEBSTER_ROOTS);
			String[] tokens = null;
			if (roots != null)
				tokens = toArray(roots);
			try {
				InternalWebster.startWebster(null, tokens);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setMogram(Mogram mogram) {
		this.mogram = mogram;
	}

	public Mogram getMogram(String... args) throws MogramException {
        // implement in subclsses
        throw new MogramException("Mograms should be declared in subclasses!");
    }

    public List<Service> getService(String... args) throws ServiceException {
        // implement in subclsses
        throw new ServiceException("Consumer services should be declared in subclasses!");
    }

    public String getJobberName() {
		return jobberName;
	}

	public void preprocess(String... args) throws RoutineException, ContextException {
		Mogram in = null;
		try {
			in = requestor.getMogram(args);
			if (in == null)
				throw new RoutineException("No mogram definde for this consumer!");

			if (logger.isDebugEnabled())
				logger.debug("ServiceConsumer java.rmi.server.codebase: "
						+ System.getProperty("java.rmi.server.codebase"));

			if (in != null) {
				requestor.setMogram(in);
				if (mogram != null && mogram instanceof Routine)
					logger.info(">>>>>>>>>> Input context: \n" + ((Routine) mogram).getContext());
				else {
					logger.info(">>>>>>>>>> Inputs: \n" + ((Model) mogram).getInputs());
				}

				// Starting RemoteLoggerListener
				java.util.List<Map<String, String>> filterMapList = new ArrayList<Map<String, String>>();
				for (String exId : ((ServiceMogram) requestor.mogram).getAllMogramIds()) {
					Map<String, String> map = new HashMap<String, String>();
					map.put(RemoteLogger.KEY_MOGRAM_ID, exId);
					filterMapList.add(map);
				}
				if (!filterMapList.isEmpty()) {
					try {
						listener = new RemoteLoggerListener(filterMapList, System.out);
					} catch (LoggerRemoteException lre) {
						logger.warn("Remote logging disabled: " + lre.getMessage());
						listener = null;
					}
				}
			}
		} catch (Exception e) {
			logger.error("main", e);
			System.exit(1);
		}
	}

	public void process(String... args) throws RoutineException, ContextException {
		try {
			if (jobberName != null) {
				mogram = mogram.exert(requestor.getTransaction(), prvName(requestor.getJobberName()));
			} else {
				mogram = mogram.exert(requestor.getTransaction());
			}

		} catch (Exception e) {
			throw new RoutineException(e);
		} 
	}

	public void postprocess(String... args) throws RoutineException, ContextException {
		try {
			if (mogram != null) {
				if (mogram.getExceptions() != null && mogram.getExceptions().size() > 0)
					logger.info("<<<<<<<<<< Exceptions: \n" + mogram.getExceptions());
				if (mogram.getTrace() != null && mogram.getTrace().size() > 0)
					logger.info("<<<<<<<<<< Traces: \n" + mogram.getTrace());

				if (mogram instanceof Routine) {
					logger.info("<<<<<<<<<< Ouput context: \n" + ((Routine) mogram).getContext());
				} else {
					logger.info("<<<<<<<<<< Response: \n" + ((Model) mogram).getResponse());
				}
			}
			if (listener != null) listener.destroy();
		} catch (Exception e) {
			throw new RoutineException(e);
		}
	}

	public Object evaluate(File scriptFile) throws Throwable {
		ServiceScripter serviceScripter = null;
		if (scriptFile.exists()) {
			serviceScripter = new ServiceScripter(scriptFile, getClass().getClassLoader());
		} else {
			logger.warn("Missing script input filename: " + scriptFile.getAbsolutePath());;
		}
		Object outObject = serviceScripter.interpret();

		return outObject;
	}

	public Transaction getTransaction() {
		return null;
	}

	/**
	 * Loads service consumer properties from a <code>filename</code> file. By
	 * default a service consumer loads its properties from
	 * <code>consumer.properties</code> file located in the consumer's
	 * package. Also, a service consumer properties file key can be specified
	 * as a system property when starting the consumer with
	 * <code>-DrequestorProperties=&ltfilename&gt<code>. In this case the consumer loads
	 * properties from <code>filename</code> file. Properties are accessible
	 * calling the <code>
	 * getProperty(String)</code> method.
	 * 
	 * @param filename
	 *            the properties file key see #getProperty
	 */
	public void loadProperties(String filename) {
		try {
			props = new Properties();
			if (filename != null) {
				props.load(new FileInputStream(filename));
			} else {
				// check the class resource
				InputStream is = this.getClass().getResourceAsStream(filename);
				// check local resource
				if (is == null)
					is = (InputStream) (new FileInputStream(filename));
				if (is != null) {
					props.load(is);
				} else {
					System.err
							.println("Not able to open stream on properties: "
									+ filename);
					System.err.println("Service consumer class: "
							+ this.getClass());
					return;
				}
			}
		} catch (IOException ioe) {
			logger.info("Not able to load consumer properties");
			// ioe.printStackTrace();
		}

	}

	public String getProperty(String key) {
		return props.getProperty(key);
	}

	public String getProperty(String property, String defaultValue) {
		return props.getProperty(property, defaultValue);
	}

	private static String[] toArray(String arg) {
		StringTokenizer token = new StringTokenizer(arg, " ,;");
		String[] array = new String[token.countTokens()];
		int i = 0;
		while (token.hasMoreTokens()) {
			array[i] = token.nextToken();
			i++;
		}
		return (array);
	}

    /**
     * Added for compatibility with Sorcersoft.com SORCER - in this implementation may be used to publish
     * jar files directly using the internal Webster
     *
     * @param artifactCoords
     * @return
     */
    public static Webster prepareCodebase(String[] artifactCoords) {
        try {
            return InternalWebster.startWebster(artifactCoords);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void prepareEnvironment() {
        //System.setProperty("webster.internal", "true");
        prepareBasicEnvironment();
    }

    protected static void prepareBasicEnvironment(){
        System.setProperty("java.rmi.server.useCodebaseOnly", "false");
        System.setProperty("java.protocol.handler.pkgs", "net.jini.url|sorcer.util.url|org.rioproject.url");
        System.setProperty("java.security.policy", Sorcer.getHome() + "/policy/sorcer.policy");
        System.setProperty("logback.configurationFile", Sorcer.getHome() + "/configs/sorcer-logging.groovy");
        if (System.getSecurityManager() == null)
			System.setSecurityManager(new SecurityManager());
    }

	@Override
	public Context exec(Service service, Context context, Arg[] args) throws ServiceException, RemoteException, TransactionException {
		Object obj = execute(args);
		if (obj instanceof Context) {
			return context.append((Context)obj);
		} if (obj instanceof Routine) {
			return context.append(((Routine)obj).getContext());
		} else {
			context.putValue("consumer/result", obj);
			return context;
		}

	}

	public String getName() {
        return name;
    }

	@Override
	public Context consume(Context context, Arg[] args) throws ServiceException, RemoteException, TransactionException {
        inContext = context;
        consumerArgs = args;
		Context outContext = context;
		for (Service service: services) {
			inContext = outContext;
			outContext = exec(service, inContext, consumerArgs);
		}
		return outContext;
	}

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }


    public Context getInContext() {
        return inContext;
    }

    public void setInContext(Context inContext) {
        this.inContext = inContext;
    }

    public static Arg[] getConsumerArgs() {
        return consumerArgs;
    }

    public static void setConsumerArgs(Arg[] consumerArgs) {
        ServiceConsumer.consumerArgs = consumerArgs;
    }

}
