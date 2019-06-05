/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.ui.provider;

import java.lang.reflect.Method;
import java.util.Observable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;

import net.jini.core.lookup.ServiceItem;

/**
 * SignatureModel - Resposible for storing the interfaces and methods that the
 * user had entered. The signature model would update the method list model when
 * a new interface is selected. This SignatureModel was implemented and used but
 * could not getValue the observable part to work to create dynamic lists.
 */
public class SignatureModel extends Observable {

	private DefaultComboBoxModel interfaceModel;
	private DefaultListModel methodModel;
	private ServiceItem item;
	private Class[] interfaces;
	private Method[] methods;

	final static String intSelected = "intSelected";

	public SignatureModel(ServiceItem item, int intIndex) {
		this.item = item;
		interfaceModel = new DefaultComboBoxModel();
		interfaces = null;
		interfaces = item.service.getClass().getInterfaces();

		for (int i = 0; i < interfaces.length; i++) {
			interfaceModel.addElement(interfaces[i].getName());
		}

		methods = null;
		methodModel = new DefaultListModel();

		methods = interfaces[intIndex].getMethods();

		for (int i = 0; i < methods.length; i++) {

			methodModel.addElement(methods[i].getName());
		}
	}

	public DefaultComboBoxModel getInterfaces() {
		return interfaceModel;
	}

	public DefaultListModel getMethods(int interfaceIndex) {
		return methodModel;
	}

	public void setMethodModel(int intIndex) {

		// interfaceModel = new DefaultComboBoxModel();
		// interfaces = null;
		// interfaces = impl.service.getClass().getInterfaces();

		// for (int i = 0; i < interfaces.length; i++)
		// {
		// interfaceModel.addElement(interfaces[i].getName());
		// }

		// methods = null;
		// methodModel = new DefaultListModel();

		methods = interfaces[intIndex].getMethods();

		for (int i = 0; i < methods.length; i++) {

			methodModel.addElement(methods[i].getName());
		}
		notifyObservers(intSelected);
	}

	public Object getDataType() {
		// TODO Auto-generated method stub
		return null;
	}

	public void clearDataNodeType() {
		// TODO Auto-generated method stub

	}

	public void clearDataType() {
		// TODO Auto-generated method stub

	}

	public void setDataNodeType(String string) {
		// TODO Auto-generated method stub

	}

	public String getDataNodeType() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDataType(String dt) {
		// TODO Auto-generated method stub

	}
}