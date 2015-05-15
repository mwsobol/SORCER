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

package org.sorcer.core.requestor;

import junitx.framework.FileAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.context.Contexts;
import sorcer.data.DataService;
import sorcer.scratch.ScratchManager;
import sorcer.scratch.ScratchManagerSupport;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.tools.webster.InternalWebster;
import sorcer.util.GenericUtil;
import sorcer.util.Sorcer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

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
	protected static final Logger logger = LoggerFactory.getLogger(SorcerTester.class.getName());
    private static final ScratchManager scratchManager = new ScratchManagerSupport();
	public static String R_PROPERTIES_FILENAME = "requestor.properties";
	public static String R_VERSIONS_FILENAME = "versions.properties.file";

	protected static SorcerTester tester = null;
	protected Properties props, versions;
	protected int port;

	public SorcerTester(String... args) {
        try {
            init(args);
        } catch (Exception e) {
            logger.error("Unable to properly initialize SorcerTester", e);
        }
    }

	/**
	 * init method for the SorcerTester class
	 * @param args String array containing arguments for the init method
	 * @throws Exception
	 */
	public void init(String... args) throws Exception {
		
		// Attempt to load the tester properties file
		String filename = System.getProperty(R_PROPERTIES_FILENAME);
		logger.info(R_PROPERTIES_FILENAME + " = " + filename);
		if (filename != null && filename != "") {
			logger.info("loading requestor properties:" + filename);
			props = Sorcer.loadProperties(filename);
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
		String val = System.getProperty(SorcerConstants.SORCER_WEBSTER_INTERNAL);
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
		System.setProperty(SorcerConstants.DOC_ROOT_DIR, DataService.getDataDir());
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

	 public Properties getProperties() {
		 return tester.getProps();
	 }
	 
	/**
	 * Returns the URL of a scratch file at the tester HTTP data server.
	 * 
	 * @param scratchFile
	 * @return the URL of a scratch file
	 * @throws IOException
	 */
	public static URL getScratchURL(File scratchFile) throws IOException {
		return scratchManager.getScratchURL(scratchFile);
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
