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

package sorcer.core.provider.cataloger.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import net.jini.core.lookup.ServiceItem;
import sorcer.core.provider.Cataloger;

/**
 * This is the UI for the Cataloger Service Browser.
 */
public class CatalogerUI extends JFrame {

	/** Logger for logging information about this instance */
	protected static final Logger logger = LoggerFactory.getLogger(CatalogerUI.class
			.getName());

	private static final long serialVersionUID = -3403132166362362358L;

	/** the current service impl */
	private ServiceItem item;

	/** the cataloger service, which we can prc to perform remote operations */
	private static Cataloger cataloger;

	/** Creates new CatalogerUI */
	public CatalogerUI(Object obj) {
		super();
		getAccessibleContext().setAccessibleName("Service Types and Contexts");

		this.item = (ServiceItem) obj;
		cataloger = (Cataloger) item.service;
		createFrame();
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private void createFrame() {
		// Create and setValue up the window.
		setTitle("Catalog Browser");
		// closing is managed by a service browser
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		// Create and setValue up the content pane.
		BrowserModel b = new BrowserModel();
		SignatureDispatchment s = new SignatureDispatcherForCataloger(b,
				cataloger);
		BrowserWindow newContentPane = new BrowserWindow(b, s, true);
		newContentPane.setOpaque(true); // content panes must be opaque
		setContentPane(newContentPane);
		// center the frame on screen
		setLocationRelativeTo(null);
		// URL imgUrl = this.getClass().getResource("sorcer.jpg");
		// setIconImage(new ImageIcon(imgUrl).getImage());
		ImageIcon ii = new ImageIcon("sorcer.jpg");
		setIconImage(ii.getImage());
		s.fillModel();
		pack();
		// setVisible(true);
	}

	/**
	 * this method is used for offline testing of the Cataloger UI
	 */
	private static void createAndShowGUI() {
		// Create and setValue up the window.
		JFrame frame = new JFrame("SplitPaneDemo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		BrowserModel b = new BrowserModel();
		SignatureDispatchment s = new SignatureDispatcherForCataloger(b,
				cataloger);
		BrowserWindow newContentPane = new BrowserWindow(b, s, true);
		s.fillModel();

		frame.getContentPane().add(newContentPane);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * main used for offline testing of the UI
	 * 
	 */
	public static void main(String[] args) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
