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

import net.jini.config.Configuration;
import sorcer.core.SorcerConstants;
import sorcer.ssb.jini.studio.CodeServer;
import sorcer.ssb.jini.studio.StudioTheme;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;

//import sorcer.ssb.osx.OSXApplication;

public class SorcerServiceBrowser {

	static ServiceBrowserUI browser;
	
	public static String BASE_TITLE = "SORCER Service Browser v6.0 [SSB-6.03]";

	public static String TITLE = BASE_TITLE;

	public static boolean PROPS_MODE = true;

	private static String LOG_FILE = "browserLog.txt";

	public static PrintStream sysOut = System.out;

	private static String STATE = "issb.state";

	public static StudioTheme _theme;

	public static boolean GTKLookAndFeel;

	public static long MAX_LEN = 500l * 1024;

	public static boolean isPlugin;

	public static String ABOUT = BASE_TITLE;

	public static boolean EXPIRED;
	

	public static void start(String[] args, boolean pluginStart,
			Configuration config) {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		ServiceBrowserConfig.setConfiguration(config);
		try {

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					saveSettings();
					ServiceBrowserUI.terminateAll();
				}
			});
			// Initialize system properties: configs/sorcer.env
//			Sorcer.getEnvProperties();
//			ServiceBrowserUI._logger.info("Provider accessor: " 
//					+ Sorcer.getProperty(SorcerConstants.S_SERVICE_ACCESSOR_PROVIDER_NAME));
//			
			ABOUT = TITLE;
			isPlugin = pluginStart;
			TreeRenderer tr = new TreeRenderer(); // force images to load

			try {
				String path = CodeServer.getPathForClass(tr.getClass());
				if (path.endsWith(".jar")) {
					path = path.substring(0, path.lastIndexOf("/"));
				}

				if (path.endsWith(File.separator)) {
					LOG_FILE = path + LOG_FILE;
				} else {
					LOG_FILE = path + File.separator + LOG_FILE;
				}
				ServiceBrowserConfig.LOG_FILE = LOG_FILE;
				
				String userHome = System.getProperty("user.home");
				STATE = userHome + File.separator + STATE;
				ServiceBrowserConfig.BROWSER_HOME = path;
				ServiceBrowserConfig.STATE = STATE;
				File pluginDir = new File(path + "/plugins");
				pluginDir.mkdirs();

			} catch (Exception ex) {
				System.err
						.println("Unable to determine path, using working directory");
			}
			ImageIcon logo = null;
			// BRANDING
			boolean isBranded = false;
			/*
			 * if (sorcer.ssb.tools.plugin.browser.lic.License.OK) { File
			 * brandingFile = new File(BROWSER_HOME+ "/branding.properties");
			 * System.out.println("Branding file=" +
			 * brandingFile.getAbsolutePath()); if (brandingFile.exists()) {
			 * isBranded=true; ServiceBrowserUI.IS_BRANDED=true; try {
			 * Properties props = new Properties();
			 * 
			 * FileInputStream fis = new FileInputStream(brandingFile);
			 * props.load(fis); fis.close(); TITLE =
			 * props.getProperty("browser.title", "No branding");
			 * ServiceBrowserUI.TITLE_TAG=" - "+TITLE; ABOUT = TITLE; String
			 * splash = BROWSER_HOME + "/" +
			 * props.getProperty("browser.splash"); logo = new ImageIcon(new
			 * File(splash).getAbsolutePath()); String
			 * help=props.getProperty("browser.help");
			 * ServiceBrowserUI.BRANDED_HELP=new File(BROWSER_HOME + "/"+help);
			 * } catch (Throwable t) { t.printStackTrace(); } } }
			 */
			// disable output unless debug settings setValue to true
			boolean debug = Boolean.getBoolean("ssb.debug");
			if (!debug) {
				createLog();
			}
			System.out.println(TITLE);

			// License.generateLicense();

			final boolean showDefaults = !isBranded;
			/*
			 * new License(new MessageListener() { public void
			 * licenseMessage(String msg) { //
			 * JOptionPane.showMessageDialog(frame,msg); // ABOUT+="\n"+msg;
			 * System.err.println(msg); ABOUT += "\n" + msg; }
			 * 
			 * public void expiryMessage(String msg) { ABOUT += "\n" + msg;
			 * if(showDefaults){ TITLE += " - " + msg; } } public void
			 * licenseExpired(String msg){ EXPIRED=true;
			 * 
			 * } });
			 */
			ABOUT += "\n(c)2009-2010 SORCERsoft.org";
			boolean useCodeServer = "true".equals(System.getProperty(SorcerConstants.SORCER_CODE_SERVER_INTERNAL));
			if (!StartSorcerBrowser.isWebsterInt && useCodeServer) {
				CodeServer.autoStart(tr.getClass(), ServiceBrowserConfig.getConfiguration());
			}

			boolean isMacOS = false;
			if (System.getProperty("mrj.version") != null) {
				System.setProperty("com.apple.macos.useScreenMenuBar", "true");
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				isMacOS = true;

			}
			String jvmVersion = System.getProperty("java.vm.version");
			boolean isJava5 = jvmVersion.startsWith("1.5");

			String lfClassName = UIManager.getSystemLookAndFeelClassName();
			// Check for doggey 1.5 GTK laf
			if (lfClassName.indexOf("GTKLookAndFeel") != -1) {
				GTKLookAndFeel = true;
				lfClassName = "javax.swing.plaf.metal.MetalLookAndFeel";
			}

			String uiClass = "javax.swing.plaf.metal.MetalLookAndFeel";
			if (uiClass.equals(lfClassName)) {
				if (!isJava5) {
					_theme = new StudioTheme();
					MetalLookAndFeel.setCurrentTheme(_theme);
				} else {

					UIManager.put("swing.boldMetal", Boolean.FALSE);
				}
			}

			UIManager.setLookAndFeel(lfClassName);

			final BrowserFrame f = new BrowserFrame(TITLE, true/* isDefaultView */);
			if (logo == null) {
				logo = new ImageIcon(f.getClass().getClassLoader().getResource(
						"rt-images/splash.png"));

			}
//			if (isMacOS) {
//				OSXApplication app = new OSXApplication() {
//					public void quit() {
//						saveSettings();
//						System.exit(0);
//					}
//
//					public void about() {
//						String aboutText = TITLE
//								+ "\n(c)2009-20010 SORCERsoft - www.SORCERsoft.org";
//						JOptionPane.showMessageDialog(f, aboutText, "About",
//								JOptionPane.INFORMATION_MESSAGE);
//					}
//
//					public void preferences() {
//
//					}
//				};
//				registerOSXApp(app);
//			}

			Rectangle bounds = null;
			java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit()
					.getScreenSize();
			ViewHolder defaultView = null;
			final ArrayList windows = (ArrayList) load();
			if (windows != null) {

				defaultView = removeDefaultView(windows);
				if (defaultView != null) {
					bounds = defaultView.bounds;
					f.setBounds(defaultView.bounds);
				}

			}
			if (bounds == null) {
				f.setSize((int) (screen.width * .75),
						(int) (screen.height * .55));
			}
			ServiceBrowserConfig.FRAME = f;

			if (defaultView != null) {
				FiltersView fv = new FiltersView();
				fv.restoreText(defaultView.filters);
				browser = new ServiceBrowserUI(f, fv);
			} else {
				browser = new ServiceBrowserUI(f);
			}
			f.setBrowser(browser);

			JPanel ui = browser.getUI();

			// f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

			f.addWindowListener(new WindowAdapter() {

				public void windowClosing(WindowEvent evt) {

					boolean quit = true;
					/*
					 * if(FRAME.getFrames().length>1){ String message="Closing
					 * the main browser window will quit this application\nAre
					 * you sure you want to quit?"; int
					 * yesNo=JOptionPane.showConfirmDialog
					 * (FRAME,message,"Quit",JOptionPane.YES_NO_OPTION);
					 * quit=yesNo==JOptionPane.YES_OPTION; }
					 */
					if (quit) {
						saveSettings();
						// terminate now in save settings to make sure leases on
						// all windows are terminated
						// browser.terminate();
						System.exit(0);
					}

				}
			});

			f.setIconImage(TreeRenderer._frameIcon.getImage());

			f.getContentPane().add(ui);
			// disable for Mac
			if (!isMacOS && bounds == null) {
				centreFrame(f);
			}

			final SplashScreen splash = new SplashScreen(f, logo.getImage());
			f.setVisible(true);
			Thread t = new Thread() {
				public void run() {

					try {
						Thread.sleep(2000);
					} catch (Exception ex) {
					}
					splash.dispose();
					if (windows != null) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								openWindows(windows);
							}
						});
					}
					f.toFront();
				}
			};
			t.start();

			// now open any other persistent views

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	static void saveSettings() {
		File store = new File(STATE);
		store.delete();

		ArrayList settings = new ArrayList();
		ArrayList win = ServiceBrowserUI.getWindows();
		int nw = win.size();
		System.out.println("Saving settings: number of windows=" + nw);
		for (int i = 0; i < nw; i++) {
			Object window = win.get(i);
			if (window instanceof BrowserFrame) {
				BrowserFrame bf = (BrowserFrame) window;
				ViewHolder vh = new ViewHolder(bf.getTitle(), bf.getBounds(),
						bf.getFilters(), bf.hasFocus(), bf.isDefault());
				settings.add(vh);

			} else if (window instanceof LogFileView) {
				LogFileView lfv = (LogFileView) window;
				ViewHolder vh = new ViewHolder(lfv.getTitle(), lfv.getBounds(),
						null, lfv.hasFocus(), false);
				vh.logView = true;
				settings.add(vh);
			} else if (window instanceof MulticastView) {
				MulticastView lfv = (MulticastView) window;
				ViewHolder vh = new ViewHolder(lfv.getTitle(), lfv.getBounds(),
						null, lfv.hasFocus(), false);
				vh.multicastView = true;
				settings.add(vh);
			}

		}
		save(settings);

	}

	private static void openWindows(ArrayList win) {
		int nw = win.size();
		for (int i = 0; i < nw; i++) {
			ViewHolder vh = (ViewHolder) win.get(i);
			if (vh.logView == false && vh.multicastView == false) {

				FiltersView fv = new FiltersView();
				fv.restoreText(vh.filters);

				// System.out.println(vh.title+" "+fv);

				BrowserFrame bv = new BrowserFrame(vh.title);
				try {
					ServiceBrowserUI ui = new ServiceBrowserUI(bv, fv);
					bv.setBrowser(ui);
					bv.getContentPane().add(ui.getUI(), BorderLayout.CENTER);
					bv.setBounds(vh.bounds);
					bv.setVisible(true);
					if (vh.isSelected) {
						bv.requestFocus();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else if (vh.logView == true) {
				try {

					ServiceBrowserUI.createLogView(vh.bounds, vh.isSelected);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else if (vh.multicastView == true) {
				try {

					ServiceBrowserUI.createMulticastView(vh.bounds,
							vh.isSelected);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	static void save(Object obj) {
		try {

			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(STATE));
			oos.writeObject(obj);
			oos.flush();
			oos.close();

			// System.out.println("Saving object "+obj);

		} catch (Exception ex) {
			System.err.println(ex.getMessage());
		}
	}

	static Object load() throws IOException {
		Object obj = null;
		try {
			File file = new File(STATE);

			// System.out.println(file.getAbsolutePath());

			ObjectInputStream oos = new ObjectInputStream(new FileInputStream(
					file));
			obj = oos.readObject();

			oos.close();
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
		}
		return obj;
	}

	public static void centreFrame(java.awt.Frame f) {
		java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
		java.awt.Dimension size = f.getSize();

		int xpos = (screen.width / 2) - (size.width / 2);
		int ypos = (screen.height / 2) - (size.height / 2);
		f.setLocation(xpos, ypos);
	}

	public static void createLog() throws Exception {

		System.out.println("Creating new log file...");

		String ssbLogLen = System.getProperty("ssb.logLen");
		if (ssbLogLen != null) {
			try {
				MAX_LEN = Long.parseLong(ssbLogLen) * 1024;

			} catch (Exception ex) {
				System.err.println("Error in property ssb.logLen " + ex);
			}
		}

		String logName = System.getProperty("ssb.logFile");
		if (logName != null) {
			LOG_FILE = logName;
			ServiceBrowserConfig.LOG_FILE = logName;
		}
		File file = new File(LOG_FILE);

		System.out.println("Browser log file=" + file.getAbsolutePath());

		FileOutputStream fos = new FileOutputStream(file);
		MyOutputStream myOs = new MyOutputStream(fos);

		PrintStream ps = new MyPrintStream(myOs, LOG_FILE);
		System.setErr(ps);
		System.setOut(ps);

		System.out.println("Log file maximum size (-Dssb.loglen)="
				+ (MAX_LEN / 1024) + " KB");

	}

	private static ViewHolder removeDefaultView(ArrayList windows) {

		int nw = windows.size();
		for (int i = 0; i < nw; i++) {
			ViewHolder vh = (ViewHolder) windows.get(i);
			if (vh.isDefaultView) {

				windows.remove(i);
				return vh;
			}
		}
		return null;
	}

	public static class MyOutputStream extends OutputStream {
		public FileOutputStream _fos;

		MyOutputStream(FileOutputStream fos) {
			_fos = fos;
		}

		public void close() throws IOException {
			_fos.close();
		}

		public void flush() throws IOException {
			_fos.flush();
		}

		public void write(byte[] b, int off, int len) throws IOException {
			_fos.write(b, off, len);
		}

		public void write(byte[] b) throws IOException {
			_fos.write(b);
		}

		public void write(int b) throws IOException {

			_fos.write(b);
		}
	}

	public static class MyPrintStream extends PrintStream {
		private String logName;

		private MyOutputStream fos;

		public MyPrintStream(MyOutputStream os, String fName) {
			super(os);
			logName = fName;
			fos = os;

		}

		public void println(String str) {
			File f = new File(logName);

			if (f.length() > MAX_LEN) {
				// create new MyPrintStream
				try {
					fos.close();

					// f.renameTo(new File(logName+"_"+(logCount++)));
					f.delete();
					// PrintStream ps=new MyPrintStream(
					fos._fos = new FileOutputStream(logName);

					// System.setErr(ps);
					// System.setOut(ps);

					// System.out.println("New logFile created "+new
					// java.util.Date());
					ServiceBrowserUI._logger.info("New logFile created");

				} catch (Exception ex) {
					sysOut.println("Unable to create new log file " + ex);
				}
			}
			super.println(str);
		}

	}

	static void installLicense(File f) throws Exception {
		copyFile(f.getAbsolutePath(), System.getProperty("user.home")
				+ "/.ssb.lic");
		final String[] errMsg = new String[1];

		/*
		 * new License(new MessageListener() { public void licenseMessage(String
		 * msg) { // JOptionPane.showMessageDialog(frame,msg); //
		 * ABOUT+="\n"+msg; System.err.println(msg); errMsg[0] = msg; ABOUT =
		 * BASE_TITLE + "\n" + msg; }
		 * 
		 * public void expiryMessage(String msg) { ABOUT = BASE_TITLE + "\n" +
		 * msg; // System.err.println(msg); TITLE = BASE_TITLE + " - " + msg;
		 * 
		 * } public void licenseExpired(String msg){ ABOUT=BASE_TITLE+"\n"+msg;
		 * 
		 * } }); if (errMsg[0] != null) { throw new Exception(errMsg[0]); }
		 */
	}

	public static void copyFile(String src, String dest) throws IOException {

		FileInputStream fis = new FileInputStream(src);
		FileOutputStream fos = new FileOutputStream(dest);
		byte[] b = new byte[2048];
		int nBytes = fis.read(b);
		while (nBytes != -1) {
			fos.write(b, 0, nBytes);
			nBytes = fis.read(b);
		}
		fis.close();
		fos.close();
	}

//	static void registerOSXApp(sorcer.ssb.osx.OSXApplication theApp) {
//		try {
//			Class osxAdapter = Class.forName("sorcer.ssb.osx.OSXAdapter");
//
//			Class[] defArgs = { sorcer.ssb.osx.OSXApplication.class };
//			Method registerMethod = osxAdapter.getDeclaredMethod(
//					"registerMacOSXApplication", defArgs);
//			if (registerMethod != null) {
//				Object[] args = { theApp };
//				registerMethod.invoke(osxAdapter, args);
//			}
//			// This is slightly gross. to reflectively access methods with
//			// boolean args,
//			// use "boolean.class", then pass a Boolean object in as the arg,
//			// which apparently
//			// gets converted for you by the reflection system.
//			defArgs[0] = boolean.class;
//			Method prefsEnableMethod = osxAdapter.getDeclaredMethod(
//					"enablePrefs", defArgs);
//			if (prefsEnableMethod != null) {
//				Object args[] = { Boolean.TRUE };
//				prefsEnableMethod.invoke(osxAdapter, args);
//			}
//		} catch (NoClassDefFoundError e) {
//			// This will be thrown first if the OSXAdapter is loaded on a system
//			// without the EAWT
//			// because OSXAdapter extends ApplicationAdapter in its def
//			System.err
//					.println("This version of Mac OS X does not support the Apple EAWT.  Application Menu handling has been disabled ("
//							+ e + ")");
//		} catch (ClassNotFoundException e) {
//			// This shouldn't be reached; if there's a problem with the
//			// OSXAdapter we should getValue the
//			// above NoClassDefFoundError first.
//			System.err
//					.println("This version of Mac OS X does not support the Apple EAWT.  Application Menu handling has been disabled ("
//							+ e + ")");
//		} catch (Exception e) {
//			System.err.println("Exception while loading the OSXAdapter:");
//			e.printStackTrace();
//		}
//	}
}
