package sorcer.serviceui;

import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.awt.Dialog;
import java.awt.Frame;
import javax.swing.JDialog;
import java.lang.reflect.*;
import net.jini.lookup.ui.factory.JDialogFactory;
import net.jini.core.lookup.ServiceItem;

/**
 * The UIDialogFactory class is a helper for use with the ServiceUI
 */
public class UIDialogFactory implements JDialogFactory, Serializable {
	
    private static final long serialVersionUID = 806137627054275825L;
    
    private String className;
    
    private URL[] exportURL;

    public UIDialogFactory(URL exportUrl, String className) {
        this.className = className;
        this.exportURL = new URL[]{exportUrl};
    }

    public UIDialogFactory(URL[] exportURL, String className) {
        this.className = className;
        this.exportURL = exportURL;
    }

    public JDialog getJDialog(Object roleObject, Dialog dialog, boolean modal) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JDialog component=null;
        try {
            component = (JDialog)loadComponent(roleObject, dialog);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    public JDialog getJDialog(Object roleObject, Dialog dialog) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JDialog component=null;
        try {
            component = (JDialog)loadComponent(roleObject, dialog);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    public JDialog getJDialog(Object roleObject, Frame frame, boolean modal) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JDialog component=null;
        try {
            component = (JDialog)loadComponent(roleObject, frame);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    public JDialog getJDialog(Object roleObject, Frame frame) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JDialog component=null;
        try {
            component = (JDialog)loadComponent(roleObject, frame);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    public JDialog getJDialog(Object roleObject) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JDialog component=null;
        try {
            component = (JDialog)loadComponent(roleObject, null);
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
            } else if(param instanceof Dialog) {
                constructor = clazz.getConstructor(new Class[] {Object.class, Dialog.class});
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






