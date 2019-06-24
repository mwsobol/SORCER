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

import java.awt.BorderLayout;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;

import sorcer.service.Exerter;
import sorcer.util.SorcerUtil;

public class PropertiesPanel extends JPanel {

	private Exerter provider;
	PropertiesModel _model;
	ArrayList _uiDescriptors;
	// Entry atts[];
	List data = new ArrayList();
	List fields = new ArrayList();

	PropertiesPanel(Exerter prv, String borderTitle) {
		provider = prv;
		List attributes = null;
		try {
			attributes = provider.getProperties();
		} catch (Exception pe) {
			pe.printStackTrace();
		}
		update(attributes);
		_model = new PropertiesModel(data, fields);
		JTable table = new JTable(_model);
		JScrollPane sp = new JScrollPane(table);
		setLayout(new BorderLayout());
		TitledBorder title;
		title = BorderFactory.createTitledBorder(borderTitle);
		setBorder(title);
		add(sp, BorderLayout.CENTER);
	}

	PropertiesPanel(List properties) {
		// atts=attributes;
		update(properties);
		_model = new PropertiesModel(data, fields);
		JTable table = new JTable(_model);
		JScrollPane sp = new JScrollPane(table);
		setLayout(new BorderLayout());
		add(sp, BorderLayout.CENTER);
	}

	void update(List properties) {
		if (properties == null) {
			return;
		}
		List<Properties> providerProps = new ArrayList();
		;
		data = new ArrayList();
		fields = new ArrayList();
		for (Object prop : properties) {
			if (prop == null) {
				continue;
			}
			if (prop instanceof Properties) {
				providerProps.add((Properties) prop);
			}
			Class eClass = prop.getClass();
			String className = eClass.getName();
			Field f[] = eClass.getFields();
			for (int k = 0; k < f.length; k++) {
				String fName = f[k].getName();
				String displayName = className.substring(className
						.lastIndexOf(".") + 1, className.length());
				displayName = displayName + "." + fName;
				try {
					Object value = f[k].get(prop);
					if (value != null) {
						data.add(((Object) (new Object[] {
								displayName,
								(value.getClass().isArray() ? SorcerUtil
										.arrayToString(value) : value) })));
						// now store the actual class, field, isEditable
						fields.add(new Object[] { className, fName });
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

		}

		try {
			providerProps.add(provider.getJavaSystemProperties());
		} catch (RemoteException e1) {
			// do nothing
		}

		// append provider, SORCER Env properties,and JVM system properties
		for (Properties props : providerProps) {
			if (props != null) {
				Enumeration e = props.propertyNames();
				data.add(new Object[] { "--------------------",
						"--------------------" });
				while (e.hasMoreElements()) {
					Object key = e.nextElement();
					data.add(new Object[] { key, props.get(key) });
				}
			}
		}

		if (_model != null) {
			_model.update(data, fields);

		}
	}

}
