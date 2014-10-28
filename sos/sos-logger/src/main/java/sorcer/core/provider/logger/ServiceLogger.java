/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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
package sorcer.core.provider.logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.MainUI;
import net.jini.lookup.ui.factory.JFrameFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.RemoteLogger;
import sorcer.core.provider.ServiceProvider;
import sorcer.core.provider.logger.ui.LoggerFrameUI;
import sorcer.serviceui.UIDescriptorFactory;
import sorcer.serviceui.UIFrameFactory;
import sorcer.util.SOS;
import sorcer.util.Sorcer;

import com.sun.jini.start.LifeCycle;

public class ServiceLogger extends ServiceProvider implements RemoteLogger,
		SorcerConstants {
	// The list of all known loggers.
	private static List<LoggingConfig> knownLoggers;
	// The logger for this application
	private static Logger logger = Logger.getLogger(ServiceLogger.class
			.getName());
	// The directory to store the logs in
	private String logDir = null;
	public RandomAccessFile logFile;

	// require ctor for Jini 2 NonActivatableServiceDescriptor
	public ServiceLogger(String[] args, LifeCycle lifeCycle)
			throws Exception {
		super(args, lifeCycle);

		init(args);
	}

	public void init(String[] args) {
		knownLoggers = new Vector<LoggingConfig>();
		final String[] argv = args;
		final String defautDir = System.getProperty("iGrid.home")
				+ File.separator + "logs" + File.separator + "remote";
		new Thread() {
			public void run() {
				Configuration config;
				if (argv.length == 0) {
					logDir = defautDir;
				} else {
					try {
						config = ConfigurationProvider.getInstance(argv);
						logDir = (String) config.getEntry(
								"sorcer.core.provider.logger.loggerConfig",
								"loggerDir", String.class, defautDir);
					} catch (ConfigurationException ce) {
						ce.printStackTrace();
					}
				}
				logger.info("ServiceLogManager logDir:" + logDir);
				File dir = new File(logDir);
				if (!dir.exists()) {
					dir.getParentFile().mkdir();
					dir.mkdir();
				}
			}
		}.start();
	}

	public String[] getLogNames() throws RemoteException {
		File logDir = new File(this.logDir);
		ArrayList<String> list = new ArrayList<String>();
		String[] loggerNames = null;
		if (logDir != null)
			loggerNames = logDir.list();
		for (int i=0; i<loggerNames.length; i++)
			if (loggerNames[i].indexOf("lck") < 0)
				list.add(loggerNames[i]);
		String[] outArray = new String[list.size()];
		return list.toArray(outArray);
	}

	public synchronized void publish(LogRecord record) throws RemoteException {
		boolean contains = false;
		Logger log = null;

		Enumeration<String> e = LogManager.getLogManager().getLoggerNames();

		while (e.hasMoreElements()) {
			String loggerName = e.nextElement();
			if (record.getLoggerName().equals(loggerName))
				contains = true;
		}

		if (!contains) {
			log = Logger.getLogger(record.getLoggerName());
			log.setLevel(Level.ALL);

			FileHandler h;
			try {
				String fs = File.separator;
				h = new FileHandler(System.getProperty(IGRID_HOME) + fs
						+ "logs" + fs + "remote" + fs + "remote-logger-" + log.getName()
						+ "%g.log", 20000, 8, true);

				if (h != null) {
					h.setFormatter(new SimpleFormatter());
					log.addHandler(h);
				}
				log.setUseParentHandlers(false);
			} catch (SecurityException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
			log = Logger.getLogger(record.getLoggerName());
		}
		if (log.isLoggable(record.getLevel()))
			log.log(record);

		String name = record.getLoggerName();
		LoggingConfig lc = new LoggingConfig(name, null);
		if (!knownLoggers.contains(lc)) {
			Level lev = logger.getLevel();
			lc.setLevel(lev);
			knownLoggers.add(lc);
		}
	}

	private void openFile(String fileName) {
		File file = new File(this.logDir, fileName);
		try {
			logFile = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException ex) {
			logger.warning("File Not Found: " + ex.toString());
		}
	}

	private void closeFile() {
		try {
			logFile.close();
		} catch (IOException ex) {
			logger.warning("Error closing file: " + ex.toString());
		}
	}

	public List<String> getLog(String fileName) throws RemoteException {
		openFile(fileName);
		List<String> lines = new ArrayList<String>();
		String line = null;
		try {
			line = logFile.readLine();
			while (line != null) {
				lines.add(line);
				line = logFile.readLine();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			closeFile();
		}
		return lines;
	}

	/**
	 * Overridden from {@link java.util.logging.LogManager}. Call to reset the
	 * state and reload the logging configuration.
	 */
	public void reset() {
		knownLoggers.clear();
	}

	/**
	 * Overridden from {@link java.util.logging.LogManager}
	 * 
	 * @param is
	 *            The InputStream to read configuration from.
	 * @throws java.io.IOException
	 *             if an error occurs reading the configuration stream.
	 * @throws java.lang.SecurityException
	 *             if an error occurs accessing resources due to a policy
	 *             conflict.
	 */
	public void readConfiguration(InputStream is) throws IOException,
			SecurityException {
		knownLoggers.clear();
	}

	/**
	 * Get the list of active logger names. These are loggers that have tried to
	 * log something at some level. The active level may have kept the output
	 * from appearing. program logic using Logger.isLevelLoggable() can hide
	 * loggers from appearing in this list.
	 * 
	 * @throws java.io.IOException
	 *             if an error occurs accessing information about the logging
	 *             configuration.
	 * @return
	 */
	public List<LoggingConfig> getLoggers() throws IOException,
			RemoteException {

		return knownLoggers;
	}

	public Level getLoggerLevel(String loggerName) throws IOException {
		Level lev = Logger.getLogger(loggerName).getLevel();
		logger.fine("getLoggerLevel(" + loggerName + "): " + lev);
		return lev;
	}

	public void deleteLog(String loggerName) throws RemoteException {
//		(new File(this.logDir, loggerName)).delete();
//		if (!knownLoggers.equals(null)) {
//			knownLoggers.remove(new LoggingConfig(loggerName, Level.ALL));
//		}
	}

	/**
	 * Returns a service UI descriptor for LoggerManagerUI. The service
	 * UI allows for viewing remote logs of selected providers.
	 * 
	 * @see sorcer.core.provider.Provider#getMainUIDescriptor()
	 */
	/*
	 * (non-Java doc)
	 * 
	 * @see sorcer.core.provider.ServiceProvider#getMainUIDescriptor()
	 */
	public UIDescriptor getMainUIDescriptor() {
		UIDescriptor uiDesc = null;
		try {
			URL uiUrl = new URL(Sorcer.getWebsterUrl() + "/sos-logger-"+ SOS.getSorcerVersion()+"-ui.jar");
			URL helpUrl = new URL(Sorcer.getWebsterUrl() + "/logger.html");
		
			uiDesc = UIDescriptorFactory.getUIDescriptor(MainUI.ROLE,
					(JFrameFactory) new UIFrameFactory(new URL[] { uiUrl }, 
							LoggerFrameUI.class
							.getName(),
							"Log Viewer",
							helpUrl));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return uiDesc;
	}
	
	public String getLogComments(String filename) {
		Pattern p = null;
		try {
			// The following pattern lets this extract multiline comments that
			// appear on a single line (e.g., /* same line */) and single-line
			// comments (e.g., // some line). Furthermore, the comment may
			// appear anywhere on the line.
			p = Pattern.compile(".*/\\*.*\\*/|.*//.*$");
		} catch (PatternSyntaxException e) {
			System.err.println("Regex syntax error: " + e.getMessage());
			System.err.println("Error description: " + e.getDescription());
			System.err.println("Error index: " + e.getIndex());
			System.err.println("Erroneous pattern: " + e.getPattern());
		}
		BufferedReader br = null;
		StringBuffer bw = new StringBuffer();
		try {
			FileReader fr = new FileReader(filename);
			br = new BufferedReader(fr);
			Matcher m = p.matcher("");
			String line;
			while ((line = br.readLine()) != null) {
				m.reset(line);
				if (m.matches()) /* entire line must match */
				{
					bw.append(line + "\n");
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} finally // Close file.
		{
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}
		return bw.toString();
	}
	
}