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

package sorcer.ssb.jini.studio;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.url.httpmd.HttpmdUtil;
import sorcer.ssb.tools.plugin.browser.ServiceBrowserUI;

public class CodeServer implements Runnable {
	private ServerSocket _server;
	private String _path;
	private Class _rootClass;
	private static int STARTPORT = 8100;
	private static int ENDPORT = 10000;
	private static CodeServer _jsInstance;
	private static Thread _thread;
	private static boolean _useHttpmd;
	public final static String STARTPORT_ENTRY = "websterStartPort";
	public final static String ENDPORT_ENTRY = "websterEndPort";
	private static Configuration config;
	
	private CodeServer(Configuration configuration) {
		int val = 8100;
		config = configuration;
		
		try {
			STARTPORT = (Integer) config.getEntry(ServiceBrowserUI.CONFIG_MODULE,
					STARTPORT_ENTRY, int.class);
		} catch (ConfigurationException e) {
			STARTPORT = 8100;
		}
		
		try {
			ENDPORT = (Integer) config.getEntry(ServiceBrowserUI.CONFIG_MODULE,
					ENDPORT_ENTRY, int.class);
		} catch (ConfigurationException e) {
			ENDPORT = 10000;
		}
		System.out.println("webster start port: " + STARTPORT + ", end port: " + ENDPORT);
	}
		

	public static void useHttpmd() {
		_useHttpmd = true;
	}

	public static void autoStart() {
		autoStart(null, config);
	}

	public static void autoStart(Class rootClass, Configuration configuration) {
		_jsInstance = new CodeServer(configuration);
		_jsInstance._rootClass = rootClass;
		_jsInstance.start();
	}

	public static void stop() {
		if (_jsInstance == null) {
			return;
		}
		_thread.interrupt();
		try {
			_jsInstance._server.close();
		} catch (Exception ex) {
		}

	}

	// picks a port between STARTPORT-ENDPORT
	public void start() {
		// set the class to use the path for, this alls the CodeServer to be in
		// a separate JAR
		if (_rootClass == null) {
			_rootClass = getClass();
		}
		for (int i = STARTPORT; i < ENDPORT; i++) {
			try {
				start(i);
				return;
			} catch (Exception ex) {
				System.err.println(ex.getMessage());
			}
		}
	}

	public void start(int port) throws Exception {
		_server = new ServerSocket(port);

		System.out.println("SSB Code Server started on port " + port);

		setCodebase(port);
		_thread = new Thread(this);
		_thread.start();
	}

	public void run() {
		while (Thread.currentThread().isInterrupted() == false) {
			try {
				final Socket request = _server.accept();
				Thread t = new Thread() {
					public void run() {
						serviceRequest(request);
					}
				};
				t.start();
			} catch (Exception ex) {
				System.out.println(ex);
			}
		}
		System.out.println("SSB code server interrupted, Stopped");
	}

	public static String getPathForClass(Class clazz) throws Exception {
		URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		String path = url.getPath();
		// if we'return running on windows remove the proceeding /
		if (java.io.File.separator.equals("\\") && path.startsWith("/")) {
			path = path.substring(1, path.length());
		}
		// remove any %20 with spaces
		int index = path.indexOf("%20");
		while (index != -1) {
			path = path.substring(0, index) + " "
					+ path.substring(index + 3, path.length());
			index = path.indexOf("%20");
		}
		return path;
	}

	void setCodebase(int port) throws Exception {

		String cb = System.getProperty("java.rmi.server.codebase");
		if (cb == null) {

			_path = getPathForClass(_rootClass);

			InetAddress host = InetAddress.getLocalHost();

			// System.out.println("ssb.webster="+=System.getProperty("ssb.webster"));

			String hostToUse = System.getProperty("ssb.webster",
					host.getHostAddress());

			String codebase = null;
			if (_path.endsWith(".jar")) {
				File f = new File(_path);
				String jarName = f.getName();
				_path = _path.substring(0, _path.length() - jarName.length());
				if (_useHttpmd) {
					String path = _path.substring(0, _path.lastIndexOf("/"));
					codebase = "httpmd://" + hostToUse + ":" + port + "/"
							+ jarName + ";sha=0";
					codebase = HttpmdUtil.computeDigestCodebase(path, codebase);
				} else {
					codebase = "http://" + hostToUse + ":" + port + "/"
							+ jarName;
				}
			} else {
				codebase = "http://" + hostToUse + ":" + port + "/";
			}
			System.setProperty("java.rmi.server.codebase", codebase);

			System.out.println("Setting codebase to " + codebase);
			System.out.println("Path to files=" + _path);
		}
	}

	private void serviceRequest(final Socket request) {

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					request.getInputStream()));

			String ln = br.readLine();

			int spos = ln.indexOf("/");
			int epos = ln.indexOf(" ", spos);

			String fileName = null;
			String resourceName = ln.substring(spos + 1, epos);

			//System.out.println("Request for " + resourceName);
			boolean isJar = false;

			if (resourceName.endsWith(".class")) {
				// just get the name
				spos = resourceName.indexOf(".class");
				String className = pathToClass(resourceName.substring(0, spos));
				//System.out.println("Class name="+className);
				String path = getPathForClass(Class.forName(className));
				if (path.endsWith(".jar")) {
					fileName = path;
					isJar = true;
				} else {
					fileName = path + resourceName;
				}
			} else if (resourceName.endsWith(".jar")) {
					fileName = _path + resourceName;
			} else {
				// serves .jar or .class files only
				request.close();
				return;
				//fileName = _path + ln.substring(spos + 1, epos);
			}
			DataOutputStream os = new DataOutputStream(
						new BufferedOutputStream(request.getOutputStream()));
			try {
				if (fileName == null)
					os.writeBytes("HTTP/1.0 404 Not found\r\n\r\n");
				else {
					InputStream fis = null;
					long len = 0;

					if (isJar) {
						ZipFile zippy = new ZipFile(fileName);
						ZipEntry ze = zippy.getEntry(resourceName);
						len = ze.getSize();
						fis = zippy.getInputStream(ze);
						// System.out.println("Using ZIP len="+len);
					} else {
						File file = new File(fileName);
						len = file.length();
						fis = new FileInputStream(file);
					}
					os.writeBytes("HTTP/1.0 200 OK\r\n");
					os.writeBytes("Content-Length: " + len + "\r\n");
					os.writeBytes("Content-Type: application/java\r\n\r\n");

					byte[] b = new byte[1024];
					int nBytes = fis.read(b);
					while (nBytes != -1) {
						os.write(b, 0, nBytes);
						nBytes = fis.read(b);
					}
					fis.close();
				}
			} catch (IOException ex) {
				//ex.printStackTrace();
				System.out.println(ex.getMessage() + " for : " + fileName + "from: " + request);
			}
			os.flush();
			os.close();
			request.close();
			//System.out.println("Completed request for "+fileName);
		} catch (Exception ex) {
			//ex.printStackTrace();
		}
	}

	String pathToClass(String packageName) {
		int index = packageName.indexOf("/");
		while (index != -1) {
			packageName = packageName.replace('/', '.');
			index = packageName.indexOf("/");
		}
		return packageName;
	}
}
