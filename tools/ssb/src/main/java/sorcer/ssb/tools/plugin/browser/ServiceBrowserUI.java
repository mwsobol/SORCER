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

import com.sun.jini.config.Config;
import com.sun.jini.proxy.BasicProxyTrustVerifier;
import groovy.ui.Console;
import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.constraint.MethodConstraints;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.*;
import net.jini.discovery.*;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.jrmp.JrmpExporter;
import net.jini.lease.LeaseListener;
import net.jini.lease.LeaseRenewalEvent;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.factory.JComponentFactory;
import net.jini.lookup.ui.factory.JDialogFactory;
import net.jini.lookup.ui.factory.JFrameFactory;
import net.jini.lookup.ui.factory.JWindowFactory;
import net.jini.security.*;
import net.jini.security.proxytrust.ServerProxyTrust;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.serviceui.UIFrameFactory;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ServiceBrowserUI extends Thread implements RemoteEventListener,
		ServerProxyTrust {// , Remote{

	public final static String ALL_GROUPS = "ALL_GROUPS";
		
	public final static Logger _logger = LoggerFactory.getLogger("sorcer.ui.tools");

	private PluginRegistry _pluginReg;

	private JPanel _ui = new JPanel();

	private JPanel _serviceUIPanel = new JPanel();

	private JComponent _helpView;

	private JComponent _pluginHelpView;

	private JSplitPane _splitter;

	private JMenu _pluginMenu = new JMenu("Plugins");

	private JTree _tree;

	private DefaultMutableTreeNode _root;

	private JTree _tree2;

	private DefaultMutableTreeNode _root2;

	private JTree _selectedTree;

	private FiltersView _filtersView = new FiltersView();

	private Glyph _lusGlyph; // currently selected LUS

	private GlyphView _glyphView;

	private ServiceRegistrar _lus;

	private DiscoveryManagement _disco;

	private RemoteEventListener _listener;

	private DefaultMutableTreeNode _selectedLus;

	private DefaultMutableTreeNode _currentRoot;

	private static int TRANSITIONS = ServiceRegistrar.TRANSITION_NOMATCH_MATCH
			| ServiceRegistrar.TRANSITION_MATCH_NOMATCH
			| ServiceRegistrar.TRANSITION_MATCH_MATCH;

	private NotifyImpl _notifyImpl;

	private ServiceTemplate _template = new ServiceTemplate(null, null, null);

	private transient ProxyPreparer _adminPreparer = new BasicProxyPreparer();

	private transient DefaultMutableTreeNode _selectedNode;

	private transient DefaultMutableTreeNode _rightPaneNode;

	private transient DefaultMutableTreeNode _serviceSelectedNode;

	private transient DefaultMutableTreeNode _lusSelectedNode;

	// private static ServiceBrowserUI _instance;

	//private AttsPropPanel _propsPane;

	private JComponent _leftPane;

	private JTabbedPane tp = new JTabbedPane();

	private SSBLookAndFeelAction _ixLandF = new SSBLookAndFeelAction();

	private PlafLookAndFeelAction _plafLandF;

	private int MAX_EVENT_STORE = 100;

	private EventTableView _eventView = new EventTableView(MAX_EVENT_STORE);

	private JFrame _frame;

	public static ArrayList _windows = new ArrayList();
	
	private JMenu windowMenu;

	private static final int XINC = 30;

	private static final int YINC = 50;

	private static int winCounter = 1;

	private OnCascadeAction _cascadeAction = new OnCascadeAction();

	private OnTileAction _tileAction = new OnTileAction();

	// private Lease _lease;
	private LeaseRenewalManager _lrm;

	private Exporter _exporter = new JrmpExporter();

	public static String TITLE_TAG = " - SORCER";

	private static LogFileView _logView;

	private boolean _inDiscoveredImpl;

	private Object _discoveryLock = new Object();

	private static MulticastView _multicastView;

	// added for Bantum public LUS over http
	private DiscoveryManagement _discoMgr;
	
	private String[] _initialLookupGroups;
	private boolean allowDestroy = true;
	private LookupLocator[] _initialLookupLocators;
	
	// security stuff
	// private transient LeaseRenewalManager _leaseMgr;
	private transient ProxyPreparer _leasePreparer = new BasicProxyPreparer();

	private transient ProxyPreparer _servicePreparer = new BasicProxyPreparer();

	// private transient ProxyPreparer _adminPreparer;
	private transient MethodConstraints _locatorConstraints;

	public static Configuration _config;

	private static SecurityContext _ctx;

	private static ClassLoader _ccl;

	public static boolean LOGGED_IN;

	public final static String CONFIG_MODULE = "sorcer.sbb.browser.SorcerServiceBrowser";

	// public static ServiceBrowserUI getInstance(){
	// return _instance;
	// }

	// BRANDING
	public static File BRANDED_HELP;

	public static boolean IS_BRANDED;

	public TrustVerifier getProxyVerifier() {
		return new BasicProxyTrustVerifier(_listener);
	}

	public static ArrayList getWindows() {
		return _windows;
	}

	public ServiceBrowserUI(BrowserFrame frame, FiltersView fv)
			throws RemoteException, Exception {
		_filtersView = fv;
		init(frame);
	}

	public ServiceBrowserUI(BrowserFrame frame) throws RemoteException,
			Exception {
		init(frame);
	}

	private void initWithConfig() {
		// hack to try and reuse the initial contexts in the Multicast View
		if (_ctx == null) {
			_ctx = Security.getContext();
			_ccl = Thread.currentThread().getContextClassLoader();
		}

		System.out.println("Intialized with SSB configuration file: " + _config);
		try {
			_discoMgr = (DiscoveryManagement) _config.getEntry(CONFIG_MODULE,
					"discoveryManager", DiscoveryManagement.class);
		} catch (ConfigurationException e) {
			//e.printStackTrace();
			_discoMgr = null;
		}

		try {
			_initialLookupGroups = (String[]) _config.getEntry(CONFIG_MODULE,
					"initialLookupGroups", String[].class);
		} catch (ConfigurationException e) {
			//e.printStackTrace();
			_initialLookupGroups = LookupDiscovery.ALL_GROUPS;
			//_initialLookupGroups =  new String[] {ALL_GROUPS};
		}
		
		System.out.println("initial lookup groups: "
				+ Arrays.toString(_initialLookupGroups));

		try {
			_initialLookupLocators = (LookupLocator[]) _config.getEntry(
			        CONFIG_MODULE,
			        "initialLookupLocators",
			        LookupLocator[].class,
			        new LookupLocator[0]);
		} catch (ConfigurationException e) { 
			e.printStackTrace();
		}
		System.out.println("initial lookup locators: "
				+ Arrays.toString(_initialLookupLocators));

		Boolean canDestroy = true;
		try {
			canDestroy = (Boolean) Config.getNonNullEntry(_config,
					CONFIG_MODULE, "allowDestroy", Boolean.class, new Boolean(
							true));
		} catch (ConfigurationException e) {
		}
		allowDestroy = canDestroy.booleanValue();
		AdminView.DESTROYABLE = allowDestroy;

		System.out.println("DiscoveryManagement=" + _discoMgr);
		System.out.println("allowDestroy=" + allowDestroy);
		System.out.println("security context=" + _ctx);

		try {
			_lrm = (LeaseRenewalManager) Config.getNonNullEntry(_config,
					CONFIG_MODULE, "leaseManager", LeaseRenewalManager.class,
					new LeaseRenewalManager(_config));
		} catch (ConfigurationException e) {
		}
		;

		try {
			_leasePreparer = (ProxyPreparer) Config.getNonNullEntry(_config,
					CONFIG_MODULE, "leasePreparer", ProxyPreparer.class,
					new BasicProxyPreparer());
		} catch (ConfigurationException e) {
		}
		;

		try {
			_servicePreparer = (ProxyPreparer) Config.getNonNullEntry(_config,
					CONFIG_MODULE, "servicePreparer", ProxyPreparer.class,
					new BasicProxyPreparer());
		} catch (ConfigurationException e) {
		}
		;

		try {
			_adminPreparer = (ProxyPreparer) Config.getNonNullEntry(_config,
					CONFIG_MODULE, "adminPreparer", ProxyPreparer.class,
					new BasicProxyPreparer());
		} catch (ConfigurationException e) {
		}
		;

		try {
			_locatorConstraints = (MethodConstraints) _config.getEntry(
					CONFIG_MODULE, "locatorConstraints",
					MethodConstraints.class, null);
		} catch (ConfigurationException e) {
			_locatorConstraints = null;
		}
		;

		try {
			_exporter = (Exporter) Config.getNonNullEntry(_config,
					CONFIG_MODULE, "listenerExporter", Exporter.class,
					new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
							new BasicILFactory()));
		} catch (ConfigurationException e) {
		}
		;
		if (_exporter instanceof BasicJeriExporter)
			System.out.println("exporter endpoint: "
					+ ((BasicJeriExporter) _exporter).getServerEndpoint());
	}

	private void init(final BrowserFrame frame) throws RemoteException,
			Exception {

		if (_config != null) {
			initWithConfig();
		} else {
			_exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
					new BasicILFactory());
		}

		frame.setFiltersView(_filtersView);
		_frame = frame;

		// if(_instance==null){
		// _instance=this;
		// }
		_windows.add(_frame);
		if (frame.isDefault() == false) {
			_frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					_windows.remove(_frame);
					terminate();

					_frame.dispose();

				}

			});
		}

		// first build tree from existing services
		_root = new DefaultMutableTreeNode("Lookup services");
		_tree = new JTree(_root);
		_lusSelectedNode = _root;
		_currentRoot = _root;
		// _tree.setRootVisible(false);

		_root2 = new DefaultMutableTreeNode("Services");
		_tree2 = new JTree(_root2);
		_serviceSelectedNode = _root2;
		// _tree2.setRootVisible(false);

		// _filtersView.load();
		// CodeServer.autoStart(getClass());

		_notifyImpl = new NotifyImpl(this, this);

		_listener = (RemoteEventListener) _exporter.export(_notifyImpl);

		URL help = getClass().getClassLoader().getResource("html/index.html");
		if (BRANDED_HELP != null) {
			help = BRANDED_HELP.toURL();
		}
		/*
		 * final JEditorPane helpView=new JEditorPane(help);//new
		 * HTMLView(help); helpView.setEditable(false);
		 * helpView.addHyperlinkListener(new HyperlinkListener(){ public void
		 * hyperlinkUpdate(HyperlinkEvent e) { System.out.println(e); if
		 * (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
		 * 
		 * if (e instanceof HTMLFrameHyperlinkEvent) { HTMLFrameHyperlinkEvent
		 * evt = (HTMLFrameHyperlinkEvent)e; HTMLDocument doc =
		 * (HTMLDocument)helpView.getDocument();
		 * doc.processHTMLFrameHyperlinkEvent(evt); } } } }); _helpView=new
		 * JScrollPane(helpView);
		 */
		_helpView = new HTMLView(help);
		// _helpView.setPreferredSize( new Dimension(300,300));
		help = getClass().getClassLoader().getResource("html/plugin-dev.html");
		JEditorPane pluginHelpView = new JEditorPane(help);// new
		// HTMLView(help);
		pluginHelpView.setEditable(false);
		_pluginHelpView = new JScrollPane(pluginHelpView);

		Border emptyBorder = BorderFactory.createEmptyBorder();
		_serviceUIPanel.setLayout(new BorderLayout());
		_serviceUIPanel.setBorder(emptyBorder);
		
		_serviceUIPanel.setPreferredSize(new Dimension(300, 300));
		_serviceUIPanel.add(_helpView, BorderLayout.CENTER);

		TreeRenderer renderer = new TreeRenderer();
		_tree.setCellRenderer(renderer);
		_tree2.setCellRenderer(renderer);

		_ui.setLayout(new BorderLayout());

		// EE version
		// _tree.setMinimumSize(new Dimension(300,200));
		// _tree.setPreferredSize(new Dimension(300,200));

		TreeSelectionListener tsl = new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent evt) {

				JTree theTree = (JTree) evt.getSource();
				_selectedTree = theTree;

				TreePath tPath = theTree.getSelectionPath();// evt.getNewLeadSelectionPath();
				if (tPath == null)
					return;

				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tPath
						.getLastPathComponent();
				Object userObject = selectedNode.getUserObject();
				if (theTree == _tree2) {
					// System.out.println("_serviceSelectedNode="+selectedNode);
					_serviceSelectedNode = selectedNode;
				} else {
					// System.out.println("_lusSelectedNode="+selectedNode);
					_lusSelectedNode = selectedNode;
				}
				// if(selectedNode == _root){
				// return;
				// }

				try {

					_selectedNode = selectedNode;
					// _tree.setSelectionPath(tPath);
					showRightPane(selectedNode);
					// showProps(userObject);

				} catch (Exception ex) {
					showError(ex);
				}
			}
		};
		_tree.addTreeSelectionListener(tsl);
		_tree2.addTreeSelectionListener(tsl);

		JScrollPane sp = new JScrollPane(_tree);
		Dimension fd = frame.getSize();

		// System.out.println(fd);

		sp.setMinimumSize(new Dimension(150, 200));
		// sp.setPreferredSize(new Dimension(150,(int)(fd.height*.75)));

		// System.out.println("sp="+sp.getPreferredSize());

		//_propsPane = new AttsPropPanel(new Entry[] {});
		// _propsPane.setSize(new Dimension(150,(int)(fd.height*25)));

		tp.add("Lookup services", sp);
		tp.setIconAt(0, TreeRenderer._lusIcon);
		tp.add("Services", new JScrollPane(_tree2));
		tp.setIconAt(1, TreeRenderer._serviceIcon);

		tp.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				if (tp.getSelectedIndex() == 1) {
					_currentRoot = _root2;
					showRightPane(_serviceSelectedNode);
				} else {
					_currentRoot = _root;
					showRightPane(_lusSelectedNode);
				}
			}
		});

		//_leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tp, _propsPane);

		_leftPane = tp;
		
		_leftPane.setPreferredSize(new Dimension(300, 400));
		_leftPane.setBorder(emptyBorder);
		
		_splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _leftPane,
				_serviceUIPanel);
		
		//_splitter.setOneTouchExpandable(true);
		//_splitter.setDividerSize(2);
		_splitter.setOneTouchExpandable(true);
		_ui.add(_splitter, BorderLayout.CENTER);

		frame.setJMenuBar(createMenus());

		// _ui.add( new JScrollPane(_tree) ,BorderLayout.CENTER);
		// _disco=new LookupDiscovery(LookupDiscovery.ALL_GROUPS);
		// _disco.addDiscoveryListener(new Listener());
		
		if (!SorcerServiceBrowser.EXPIRED) {
			updateFilters();
			start();
		}
		_pluginReg = new PluginRegistry(_frame, this);
		_pluginReg.init(new File(ServiceBrowserConfig.BROWSER_HOME + "/plugins"));

	}

	Runnable wrap(Runnable r) {
		// only do this if we are logged ibn
		if (!LOGGED_IN) {
			return r;
		}
		return (Runnable) wrap((Object) r, Runnable.class);
	}

	private Object wrap(Object obj, Class iface) {
		return Proxy.newProxyInstance(obj.getClass().getClassLoader(),
				new Class[] { iface }, new Handler(obj));
	}

	private class Handler implements InvocationHandler {
		private final Object obj;

		Handler(Object obj) {
			this.obj = obj;
		}

		public Object invoke(Object proxy, final Method method,
				final Object[] args) throws Throwable {
			if (method.getDeclaringClass() == Object.class) {
				if ("equals".equals(method.getName()))
					return Boolean.valueOf(proxy == args[0]);
				else if ("hashCode".equals(method.getName()))
					return new Integer(System.identityHashCode(proxy));
			}
			try {
				return AccessController.doPrivileged(_ctx
						.wrap(new PrivilegedExceptionAction() {
							public Object run() throws Exception {
								Thread t = Thread.currentThread();
								ClassLoader occl = t.getContextClassLoader();
								try {
									t.setContextClassLoader(_ccl);
									try {
										return method.invoke(obj, args);
									} catch (InvocationTargetException e) {
										Throwable tt = e.getCause();
										if (tt instanceof Error)
											throw (Error) tt;
										throw (Exception) tt;
									}
								} finally {
									t.setContextClassLoader(occl);
								}
							}
						}), _ctx.getAccessControlContext());
			} catch (PrivilegedActionException e) {
				throw e.getCause();
			}
		}
	}

	public void iconDoubleClick(ServiceItem si) {
		DefaultMutableTreeNode node = getNode(si.serviceID);
		if (node != null) {
			_tree.setSelectionPath(new TreePath(node.getPath()));
		}
	}

	public void showProps(Entry[] atts) {// Object userObject){
		// if(!(userObject instanceof ServiceNode)){
		// return;
		// }
		try {
			// ServiceNode sn=(ServiceNode)userObject;
			// Entry [] atts=sn.getLookupAttributes();
			// _logger.info(""+atts);
			//_propsPane.update(atts);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void updateFilters() {
		Thread t = new Thread(wrap(new Runnable() {
			public void run() {
				updateFiltersImpl();
			}
		}));
		t.start();
	}

	private synchronized void updateFiltersImpl() {
		// 1: terminate discovery
		// 2: need to cancel all event registrations
		if (_disco != null) {
			// will be null on startup
			_logger.info("Terminating discovery");
			_disco.terminate();
		}
		cancelLeases();
		synchronized (_discoveryLock) {
			while (_inDiscoveredImpl) {
				try {
					_logger
							.error("#### updateFilters: discovery in process: waiting...");
					_discoveryLock.wait();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		int nLus = _root.getChildCount();
		// remove all kids
		DefaultTreeModel model = (DefaultTreeModel) _tree.getModel();
		for (int i = 0; i < nLus; i++) {
			model.removeNodeFromParent((DefaultMutableTreeNode) _root
					.getChildAt(0));
		}
		DefaultTreeModel model2 = (DefaultTreeModel) _tree2.getModel();
		int snum = _root2.getChildCount();
		for (int i = 0; i < snum; i++) {
			model2.removeNodeFromParent((DefaultMutableTreeNode) _root2
					.getChildAt(0));
		}
		// next setValue the filters properly
		String[] grps = _filtersView.getGroups();
		grps = getAllGroups(grps);
		
		String[] flocs = _filtersView.getLookupLocators();
		final LookupLocator[] locs = getAllLocators(flocs);
		
		if (locs.length > 0 && _discoMgr == null) {
			_logger.info("Unicast discovery overrides Group filters if no 'discoveryManager' available");
			_logger.info("Locators for unicast discovery: " + Arrays.toString(locs));
			for (int i = 0; i < locs.length; i++) {
				final int index = i;
				Thread lloc = new Thread() {
					public void run() {
						try {
							LookupLocator loc = locs[index];
							ServiceRegistrar reggie = loc.getRegistrar();
							dicoveredImpl(new ServiceRegistrar[] { reggie });

						} catch (Exception ex) {
							_logger.error("Failed discovery for: "
									+ locs[index] + " " + ex.getMessage());
							return;
						}
					}
				};
				lloc.start();
			}
		} else {
			try {
				_logger.info("Starting new LookupDiscoveryManager");
				if (_discoMgr != null) {
					_logger.info("Locators for multicast discovery: " + Arrays.toString(locs));
					_logger.info("with groups for lookup discovery: " + Arrays.toString(grps));
					_disco = _discoMgr;
					if (_discoMgr instanceof DiscoveryLocatorManagement)
						((DiscoveryLocatorManagement)_disco).setLocators(locs);
					if (_discoMgr instanceof DiscoveryGroupManagement) {
						if (grps != null)
							((DiscoveryGroupManagement)_disco).addGroups(grps);
						else
							((DiscoveryGroupManagement)_disco).setGroups(LookupDiscovery.ALL_GROUPS);
					}
				} else {
					_logger.info("Starting new LookupDiscovery");
					_logger.info("Groups for lookup discovery: " + Arrays.toString(grps));
					if (grps.length == 1 && grps[0].equals(ALL_GROUPS))
						grps = LookupDiscovery.ALL_GROUPS;
					_disco = new LookupDiscovery(grps);
				}
				_disco.addDiscoveryListener(new Listener());
			} catch (Exception ex) {
				ex.printStackTrace();
				return;
			}
		}
	}

	private String[] getAllGroups(String[] grps) {
		String[] grpsToUse = new String[0];
		if (_initialLookupGroups != null)
			grpsToUse = _initialLookupGroups;
		if (grps.length > 0) {
			List<String> allGroups = new ArrayList<String>();
			if (_initialLookupGroups != null) {
				for (String s : _initialLookupGroups)
					allGroups.add(s);
			}
			for (String s : grps)
				allGroups.add(s);
			for (String s : allGroups) {
				// _logger.info("GROUP " + s);
				if (s.equals("PUBLIC")) {
					allGroups.set(allGroups.indexOf(s), "");
				}
			}
			if (allGroups.size() > 1 && allGroups.contains(ALL_GROUPS)) 
				allGroups.remove(ALL_GROUPS);

			grpsToUse = new String[allGroups.size()];
			allGroups.toArray(grpsToUse);
		}
		System.out.println("all lookup discovery groups: "
				+ Arrays.toString(grpsToUse));
		return grpsToUse;
	}
	
	private LookupLocator[] getAllLocators(String[] locs) {
		List<LookupLocator> locList = null;
		LookupLocator[] allLocs = new LookupLocator[0];
		if (_initialLookupLocators.length > 0) {
			allLocs = _initialLookupLocators;
			if (locs.length > 0) {
				locList = Arrays.asList(_initialLookupLocators);
				for (int i = 0; i < locs.length; i++) {
					try {
						locList.add(new LookupLocator(locs[i]));
					} catch (MalformedURLException ex) {
						_logger.error("Malformed URL for: " + locs[i] + " "
								+ ex.getMessage());
					}
				}
				allLocs = (LookupLocator[]) locList.toArray();
			}
		}
		System.out.println("all lookup locators: " + Arrays.toString(allLocs));
		return allLocs;
	}
	
	public void run() {
		while (true) {
			try {
				sleep(5000);
				refresh();
				/*
				 * SwingUtilities.invokeLater(wrap(new Runnable() { public void
				 * run() { refresh();
				 * 
				 * } }));
				 */
			} catch (Throwable ex) {

			}
		}
	}

	private JMenuBar createMenus() {
		final JComponent view = _ui;

		JMenuBar mb = new JMenuBar();
		JMenu file = new JMenu("File");

		JMenuItem fileNew = new JMenuItem("New browser window...");
		fileNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				try {
					Object name = JOptionPane.showInputDialog(_frame,
							"Window title", "New Window",
							JOptionPane.PLAIN_MESSAGE, null, null, "untitled "
									+ winCounter);
					if (name == null) {
						return;
					}
					winCounter++;
					BrowserFrame f = new BrowserFrame(name.toString()
							+ TITLE_TAG);// ServiceBrowser.TITLE);
					f.setIconImage(TreeRenderer._frameIcon.getImage());
					ServiceBrowserUI browser = new ServiceBrowserUI(f);
					f.setBrowser(browser);
					f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					f.getContentPane()
							.add(browser.getUI(), BorderLayout.CENTER);
					Rectangle bounds = _frame.getBounds();
					bounds.x += 25;
					bounds.y += 25;
					f.setBounds(bounds);

					f.setVisible(true);
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(_frame, ex, "SORCER",
							JOptionPane.INFORMATION_MESSAGE);
				}

			}
		});
		final JMenuItem fileClose = new JMenuItem("Close");
		fileClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				_windows.remove(_frame);
				SorcerServiceBrowser.saveSettings();
				_frame.dispose();
				terminate();
				if (_windows.size() == 0) {

					System.exit(0);
				}
			}
		});

		fileClose.setEnabled(!_frame.getTitle().equals(SorcerServiceBrowser.TITLE));

		JMenuItem fileExit = new JMenuItem("Quit");
		fileExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				SorcerServiceBrowser.saveSettings();
				// terminate now in shutdown hook
				// terminate();
				System.exit(0);
			}
		});

		JMenu options = new JMenu("Options");
		JMenuItem filters = new JMenuItem("Edit filters...");
		filters.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showFilters();
			}
		});
		JMenuItem reset = new JMenuItem("Reset filters...");
		reset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				_filtersView.setDefaultText();
				showFilters();
			}
		});
		JMenuItem refresh = new JMenuItem("Refresh");
		refresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

				showProps(new Entry[] {});
				showRightPaneImpl(null);
				_selectedNode = null;
				_serviceSelectedNode = _root2;
				_lusSelectedNode = _root;
				updateFilters();
			}
		});
		JMenuItem addLoc = new JMenuItem("Unicast discovery...");
		addLoc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

				final String input = JOptionPane.showInputDialog(view,
						"jini://<host>:<port>", "Add Lookup Locator",
						JOptionPane.QUESTION_MESSAGE);
				if (input != null) {
					Thread t = new Thread() {
						public void run() {

							try {
								LookupLocator loc = new LookupLocator(input);
								ServiceRegistrar reggie = loc.getRegistrar();
								DefaultMutableTreeNode lusNode = getLUSNode(reggie
										.getServiceID());

								if (lusNode == null) {
									dicoveredImpl(new ServiceRegistrar[] { reggie });
									lusNode = getLUSNode(reggie.getServiceID());

								} else {
									String msg = "The Lookup Service at "
											+ input
											+ " has already been discovered";
									JOptionPane.showMessageDialog(view, msg,
											"SORCER",
											JOptionPane.INFORMATION_MESSAGE);

								}
								if (lusNode != null) {
									_tree.setSelectionPath(new TreePath(lusNode
											.getPath()));
								}

							} catch (Exception ex) {
								ex.printStackTrace();
								String msg = "Failed to connect to: " + input
										+ "\n";
								// JOptionPane.showMessageDialog(view,msg+ex);
								_logger.error(msg);
							}
						}
					};
					t.start();
				}
			}
		});

		JMenuItem federate = new JMenuItem("Federate Lookup services...");
		federate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

				ArrayList lusList = new ArrayList();
				int nLus = _root.getChildCount();

				for (int i = 0; i < nLus; i++) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) _root
							.getChildAt(i);
					lusList.add(node.getUserObject());
				}
				if (lusList.size() < 2) {
					JOptionPane
							.showMessageDialog(
									_frame,
									"A minimum of two Lookup Services are required to perform this function",
									"SORCER", JOptionPane.WARNING_MESSAGE);
					return;
				}
				FederateDialog dlg = new FederateDialog(_frame, lusList);
				dlg.setResizable(false);
				dlg.setSize(385, 165);
				centreDialog(dlg, _frame);
				dlg.setVisible(true);
			}
		});

		JMenuItem updateMenu = new JMenuItem("Check for updates...");
		updateMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				doUpdates();
			}
		});
		JMenu help = new JMenu("Help");
		JMenuItem about = new JMenuItem("About");

		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				JOptionPane.showMessageDialog(view, SorcerServiceBrowser.ABOUT,
						"About", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		JMenuItem helpView = new JMenuItem("Browser help...");

		helpView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showRightPaneImpl(_root);

			}
		});
		JMenuItem pluginHelp = new JMenuItem("Plugin and Filter development...");

		pluginHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				setPluginRightPane(_pluginHelpView);

			}
		});

		JMenuItem lic = new JMenuItem("Install license file...");

		lic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				JFileChooser fileDlg = new JFileChooser();
				fileDlg.setDialogTitle("Select SSB license file");
				fileDlg.setFileSelectionMode(JFileChooser.FILES_ONLY);

				fileDlg.showOpenDialog(view);
				File file = fileDlg.getSelectedFile();
				if (file != null && file.exists()) {
					try {
						SorcerServiceBrowser.installLicense(file);
						_frame.setTitle(SorcerServiceBrowser.TITLE);
						JOptionPane.showMessageDialog(view,
								"Please re-start the browser", "SORCER:",
								JOptionPane.INFORMATION_MESSAGE);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(view, ex.getMessage(),
								"SORCER:", JOptionPane.WARNING_MESSAGE);
					}

				}

			}
		});

		help.add(lic);
		help.addSeparator();

		final String show = "Show full layer popups";
		final String hide = "Hide full layer popups";

		final JMenuItem layer = new JMenuItem(show);
		layer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (layer.getText().equals(show)) {
					layer.setText(hide);
				} else {
					layer.setText(show);
				}
				SorcerServiceBrowser.PROPS_MODE = !SorcerServiceBrowser.PROPS_MODE;
			}
		});
		JMenuItem mcView = new JMenuItem("Multicast monitor");
		mcView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

				if (_multicastView == null) {
					Rectangle bounds = _frame.getBounds();
					bounds.x += 25;
					bounds.y += 25;
					try {
						createMulticastView(bounds, true);
					} catch (Exception ex) {
						System.err.println("Caught Exception: "
								+ ex.getClass().getName() + "; Msg: "
								+ ex.getMessage());
						ex.printStackTrace();
					}

				} else {
					_multicastView.toFront();
				}
			}
		});
		JMenuItem groovyConsole = new JMenuItem("Groovy console");
		groovyConsole.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
			    Console console = new Console(Thread.currentThread().getContextClassLoader());
			    //console.setVariable("exertlet", getExertlet());
			    console.run();
			}
		});
		JMenu lookMenu = new JMenu("View");
		lookMenu.add(refresh);
		lookMenu.addSeparator();

		lookMenu.add(layer);
		// lookMenu.add( new JMenuItem(new LogFileAction()));

		JMenu toolsMenu = new JMenu("Tools");

		// lookMenu.addSeparator();
		toolsMenu.add(groovyConsole);
		toolsMenu.add(mcView);

		// disable if its that horrible GTK look
		if (!SorcerServiceBrowser.GTKLookAndFeel) {

			lookMenu.addSeparator();
			lookMenu.add(_ixLandF);
			_ixLandF.setEnabled(false);

			String lfClassName = UIManager.getSystemLookAndFeelClassName();

			_plafLandF = new PlafLookAndFeelAction(lfClassName,
					"Platform Look & Feel");
			_plafLandF.setEnabled(false);
			lookMenu.add(_plafLandF);

			if (SorcerServiceBrowser._theme == null) {
				_ixLandF.setEnabled(true);
			}
		}

		final JMenuItem showAll = new JMenuItem("Bring All to Front");
		showAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// sortWindows();
				int nw = _windows.size();
				// first parse to Maximized windows
				for (int i = 0; i < nw; i++) {
					final JFrame jf = (JFrame) _windows.get(i);
					jf.setState(Frame.NORMAL);

					jf.toFront();

				}
				_frame.toFront();
			}
		});
		
		final JMenu windows = new JMenu("Window");
		windows.addMenuListener(new MenuListener() {
			public void menuSelected(MenuEvent evt) {
				windows.removeAll();
				int n = _windows.size();

				windows.add(showAll);
				windows.addSeparator();
				showAll.setEnabled(n > 1);

				windows.add(_cascadeAction);
				windows.add(_tileAction);
				windows.addSeparator();
				windows.add(fileClose);
				windows.addSeparator();

				_cascadeAction.setEnabled(n > 1);
				_tileAction.setEnabled(n > 1);
				showAll.setEnabled(true);

				for (int i = 0; i < n; i++) {
					final JFrame jf = (JFrame) _windows.get(i);
					// if a frame is maximized disable cascade/time
					if (jf.getExtendedState() == Frame.MAXIMIZED_BOTH) {
						_cascadeAction.setEnabled(false);
						_tileAction.setEnabled(false);

						showAll.setEnabled(false);

					}
					String title = jf.getTitle();
					if (title.endsWith(TITLE_TAG)) {
						title = title.substring(0, title.length()
								- TITLE_TAG.length());
					}
					JMenuItem mi = new JMenuItem(title);
					mi.setEnabled(jf != _frame);
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							jf.setState(Frame.NORMAL);
							jf.toFront();

						}
					});
					windows.add(mi);
				}
			}

			public void menuDeselected(MenuEvent evt) {

			}

			public void menuCanceled(MenuEvent evt) {

			}
		});

		file.add(fileNew);
		file.addSeparator();

		file.add(fileExit);
		if (!SorcerServiceBrowser.EXPIRED) {
			mb.add(file);
			mb.add(lookMenu);

			toolsMenu.addSeparator();
			toolsMenu.add(addLoc);
			toolsMenu.add(federate);

			toolsMenu.addSeparator();
			toolsMenu.add(new JMenuItem(new LogFileAction()));

			JMenu filterMenu = new JMenu("Filters");
			mb.add(filterMenu);

			// options.add(new JSeparator());
			filterMenu.add(filters);
			filterMenu.add(reset);
			// options.add(new JSeparator());
			// options.add(layer);

			if (!SorcerServiceBrowser.isPlugin && !IS_BRANDED) {
				toolsMenu.addSeparator();
				toolsMenu.add(updateMenu);

			}
			// mb.add(options);
			mb.add(toolsMenu);
			// _pluginMenu.setEnabled(false);
			if (BRANDED_HELP == null) {
				mb.add(_pluginMenu);
			}
			mb.add(windows);
			help.add(helpView);
			_pluginMenu.add(pluginHelp);
		}
		help.add(about);

		mb.add(help);
		return mb;
	}

	private synchronized void refresh() {
		// iterate LUS and remove if not contactable
		/*
		 * try{ boolean debug=Boolean.getBoolean("incax.debug"); if(!debug){
		 * File f=new File(ServiceBrowserConfig.LOG_FILE); //long len=f.length();
		 * //if(len>5000){ f.delete();
		 * //ServiceBrowserConfig.LOG_FILE="browser-"+System
		 * .currentTimeMillis()+".log"; //ServiceBrowser.createLog(); //} }
		 * }catch(Exception ex){ System.err.println(ex); }
		 */
		ArrayList toRemove = new ArrayList();
		int nKids = _root.getChildCount();
		for (int i = 0; i < nKids; i++) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) _root
					.getChildAt(i);
			if (n == null) {
				continue;
			}
			ServiceNode sn = (ServiceNode) n.getUserObject();
			if (sn == null) {
				continue;
			}
			if (sn.ping() == false) {
				toRemove.add(n);
				if (sn.isLus()) {
					try {
						Lease lease = (Lease) sn.getUserObject();
						_logger.info("Cancelling lease " + lease + " LUS="
								+ sn.getServiceID());
						if (lease != null && _lrm != null) {
							_lrm.remove(lease);
						}

					} catch (Exception ex) {
						_logger.error(ex.getMessage());
					}

				}
				if (_rightPaneNode != null && n.equals(_rightPaneNode)) {
					// remove view
					showRightPaneImpl(null);
					_serviceSelectedNode = _root2;
					_lusSelectedNode = _root;
				}
				ServiceID sid = sn.getServiceID();

				if (_selectedNode != null && isSelectedNodeChildOf(sid)) {
					SwingUtilities.invokeLater(wrap(new Runnable() {
						public void run() {
							showRightPane(_currentRoot);
						}
					}));

					_serviceSelectedNode = _root2;
					_lusSelectedNode = _root;
				}

			}
		}
		// now remove all the dead proxies
		DefaultTreeModel model = (DefaultTreeModel) _tree.getModel();
		int nr = toRemove.size();
		for (int i = 0; i < nr; i++) {

			try {
				model.removeNodeFromParent((DefaultMutableTreeNode) toRemove
						.get(i));
			} catch (Exception ex) {
				_logger.info("" + ex);
			}
		}
		// update service's tree
		toRemove = new ArrayList();
		model = (DefaultTreeModel) _tree2.getModel();
		nKids = _root2.getChildCount();
		for (int i = 0; i < nKids; i++) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) _root2
					.getChildAt(i);
			ServiceNode sn = (ServiceNode) n.getUserObject();
			if (sn.ping() == false) {
				toRemove.add(n);
			}
		}
		nr = toRemove.size();
		for (int i = 0; i < nr; i++) {

			try {
				model.removeNodeFromParent((DefaultMutableTreeNode) toRemove
						.get(i));
			} catch (Exception ex) {
				_logger.info("" + ex);
			}
		}
	}

	public JPanel getUI() {
		return _ui;
	}

	private void showFilters() {
		Frame frame = JOptionPane.getFrameForComponent(_ui);

		final JDialog dlg = new JDialog(frame, "Filters", true);

		JPanel ctrls = new JPanel();
		JButton ok = new JButton("Apply");
		JButton cancel = new JButton("Cancel");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				dlg.dispose();
				// _filtersView.save();

				showRightPane(_currentRoot);

				Thread t = new Thread() {
					public void run() {
						updateFilters();
					}
				};
				t.start();

			}
		});
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// _filtersView.load();
				dlg.dispose();
			}
		});
		ctrls.add(ok);
		ctrls.add(cancel);
		dlg.getContentPane().add(_filtersView, BorderLayout.CENTER);
		dlg.getContentPane().add(ctrls, BorderLayout.SOUTH);
		dlg.setSize(430, 300);
		centreDialog(dlg, frame);
		dlg.setVisible(true);
	}

	private JComponent getGraphics(DefaultMutableTreeNode selectedNode) {
		// _logger.info("getGraphics");
		GlyphView gv = new GlyphView(this);
		_glyphView = gv;
		// get the services
		ServiceNode sn = (ServiceNode) selectedNode.getUserObject();
		// IX04.01
		String label = sn.toString();// TreeRenderer.getJiniName(sn.getServiceItem().service,sn.toString());
		/*
		 * try{ ServiceRegistrar
		 * reggie=(ServiceRegistrar)sn.getServiceItem().service;
		 * label=""+reggie.getLocator(); label=label.substring(7);
		 * }catch(Exception ex){ }
		 */
		Rectangle rect = new Rectangle(50, 50, 350, 350);
		Glyph lus = new Glyph(label,/* TreeRenderer._lusGlyphIcon.getImage() */
		null, rect);
		lus.setAsRoot();
		// modified v4.5 [ix-02]
		lus.setUserObject(sn);// .getServiceItem());

		_lusGlyph = lus;

		label = addServicesToGlyph(lus, selectedNode, label);

		gv.add(lus);
		// _serviceUIPanel.add(gv,BorderLayout.CENTER);
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(gv, BorderLayout.CENTER);

		// _logger.info("getGraphics - end");

		return p;
	}

	private String addServicesToGlyph(Glyph lus,
			DefaultMutableTreeNode selectedNode, String label) {
		// add the services
		int n = selectedNode.getChildCount();
		// _logger.info("+++ Number of glyphs "+lus.getText()+" = "+n);
		for (int i = 0; i < n; i++) {
			DefaultMutableTreeNode kid = (DefaultMutableTreeNode) selectedNode
					.getChildAt(i);
			Object userObject = kid.getUserObject();
			if (userObject instanceof ServiceNode) {

				ServiceNode kidObject = (ServiceNode) userObject;
				Object proxy = kidObject.getServiceItem().service;
				label = TreeRenderer.getJiniName(proxy, kidObject.toString());
				if (proxy instanceof ServiceRegistrar) {
					try {
						ServiceRegistrar reggie = (ServiceRegistrar) proxy;
						LookupLocator loc = reggie.getLocator();
						label += " " + loc.getHost() + ":" + loc.getPort();
					} catch (Exception ex) {
						_logger.info("" + ex);
					}

				} else {
					// REMOVED for 2.3
					// int delim=label.lastIndexOf(".");
					// if(delim!=-1){
					// label=label.substring(delim+1);
					// }
				}

				// _logger.info("+++ adding glyph "+label);
				Glyph g1 = new Glyph(label,/*
											 * TreeRenderer._serviceGlyphIcon.getImage
											 * ()
											 */
				null);
				// modified v4.5 [ix-02]
				g1.setUserObject(kidObject);// .getServiceItem());
				lus.addChild(g1);
			}
		}
		return label;
	}

	synchronized void lusUpdated(DefaultMutableTreeNode root) {

		if (root == null || root == _currentRoot) {
			showRightPane(_currentRoot);
		}
		// _logger.info("+++ updating glyphs _lusGlyph="+_lusGlyph);

		if (_lusGlyph != null) {
			_lusGlyph.removeAll();
			addServicesToGlyph(_lusGlyph, _selectedNode, "");
			_glyphView.repaint();
		}
		// _logger.info("+++ updating glyphs: complete");
		/*
		 * if(root==_selectedLus){ showRightPane(root); }
		 */
	}

	private void showRightPane(final DefaultMutableTreeNode selectedNode) {
		_lusGlyph = null;
		final boolean[] res = new boolean[1];
		int divLoc = _splitter.getDividerLocation();
		DefaultMutableTreeNode nodeToShow = selectedNode;
		Object node = selectedNode.getUserObject();
		if (!(node instanceof ServiceNode)) {
			ServiceNode sn = getServiceNode(selectedNode);
			if (sn != null) {
				showProps(sn.getLookupAttributes());
			} else {
				showProps(new Entry[] {});
			}
			if (node instanceof PropertiesNode) {

				PropertiesNode pn = (PropertiesNode) node;
				if (nodeToShow.isLeaf()) {

					if (pn.getType() == pn.METHOD) {
						try {
							_splitter.setRightComponent(new MethodView(pn));
							_splitter.setDividerLocation(divLoc);
						} catch (Exception ex) {
							System.err.println("Caught Exception: "
									+ ex.getClass().getName() + "; Msg: "
									+ ex.getMessage());
							ex.printStackTrace();
						}
						return;
					}
				}
				if (pn.getType() == pn.INTERFACE) {
					// show a list of methods and
					_splitter.setRightComponent(new PropertiesView(
							(DefaultMutableTreeNode) nodeToShow.getChildAt(0)));

					_splitter.setDividerLocation(divLoc);
					return;
				}

			} else if (node instanceof CodebaseNode && selectedNode.isLeaf()) {
				CodebaseNode cbn = (CodebaseNode) node;

				JarView jarView = new JarView(cbn.getCodebase());
				_splitter.setRightComponent(jarView);
				_splitter.setDividerLocation(divLoc);
				String[] cp = jarView.getClasspathURLs();
				if (cp != null) {
					// System.out.println("Class-Path: jars detected");
					addClassPathJars(cp, cbn.getCodebase(),
							(DefaultMutableTreeNode) selectedNode.getParent());
				}
				return;

			}
			if (node.toString().equals("Methods")) {
				_splitter.setRightComponent(new MethodListView(_frame,
						nodeToShow));
			} else {
				_splitter.setRightComponent(new PropertiesView(nodeToShow));
			}
			_splitter.setDividerLocation(divLoc);

			// fallen though all checks so do nada
			return;
		}
		Component gp = ServiceBrowserConfig.FRAME.getGlassPane();
		gp.setVisible(true);
		gp.setCursor(Cursor.getPredefinedCursor(3));

		try {
			final ServiceNode sn = (ServiceNode) node;
			Thread t = new Thread(wrap(new Runnable() {
				public void run() {
					try {

						res[0] = sn.ping();

					} catch (Throwable ex) {
						_logger.info(sn + " " + ex);
						ex.printStackTrace();

					}
				}
			}));
			t.start();
			try {

				t.join(3000);
				if (res[0] == false) {
					// reset selected node
					showRightPaneImpl(null);
					return;
				}

				showProps(sn.getLookupAttributes());// new Entry[]{});

			} catch (Exception ex) {
				showError(ex);
				return;
			}
			showRightPaneImpl(selectedNode);

		} finally {
			gp.setCursor(Cursor.getDefaultCursor());
			gp.setVisible(false);
		}
		// _logger.info("+++ _lusGlyph="+_lusGlyph);
		// }
		// });
		// t.start();
	}

	public void showRightPaneImpl(final DefaultMutableTreeNode selectedNode) {
		SwingUtilities.invokeLater(wrap(new Runnable() {
			public void run() {
				showRightPaneImpl2(selectedNode);
			}
		}));
	}

	public void showRightPaneImpl2(DefaultMutableTreeNode selectedNode) {
		// _logger.info("showRightPaneImpl");
		_rightPaneNode = selectedNode;

		final ServiceBrowserUI browser = this;
		_serviceUIPanel.removeAll();
		int divLoc = _splitter.getDividerLocation();

		_splitter.setRightComponent(new JPanel());
		_splitter.setDividerLocation(divLoc);
		if (selectedNode == null) {

			// showProps(new Entry[]{});
			return;
		}
		Object userObject = selectedNode.getUserObject();
		_selectedLus = null;
		boolean added = false;
		boolean addedWin = false;
		
		if (userObject instanceof ServiceNode) {
			final ServiceNode sn = (ServiceNode) userObject;
			final Object proxy = sn.getProxy();
			boolean isLus = sn.isLus();
			final JTabbedPane tp = new JTabbedPane();
			_serviceUIPanel.add(tp, BorderLayout.CENTER);

			if (isLus) {
				final JComponent gv = getGraphics(selectedNode);
				tp.add("Services", gv);
				tp.setIconAt(0, TreeRenderer._lusIcon);
				tp.add("LUS Admin", new JPanel());
				tp.setIconAt(1, TreeRenderer._sidIcon);

				_eventView.setFilter(sn.toString());

				final JPanel emptyPanel = new JPanel();
				tp.add("LUS Events", emptyPanel);
				tp.setIconAt(2, TreeRenderer._serviceIcon);

				added = true;

				// final boolean [] done=new boolean[1];

				tp.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent evt) {
						// clear the eventview
						tp.setComponentAt(2, emptyPanel);
						if (tp.getSelectedIndex() == 1) {
							SwingUtilities.invokeLater(wrap(new Runnable() {
								public void run() {

									try {
										AdminView av = new AdminView(proxy, sn
												.toString(), _adminPreparer,
												ServiceBrowserUI.this);
										// tp.setComponentAt(0,new JPanel());
										tp.setComponentAt(1, av.makeGUI());

									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}
							}));

						} else if (tp.getSelectedIndex() == 2) {
							tp.setComponentAt(2, _eventView);
						}
					}
				});

			} else {
				JPanel contentPane = null;
				String name = null;
				try {	
					// see if the service has a service UI
					Entry[] atts = sn.getLookupAttributes();
					final Frame frame = JOptionPane.getFrameForComponent(_ui);
					if (proxy instanceof Administrable) {
						Administrable admin = (Administrable) proxy;
						Object adminProxy = _adminPreparer.prepareProxy(admin
								.getAdmin());
						if (adminProxy instanceof JoinAdmin
								|| AdminView.canDestroy(adminProxy)) {
							name = sn.toString();
							tp.add("Admin", new AdminView(proxy, name,
									_adminPreparer, ServiceBrowserUI.this)
									.makeGUI());
							tp.setIconAt(tp.getComponentCount() - 1,
									TreeRenderer._sidIcon);
							added = true;
						}
						if (adminProxy instanceof com.sun.jini.outrigger.JavaSpaceAdmin) {
							com.sun.jini.outrigger.JavaSpaceAdmin spaceAdmin = (com.sun.jini.outrigger.JavaSpaceAdmin) adminProxy;
							tp.add("Space browser", new OutriggerViewer(_frame,
									spaceAdmin, this));
							tp.setIconAt(tp.getComponentCount() - 1,
									TreeRenderer._spaceIcon);
						}
						// for backward compatibility
						try {
							if (proxy instanceof JavaSpace) {
								_logger.info("Checking for JavaSpace05");
								Class.forName("net.jini.space.JavaSpace05");
								if (proxy instanceof JavaSpace05) {
									tp.add("JavaSpace05",
											new JavaSpaceContentsView(_frame,
													(JavaSpace05) proxy, this));
									tp.setIconAt(tp.getComponentCount() - 1,
											TreeRenderer._spaceIcon);
								}
							}
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}

					windowMenu = new JMenu("Windows");
					for (int i = 0; i < atts.length; i++) {
						//System.out.println("Att="+atts[i]);
						if (atts[i] != null && atts[i] instanceof UIDescriptor) {
							contentPane = new JPanel();
							contentPane.setLayout(new BorderLayout());

							UIDescriptor desc = (UIDescriptor) atts[i];
							name = desc.role;

							if (desc.toolkit.equals("javax.swing")) {

								int index = name.lastIndexOf(".");
								if (index != -1 && index < name.length() - 1) {
									name = name.substring(index + 1);
								}
								final Object factory = desc.getUIFactory(proxy
										.getClass().getClassLoader());								
								if (factory instanceof JComponentFactory) {
									JComponentFactory jcf = (JComponentFactory) factory;
									JComponent comp = jcf.getJComponent(sn
											.getServiceItem());
									
										name = addAccessibleName(name, comp);
										contentPane.add(comp, BorderLayout.CENTER);		
										added = true;
										tp.add(name, contentPane);
										tp.setIconAt(tp.getComponentCount() - 1,
												TreeRenderer._serviceUIIcon);
								}
								if (factory instanceof JFrameFactory) {
									String windowName = "New Window";
									if (factory instanceof UIFrameFactory) {
										windowName = ((UIFrameFactory)factory).getAccessibleName();
									}
									addedWin = true;
									JMenuItem mi = new JMenuItem(windowName);
									windowMenu.add(mi);
									mi.addActionListener(new ActionListener() {
										public void actionPerformed(
												ActionEvent evt) {

											try {
												JFrameFactory jcf = (JFrameFactory) factory;
												JFrame jf = jcf.getJFrame(sn
														.getServiceItem());
												if (jf == null) {
													JOptionPane
															.showMessageDialog(
																	frame,
																	"Factory returned null",
																	"SORCER",
																	JOptionPane.INFORMATION_MESSAGE);
													return;
												}

												Dimension d = jf.getSize();
												if (d.width == 0) {
													jf.setSize(300, 300);
												}
												jf.setVisible(true);

											} catch (Exception ex) {
												showError(ex);
											}
										}
									});

								}
								else if (factory instanceof JDialogFactory) {
									addedWin = true;
									JMenuItem mi = new JMenuItem("JDialog");
									windowMenu.add(mi);
									mi.addActionListener(new ActionListener() {
										public void actionPerformed(
												ActionEvent evt) {

											try {
												JDialogFactory jcf = (JDialogFactory) factory;
												JDialog jf = jcf
														.getJDialog(
																sn
																		.getServiceItem(),
																JOptionPane
																		.getFrameForComponent(_ui));
												if (jf == null) {
													JOptionPane
															.showMessageDialog(
																	frame,
																	"Factory returned null",
																	"SORCER",
																	JOptionPane.INFORMATION_MESSAGE);
													return;
												}

												Dimension d = jf.getSize();
												if (d.width == 0) {
													jf.setSize(300, 300);
												}
												jf.setVisible(true);

											} catch (Exception ex) {
												showError(ex);
											}
										}
									});

								}
								else if (factory instanceof JWindowFactory) {
									addedWin = true;
									JMenuItem mi = new JMenuItem("JWindow");
									windowMenu.add(mi);
									mi.addActionListener(new ActionListener() {
										public void actionPerformed(
												ActionEvent evt) {

											try {
												JWindowFactory jcf = (JWindowFactory) factory;
												JWindow jf = jcf
														.getJWindow(
																sn
																		.getServiceItem(),
																JOptionPane
																		.getFrameForComponent(_ui));
												if (jf == null) {
													JOptionPane
															.showMessageDialog(
																	frame,
																	"Factory returned null",
																	"SORCER",
																	JOptionPane.INFORMATION_MESSAGE);
													return;
												}

												Dimension d = jf.getSize();
												if (d.width == 0) {
													jf.setSize(300, 300);
												}
												jf.setVisible(true);

											} catch (Exception ex) {
												showError(ex);
											}
										}
									});

								}
							}
						}
					}
					if (addedWin) {
						if (windowMenu.getMenuComponents().length > 0) {
							JMenuBar mb = new JMenuBar();
							mb.add(windowMenu);
							contentPane.add(mb, BorderLayout.NORTH);
						}
						//tp.add(name, contentPane);
						tp.add("Windows", contentPane);
						tp.setIconAt(tp.getComponentCount() - 1,
								TreeRenderer._serviceUIIcon);
					}
				} catch (Exception ex) {
					showError(ex);
				}
				if (!added) {
					// show parent lookup service
					JComponent gv = getGraphics((DefaultMutableTreeNode) selectedNode
							.getParent());
					_serviceUIPanel.add(gv, BorderLayout.CENTER);
					added = true;
				}
			}
		}
		if (!added) {

			_logger.debug("View NOT added ");
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode
					.getParent();
			while (parent != null) {
				userObject = parent.getUserObject();
				if (userObject instanceof ServiceNode) {
					ServiceNode sn = (ServiceNode) userObject;
					if (sn.isLus()) {

						JComponent gv = getGraphics(parent);
						_serviceUIPanel.add(gv, BorderLayout.CENTER);
						added = true;
						break;
					}
				}
				parent = (DefaultMutableTreeNode) parent.getParent();
			}
			if (!added) {
				showProps(new Entry[] {});
				_logger.debug("Help view being added ");
				_serviceUIPanel.add(_helpView, BorderLayout.CENTER);
				// _serviceUIPanel.add(new JPanel(),BorderLayout.CENTER);
			}

		}
		// _logger.info("Resetting the divider");
		// int divLoc=_splitter.getDividerLocation();
		_splitter.setRightComponent(_serviceUIPanel);
		_splitter.setDividerLocation(divLoc);
		// _logger.info("Done");
	}

	private void showError(Exception ex) {
		ex.printStackTrace();
		// JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(_ui),ex);
	}

	private void cancelLeases() {
		_logger.info("Cancelling leases for " + _frame.getTitle());
		int nLus = _root.getChildCount();
		for (int i = 0; i < nLus; i++) {
			DefaultMutableTreeNode tn = (DefaultMutableTreeNode) _root
					.getChildAt(i);
			Object userObject = tn.getUserObject();
			ServiceNode sn = (ServiceNode) userObject;

			// System.out.println("### check node for lease "+sn);

			if (sn == null) {
				continue;
			}
			Object o = sn.getUserObject();

			// System.out.println("### Node object = "+o);

			if (o instanceof Lease) {
				try {
					((Lease) o).cancel();
					_logger.info("Cancelled lease for " + tn);
				} catch (Exception ex) {
					System.err.println(ex.getMessage());
				}
			}
		}
	}

	public void blockingTerminate() {
		Thread t = new Thread(wrap(new Runnable() {
			public void run() {

				terminateImpl();
			}
		}));
		t.start();
		try {
			t.join();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void terminate() {
		Thread t = new Thread(wrap(new Runnable() {
			public void run() {

				terminateImpl();
			}
		}));
		t.start();
	}

	public static void terminateAll() {
		ArrayList win = getWindows();
		int nw = win.size();
		_logger.info("Browser shutdown sequence commencing");
		for (int i = 0; i < nw; i++) {
			Object window = win.get(i);
			if (window instanceof BrowserFrame) {
				BrowserFrame bf = (BrowserFrame) window;

				// cancels leases
				bf.terminate();
			}
		}
		_logger.info("Browser ready to quit");
	}

	private void terminateImpl() {
		_logger.info("Terminating discovery for " + _frame.getTitle());
		if (_disco != null) {
			_disco.terminate();
		}

		cancelLeases();

		if (_lrm != null) {
			_lrm.clear();
		}

		try {
			_logger.info("Unexporting listener for " + _frame.getTitle());
			if (_exporter != null) {
				_exporter.unexport(true);
			}

		} catch (Exception ex) {
			System.err.println(ex.getMessage());
		}

	}

	class Listener implements DiscoveryListener {
		// invoked when a LUS is discovered
		public void discovered(DiscoveryEvent ev) {
			_logger.info("LUS: " + ev);

			final ServiceRegistrar[] reg = ev.getRegistrars();
			Thread t = new Thread(wrap(new Runnable() {
				public void run() {
					dicoveredImpl(reg);
				}
			}));
			t.start();

		}

		public void discarded(DiscoveryEvent ev) {
			_logger.info("LUS:discarded event " + ev);
			ServiceRegistrar[] lus = ev.getRegistrars();
			for (int i = 0; i < lus.length; i++) {

				ServiceID sid = lus[i].getServiceID();

				_logger.info("LUS:discarded " + lus[i] + " serviceID=" + sid);

				if (_selectedNode != null && isSelectedNodeChildOf(sid)) {
					showRightPane(_currentRoot);
				}
				removeFromTree(sid);
			}
		}
	}

	// Synchronized remove v5.2 [IX03]
	private void dicoveredImpl(ServiceRegistrar[] reg) {
		for (int i = 0; i < reg.length; i++) {
			try {
				synchronized (_discoveryLock) {
					_inDiscoveredImpl = true;
					// _logger.info("----
					// _inDiscoveredImpl="+_inDiscoveredImpl);
					_logger.info("Discovered LUS " + reg[i].getLocator());

					// changed in v4.2 [ix01[
					// WILL ONLY WORK WITH notify() synchronized on
					// _discoveryLock
					EventRegistration evtReg = reg[i].notify(_template,
							TRANSITIONS, _listener, null, Long.MAX_VALUE);
					Lease lease = (Lease) _leasePreparer.prepareProxy(evtReg
							.getLease());

					LusTree lusTree = new LusTree(reg[i], _tree, _template,
							_filtersView.getInterfaces(), _filtersView
									.getNameFilters(), _servicePreparer,
							_filtersView.getPlugin());

					_pluginReg.lusDiscovered(reg[i]);

					ServiceNode lusNode = lusTree.getRoot();

					// EventRegistration
					// evtReg=reg[i].notify(_template,TRANSITIONS,_listener,null,Long.MAX_VALUE);
					// Lease lease=lease=evtReg.getLease();

					lusNode.setUserObject(lease);

					_lrm = new LeaseRenewalManager();
					_lrm.renewUntil(lease, Long.MAX_VALUE, new LeaseListener() {
						public void notify(LeaseRenewalEvent evt) {
							_logger.info("LeaseRenewalEvent " + evt);
						}
					});
					_logger
							.info("Registering for events: initial lease expires "
									+ new java.util.Date(lease.getExpiration()));
					// updates the Service-centric view
					updateServicesTree();
					_inDiscoveredImpl = false;
					// _logger.info("----
					// _inDiscoveredImpl="+_inDiscoveredImpl);
					_discoveryLock.notifyAll();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				_inDiscoveredImpl = false;

			}

		}
		_tree.expandRow(0);
	}

	private void removeFromTree(ServiceID sid) {
		DefaultMutableTreeNode node = getNode(sid);
		if (node != null) {
			if (node == _selectedNode) {
				// clear the right pane
				showRightPaneImpl(null);
			}
			DefaultTreeModel model = (DefaultTreeModel) _tree.getModel();
			model.removeNodeFromParent(node);
		}
	}

	private void removeFromServiceTree(ServiceID sid) {
		int n = _root2.getChildCount();
		for (int i = 0; i < n; i++) {
			DefaultMutableTreeNode serviceNode = (DefaultMutableTreeNode) _root2
					.getChildAt(i);
			ServiceNode sn = (ServiceNode) serviceNode.getUserObject();
			if (sn.sameServiceID(sid)) {
				if (serviceNode == _selectedNode) {
					// clear the right pane
					showRightPaneImpl(null);
				}
				DefaultTreeModel model = (DefaultTreeModel) _tree2.getModel();
				model.removeNodeFromParent(serviceNode);
				return;
			}
		}

	}

	private DefaultMutableTreeNode getNode(ServiceID sid) {
		int nLus = _root.getChildCount();
		for (int i = 0; i < nLus; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) _root
					.getChildAt(i);
			// now iterate the children for matching serviceIDs
			int nKids = node.getChildCount();
			for (int j = 0; j < nKids; j++) {
				DefaultMutableTreeNode kid = (DefaultMutableTreeNode) node
						.getChildAt(j);
				ServiceNode sn = (ServiceNode) kid.getUserObject();
				if (sn.sameServiceID(sid)) {
					return kid;
				}
			}
		}
		return null;
	}

	private void updateNodes(ServiceID sid, ServiceItem si) {
		int nLus = _root.getChildCount();
		for (int i = 0; i < nLus; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) _root
					.getChildAt(i);
			int nKids = node.getChildCount();
			for (int j = 0; j < nKids; j++) {
				DefaultMutableTreeNode kid = (DefaultMutableTreeNode) node
						.getChildAt(j);
				ServiceNode sn = (ServiceNode) kid.getUserObject();
				if (sn.sameServiceID(sid)) {
					sn.updateServiceItem(si, true);
					updateAttributes(_tree, kid, sn);
				}
			}

		}

		int nServices = _root2.getChildCount();
		for (int i = 0; i < nServices; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) _root2
					.getChildAt(i);
			ServiceNode sn = (ServiceNode) node.getUserObject();
			if (sn.sameServiceID(sid)) {
				sn.updateServiceItem(si, true);
				updateAttributes(_tree2, node, sn);
			}
		}

	}

	private void updateAttributes(JTree tree, DefaultMutableTreeNode kid,
			ServiceNode sn) {
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode attsNode = getAttsNode(kid);

		if (attsNode != null) {
			boolean expanded = _tree
					.isExpanded(new TreePath(attsNode.getPath()));
			model.removeNodeFromParent(attsNode);
			attsNode = LusTree.addAttributes(kid, sn.getServiceItem());
			model.nodeStructureChanged(kid);
			if (expanded) {
				TreePath nsp = new TreePath(attsNode.getPath());
				_tree.expandPath(nsp);
			}
		} else {
			_logger.error("Bug: can't locate attributes node for service "
					+ kid);
		}
	}

	private DefaultMutableTreeNode getAttsNode(DefaultMutableTreeNode snode) {
		int nkids = snode.getChildCount();
		for (int i = 0; i < nkids; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) snode
					.getChildAt(i);
			if (node.getUserObject().equals("Lookup attributes"))
				return node;
		}

		return null;
	}

	private ServiceNode getServiceNode(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode tn = node;

		while (tn != null) {
			Object userObject = tn.getUserObject();
			if (userObject instanceof ServiceNode) {
				return (ServiceNode) userObject;
			}
			tn = (DefaultMutableTreeNode) tn.getParent();
		}
		return null;
	}

	private DefaultMutableTreeNode getLUSNode(ServiceID sid) {

		int nLus = _root.getChildCount();
		for (int i = 0; i < nLus; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) _root
					.getChildAt(i);
			ServiceNode sn = (ServiceNode) node.getUserObject();
			if (sn.sameServiceID(sid)) {
				return node;
			}
		}

		return null;
	}

	public void notify(RemoteEvent theEvent) throws UnknownEventException,
			java.rmi.RemoteException {

		synchronized (_discoveryLock) {
			while (_inDiscoveredImpl) {
				_logger.info("##### Waiting for discovery to complete");
				try {
					_discoveryLock.wait();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// _logger.info("##### _inDiscoveredImpl=false");
		}

		_logger.info("" + theEvent);

		ServiceEvent se = (ServiceEvent) theEvent;
		final ServiceID sid = se.getServiceID();

		int trans = se.getTransition();
		ServiceRegistrar lus = (ServiceRegistrar) theEvent.getSource();
		final ServiceID lusSid = lus.getServiceID();
		final ServiceItem item = se.getServiceItem();

		DefaultMutableTreeNode lusNode = getLUSNode(lusSid);
		final Object[] eventData = new Object[] {  lusNode == null ? "null" : lusNode.toString(),
				new java.util.Date(), "",// name
				"",// event type
				sid };
		if ((trans & ServiceRegistrar.TRANSITION_NOMATCH_MATCH) > 0) {
			_logger.info(lusSid + " ServiceRegistrar.TRANSITION_NOMATCH_MATCH "
					+ sid);

			if (item != null && item.service != null) {

				item.service = _servicePreparer.prepareProxy(item.service);
				try {
					ServiceNode sNode = new ServiceNode(item);
					DefaultMutableTreeNode service = new DefaultMutableTreeNode(
							sNode);
					// setValue the serviceName
					eventData[2] = sNode.toString();
					eventData[3] = "NOMATCH_MATCH";

					if (lusNode != null) {
						// check service is not in tree
						int ns = lusNode.getChildCount();
						for (int i = 0; i < ns; i++) {
							DefaultMutableTreeNode tn = (DefaultMutableTreeNode) lusNode
									.getChildAt(i);
							ServiceNode sn = (ServiceNode) tn.getUserObject();
							if (sn.sameServiceID(item.serviceID)) {
								_logger.debug("Service exists in tree " + sNode);
								return;
							}
						}

						if (LusTree
								.addServiceItems(item, service, _filtersView
										.getNameFilters(), _filtersView
										.getInterfaces())) {

							DefaultTreeModel model = (DefaultTreeModel) _tree
									.getModel();
							model.insertNodeInto(service, lusNode, lusNode
									.getChildCount());
							_tree.expandRow(0);
							lusUpdated(lusNode);
							updateServicesTree();

						}
					}
					_pluginReg.serviceDiscovered(item);

				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}

		} else if ((trans & ServiceRegistrar.TRANSITION_MATCH_NOMATCH) > 0) {
			_logger.info(lusSid + "ServiceRegistrar.TRANSITION_MATCH_NOMATCH "
					+ sid);
			// SwingUtilities.invokeLater( new Runnable() {
			// public void run(){

			// if(isSelectedNodeChildOf(sid)){
			// showRightPaneImpl(null);
			// }
			// DefaultMutableTreeNode lusNode=getLUSNode(lusSid);
			DefaultMutableTreeNode node = getNode(sid);
			if (node != null) {
				eventData[2] = "" + node;
			}
			eventData[3] = "MATCH_NOMATCH";

			removeFromTree(sid);
			removeFromServiceTree(sid);
			if (lusNode != null) {
				lusUpdated(lusNode);
			}
			if (isSelectedNodeChildOf(sid) && lusNode != null) {

				_tree.setSelectionPath(new TreePath(lusNode.getPath()));

			}
			_pluginReg.serviceDiscarded(sid);
			// });

		} else {
			_logger.info("ServiceRegistrar.TRANSITION_MATCH_MATCH " + item);
			// Added version 2.1
			// onlu update is selected Node has changed
			/*
			 * if(isSelectedNodeChildOf(sid)){ showRightPane(_selectedNode); }
			 */
			DefaultMutableTreeNode node = getNode(sid);
			eventData[2] = "" + node;
			eventData[3] = "MATCH_MATCH";

			final DefaultMutableTreeNode serviceNode = getNode(sid);

			if (serviceNode != null) {
				ServiceNode sn = (ServiceNode) serviceNode.getUserObject();
				try {
					if (item != null && item.attributeSets != null) {
						_logger.info("Updating service attributes from event ["
								+ sn + "]");
						sn.updateServiceItem(item, true); // = new
						// ServiceNode(item);
						serviceNode.setUserObject(sn);
					} else {
						_logger
								.info("Updating service attributes from JoinAdmin ["
										+ sn + "]");
						sn.ping();
					}
					final ServiceNode snode = sn;
					SwingUtilities.invokeAndWait(new Runnable() {

						public void run() {
							_logger.info("Updating tree for " + serviceNode);
							updateNodes(sid, snode.getServiceItem());

							final Object userObject = _selectedNode
									.getUserObject();
							if (userObject instanceof ServiceNode) {
								ServiceNode sn2 = (ServiceNode) userObject;

								if (sn2.sameServiceID(sid)) {
									_logger.info("Updating view for "
											+ userObject);
									showRightPane(_selectedNode);
								}
							}
						}
					});
				} catch (Exception ex) {

					System.err.println("Caught Exception: "
							+ ex.getClass().getName() + "; Msg: "
							+ ex.getMessage());
					ex.printStackTrace();
				}
				_pluginReg.serviceModified(sid);
			}
			/*
			 * final Object userObject=_selectedNode.getUserObject();
			 * if(userObject instanceof ServiceNode){ final ServiceNode
			 * sn=(ServiceNode)userObject;
			 * 
			 * if(sn.sameServiceID(sid)){ Thread t=new Thread(){ public void
			 * run(){
			 * 
			 * SwingUtilities.invokeLater(new Runnable(){ public void run(){
			 * _logger.info("Updating view for "+userObject);
			 * showRightPane(_selectedNode); } }); } }; t.start(); } }
			 */

		}
		_logger.info("-- RemoteEvent end --");
		_eventView.update(eventData);
	}

	// TO DO
	/*
	 * private boolean updateExistsingNodes(ServiceItem si){ boolean
	 * updated=false; int nLus=_root.getChildCount(); for(int i=0;i<nLusli++){
	 * DefaultMutableTreeNode tn=(DefaultMutableTreeNode)_root.getChildAt(i);
	 * //for each LUS } return updated; }
	 */
	private boolean isSelectedNodeChildOf(ServiceID sid) {

		DefaultMutableTreeNode n = _selectedNode;

		if (n == null) {
			return false;
		}

		while (n != null) {
			Object userObject = n.getUserObject();
			if (userObject instanceof ServiceNode) {
				ServiceNode sn = (ServiceNode) userObject;
				if (sn.sameServiceID(sid)) {
					return true;
				}
			}
			n = (DefaultMutableTreeNode) n.getParent();
		}
		return false;
	}

	public static void centreDialog(java.awt.Dialog dlg, java.awt.Frame f) {
		java.awt.Dimension size = dlg.getSize();
		java.awt.Rectangle loc = f.getBounds();
		int xpos = loc.x + (loc.width / 2) - (size.width / 2);
		int ypos = loc.y + (loc.height / 2) - (size.height / 2);
		dlg.setLocation(xpos, ypos);
	}

	private String addAccessibleName(String name, JComponent comp) {
		AccessibleContext ctx = comp.getAccessibleContext();
		if (ctx != null) {
			String ctxName = ctx.getAccessibleName();
			if (ctxName != null) {
				return ctxName;
			}
		}
		return name;
	}

	// LIVE UPDATE
	private void doUpdates() {
		String JAVA_HOME = System.getProperty("java.home");
		String HOME = ServiceBrowserConfig.BROWSER_HOME;

		String[] cmd = { JAVA_HOME + "/bin/java",
				"-Djava.security.policy=" + HOME + "/policy.all", "-cp",
				HOME + "/update.jar", "Start", HOME };
		Runtime rt = Runtime.getRuntime();
		try {
			rt.exec(cmd);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(_ui, ex, "SORCER",
					JOptionPane.INFORMATION_MESSAGE);
		}

	}

	private class SSBLookAndFeelAction extends AbstractAction {

		public SSBLookAndFeelAction() {
			super("SORCER Look & Feel");

		}

		public void actionPerformed(ActionEvent evt) {
			try {
				String uiClass = "javax.swing.plaf.metal.MetalLookAndFeel";

				JFrame f = (JFrame) JOptionPane.getFrameForComponent(_ui);
				MetalLookAndFeel
						.setCurrentTheme(new sorcer.ssb.jini.studio.StudioTheme());
				UIManager.setLookAndFeel(uiClass);
				SwingUtilities.updateComponentTreeUI(f);
				_splitter.setDividerSize(3);
				setEnabled(false);
				_plafLandF.setEnabled(true);
			} catch (Exception ex) {
			}

		}
	}

	private class PlafLookAndFeelAction extends AbstractAction {
		private String className;

		private String lookName;

		public PlafLookAndFeelAction(String c, String name) {
			super(name);

			className = c;
			lookName = name;

		}

		public void actionPerformed(ActionEvent evt) {
			try {
				UIManager.setLookAndFeel(className);

				JFrame f = (JFrame) JOptionPane.getFrameForComponent(_ui);
				SwingUtilities.updateComponentTreeUI(f);
				setEnabled(false);
				_ixLandF.setEnabled(true);
			} catch (Exception ex) {
			}

		}

	}

	private class LogFileAction extends AbstractAction {
		public LogFileAction() {
			super("Log file");
		}

		public void actionPerformed(ActionEvent evt) {
			try {

				if (_logView == null) {

					Rectangle bounds = _frame.getBounds();
					bounds.x += 25;
					bounds.y += 25;
					createLogView(bounds, true);

				} else {
					_logView.toFront();
				}
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(_frame, ex, "SORCER",
						JOptionPane.WARNING_MESSAGE);
			}
		}
	}

	public static void createLogView(Rectangle bounds, boolean requestFocus)
			throws Exception {

		_logView = new LogFileView();

		_windows.add(_logView);

		_logView.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {

				boolean ok = _windows.remove(_logView);
				System.out.println("LogView removed=" + ok);
				_logView = null;

			}
		});

		_logView.setBounds(bounds);
		_logView.setVisible(true);
		if (requestFocus) {
			_logView.requestFocus();
		}

	}

	public static void createMulticastView(Rectangle bounds,
			boolean requestFocus) throws Exception {

		_multicastView = new MulticastView();

		_windows.add(_multicastView);

		_multicastView.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {

				boolean ok = _windows.remove(_multicastView);
				System.out.println("LogView removed=" + ok);
				_multicastView = null;

			}
		});

		_multicastView.setBounds(bounds);
		_multicastView.setVisible(true);
		if (requestFocus) {
			_multicastView.requestFocus();
		}

	}

	private void updateServicesTree() {
		// invoked when new LookupService discovered
		int n = _root.getChildCount();
		for (int i = 0; i < n; i++) {
			DefaultMutableTreeNode lusNode = (DefaultMutableTreeNode) _root
					.getChildAt(i);
			ServiceNode lsn = (ServiceNode) lusNode.getUserObject();
			// create a new ServiceNode and then tag it as NOT a LUS
			ServiceNode newSn = new ServiceNode(lsn.getServiceItem());
			newSn.markAsService();

			DefaultMutableTreeNode slusNode = new DefaultMutableTreeNode(newSn);
			if (LusTree
					.addServiceItems(lsn.getServiceItem(), slusNode,
							_filtersView.getNameFilters(), _filtersView
									.getInterfaces())) {

				addIfNew(newSn, slusNode);
			}

			// next build a list of unique service
			int nj = lusNode.getChildCount();
			for (int j = 0; j < nj; j++) {
				DefaultMutableTreeNode sNode = (DefaultMutableTreeNode) lusNode
						.getChildAt(j);
				ServiceNode sn = (ServiceNode) sNode.getUserObject();
				// ServiceID sid=sn.getServiceItem().serviceID;
				addIfNew(sn, sNode);
			}

		}
	}

	private void addIfNew(ServiceNode toAdd, DefaultMutableTreeNode node) {
		int n = _root2.getChildCount();
		for (int i = 0; i < n; i++) {
			DefaultMutableTreeNode serviceNode = (DefaultMutableTreeNode) _root2
					.getChildAt(i);
			ServiceNode sn = (ServiceNode) serviceNode.getUserObject();
			if (toAdd.sameServiceID(sn.getServiceItem().serviceID)) {
				return;
			}
		}
		DefaultMutableTreeNode nodeClone = cloneNode(node);

		DefaultTreeModel model = (DefaultTreeModel) _tree2.getModel();
		model.insertNodeInto(nodeClone, _root2, _root2.getChildCount());
		_tree2.expandRow(0);
	}

	private DefaultMutableTreeNode cloneNode(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(node
				.getUserObject());
		int n = node.getChildCount();
		for (int i = 0; i < n; i++) {
			newNode.add(cloneNode((DefaultMutableTreeNode) node.getChildAt(i)));
		}
		return newNode;
	}

	private void addClassPathJars(String[] cp, URL jar,
			DefaultMutableTreeNode cbn) {

		try {
			DefaultTreeModel model = (DefaultTreeModel) _selectedTree
					.getModel();

			String exf = jar.toExternalForm();
			String rup = exf.substring(0, exf.lastIndexOf("/"));

			int nkids = cbn.getChildCount();

			for (int i = 0; i < cp.length; i++) {
				boolean addUrl = true;
				URL url = new URL(rup + "/" + cp[i]);
				// check that this node doesn't already exist
				for (int j = 0; j < nkids; j++) {
					DefaultMutableTreeNode kid = (DefaultMutableTreeNode) cbn
							.getChildAt(j);
					CodebaseNode n = (CodebaseNode) kid.getUserObject();
					if (n.sameURL(url)) {
						addUrl = false;
						break;
					}
				}
				if (addUrl) {
					CodebaseNode cbNode = new CodebaseNode(url, true);
					DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
							cbNode);
					model.insertNodeInto(newNode, cbn, cbn.getChildCount());
				}
			}

		} catch (Exception ex) {
			System.err.println("Caught Exception: " + ex.getClass().getName()
					+ "; Msg: " + ex.getMessage());
			ex.printStackTrace();
		}

	}

	private class OnCascadeAction extends AbstractAction {

		public OnCascadeAction() {
			super("Cascade", null);
		}

		public void actionPerformed(ActionEvent evt) {

			Dimension dim = java.awt.Toolkit.getDefaultToolkit()
					.getScreenSize();
			int wid = (int) (dim.width * .85);
			int hi = (int) (dim.height * .75);
			int xPos = 0;
			int yPos = 0;
			Iterator e = _windows.iterator();
			JFrame lastWindow = null;
			while (e.hasNext()) {
				JFrame win = (JFrame) e.next();
				win.setState(Frame.NORMAL);
				win.setBounds(xPos, yPos, wid, hi);
				xPos += XINC;
				yPos += YINC;
				if (xPos > dim.height) {
					xPos = 20;
					yPos = 0;
				}
				// win.toFront();
				win.toFront();
				win.validate();
				lastWindow = win;
				// }
			}
			if (lastWindow != null) {
				lastWindow.requestFocus();

			}
			// activeWin.setBounds(xPos,yPos,wid,hi);
			// activeWin.toFront();

		}
	}

	private class OnTileAction extends AbstractAction {
		public OnTileAction() {
			super("Tile", null);
		}

		public void actionPerformed(ActionEvent evt) {

			Dimension dim = java.awt.Toolkit.getDefaultToolkit()
					.getScreenSize();
			int nwins = _windows.size();

			if (nwins == 0) {
				return;
			}
			int offset = 0;
			double sqrt = Math.sqrt(nwins);

			int num = (int) (sqrt);

			double xwid = dim.width / (num);
			double fact = (double) nwins / (double) num;
			if (fact != (int) fact)
				fact = (int) (fact + 1.0);

			double yhi = dim.height / (fact);

			int xpos = 0;
			int ypos = 0;
			int count = 0;

			Iterator e = _windows.iterator();
			while (e.hasNext()) {
				JFrame win = (JFrame) e.next();
				// System.out.println(win.getTitle()+" "+xpos+" "+ypos);

				win.setState(Frame.NORMAL);

				win.setBounds(xpos, ypos, (int) xwid, (int) yhi);
				win.validate();

				win.toFront();
				count++;
				xpos += xwid;
				if (count == num) {
					xpos = 0;
					ypos += yhi;
					count = 0;
				}
				//
			}

		}
	}

	// PLUGINS STUFF
	public JTabbedPane getTabbedPane() {
		return tp;
	}

	public void setPluginRightPane(JComponent comp) {
		int divLoc = _splitter.getDividerLocation();

		_splitter.setRightComponent(comp);
		_splitter.setDividerLocation(divLoc);
	}

	public void pluginSetMenu(JMenuItem menu) {
		_pluginMenu.addSeparator();
		_pluginMenu.add(menu);
	}

	/*
	 * private void sortWindows(){ Comparator comp=new Comparator(){ public int
	 * compare(Object o1,Object o2){ JFrame jf1=(JFrame)o1; JFrame
	 * jf2=(JFrame)o2; Rectangle r1=jf1.getBounds(); Rectangle
	 * r2=jf1.getBounds();
	 * 
	 * int ret= r2.y-r1.y; System.out.println(jf1.getTitle()+" "+ret);
	 * 
	 * return ret; } }; Collections.sort(_windows,comp); }
	 */
}
