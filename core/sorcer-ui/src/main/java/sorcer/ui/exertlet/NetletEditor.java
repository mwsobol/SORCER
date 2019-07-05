/*
 * Copyright 2014 the original author or authors.
 * Copyright 2014 SorcerSoft.org.
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
/**
 * @author Mike Sobolewski
 */
package sorcer.ui.exertlet;

import net.jini.core.lookup.ServiceItem;
import org.slf4j.Logger;
import sorcer.service.Exerter;
import sorcer.core.proxy.Outer;
import sorcer.ui.util.WindowUtilities;
import sorcer.util.Log;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.rmi.RemoteException;

public class NetletEditor extends JPanel { 
	
	static final long serialVersionUID = -8967394998372233193L;
	
	private final static Logger logger = Log.getTestLog();
	
	final static String DELETE_BUTTON_LABEL = "Delete";
	final static String RESET_BUTTON_LABEL = "Reset";
	private EditorView inViewer;
	private EditorView outViewer;
	private EditorView browser;
	private JTabbedPane tabbedPane;
	private ServiceItem item;
	private Exerter provider;

    public NetletEditor(Object obj) {
        super();
		getAccessibleContext().setAccessibleName("Netlet Editor");
    	WindowUtilities.setNativeLookAndFeel();
    	setLayout(new BorderLayout());
		try {
			this.item = (ServiceItem) obj;
			logger.info("ProviderUI>>impl.service:" + item.service);
			if (item.service instanceof Outer) {
				Object inner;
				inner = ((Outer) item.service).getInner();
				logger.info("ProviderUI>>inner:" + inner);
				// check if smart proxy's inner exported server does contain a
				// provider
				if (inner instanceof Outer)
					provider = (Exerter) ((Outer) inner).getInner();
				else
					// in this case provider contains a non exported server
					provider = (Exerter) inner;
			} else if (item.service instanceof Exerter)
				provider = (Exerter) item.service;
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		URL helpURL = getClass().getResource("netlets.html");
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Help", null,
				createHelpPanel(helpURL), "Help");
		tabbedPane.addTab("Browser", null,
				createBrowserPanel(null), "Browser");
		tabbedPane.addTab("Editor", null,
				createEditorPanel(null), "Editor");
		tabbedPane.setSelectedIndex(2);
		add(tabbedPane, BorderLayout.CENTER);
	}

    private JPanel createHelpPanel(URL url) {
		inViewer = new EditorView("" + url, false);
		return inViewer;
	}
    
    public JPanel createOutContextPanel(String text) {
		outViewer = new EditorView(null, false, false, false, false, true);
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
		browser = new EditorView(null, true, false, true, false, false);
		browser.setProvider(provider);
		if (text != null && text.length() >0)
			browser.setText(text);
		browser.setTabbedPane(tabbedPane);
		inViewer.setDelegate(browser);
		inViewer.setTabbedPane(tabbedPane);
		return browser;
	}

	private JPanel createEditorPanel(String text) {
		EditorView editor = new EditorView(text, false, true, true, true, false, null);
		tabbedPane.addTab("Editor", null, editor, "Editor");
		tabbedPane.setSelectedComponent(editor);
		editor.setTabbedPane(tabbedPane);
		return editor;
	}

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Disable boldface controls.
        UIManager.put("swing.boldMetal", Boolean.FALSE); 

        //Create and setValue up the window.
        JFrame frame = new JFrame("Subroutine Scripting");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and setValue up the content pane.
        NetletEditor newContentPane = new NetletEditor(null);
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
	
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

}
