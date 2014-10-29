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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ContextView extends JPanel {

	TaskEditorController dispatcher;

	ContextView(TaskEditorController dispatcher) {
		this.dispatcher = dispatcher;
		setLayout(new BorderLayout());
		createView();
	}

	public ContextView() {
		this.dispatcher = null;
		setLayout(new BorderLayout());
		createView();
	}

	private void createView() {

		// *****************Context Area**********************
		JTextArea context = new JTextArea("Context");
		JScrollPane contextView = new JScrollPane(context);
		add(contextView, BorderLayout.CENTER);

		// *****************Control Panel*********************
		JPanel cmdPanel = new JPanel();
		cmdPanel.setLayout(new BoxLayout(cmdPanel, BoxLayout.X_AXIS));
		cmdPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

		// West panel within Bottom Panel
		JPanel cmdPanelWest = new JPanel();
		cmdPanelWest.setLayout(new BorderLayout());
		cmdPanelWest.setLayout(new BoxLayout(cmdPanelWest, BoxLayout.X_AXIS));
		cmdPanelWest.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

		// East panel within Bottom Panel
		JPanel cmdPanelEast = new JPanel();
		cmdPanelEast.setLayout(new BorderLayout());
		cmdPanelEast.setLayout(new BoxLayout(cmdPanelEast, BoxLayout.X_AXIS));
		cmdPanelEast.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

		// //////////////////////////////////////////////
		// Bottom Buttons - BorderLayout is incorrect
		// uncomment ActionListners for button actions
		// //////////////////////////////////////////////

		ButtonGroup radioGroup = new ButtonGroup();

		JButton btn;
		JLabel blank;
		JRadioButton rbtn, rbtn2;

		rbtn = new JRadioButton("List");
		radioGroup.add(rbtn);
		// rbtn.addActionListener(new ActionListner());
		// rbtn.setSelected(true);

		cmdPanelWest.add(rbtn, BorderLayout.WEST);

		// NEEDS AN ACTION LISTNER TO WORK
		if (rbtn.isSelected()) {

		}

		blank = new JLabel("  ");
		cmdPanelWest.add(blank);

		rbtn2 = new JRadioButton("Tree");
		radioGroup.add(rbtn2);
		rbtn.setActionCommand("Tree");
		// btn.addActionListener(new BtnActionListener());
		cmdPanelWest.add(rbtn2, BorderLayout.EAST);

		blank = new JLabel(
				"                                                    ");
		cmdPanelWest.add(blank);

		btn = new JButton("New");
		btn.setActionCommand("New");
		btn.addActionListener(dispatcher);
		cmdPanelEast.add(btn);

		blank = new JLabel("  ");
		cmdPanelEast.add(blank);

		btn = new JButton("Delete");
		btn.setActionCommand("Delete");
		// btn.addActionListener(new BtnActionListener());
		cmdPanelEast.add(btn);

		blank = new JLabel("  ");
		cmdPanelEast.add(blank);

		btn = new JButton("Update");
		btn.setActionCommand("Update");
		// btn.addActionListener(new BtnActionListener());
		cmdPanelEast.add(btn);

		blank = new JLabel("  ");
		cmdPanelEast.add(blank);

		btn = new JButton("Execute");
		btn.setActionCommand("Execute");
		// btn.addActionListener(new BtnActionListener());
		cmdPanelEast.add(btn);
		// //////////////////////////////////////////////

		// Add east and west to cmdPanel then add cmd to
		// south of contextPanel
		cmdPanel.add(cmdPanelWest, BorderLayout.WEST);
		cmdPanel.add(cmdPanelEast, BorderLayout.EAST);
		add(cmdPanel, BorderLayout.SOUTH);
	}

}
