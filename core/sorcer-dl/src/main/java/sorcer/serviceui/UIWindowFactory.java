package sorcer.serviceui;

import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.JWindow;
import java.lang.reflect.*;
import net.jini.lookup.ui.factory.JWindowFactory;
import net.jini.core.lookup.ServiceItem;

/**
 * The UIWindowFactory class is a helper for use with the ServiceUI
 */
public class UIWindowFactory implements JWindowFactory, Serializable {
    private String className;
    private URL[] exportURL;

    public UIWindowFactory(URL exportUrl, String className) {
        this.className = className;
        this.exportURL = new URL[]{exportUrl};
    }

    public UIWindowFactory(URL[] exportURL, String className) {
        this.className = className;
        this.exportURL = exportURL;
    }

    public JWindow getJWindow(Object roleObject, Frame parent) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JWindow component=null;
        try {
            component = (JWindow)loadComponent(roleObject, parent);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    public JWindow getJWindow(Object roleObject, Window parent) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JWindow component=null;
        try {
            component = (JWindow)loadComponent(roleObject, parent);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    public JWindow getJWindow(Object roleObject) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JWindow component=null;
        try {
            component = (JWindow)loadComponent(roleObject, null);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    private Object loadComponent(Object roleObject, Object param) throws Exception {
        ClassLoader cl = ((ServiceItem)roleObject).service.getClass().getClassLoader();
        Object component=null;
        Object instanceObj = null;
        final URLClassLoader uiLoader = URLClassLoader.newInstance(exportURL, cl);
        //final URLClassLoader uiLoader = URLClassLoader.newInstance(exportURL);
        final Thread currentThread = Thread.currentThread();
        /*
        final ClassLoader parentLoader =
            (ClassLoader) AccessController.doPrivileged(
                                                       new PrivilegedAction() {
                                                           public Object run() {
                                                               return(currentThread.getContextClassLoader());
                                                           }
                                                       }
                                                       );

        try {
        */
            AccessController.doPrivileged(
                                         new PrivilegedAction() {
                                             public Object run() {
                                                 currentThread.setContextClassLoader(uiLoader);
                                                 return(null);
                                             }
                                         }
                                         );

            Class clazz = uiLoader.loadClass(className);
            Constructor constructor = null;
            
            if(param==null) {
                constructor = clazz.getConstructor(new Class[] {Object.class});
                instanceObj = constructor.newInstance(new Object[] {roleObject});
            } else if(param instanceof Window) {
                constructor = clazz.getConstructor(new Class[] {Object.class, Window.class});
                instanceObj = constructor.newInstance(new Object[] {roleObject, param});
            } else {
                constructor = clazz.getConstructor(new Class[] {Object.class, Frame.class});
                instanceObj = constructor.newInstance(new Object[] {roleObject, param});
            }
            /*
        } finally {
            AccessController.doPrivileged(
                                         new PrivilegedAction() {
                                             public Object run() {
                                                 currentThread.setContextClassLoader(parentLoader);
                                                 return(null);
                                             }
                                         }
                                         );
        }
        */
        component = instanceObj;
        return(component);

    }
}






