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

package sorcer.ssb.browser.api.impl;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import sorcer.ssb.browser.api.SSBrowserPlugin;
import sorcer.ssb.browser.api.SSBrowserTab;
import sorcer.ssb.browser.api.SSBEnvironment;
import sorcer.ssb.browser.api.SSBPluginException;
import sorcer.ssb.tools.plugin.browser.ServiceBrowserUI;
import sorcer.ssb.tools.plugin.browser.TreeRenderer;

public class SSBrowserEnvironmentImpl implements SSBEnvironment {

	private JFrame _frame;
	private SSBrowserPlugin _plugin;
	private ServiceBrowserUI _ui;
	private List _tabs = new ArrayList();

	public SSBrowserEnvironmentImpl(JFrame frame, SSBrowserPlugin plugin,
			ServiceBrowserUI ui) {
		_frame = frame;
		_plugin = plugin;
		_ui = ui;
	}

	/**
	 * registerTab
	 * 
	 * @param aSSBrowserTab
	 * @throws sorcer.ssb.browser.api.SSBPluginException
	 */
	public void registerTab(SSBrowserTab aSSBrowserTab)
			throws SSBPluginException {
		System.out.println("Register TAB for " + _plugin.getDisplayName());
		_tabs.add(aSSBrowserTab);
		JTabbedPane tp = _ui.getTabbedPane();
		tp.addTab(aSSBrowserTab.getTabName(), new JScrollPane(aSSBrowserTab
				.getServiceTree()));
		ImageIcon icon = aSSBrowserTab.getIcon();
		if (icon == null) {
			icon = TreeRenderer._pluginIcon;
		}
		tp.setIconAt(tp.getTabCount() - 1, icon);

	}

	public void serviceDiscovered(ServiceItem serviceItem) {
		synchronized (_tabs) {
			int nt = _tabs.size();
			for (int i = 0; i < nt; i++) {
				((SSBrowserTab) _tabs.get(i)).serviceDiscovered(serviceItem);
			}
		}
	}

	public void serviceDiscarded(ServiceID serviceID) {
		synchronized (_tabs) {
			int nt = _tabs.size();
			for (int i = 0; i < nt; i++) {
				((SSBrowserTab) _tabs.get(i)).serviceDiscarded(serviceID);
			}
		}
	}

	public void serviceModified(ServiceID serviceID) {
		synchronized (_tabs) {
			int nt = _tabs.size();
			for (int i = 0; i < nt; i++) {
				((SSBrowserTab) _tabs.get(i)).serviceModified(serviceID);
			}
		}
	}

	/**
	 * setPluginMenu
	 * 
	 * @param aJMenu
	 * @throws sorcer.ssb.browser.api.SSBPluginException
	 */
	public void setPluginMenu(JMenuItem menu) throws SSBPluginException {
		_ui.pluginSetMenu(menu);
	}

	/**
	 * setRightPane
	 * 
	 * @param aJComponent
	 * @throws sorcer.ssb.browser.api.SSBPluginException
	 */
	public void setRightPane(JComponent aJComponent) throws SSBPluginException {
		_ui.setPluginRightPane(aJComponent);
	}

	/**
	 * getBrowserFrame
	 * 
	 * @return JFrame
	 */
	public JFrame getBrowserFrame() {
		return _frame;
	}

	public void showAttributes(Entry[] atts) {
		if (atts == null) {
			atts = new Entry[] {};
		}
		_ui.showProps(atts);
	}

}
