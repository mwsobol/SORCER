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
 * Created Fri Aug 19 08:41:57 BST 2005
 */
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFrame;

import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import sorcer.ssb.browser.api.SSBPluginException;
import sorcer.ssb.browser.api.SSBrowserPlugin;
import sorcer.ssb.browser.api.impl.SSBrowserEnvironmentImpl;

public class PluginRegistry implements Runnable {

	private ArrayList _plugins = new ArrayList();
	private JFrame _frame;
	private ServiceBrowserUI _ui;
	private File _toolsDir;
	private static ServiceTemplate _template = new ServiceTemplate(null, null,
			null);

	public PluginRegistry(JFrame frame, ServiceBrowserUI ui) {
		_frame = frame;
		_ui = ui;
	}

	void lusDiscovered(ServiceRegistrar reg) {
		try {
			ServiceMatches sm = reg.lookup(_template, Integer.MAX_VALUE);
			ServiceItem[] items = sm.items;
			for (int i = 0; i < items.length; i++) {
				serviceDiscovered(items[i]);
			}
		} catch (Exception ex) {
			System.err.println("Plugin registry: Caught Exception: "
					+ ex.getClass().getName() + "; Msg: " + ex.getMessage());
			ex.printStackTrace();
		}

	}

	void serviceDiscovered(ServiceItem serviceItem) {
		synchronized (_plugins) {
			int nt = _plugins.size();
			for (int i = 0; i < nt; i++) {
				((SSBrowserEnvironmentImpl) _plugins.get(i))
						.serviceDiscovered(serviceItem);
			}
		}
	}

	void serviceDiscarded(ServiceID serviceID) {
		synchronized (_plugins) {
			int nt = _plugins.size();
			for (int i = 0; i < nt; i++) {
				((SSBrowserEnvironmentImpl) _plugins.get(i))
						.serviceDiscarded(serviceID);
			}
		}
	}

	void serviceModified(ServiceID serviceID) {
		synchronized (_plugins) {
			int nt = _plugins.size();
			for (int i = 0; i < nt; i++) {
				((SSBrowserEnvironmentImpl) _plugins.get(i))
						.serviceModified(serviceID);
			}
		}
	}

	public void init(File toolsDir) {
		_toolsDir = toolsDir;
		// System.out.println("### plugins dir="+toolsDir.getAbsolutePath());
		new Thread(this).start();
	}

	public void run() {

		String[] list = _toolsDir.list();
		for (int i = 0; i < list.length; i++) {

			if (list[i].endsWith(".jar")) {
				File f = new File(_toolsDir.getAbsolutePath() + File.separator
						+ list[i]);
				try {
					scanJar(f, true);

				} catch (Exception ex) {
					System.out.println("Failed to load plugin " + ex);
					ex.printStackTrace();
				}
			}
		}
		System.out.println("Number of installed plugin: " + _plugins.size());
	}

	public boolean scanJar(File jar, boolean doLoad) throws Exception {

		// COULD TRY USING HTTP URLS and appending ?currenttime?
		// URL httpURL=new
		// URL(_baseUrl+jar.getName()+"?"+System.currentTimeMillis());

		URLClassLoader cl = new URLClassLoader(new URL[] { jar.toURL() });
		Class iPlugin = sorcer.ssb.browser.api.SSBrowserPlugin.class;

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

					System.out.println("Plugin found class=" + c.getName());
					if (!doLoad) {

						return true;
					}
					ClassLoader ccl = Thread.currentThread()
							.getContextClassLoader();
					try {
						Thread.currentThread().setContextClassLoader(cl);
						addPlugin(c);
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

	private void addPlugin(Class pluginClass) throws Exception {

		SSBrowserPlugin iPlugin = (SSBrowserPlugin) pluginClass.newInstance();
		// System.out.println("Plugin="+iPlugin.getDisplayName());

		System.out.println("Initializing plugin: " + iPlugin.getDisplayName());
		SSBrowserEnvironmentImpl env = new SSBrowserEnvironmentImpl(_frame,
				iPlugin, _ui);
		try {
			iPlugin.initialize(env);
			iPlugin.enable(true);
			_plugins.add(env);

		} catch (Exception ex) {
			System.out.print(ex);
			ex.printStackTrace();
		}

	}
}
