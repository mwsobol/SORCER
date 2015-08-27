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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Controller for Task Editor. This class controls all user events. Using action
 * listners.
 * 
 */
public class TaskEditorController implements ActionListener {

	private final static Logger logger = LoggerFactory.getLogger("open.optimization.provider.ui");

	private SignatureModel model;
	private DataNodeEditor dataView;
	private SignatureView view;

	// private Account account;

	public TaskEditorController(SignatureModel model, SignatureView view) {
		this.model = model;
		this.view = view;
	}

	private void dataNodeEditor() {
		JDialog nodeWindow = new JDialog();
		JPanel nodePanel = new JPanel();

		JPanel topPanel = new JPanel();
		JPanel midPanel = new JPanel();
		JPanel midPanel2 = new JPanel();
		JPanel bottomPanel = new JPanel();

		JLabel nodeLabel = new JLabel("Enter Node Data");
		JLabel dataType = new JLabel("Data Type");
		JLabel dataValue = new JLabel("Data Value");

		JTextField data = new JTextField(20);
		JButton ok = new JButton("Ok");
		JButton cancel = new JButton("Cancel");

		JComboBox list = new JComboBox();

		list.addItem("int");
		list.addItem("double");
		list.addItem("char");
		list.addItem("string");

		nodePanel.setLayout(new BorderLayout());

		nodePanel.add(topPanel, BorderLayout.NORTH);
		nodePanel.add(midPanel, BorderLayout.EAST);
		nodePanel.add(midPanel2, BorderLayout.WEST);
		nodePanel.add(bottomPanel, BorderLayout.SOUTH);

		topPanel.add(nodeLabel, BorderLayout.WEST);

		midPanel2.add(dataType, BorderLayout.EAST);
		midPanel.add(dataValue, BorderLayout.EAST);

		midPanel2.add(list, BorderLayout.WEST);
		midPanel.add(data, BorderLayout.WEST);

		bottomPanel.add(ok, BorderLayout.CENTER); // , BorderLayout.WEST);
		bottomPanel.add(cancel, BorderLayout.CENTER); // , BorderLayout.EAST );

		nodeWindow.add(nodePanel);
		nodeWindow.setSize(400, 150);
		nodeWindow.setVisible(true);
		// ok.setActionCommand("Ok");
		// ok.addActionListener(new BtnActionListener());

		// cancel.setActionCommand("Cancel");
		// cancel.addActionListener(this);

		// nodeWindow.setSize(400, 150);
		// nodeWindow.setVisible(true);
		// JDialog f = new JDialog();
		// J/TextArea g = new JTextArea("Inside the Controller");
		// f.setSize(400, 300);
		// f.add(g);
		// f.setVisible(true);

	}

	private void openDialog() {
		// String action = event.getActionCommand();
		JDialog f = new JDialog();

		JTextArea test2 = new JTextArea("Combo Box Selected!!!");
		f.add(test2);
		f.setSize(400, 300);
		f.setVisible(true);
		// logger.info("actionPerformed>>action: " + action);
	}

	private void updateSignatureModel() {
		JDialog f = new JDialog();
		JTextArea g = new JTextArea("Inside the Controller");
		f.setSize(400, 300);
		f.add(g);
		f.setVisible(true);
		model.setMethodModel(4);
	}

	public void actionPerformed(ActionEvent event) {
		String action = event.getActionCommand();
		if (action.equals("dialog"))
			openDialog();
		if (action.equals("intSelected"))
			updateSignatureModel();
		if (action.equals("New"))
			dataNodeEditor();
		logger.info("actionPerformed>>action: " + action);
		//	
		// }
	}
}
