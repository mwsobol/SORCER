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

package sorcer.ssb.tools.plugin.browser;
/*
 *
 * Created Fri Aug 19 10:34:02 BST 2005
 */

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import sorcer.ssb.browser.api.SSBPluginException;

public class FilterRegistry implements Runnable {
	private ArrayList _filters = new ArrayList();
	private File _toolsDir;

	/**
	 * getFilters
	 * 
	 * @return java.util.ArrayList
	 */
	public synchronized ArrayList getFilters() {
		return _filters;
	}

	public void init(File toolsDir) {
		_toolsDir = toolsDir;
		//System.out.println("### filters dir="+toolsDir.getAbsolutePath());
		// new Thread(this).start();
		run();
	}

	public void run() {

		synchronized (_filters) {

			String[] list = _toolsDir.list();
			for (int i = 0; i < list.length; i++) {

				if (list[i].endsWith(".jar")) {
					File f = new File(_toolsDir.getAbsolutePath()
							+ File.separator + list[i]);
					try {
						scanJar(f, true);

					} catch (Exception ex) {
						System.out
								.println("Failed to load filter plugin " + ex);
						ex.printStackTrace();
					}
				}
			}
		}
		System.out.println("Number of installed filters: " + _filters.size());
	}

	public boolean scanJar(File jar, boolean doLoad) throws Exception {

		// COULD TRY USING HTTP URLS and appending ?currenttime?
		// URL httpURL=new
		// URL(_baseUrl+jar.getName()+"?"+System.currentTimeMillis());

		URLClassLoader cl = new URLClassLoader(new URL[] { jar.toURL() });
		Class iPlugin = sorcer.ssb.browser.api.SSBrowserFilter.class;

		ZipFile zip = new ZipFile(jar);
		Enumeration iter = zip.entries();
		while (iter.hasMoreElements()) {
			ZipEntry ze = (ZipEntry) iter.nextElement();

			String clazzName = ze.getName();
			if (clazzName.endsWith(".class") == false) {
				continue;
			}
			clazzName = clazzName.replace('/', '.');
			clazzName = clazzName.substring(0, clazzName.lastIndexOf("."));
			try {
				Class c = cl.loadClass(clazzName);

				// System.out.println("Class="+c.getName());
				if (!c.equals(iPlugin) && iPlugin.isAssignableFrom(c)) {

					System.out.println("Filter found class=" + c.getName());

					ClassLoader ccl = Thread.currentThread()
							.getContextClassLoader();
					try {
						Thread.currentThread().setContextClassLoader(cl);
						_filters.add(c.newInstance());
					} catch (Throwable t) {
						t.printStackTrace();
						System.out.print(t);
						throw new SSBPluginException(t);
					} finally {
						Thread.currentThread().setContextClassLoader(ccl);
					}
				}
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}
		return false;

	}

}
