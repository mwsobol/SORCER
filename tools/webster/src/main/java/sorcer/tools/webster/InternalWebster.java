/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package sorcer.tools.webster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.StringTokenizer;

/**
 * Helper class for starting an Internal Webster
 * 
 * @author Dennis Reedy and Mike Sobolewski
 */
public class InternalWebster {
	private static Logger logger = LoggerFactory.getLogger(InternalWebster.class.getName());
	private static boolean debug = false;
	public static final String WEBSTER_ROOTS = "sorcer.webster.roots";

	  /**
     * Start an internal webster, setting the webster root to the location of
     * SORCER lib-dl directories, and appending exportJars as the codebase jars
     * for the JVM.
     *
     * @param exportJars
     *            The jars to set for the codebase
     *
     * @return The port Webster has been started on
     *
     * @throws IOException
     *             If there are errors creating Webster
     */
    public static Webster startWebster(String... exportJars) throws IOException {
        return startWebster(exportJars, null);
    }

    
	/**
	 * Start an internal webster, setting the webster root to the location of
	 * SORCER lib-dl directories, and appending exportJars as the codebase jars
	 * for the JVM.
	 * 
	 * @param exportJars
	 *            The jars to set for the codebase
	 * 
	 * @return The port Webster has been started on
	 * 
	 * @throws IOException
	 *             If there are errors creating Webster
	 */
	public static Webster startWebster(String[] exportJars, String[] websterRoots) throws IOException {
		String codebase = System.getProperty("java.rmi.server.codebase");
//		if (codebase != null)
//			throw new RuntimeException("Codebase is alredy specified: "
//					+ codebase);

		String d = System.getProperty("webster.debug");
		if (d != null && d.equals("true"))
			debug = true;	

		String roots;
		InetAddress ip = InetAddress.getLocalHost();
		String localIPAddress = ip.getHostAddress();
		String sorcerHome = System.getProperty("sorcer.home");
        StringBuilder sb = new StringBuilder();
		if (websterRoots == null) {
			// defaults Sorcer roots
			roots = System.getProperty(WEBSTER_ROOTS);
	    } else {
	    	for (int i=0; i<websterRoots.length; i++) {
	    		sb.append(';').append(websterRoots[i]);
	        }
	    	roots = sb.toString();
	    }
	   if (roots == null) {
			// defaults iGrid roots
			String fs = File.separator;
			sb = new StringBuilder();
			sb.append(sorcerHome).append(fs).append("lib").append(fs).append("sorcer").append(fs).append("lib-dl")
					.append(';').append(sorcerHome).append(fs).append("lib").append(fs).append("sorcer").append(fs).append("lib")
					.append(';').append(sorcerHome).append(fs).append("lib").append(fs).append("eng").append(fs).append("lib-dl")
					.append(';').append(sorcerHome).append(fs).append("lib").append(fs).append("eng").append(fs).append("lib")
					.append(';').append(sorcerHome).append(fs).append("lib").append(fs).append("river").append(fs).append("lib-dl")
					.append(';').append(sorcerHome).append(fs).append("deploy")
					.append(';').append(sorcerHome).append(fs).append("lib").append(fs).append("local").append(fs).append("lib-dl");
			roots = sb.toString();
		}

		String sMinThreads = System.getProperty("sorcer.webster.minThreads",
				"1");
		int minThreads = 1;
		try {
			minThreads = Integer.parseInt(sMinThreads);
		} catch (NumberFormatException e) {
			logger.warn("Bad Min Threads Number [" + sMinThreads
					+ "], " + "default to " + minThreads, e);
		}
		String sMaxThreads = System.getProperty("webster.maxThreads",
				"10");
		int maxThreads = 10;
		try {
			maxThreads = Integer.parseInt(sMaxThreads);
		} catch (NumberFormatException e) {
			logger.warn("Bad Max Threads Number [" + sMaxThreads
					+ "], " + "default to " + maxThreads, e);
		}
		String sPort = System.getProperty("webster.port", "0");
		int port = 0;
		try {
			port = Integer.parseInt(sPort);
		} catch (NumberFormatException e) {
			logger.warn("Bad port Number [" + sPort + "], "
					+ "default to " + port, e);
		}

		String address = System.getProperty("webster.interface");
		Webster webster = new Webster(port, roots, address, minThreads, maxThreads, true);
		port = Webster.getWebsterPort();
		if (logger.isTraceEnabled())
			logger.trace("Webster MinThreads=" + minThreads + ", "
					+ "MaxThreads=" + maxThreads);

		if (logger.isDebugEnabled())
			logger.debug("Webster serving on port=" + port);

		String[] jars = null;
		String jarsList = null;
		if (exportJars != null)
			jars = exportJars;
		else {
			jarsList = System.getProperty("codebase.jars");
			if (jarsList == null || jarsList.length() == 0)
				throw new RuntimeException(
						"No jar files available for the webster codebase");
			else
				jars = toArray(jarsList);
		}
		
		codebase = "";
		sb = new StringBuilder();
		for (int i = 0; i < jars.length - 1; i++) {
			sb.append("http://").append(localIPAddress).append(":")
					.append(port).append("/").append(jars[i]).append(" ");
		}
		sb.append("http://").append(localIPAddress).append(":").append(port)
				.append("/").append(jars[jars.length - 1]);
		codebase = sb.toString();
		System.setProperty("java.rmi.server.codebase", codebase);
		if (logger.isDebugEnabled())
			logger.debug("Setting 'java.rmi.server.codebase': " + codebase);

		return webster;
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

	public static void main(String[] args) {
		try {
			startWebster(new String[] { "sorcer-dl.jar" });
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
