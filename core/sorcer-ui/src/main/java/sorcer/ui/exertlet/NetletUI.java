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

package sorcer.ui.exertlet;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * The user agent UI for the exertlet editing and execution.
 */
public class NetletUI extends JFrame {

	/** Creates new CatalogerUI */
	public NetletUI(Object obj) {
		super();
		getAccessibleContext().setAccessibleName("Exertlet Editor");
		createFrame(obj);
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private void createFrame(Object obj) {
		// Create and set up the window.
		setTitle("Exertlet Editor");
		// closing is managed by a service browser
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		NetletEditor eEditor = new NetletEditor(obj);
		eEditor.setOpaque(true); // content panes must be opaque
		setContentPane(eEditor);
		// center the frame on screen
		setLocationRelativeTo(null);
		// URL imgUrl = this.getClass().getResource("sorcer.jpg");
		// setIconImage(new ImageIcon(imgUrl).getImage());
		ImageIcon ii = new ImageIcon("sorcer.jpg");
		setIconImage(ii.getImage());
		this.setPreferredSize(new Dimension(600, 400));
		pack();
	}

}
