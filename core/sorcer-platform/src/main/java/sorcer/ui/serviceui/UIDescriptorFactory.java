/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.ui.serviceui;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.util.Collections;

import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.attribute.UIFactoryTypes;
import net.jini.lookup.ui.factory.JComponentFactory;
import net.jini.lookup.ui.factory.JDialogFactory;
import net.jini.lookup.ui.factory.JFrameFactory;
import net.jini.lookup.ui.factory.JWindowFactory;

/**
 * A helper utility that creates a UIDescriptor as part of the ServiceUI project
 */
public class UIDescriptorFactory {
	 
    public static UIDescriptor getUIDescriptor(String role, JComponentFactory factory) throws IOException{
        UIDescriptor desc = new UIDescriptor();
        desc.role = role;
        desc.toolkit = JComponentFactory.TOOLKIT;
        desc.attributes = Collections.singleton(new UIFactoryTypes(Collections.singleton(JComponentFactory.TYPE_NAME)));
        desc.factory = new MarshalledObject(factory);
        return(desc);
    }

    public static UIDescriptor getUIDescriptor(String role, JDialogFactory factory) throws IOException{
        UIDescriptor desc = new UIDescriptor();
        desc.role = role;
        desc.toolkit = JDialogFactory.TOOLKIT;
        desc.attributes = Collections.singleton(new UIFactoryTypes(Collections.singleton(JDialogFactory.TYPE_NAME)));
        desc.factory = new MarshalledObject(factory);
        return(desc);
    }

    public static UIDescriptor getUIDescriptor(String role, JFrameFactory factory) throws IOException{
    	UIDescriptor desc = new UIDescriptor();
    	desc.role = role;
    	desc.toolkit = JFrameFactory.TOOLKIT;
    	desc.attributes = Collections.singleton(new UIFactoryTypes(Collections.singleton(JFrameFactory.TYPE_NAME)));
    	desc.factory = new MarshalledObject(factory);
    	return(desc);
    }

    public static UIDescriptor getUIDescriptor(String role, JWindowFactory factory) throws IOException{
        UIDescriptor desc = new UIDescriptor();
        desc.role = role;
        desc.toolkit = JWindowFactory.TOOLKIT;
        desc.attributes = Collections.singleton(new UIFactoryTypes(Collections.singleton(JWindowFactory.TYPE_NAME)));
        desc.factory = new MarshalledObject(factory);
        return(desc);
    }
}
