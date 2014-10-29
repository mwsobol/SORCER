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

package sorcer.core.requestor;

import junitx.framework.FileAssert;
import sorcer.core.SorcerConstants;
import sorcer.core.context.Contexts;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.tools.webster.InternalWebster;
import sorcer.util.GenericUtil;
import sorcer.util.Sorcer;
import sorcer.util.SorcerUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

/**
 * This class defines the JUnit requestor's scaffolding. The init method of the
 * class initializes system properties; if required an internal Webster is
 * started. If one is running it attempts to get the webster roots paths.
 * 
 * @author M. W. Sobolewski
 */
 public class SorcerTester implements SorcerConstants {
	/** Logger for logging information about this instance */
	protected static final Logger logger = Logger.getLogger(SorcerTester.class.getName());

	public static String R_PROPERTIES_FILENAME = "requestor.properties";
	public static String R_VERSIONS_FILENAME = "versions.properties.file";

	protected static SorcerTester tester = null;
	protected Properties props, versions;
	protected int port;

	public SorcerTester(String... args) {
        try {
            init(args);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to properly initialize SorcerTester", e);
        }
    }
	
	/**
	 * init method for the SorcerTester class
	 * @param args String array containing arguments for the init method
	 * @throws Exception
	 */
	public void init(String... args) throws Exception {
		
		// add sos protocol handler
		//URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		
		// Attempt to load the tester properties file
		String filename = System.getProperty(R_PROPERTIES_FILENAME);
		logger.info(R_PROPERTIES_FILENAME + " = " + filename);
		if (filename != null && filename != "") {
			logger.info("loading requestor properties:" + filename);
			props = Sorcer.loadProperties(filename);
  		} else {
			//throw new RuntimeException("No tester properties file available!");
		}
		filename = System.getProperty(R_VERSIONS_FILENAME);
		logger.info(R_VERSIONS_FILENAME + " = " + filename);
		if (filename != null && filename != "") {
			logger.info("loading artifact versions from:" + filename);
			versions = Sorcer.loadProperties(filename); 
  		} else {
			//throw new RuntimeException("No versions properties file available!");
		}
		// Determine if an internal web server is running if so obtain the root paths
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
				InternalWebster.startWebster(tokens);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
				
		// system property for DOC_ROOT_DIR - needed for scratchURL
		System.setProperty(Sorcer.DOC_ROOT_DIR, Sorcer.getHome() + File.separator + "data");
	}
	
	public String getProperty(String key) {
		return tester.getProps().getProperty(key);
	}

	public Object setProperty(String key, String value) {
		return tester.getProps().setProperty(key, value);
	}
	
	public String getProperty(String property, String defaultValue) {
		return tester.getProps().getProperty(property, defaultValue);
	}
	
	/**
	 * Returns a URL for the tester's data server.
	 * 
	 * @return the current URL for the requestor's data server.
	 */
	public String getDataServerUrl() {
		return "http://" + getProperty(DATA_SERVER_INTERFACE) + ':' + getProperty(DATA_SERVER_PORT);
	}

	/**
	 * Returns the hostname of a requestor data server.
	 * 
	 * @return a data server name.
	 */
	public String getDataServerInterface() {
		return  System.getProperty(DATA_SERVER_INTERFACE);
		}
	

	/**
	 * Returns the port of a tester data server.
	 * 
	 * @return a data server port.
	 */
	public String getDataServerPort() {
		return  System.getProperty(DATA_SERVER_PORT);
		}
	
	/**
	 * Returns a URL for the SORCER class server.
	 * 
	 * @return the current URL for the SORCER class server.
	 */
	public String getWebsterUrl() {
		return "http://" + getWebsterInterface() + ':' + getWebsterPort();
	}

	/**
	 * Returns the hostname of a tester class server.
	 * 
	 * @return a webster host name.
	 */
	public String getWebsterInterface() {
		String hn = System.getenv("IGRID_WEBSTER_INTERFACE");

		if (hn != null && hn.length() > 0) {
			logger.finer("webster hostname as the system environment value: "
					+ hn);
			return hn;
		}

		hn = System.getProperty(R_WEBSTER_INTERFACE);
		if (hn != null && hn.length() > 0) {
			logger
					.finer("webster hostname as '" + R_WEBSTER_INTERFACE + "' system property value: "
							+ hn);
			return hn;
		}

		hn = tester.getProps().getProperty(R_WEBSTER_INTERFACE);
		if (hn != null && hn.length() > 0) {
			logger
					.finer("webster hostname as '" + R_WEBSTER_INTERFACE + "' provider property value: "
							+ hn);
			return hn;
		}

		try {
			hn = Sorcer.getHostName();
			logger.finer("webster hostname as the local host value: " + hn);
		} catch (UnknownHostException e) {
			logger.severe("Cannot determine the webster hostname.");
		}

		return hn;
	}
	
	/**
	 * Checks which port to use for a tester class server.
	 * 
	 * @return a port number
	 */
	public int getWebsterPort() {
		if (port != 0)
			return port;

		String wp = System.getenv("IGRID_WEBSTER_PORT");
		if (wp != null && wp.length() > 0) {
			logger.finer("requestor webster port as 'IGRID_WEBSTER_PORT': " + wp);
			return new Integer(wp);
		}

		wp = System.getProperty(R_WEBSTER_PORT);
		if (wp != null && wp.length() > 0) {
			logger.finer("requestor webster port as System '" + R_WEBSTER_PORT + "': "
					+ wp);
			return new Integer(wp);
		}

		wp = tester.getProps().getProperty(R_WEBSTER_PORT);
		if (wp != null && wp.length() > 0) {
			logger.finer("requestor webster port as Sorcer '" + R_WEBSTER_PORT + "': "
					+ wp);
			return new Integer(wp);
		}

		try {
			port = Sorcer.getAnonymousPort();
			logger.finer("anonymous requestor webster port: " + wp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return port;
	}

	/**
	 * Returns the URL for the tester's <code>filename</code>
	 * 
	 * @return the current URL for the SORCER tester data server.
	 * @throws MalformedURLException 
	 */
	public URL getRequestorDataFileURL(String filename) throws MalformedURLException {
		return new URL("http://" + getDataServerUrl() + '/'
				+ getProperty(R_DATA_DIR) + '/' + filename);
	}

	public File getScrachFile(String filename) {
		return new File(getNewScratchDir() + File.separator + filename);
	}

	/**
	 * Returns a directory for requestor's scratch files
	 * 
	 * @return a scratch directory
	 */
	 public static File getScratchDir() {
		 return Sorcer.getNewScratchDir();
	}
	
	/**
	 * Deletes a directory and all its files.
	 * 
	 * @param dir
	 *            to be deleted
	 * @return true if the directory is deleted
	 * @throws Exception
	 */
	public boolean deleteDir(File dir) throws Exception {
		return SorcerUtil.deleteDir(dir);
	}
		
	/**
	 * Returns a directory for requestor's scratch files
	 * 
	 * @return a scratch directory
	 */
	public File getNewScratchDir() {
		return Sorcer.getNewScratchDir();
	}

	public File getDataFile(String filename) {
		return new File(getDataDir() + File.separator + filename);
	}
	
	/**
	 * Returns a directory for requestor's data root.
	 * 
	 * @return a tester data root directory
	 */
	public File getDataRootDir() {
		return new File(getProperty(R_DATA_ROOT_DIR));
	}
	
	/**
	 * Returns a directory for requestor's data.
	 * 
	 * @return a tester data directory
	 */
	public File getDataDir() {
		//return new File(getProperty(R_DATA_ROOT_DIR) + File.separator + getProperty(R_DATA_DIR));
		return new File(System.getProperty(DOC_ROOT_DIR));
	}

	/**
	 * Returns the URL for a specified data file.
	 * 
	 * @param dataFile
	 *            a file
	 * @return a URL
	 * @throws MalformedURLException
	 */
	public String getDataFileUrl(File dataFile) throws MalformedURLException {
		String dataURL = getDataServerUrl();
		String path = dataFile.getAbsolutePath();
		int index = path.indexOf(Sorcer.getProperty(R_DATA_DIR));
		return dataURL + File.separator + path.substring(index);
	}

	/**
	 * Returns the requestor's scratch directory
	 * 
	 * @return a scratch directory
	 */
	 public File getUserHomeDir() {
		return new File(System.getProperty("user.home"));
	}
	
	 public Properties getProperties() {
		 return tester.getProps();
	 }
	 
	/**
	 * Returns the URL of a scratch file at the tester HTTP data server.
	 * 
	 * @param scratchFile
	 * @return the URL of a scratch file
	 * @throws MalformedURLException
	 */
	public static URL getScratchURL(File scratchFile)
			throws MalformedURLException {
		return Sorcer.getScratchURL(scratchFile);
	}

	/**
	 * Returns the URL of a dataFile at the tester HTTP data server.
	 * 
	 * @param dataFile
	 * @return the URL of a data file
	 * @throws MalformedURLException
	 */
	public static URL getDataURL(File dataFile)
			throws MalformedURLException {
		return Sorcer.getDataURL(dataFile);
	}
	
	protected static String[] toArray(String arg) {
		StringTokenizer token = new StringTokenizer(arg, " ,;");
		String[] array = new String[token.countTokens()];
		int i = 0;
		while (token.hasMoreTokens()) {
			array[i] = token.nextToken();
			i++;
		}
		return (array);
	}			
	
	public static File getFileAndCheck(File file) {
		if (!file.exists()) throw new RuntimeException(file 
				+ " does not exist!");
		return file;
	}
		
	public static Thread runAntFile(File antFile, File testerScratch) {
		logger.info("running antFile = " + antFile.getAbsolutePath());
		return (Thread) GenericUtil.executeCommandWithWorkerNoBlocking(
				new String[]{"ant", "-f", antFile.getAbsolutePath()} 
				, false
				, true
				, 0
				, testerScratch
				, new File(testerScratch.getAbsolutePath(), antFile.getName() + ".log.txt")
				, true); 
	}	
	
    protected void assertFilesWithMarkedValue(Context<?> context,
                                              File scratchDir,
                                              String junitCaseOutputDir,
                                              String dataFormatType)
        throws ContextException {

		assertTrue(dataFormatType + " not found in return context",Contexts.hasMarkedValue(context, dataFormatType));
		List<File> localResultFiles = makeOutFilesLocal(context, scratchDir,dataFormatType);

		for (int i = 0; i < localResultFiles.size(); i++) {
			String[] fn = localResultFiles.get(i).getName().split("_computed");
			File iTruthFile = new File(junitCaseOutputDir + File.separator+ fn[0]);
			logger.info(" ith truth file >" + iTruthFile + "<");
			logger.info(" ith  computed file >" + localResultFiles.get(i) + "<");
			FileAssert.assertEquals("ith file returned in context does not equal"+ iTruthFile, iTruthFile, localResultFiles.get(i));
		}

	}

	protected void assertBinaryFilesWithMarkedValue(Context<?> context, File scratchDir,
			String junitCaseOutputDir, String dataFormatType)
			throws ContextException {

		assertTrue(dataFormatType + " not found in return context",Contexts.hasMarkedValue(context, dataFormatType));
		List<File> localResultFiles = makeOutFilesLocal(context, scratchDir,dataFormatType);

		for (int i = 0; i < localResultFiles.size(); i++) {
			String[] fn = localResultFiles.get(i).getName().split("_computed");
			File iTruthFile = new File(junitCaseOutputDir + File.separator+ fn[0]);
			logger.info(" ith truth file >" + iTruthFile + "<");
			logger.info(" ith  computed file >" + localResultFiles.get(i) + "<");
			FileAssert.assertBinaryEquals("ith file returned in context does not equal"+ iTruthFile, iTruthFile, localResultFiles.get(i));
		}

	}

	protected List<File> makeOutFilesLocal(Context<?> context, File scratchDir, String engineeringDataFormat) {
		List<URL> markedURLs = new ArrayList<URL>(); 
		List<File> localFile = new ArrayList<File>();
		try {
			markedURLs = (List<URL>)context.getMarkedValues(engineeringDataFormat);

			for (int i = 0; i<markedURLs.size(); i++){
				String[] fn = markedURLs.get(i).getFile().split("/");
				localFile.add(new File(scratchDir + File.separator+ fn[fn.length - 1]+"_computed"));
				GenericUtil.download(markedURLs.get(i), localFile.get(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		 return localFile;
	}

	public static URL copyFileToScratchAndGetUrl(File source, File scratchDir) 
			throws IOException {
		
		File dest = new File(scratchDir, source.getName());
		
		logger.info("source = " + source.getAbsolutePath()
				+ "\nscratchDir = " + scratchDir.getAbsolutePath()
				+ "\ndestination = " + dest.getAbsolutePath());
		
		GenericUtil.copyFile(source, dest);
		
		URL url = getScratchURL(dest);
		
		logger.info("url = " + url);
		return url; 
	}
	
	public static URL copyDirectoryToScratchAndGetUrl(File dir, File scratchDir) throws IOException {
		
		if (dir.isFile()) return copyFileToScratchAndGetUrl(dir, scratchDir);
		
		File destDir = new File(scratchDir, dir.getName());
		destDir.mkdirs();
		URL destDirUrl = getScratchURL(destDir);
		
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				GenericUtil.copyFile(file, new File(destDir, file.getName()));
			} else {
				if (file.getName().equals(".svn")) continue;
				copyDirectoryToScratchAndGetUrl(file, destDir);
			}
		}
		
		return destDirUrl;
	}


	public Properties getProps() {
		return props;
	}

	public void setProps(Properties props) {
		this.props = props;
	}
	
	public String getVersion(String prop) {
			return versions.getProperty(prop);
	}
	
	public String[] getSorcerCodebaseJars(String... provided) {
		List<String> codebase = new ArrayList<String>();
		Collections.addAll(codebase, provided);
		codebase.add("sorcer-dl-" + getVersion("sorcer.version") + ".jar");
		codebase.add("sorcer-modeling-lib-" + getVersion("sorcer.modeling.version") + ".jar");
		return codebase.toArray(new String[codebase.size()]);
	}
}
