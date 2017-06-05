package sorcer.serviceui;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.ui.factory.JFrameFactory;

/**
 * The UIFrameFactory class is a helper for use with the ServiceUI
 */
public class UIFrameFactory implements JFrameFactory, Serializable {
	
	static final long serialVersionUID = 5806535989492809459L;

	private final static Logger logger = LoggerFactory.getLogger(UIFrameFactory.class.getName());
	
	private String className;
	private URL[] exportURL;
	//private String name;
	private URL helpURL;
	private String accessibleName = "Main Window";

	public String getAccessibleName() {
		return accessibleName;
	}

	public UIFrameFactory(URL exportUrl, String className, String name, URL helpUrl) {
		this.className = className;
		this.exportURL = new URL[] { exportUrl };
		this.accessibleName = name;
		this.helpURL = helpUrl;
	}

	public UIFrameFactory(URL exportUrl, String className) {
		this(exportUrl, className, null, null);
	}

	public UIFrameFactory(URL[] exportURL, String className, String name, URL helpUrl) {
		this.className = className;
		this.exportURL = exportURL;
		this.accessibleName = name;
		this.helpURL = helpUrl;
	}

	public UIFrameFactory(URL[] exportURL, String className) {
		this(exportURL, className, null, null);
	}

	public JFrame getJFrame(Object roleObject) {
		if (!(roleObject instanceof ServiceItem)) {
			throw new IllegalArgumentException("ServiceItem required");
		}
		ClassLoader cl = ((ServiceItem) roleObject).service.getClass()
				.getClassLoader();
		JFrame component = null;
		final URLClassLoader uiLoader = URLClassLoader.newInstance(exportURL,
				cl);
		final Thread currentThread = Thread.currentThread();

		final ClassLoader parentLoader = (ClassLoader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return (currentThread.getContextClassLoader());
					}
				});

		try {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					currentThread.setContextClassLoader(uiLoader);
					return (null);
				}
			});

			try {
				Class clazz = uiLoader.loadClass(className);
				Constructor constructor = clazz
						.getConstructor(new Class[] { Object.class });
				Object instanceObj = constructor
						.newInstance(new Object[] { roleObject });
				component = (JFrame) instanceObj;
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException(
						"Unable to instantiate ServiceUI " + className + ": "
								+ e.getClass().getName() + ": "
								+ e.getLocalizedMessage());
			}
		} finally {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					currentThread.setContextClassLoader(parentLoader);
					return (null);
				}
			});
		}
		return (component);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.jini.lookup.ui.factory.JComponentFactory#getJComponent(java.lang.
	 * Object)
	 */
	public JComponent getJComponent(Object arg) {
		if (helpURL == null)
			return new JLabel("No help page available");
		
		try {
			logger.info("help url: " + helpURL);
			JEditorPane htmlView = new JEditorPane(helpURL);
			htmlView.setEditable(false);
			// set the AccessibleContext Tag for this view
			// so the SORCER browser will display it
			JScrollPane sp = new JScrollPane(htmlView);
			sp.getAccessibleContext().setAccessibleName(accessibleName);
			return sp;
		} catch (Exception ex) {
			//return new JLabel(ex.toString() + " = " + helpFilename);
			JLabel lb = new JLabel("");;
			lb.getAccessibleContext().setAccessibleName(accessibleName);
			return lb;
		}
	}

	public String toString() {
		return "UIFrameFactory for className: " + className + ", exportURL: "
				+ Arrays.toString(exportURL);
	}
}
