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
package sorcer.util;

import edu.emory.mathcs.util.classloader.URIClassLoader;
import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.url.artifact.ArtifactURLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.ServiceProvider;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * SORCER generic utility class.
 */
public class GenericUtil {
    public static final String LOCAL_JARS = "LOCAL";
    public static final String REMOTE_JARS = "REMOTE";
	
	/**
	 * This is a class used for running shell scripts
	 * 
	 * @author S. A. Burton April 2011
	 * 
	 */
	private static class Worker extends Thread {
		private Integer exitValue;
		private final Process process;

		/**
		 * Constructor
		 * 
		 * @param process
		 */
		protected Worker(final Process process) {
			this.process = process;
		}

		/**
		 * This method gets the shell script exit value
		 *
		 * @return Exit value
		 */
		public Integer getExitValue() {
			return exitValue;
		}

		/**
		 * This method run the shell script
		 */
		public void run() {
			try {
				System.out.println("***worker going to wait...");
				exitValue = process.waitFor();
				System.out.println("***worker DONE waiting.");
			} catch (InterruptedException e) {
				System.out.println("***worker exception = " + e);
				return;
			} finally {
				// keep the process alive so that threads reading
				// streams can shut down before process is destroyed
				// as a result of a timeout
				//System.out.println("***worker destroying process.");
				//process.destroy();
			}
		}
	}

	/**
	 * This is a class used for running shell scripts without blocking I/O
	 * 
	 * @author S. A. Burton April 2011
	 * 
	 */
	private static class WorkerNoBlock extends Thread {
		private Integer exitValue;
		private final Process process;

		/**
		 * Constructor
		 * 
		 * @param process
		 */
		protected WorkerNoBlock(final Process process) {
			this.process = process;
		}

		/**
		 * This method gets the exit value of the shell script
		 * 
		 * @return
		 */
		public Integer getExitValue() {
			return exitValue = process.exitValue();
		}

		/**
		 * Implementation is commented out
		 */
		public void run() {
			// try {
			// //exitValue = process.waitFor();
			// } catch (InterruptedException e) {
			// return;
			// }
		}
	}

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(GenericUtil.class.getName());

	/**
	 * This is a function that appends a File object to open jar archive
	 * 
	 * @param fileObj
	 *            File or directory
	 * @param jarOut
	 *            Output stream connected to an open archive
	 */
	public static void addToArchive(File fileObj, JarOutputStream jarOut) {
		addToArchive(fileObj, null, jarOut);
	}

	/**
	 * This is a function that appends a File object to open jar archive
	 * 
	 * @param fileObj
	 *            File or directory
	 * @param parentPath
	 *            Relative parent path of fileObj to be stored in the archive
	 * @param jarOut
	 *            Output stream connected to an open archive
	 */
	public static void addToArchive(File fileObj, String parentPath,
			JarOutputStream jarOut) {
		FileInputStream in;
		byte[] buffer = new byte[1024];
		if (fileObj.isFile()) {
			// dirObj is a file
			try {
				String fileName = null;
				if (parentPath == null) {
					fileName = fileObj.getName();
				} else {
					fileName = parentPath + fileObj.getName();
				}
				in = new FileInputStream(fileObj);
				System.out.println("Adding: " + fileName);
				jarOut.putNextEntry(new JarEntry(fileName));
				int len = in.read(buffer);
				while (len > 0) {
					jarOut.write(buffer, 0, len);
					len = in.read(buffer);
				}
				jarOut.closeEntry();
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (fileObj.isDirectory()) {
			// dirObj is a directory
			File[] files = fileObj.listFiles();

			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					if (parentPath == null) {
						parentPath = fileObj.getName() + File.separator
								+ files[i].getName();
					} else {
						parentPath = parentPath + File.separator
								+ fileObj.getName() + File.separator
								+ files[i].getName();
					}
					addToArchive(files[i], parentPath, jarOut);
					continue;
				}

				try {
					in = new FileInputStream(files[i]);
					String fileName = null;
					if (parentPath == null) {
						fileName = fileObj.getName() + File.separator
								+ files[i].getName();
					} else {
						fileName = parentPath + File.separator
								+ fileObj.getName() + File.separator
								+ files[i].getName();
					}
					System.out.println("Adding: " + fileName);
					jarOut.putNextEntry(new JarEntry(fileName));
					int len = in.read(buffer);
					while (len > 0) {
						jarOut.write(buffer, 0, len);
						len = in.read(buffer);
					}
					jarOut.closeEntry();
					in.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return;
	}

	/**
	 * This method append the contents of the string array sA to file at the
	 * path dataFile
	 * 
	 * @param dataFile
	 *            File where the string array contents is to be appended
	 * @param sA
	 *            String array
	 * @throws IOException
	 */
	public static void appendFileContents(File dataFile, String[] sA)
			throws IOException {
		GenericUtil.appendFileContents(dataFile, GenericUtil.string2Vect(sA));
	}

	/**
	 * This method gets a script to run a compiled Matlab program
	 * 
	 * @param command
	 * @param scratchDir
	 * @param scratchDir
	 * @param mcrHome
	 * @return
	 */
	public static Vector<String> getScriptToRunCompiledMatlabExec(
			String command, File scratchDir, String mcrHome, String mcrEnv) {
		
		// the .replace("\\", "/") is for windows-cygwin safety
		mcrEnv = mcrEnv.replace("\\", "/");
		
		Vector<String> scriptVect = new Vector<String>();
		scriptVect.add("#!/bin/bash");
		
		// for windows safety (cygwin)
		scriptVect.add("export PATH=$PATH:/bin");
		
		scriptVect.add("echo \"the date is:\" `date`");
		scriptVect.add("echo \"uname -a = \" `uname -a`");
		scriptVect.add("echo \"whoami = \" `whoami`");
		scriptVect.add("echo \"current directory = ${PWD}\"");
		
		// the .replace("\\", "/") is for windows-cygwin safety
		scriptVect.add("cd " + scratchDir.getAbsolutePath().replace("\\", "/"));
		scriptVect.add("echo \"executing in ${PWD}\"");
		scriptVect.add("echo \"directory listing:\"");
		scriptVect.add("ls -la");
		scriptVect.add("echo going to exert: " + command);
		
		// to supress cygwin (on windows) std err warning
		//
		scriptVect.add("export CYGWIN=\"nodosfilewarning\"");
		
		// source the platform-specific mcr environment
		// 
		scriptVect.add("if [ ! -f " + mcrEnv + " ]; then ");
		scriptVect.add("\tEXIT_CODE=3");
		scriptVect.add("\techo \"***error: mcr environment file not found: " + mcrEnv + "\"");
		scriptVect.add("\techo $EXIT_CODE > compiledMatlabFail.txt");
		scriptVect.add("\techo \"done\" > compiledMatlabDone.txt");
		scriptVect.add("\texit $EXIT_CODE");
		scriptVect.add("fi");
		scriptVect.add("source " + mcrEnv);
		
		// mcr cache root should be set; it will use up all the space
		// in a tmp dir; hard to debug failure in slurm...script gets
		// on queue then dies because drive is full
		//
		File mcrCacheRoot = new File(scratchDir, "mcrCacheRoot");
		mcrCacheRoot.mkdir();
		
		// the .replace("\\", "/") is for windows-cygwin safety
		scriptVect.add("export MCR_CACHE_ROOT=" + mcrCacheRoot.getAbsolutePath().replace("\\", "/"));
		
		// shorten the mcrcache path for windows...hit a limit on some installations
		scriptVect.add("export IS_CYGWIN=`uname -s | awk 'BEGIN{flag=0} {if ($0~/CYGWIN/) {flag=1}} END{print flag}'`");
		scriptVect.add("if [ $IS_CYGWIN -eq 1 ]; then");
//		scriptVect.add("\texport MCR_CACHE_ROOT=`cygpath -ds $MCR_CACHE_ROOT`");
		scriptVect.add("\texport MCR_CACHE_ROOT=`cygpath -w $MCR_CACHE_ROOT`");
		scriptVect.add("\techo \"MCR_CACHE_ROOT=$MCR_CACHE_ROOT\"");
		scriptVect.add("fi");
				
		scriptVect.add("echo \"starting\" > compiledMatlabRunning.txt");
		scriptVect.add("EXIT_CODE=0");
		scriptVect.add(command);
		scriptVect.add("EXIT_CODE=$?");
		scriptVect.add("rm compiledMatlabRunning.txt");
		scriptVect.add("if [ $EXIT_CODE -eq 0 ]; then echo \"$ctr\" > compiledMatlabGood.txt; fi");		
		scriptVect.add("if [ $EXIT_CODE -ne 0 ]; then echo \"$EXIT_CODE\" > compiledMatlabFail.txt; fi");		
		scriptVect.add("echo \"done\" > compiledMatlabDone.txt");
		scriptVect.add("echo \"exit code = $EXIT_CODE\"");
		scriptVect.add("echo $EXIT_CODE > compiledMatlabExitCode.txt");
		scriptVect.add("exit $EXIT_CODE");
		
		return scriptVect;
	}
	
	public static void logStackTrace(Logger logger) {
		logStackTrace(logger, "GenericUtil.logStackTrace() was called.");	
	}
	
	
	public static void logStackTrace(Logger logger, String msg) {
		logger.warn(msg, new Throwable());	
	}

	/**
	 * This method given a URL sourceURL and file path destinationFile writes
	 * the contents of the URL to file.
	 * 
	 * @param sourceUrl
	 *            Source URL
	 * @param destinationFile
	 *            Destination file
	 * @throws IOException
	 */
	public static void download(URL sourceUrl, File destinationFile)
			throws IOException {
        try {
            writeUrlToFile(sourceUrl, destinationFile);
        } catch (Exception e) {
            if (e instanceof IOException) throw ((IOException)e);
            throw new IOException("problem downloading " + sourceUrl + " to " + destinationFile + "; exception = " + e);
        }
	}

	/**
	 * Appends the contents of Vector afc to file at path dataFile
	 * 
	 * @param dataFile
	 *            Path to a file where contents are to be appended
	 * @param afc
	 *            Contents to append to file dataFile
	 * @throws IOException
	 */
	public static void appendFileContents(File dataFile, Vector<?> afc)
			throws IOException {
		GenericUtil.appendVectorToFile(dataFile, afc);
	}

	public static void catFileToLogger(String filename, Logger logger) {

		// Contents of File
		StringBuilder contents = new StringBuilder();

		// Operate on the file
		try {

			// Open File
			BufferedReader stdio_stream = new BufferedReader(new FileReader(
					filename));
			logger.info("Log file '" + filename + "' is open");

			// Parse file one line at a time and append line to contents
			String line = null;
			try {
				while ((line = stdio_stream.readLine()) != null) {
					// Append line
					contents.append(line);
					// Append \r or \r\n
					contents.append(System.getProperty("line.separator"));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Send contents of file to logger
			logger.info(contents.toString());

		} catch (FileNotFoundException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

	}

	/**
	 * A method that creates a copy of file at new file path
	 * @param sourceFile Source file 
	 * @param destinationFile Destination file 
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destinationFile) throws IOException {
		// If the destination file's parent directory does not exit, create it
		destinationFile.getParentFile().mkdirs();
		
		//  Creating a new input file stream that is connected to the source file
		InputStream is = new FileInputStream(sourceFile);
		try {
			// Attempting to redirect the input file stream of the source file to the destination file path 
			redirectInputStream2File(is, destinationFile);
		} catch (IOException e) {
			// Caught an IOException 
			
			// Check if the destination file exist
			if(destinationFile.exists()) {
				// If the destination file exist delete the file 
				destinationFile.delete();
			}
			// The input file stream is reset
			is.reset();
			// Making a second attempt to copy the source file 
			redirectInputStream2File(is, destinationFile);
			// If the redirection fails a second time an IO Exception is thrown
		}
	}

	
	// method behaves like unix 'cp -r <sourceDir> <destinationDir>'
	// (or should behave like that!)
	//
	public static void copyDirectory(File sourceDir, File destinationDir)
			throws IOException {
		
//		System.out.println("sourceDir = " + sourceDir);
//		System.out.println("destinationDir = " + destinationDir);
		
		if (!sourceDir.isDirectory())
			throw new IOException("sourceDir is not a directory: sourceDir = "
					+ sourceDir);

		if (destinationDir.exists() && destinationDir.isFile())	
			throw new IOException("destinationDir exists and is a file: destinationDir = "
				+ destinationDir);
		
		// if dest dir exists copy sourceDir to sub-dir of dest dir
		if (destinationDir.exists()) {
			destinationDir = new File(destinationDir, sourceDir.getName());
//			System.out.println("mod destinationDir = " + destinationDir);
		}
			
		for (File file : sourceDir.listFiles()) {
			System.out.println("file = " + file);
			if (file.isFile()) {
//				System.out.println("\tcopying file = " + file + " to " + new File(destinationDir, file.getName()));
				copyFile(file, new File(destinationDir, file.getName()));
			} else {
				// is directory
				copyDirectory(file, destinationDir);
			}
		}
	}
	
	/**
	 * Given a list of File objects. This function constructs a
	 * jar archive. the list of File objects can be files and or directories
	 * @param fileList List of files and directories
	 * @param jarArchive File path of the jar archive
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void createArchive(List<File> fileList, File jarArchive)
			throws FileNotFoundException, IOException {
		System.out.println("Creating archive: " + jarArchive.getAbsolutePath());
		JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(
				jarArchive));
		for (int i = 0; i < fileList.size(); i++) {
			addToArchive(fileList.get(i), jarOut);
		}
		jarOut.close();
		return;
	}

	/**
	 * Create Script log file Written by: R. M. Kolonay
	 */
	public static PrintWriter createScriptLogFile(String logFileName)
			throws IOException {
		return new PrintWriter(new FileOutputStream(logFileName + ".log"));
	}

	// deletes all files and sub-directories in the given dir arg
	//
	public static void deleteFilesAndSubDirs(File dir) {
        if (dir.isFile()) {
            dir.delete();
            return;
        }
		if (!dir.canRead())
			return;
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				System.out.println("deleting " + file.getAbsolutePath());
				file.delete();
			} else {
				if (file.isDirectory()
						&& (file.listFiles() == null || file.listFiles().length == 0)) {
					file.delete();
				} else {
					deleteFilesAndSubDirs(file);
                    file.delete();
				}
			}
		}
	}


	public static double[][][] dObjATodPrimA(Double[][][] aD) {
		double[][][] ad = new double[aD.length][][];
		for (int i = 0; i < aD.length; i++)
			ad[i] = dObjATodPrimA(aD[i]);
		return ad;
	}

	public static double[][] dObjATodPrimA(Double[][] aD) {
		double[][] ad = new double[aD.length][];
		for (int i = 0; i < aD.length; i++)
			ad[i] = dObjATodPrimA(aD[i]);
		return ad;
	}

	/**
	 * DoubleToDouble is a function that converts an array of double[] to and
	 * array of Double[]. originally written by R.M. Kolonay
	 * 
	 * @author R.M. Kolonay
	 */
	public static double[] dObjATodPrimA(Double[] aD) {
		double[] ad = new double[aD.length];
		for (int i = 0; i < aD.length; i++)
			ad[i] = aD[i];
		return ad;
	}

	public static Double[][][] dPrimATodObjA(double[][][] ad) {
		Double[][][] aD = new Double[ad.length][][];
		for (int i = 0; i < ad.length; i++)
			aD[i] = dPrimATodObjA(ad[i]);
		return aD;
	}

	public static Double[][] dPrimATodObjA(double[][] ad) {
		Double[][] aD = new Double[ad.length][];
		for (int i = 0; i < ad.length; i++)
			aD[i] = dPrimATodObjA(ad[i]);
		return aD;
	}

	/**
	 * dPrimAToDObjA is a function that converts an array of double[] to and
	 * array of Double[]. originally written by R.M. Kolonay
	 * 
	 * @author R.M. Kolonay
	 */
	public static Double[] dPrimATodObjA(double[] ad) {
		Double[] aD = new Double[ad.length];
		for (int i = 0; i < ad.length; i++)
			aD[i] = ad[i];
		return aD;
	}

	public static boolean envVarExists(String envName) {
		Map<String, String> envMap = System.getenv();
		if (envMap.containsKey(envName))
			return true;
		return false;
	}

	/**
	 * execScript is a function that executes shell scripts
	 * 
	 * @author R. M. Kolonay
	 * */
	public static Process execScript(File shellScriptFile,
			String[] scriptCommand, PrintWriter execLog) throws IOException,
			InterruptedException {
		Process child2 = Runtime.getRuntime().exec(scriptCommand);
		String line;
		BufferedReader in = new BufferedReader(new InputStreamReader(
				child2.getInputStream()));

		while ((line = in.readLine()) != null) {
			execLog.println(line);
			execLog.flush();
		}

		BufferedReader errorReader = new BufferedReader(new InputStreamReader(
				child2.getErrorStream()));
		while ((line = errorReader.readLine()) != null) {
			execLog.println("*** std error: " + line);
			execLog.flush();
		}

		execLog.close();

		return child2;
	}

	/**
	 * Execute System Shell command with parameters - Paweł Rubach
	 * @param scriptCommand
	 * @param directory
	 * @param result
	 * @param errorString
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static Process execScript(String[] scriptCommand, File directory, List<String> result, List<String> errorString) throws IOException,
			InterruptedException {
		Process child2 = Runtime.getRuntime().exec(scriptCommand, null, directory);
		String line;
		BufferedReader in = new BufferedReader(new InputStreamReader(
				child2.getInputStream()));
		while ((line = in.readLine()) != null) {
			result.add(line);
		}
		BufferedReader errorReader = new BufferedReader(new InputStreamReader(
				child2.getErrorStream()));
		while ((line = errorReader.readLine()) != null) {
			errorString.add(line);
		}
		return child2;
	}

	public static String getEnvVar(String envName) throws Exception {
		Map<String, String> envMap = System.getenv();
		if (envMap.containsKey(envName))
			return envMap.get(envName);
		throw new Exception("***error: " + envName
				+ " was not found in the environment");
	}

	public static String getEnvVarNoException(String envName) {
		Map<String, String> envMap = System.getenv();
		if (envMap.containsKey(envName))
			return envMap.get(envName);
		try {
			throw new Exception("***error: " + envName
					+ " was not found in the environment");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return null;
		}
		//return envMap.get(envName);
	}
	public static File[] getFilesWithExtension(File dir, String extension){
		List<File> fileList = new ArrayList<File>();
		for (File file : dir.listFiles()){
			if (file.getName().toLowerCase().endsWith("."+extension))
				fileList.add(file);	
		}
		File[] fileListA = (File[])fileList.toArray(new File[fileList.size()]);
		return fileListA;
	}
	/**
	 * This method was originally implemented by S. A. Burton, but has been
	 * rewritten by E. D. Thompson in a a cleaner more efficient syntax. The
	 * function gets the path to a file with the path file as an argument and
	 * returns the file's contents in a vector of strings where each element of
	 * the vector corresponds to a line in the file.
	 * 
	 * @param file
	 *            Path to the file
	 * @return Vector containing the file at the path file contents
	 * 
	 * @author E. D. Thompson, S. A. Burton
	 * @throws FileNotFoundException
	 */
	public static Vector<String> getFileContents(File file)
			throws FileNotFoundException {
		Vector<String> fileContents = null;
		// Check if file exist
		if (file.exists()) {
			// Creating a vector of strings
			fileContents = new Vector<String>();
			// Creating a input stream
			Scanner fin = new Scanner(new FileReader(file));
			String line;
			// Looping over files lines while there are lines to loop over
			while (fin.hasNext()) {
				line = fin.nextLine();
				fileContents.add(line);
			}
			// Closing file input stream
			fin.close();
		} else {
			// File not found
			System.out
					.printf("getFileContents:\nFile, %s, does not exist. Returning a NULL vector of strings\n",
							file.getAbsolutePath());
		}
		return fileContents;
	}

	public static Vector<String> getFileContents(URL url) throws IOException {
		URLConnection myConnect = url.openConnection();
		myConnect.setDoInput(true);
		myConnect.setDoOutput(true);
		myConnect.setUseCaches(false);
		InputStreamReader iSR = new InputStreamReader(
				myConnect.getInputStream());

		BufferedReader bR = new BufferedReader((Reader) iSR);
		boolean eof = false;
		Vector<String> fileContents = new Vector<String>();

		while (!eof) {
			String line = bR.readLine();
			if (line == null) {
				eof = true;
			} else {
				fileContents.add(line);
			}
		}
		bR.close();
		return fileContents;
	}

	/**
	 * The method returns an Object of type fullClassName using a constructor
	 * from that type with arguments matching the types supplied in the
	 * constructorArgs.
	 */
	public static Object getInstance(String fullClassName,
			Object[] constructorArgs) throws ClassNotFoundException,
			NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		Class<?> c = Class.forName(fullClassName);
		Class<?>[] constrArgClasses = new Class[constructorArgs.length];
		for (int i = 0; i < constructorArgs.length; i++) {
			constrArgClasses[i] = constructorArgs[i].getClass();
		}
		Constructor<?> constr = c.getConstructor(constrArgClasses);
		return constr.newInstance(constructorArgs);
	}

	public static String getObjectMethodName(String desiredMethodName,
			Object obj) throws NoSuchMethodException {
		String methodName = null;
		Class<? extends Object> cl = obj.getClass();
		Method[] methods = cl.getMethods();
		// loop through the method names and see if a match can be found
		for (int i = 0; i < methods.length; i++) {
			if (desiredMethodName.compareToIgnoreCase(methods[i].getName()) == 0) {
				methodName = methods[i].getName();
			}
		}
		if (methodName == null) {
			System.out.println("Error:Method with name:" + desiredMethodName
					+ " Not Found in class " + cl.getName());
			throw new NoSuchMethodException("Error:Method with name:"
					+ desiredMethodName + " Not Found in class " + cl.getName());
		}
		return methodName;
	}

	public static String getPropertiesString(Properties myProps) {

		int maxKeySize = 0;
		int maxValueSize = 0;
		Enumeration<Object> keys = myProps.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String value = myProps.getProperty(key);
			if (key.length() > maxKeySize)
				maxKeySize = key.length();
			if (value.length() > maxValueSize)
				maxValueSize = value.length();
		}

		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		// String format = "%" + maxKeySize + "s = %" + maxValueSize + "s\n";
		String format = "%-" + maxKeySize + "s = %s\n";
		Enumeration<Object> keys2 = myProps.keys();
		if (!keys2.hasMoreElements())
			sb.append("*** no properties to print ***\n");
		while (keys2.hasMoreElements()) {
			String key = (String) keys2.nextElement();
			String value = myProps.getProperty(key);
			formatter.format(format, key, value);
		}
		return sb.toString();
	}

	public static String getRootName(String scriptName) {
		String rootName;
		StringTokenizer fields = new StringTokenizer(scriptName, ".");
		// assume only one "." in bin name
		rootName = scriptName;
		if (fields.countTokens() > 0)
			rootName = fields.nextToken();
		// take the string up to the first "." and call this the root name
		return rootName;
	}

	public static Set<Object> getSet(Object[] oA) {
		Set<Object> set = Collections.synchronizedSet(new HashSet<Object>());
		for (int i = 0; i < oA.length; i++) {
			set.add(oA[i]);
		}
		return set;
	}

	public static Set<Object> getSet(Vector<?> v) {
		return getSet(v.toArray());
	}

	/**
	 * Builds Shell script File Object Written by: R. M. Kolonay
	 */
	public static File getShellScriptFile(String scriptName, String runDir) {
		String rootName = getRootName(scriptName);
		return new File(runDir + rootName + ".script");
	}

	public static File getShellScriptFile(String scriptName, String runDir,
			boolean windows) {
		String rootName = getRootName(scriptName);
		if (windows)
			return new File(runDir + rootName + ".bat");
		return new File(runDir + rootName + ".script");
	}

	public static String getStandardInLine() throws IOException {
		InputStream is = System.in;
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line = br.readLine();
		is.close();
		return line;
	}

	/**
	 * getTildePath
	 */
	public static String getTildePath(String absolutePath, String lastName) {
		StringTokenizer aST = new StringTokenizer(absolutePath, "/");

		Vector<String> relDir = new Vector<String>();
		boolean foundLastName = false;

		while (aST.hasMoreTokens()) {
			String test = aST.nextToken();
			if (test.equals(lastName)) {
				foundLastName = true;
				test = "~" + test;
			}
			if (foundLastName) {
				relDir.add(test + "/");
			}
		}

		String tildePath = "";
		for (int i = 0; i < relDir.size(); i++) {
			tildePath = tildePath + relDir.elementAt(i);
		}

		tildePath = tildePath.substring(0, tildePath.length() - 1);

		return tildePath;
	}

	/** Returns a unique set of references * */
	public static Object[] getUniqueReferences(Object[] objA) {
		Set<Object> mySet = Collections.synchronizedSet(new HashSet<Object>());
		for (int i = 0; i < objA.length; i++) {
			mySet.add(objA[i]);
		}
		Object[] myObjA = new Object[mySet.size()];
		Iterator<Object> myIter = mySet.iterator();
		int ctr = 0;
		while (myIter.hasNext()) {
			myObjA[ctr] = myIter.next();
			ctr++;
		}
		return myObjA;
	}

	/**
	 * Unpacks all Object[] in myVect and hands back a unique set of references
	 * (myVect elements must be Object[]).
	 */
	public static Object[] getUniqueReferences(Vector<Object> myVect) {
		int numElements = myVect.size();
		// System.out.print("numElements: "+numElements+"\n");
		Vector<Object> v = new Vector<Object>();
		for (int i = 0; i < numElements; i++) {
			// Object[] oA = (Object[]) myVect.elementAt(i);
			Object[] oA = new Object[] { (Object) myVect.elementAt(i) };
			// printArray(oA);
			for (int j = 0; j < oA.length; j++) {
				v.add(oA[j]);
			}
		}
		Object[] myOA = new Object[v.size()];
		for (int i = 0; i < v.size(); i++) {
			myOA[i] = v.elementAt(i);
		}
		return GenericUtil.getUniqueReferences(myOA);
	}

	/**
	 * The method returns a unique string based on time
	 * 
	 * @return String containing time represented as a long hexadecimal number
	 */
	public static String getUniqueString() {
		// String uniqueID = (new UID()).toString();
		String identifier = Long.toHexString(new Date().getTime());
		return identifier;
	}

	/**
	 * This method returns the user's current work directory
	 * 
	 * @return String containing the users current working directory
	 */
	public static String getVmDir() {
		return System.getProperty("user.dir");
	}

	public static boolean isElement(String[] array, String s) {
		//System.out.println("looking for = \"" + s + "\"");
		if (array == null || s == null || array.length == 0)
			return false;
		for (String test : array) {
			//System.out.println("test = \"" + test + "\"");
			if (test.trim().equals(s.trim()))
				return true;
		}
		return false;
	}

	/**
	 * This method returns a string containing the Operating system name
	 * 
	 * @return String containing the operating system name
	 */
	public static String whatOS() {
		return System.getProperty("os.name").trim();
	}

	/**
	 * This method returns a boolean value of true if the operating system name
	 * starts with Linux
	 * 
	 * @return Boolean value indicating if the operating system is Linux
	 */
	public static boolean isLinux() {
		if (whatOS().startsWith("Linux")) {
			return true;
		}
		return false;
	}

	/**
	 * This method returns a boolean value of true if the operating system name
	 * starts with Mac
	 * 
	 * @return Boolean value indicating if the operating system is Mac
	 */
	public static boolean isMac() {
		if (whatOS().startsWith("Mac")) {
			return true;
		}
		return false;
	}

	/**
	 * This method returns a boolean value of true if the operating system name
	 * starts with Linux and Mac
	 * 
	 * @return
	 */
	public static boolean isLinuxOrMac() {
		if (isLinux() || isMac()) {
			return true;
		}
		return false;
	}

	/**
	 * This method returns a boolean value of true if the operating system name
	 * starts with Win
	 * 
	 * @return
	 */
	public static boolean isWindows() {
		if (whatOS().startsWith("Win")) {
			return true;
		}
		return false;
	}
	
	public static boolean isWindows64() {
		if (!isWindows()) return false;
		if (System.getProperty("os.arch").contains("64")) return true;
		return false;
	}
	
	public static boolean isWindows32() {
		if (!isWindows()) return false;
		if (System.getProperty("os.arch").contains("32")) return true;
		return false;
	}

	/**
	 * Main method for the GenericUtil class
	 * 
	 * @param args
	 *            Argument array
	 */
	public static void main(String[] args) {
		if (hasArg("-h", args) || args.length == 0)
			printHelp();
		if (hasArg("-sp", args))
			printSystemProperties();
		if (hasArg("-ev", args))
			printEnvVars();
		if (hasArg("-cpDir", args)) copyDir(args);
		if (hasArg("-dl", args))
			try {
				downloadDirectory(new URL(args[1]), new File(args[2]));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	private static void copyDir(String args[]) {
		File sourceDir = new File(args[1]);
		File destDir = new File(args[2]);
		try {
			copyDirectory(sourceDir, destDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Give exert privilege to a UNIX file Written by: S. A. Burton
	 */
	public static void makeExecutable(File file) throws IOException,
			InterruptedException {
		String[] cmds2 = { "chmod", "-R", "+rx", file.getAbsolutePath() };
		Process child1 = Runtime.getRuntime().exec(cmds2);
		child1.waitFor();
	}

	public static void printArray(double[] da) {
		for (double d : da) {
			System.out.println(d);
		}
	}

	public static void printArray(int[] ia) {
		for (int i : ia) {
			System.out.println(i);
		}
	}

	public static void printArray(Object obj[]) {
		for (int i = 0; i < obj.length; i++) {
			System.out.println("GenericUtil: printArray: Index " + i + ": "
					+ obj[i]);
		}
	}

	public static String arrayToString(Object obj[]) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < obj.length; i++) {
			sb.append("GenericUtil: printArray: Index " + i + ": " + obj[i]
					+ "\n");
		}
		return sb.toString();
	}
	
	public static String arrayToMatlabStringArray(double da[]) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < da.length; i++) {
			sb.append(da[i]);
			if (i + 1 < da.length) sb.append(","); 
		}
		sb.append("]");
		return sb.toString();
	}
	
	public static String arrayToMatlabStringArray(Object da[]) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < da.length; i++) {
			sb.append(da[i]);
			if (i + 1 < da.length) sb.append(","); 
		}
		sb.append("]");
		return sb.toString();
	}
	
	public static String arrayToString(Object obj[], boolean printIndex) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < obj.length; i++) {
			if (printIndex) {
				sb.append("GenericUtil: printArray: Index " + i + ": " + obj[i] + "\n");
			} else {
				sb.append(obj[i] + "\n");
			}
		}
		return sb.toString();
	}
	public static String arrayToOneLineSpaceDelimitedString(Object obj[]) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < obj.length; i++) {
			sb.append(obj[i] + " ");
		}
		sb.append("\n");
		return sb.toString();
	}
	
	public static void printArray(Object obj[], Logger logger) {
		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		for (int i = 0; i < obj.length; i++) {
			// String msg = "GenericUtil: printArray: Index " + i + ":" +
			// obj[i];
			sb.append("GenericUtil: printArray: Index " + i + ": " + obj[i]
					+ "\n");
			// logger.info(msg);
		}
		logger.info(sb.toString());
	}

	public static void printEnvVars() {
		Map<String, String> envMap = System.getenv();
		Iterator<String> it = envMap.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			System.out.println(key + " = " + envMap.get(key));
		}
	}

	public static void printFile(File file) throws IOException {
		printVect(getFileContents(file));
	}

	public static void printFile(File file, Logger logger) throws IOException {
		if (logger == null) {
			printFile(file);
			return;
		}
		logger.info("printing file contents for: " + file);
		printVect(getFileContents(file), logger);
	}

	public static void printProperties(Properties myProps) {
		System.out.println(getPropertiesString(myProps));
	}

	public static void printSystemProperties() {
		printProperties(System.getProperties());
	}

	/**
	 * printVect writes the contents of vector to standard output. The function
	 * is originally written by S. A. Burton and modified by E. D. Thompson
	 * 
	 * @author S. A. Burton, E. D. Thompson
	 * @since JDK 1.6
	 * */
	public static void printVect(Vector<?> vect) {
		printArray(GenericUtil.vect2String(vect));
	}

	public static void printVect(Vector<?> vect, Logger logger) {
		printArray(vect2String(vect), logger);
	}

	public static Object readObjectFromFile(File file) throws IOException,
			ClassNotFoundException {
		FileInputStream fIS = new FileInputStream(file);
		ObjectInputStream oIS = new ObjectInputStream(fIS);
		Object obj = (Object) oIS.readObject();
		fIS.close();
		return obj;
	}

	public static String removeSpaces(String s) {
		StringTokenizer st = new StringTokenizer(s, " ", false);
		String t = "";
		while (st.hasMoreElements()) {
			t += st.nextElement();
		}
		return t;
	}

	/**
	 * Runs an executable program Written by: R. M. Kolonay
	 */
	public static void runExecutable(String executableName, String logFileName,
			Vector<?> fileInputFileRecords, String runDir) throws IOException,
			InterruptedException {
		String rootName;
		String executableLogFile;
		Vector<String> scriptRec = new Vector<String>();
		Vector<String> scriptCmd = new Vector<String>();

		StringTokenizer fields = new StringTokenizer(executableName, ".");
		// assume only one "." in bin name
		rootName = executableName;
		if (fields.countTokens() > 0)
			rootName = fields.nextToken();
		// take the string up to the first "." and call this the root name

		executableLogFile = logFileName;
		if (logFileName == null)
			executableLogFile = rootName + ".log";

		String executableInputFile = runDir + rootName + ".inp";
		GenericUtil.setFileContents(new File(executableInputFile),
				fileInputFileRecords);

		scriptRec.add("#Script to run " + executableName);
		scriptRec.add("cd " + runDir);
		scriptRec.add(executableName + " < " + executableInputFile);

		scriptCmd.add("sh");

		GenericUtil.runShellScript(rootName, executableLogFile, scriptRec,
				runDir, scriptCmd);
	}

	public static void runRemoteShellCommand(String host, String cmd, File dir)
			throws IOException, InterruptedException {

		String[] commands = { "rsh", host, "-n", cmd };
		String[] env = null;
		Process proc = Runtime.getRuntime().exec(commands, env, dir);

		String line;
		BufferedReader is = new BufferedReader(new InputStreamReader(
				proc.getInputStream()));
		while ((line = is.readLine()) != null) {
			System.out.println("GenericUtil: runRemoteShellCmd: "
					+ " Stream from rsh :" + line);
		}
		proc.waitFor();
		is.close();
	}

	public static void runRemoteShellScript(String machineName,
			String[] shellScript, File directory) throws InterruptedException,
			IOException {

		String tildePath = directory.getAbsolutePath();
		String fullPath = tildePath;

		File shellFile = new File(directory, "SAB.sh");
		GenericUtil.setFileContents(shellFile, shellScript);
		GenericUtil.makeExecutable(shellFile);

		// build remote shell argument
		//
		String remshCmd = "remsh " + machineName + " -n 'cd " + tildePath
				+ "; SAB.sh'";
		//
		// build script to invoke remsh (stupid java)
		//
		Vector<String> remshV = new Vector<String>();
		remshV.add(remshCmd);
		File remshFile = new File(fullPath + "/SAB.remsh");

		GenericUtil.setFileContents(remshFile, remshV);
		GenericUtil.makeExecutable(remshFile);

		// run script on remote machine
		//
		Runtime rt = Runtime.getRuntime();
		String[] temp = { "csh", "-f", "-c", fullPath + "/SAB.remsh" };
		Process proc = rt.exec(temp);

		String line;
		BufferedReader is = new BufferedReader(new InputStreamReader(
				proc.getInputStream()));
		while ((line = is.readLine()) != null) {
			System.out.println("GenericUtil: runRemoteShellScript: "
					+ "Stream from remsh :" + line);
		}
		proc.waitFor();
	}

	public static void waitForFileToExistAndBeReadable(File file,
			int maxMilliSecondsToWait, int milliSecondsBetweenChecks)
			throws Exception {

		long startTime = System.currentTimeMillis();
		while (!file.exists()
				& (System.currentTimeMillis() - startTime) < maxMilliSecondsToWait) {
			Thread.sleep(milliSecondsBetweenChecks);
		}
		while (!file.canRead()
				& (System.currentTimeMillis() - startTime) < maxMilliSecondsToWait) {
			Thread.sleep(milliSecondsBetweenChecks);
		}

		// throw exceptions if necessary
		//
		if (!file.exists())
			throw new Exception("***error: " + file.getAbsolutePath()
					+ " does not exist after waiting " + maxMilliSecondsToWait
					+ " [ms].");
		if (!file.canRead())
			throw new Exception("***error: " + file.getAbsolutePath()
					+ " cannot be read after waiting " + maxMilliSecondsToWait
					+ " [ms].");
	}

	public static void runRemoteShellScript(String machineName,
			String[] shellScript, String tildePath)
			throws InterruptedException, IOException {

		// get absolute path from tildePath on current file system
		StringTokenizer sT = new StringTokenizer(tildePath, "/");
		String lastName = sT.nextToken();
		lastName = lastName.substring(1);
		// System.out.println(lastName);
		String vmDir = GenericUtil.getVmDir();
		StringTokenizer sT2 = new StringTokenizer(vmDir, "/");
		Vector<String> baseDir = new Vector<String>();

		String test = sT2.nextToken();
		boolean foundLastName = (test.equals(lastName));

		while (!(foundLastName) && sT2.hasMoreTokens()) {
			if (test.equals(lastName)) {
				foundLastName = true;
			} else {
				baseDir.add("/" + test);
			}
			test = sT2.nextToken();
		}

		String baseDirString = "";
		for (int i = 0; i < baseDir.size(); i++) {
			baseDirString = baseDirString + baseDir.elementAt(i);
		}
		String fullPath = baseDirString + "/" + tildePath.substring(1);
		// write shellScript to file
		File shellFile = new File(fullPath + "/SAB.sh");
		GenericUtil.setFileContents(shellFile, shellScript);
		GenericUtil.makeExecutable(shellFile);
		// build remote shell argument
		String remshCmd = "remsh " + machineName + " -n 'cd " + tildePath
				+ "; SAB.sh'";

		// build script to invoke remsh (stupid java)
		Vector<String> remshV = new Vector<String>();
		remshV.add(remshCmd);
		File remshFile = new File(fullPath + "/SAB.remsh");
		GenericUtil.setFileContents(remshFile, remshV);
		GenericUtil.makeExecutable(remshFile);
		// run script on remote machine
		Runtime rt = Runtime.getRuntime();
		String[] temp = { "csh", "-f", "-c", fullPath + "/SAB.remsh" };
		Process proc = rt.exec(temp);
		String line;
		BufferedReader is = new BufferedReader(new InputStreamReader(
				proc.getInputStream()));
		while ((line = is.readLine()) != null) {
			System.out.println("GenericUtil: runRemoteShellScript: "
					+ "Stream from remsh :" + line);
		}
		proc.waitFor();
		is.close();
	}

	public static void runRemoteShellScript(String machineName,
			Vector<?> shellScript, File directory) throws IOException,
			InterruptedException {

		runRemoteShellScript(machineName, GenericUtil.vect2String(shellScript),
				directory);
	}

	public static void runRemoteShellScript(String machineName,
			Vector<?> shellScript, String tildePath) throws IOException,
			InterruptedException {

		runRemoteShellScript(machineName, vect2String(shellScript), tildePath);
	}

	public static void runRemoteShellScript2(String host, Vector<?> script,
			File directory) throws IOException, InterruptedException {
		// write shellScript to file
		File scriptFile = new File(directory, "SAB.sh");
		GenericUtil.setFileContents(scriptFile, script);
		GenericUtil.makeExecutable(scriptFile);
		// build remote shell argument
		String rshCmd = "rsh " + host + " -n 'cd " + directory.getPath()
				+ "; SAB.sh'";
		// write rsh script to file
		File rshFile = new File(directory, "SAB.rsh");
		GenericUtil.setFileContents(rshFile, new String[] { rshCmd });
		GenericUtil.makeExecutable(rshFile);
		// run script on remote machine
		Process proc = Runtime.getRuntime().exec(
				new String[] { "csh", "-f", rshFile.getPath() });
		String line;
		BufferedReader is = new BufferedReader(new InputStreamReader(
				proc.getInputStream()));
		while ((line = is.readLine()) != null) {
			System.out.println("GenericUtil: runRemoteShellScript2: "
					+ "Stream from remsh :" + line);
		}
		proc.waitFor();
		is.close();
	}

	public static Process runShellScript(File shellScriptFile,
			PrintWriter execLog, Vector<?> shellCommand) throws IOException,
			InterruptedException {
		shellCommand.trimToSize();
		GenericUtil.makeExecutable(shellScriptFile);
		int aSize = shellCommand.size() + 1;
		String[] cmdArray2 = new String[aSize];
		for (int i = 0; i < shellCommand.size(); i++)
			cmdArray2[i] = (String) shellCommand.get(i);
		cmdArray2[aSize - 1] = shellScriptFile.getPath();

		Process process = GenericUtil.execScript(shellScriptFile, cmdArray2,
				execLog);
		logger.info("sorcer.util.GenericUtil>>" + shellScriptFile.getName()
				+ " Completed with Status = " + process.exitValue());
		return process;
	}

	/**
	 * Runs a series of commands that are in a vector in a UNIX shell script
	 * 
	 * @author R. M. Kolonay, S. A. Burton
	 * @param shellScriptFile
	 *            Shell script file path
	 * @param execLog
	 *            Log file
	 * @param scriptInputRecords
	 *            Script file contents
	 * @param shellCommand
	 *            Shell command
	 * @return Script execution process
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static Process runShellScript(File shellScriptFile,
			PrintWriter execLog, Vector<?> scriptInputRecords,
			Vector<?> shellCommand) throws IOException, InterruptedException {
		scriptInputRecords.trimToSize();
		shellCommand.trimToSize();
		GenericUtil.setFileContents(shellScriptFile, scriptInputRecords);
		GenericUtil.makeExecutable(shellScriptFile);
		int aSize = shellCommand.size() + 1;
		String[] cmdArray2 = new String[aSize];
		for (int i = 0; i < shellCommand.size(); i++)
			cmdArray2[i] = (String) shellCommand.get(i);
		cmdArray2[aSize - 1] = shellScriptFile.getPath();

		Process process = GenericUtil.execScript(shellScriptFile, cmdArray2,
				execLog);
		process.waitFor();
		logger.info("sorcer.util.GenericUtil>>" + shellScriptFile.getName()
				+ " Completed with Status = " + process.exitValue());
		return process;
	}

	/**
	 * Runs a series of commands that are in a vector in a UNIX shell script
	 * Written by: S. A. Burton
	 */
	public static Process runShellScript(File shellScriptFile,
			Vector<?> scriptInputRecords, Vector<?> shellCommand)
			throws IOException, InterruptedException {
		String scriptName = shellScriptFile.getAbsolutePath();
		String rootName = getRootName(scriptName);
		PrintWriter execLog = createScriptLogFile(rootName);

		Process process = runShellScript(shellScriptFile, execLog,
				scriptInputRecords, shellCommand);
		if (execLog.checkError()) {
			System.out.println("execLog encountered errors" + execLog);
		}
		return process;
	}

	/**
	 * Runs a series of commands that are in a vector in a UNIX shell script
	 * Written by: S. A. Burton
	 */
	public static void runShellScript(File shellScriptFile,
			Vector<String> scriptInputRecords) throws IOException,
			InterruptedException {
		Vector<String> scriptCmd = new Vector<String>();
		scriptCmd.add("csh");
		scriptCmd.add("-f");
		scriptCmd.add("-c");
		runShellScript(shellScriptFile, scriptInputRecords, scriptCmd);
	}

	/**
	 * Run shell script
	 * 
	 * @param scriptFile
	 *            Script file
	 * @param scriptContents
	 *            Script file Contents
	 * @param stdout
	 *            Standard output
	 * @param stderr
	 *            Standard error
	 * @return Exit value
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int runShellScript(File scriptFile,
			List<String> scriptContents, File stdout, File stderr)
			throws IOException, InterruptedException {
		// Script command
		List<String> scriptCommand = new ArrayList<String>();

		// Set script file conentents
		setFileContents(scriptFile, scriptContents);

		// Make script file executable
		scriptFile.setExecutable(true);

		// Determine platform
		if (isLinuxOrMac()) {
			// *nix (Gnu/Linux, Mac OS, ...)
			scriptCommand.add("sh");
			scriptCommand.add(scriptFile.getAbsolutePath());
		} else {
			// Windows

			if (!scriptFile.getName().endsWith(".bat")) {
				// If the script file name does not end with .bat add .bat to
				// end of the pat
				scriptFile.renameTo(new File(scriptFile.getAbsolutePath()
						+ ".bat"));
			}

			scriptCommand.add("cmd");
			scriptCommand.add("/C");
			scriptCommand.add(scriptFile.getAbsolutePath());
		}

		ProcessBuilder pb = new ProcessBuilder(scriptCommand);
		// Redirecting standard output and standard error
		// JAVA 7
		// pb.redirectOutput(stdout);
		// pb.redirectError(stderr);
		// Starting process
		Process p = pb.start();

		// JAVA 6 and below
		redirectOutput(p, stdout);
		redirectError(p, stderr);

		// Wait for the process then return the exit value
		return p.waitFor();
	}

	/**
	 * Redirecting the standard output of a process to a file
	 * 
	 * @param p
	 *            Process
	 * @param file
	 *            File
	 * @throws IOException
	 */
	public static void redirectOutput(Process p, File file) throws IOException {
		new Thread(new RedirectInputStreamRunnable(p.getInputStream(), file))
				.run();
	}

	/**
	 * Redirecting the standard error of a process to a file
	 * 
	 * @param p
	 *            Process
	 * @param file
	 *            File
	 * @throws IOException
	 */
	public static void redirectError(Process p, File file) {
		new Thread(new RedirectInputStreamRunnable(p.getErrorStream(), file))
				.run();
	}

	/**
	 * Class starts a thread that redirects and input stream to file
	 * 
	 * @author E. D. Thompson
	 * 
	 */
	public static class RedirectInputStreamRunnable implements Runnable {
		InputStream i;
		File f;

		RedirectInputStreamRunnable(InputStream i, File f) {
			this.i = i;
			this.f = f;
		}

		public void run() {
			try {
				redirectInputStream2File(i, f);
			} catch (IOException e) {
				StringBuilder msg = new StringBuilder(
						"Attempted to redirect input stream [")
						.append(i.toString()).append("] to file [")
						.append(f.getAbsolutePath()).append("]");
				System.err.print(msg.toString());
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method redirects an input stream to a file
	 * 
	 * @param is Input stream
	 * @param file File
	 * @throws IOException
	 */
	public static void redirectInputStream2File(InputStream is, File file)
			throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            int bufferSize = 4096;
//            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) { // EOF
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            is.close();
        } catch (IOException e) {
            throw(e);
        } finally {
            if (fos != null) {
                fos.close();
                fos = null;
            }
            if (is != null) {
                is.close();
                is = null;
            }
        }
	}
//	private static Vector<String> getJobControlWrapperScript(File childScriptFile) {
//		return getJobControlWrapperScript(childScriptFile, null, 999);
//	}

	private static Vector<String> getJobControlWrapperScript(File childScriptFile
			, Vector<String> extraKillTimeCommands, int wrapperId) {
		
		Vector<String> script = new Vector<String>();
		script.add("#!/bin/bash");
		
		String wrapperLog = childScriptFile.getName() + "_wrap" + wrapperId + ".log";
		
		// kill process function
		script.add("function killChildProcess () {");
		script.add("\tfor child in `ps -ef | awk -v parent=$1 '{if ($3==parent){print $2}}'`; do");
		script.add("\t\tkillChildProcess $child");
		script.add("\tdone");
		script.add("\techo \"attempting to kill process (dropping kill.txt too) $1:\"");
		script.add("\techo \"attempting to kill process (dropping kill.txt too) $1:\" >> " + wrapperLog);
		script.add("\techo genericUtilFileWrapperScript>kill.txt");
		script.add("\tsleep 2");
		script.add("\t#ps -efp $1");
		script.add("\tkill -9 $1");
		script.add("\techo \"waiting for process $1 to die...\"");
		script.add("\techo \"waiting for process $1 to die...\" >> " + wrapperLog);
		script.add("\twait $1");
		script.add("\techo \"process $1 is dead.\"");
		script.add("\techo \"process $1 is dead.\" >> " + wrapperLog); 
		script.add("}");
		script.add("function killChildProcess0 () {");
		script.add("\tfor child in `ps -ef | awk -v parent=$1 '{if ($3==parent){print $2}}'`; do");
		script.add("\t\techo \"killChildProcess0: parent=$1; child=$child\"");
		script.add("\t\techo \"killChildProcess0: parent=$1; child=$child\" >> " + wrapperLog);
		script.add("\t\tkillChildProcess $child");
		script.add("\tdone");
		script.add("}");
		
		// for windows safety (cygwin)
		script.add("export PATH=$PATH:/bin");
		
		// to supress cygwin (on windows) std err warning
		//
		script.add("export CYGWIN=\"nodosfilewarning\"");
		
		
		script.add("THIS_PID=$$");
		script.add("echo \"THIS_PID=$THIS_PID\"");
		script.add("echo \"THIS_PID=$THIS_PID\" >> " + wrapperLog);
		script.add(childScriptFile.getAbsolutePath().replace("\\", "/") + " &");
		script.add("SCRIPT_PID=$!");
		script.add("echo \"SCRIPT_PID=$SCRIPT_PID\"");
		script.add("echo \"SCRIPT_PID=$SCRIPT_PID\" >> " + wrapperLog);
		//script.add("sleep 2");
		script.add("scriptState=\"RUNNING\"");
		script.add("while [ $scriptState != \"DONE\" ]; do");
		script.add("\techo \"0 scriptState = $scriptState\"");   
		script.add("\techo \"0 scriptState = $scriptState\" >> " + wrapperLog);   
		script.add("\techo \"calling read\"");   
		script.add("\techo \"calling read\" >> " + wrapperLog);   
		script.add("\tread -t 2 line");
		script.add("\techo \"line=$line\"");
		script.add("\techo \"line=$line\" >> " + wrapperLog);
		//script.add("\tps axo pid,ppid,cmd");
		script.add("\tif [ -z \"$line\" ]; then");
		script.add("\t\tline=\"null\"");
		script.add("\tfi");
		script.add("\techo \"line1=$line\"");
		script.add("\techo \"line1=$line\" >> " + wrapperLog);
		script.add("\tif [ $line = \"KILL\" ]; then"); 
//		script.add("\t\tCHILD=`ps axo pid,ppid | awk -F\" \" -v p=\"$SCRIPT_PID\" '{if ($2~p) {print $1; exit}}'`");
//		script.add("\t\techo \"CHILD=$CHILD\"");
//		script.add("\t\tif [ -n \"$CHILD\" ]; then");
//		script.add("\t\t\techo \"killing CHILD=$CHILD\"");
//		script.add("\t\t\tkill -9 $CHILD");
//		//script.add("\t\t\tkill -TERM $CHILD");
//		script.add("\t\tfi");
		script.add("\t\techo \"received kill command from java; dropping kill.txt file...\"");
		script.add("\t\techo \"received kill command from java; dropping kill.txt file...\" >> " + wrapperLog);
		script.add("\t\techo kill>kill.txt");
		if (extraKillTimeCommands != null && extraKillTimeCommands.size() > 0) {
			script.addAll(extraKillTimeCommands);
			script.add("\t\tsleep 2");
		}
		
		//script.add("\t\tkillChildProcess0 $SCRIPT_PID");
		script.add("\t\techo \"DONE dropping kill.txt file; killing child processes...\"");
		script.add("\t\techo \"DONE dropping kill.txt file; killing child processes...\" >> " + wrapperLog);
		script.add("\t\tkillChildProcess0 $THIS_PID");
		script.add("\t\techo \"DONE killing child processes; exiting with status = 1.\"");
		script.add("\t\techo \"DONE killing child processes; exiting with status = 1.\" >> " + wrapperLog);
		script.add("\t\texit 1");
		script.add("\tfi");
		script.add("\t# check to see if child is still running");
		//script.add("\tIS_RUN=`ps -p $SCRIPT_PID | awk -v p=\"$SCRIPT_PID\" 'BEGIN{flag=0} {if ($0~p) {flag=1}} END{print flag}'`");
		script.add("\tif [ -f psLoop.txt ]; then mv psLoop.txt psLoop0.txt; fi");
		script.add("\tIS_RUN=0");
		script.add("\tIS_RUN=`ps -p $SCRIPT_PID > psLoop.txt ; awk -v p=\"$SCRIPT_PID\" 'BEGIN{flag=0} {if ($1~p) {flag=1}} END{print flag}' psLoop.txt`");
		//script.add("\tIS_RUN=`ps -p $SCRIPT_PID > psLoop.txt ; awk -v p=\"$SCRIPT_PID\" 'BEGIN{flag=0} {if ($0~p) {flag=1}} END{print flag}' psLoop.txt`");
		script.add("\techo \"loop: IS_RUN = $IS_RUN\"");
		script.add("\techo \"loop: IS_RUN = $IS_RUN\" >> " + wrapperLog);

		//script.add("\tif ps -p $SCRIPT_PID > /dev/null; then"); 
		script.add("\tscriptState=\"RUNNING\"");
		script.add("\tif [ $IS_RUN -eq 0 ]; then");
		script.add("\t\tsleep 1");
		//script.add("\t\tIS_RUN=`ps -p $SCRIPT_PID > psLoopDc.txt ; awk -v p=\"$SCRIPT_PID\" 'BEGIN{flag=0} {if ($0~p) {flag=1}} END{print flag}' psLoopDc.txt`");
		script.add("\t\tIS_RUN=`ps -p $SCRIPT_PID > psLoopDc.txt ; sleep 1; awk -v p=\"$SCRIPT_PID\" 'BEGIN{flag=0} {if ($1~p) {flag=1}} END{print flag}' psLoopDc.txt`");
		script.add("\t\techo \"loop2: IS_RUN = $IS_RUN\"");
		script.add("\t\techo \"loop2: IS_RUN = $IS_RUN\" >> " + wrapperLog);

		script.add("\t\tif [ $IS_RUN -eq 0 ]; then");
		script.add("\t\t\tscriptState=\"DONE\"");
		script.add("\t\tfi");
		script.add("\tfi");
		script.add("\techo \"1 scriptState = $scriptState\"");   
		script.add("\techo \"1 scriptState = $scriptState\" >> " + wrapperLog);  
		script.add("done");

		
		//script.add("#wait $SCRIPT_PID");
		//script.add("#EXIT_CODE=$?");
		//script.add("IS_RUN=`ps -p $SCRIPT_PID | awk -v p=\"$SCRIPT_PID\" 'BEGIN{flag=0} {if ($0~p) {flag=1}} END{print flag}'`");
		
//		script.add("\tsleep 1");
//		script.add("\tIS_RUN=0");
//		script.add("\tIS_RUN=`ps -p $SCRIPT_PID > psCheck.txt ; awk -v p=\"$SCRIPT_PID\" 'BEGIN{flag=0} {if ($0~p) {flag=1}} END{print flag}' psCheck.txt`");
//		script.add("\techo \"check: IS_RUN = $IS_RUN\" >> " + wrapperLog);
//		script.add("if [ $IS_RUN -eq 0 ]; then");
//		script.add("\t# need to capture background process exit code");
//		script.add("\t#");
//		script.add("\tEXIT_CODE=0");
//		script.add("\techo \"calling wait to get exit code\"");
//		script.add("\techo \"calling wait to get exit code\" >> " + wrapperLog);
		
		// drop kill.txt just in case background process didn't die and is checking for kill.txt
		script.add("echo \"dropping kill.txt for good measure...\"");
		script.add("echo \"dropping kill.txt for good measure...\" >> " + wrapperLog);
		script.add("echo good_measure_kill>kill.txt");

		
		// calling wait with $SCRIPT_PID has been causing a problem on cygwin; just 'wait' should
		// work
		//script.add("echo \"waiting for $SCRIPT_PID...\"");
		//script.add("echo \"waiting for $SCRIPT_PID...\" >> " + wrapperLog);
		script.add("echo \"waiting for background processes...\"");
		script.add("echo \"waiting for background processes...\" >> " + wrapperLog);
		script.add("wait");
//		script.add("wait $SCRIPT_PID");
		script.add("EXIT_CODE=$?");
		
		script.add("echo \"done waiting, setting exit code from background process = $EXIT_CODE\"");
		script.add("echo \"done waiting, setting exit code from background process = $EXIT_CODE\" >> " + wrapperLog );
		//script.add("fi");

//		script.add("echo \"0 ps coming====v\"");
//		script.add("echo \"0 ps coming====v\" >> " + wrapperLog);
//		script.add("ps ");
//		script.add("ps >> " + wrapperLog);
		
//		script.add("if [ $IS_RUN -eq 1 ]; then");
//		script.add("\techo \"SCRIPT_PID=$SCRIPT_PID is still running and shouldn't be; killing...\"");
//		script.add("\techo \"1 ps coming====v\"");
//		script.add("\techo \"1 ps coming====v\" >> " + wrapperLog);
//		script.add("\tps ");
//		script.add("\tps >> " + wrapperLog);
//		script.add("\techo \"because pid = $SCRIPT_PID is still running; dropping kill.txt file...\" >> " + wrapperLog);
//		script.add("\techo \"because pid = $SCRIPT_PID is still running; dropping kill.txt file...\"");
//		script.add("\techo \"killed by genericutil wrapper script\">kill.txt");		
//		script.add("\techo \"DONE dropping kill.txt file due to pid $SCRIPT_PID running; killing child processes...\" >> " + wrapperLog);
//		script.add("\techo \"DONE dropping kill.txt file due to pid $SCRIPT_PID running; killing child processes...\"");
//		script.add("\tkillChildProcess0 $SCRIPT_PID");
//		script.add("\tkill -9 $SCRIPT_PID");
//		script.add("\techo \"DONE killing SCRIPT_PID=$SCRIPT_PID.\"");
//		script.add("\tEXIT_CODE=007");
//		script.add("fi");
		script.add("echo \"exiting with code = $EXIT_CODE\"");
		script.add("echo \"exiting with code = $EXIT_CODE\" >> " + wrapperLog);
		script.add("exit $EXIT_CODE");
		
		return script;
	}
	
	public static String getFileNameWithoutExtension(File file) {
		String name = file.getName();
		int dotIdx = name.indexOf(".");
		if (dotIdx > 0) {
			return name.substring(0, dotIdx);
		}
		return name;
	}
	
		
	// this method exits the jvm if the file or directory is not readable; the exit is
	// necessary for boot strapping providers since exceptions in provider constructors
	// are simply caught and ignored...exit brings the provider down, which is good.
	public static void checkFileExistsAndIsReadable(File file, ServiceProvider sp) {
		
		try {
			
			if(!file.exists()) {
                logger.warn("***error: file does not exist or is not readable = "+ file.getAbsolutePath());
				if (sp != null) sp.destroy();
				throw new IOException("***error: file does not exist or is not readable = " 
						+ file.getAbsolutePath());
				
			}
					
			if (!file.canRead()){
                logger.warn("***error: file does not have read permission = "+ file.getAbsolutePath());
				if (sp != null) sp.destroy();				
				throw new IOException("***error: file does not have read permission = "+ file.getAbsolutePath());
			}
					
		} catch (IOException e) {
            logger.warn("***error:  problem with file = " + file.getAbsolutePath(), e);
			throw new RuntimeException(e);
		}
	}
	
	public static int runShellScript(File scriptFile,
			Vector<String> scriptContents, File logFile, long timeout,
			boolean printStdOut, boolean printStdError,
			boolean doSynchronizedLaunch) throws Exception {
		
		return runShellScript(scriptFile, scriptContents, logFile, timeout, printStdOut
				, printStdError, doSynchronizedLaunch, null);
		
	}

	public static File getCygwinHome() throws Exception {

		String cygwinHome = null;

		String nativeOpenDistHome = System.getProperty("native.open.dist");
		if (nativeOpenDistHome != null) {
			File nativeOpenHome = new File(System.getProperty("native.open.dist"));
			if (nativeOpenHome.exists() && nativeOpenHome.canRead()) {
				File cygwinHomeF = new File(nativeOpenHome, "win/cygwin");
				if (cygwinHomeF.exists() && cygwinHomeF.canRead()) {
					cygwinHome = cygwinHomeF.getAbsolutePath().replace("\\", "/");
				}
			}
		}

		if (cygwinHome == null) {
			String cygwinHomeTest = GenericUtil.getEnvVarNoException("CYGWIN_HOME").trim().replace("\\", "/");
			File cygwinHomeFile = new File(cygwinHomeTest);
			if ((cygwinHomeFile.exists()) && (cygwinHomeFile.canRead())) {
				cygwinHome = cygwinHomeTest;
			}
		}

		String msg = "***warn: cannot determine cygwin home.";
		if (cygwinHome == null) {
			logger.warn(msg);
			throw new Exception(msg);
		}

		return new File(cygwinHome);
	}

	public static boolean cygwinHomeExists() {
		try {
			getCygwinHome();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private static int wrapperScriptId = 0;
	/**
	 * Runs a series of commands that are in a vector in a UNIX or 
	 * Windows shell script
	 * 
	 * @author S. A. Burton
	 * @param scriptFile
	 *            Script file path
	 * @param scriptContents
	 *            Vector of strings containing the script file contents
	 * @param logFile
	 *            Log file path
	 * @param timeout
	 *            Time out period
	 * @param printStdOut
	 *            Print standard output flag
	 * @param printStdError
	 *            Print standard error flag
	 * @param doSynchronizedLaunch
	 *            Do synchronized launch flag
	 * @param extraKillTimeCommands
	 *            Commands placed in bash shell and invoked when timout is reached followed
	 *            by standard killing of child processes; this is usefull in Windows
	 *            and possibly other OS when a provider uses a script to run compiled Matlab
	 *            which, in-turn, makes a system call to a native code.  In this case, the
	 *            parent-child relationship of process ids appears to be lost (at least on
	 *            Windows) so specific commands to kill child process may be included in
	 *            this argument.  For example,
	 *            
	 *            wmic /interactive:off process where name=\"analyzeWingNoise.exe\" delete
	 *	     	  wmic /interactive:off process where name=\"NAFNoise.exe\" delete
	 *
	 *			 kill all processes matching the exec names in "".
	 *
	 * @return Exit value of exert command with worker method
	 * @throws Exception 
	 */
	public static int runShellScript(File scriptFile,
			Vector<String> scriptContents, File logFile, long timeout,
			boolean printStdOut, boolean printStdError,
			boolean doSynchronizedLaunch, Vector<String> extraKillTimeCommands) throws Exception {

		File executionDir = scriptFile.getParentFile();
		
		String[] scriptCommand = new String[3];
//		boolean isWrapper = false;
		//if (isLinuxOrMac()) {
			
			// create wrapper script
			//
			int wrapperId;
			synchronized (GenericUtil.class) {
				wrapperId = wrapperScriptId++;
			}
			Vector<String> wrapperScript = getJobControlWrapperScript(scriptFile, extraKillTimeCommands, wrapperId);
			File wrapperScriptFile = new File(scriptFile.getParentFile(), getFileNameWithoutExtension(scriptFile) 
					+ "_wrap" + wrapperId + ".sh");
			setFileContents(wrapperScriptFile, wrapperScript);
			//makeExecutable(wrapperScriptFile);
			wrapperScriptFile.setExecutable(true);
			
//			isWrapper = true;
			
		if (isLinuxOrMac()) {	
			logger.info("doing linux/mac system call...");
			scriptCommand[0] = "sh";
			//scriptCommand[1] = scriptFile.getAbsolutePath();
			scriptCommand[1] = wrapperScriptFile.getAbsolutePath();
			scriptCommand[2] = " ";
			logger.info("doing linux/mac system call, cmd = "
					+ scriptCommand[0]);
			logger.info("doing linux/mac system call, arg = "
					+ scriptCommand[1]);	
		} else {
			logger.info("doing windows system call...");

			scriptCommand[0] = "cmd";
			scriptCommand[1] = "/C";

			//String iGridHome = GenericUtil.getEnvVar("SORCER_HOME").trim().replace("\\", "/");
			
			String shExec = null;
//			String cygwinHome = null;
//
//			String nativeOpenDistHome = System.getProperty("native.open.dist");
//			if (nativeOpenDistHome != null) {
//				File nativeOpenHome = new File(System.getProperty("native.open.dist"));
//				if (nativeOpenHome.exists() && nativeOpenHome.canRead()) {
//					File cygwinHomeF = new File(nativeOpenHome, "win/cygwin");
//					if (cygwinHomeF.exists() && cygwinHomeF.canRead()) {
//						cygwinHome = cygwinHomeF.getAbsolutePath().replace("\\", "/");
//					}
//				}
//			}
//
//			if (cygwinHome == null) {
//				String cygwinHomeTest = GenericUtil.getEnvVarNoException("CYGWIN_HOME").trim().replace("\\", "/");
//				File cygwinHomeFile = new File(cygwinHomeTest);
//				if ((cygwinHomeFile.exists()) && (cygwinHomeFile.canRead())) {
//					cygwinHome = cygwinHomeTest;
//				}
//			}
//
//			String msg = "***warn: CYGWIN_HOME location is not readable: "
//			if (cygwinHome == null) logger.warn(msg);

//			logger.info("cygwinHomeTest = " + cygwinHomeTest);
//			if (cygwinHomeTest != null) {
//				File cygwinHomeFile = new File(cygwinHomeTest);
//				if ((cygwinHomeFile.exists()) && (cygwinHomeFile.canRead())) {
//					cygwinHome = cygwinHomeTest;
//				} else {
//							+ cygwinHomeTest;
//					System.out.println(msg);
//
//				}
//			}
			
//			// try default location fo cygwin home
//			if (cygwinHome == null) {
//				File cygwinHomeFile = new File(iGridHome + "/../Library/cygwin");
//				if ((cygwinHomeFile.exists()) && (cygwinHomeFile.canRead())) {
//					cygwinHome = cygwinHomeTest;
//				} else {
//					String msg = "***error: the environment variable CYGWIN_HOME is not set correctly."
//							+ "(Check env and make sure to use DOS file path format.)";
//					logger.error(msg);
//					throw new Exception(msg);
//				}
//			}

			String cygwinHome = (getCygwinHome()).getAbsolutePath().replace("\\", "/");

			shExec = cygwinHome + "/bin/sh.exe";
			try {
				GenericUtil.checkFileExistsAndIsReadable(new File(shExec), null);
			} catch (Exception e) {
				String msg = "***error: the cygwin \"sh.exe\" must be installed in: " + shExec;
				logger.error(msg, e);
				throw e;
			}
			
			//scriptCommand[2] = "\"E:\\LibraryBack\\cygwin\\bin\\sh " + wrapperScriptFile.getAbsolutePath() + "\"";
			scriptCommand[2] = shExec + " " +  wrapperScriptFile.getAbsolutePath() + "\"";
			
			logger.info("doing windows system call, cmd = " + scriptCommand[0]);
			logger.info("doing windows system call, arg1 = " + scriptCommand[1]);
			logger.info("doing windows system call, arg2 = " + scriptCommand[2]);

		}
		setFileContents(scriptFile, scriptContents);
//		if (isLinuxOrMac())
//			makeExecutable(scriptFile);
		scriptFile.setExecutable(true);

//		int exitValue = executeCommandWithWorker(scriptCommand, printStdOut,
//				printStdError, timeout, executionDir, logFile,
//				doSynchronizedLaunch, isWrapper);
		int exitValue = executeCommandWithWorker(scriptCommand, printStdOut,
				printStdError, timeout, executionDir, logFile,
				doSynchronizedLaunch);
		
		return exitValue;
	}

	/**
	 * Runs a series of commands that are in a vector in a UNIX shell script
	 * 
	 * @author E. D. Thompson and S. A. Burton
	 * @param scriptFile
	 *            Script file path
	 * @param scriptContents
	 *            List of strings containing the script file contents
	 * @param outFile
	 *            Standard out file path
	 * @param errFile
	 *            Standard error file path
	 * @param jobCheckInterval
	 *            Time to wait between job status checks
	 * @return Exit value of exert command with worker method
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int runPBSShellScript(File scriptFile,
			List<String> scriptContents, File outFile, File errFile,
			long jobCheckInterval) throws IOException, InterruptedException {

		File executionDir = scriptFile.getParentFile();
		File jobSubmitFile = new File(executionDir, "pbs_jobSubmit.sh");
		File jobSubmitOutFile = new File(executionDir, "pbs_jobSubmit.out");
		File jobSubmitErrFile = new File(executionDir, "pbs_jobSubmit.err");
		File jobIDFile = new File(executionDir, "pbs_jobid.txt");
		File checkJobFile = new File(executionDir, "pbs_checkJob.sh");
		File jobCheckOutFile = new File(executionDir, "pbs_checkJob.out");
		File jobCheckErrFile = new File(executionDir, "pbs_checkJob.err");
		File jobStatusFile = new File(executionDir, "pbs_jobStatusStdErr.txt");
		List<String> jobSubmitCont = new ArrayList<String>();
		List<String> jobCheckCont = new ArrayList<String>();

		String jobID = new String();
		boolean jobInQueue = true;
		int status = 0;

		// Creating the script file
		setFileContents(scriptFile, scriptContents);

		// Changing the script file permissions
		if (isLinuxOrMac()) {
			makeExecutable(scriptFile);
		}

		// Queue job submission file contents
		jobSubmitCont.add("#!/bin/sh");
		jobSubmitCont.add("qsub " + scriptFile.getAbsolutePath() + " 1> "
				+ jobIDFile.getAbsolutePath());
		jobSubmitCont.add("exit 0;");
		// Creating the script file
		setFileContents(jobSubmitFile, jobSubmitCont);
		// Changing the script file permissions
		if (isLinuxOrMac()) {
			makeExecutable(jobSubmitFile);
		}

		// Running a script file that submits a job to the queue and redirects
		// standard output
		logger.info("Submitting script file [" + scriptFile.getAbsolutePath()
				+ "] to the PBS queue.");
		status = runShellScript(jobSubmitFile, jobSubmitCont, jobSubmitOutFile,
				jobSubmitErrFile);

		// Getting the job identification number
		jobID = getFileContents(jobIDFile).get(0).trim();

		// Queue job check script file contents
		jobCheckCont.add("#!/bin/sh");
		jobCheckCont.add("qstat " + jobID + " 2> "
				+ jobStatusFile.getAbsolutePath());
		jobCheckCont.add("exit 0;");

		// Job status unknown
		String jsu = new String("qstat: Unknown Job Id " + jobID);

		// Checking the queue
		while (jobInQueue) {
			// Waiting
			wait_timer(jobCheckInterval);

			// Running a script file that checks the queue for the job and
			// redirects standard error
			runShellScript(checkJobFile, jobCheckCont, jobCheckOutFile,
					jobCheckErrFile);

			// Write the job status check standard output to a string vector
			List<String> jobStatus = getFileContents(jobStatusFile);

			// Check for "Unknown Job Id ..." string
			if (jobStatus.size() > 0) {
				if (jobStatus.get(0).contains(jsu)) {
					jobInQueue = false;
				}
			}
		}
		// Deleting scripts and log files
		jobSubmitFile.delete();
		jobSubmitOutFile.delete();
		jobSubmitErrFile.delete();
		jobIDFile.delete();
		checkJobFile.delete();
		jobCheckOutFile.delete();
		jobCheckErrFile.delete();
		jobStatusFile.delete();
		return status;
	}

	public static int runSLURMShellScript(File scriptFile,
			List<String> scriptContents, File outFile, File errFile,
			long jobCheckInterval) throws IOException, InterruptedException {

		File executionDir = scriptFile.getParentFile();
		File jobSubmitFile = new File(executionDir, "slurm_jobSubmit.sh");
		File jobSubmitOutFile = new File(executionDir, "slurm_jobSubmit.out");
		File jobSubmitErrFile = new File(executionDir, "slurm_jobSubmit.err");
		File jobIDFile = new File(executionDir, "slurm_jobid.txt");
		File checkJobFile = new File(executionDir, "slurm_checkJob.sh");
		File jobCheckLogFile = new File(executionDir, "slurm_jobSubmit.log");
		File jobStatusFileOUT = new File(executionDir,
				"slurm_jobstatus_out.txt");
		File jobStatusFileERR = new File(executionDir,
				"slurm_jobstatus_errior.txt");
		List<String> jobSubmitCont = new ArrayList<String>();
		List<String> jobCheckCont = new ArrayList<String>();

		String jobID = new String();
		boolean jobInQueue = true;
		int status = 0;

		// Creating the script file
		setFileContents(scriptFile, scriptContents);

		// Changing the script file permissions
		if (isLinuxOrMac()) {
			makeExecutable(scriptFile);
		}

		// Queue job submission file contents
		jobSubmitCont.add("#!/bin/sh");
		jobSubmitCont.add("sbatch " + scriptFile.getAbsolutePath() + " 1> "
				+ jobIDFile.getAbsolutePath());
		jobSubmitCont.add("exit 0;");
		// Creating the script file
		setFileContents(jobSubmitFile, jobSubmitCont);
		// Changing the script file permissions
		if (isLinuxOrMac()) {
			makeExecutable(jobSubmitFile);
		}

		// Running a script file that submits a job to the queue and redirects
		// standard output
		logger.info("Submitting script file [" + scriptFile.getAbsolutePath()
				+ "] to the SLURM queue.");
		status = runShellScript(jobSubmitFile, jobSubmitCont, jobSubmitOutFile,
				jobSubmitErrFile);

		// Getting the job identification number
		jobID = getFileContents(jobIDFile).get(0).trim().split(" ")[3];

		// Queue job check script file contents
		jobCheckCont.add("#!/bin/sh");
		jobCheckCont.add("squeue -j " + jobID);
		jobCheckCont.add("exit 0;");

		// Checking the queue
		while (jobInQueue) {
			// Running a script file that checks the queue for the job and
			// redirects standard error
			status += runShellScript(checkJobFile, jobCheckCont,
					jobStatusFileOUT, jobStatusFileERR);
			// Write the job status check standard output to a string vector
			List<String> jobStatusOUT = getFileContents(jobStatusFileOUT);
			List<String> jobStatusERR = getFileContents(jobStatusFileERR);
			// Checking standard out
			for (int i = 0; i < jobStatusOUT.size(); i++) {
				if (jobStatusOUT.size() == 1) {
					jobInQueue = false;
					break;
				}
			}
			// Checking standard error
			for (int i = 0; i < jobStatusERR.size(); i++) {
				if (jobStatusERR.get(i).contains(
						"slurm_load_jobs error: Invalid job id specified")) {
					jobInQueue = false;
					break;
				}
			}
			wait_timer(jobCheckInterval);
		}

		// Deleting scripts and log files
		jobSubmitFile.delete();
		jobSubmitOutFile.delete();
		jobSubmitErrFile.delete();
		jobIDFile.delete();
		checkJobFile.delete();
		jobCheckLogFile.delete();
		jobStatusFileOUT.delete();
		jobStatusFileERR.delete();
		return status;
	}

	/**
	 * This function wait for a specified number of milliseconds
	 * 
	 * @param waitTime_ms
	 */
	public static void wait_timer(long waitTime_ms) {
		long sTime = System.currentTimeMillis();
		long eTime = System.currentTimeMillis();
		long elapse = eTime - sTime;
		while (elapse < waitTime_ms) {
			eTime = System.currentTimeMillis();
			elapse = eTime - sTime;
		}
		return;
	}

	public static int runShellScript(File scriptFile,
			Vector<String> scriptContents, File logFile, long timeout,
			boolean doSynchronizedLaunch) throws Exception {
		return runShellScript(scriptFile, scriptContents, logFile, timeout,
				false, true, doSynchronizedLaunch);
	}

	public static int runShellScript(File scriptFile,
			Vector<String> scriptContents, File logFile, long timeout)
			throws Exception {
		return runShellScript(scriptFile, scriptContents, logFile, timeout,
				false, true, true);
	}
	
//	public static int runShellScript(File scriptFile, Vector<String> scriptContents) throws Exception {
//		return runShellScript(scriptFile, scriptContents
//				, new File(scriptFile.getParent()
//						, scriptFile.getName() + ".log"), 0, false, true, true);
//	}

	/**
	 * Runs a series of commands that are in a vector in a UNIX shell script
	 * 
	 * @author R. M. Kolonay, S. A. Burton
	 */
	public static Process runShellScript(String scriptName, String logFileName,
			Vector<?> scriptInputRecords, String runDir, Vector<?> shellCommand)
			throws IOException, InterruptedException {
		// create shell script file from name
		File shellScriptFile = GenericUtil.getShellScriptFile(scriptName,
				runDir);
		// create shell script log file from name
		String rootName = getRootName(scriptName);
		PrintWriter execLog = null;
		if (logFileName == null) {
			execLog = createScriptLogFile(runDir + rootName);
		} else {
			execLog = new PrintWriter(
					new FileOutputStream(runDir + logFileName));
		}
		return GenericUtil.runShellScript(shellScriptFile, execLog,
				scriptInputRecords, shellCommand);
	}

	public static Process runShellScript(String scriptName, String logFileName,
			Vector<?> scriptInputRecords, String runDir,
			Vector<?> shellCommand, boolean windows) throws IOException,
			InterruptedException {
		// create shell script file from name
		File shellScriptFile = GenericUtil.getShellScriptFile(scriptName,
				runDir, windows);
		// create shell script log file from name
		String rootName = getRootName(scriptName);
		PrintWriter execLog = null;
		if (logFileName == null) {
			execLog = createScriptLogFile(runDir + rootName);
		} else {
			execLog = new PrintWriter(
					new FileOutputStream(runDir + logFileName));
		}
		return GenericUtil.runShellScript(shellScriptFile, execLog,
				scriptInputRecords, shellCommand);
	}

	public static void runShellScript(Vector<String> script)
			throws Exception {
		runShellScript(new File("genericUtil.sh"), script);
	}

	/**
	 * Set the file contents stored in a Collection of Objects
	 * 
	 * @param file
	 *            File the objects will be stored in
	 * @param fileContents
	 *            Collection of objects
	 * @throws FileNotFoundException
	 */
	public static void setFileContents(File file, List<?> fileContents)
			throws FileNotFoundException {
		
		File parentDir = new File(file.getAbsolutePath()).getParentFile();
		if (!(parentDir.exists())) parentDir.mkdirs();
		
		Iterator<?> iterator = fileContents.iterator();
		PrintWriter fout = new PrintWriter(file);
		while (iterator.hasNext()) {
			Object object = iterator.next();
			//fout.println(object); // this will put \n\r in windows screwing up cygwin
			fout.print(object.toString() + "\n");
		}
		fout.close();
	}

	/**
	 * setFileContents writes an array of string to file. This function was
	 * written by S. A. Burton but modified by E. D. Thompson
	 * 
	 * @author S. A. Burton, E. D. Thompson
	 */
	public static void setFileContents(File file, String[] sA)
			throws IOException {
		if (sA == null) throw new IOException("***nothing to write; string[] is null.");
		// Creating a vector of strings
		List<String> fileContents = new ArrayList<String>();
		// Looping over a string array
		for (int i = 0; i < sA.length; i++) {
			// Adding string array element i to file contents vector of strings
			fileContents.add(sA[i]);
		}
		// Writing file contents to file
		setFileContents(file, fileContents);
	}

	public static void setFileContents(File file, String string)
			throws IOException {
		setFileContents(file, new String[] { string });
	}

	/**
	 * setFileContents writes the contents of a vector of a string to a file.
	 * The function receives as arguments an output file path and a vector. This
	 * function is written by S. A. Burton and modified by E. D. Thompson
	 * 
	 * @author S. A. Burton
	 * @since JDK 1.6
	 *
	 */
	public static void setFileContents(File file, Vector<?> fileContents)
			throws IOException {
		writeVectorToFile(file, fileContents);
	}

	/**
	 * spaceDelimit - Divides a string by spaces and stores the contents into a
	 * vector of spaces
	 * 
	 * @param str
	 *            String to be split
	 * @return Vector containing the split string
	 * 
	 * @author E. D. Thompson
	 * @since JDK 1.6
	 */
	public static Vector<String> spaceDelimit(String str) {
		Vector<String> strVect = new Vector<String>();
		String str1 = str.trim();
		int i = 0, f = 0, l = 0;
		while (i < str1.length()) {
			if (str1.charAt(i) != ' ') {
				f = i;
				i++;
				while ((i < str1.length()) && (str1.charAt(i) != ' ')) {
					i++;
				}
				l = i;
				strVect.add(str1.substring(f, l));
			} else {
				i++;
			}
		}
		return strVect;
	}

	/**
	 * string2Vect is a method that converts a string array to a vector of
	 * strings
	 * 
	 * @param sA
	 *            -string array
	 * @return vector of strings
	 */
	public static Vector<String> string2Vect(String[] sA) {
		Vector<String> v = new Vector<String>();

		for (int i = 0; i < sA.length; i++) {
			v.add(sA[i]);
		}
		return v;
	}

	/**
	 * StringVectorFindAndReplace is a function that takes as argument a vector
	 * of strings stringVector and two strings find and replace. It returns a
	 * modified version of the argument string vector stringVector where the
	 * string replace substitutes the string find in the vector of strings.
	 * 
	 * @param stringVector
	 *            - Vector of strings
	 * @param find
	 *            - Sub-string to be replaced in the vector of strings
	 * @param replace
	 *            - Sub-string that is substituted in to the vector of string
	 *            where the find sub-string location
	 * @return Vector of strings containing a modified stringVector with the
	 *         sub-string replace everywhere there was sub-string find
	 * 
	 * @author E. D. Thompson
	 * @since JDK 1.6
	 */
	public static Vector<String> StringVectorFindAndReplace(
			Vector<String> stringVector, String find, String replace) {
		for (int i = 0; i < stringVector.size(); i++) {
			stringVector.set(i, stringVector.get(i).replace(find, replace));
		}
		return stringVector;
	}

	/**
	 * unpackArchive - Unpacks a JAR archive to the destination file path
	 * 
	 * @param jarArchive
	 *            JAR archive file path
	 * @param destinationPath
	 *            Destination file path
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void unpackArchive(File jarArchive, File destinationPath)
			throws FileNotFoundException, IOException {
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];
		JarInputStream jarInputStream = null;
		JarEntry jarEntry;

		jarInputStream = new JarInputStream(new FileInputStream(jarArchive));
		jarEntry = jarInputStream.getNextJarEntry();
		while (jarEntry != null) {
			String entryName = jarEntry.getName();
			FileOutputStream fileOutputStream;
			File newFile = new File(entryName);
			String directory = newFile.getParent();

			if (directory == null) {
				// If directory is null
				if (newFile.isDirectory()) {
					// If new file is a directory
					break;
				}
			} else {
				// If directory is not null
				File directoryPath = new File(destinationPath, directory);
				if (!directoryPath.exists()) {
					directoryPath.mkdir();
				}
			}
			fileOutputStream = new FileOutputStream(new File(destinationPath,
					entryName));
			int n = jarInputStream.read(buffer, 0, bufferSize);
			while (n > -1) {
				fileOutputStream.write(buffer, 0, n);
				n = jarInputStream.read(buffer, 0, bufferSize);
			}
			fileOutputStream.close();
			jarInputStream.closeEntry();
			jarEntry = jarInputStream.getNextJarEntry();
		}
		jarInputStream.close();

		return;
	}

	public static void upload(File fromFile, URL toUrl) throws IOException {
		logger.warn("********** WARNING: using GenericUtil.upload() "
				+ "will corrupt binary files! Use at your own risk.");
		upload(vect2String(getFileContents(fromFile)), toUrl);
	}

	public static void upload(Vector<String> v, URL url) throws IOException {
		upload(vect2String(v), url);
	}
	
	public static void upload(String[] sa, URL url) throws IOException {

		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setRequestMethod("PUT");
		con.setRequestProperty("Content-Type", "text/plain");
		con.setReadTimeout(30000);

		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
				con.getOutputStream()));

		for (String line : sa) {
			out.writeBytes(line);
			out.writeBytes("\n");
		}
		out.flush();
		out.close();

		if (con.getErrorStream() != null) {
			try {
				BufferedReader er = new BufferedReader(new InputStreamReader(
						con.getErrorStream()));
				String line;
				while ((line = er.readLine()) != null) {
					logger.info("server ErrorStream: " + line);
				}
				er.close();
			} catch (SocketTimeoutException e) {
				logger.warn("Socket Timeout " + con.getReadTimeout()
						+ " (ms) Exceeded");
				e.printStackTrace();
			}
		}

		try {
			BufferedReader is = new BufferedReader(new InputStreamReader(
					con.getInputStream()));

			// empty server's input stream
			String line;
			while ((line = is.readLine()) != null) {
				logger.info("server reply: " + line);
			}
			// close the inputstream
			is.close();
		} catch (SocketTimeoutException e) {
			logger.warn("Socket Timeout " + con.getReadTimeout()
					+ " (ms) Exceeded");
			e.printStackTrace();
		}

		out.close();
		con.disconnect();

		logger.info("disconnect");

		int rc = con.getResponseCode();
		String msg = con.getResponseMessage();
		logger.info("response: " + rc + " message: " + msg + " at: " + url);

	}

	/**
	 * vect2String is a function that converts a vector to a string array.
	 * originally written by S. A. Burton modified by E. D. Thompson
	 * 
	 * @author S. A. Burton, E. D. Thompson
	 */
	public static String[] vect2String(Vector<?> v) {
		String[] sA = new String[v.size()];
		for (int i = 0; i < v.size(); i++) {
			try {
				sA[i] = (String) v.get(i);
			} catch (ClassCastException e) {
				sA[i] = v.get(i).toString();
				e.printStackTrace();
			}
		}
		return sA;
	}

	public static void writeObjectToFile(File file, Object obj)
			throws IOException {
		FileOutputStream fOS = new FileOutputStream(file);
		ObjectOutputStream oOS = new ObjectOutputStream(fOS);
		oOS.writeObject(obj);
		oOS.flush();
		oOS.close();
	}

	/**
	 * Method that writes the content of URL inputURL to file path
	 * locatlInputFile
	 * 
	 * @param inputUrl
	 *            Input URL
	 * @param localInputFile
	 *            File path
	 * @throws IOException
	 */
	public static void writeUrlToFile(URL inputUrl, File localInputFile)
			throws Exception {
        InputStream is = null;
        try {
            is = inputUrl.openStream();
            redirectInputStream2File(is, localInputFile);
        } catch (Exception e) {
            throw(e);
        } finally {
            if (is != null) {
                is.close();
                is = null;
            }
        }
	}
	
	/**
	 * Downloads url directory to target directory (url directory will be 
	 * subdirectory of target directory); example:
	 * 
	 * dowloadDirectory(new URL("http://dir0/dir1/dir2"), new File("/home/junk"))
	 * 
	 * behaves like
	 * 
	 * cp -r dir2 /home/junk/dir2
	 * 
	 * such that the directory /home/junk/dir2 exists with all its sub dirs and 
	 * files.
	 * 
	 * 
	 * @param dirUrl
	 * @param destDir
	 * @throws IOException
	 */
	public static void downloadDirectory(URL dirUrl, File destDir) throws IOException {
		
		if (!destDir.exists())	throw new IOException(destDir + " does not exist.");
		if (!destDir.isDirectory())	throw new IOException(destDir + " is not a directory.");

		Vector<String> dirListing = getFileContents(dirUrl);
		//printVect(dirListing);
		
		String urlDirName = (new File(dirUrl.getFile()).getName());
		File destDir2 = new File(destDir, urlDirName);
		//System.out.println("destDir2 = " + destDir2);
		
		for (String line : dirListing) {
			String[] fields = line.split("\\s");
			//System.out.println("==============================");
			//printArray(fields);
			
			//System.out.println("fields[0] = " +  fields[0]);
			//System.out.println("dirUrl.getFile() = " +  dirUrl.getFile());
			
			String fileField = fields[0].replace("\\", "/");
			String[] fileFieldArray = fileField.split("/");
			
			String filename = fileFieldArray[fileFieldArray.length - 1];
			//System.out.println("filename = " +  filename);
			URL fileUrl = new URL(dirUrl.toString() + "/" + filename);
			//System.out.println("fileUrl = " + fileUrl);
			
			boolean madeDirs = destDir2.mkdirs();
			//System.out.println(madeDirs + ": made " + destDir2);
			destDir2.setWritable(true);
			destDir2.setReadable(true);
			destDir2.setExecutable(true);
			
			if (fields[1].equals("f")) {
				File fileTarget = new File(destDir2, new File(fileUrl.getFile()).getName());
				//System.out.println("downloading " + fileUrl + " to " + fileTarget);
				download(fileUrl, fileTarget);
			}
			if (fields[1].equals("d")) {
				downloadDirectory(fileUrl, destDir2);
			}
		}
		
		
	}
	
	private static void appendVectorToFile(File dataFile, Vector<?> fC)
			throws IOException {
		
		if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
		
		FileWriter out = new FileWriter(dataFile.getAbsolutePath(), true);
		BufferedWriter bwOut = new BufferedWriter(out);

		for (int i = 0; i < fC.size(); i++) {
			// System.out.println("SabUtil: appendVectorToFile: element "
			// +i+" is "+fC.elementAt(i));
			String pclLine = (String) fC.elementAt(i);
			bwOut.write(pclLine, 0, pclLine.length());
			bwOut.newLine();
			bwOut.flush();
		}
		bwOut.close();
	}

	public static int executeCommandWithWorker(final String command,
			final boolean printOutput, final boolean printError,
			final long timeOut, File dir, File logFile) {

		return executeCommandWithWorker(new String[] { command }, printOutput,
				printError, timeOut, dir, logFile, false);

	}
	
//	private static int executeCommandWithWorker(final String[] command,
//			final boolean printOutput, final boolean printError,
//			final long timeOut, File dir, File logFile,
//			boolean doSynchronizedLaunch) {
//		
//		return executeCommandWithWorker(command, printOutput
//				, printError, timeOut, dir, logFile, doSynchronizedLaunch, false);
//		
//	}
	
//	private static int executeCommandWithWorker(final String[] command,
//			final boolean printOutput, final boolean printError,
//			final long timeOut, File dir, File logFile,
//			boolean doSynchronizedLaunch, boolean isWrapper) {
		public static int executeCommandWithWorker(final String[] command,
				final boolean printOutput, final boolean printError,
				final long timeOut, File dir, File logFile,
				boolean doSynchronizedLaunch) {
			
		Runtime runtime;
		Worker worker;
		Process process;
		StreamGobbler outputGobbler, errorGobbler;
		StreamJobControlWriter jobControlWriter = null;
		
		try {
			if (doSynchronizedLaunch) {
				synchronized (GenericUtil.class) {
					runtime = Runtime.getRuntime();
					process = runtime.exec(command, null, dir);
					outputGobbler = new StreamGobbler(process.getInputStream(),
							"STD OUT SYNC", printOutput, logFile, dir);
					errorGobbler = new StreamGobbler(process.getErrorStream(),
							"STD ERR SYNC ", printError, logFile, dir);
					jobControlWriter = new StreamJobControlWriter(process.getOutputStream()
							, printOutput, logger, dir);
					jobControlWriter.start();
					outputGobbler.start();
					errorGobbler.start();
					worker = new Worker(process);
					worker.start();
				}
			} else {
				runtime = Runtime.getRuntime();
				process = runtime.exec(command, null, dir);
				outputGobbler = new StreamGobbler(process.getInputStream(),
						"STD OUT ", printOutput, logFile, dir);
				errorGobbler = new StreamGobbler(process.getErrorStream(),
						"STD ERR ", printError, logFile, dir);
				jobControlWriter = new StreamJobControlWriter(process.getOutputStream(), true, logger, dir);
				jobControlWriter.start();
				outputGobbler.start();
				errorGobbler.start();
				worker = new Worker(process);
				worker.start();
			}

			try {
				GenericUtil.appendFileContents("executeCommandWithWorker(): calling worker.join(timeout)...", dir);
				worker.join(timeOut);
				GenericUtil.appendFileContents("executeCommandWithWorker(): done calling worker.join(timeout).", dir);

				Integer exitValue = worker.getExitValue();
				GenericUtil.appendFileContents("executeCommandWithWorker(): exitValue = " + exitValue, dir);

				if (exitValue == null || exitValue > 0) {
					GenericUtil.appendFileContents("executeCommandWithWorker(): worker exit value was null or > 0, sending kill...", dir);
					jobControlWriter.sendKillToWrapperScript();
//					GenericUtil.appendFileContents("executeCommandWithWorker(): waiting for the kill; calling worker.join()...", dir);
//					worker.join();
				} else {
					GenericUtil.appendFileContents("executeCommandWithWorker(): calling jobControlWriter.closeDown()...", dir);				
					jobControlWriter.closeDown();
				}

				
				GenericUtil.appendFileContents("executeCommandWithWorker(): calling jobControlWriter.join(1000)...", dir);
				jobControlWriter.join(1000);
				if (jobControlWriter.isAlive()) {
					GenericUtil.appendFileContents("executeCommandWithWorker():calling jobControlWriter on output gobbler...", dir);
				}

				GenericUtil.appendFileContents("executeCommandWithWorker(): DONE calling jobControlWriter.join(1000)...closing gobblers...", dir);
				outputGobbler.closeDown();
				errorGobbler.closeDown();
				GenericUtil.appendFileContents("executeCommandWithWorker(): DONE closing gobblers...calling join(1000) on gobblers...", dir);
				outputGobbler.join(1000);
				errorGobbler.join(1000);

				if (outputGobbler.isAlive()) {
					GenericUtil.appendFileContents("executeCommandWithWorker():calling interrupt on output gobbler...", dir);
					outputGobbler.interrupt();
				}
				if (outputGobbler.isAlive()){
					GenericUtil.appendFileContents("executeCommandWithWorker():calling interrupt on errorGobbler gobbler...", dir);
					errorGobbler.interrupt();
				}
				if (worker.isAlive()) {
					GenericUtil.appendFileContents("executeCommandWithWorker():calling interrupt on worker", dir);
					worker.interrupt();
				}

				GenericUtil.appendFileContents("executeCommandWithWorker(): calling process.destroy()...", dir);
				process.destroy();

				GenericUtil.appendFileContents("executeCommandWithWorker(): exitValue = " + exitValue, dir);
				if (exitValue != null) {
					logger.info("exitValue is not null; exitValue = " + exitValue);
					return exitValue;
				}
				
				String errorMessage = "***error: the command(s) timed out: \n" 
						+ arrayToOneLineSpaceDelimitedString(command) + "\n";
				setFileContents(new File(dir, "timeout.txt"), errorMessage);
				
				throw new RuntimeException(errorMessage);
			} catch (InterruptedException ex) {
				GenericUtil.appendFileContents("executeCommandWithWorker(): ***exception in executeCommandWithWorker: " + ex, dir);
				logger.warn("***exception in executeCommandWithWorker", ex);
				throw ex;
			}
		} catch (InterruptedException ex) {
			String errorMessage = "the command: " + arrayToOneLineSpaceDelimitedString(command)
					+ ", did not complete due to an "
					+ "unexpected interruption.";
            logger.warn(errorMessage, ex);
			throw new RuntimeException(errorMessage, ex);
		} catch (FileNotFoundException ex) {
			String errorMessage = "the log file was not found.";
            logger.warn(errorMessage, ex);
			throw new RuntimeException(errorMessage, ex);
		} catch (IOException ex) {
			String errorMessage = "the command: " + arrayToOneLineSpaceDelimitedString(command)
					+ ", did not complete due to an " + "io error.";
            logger.warn(errorMessage, ex);
			throw new RuntimeException(errorMessage, ex);
		}
	}

    /**
     * Get the aggregate classpath, using ClassLoader hierarchy
     *
     * @param loader The ClassLoader to use as a starting point. if {@code null}, the {@code Thread}'s context
     *               classloader will be used.
     *
     * @return A {@link Map} containing local and remote jars. If local jars (file based) are identified, the
     * local jars values is a {@code File.separator} delimited string of classpath artifacts. If remote jars
     * are identified (http based), the remote jars values is a space delimited string of classpath artifacts.
     *
     * @throws Exception
     */
	public static Map<String, String> getAggregateClassPath(
			final ClassLoader loader) throws Exception {
		Map<String, String> jarMap = new HashMap<String, String>();
		List<ClassLoader> loaderList = new ArrayList<ClassLoader>();
		ClassLoader currentLoader = loader == null ? Thread.currentThread()
				.getContextClassLoader() : loader;
		StringBuilder fileUrlBuilder = new StringBuilder();
		StringBuilder httpUrlBuilder = new StringBuilder();
		while (currentLoader != null
				&& !ClassLoader.getSystemClassLoader().equals(currentLoader)) {
			loaderList.add(currentLoader);
			currentLoader = currentLoader.getParent();
		}
		Collections.reverse(loaderList);
		for (ClassLoader cl : loaderList) {
			URL[] urls = null;
			if (cl instanceof ServiceClassLoader) {
				urls = ((ServiceClassLoader) cl).getSearchPath();
			} else if (cl instanceof URIClassLoader) {
				ClassLoader cCL = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(cl);
				try {
					urls = ((URIClassLoader) cl).getURLs();
				} finally {
					Thread.currentThread().setContextClassLoader(cCL);
				}
			} else if (cl instanceof URLClassLoader) {
				ClassLoader cCL = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(cl);
                /* Subclasses of URLClassLoader may override getURLs(), in order to
                 * return the URLs of the provided export codebase. This is a workaround to get
                 * the search path (not the codebase) of the URLClassLoader. We get the ucp property from the
                 * URLCLassLoader (of type sun.misc.URLClassPath), and invoke the sun.misc.URLClassPath.getURLs()
                 * method */
				try {
                    try {
                        for(java.lang.reflect.Field field : URLClassLoader.class.getDeclaredFields()) {
                            if(field.getName().equals("ucp")) {
                                field.setAccessible(true);
                                Object ucp = field.get(cl);
                                Method getURLs = ucp.getClass().getMethod("getURLs");
                                urls = (URL[])getURLs.invoke(ucp);
                                break;
                            }
                        }
                    } catch(Exception e) {
                        logger.warn("Could not get or access field \"ucp\", just call getURLs()", e);
                        urls = ((URLClassLoader) cl).getURLs();
                    }
                } finally {
					Thread.currentThread().setContextClassLoader(cCL);
				}
			}

			if (urls != null) {
				for (URL url : urls) {
					if (logger.isDebugEnabled())
						logger.debug("Processing url: " + url.toExternalForm());
					if (url.getProtocol().equals("artifact")) {
						String artifact = new ArtifactURLConfiguration(
								url.toExternalForm()).getArtifact();

						for (String item : ResolverHelper.getResolver()
								.getClassPathFor(artifact)) {
							if (fileUrlBuilder.length() > 0) {
								fileUrlBuilder.append(File.pathSeparator);
							}
							fileUrlBuilder.append(item);
						}

					} else if (url.getProtocol().startsWith("http")) {
						if (httpUrlBuilder.length() > 0) {
							httpUrlBuilder.append(" ");
						}
						httpUrlBuilder.append(url.toExternalForm());
					} else {
						File jar = new File(url.toURI());
						if (fileUrlBuilder.length() > 0) {
							fileUrlBuilder.append(File.pathSeparator);
						}
						fileUrlBuilder.append(jar.getAbsolutePath());
					}
				}
			}
		}

		/*
		 * Build the table only if we have file URLs. This happens if the
		 * ClassLoader is a Rio classloader, and triggers the approach for the
		 * DelegateLauncher to create a URLClassLoader. If there are no file
		 * URLs. the CLASSPATH environment variable will be used.
		 */
		if (fileUrlBuilder.length() > 0) {
			fileUrlBuilder.insert(0, File.pathSeparator);
			fileUrlBuilder.insert(0, System.getProperty("java.class.path"));
			jarMap.put(LOCAL_JARS, fileUrlBuilder.toString());

			if (httpUrlBuilder.length() > 0) {
				jarMap.put(REMOTE_JARS, httpUrlBuilder.toString());
			}
		}

		return jarMap;
	}

    public static Thread executeCommandWithWorkerNoBlocking(
            String[] command, final boolean printOutput,
			final boolean printError, final long timeOut, File dir,
			File logFile, boolean doSynchronizedLaunch) {

		Runtime runtime;
		WorkerNoBlock worker;
		Process process;
		StreamGobbler outputGobbler, errorGobbler;
		
		// windows platform independent
		//
		if (GenericUtil.isWindows()) {
			String[] ncmdarray = new String[command.length + 2];
			ncmdarray[0] = "cmd";
			ncmdarray[1] = "/C";
			int ctr = 2;
			for (int i = 0; i < command.length; i++) {
				ncmdarray[ctr] = command[i];
				ctr++;
			}
			command = ncmdarray;
		}
		
		// mkdirs
		//
		if (!dir.exists()) dir.mkdirs();
		
		try {
			if (doSynchronizedLaunch) {
				synchronized (GenericUtil.class) {
					runtime = Runtime.getRuntime();
					process = runtime.exec(command, null, dir);
					outputGobbler = new StreamGobbler(process.getInputStream(),
							"STD OUT", printOutput, logFile, dir);
					errorGobbler = new StreamGobbler(process.getErrorStream(),
							"STD ERR", printError, logFile, dir);
					outputGobbler.start();
					errorGobbler.start();
					worker = new WorkerNoBlock(process);
					worker.start();
				}
			} else {
				runtime = Runtime.getRuntime();
				process = runtime.exec(command, null, dir);
				outputGobbler = new StreamGobbler(process.getInputStream(),
						"STD OUT", printOutput, logFile, dir);
				errorGobbler = new StreamGobbler(process.getErrorStream(),
						"STD ERR", printError, logFile, dir);
				outputGobbler.start();
				errorGobbler.start();
				worker = new WorkerNoBlock(process);
				worker.start();
			}

		} catch (FileNotFoundException ex) {
			String errorMessage = "the log file was not found.";
			throw new RuntimeException(errorMessage, ex);

		} catch (IOException ex) {
			String errorMessage = "the command: " + command
					+ ", did not complete due to an " + "io error.";
			throw new RuntimeException(errorMessage, ex);
		}
		return (Thread) worker;
	}

	private static boolean hasArg(String test, String[] args) {
		for (int ctr = 0; ctr < args.length; ctr++) {
			if (args[ctr].equals(test))
				return true;
		}
		return false;
	}

	private static void printHelp() {
		StringBuilder sb = new StringBuilder();

		sb.append("\nUsage: java sorcer.util.GenericUtil [-options]");
		sb.append("\n\nand options include:\n\n");
		sb.append("-h\t\t\tprint this message\n");
		sb.append("-ev\t\t\tprint environment variables\n");
		sb.append("-sp\t\t\tprint system properties\n");
		sb.append("-dl <url> <dest dir>\tdownload directory from url (recursive)\n");
		System.out.println(sb.toString());

	}

	/**
	 * writeVectorToFile was originally implemented by S. A. Burton, but has
	 * been rewritten by E. D. Thompson in a a cleaner more efficient syntax. 
	 * (<==says Earnest! SAB)
	 * The function receives as arguments an output file path and a vector. It
	 * then creates an ASCII file that has the contents of vector.
	 * 
	 * @author E. D. Thompson, S. A. Burton
	 * @since JDK 1.6
	 */
	private static void writeVectorToFile(File file, Vector<?> fileContents)
			throws IOException {

		// mkdirs if necessary
		if (file.getParentFile() != null)
			file.getParentFile().mkdirs();

		// File output stream
		PrintWriter fout = new PrintWriter(file);
		// Looping over the file contents array
		for (int i = 0; i < fileContents.size(); i++) {
			// Writing line string to file
			//fout.println(fileContents.get(i));
			//fout.println(object); // this will put \n\r in windows screwing up cygwin
			fout.print(fileContents.get(i).toString() + "\n");
		}
		// Close file stream
		fout.close();
	}
	
	public static void dos2unix(File file) throws FileNotFoundException {
		Vector<String> fileContents = getFileContents(file);
		file.delete();
		
		// File output stream
		PrintWriter fout = new PrintWriter(file);
		// Looping over the file contents array
		for (int i = 0; i < fileContents.size(); i++) {
			// Writing line string to file
			//fout.println(fileContents.get(i));
			//fout.println(object); // this will put \n\r in windows screwing up cygwin
			String line = fileContents.get(i).replace("\r", "");
			fout.print(line + "\n");
		}
		// Close file stream
		fout.close();		
	}

	/**
	 * vect2String is a function that converts a vector to a string array.
	 * originally written by S. A. Burton modified by E. D. Thompson
	 * 
	 * @author S. A. Burton, E. D. Thompson
	 */
	private static int slurmSubmitCtr = 0;

	public static int runShellScriptViaSlurm(String appName, File scriptFile,
			Vector<String> scriptRecords, File logFile) throws Exception {
		return runShellScriptViaSlurm(appName, scriptFile, scriptRecords,
				logFile, 0);
	}

	public static int runShellScriptViaSlurm(String appName, File scriptFile,
			Vector<String> scriptRecords, File logFile, long scriptTimeout)
			throws Exception {

		logger.info("GenericUtil.runShellScriptViaSlurm():"
				+ "\n\tappName = + appName" + "\n\tscriptFile = "
				+ scriptFile.getAbsolutePath());

		GenericUtil.setFileContents(scriptFile, scriptRecords);
		GenericUtil.makeExecutable(scriptFile);

		File scratchDir = scriptFile.getParentFile();

		String uid = null;
		String jobName = null;
		synchronized (GenericUtil.class) {
			// uid = UUID.randomUUID().toString();
			uid = Integer.toString(slurmSubmitCtr++);
			jobName = "s" + appName + "_" + uid;
			jobName = jobName.substring(0, Math.min(19, jobName.length() - 1));
		}

		// middle_.sh calls the script passed in by user in background, then
		// slurm script calls middle_.sh; slurm script kills middle_.sh if
		// user timeout option expires
		//
		// add semaphore: drop file to signal slurm job done
		//
		File middleScriptFile = new File(scratchDir, "middle_" + uid + ".sh");
		File middleScriptLogFile = new File(scratchDir, "middleLog_" + uid
				+ ".txt");
		File middleScriptErrFile = new File(scratchDir, "middleErr_" + uid
				+ ".txt");
		File middleScriptDoneFile = new File(scratchDir, "middleDone_" + uid
				+ ".txt");
		File middleScriptRunningFile = new File(scratchDir, "middleRunning_"
				+ uid + ".txt");
		File middleScriptKilledFile = new File(scratchDir, "middleKilled_"
				+ uid + ".txt");

		// slurm script calls middle.sh via srun command
		//
		File slurmScriptFile = new File(scratchDir, "slurmScript_" + uid
				+ ".sh");

		File slurmScriptLogFile = logFile;
		if (logFile == null) {
			slurmScriptLogFile = new File(scratchDir, "slurmScriptLog_" + uid
					+ ".txt");
		}
		File srunCmdJobIdFile = new File(scratchDir, "srunCmd_" + uid + ".log");
		File slurmRunningFile = new File(scratchDir, "slurmScriptRunning_"
				+ uid + ".txt");
		File slurmDoneFile = new File(scratchDir, "slurmScriptDone_" + uid
				+ ".txt");
		File slurmRunErrorFile = new File(scratchDir, "slrumRunErrorDetected_"
				+ uid + ".txt");

		// middleScript
		//
		Vector<String> middleScriptRecords = new Vector<String>();
		middleScriptRecords.add("#!/bin/bash");
		middleScriptRecords.add("THIS_PID=$$");
		middleScriptRecords.add("echo $THIS_PID > "
				+ middleScriptRunningFile.getAbsolutePath());
		middleScriptRecords.add("if [ ! -s "
				+ srunCmdJobIdFile.getAbsolutePath() + " ]; then");
		middleScriptRecords.add("	echo \"***error from "
				+ middleScriptFile.getName()
				+ ": slurm job id file does not exist; job id file = "
				+ srunCmdJobIdFile.getName() + "\" > "
				+ middleScriptKilledFile.getAbsolutePath());
		middleScriptRecords.add("	rm -f "
				+ middleScriptRunningFile.getAbsolutePath());
		middleScriptRecords
				.add("	echo \""
						+ middleScriptFile.getName()
						+ " is done, with ERRORs; middleScript did not find slurm job id file.\" > "
						+ middleScriptDoneFile);
		middleScriptRecords.add("	exit 1");
		middleScriptRecords.add("fi");
		middleScriptRecords.add("SLURM_JOB_ID=`cat "
				+ srunCmdJobIdFile.getAbsolutePath() + "`");
		middleScriptRecords.add("echo \"SLURM_JOB_ID=$SLURM_JOB_ID\"");
		middleScriptRecords.add("ELAPSED_TIME=0");
		middleScriptRecords.add(scriptFile.getAbsolutePath() + " &");
		middleScriptRecords.add("SCRIPT_PID=$!");
		middleScriptRecords.add("SCRIPT_RUNNING=1");
		middleScriptRecords.add("SCRIPT_TIMEOUT_SEC=" + scriptTimeout / 1000);
		middleScriptRecords.add("while [ $SCRIPT_RUNNING -eq 1 ]; do");
		middleScriptRecords
				.add("	TEST_RESULT=`ps ax | grep -v grep | grep \" $SCRIPT_PID \"`");
		middleScriptRecords
				.add("	TEST=\"ps ax | grep -v grep | grep \\\" $SCRIPT_PID \\\"\"");
		middleScriptRecords.add("	echo \"TEST=$TEST\"");
		middleScriptRecords.add("	echo \"TEST_RESULT=$TEST_RESULT\"");
		middleScriptRecords
				.add("	if ps ax | grep -v grep | grep \" $SCRIPT_PID \" > /dev/null; then");
		middleScriptRecords.add("		SCRIPT_RUNNING=1");
		middleScriptRecords.add("	else");
		middleScriptRecords.add("		SCRIPT_RUNNING=0");
		middleScriptRecords.add("	fi");
		middleScriptRecords
				.add("	ELAPSED_TIME=`echo $ELAPSED_TIME | awk '{t=$0+1.0; print t}'`");
		middleScriptRecords
				.add("	EXPIRED=`echo $ELAPSED_TIME | awk -v to=$SCRIPT_TIMEOUT_SEC 'BEGIN{if (to<=0) {print 0; exit}} {if ($0>to){print 1;exit}; print 0}'`");
		middleScriptRecords.add("	if [ $EXPIRED -eq 1 ]; then");
		middleScriptRecords.add("		kill -9 $SCRIPT_PID");
		middleScriptRecords.add("		echo \"killed middleScript due to timeout, "
				+ middleScriptFile.getName() + ", with PID=$SCRIPT_PID\" > "
				+ middleScriptKilledFile.getAbsolutePath());
		middleScriptRecords.add(" 		scancel $SLURM_JOB_ID");
		middleScriptRecords
				.add("		echo \"killed slurm job id = $SLURM_JOB_ID\" >> "
						+ middleScriptKilledFile.getAbsolutePath());
		middleScriptRecords.add("		rm -f "
				+ middleScriptRunningFile.getAbsolutePath());
		middleScriptRecords.add("		echo \"" + middleScriptFile.getName()
				+ " is done, with ERRORs; middleScript timed out.\"");
		middleScriptRecords.add("		EXIT_CODE=1");
		middleScriptRecords.add("		echo \"$EXIT_CODE\" > "
				+ middleScriptDoneFile);
		middleScriptRecords.add("		exit $EXIT_CODE");
		middleScriptRecords.add("	fi");
		middleScriptRecords.add("	sleep 0.1");
		middleScriptRecords.add("done");
		middleScriptRecords.add("wait $SCRIPT_PID");
		middleScriptRecords.add("EXIT_CODE=$?");
		middleScriptRecords.add("echo \"" + middleScriptFile.getName()
				+ " is done, exit code = $EXIT_CODE\"");
		middleScriptRecords
				.add("echo \"$EXIT_CODE\" > " + middleScriptDoneFile);
		middleScriptRecords.add("rm -f "
				+ middleScriptRunningFile.getAbsolutePath());
		middleScriptRecords.add("exit $EXIT_CODE");

		middleScriptDoneFile.delete();
		setFileContents(middleScriptFile, middleScriptRecords);
		GenericUtil.makeExecutable(middleScriptFile);

		Vector<String> slurmScriptRecords = new Vector<String>();
		slurmScriptRecords.add("#!/bin/bash");
		slurmScriptRecords.add("EXIT_VALUE=0");
		slurmScriptRecords.add("echo \"slurm script running\" >  "
				+ slurmRunningFile.getAbsolutePath());
		// String cmd = "(srun --overcommit --ntasks-per-core=1 -J " + jobName +
		// " -b "+ localJobFile
		// String cmd = "(srun --cpus-per-task=1 -J " + jobName + " -b "+
		// localJobFile

		// earnest says --nodes == 1 is good
		String cmd = "(srun"
				+ " --nodes=1"
				// + " --no-kill"
				+ " --wait 0"
				+ " --verbose"
				// + " --label"
				// + " --ntasks-per-node=1"
				+ " -J "
				+ jobName
				// + " --ntasks 1"
				+ " --output "
				+ middleScriptLogFile.getAbsolutePath()
				+ " --error "
				+ middleScriptErrFile.getAbsolutePath()
				+ " -b"
				+ " "
				+ middleScriptFile.getAbsolutePath()
				+ " 3>&1 1>&2- 2>&3-) | awk 'BEGIN{FS=\":| \"} {if (NR==2) {print $4; exit}}' > "
				+ srunCmdJobIdFile.getAbsolutePath();

		slurmScriptRecords.addElement("cd " + scratchDir);
		slurmScriptRecords.addElement(cmd);
		slurmScriptRecords.addElement("SLURM_JOB_ID=`cat "
				+ srunCmdJobIdFile.getAbsolutePath() + "`");
		slurmScriptRecords.addElement("echo \"SLURM_JOB_ID=$SLURM_JOB_ID\"");
		slurmScriptRecords
				.addElement("while squeue | grep \" $SLURM_JOB_ID \" > /dev/null; do sleep 0.1; echo \"waiting for slurm job\";done");
		slurmScriptRecords.addElement("sleep 0.1");
		slurmScriptRecords.addElement("if [ ! -s "
				+ middleScriptDoneFile.getAbsolutePath() + " ]; then");
		slurmScriptRecords
				.addElement("     echo \"middleScriptDoneFile missing, reporting error!\"");
		slurmScriptRecords
				.addElement("     echo \"middleScriptDoneFile missing, reporting error!\" > "
						+ slurmRunErrorFile.getAbsolutePath());
		slurmScriptRecords.addElement("     EXIT_VALUE=1");
		slurmScriptRecords.addElement("fi");
		slurmScriptRecords.addElement("EXIT_VALUE=`cat "
				+ middleScriptDoneFile.getAbsolutePath() + "`");
		slurmScriptRecords.addElement("rm -f "
				+ slurmRunningFile.getAbsolutePath());
		slurmScriptRecords.addElement("echo \"slurm done\" > "
				+ slurmDoneFile.getAbsolutePath());
		slurmScriptRecords.addElement("exit $EXIT_VALUE");

		logger.info("GenericUtil.runShellScriptViaSlurm(): cmd = " + cmd);
		// GenericUtil.setFileContents(slurmScriptFile, slurmScriptRecords);
		// GenericUtil.makeExecutable(slurmScriptFile);

		File triesFile = new File(scratchDir, "triesError_" + uid + ".txt");

		int exitValue = 1;
		try {
			int tries = 0;
			while (exitValue > 0 && tries < 1) {
				// synchronized (GenericUtil.class) {

				// exitValue = runShellScript(slurmScriptFile,
				// slurmScriptRecords, slurmScriptLogFile, 0);

				exitValue = runShellScript(slurmScriptFile, slurmScriptRecords,
						slurmScriptLogFile, 0, false, true, true);

				logger.info("GenericUtil.runShellScriptViaSlurm(): "
						+ "script complete: exitValue = " + exitValue
						+ "; tries = " + tries);
				if (exitValue == 0)
					break;

				logger.info("GenericUtil.runShellScriptViaSlurm(): problem trying to "
						+ "submit to slurm, going to try again: exitValue = "
						+ exitValue + "; tries = " + tries);

				tries++;
				GenericUtil.setFileContents(triesFile,
						new String[] { new Integer(tries).toString() });

				Thread.sleep(100);
				// }
			}
			if (exitValue != 0) {
				String msg = "***error: had problem submitting "
						+ "slurm script:" + "\n\texit code = " + exitValue
						+ "\n\tscratchDir = " + scratchDir + "\n\ttries = "
						+ tries + "\n\tslurmScriptFile = "
						+ slurmScriptFile.getAbsolutePath();
				logger.info(msg);
				logger.warn(msg);
				throw new Exception(msg);
			}

			Vector<String> srunCmdLogContents = GenericUtil
					.getFileContents(srunCmdJobIdFile);
			String jobID = srunCmdLogContents.get(0);
			logger.info("GenericUtil.runShellScriptViaSlurm(): jobID = "
					+ jobID);

			// wait until slurm job is done (if -b option is used, this is
			// necessary)
			//
			// boolean isDone = middleScriptDoneFile.canRead();
			boolean isDone = slurmDoneFile.canRead();
			while (!isDone) {
				Thread.sleep(1000);
				isDone = slurmDoneFile.canRead();
				// isDone = middleScriptDoneFile.canRead();
			}
			// if (!isDone) {
			// String msg = "***error: had problem with slurm script; the file "
			// + middleScriptDoneFile.getAbsolutePath()
			// + " was not generated fast enough: "
			// + "\n\tscratchDir = " + scratchDir
			// + "\n\tslurmScriptFile = " + slurmScriptFile.getAbsolutePath()
			// + "\n\ttries = " + tries
			// + "\n\tisDone = " + isDone
			// + "\n\tcanRead() = " + middleScriptDoneFile.canRead()
			// + "\n\texists() = " + middleScriptDoneFile.exists();
			// logger.info(msg);
			// logger.warn(msg);
			// throw new Exception(msg);
			// }
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
		logger.info("GenericUtil.runShellScriptViaSlurm(): exitValue = "
				+ exitValue);

		return exitValue;
	}

	// scriptTimeout = 0 is infinity
	public static int runShellScriptViaSlurm3(String appName, File scriptFile,
			Vector<String> scriptRecords, File logFile, long scriptTimeout)
			throws Exception {

		logger.info("GenericUtil.runShellScriptViaSlurm():"
				+ "\n\tappName = + appName" + "\n\tscriptFile = "
				+ scriptFile.getAbsolutePath());

		GenericUtil.setFileContents(scriptFile, scriptRecords);
		GenericUtil.makeExecutable(scriptFile);

		File scratchDir = scriptFile.getParentFile();

		String uid = null;
		String jobName = null;
		synchronized (GenericUtil.class) {
			// uid = UUID.randomUUID().toString();
			uid = Integer.toString(slurmSubmitCtr++);
			jobName = "s" + appName + "_" + uid;
			jobName = jobName.substring(0, Math.min(19, jobName.length() - 1));
		}

		// middle_.sh calls the script passed in by user in background, then
		// slurm script calls middle_.sh; slurm script kills middle_.sh if
		// user timeout option expires
		//
		// add semaphore: drop file to signal slurm job done
		//
		File middleScriptFile = new File(scratchDir, "middle_" + uid + ".sh");
		File middleScriptLogFile = new File(scratchDir, "middleLog_" + uid
				+ ".txt");
		File middleScriptErrFile = new File(scratchDir, "middleErr_" + uid
				+ ".txt");
		File middleScriptDoneFile = new File(scratchDir, "middleDone_" + uid
				+ ".txt");
		File middleScriptRunningFile = new File(scratchDir, "middleRunning_"
				+ uid + ".txt");
		File middleScriptFailFile = new File(scratchDir, "middleFail_" + uid
				+ ".txt");

		// slurm script calls middle.sh via srun command
		//
		File slurmScriptFile = new File(scratchDir, "slurmScript_" + uid
				+ ".sh");

		File slurmScriptLogFile = logFile;
		if (logFile == null) {
			slurmScriptLogFile = new File(scratchDir, "slurmScriptLog_" + uid
					+ ".txt");
		}
		File srunCmdJobIdFile = new File(scratchDir, "srunCmd_" + uid + ".log");
		File slurmRunningFile = new File(scratchDir, "slurmScriptRunning_"
				+ uid + ".txt");
		File slurmDoneFile = new File(scratchDir, "slurmScriptDone_" + uid
				+ ".txt");
		File slurmFailFile = new File(scratchDir, "slrumRunFail_" + uid
				+ ".txt");

		// middleScript
		//
		Vector<String> middleScriptRecords = new Vector<String>();
		middleScriptRecords.add("#!/bin/bash");
		middleScriptRecords.add("THIS_PID=$$");
		middleScriptRecords
				.add("echo \"middleScript starting, THIS_PID=$THIS_PID\"");
		middleScriptRecords.add("echo $THIS_PID > "
				+ middleScriptRunningFile.getAbsolutePath());
		middleScriptRecords.add("if [ ! -s "
				+ srunCmdJobIdFile.getAbsolutePath() + " ]; then");
		middleScriptRecords
				.add("	echo \"***error: slurm job id file does not exist; job id file = "
						+ srunCmdJobIdFile.getName()
						+ "\" > "
						+ middleScriptFailFile.getAbsolutePath());
		middleScriptRecords
				.add("	echo \"***error: slurm job id file does not exist; job id file = "
						+ srunCmdJobIdFile.getName());
		middleScriptRecords.add("	rm -f "
				+ middleScriptRunningFile.getAbsolutePath());
		middleScriptRecords
				.add("	echo \"middleScript is done with ERRORs; middleScript did not find slurm job id file; setting EXIT_VALUE=1 and exiting...\"");
		middleScriptRecords.add("   EXIT_VALUE=1");
		middleScriptRecords.add("	echo \"$EXIT_VALUE\" > "
				+ middleScriptDoneFile);
		middleScriptRecords.add("	exit $EXIT_VALUE");
		middleScriptRecords.add("fi");
		middleScriptRecords.add("SLURM_JOB_ID=`cat "
				+ srunCmdJobIdFile.getAbsolutePath() + "`");
		middleScriptRecords.add("EXIT_VALUE=$?");
		middleScriptRecords.add("if [ $EXIT_VALUE -ne 0 ]; then");
		middleScriptRecords
				.add("	echo \"***error: had problem cat-ing job id file = "
						+ srunCmdJobIdFile.getName() + "\" > "
						+ middleScriptFailFile.getAbsolutePath());
		middleScriptRecords
				.add("	echo \"***error: had problem cat-ing job id file = "
						+ srunCmdJobIdFile.getName());
		middleScriptRecords.add("	rm -f "
				+ middleScriptRunningFile.getAbsolutePath());
		middleScriptRecords
				.add("	echo \"middleScript is done with ERRORs; middleScript couldn't cat slurm job id file; cat EXIT_VALUE=$EXIT_VALUE; exiting...\"");
		middleScriptRecords.add("	echo \"$EXIT_VALUE\" > "
				+ middleScriptDoneFile);
		middleScriptRecords.add("	exit $EXIT_VALUE");
		middleScriptRecords.add("fi");
		middleScriptRecords.add("echo \"SLURM_JOB_ID=$SLURM_JOB_ID\"");
		middleScriptRecords.add("ELAPSED_TIME=0");
		middleScriptRecords.add("echo \"invoking in background: "
				+ scriptFile.getAbsolutePath() + "\"");
		middleScriptRecords.add(scriptFile.getAbsolutePath() + " &");
		middleScriptRecords.add("SCRIPT_PID=$!");
		middleScriptRecords.add("echo \"SCRIPT_PID=$SCRIPT_PID\"");
		middleScriptRecords.add("SCRIPT_RUNNING=1");
		middleScriptRecords.add("SCRIPT_TIMEOUT_SEC=" + scriptTimeout / 1000);
		middleScriptRecords.add("while [ $SCRIPT_RUNNING -eq 1 ]; do");
		middleScriptRecords
				.add("	TEST_RESULT=`ps ax | grep -v grep | grep \" $SCRIPT_PID \"`");
		middleScriptRecords
				.add("	TEST=\"ps ax | grep -v grep | grep \\\" $SCRIPT_PID \\\"\"");
		middleScriptRecords.add("	echo \"TEST=$TEST\"");
		middleScriptRecords.add("	echo \"TEST_RESULT=$TEST_RESULT\"");
		middleScriptRecords
				.add("	if ps ax | grep -v grep | grep \" $SCRIPT_PID \" > /dev/null; then");
		middleScriptRecords.add("		SCRIPT_RUNNING=1");
		middleScriptRecords.add("	else");
		middleScriptRecords.add("		SCRIPT_RUNNING=0");
		middleScriptRecords.add("	fi");
		middleScriptRecords
				.add("	ELAPSED_TIME=`echo $ELAPSED_TIME | awk '{t=$0+1.0; print t}'`");
		middleScriptRecords
				.add("   echo \"ELAPSED_TIME=$ELAPSED_TIME; SCRIPT_TIMEOUT_SEC=$SCRIPT_TIMEOUT_SEC\"");
		middleScriptRecords
				.add("	EXPIRED=`echo $ELAPSED_TIME | awk -v to=$SCRIPT_TIMEOUT_SEC 'BEGIN{if (to<=0) {print 0; exit}} {if ($0>to){print 1;exit}; print 0}'`");
		middleScriptRecords.add("   echo \"EXPIRED=$EXPIRED\"");
		middleScriptRecords.add("	if [ $EXPIRED -eq 1 ]; then");
		middleScriptRecords
				.add("   	echo \"user timeout, SCRIPT_TIMEOUT_SEC=$SCRIPT_TIMEOUT_SEC, reached, killing user script process...\"");
		middleScriptRecords.add("		kill -9 $SCRIPT_PID");
		middleScriptRecords
				.add("		echo \"killed user script process due to timeout, "
						+ scriptFile.getName() + ", with PID=$SCRIPT_PID\"");
		// middleScriptRecords.add(" 	scancel $SLURM_JOB_ID");
		// middleScriptRecords.add("		echo \"killed slurm job id = $SLURM_JOB_ID\" >> "
		// + middleScriptKilledFile.getAbsolutePath());
		middleScriptRecords
				.add("		echo \"***error: middleScript is done with ERRORs; hit user timeout;  setting EXIT_VALUE=1; exiting...\" > "
						+ middleScriptFailFile.getAbsolutePath());
		middleScriptRecords
				.add("		echo \"***error: middleScript is done with ERRORs; hit user timeout;  setting EXIT_VALUE=1; exiting...\"");
		middleScriptRecords.add("		rm -f "
				+ middleScriptRunningFile.getAbsolutePath());
		middleScriptRecords.add("		EXIT_VALUE=1");
		middleScriptRecords.add("		echo \"$EXIT_VALUE\" > "
				+ middleScriptDoneFile.getAbsolutePath());
		middleScriptRecords.add("		exit $EXIT_VALUE");
		middleScriptRecords.add("	fi");
		middleScriptRecords.add("	sleep 0.1");
		middleScriptRecords.add("done");
		middleScriptRecords
				.add("echo \"wating for user script to finish, SCRIPT_PID=$SCRIPT_PID\"");
		middleScriptRecords.add("wait $SCRIPT_PID");
		middleScriptRecords.add("EXIT_VALUE=$?");
		middleScriptRecords.add("echo \"user script, " + scriptFile.getName()
				+ " is done, exit code = $EXIT_VALUE\"");
		middleScriptRecords.add("echo \"$EXIT_VALUE\" > "
				+ middleScriptDoneFile);
		middleScriptRecords.add("if [ $EXIT_VALUE -ne 0 ]; then");
		middleScriptRecords
				.add("	echo \"user script exit value was non-zero, EXIT_VALUE=$EXIT_VALUE, dropping middleFailFile and exiting with $EXIT_VALUE.\"");
		middleScriptRecords
				.add("	echo \"user script exit value was non-zero, EXIT_VALUE=$EXIT_VALUE, dropping middleFailFile and exiting with $EXIT_VALUE.\" > "
						+ middleScriptFailFile.getAbsolutePath());
		middleScriptRecords.add("fi");
		middleScriptRecords.add("rm -f "
				+ middleScriptRunningFile.getAbsolutePath());
		middleScriptRecords.add("exit $EXIT_VALUE");

		// initialize files
		//
		middleScriptDoneFile.delete();
		setFileContents(middleScriptFile, middleScriptRecords);
		GenericUtil.makeExecutable(middleScriptFile);

		// slurm submission script
		//
		Vector<String> slurmScriptRecords = new Vector<String>();
		slurmScriptRecords.add("#!/bin/bash");
		slurmScriptRecords.add("EXIT_VALUE=0");
		slurmScriptRecords.add("echo \"slurm script running\" >  "
				+ slurmRunningFile.getAbsolutePath());
		// String cmd = "(srun --overcommit --ntasks-per-core=1 -J " + jobName +
		// " -b "+ localJobFile
		// String cmd = "(srun --cpus-per-task=1 -J " + jobName + " -b "+
		// localJobFile
		String cmd = "(srun"
				+ " --nodes=1"
				// + " --no-kill"
				+ " --wait 0"
				+ " --verbose"
				// + " --label"
				+ " --ntasks-per-node=1"
				+ " -J "
				+ jobName
				+ " --ntasks 1"
				+ " --output "
				+ middleScriptLogFile.getAbsolutePath()
				+ " --error "
				+ middleScriptErrFile.getAbsolutePath()
				+ " -b"
				+ " "
				+ middleScriptFile.getAbsolutePath()
				+ " 3>&1 1>&2- 2>&3-) | awk 'BEGIN{FS=\":| \"} {if (NR==2) {print $4; exit}}' > "
				+ srunCmdJobIdFile.getAbsolutePath();

		slurmScriptRecords.add("cd " + scratchDir);
		slurmScriptRecords.add(cmd);
		slurmScriptRecords.add("if [ ! -s "
				+ srunCmdJobIdFile.getAbsolutePath() + " ]; then");
		slurmScriptRecords
				.add("     echo \"srun cmd file missing...will sleep 10 and try again, looking for: "
						+ srunCmdJobIdFile.getAbsolutePath() + "\"");
		slurmScriptRecords.add("     sleep 10");
		slurmScriptRecords.add("     if [ ! -s "
				+ srunCmdJobIdFile.getAbsolutePath() + " ]; then");
		slurmScriptRecords
				.add("     	 echo \"srun cmd file still missing, looked for: "
						+ srunCmdJobIdFile.getAbsolutePath() + "\"");
		slurmScriptRecords
				.add("         echo \"and failed, exiting with EXIT_VALUE=1\"");
		slurmScriptRecords.add("         EXIT_VALUE=1");
		slurmScriptRecords.add("		 echo \"$EXIT_VALUE\" > "
				+ slurmFailFile.getAbsolutePath());
		slurmScriptRecords.add("		 echo \"$EXIT_VALUE\" > "
				+ slurmDoneFile.getAbsolutePath());
		slurmScriptRecords
				.add("		 rm -f " + slurmRunningFile.getAbsolutePath());
		slurmScriptRecords.add("		exit $EXIT_VALUE");
		slurmScriptRecords.add("	fi");
		slurmScriptRecords.add("fi");

		slurmScriptRecords.add("SLURM_JOB_ID=`cat "
				+ srunCmdJobIdFile.getAbsolutePath() + "`");
		slurmScriptRecords.add("EXIT_VALUE=$?");
		slurmScriptRecords.add("if [ $EXIT_VALUE -ne 0 ]; then");
		slurmScriptRecords
				.add("	echo \"***error: had problem cat-ing job id file = "
						+ srunCmdJobIdFile.getName() + "\" > "
						+ slurmFailFile.getAbsolutePath());
		slurmScriptRecords
				.add("	echo \"***error: had problem cat-ing job id file = "
						+ srunCmdJobIdFile.getName() + "\"");
		slurmScriptRecords.add("	rm -f " + slurmRunningFile.getAbsolutePath());
		slurmScriptRecords
				.add("	echo \"slurm is done with ERRORs; slurm couldn't cat slurm job id file; cat EXIT_VALUE=$EXIT_VALUE; exiting...\"");
		slurmScriptRecords.add("	echo \"$EXIT_VALUE\" > "
				+ slurmDoneFile.getAbsolutePath());
		slurmScriptRecords.add("	exit $EXIT_VALUE");
		slurmScriptRecords.add("fi");

		slurmScriptRecords.add("echo \"SLURM_JOB_ID=$SLURM_JOB_ID\"");
		slurmScriptRecords
				.add("while squeue | grep \" $SLURM_JOB_ID \" > /dev/null; do sleep 1; echo \"waiting for slurm job\";done");
		slurmScriptRecords.add("sleep 0.1");
		slurmScriptRecords.add("if [ ! -s "
				+ middleScriptDoneFile.getAbsolutePath() + " ]; then");
		slurmScriptRecords
				.add("     echo \"slurm job not on queue and middleScriptDoneFile missing! reporting error--exiting with EXIT_VALUE=1\"");
		slurmScriptRecords.add("     EXIT_VALUE=1");
		slurmScriptRecords.add("	 echo \"$EXIT_VALUE\" > "
				+ slurmFailFile.getAbsolutePath());
		slurmScriptRecords.add("	 echo \"$EXIT_VALUE\" > "
				+ slurmDoneFile.getAbsolutePath());
		slurmScriptRecords.add("	 rm -f " + slurmRunningFile.getAbsolutePath());
		slurmScriptRecords.add("fi");
		slurmScriptRecords.add("EXIT_VALUE=`cat "
				+ middleScriptDoneFile.getAbsolutePath() + "`");

		slurmScriptRecords.add("if [ $EXIT_VALUE -ne 0 ]; then");
		slurmScriptRecords
				.add("	echo \"middle script exit value was non-zero, EXIT_VALUE=$EXIT_VALUE, dropping slurmFailFile and exiting with $EXIT_VALUE.\"");
		slurmScriptRecords
				.add("	echo \"middle script exit value was non-zero, EXIT_VALUE=$EXIT_VALUE, dropping slurmFailFile and exiting with $EXIT_VALUE.\" > "
						+ slurmFailFile.getAbsolutePath());
		slurmScriptRecords.add("fi");

		slurmScriptRecords.add("rm -f " + slurmRunningFile.getAbsolutePath());
		slurmScriptRecords.add("echo \"slurm done, EXIT_VALUE=$EXIT_VALUE\"");
		slurmScriptRecords.add("echo \"$EXIT_VALUE\" > "
				+ slurmDoneFile.getAbsolutePath());
		slurmScriptRecords.add("exit $EXIT_VALUE");

		logger.info("GenericUtil.runShellScriptViaSlurm(): cmd = " + cmd);
		// GenericUtil.setFileContents(slurmScriptFile, slurmScriptRecords);
		// GenericUtil.makeExecutable(slurmScriptFile);

		File triesFile = new File(scratchDir, "triesError_" + uid + ".txt");

		int exitValue = 1;
		try {
			int tries = 0;
			while (exitValue > 0 && tries < 1) {
				// synchronized (GenericUtil.class) {

				// exitValue = runShellScript(slurmScriptFile,
				// slurmScriptRecords, slurmScriptLogFile, 0);

				exitValue = runShellScript(slurmScriptFile, slurmScriptRecords,
						slurmScriptLogFile, 0, false, true, true);

				logger.info("GenericUtil.runShellScriptViaSlurm(): "
						+ "script complete: exitValue = " + exitValue
						+ "; tries = " + tries);
				if (exitValue == 0)
					break;

				logger.info("GenericUtil.runShellScriptViaSlurm(): problem trying to "
						+ "submit to slurm, going to try again: exitValue = "
						+ exitValue + "; tries = " + tries);

				if (tries > 0)
					GenericUtil.setFileContents(triesFile,
							new String[] { new Integer(tries).toString() });

				tries++;

				Thread.sleep(1000);
				// }
			}
			if (exitValue != 0) {
				String msg = "***error: had problem submitting "
						+ "slurm script:" + "\n\texit code = " + exitValue
						+ "\n\tscratchDir = " + scratchDir + "\n\ttries = "
						+ tries + "\n\tslurmScriptFile = "
						+ slurmScriptFile.getAbsolutePath();
				logger.info(msg);
				logger.warn(msg);
				throw new Exception(msg);
			}

			Vector<String> srunCmdLogContents = GenericUtil
					.getFileContents(srunCmdJobIdFile);
			String jobID = srunCmdLogContents.get(0);
			logger.info("GenericUtil.runShellScriptViaSlurm(): jobID = "
					+ jobID);

			// wait until slurm job is done (if -b option is used, this is
			// necessary)
			//
			// boolean isDone = middleScriptDoneFile.canRead();
			boolean isDone = slurmDoneFile.canRead();
			while (!isDone) {
				Thread.sleep(1000);
				isDone = slurmDoneFile.canRead();
				// isDone = middleScriptDoneFile.canRead();
			}
			// if (!isDone) {
			// String msg = "***error: had problem with slurm script; the file "
			// + middleScriptDoneFile.getAbsolutePath()
			// + " was not generated fast enough: "
			// + "\n\tscratchDir = " + scratchDir
			// + "\n\tslurmScriptFile = " + slurmScriptFile.getAbsolutePath()
			// + "\n\ttries = " + tries
			// + "\n\tisDone = " + isDone
			// + "\n\tcanRead() = " + middleScriptDoneFile.canRead()
			// + "\n\texists() = " + middleScriptDoneFile.exists();
			// logger.info(msg);
			// logger.warn(msg);
			// throw new Exception(msg);
			// }
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
		logger.info("GenericUtil.runShellScriptViaSlurm(): exitValue = "
				+ exitValue);

		return exitValue;
	}

	public static int runShellScriptViaSlurm2(String appName, File scriptFile,
			Vector<String> scriptRecords) throws Exception {

		logger.info("GenericUtil.runShellScriptViaSlurm():"
				+ "\n\tappName = + appName" + "\n\tscriptFile = "
				+ scriptFile.getAbsolutePath());

		GenericUtil.setFileContents(scriptFile, scriptRecords);
		GenericUtil.makeExecutable(scriptFile);

		File scratchDir = scriptFile.getParentFile();

		String jobName = null;
		synchronized (GenericUtil.class) {
			jobName = "s" + appName + "_" + UUID.randomUUID().toString();
			jobName = jobName.substring(0, 19);
		}

		// slurm script calls middle.sh via srun command
		//
		File slurmScriptFile = new File(scratchDir, "slurmScript.sh");
		File slurmScriptOutFile = new File(scratchDir, "srunStdOut.txt");
		File slurmScriptErrFile = new File(scratchDir, "srunStdErr.txt");
		File slurmScriptLogFile = new File(scratchDir, "slurmScriptLog.txt");

		Vector<String> slurmScriptRecords = new Vector<String>();
		// String cmd = "(srun --overcommit --ntasks-per-core=1 -J " + jobName +
		// " -b "+ localJobFile
		// String cmd = "(srun --cpus-per-task=1 -J " + jobName + " -b "+
		// localJobFile
		String cmd = "srun"
				+ " --nodes=1"
				// + " --no-kill"
				+ " --wait=0"
				+ " --no-requeue"
				+ " --verbose"
				// + " --label"
				+ " --ntasks-per-node=1" + " --kill-on-bad-exit" + " -J "
				+ jobName + " --ntasks 1" + " --output "
				+ slurmScriptOutFile.getAbsolutePath() + " --error "
				+ slurmScriptErrFile.getAbsolutePath() + " "
				+ scriptFile.getAbsolutePath();

		logger.info("GenericUtil.runShellScriptViaSlurm(): cmd = " + cmd);
		slurmScriptRecords.add("cd " + scratchDir);
		slurmScriptRecords.add(cmd);

		int exitValue = 2;
		// try {
		int tries = 0;
		// slurm sumbission usually is exit code > 1 (e.g., 124)
		// other error want to keep moving
		while (exitValue > 1 && tries++ < 100) {
			exitValue = runShellScript(slurmScriptFile, slurmScriptRecords,
					slurmScriptLogFile, 0, false, false, true);
			logger.info("GenericUtil.runShellScriptViaSlurm(): "
					+ "slurm script returned:" + "\n\texitValue = " + exitValue
					+ "\n\tscratchDir = " + scratchDir.getAbsolutePath()
					+ "\n\ttries = " + tries);
			if (exitValue <= 1)
				break;
			logger.info("GenericUtil.runShellScriptViaSlurm(): "
					+ "***warn: non-zero exit on slurm submission, "
					+ "trying again in one second:" + "\n\texitValue = "
					+ exitValue + "\n\tscratchDir = "
					+ scratchDir.getAbsolutePath() + "\n\ttries = " + tries);
			Thread.sleep(1000);
		}
		if (exitValue > 1) {
			String msg = "***error: had problem submitting "
					+ "slurm script:\n\texit code = " + exitValue + "\n\t"
					+ "scratchDir = " + scratchDir.getAbsolutePath()
					+ "\n\tslurmScriptFile = "
					+ slurmScriptFile.getAbsolutePath() + "\n\ttries = "
					+ tries;

			logger.info(msg);
			logger.warn(msg);
			throw new Exception(msg);
		}
		// } catch (Exception ioe) {
		// logger.info(ioe.toString());
		// logger.warn(ioe.toString());
		// ioe.printStackTrace();
		// }
		logger.info("GenericUtil.runShellScriptViaSlurm(): exitValue = "
				+ exitValue);

		return exitValue;
	}

	public static long runAppBySLURMQueue(File scratchDir, File localJobFile,
			String appName, Properties servProps) throws Exception {

		long start = System.currentTimeMillis() / 1000;

		// int minfailedtime = 0;
		int maxattempts = 10;
		// int attempts = 1;
		// long clocktime = 0;

		// while (attempts <= maxattempts && clocktime < minfailedtime) {
		// while (true) {
		// get the information used during the running of the App
		// System.out.println(" >>>>>>>> Running " + appName+
		// " by Queue in Directory " + scratchDir);
		logger.info(" >>>>>>>> Running " + appName + " by Queue in Directory "
				+ scratchDir);
		// scriptName
		// String jobName = "s" + appName+ (Long.toHexString(new
		// Date().getTime()));
		String jobName = null;
		synchronized (GenericUtil.class) {
			jobName = "s" + appName + "_" + UUID.randomUUID().toString();
			// jobName = "s" + appName + "_" + slurmSubmitCtr++;
			jobName = jobName.substring(0, 19);
		}
		String scriptName = (servProps.getProperty(
				"provider.app.qsubScriptName", appName + "qsub")).trim();
		String scriptLogName = (servProps.getProperty(
				"provider.app.qsubLogFileName", appName + "qsub.log")).trim();
		// shell command for when script executes
		Vector<String> shCmd = new Vector<String>();
		shCmd.add((servProps.getProperty("provider.app.qsubShellCommand",
				"tcsh")).trim());
		// this is very machine configuration specific for SLURM. load the
		// necessary modules SLURM module - not needed for NMEDA
		// scriptInputRecords.add("module load slurm");
		// scriptInputRecords.add("module load openmpi");
		// scriptInputRecords.add("module load munge");
		Vector<String> scriptInputRecords = new Vector<String>();
		File srunCmdLog = new File(scratchDir, "srunCmd.log");
		scriptInputRecords.add("#");
		// String cmd = "(srun --overcommit --ntasks-per-core=1 -J " + jobName +
		// " -b "+ localJobFile
		// String cmd = "(srun --cpus-per-task=1 -J " + jobName + " -b "+
		// localJobFile
		String cmd = "(srun --ntasks-per-node=1 -J " + jobName + " -b "
				+ localJobFile + " 3>&1 1>&2- 2>&3-) | awk '{print $3}' > "
				+ srunCmdLog.getAbsolutePath();
		logger.info("application Exe command = " + cmd);
		scriptInputRecords.add("#");
		scriptInputRecords.add("cd " + scratchDir);
		scriptInputRecords.add("chmod 755 " + localJobFile);
		scriptInputRecords.add(cmd);
		// add the squeue command so the jobId is written to the log file
		// scriptInputRecords.add("squeue -o\"%20j %7i\"");
		logger.info(appName + " Exe command = " + cmd);

		// add semaphore: drop file to signal slurm job done
		//
		Vector<String> localScript = getFileContents(localJobFile);
		File slurmDone = new File(scratchDir, "slurmDone.txt");
		slurmDone.delete();
		localScript.add("echo \"slurmDone\" > " + slurmDone);
		setFileContents(localJobFile, localScript);

		try {
			int exitValue = 1;
			int tries = 0;
			while (exitValue > 0 && tries++ < maxattempts) {
				synchronized (GenericUtil.class) {
					// Process qsubStatus =
					// GenericUtil.runShellScript(scriptName,
					// scriptLogName, scriptInputRecords,
					// scratchDir.getAbsolutePath()+ "/", shCmd);
					exitValue = runShellScript(
							new File(scratchDir, scriptName),
							scriptInputRecords, new File(scratchDir,
									scriptLogName), 0);
					logger.info("script complete: exitValue = " + exitValue
							+ "; tries = " + tries);
					if (exitValue == 0)
						break;
					logger.info("problem trying to submit to slurm, going to try again: exitValue = "
							+ exitValue + "; tries = " + tries);
					Thread.sleep(500);
				}
			}

			// open the log file and get the queue Job ID
			// File scriptLogFile = new File(scratchDir, scriptLogName);
			// boolean logExists = false;
			// int tries2 = 0;
			// while (!logExists && tries2++ < 1000) {
			// logExists = scriptLogFile.canRead();
			// logger.info("waiting for log file to write...tries2 = " +
			// tries2);
			// Thread.sleep(100);
			// }

			// get jobId
			Vector<String> srunCmdLogContents = GenericUtil
					.getFileContents(srunCmdLog);
			String jobID = srunCmdLogContents.get(0);

			// Vector<String> logFileContents =
			// GenericUtil.getFileContents(scriptLogFile);
			// // find the right record based on jobName
			// String jobID = null;
			// for (int i = 0; i < logFileContents.size(); i++) {
			// String rec1 = (String) (logFileContents.elementAt(i));
			// //System.out.println("Queue record =  " + rec1);
			// logger.info("looking for \"" + jobName + "\" in queue record =  "
			// + rec1);
			// String[] jobsubrec = rec1.split(" ", 2);
			// //if (jobsubrec[0].equalsIgnoreCase(jobName))
			// if (jobsubrec[0].toLowerCase().startsWith(jobName.toLowerCase()))
			// jobID = jobsubrec[1];
			// }
			// if (jobID == null) {
			// //System.out.println("No Job found in queue with name = "+
			// jobName);
			// logger.info("No Job found in queue with name = "+ jobName);
			// throw new
			// Exception("*** error: problem submittnig to slurm, see "
			// + scratchDir);
			// //return clocktime;
			// }

			logger.info(appName + " Job Id =  " + jobID);
			// System.out.println(appName + " Job Id =  " + jobID);
			// clocktime = watchSLURMQueuedJob(scratchDir, shCmd, jobID,appName,
			// servProps);

			boolean isDone = slurmDone.canRead();
			while (!isDone) {
				Thread.sleep(5000);
				isDone = slurmDone.canRead();
			}

			// return clocktime;

			// if (clocktime < minfailedtime && attempts <= maxattempts)
			// //System.out.println(appName+
			// " Job ended prematurely submitting attempt # "+ attempts);
			// //System.out.println(">>>>> Job Run Attempt # " + attempts+
			// "completed");
			// logger.info(appName+
			// " Job ended prematurely submitting attempt # "+ attempts);
			// logger.info(">>>>> Job Run Attempt # " + attempts+ "completed");
			// if (attempts > maxattempts)
			// // attempts = attempts - 1;
			// //System.out.println(appName + " Job ended prematurely "+
			// attempts+ " attempts made to submit job and failed");
			// logger.info(appName + " Job ended prematurely "+ attempts+
			// " attempts made to submit job and failed");
			// attempts = attempts + 1;
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
		// }
		long elapsed = System.currentTimeMillis() / 1000 - start;
		logger.info("elapsed time = " + elapsed);

		return elapsed;
	}

	public static String padStringOnRight(String instring, int finalLength) {
		int lofis = instring.length();
		for (int i = 0; i < finalLength - lofis; i++)
			instring = instring + " ";
		return instring;
	}

	public static String padStringOnLeft(String instring, int finalLength) {
		int lofis = instring.length();
		String padding = "";
		for (int i = 0; i < finalLength - lofis; i++)
			padding = padding + " ";
		String padInstring = padding + instring;
		return padInstring;
	}

	public static long watchSLURMQueuedJob(File scratchDir, Vector<?> shCmd,
			String jobID, String appName, Properties servProps)
			throws Exception {
		// watch the job for max wait time and qstat at specified intervals
		// scriptName
		String qstatScriptName = (servProps.getProperty(
				"provider.app.qstatScriptName", appName + "qstat")).trim();
		String qstatLogName = (servProps.getProperty(
				"provider.app.qstatbLogFileName", appName + "qstat.log"))
				.trim();

		// shell command for when script executes
		// script records
		Vector<String> qstatInputRecords = new Vector<String>();
		qstatInputRecords.add("#");
		String cmd1 = (servProps.getProperty("provider.app.qstatcommand",
				"squeue")).trim();
		qstatInputRecords.add("#");
		// no need to load slurm for nmeda
		// qstatInputRecords.add("module load slurm");
		qstatInputRecords.add("cd " + scratchDir);
		qstatInputRecords.add(cmd1 + " -j " + jobID
				+ " -o \"%20j %7i %9P %8u %2t %9M %6D %N\"");

		// Loop until the job completes or max wait time is exceeded.
		// get the time constants from the property file
		Long maxWaitTime = new Long((servProps.getProperty(
				"provider.app.maxWaitTimeinSeconds", "600")).trim());
		Long qstatIntervalTime = new Long((servProps.getProperty(
				"provider.app.qstatIntervalinSeconds", "5")).trim());
		Long sleepTimeInterval = new Long((servProps.getProperty(
				"provider.app.sleepTimeIntervalinSeconds", "5")).trim());
		long totalElapsedTime = 0;
		long timeSinceqstat = 0;
		boolean jobDone = false;
		boolean callqstat = true;
		while (!jobDone) {
			Thread.sleep(sleepTimeInterval.longValue() * 1000);
			totalElapsedTime = totalElapsedTime + sleepTimeInterval.longValue();
			// System.out.println("***************** Total Clock Time Elapsed During Run for Job Id = "
			// + jobID
			// + " Time = "
			// + totalElapsedTime
			// + " Seconds ***************");
			logger.info("***************** Total Clock Time Elapsed During Run for Job Id = "
					+ jobID
					+ " Time = "
					+ totalElapsedTime
					+ " Seconds ***************");
			if (totalElapsedTime >= maxWaitTime.longValue()) {
				jobDone = true;
				callqstat = true;
			}
			timeSinceqstat = timeSinceqstat + sleepTimeInterval.longValue();
			if (callqstat || (timeSinceqstat >= qstatIntervalTime.longValue())) {
				timeSinceqstat = 0;
				callqstat = false;
				// System.out.println("***************** qstat for job Id = "+
				// jobID + "***************");
				logger.info("***************** qstat for job Id = " + jobID
						+ "***************");
				// Process qstatStatus = null;
				int exitValue = 1;
				synchronized (GenericUtil.class) {
					// qstatStatus = GenericUtil.runShellScript(
					// qstatScriptName, qstatLogName, qstatInputRecords,
					// scratchDir.getAbsolutePath() + "/", shCmd);
					int tries = 0;
					while (exitValue > 0 && tries++ < 50) {
						exitValue = runShellScript(new File(scratchDir,
								qstatScriptName), qstatInputRecords, new File(
								scratchDir, qstatLogName), 0);
						logger.info("slurm monitor script attempt: exitValue = "
								+ exitValue + "; tries = " + tries);
						if (exitValue == 0)
							break;
						Thread.sleep(500);
					}
				}
				// System.out.println("qstatStatus = " + qstatStatus);
				// if (qstatStatus.exitValue() == 0) {
				if (exitValue == 0) {
					// open the log file and get the queue Job ID
					File qstatLogFile = new File(scratchDir, qstatLogName);
					Vector<String> qstatlogFileContents = GenericUtil
							.getFileContents(qstatLogFile);
					// Dump the contents of the qstat log file
					if (qstatlogFileContents.size() == 1)
						jobDone = true;
					for (int i = 0; i < qstatlogFileContents.size(); i++) {
						// System.out.println(qstatlogFileContents.elementAt(i));
						logger.info(qstatlogFileContents.elementAt(i));
					}
				} else {
					jobDone = true;
					return totalElapsedTime;
				}
			}
			// System.out.println("Job Done = " + jobDone);
			logger.info("Job Done = " + jobDone);
		}
		return totalElapsedTime;
	}

	/**
	 * Deep copies a Double[][] to an ArrayList<List<?>>
	 * 
	 * @param data
	 * @return ArrayList<List<?>> copy of data
	 * @author Travis Sims
	 */
	public static List<List<?>> nestedDoubleToNestedList(Double[][] data) {
		List<List<?>> list = new ArrayList<List<?>>();
		List<Double> dummy;

		for (int row = 0; row < data.length; row++) {
			dummy = new ArrayList<Double>();

			for (int col = 0; col < data[0].length; col++) {
				dummy.add(data[row][col]);
			}
			list.add(dummy);
		}
		return list;
	}
	
	public static synchronized void appendFileContents(String msg, File file) {
		if (file.isDirectory()) file = new File(file, "genericUtil.txt");
		try {
			GenericUtil.appendFileContents(file, new String[] {msg});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String findExistingDirectory(String[] dirs) {
		for (String dir : dirs) {
			File dirFile = new File(dir);
			if (dirFile.exists() && dirFile.isDirectory())
				return dir;
		}
		return null;
	}

	public static File findExistingFile(String[] fileDirs, String fileName) {
		for (String dir : fileDirs) {
			File file = new File(dir + File.separator + fileName);
			if (file.exists() && file.isFile())
				return file;
		}
		return null;
	}
}


class StreamGobbler extends Thread {

	private boolean displayStreamOutput;
	private InputStream is;
	private PrintWriter logPw;
	private String type;
	public boolean keepGoing = true;
	private File dir = null;
	
	public StreamGobbler(InputStream is, String type,
			boolean displayStreamOutput, File logFile, File dir)
			throws FileNotFoundException {
		this.is = is;
		this.type = type;
		this.displayStreamOutput = displayStreamOutput;
		logPw = new PrintWriter(new FileOutputStream(logFile));
		this.dir = dir;
	}
	
	public void closeDown() {
		GenericUtil.appendFileContents("StreamGobbler.closingDown(): setting flag to stop running "
				+ " keepGoing = false now; stream gobbler type = " + type, dir);
		keepGoing = false;
	}
	
	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null && keepGoing) {
				
				GenericUtil.appendFileContents("StreamGobbler.run(): while loop; "
						+ " stream gobbler type = " + type + "; line = " + line, dir);
				
				if (displayStreamOutput) System.out.println(type + ">" + line);
				
				GenericUtil.appendFileContents("StreamGobbler.run(): while loop; "
						+ " stream gobbler type = " + type + "; here0", dir);
				
				logPw.println(line);
				
				
				GenericUtil.appendFileContents("StreamGobbler.run(): while loop; "
						+ " stream gobbler type = " + type + "; here1", dir);
				
				logPw.flush();
				
				
				GenericUtil.appendFileContents("StreamGobbler.run(): while loop; "
						+ " stream gobbler type = " + type + "; here2", dir);
				
				while (!br.ready() && keepGoing) {
					//System.out.println("gobbler type = " + type + " is not ready.");
					GenericUtil.appendFileContents("StreamGobbler.run(): inner while loop; "
							+ "br not ready; stream gobbler type = " + type + "keepGoing = " + keepGoing, dir);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (!keepGoing) break;
			}
			GenericUtil.appendFileContents("StreamGobbler.run(): exited run inner loop for type = "
					+ type + "; keepGoing = " + keepGoing, dir);
			logPw.flush();
			logPw.close();
			br.close();
			isr.close();
			is.close();
		} catch (IOException ioe) {
			System.out.println("***exception in gobbler type = " + type + ": " + ioe);
			GenericUtil.appendFileContents("StreamGobbler.run(): exception = "
					+ ioe, dir);
			ioe.printStackTrace();
		} finally {
			logPw.flush();
			logPw.close();
			try {
				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

class StreamJobControlWriter extends Thread {

	private boolean displayStreamOutput;
	private PrintStream ps;
	private OutputStream os;
	private boolean sendKill = false;
	private boolean keepRunning = true;
	private Logger logger;
	private File dir = null;
	
	
	public StreamJobControlWriter(OutputStream os, boolean displayStreamOutput, Logger logger
			, File dir)
			throws FileNotFoundException {
		this.os = os;
		this.ps = new PrintStream(os);
		this.displayStreamOutput = displayStreamOutput;
		this.logger = logger;
		this.dir = dir;
	}

	public void closeDown() {
		keepRunning = false;
	}
	
	public void run() {
			String message = "KEEP_GOING";
			while (keepRunning) {
				if (sendKill) {
					GenericUtil.appendFileContents("StreamJobControlWriter.run(): provider sending KILL to script...", dir);
					message = "KILL";
					keepRunning = false;
				}
				if (displayStreamOutput) System.out.println("JOB CTRL WRITE>" + message);
				ps.println(message);
				ps.flush();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			ps.close();
	}
	
	public void sendKillToWrapperScript() {
		sendKill = true;
		GenericUtil.appendFileContents("StreamJobControlWriter.sendKillToWrapperScript():"
				+ " setting sendKill flag to true.", dir);
	}
}
	
