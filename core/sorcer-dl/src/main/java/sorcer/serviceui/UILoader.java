package sorcer.serviceui;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JComponent;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.attribute.UIFactoryTypes;
import net.jini.lookup.ui.factory.JComponentFactory;

/**
 * The UILoader class is a helper for use with the ServiceUI
 */
public class UILoader {

    /**
     * This method returns an array JComponents from a UIDescriptor that contain
     * an JComponentFactory for a given role
     */
    public static JComponent[] loadUI(String role, ServiceItem serviceItem) {
        try {
            int attrCount = serviceItem.attributeSets.length;
            Entry[] attr = serviceItem.attributeSets;
            ArrayList list = new ArrayList();

            for(int i = 0; i<attrCount; ++i) {
                if(attr[i] instanceof UIDescriptor) {
                    UIDescriptor desc = (UIDescriptor) attr[i];
                    if(desc.attributes == null) {
                        continue;
                    }
                    
                    //if(role!=null && !desc.role.equals(role)) {
                    if(!desc.role.equals(role)) {
                        continue;
                    }

                    Iterator it = desc.attributes.iterator();
                    while(it.hasNext()) {
                        Object uiAttr = it.next();
                        if(uiAttr instanceof UIFactoryTypes) {
                            UIFactoryTypes factoryTypes = (UIFactoryTypes) uiAttr;
                            boolean found = false;
                            found = factoryTypes.isAssignableTo(JComponentFactory.class);
                            if(found) {
                                // Should also look through required packages here
                                list.add(desc);
                            }
                        }
                    }
                }
            }

            ArrayList factoryList = new ArrayList();
            for(int i=0; i<list.size(); i++) {
                Object uiFactory = null;
                try {
                    UIDescriptor desc = (UIDescriptor)list.get(i);
                    uiFactory = desc.getUIFactory(serviceItem.service.getClass().getClassLoader());
                    if(uiFactory instanceof JComponentFactory)
                        factoryList.add((JComponentFactory)uiFactory);
                } catch(ClassNotFoundException cnfe) {
                    System.out.println("Class not found. Couldn't unmarshall the UI factory.");
                    cnfe.printStackTrace();
                }
            }

            JComponent[] jComponents = new JComponent[factoryList.size()];
            for(int i=0; i<factoryList.size(); i++) {
                JComponentFactory factory = (JComponentFactory)factoryList.get(i);
                jComponents[i] =  factory.getJComponent(serviceItem);
            }
            return(jComponents);

        } catch(MalformedURLException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return(null);
    }

    /**
     * This method returns an array of UI Factories that have been loaded from each 
     * <code>UIDescriptor</code> found. This method will return all <code>UIDescriptor</code>
     * entries regardless of their role or factory type
     */
    public static Object[] loadUI(ServiceItem serviceItem) {
        try {
            int attrCount = serviceItem.attributeSets.length;
            Entry[] attr = serviceItem.attributeSets;
            ArrayList list = new ArrayList();

            for(int i = 0; i<attrCount; ++i) {
                if(attr[i] instanceof UIDescriptor) {
                    UIDescriptor desc = (UIDescriptor) attr[i];
                    if(desc.attributes == null) {
                        continue;
                    }
                    
                    Iterator it = desc.attributes.iterator();
                    while(it.hasNext()) {
                        Object uiAttr = it.next();
                        if(uiAttr instanceof UIFactoryTypes) {
                            // Should also look through required packages here
                            list.add(desc);
                        }
                    }
                }
            }

            ArrayList factoryList = new ArrayList();
            for(int i=0; i<list.size(); i++) {
                Object uiFactory = null;
                try {
                    UIDescriptor desc = (UIDescriptor)list.get(i);
                    uiFactory = desc.getUIFactory(serviceItem.service.getClass().getClassLoader());
                    factoryList.add(uiFactory);
                } catch(ClassNotFoundException cnfe) {
                    System.out.println("Class not found. Couldn't unmarshall the UI factory.");
                    cnfe.printStackTrace();
                }
            }

            return(factoryList.toArray());

        } catch(MalformedURLException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return(null);
    }
}


