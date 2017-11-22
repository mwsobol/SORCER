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

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.util.Arrays;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.ssb.browser.api.SSBrowserFilter;

import net.jini.admin.Administrable;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.LookupDiscovery;
import net.jini.lookup.entry.Name;
import net.jini.lookup.entry.ServiceType;
import net.jini.security.ProxyPreparer;


public class LusTree {

	private JTree _tree;

	private DefaultMutableTreeNode _root;

	private ServiceRegistrar _lus;

	private LookupDiscovery _disco;

	private ServiceTemplate _template;

	private String[] _interfaceFilters;

	private String[] _nameAttributes;

	public static final String ATTS_NAME = "Lookup attributes";

	private ProxyPreparer _servicePreparer;

	private SSBrowserFilter _plugin;

	private static Logger _logger = LoggerFactory.getLogger(LusTree.class.getName());

	public LusTree(ServiceRegistrar lus, final JTree tree, ServiceTemplate tmpl,
			String[] inf, String[] names, ProxyPreparer pp,
			SSBrowserFilter plugin) throws RemoteException, Exception {

		_tree = tree;
		_lus = lus;
		_template = tmpl;
		_interfaceFilters = inf;
		_nameAttributes = names;
		_servicePreparer = pp;
		_plugin = plugin;
		/*
		 * System.out.println("_name filters"); for(int i=0;i<names.length;i++){
		 * System.out.println(names[i]); }
		 */
		// for now
		// _exporter=new JrmpExporter();
		// first build tree from existing services
		buildTree();

		final DefaultTreeModel model = (DefaultTreeModel) _tree.getModel();

		final DefaultMutableTreeNode treeRoot = (DefaultMutableTreeNode) model
				.getRoot();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				model.insertNodeInto(_root, treeRoot, treeRoot.getChildCount());
				tree.expandPath(new TreePath(_root.getPath()));
				if (tree.getSelectionPath() == null) {
					tree.setSelectionPath(new TreePath(_root.getPath()));
				}
			}
		});

		
	}

	public ServiceNode getRoot() {
		return (ServiceNode) _root.getUserObject();
	}

	private DefaultMutableTreeNode buildTree() throws Exception {

		ServiceID lusServiceId = _lus.getServiceID();
		_root = new DefaultMutableTreeNode();
		// the LUS details fir

		// add the services

		ServiceMatches sm = _lus.lookup(_template, Integer.MAX_VALUE);
		ServiceItem[] items = sm.items;
		for (int i = 0; i < items.length; i++) {
			try {
				if (items[i].service != null) {
					items[i].service = _servicePreparer
							.prepareProxy(items[i].service);

					_logger.info("service:" + items[i].service);

					ServiceNode sNode = new ServiceNode(items[i]);
					if (lusServiceId.equals(items[i].serviceID)) {
						sNode.markAsLus();
						_root.setUserObject(sNode);
						// now add the LUS as a service

						// addServiceItems(items[i],_root,new String[]{},new
						// String[]{});

					} else {
						if (_plugin != null) {
							if (!_plugin.accept(items[i])) {
								continue;
							}
						}
						if (checkName(items[i].attributeSets, _nameAttributes)
								&& matchesFilter(items[i].service.getClass())) {

							DefaultMutableTreeNode service = new DefaultMutableTreeNode(
									sNode);
							_root.add(service);

							// now add the other bits, key filter already
							// applied so make empty
							addServiceItems(items[i], service, new String[] {},
									new String[] {});
						}

					}
				}
			} catch (Exception ex) {
				System.err.println(ex);
			}
		}
		return _root;
	}

	private static boolean checkName(Entry[] atts, String[] nameAttributes) {

		for (int i = 0; i < nameAttributes.length; i++) {

			for (int j = 0; atts != null && j < atts.length; j++) {
				if (atts[j] instanceof Name) {
					Name name = (Name) atts[j];
					if (name.name.equals(nameAttributes[i])) {
						return true;
					}
				}
			}
		}
		return nameAttributes.length == 0;
	}

	private boolean matchesFilter(Class proxyClass) {

		return matchesFilterImpl(proxyClass, _interfaceFilters);
	}

	private static boolean matchesFilterImpl(Class proxyClass, String[] inf) {

		if (inf.length == 0) {
			return true;
		}

		Class clazz = proxyClass;
		while (clazz != null) {

			Class[] intf = clazz.getInterfaces();
			for (int i = 0; i < intf.length; i++) {
				Class iClazz = intf[i];
				String iName = iClazz.getName();
				for (int j = 0; j < inf.length; j++) {

					// System.out.println("Checking "+iName+" with
					// "+_interfaceFilters[j]);

					if (iName.equals(inf[j])) {
						// System.out.println("Matched");
						return true;
					}
				}
				if (matchesFilterImpl(iClazz, inf)) {
					return true;
				}

			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	private static void addInterfaces(Class proxyClass,
			DefaultMutableTreeNode node, Object proxy,
			DefaultMutableTreeNode methodsNode) {

		Class clazz = proxyClass;
		while (clazz != null) {

			Class[] intf = clazz.getInterfaces();
			for (int i = 0; i < intf.length; i++) {
				Class iClazz = intf[i];
				String iName = iClazz.getName();
				if (iName.startsWith("java") == false) {
					DefaultMutableTreeNode inode = new DefaultMutableTreeNode(
							new PropertiesNode(PropertiesNode.INTERFACE, iName));

					node.add(inode);
					addMethods(iClazz, inode, proxy);
					addMethodsToNode(iClazz, methodsNode, proxy);
					// add super interfaces

					addInterfaces(iClazz, inode, proxy, methodsNode);
				}
				
			}
			clazz = clazz.getSuperclass();
		}
	}

	private static void addMethods(Class clazz, DefaultMutableTreeNode iNode,
			Object proxy) {
		try {
			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
					"Methods");
			iNode.add(newNode);
			Method[] m = clazz.getDeclaredMethods();
			for (int i = 0; i < m.length; i++) {

				PropertiesNode pNode = new PropertiesNode(
						PropertiesNode.METHOD, m[i].getName());
				pNode.setAdditionalData(m[i]);
				pNode.setProxy(proxy);
				// String mStr=parseMethod(m[i]);
				// pNode.setAdditionalData(mStr);
				DefaultMutableTreeNode mNode = new DefaultMutableTreeNode(pNode);

				newNode.add(mNode);
			}
		} catch (Exception ex) {

		}
	}

	private static void addMethodsToNode(Class clazz,
			DefaultMutableTreeNode mNode, Object proxy) {

		try {
			Method[] m = clazz.getDeclaredMethods();
			for (int i = 0; i < m.length; i++) {

				PropertiesNode pNode = new PropertiesNode(
						PropertiesNode.METHOD, m[i].getName());
				pNode.setAdditionalData(m[i]);
				pNode.setProxy(proxy);
				// String mStr=parseMethod(m[i]);
				// pNode.setAdditionalData(mStr);
				DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
						pNode);

				mNode.add(newNode);
			}
		} catch (Exception ex) {

		}

	}

	public static String parseMethod(Method method) {
		try {
			StringBuffer buf = new StringBuffer();
			Class retClass = method.getReturnType();
			buf.append(parseClassName(retClass.getName()));

			buf.append(" ");
			buf.append(method.getName());
			buf.append("(");
			Class[] params = method.getParameterTypes();
			for (int i = 0; i < params.length; i++) {
				buf.append(parseClassName(params[i].getName()));
				if (i < params.length - 1) {
					buf.append(",");
				}
			}

			buf.append(")");
			Class[] exps = method.getExceptionTypes();
			if (exps.length > 0) {
				buf.append(" throws ");
			}
			for (int i = 0; i < exps.length; i++) {
				buf.append(parseClassName(exps[i].getName()));
				if (i < exps.length - 1) {
					buf.append(",");
				}
			}
			return buf.toString();

		} catch (Exception ex) {

		}
		return method.getName();
	}

	private static String parseClassName(String cName) {

		String tmp = cName;
		int dim = 0;
		while (tmp.startsWith("[")) {
			tmp = tmp.substring(1);
			dim++;
		}
		if (tmp.endsWith(";")) {
			tmp = tmp.substring(0, tmp.length() - 1);
		}
		int pos = tmp.lastIndexOf(".");
		if (pos != -1) {
			tmp = tmp.substring(pos + 1);
			for (int i = 0; i < dim; i++) {
				tmp += "[]";
			}
			return tmp;
		}
		return cName;
	}

    private static void addCodebase(URL[] codebase, DefaultMutableTreeNode service) {
        DefaultMutableTreeNode codebaseNode = new DefaultMutableTreeNode(new CodebaseNode("Codebase URLs"));
        for (int i = 0; i < codebase.length; i++) {
            codebaseNode.add(new DefaultMutableTreeNode(new CodebaseNode(codebase[i])));
        }
        service.add(codebaseNode);
	}

	public static boolean addServiceItems(ServiceItem si,
			DefaultMutableTreeNode service, String[] nameFilter, String[] inf) {
		// add ServiceID

		if (!checkName(si.attributeSets, nameFilter)
				|| !matchesFilterImpl(si.service.getClass(), inf)) {
			return false;
		}

		DefaultMutableTreeNode sidNode = new DefaultMutableTreeNode(
				new PropertiesNode(PropertiesNode.SERVICE_ID, "ServiceID: "
						+ si.serviceID));

        String annotation = RMIClassLoader.getClassAnnotation(si.service.getClass());
        if(annotation!=null && annotation.length()>0) {
            StringTokenizer tok = new StringTokenizer(annotation, " ");
            URL[] urls = new URL[tok.countTokens()];
            int i=0;
            while(tok.hasMoreTokens()) {
                String url = tok.nextToken();
                try {
                    urls[i] = new URL(url);
                } catch (MalformedURLException e) {
                    _logger.warn(" Unable to create URL for ["+url+"]", e);
                }
                i++;
            }
            addCodebase(urls, service);
        }

        ClassLoader cl = si.service.getClass().getClassLoader();
        /*if (cl instanceof URLClassLoader) {
			addCodebase(((URLClassLoader) cl).getURLs(), service);
		}*/

		// add the methods at the top level
		DefaultMutableTreeNode methodsNode = new DefaultMutableTreeNode("Methods");

		service.add(sidNode);

		// addInterfaces
		DefaultMutableTreeNode inode = new DefaultMutableTreeNode("Interfaces");
		service.add(inode);
		addInterfaces(si.service.getClass(), inode, si.service, methodsNode);
		Object proxy = si.service;
		if (proxy instanceof Administrable) {
			Administrable admin = (Administrable) proxy;
			try {
				Object adminProxy = admin.getAdmin();
				if (adminProxy != null) {
					DefaultMutableTreeNode anode = new DefaultMutableTreeNode(
							"Admin interfaces");
					service.add(anode);
					addInterfaces(adminProxy.getClass(), anode, adminProxy,
							methodsNode);
				}
			} catch (Exception ex) {
				System.err.println(ex);
			}
		}
		addAttributes(service, si, cl);
		service.add(methodsNode);
		return true;
	}

	public static DefaultMutableTreeNode addAttributes(
			DefaultMutableTreeNode service, ServiceItem si) {
		return addAttributes(service, si, LusTree.class.getClassLoader());
	}

	public static DefaultMutableTreeNode addAttributes(
			DefaultMutableTreeNode service, ServiceItem si, ClassLoader cl) {
		// add attributes
		DefaultMutableTreeNode attsNode = new DefaultMutableTreeNode(ATTS_NAME);
		service.add(attsNode);
		Entry[] atts = si.attributeSets;
		for (int i = 0; i < atts.length; i++) {
			if (atts[i] == null) {
				System.err.println("Null attribute detected for " + service);
				continue;
			}
			if (atts[i] instanceof ServiceType) {
				ServiceType st = (ServiceType) atts[i];
				try {

					java.awt.Image image = st
							.getIcon(java.beans.BeanInfo.ICON_COLOR_16x16);
					if (image==null && cl!=null && st instanceof SorcerServiceInfo) {
						SorcerServiceInfo ssi = (SorcerServiceInfo) atts[i];
						String iconName = ssi.iconName;
						if (iconName!=null) {
							java.net.URL url = cl.getResource(iconName);
							if (url!=null) {
								image = Toolkit.getDefaultToolkit().getImage(url);
							}
						}
					}
					if (image != null) {
						image = image.getScaledInstance(16, 16, 0);
						TreeRenderer
								.addIcon(si.serviceID, new ImageIcon(image));

					}
				} catch (Throwable t) {
					System.out.println("Exception loading service icon ");
				}
			}
			Class clazz = atts[i].getClass();
			PropertiesNode node = new PropertiesNode(
					PropertiesNode.ENTRY_CLASS, clazz.getName());

			DefaultMutableTreeNode attsClassNode = new DefaultMutableTreeNode(
					node);

			attsNode.add(attsClassNode);
			// add fields
			Field[] f = clazz.getFields();
			for (int j = 0; j < f.length; j++) {
				PropertiesNode fnode = null;
				try {
					String value = f[j].getName() + "=" + f[j].get(atts[i]);
					fnode = new PropertiesNode(PropertiesNode.ENTRY_FIELD,
							value);

				} catch (Exception ex) {
					fnode = new PropertiesNode(PropertiesNode.ENTRY_FIELD, f[j]
							.getName()
							+ "=?");
				}
				DefaultMutableTreeNode fieldNode = new DefaultMutableTreeNode(
						fnode);
				attsClassNode.add(fieldNode);

			}
		}
		return attsNode;
	}

	private void removeFromTree(ServiceID sid) {
		int nServices = _root.getChildCount();
		// System.out.println("removeFromTree() checking "+nServices+"
		// services");

		for (int i = 0; i < nServices; i++) {
			// System.out.println("Checking "+i);

			DefaultMutableTreeNode service = (DefaultMutableTreeNode) _root
					.getChildAt(i);
			ServiceNode sNode = (ServiceNode) service.getUserObject();
			// System.out.println("Checking "+sNode);
			if (sNode.sameServiceID(sid)) {
				DefaultTreeModel model = (DefaultTreeModel) _tree.getModel();
				model.removeNodeFromParent(service);
				return;
			}

		}
	}

}
