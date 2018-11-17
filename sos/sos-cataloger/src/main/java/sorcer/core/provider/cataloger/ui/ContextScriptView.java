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

import java.awt.BorderLayout;
import java.rmi.RMISecurityManager;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import sorcer.core.provider.Provider;
import sorcer.ui.exertlet.EditorView;

/**
 * This class implements the Context editing window, which is used in the
 * Provider UI.
 */
public class ContextScriptView extends JPanel {

	/**
	 * Private copy of the context used for various operations, like load other
	 * and save as.
	 */	
	private EditorView browser;
	private EditorView inViewer;
	private EditorView outViewer;
	private Provider provider; 
	private BrowserModel model;
	/**
	 * Tabbed pane to hold the input and output ContextTrees
	 */
	private JTabbedPane tabbedPane;
	
	/**
	 * Constructor for creating the context view panel
	 * 
	 * @param dispatcher
	 *            Remote explorer to use.
	 */
	public ContextScriptView(SignatureDispatchment dispatcher, BrowserModel model) {
		super(new BorderLayout());
		// needed for exerting the service
		this.model = model;
		if (System.getSecurityManager() == null)
			System.setSecurityManager(new RMISecurityManager());

		if (dispatcher instanceof SignatureDispatcherForProvider)
			provider = ((SignatureDispatcherForProvider)dispatcher).getProvider();
		else
			provider = ((SignatureDispatcherForCataloger)dispatcher).getProvider();
		
		tabbedPane = new JTabbedPane();

		inViewer = new EditorView("", false);
		
		tabbedPane.addTab("Browser", null,
				createBrowserPanel(null), "Browser");
		add(tabbedPane, BorderLayout.CENTER);
	}

	  public JPanel createOutContextPanel(String text) {
			outViewer = new EditorView(null, false, false, false, false, true, model);
			outViewer.setProvider(provider);
			outViewer.setText(text);
			outViewer.setTabbedPane(tabbedPane);
			outViewer.setDelegate(browser);
			tabbedPane.addTab("Out Viewer", null,
					outViewer, "Out Viewer");
			tabbedPane.setSelectedComponent(outViewer);
			return outViewer;
		}
	  
	   private JPanel createBrowserPanel(String text) {
			browser = new EditorView(null, true, false, true, false, false, model);
			browser.setProvider(provider);
			if (text != null && text.length() >0)
				browser.setText(text);
			browser.setTabbedPane(tabbedPane);
			inViewer.setDelegate(browser);
			inViewer.setTabbedPane(tabbedPane);
			return browser;
		}

}
