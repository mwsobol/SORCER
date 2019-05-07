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

import net.jini.core.lookup.ServiceRegistrar;
import net.jini.lookup.entry.ServiceType;
import sorcer.jini.lookup.entry.SorcerServiceInfo;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.beans.BeanInfo;
import java.util.HashMap;
import java.util.Map;

public class TreeRenderer extends DefaultTreeCellRenderer {

	public static ImageIcon _lusIcon;
	public static ImageIcon _lusIcon2;
	public static ImageIcon _serviceIcon;
    public static ImageIcon _sorcerServiceIcon;
	private static ImageIcon _attsIcon;
	public static ImageIcon _sidIcon;
	private static ImageIcon _fieldIcon;
	public static ImageIcon _frameIcon;
	public static ImageIcon _hostIcon;
	public static ImageIcon _startedIcon;
	public static ImageIcon _stoppedIcon;
	public static ImageIcon _spaceIcon;
	public static ImageIcon _jarIcon;
	public static ImageIcon _jarCpIcon;
	public static ImageIcon _luAttsIcon;
	public static ImageIcon _serviceUIIcon;
	public static ImageIcon _pluginIcon;
	public static ImageIcon _fwdIcon;
	public static ImageIcon _backIcon;
	public static ImageIcon _homeIcon;

	// public static ImageIcon _lusGlyphIcon;
	public static ImageIcon _serviceGlyphIcon;
	private static Map<Object, ImageIcon> _iconLookup = new HashMap<>();
	private static Class[] CORE_SERVICES = {
			net.jini.core.transaction.server.TransactionManager.class,
			net.jini.event.EventMailbox.class,
			net.jini.lease.LeaseRenewalService.class,
			net.jini.space.JavaSpace.class,
			net.jini.discovery.LookupDiscoveryService.class,
			net.jini.core.lookup.ServiceRegistrar.class };

	private boolean _initialized;

	public TreeRenderer() {
		init();
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

		JLabel lab = (JLabel) super.getTreeCellRendererComponent(tree, value,
				sel, expanded, leaf, row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		Object userObject = node.getUserObject();
		if (userObject instanceof PropertiesNode) {
			PropertiesNode treeNode = (PropertiesNode) userObject;

			setIconForProps(lab, treeNode);

		} else if (userObject instanceof CodebaseNode) {
			CodebaseNode cbn = (CodebaseNode) userObject;
			if (cbn.isClasspathJar()) {
				lab.setIcon(_jarCpIcon);
			} else {
				lab.setIcon(_jarIcon);
			}
		} else if (node.getParent() == null) {
			lab.setIcon(_hostIcon);
		} else if (userObject instanceof ServiceNode) {

			ServiceNode sNode = (ServiceNode) userObject;
			if (sNode.isLus()) {
				lab.setIcon(_lusIcon);
			} else if (sNode.getProxy() instanceof ServiceRegistrar) {
				lab.setIcon(_lusIcon);
			} else {
				ImageIcon icon = getIcon(sNode);

				lab.setIcon(icon);

				Object proxy = sNode.getProxy();
				lab.setText(getJiniName(proxy, lab.getText()));
			}
		} else if (value.toString().equals("Lookup attributes")) {
			lab.setIcon(_luAttsIcon);
		} else if (value.toString().equals("Methods")) {
			lab.setIcon(_spaceIcon);
		} else {
			lab.setIcon(_sidIcon);
		}
		return lab;
	}

	public static void setIconForProps(JLabel lab, PropertiesNode treeNode) {
		int type = treeNode.getType();
		switch (type) {
		case PropertiesNode.ENTRY_CLASS:
		case PropertiesNode.INTERFACE:
			lab.setIcon(_attsIcon);
			break;
		case PropertiesNode.METHOD:
		case PropertiesNode.ENTRY_FIELD:
			lab.setIcon(_fieldIcon);
			break;
		case PropertiesNode.SERVICE_ID:
			lab.setIcon(_sidIcon);
			break;

		}
	}

	public static String getJiniName(Object proxy, String defaultName) {

		// System.out.println("defaultName="+defaultName);
		if (!defaultName.equals(proxy.getClass().getName())) {
			return defaultName;
		}
		for (int i = 0; i < CORE_SERVICES.length; i++) {

			if (CORE_SERVICES[i].isAssignableFrom(proxy.getClass())) {
				String tidyText = CORE_SERVICES[i].getName();
				int pos = tidyText.lastIndexOf(".");
				if (pos != -1) {
					tidyText = tidyText.substring(pos + 1);
				}
				// if(defaultName.equals(tidyText)){
				return tidyText;
				// }
				// System.out.println("returning "+(tidyText+" "+defaultName));
				// return tidyText+" "+defaultName;
			}

		}

		return defaultName;
	}

	void init() {
		if (_initialized) {
			return;
		}
		_initialized = true; // well true-ish

		try {
			ClassLoader cl = getClass().getClassLoader();

			_lusIcon = new ImageIcon(cl.getResource("rt-images/lus.png"));
			// _lusIcon2 = new ImageIcon(cl.getResource("rt-images/lus2.png"));
			_attsIcon = new ImageIcon(cl.getResource("rt-images/atts.png"));
			_serviceIcon = new ImageIcon(cl.getResource("rt-images/service.png"));
            _sorcerServiceIcon = new ImageIcon(cl.getResource("rt-images/service-rbg.png"));
			_fieldIcon = new ImageIcon(cl.getResource("rt-images/field.png"));
			_sidIcon = new ImageIcon(cl.getResource("rt-images/sid.png"));
			_frameIcon = new ImageIcon(cl.getResource("rt-images/incax.gif"));
			_hostIcon = new ImageIcon(cl.getResource("rt-images/host.png"));
			_startedIcon = new ImageIcon(cl
					.getResource("rt-images/started.png"));
			_stoppedIcon = new ImageIcon(cl
					.getResource("rt-images/stopped.png"));
			_spaceIcon = new ImageIcon(cl
					.getResource("rt-images/space_browser.png"));
			_jarIcon = new ImageIcon(cl.getResource("rt-images/jar.png"));
			_jarCpIcon = new ImageIcon(cl.getResource("rt-images/jar_cp.png"));
			_luAttsIcon = new ImageIcon(cl.getResource("rt-images/lu_atts.png"));
			_serviceUIIcon = new ImageIcon(cl
					.getResource("rt-images/serviceui.png"));
			_pluginIcon = new ImageIcon(cl.getResource("rt-images/plugin.png"));
			_frameIcon = _hostIcon;
			_fwdIcon = new ImageIcon(cl.getResource("rt-images/fwd.png"));// UPDATED
			_backIcon = new ImageIcon(cl.getResource("rt-images/back.png"));// UPDATED
			_homeIcon = new ImageIcon(cl.getResource("rt-images/home.png"));// UPDATE

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	static ImageIcon getIcon(ServiceNode node) {
		Object serviceID = node.getServiceItem().serviceID;
		ImageIcon ii = _iconLookup.get(serviceID);
		if (ii == null) {
            for(net.jini.core.entry.Entry entry : node.getServiceItem().attributeSets) {
                if(entry instanceof ServiceType) {
                    if(entry instanceof SorcerServiceInfo) {
                        SorcerServiceInfo ssi = (SorcerServiceInfo) entry;
                        if (ssi.iconName != null && ssi.iconName.contains("sorcer")) {
                            return _sorcerServiceIcon;
                        }
                    } else {
                        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
                        try {
                            Thread.currentThread().setContextClassLoader(node.getServiceItem().service.getClass().getClassLoader());
                            Image image = ((ServiceType) entry).getIcon(BeanInfo.ICON_COLOR_16x16);
                            if (image != null) {
                                ii = new ImageIcon(image);
                                _iconLookup.put(serviceID, ii);
                                return ii;
                            }
                        } finally {
                            Thread.currentThread().setContextClassLoader(currentCl);
                        }
                    }
                }
            }
            return _serviceIcon;
		}
		return ii;
		/*
		 * if(ii!=null){ return ii; } Entry [] atts=node.getLookupAttributes();
		 * for(int i=0;i<atts.length;i++){
		 * 
		 * if(atts[i] instanceof Multitype){
		 * 
		 * Multitype st=(Multitype)atts[i];
		 * 
		 * try{ java.awt.Image
		 * icon=st.getIcon(java.beans.BeanInfo.ICON_COLOR_16x16);
		 * if(icon!=null){ //check icon size
		 * icon=icon.getScaledInstance(16,16,0); ii=new ImageIcon(icon);
		 * _iconLookup.put(serviceID,ii); return ii; } }catch(Throwable ex){
		 * //ex.printStackTrace(); } } } return _serviceIcon;
		 */
	}

	public static void addIcon(Object sid, ImageIcon icon) {
		_iconLookup.put(sid, icon);
	}
}
