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
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.jini.core.lookup.ServiceItem;

class SignatureView extends JPanel implements Observer {

	private SignatureModel model;
	TaskEditorController dispatcher;
	private JPanel signaturePanel;

	SignatureView(TaskEditorController dispatcher, ServiceItem item) {

		model = new SignatureModel(item, 0);
		model.addObserver(this);
		this.dispatcher = dispatcher;
		createView();
	}

	private void createView() {

		// Main Root Panel
		JPanel signaturePanel = new JPanel();
		signaturePanel.setLayout(new BorderLayout());

		JComboBox interfaceList = new JComboBox(model.getInterfaces());
		interfaceList.setActionCommand("intSelected");
		interfaceList.addActionListener(dispatcher);

		JList methodList = new JList();
		methodList.setModel(model.getMethods(0));

		JScrollPane methodScrollPane = new JScrollPane(methodList);
		signaturePanel.add(interfaceList, BorderLayout.NORTH);
		signaturePanel.add(methodScrollPane, BorderLayout.CENTER);
	}

	public void refreshMethods() {
		JList methodList = new JList();
		methodList.setModel(model.getMethods(4));

		JScrollPane methodScrollPane = new JScrollPane(methodList);
		signaturePanel.add(methodScrollPane, BorderLayout.CENTER);

		JDialog f = new JDialog();
		JLabel g = new JLabel("Inside the Signature View");
		f.add(g);
		f.setVisible(true);
	}

	public void update(Observable arg0, Object arg1) {
		if (arg1 != null) {
			if (arg1.equals("intSelected"))
				refreshMethods();

		}
	}

}
