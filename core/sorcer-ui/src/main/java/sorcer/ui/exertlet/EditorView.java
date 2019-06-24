/*
 * Copyright 2014 the original author or authors.
 * Copyright 2014 SorcerSoft.org.
 * Copyright 2015 SorcerSoft.com.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.Exerter;
import sorcer.core.provider.RemoteLogger;
import sorcer.core.provider.logger.LoggerRemoteException;
import sorcer.core.provider.logger.RemoteLoggerListener;
import sorcer.netlet.ServiceScripter;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.ui.util.JIconButton;
import sorcer.ui.util.TextAreaPrintStream;
import sorcer.ui.util.WindowUtilities;
import sorcer.util.IOUtils;
import sorcer.util.Sorcer;
import sorcer.util.SorcerUtil;
import sorcer.util.StringUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * HTML file browser and file editor
 */
public class EditorView extends JPanel implements HyperlinkListener {
	static final long serialVersionUID = 4473215204301624571L;
	
	private final static Logger logger = LoggerFactory.getLogger(EditorView.class);
	private static boolean debug = false;
	private final JFileChooser fileChooser = new JFileChooser(
			System.getProperty("sorcer.home"));
	private JIconButton homeButton;
	private JButton saveButton, openButton,
			saveAsButton, exertButton;

	private JTextField urlField;
	private JEditorPane editPane;
	private EditorView delegate;
	private String source;
	private JTabbedPane tabbedPane;
	private EditorView editor;
	private static String EDIT_LABEL = "Edit...";
	private static String OPEN_LABEL = "Open...";
	private static String SAVE_AS_LABEL = "Save As...";
	private static String SAVE_LABEL = "Save";
	private static String EXIT_LABEL = "Close";
	private static String EXERT_LABEL = "Exert...";
	private static String GET_CONTEXT_LABEL = "Get Context Template...";
	private JMenuItem openMenuItem, editMenuItem, saveMenuItem, saveAsMenuItem,
			getContextMenuItem, exertMenuItem, closeMenuItem;
	private Exerter provider;
	private EditorViewSignature model;
    private ServiceScripter scriptExerter;
	private JTextArea feedbackPane;
	private RemoteLoggerListener listener;

	public EditorView(String url, boolean withLocator) {
		this(url, withLocator, false);
		setDebug();
	}

	public EditorView(String url, boolean withLocator, boolean editable) {
		this(url, withLocator, editable, false, false, false, null);
		setDebug();
	}

	public EditorView(String input, boolean withLocator, boolean editable,
			boolean withEditing, boolean isEditor, boolean isDisposable) {
		this(input, withLocator, editable, withEditing, isEditor, isDisposable, null);
	}

	public EditorView(String input, boolean withLocator, boolean editable,
			boolean withEditing, boolean isEditor, boolean isDisposable, EditorViewSignature model) {
		this(input, withLocator, editable, withEditing, isEditor, isDisposable, model, null);
	}

	/** Creates new editor JPanel */
	public EditorView(String input, boolean withLocator, boolean editable,
			boolean withEditing, boolean isEditor, boolean isDisposable,
					  EditorViewSignature model, JTextArea feedbackArea) {
		setDebug();
		this.model = model;
		WindowUtilities.setNativeLookAndFeel();
		setLayout(new BorderLayout());
		boolean isURL = false;

		if (input!=null) {
			File ntlFile = new File(input);
			if (ntlFile.exists()) {
				try {
					input = ntlFile.toURI().toURL().toString();
				} catch (MalformedURLException e) {
					// ignoring - just won't open
				}
			}
		}

		if (input != null
				&& input.length() > 0
				&& (input.startsWith("http:") || input.startsWith("file:") || input
						.startsWith("jar:"))) {
			isURL = true;
		}
		this.source = input;
		
		EditActionListener actionListener = new EditActionListener();

		if (withLocator) {
			URL infoURL = getClass().getResource("icon-info16.png");
			JPanel topPanel = new JPanel();
			topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));
			topPanel.setBackground(Color.lightGray);
			if (infoURL != null) {
				homeButton = new JIconButton(infoURL);
				homeButton.addActionListener(actionListener);
			}
			JLabel urlLabel = new JLabel("URL:");
			urlField = new JTextField(24);
			urlField.setText(source);
			urlField.addActionListener(actionListener);
			if (homeButton != null)
				topPanel.add(homeButton);
			topPanel.add(urlLabel);
			topPanel.add(urlField);
			
			openButton = new JButton(OPEN_LABEL);
			openButton.setActionCommand(OPEN_LABEL);
			openButton.addActionListener(actionListener);
			topPanel.add(openButton);
			
			if (withEditing) {
				editMenuItem = new JMenuItem(EDIT_LABEL);
				add(topPanel, BorderLayout.NORTH);
			}
			exertMenuItem = new JMenuItem(EXERT_LABEL);
		}

		if (isEditor || isDisposable) {
			if (!isDisposable) {
				saveButton = new JButton(SAVE_LABEL);
				saveButton.setActionCommand(SAVE_LABEL);
				saveButton.addActionListener(actionListener);
				saveMenuItem = new JMenuItem(SAVE_LABEL);
			}
			saveAsButton = new JButton(SAVE_AS_LABEL);
			saveAsButton.setActionCommand(SAVE_AS_LABEL);
			saveAsButton.addActionListener(actionListener);
			saveAsMenuItem = new JMenuItem(SAVE_AS_LABEL);
			if (!isDisposable) {
				exertButton = new JButton(EXERT_LABEL);
				exertButton.setActionCommand(EXERT_LABEL);
				exertButton.addActionListener(actionListener);
				getContextMenuItem = new JMenuItem(GET_CONTEXT_LABEL);
				exertMenuItem = new JMenuItem(EXERT_LABEL);
			}

			JPanel bpanel = new JPanel();
			((FlowLayout) bpanel.getLayout()).setAlignment(FlowLayout.TRAILING);
			bpanel.add(saveAsButton);
			if (!isDisposable) {
				bpanel.add(saveButton);
				bpanel.add(exertButton);
			}
			add(bpanel, BorderLayout.SOUTH);
			feedbackPane = new JTextArea();
			feedbackPane.setEditable(false);
		}

		JPopupMenu popup = new JPopupMenu("Netlet Editor");

		// I don't see any reason to check disposable here
		// if (isEditor || !isDisposable || withLocator) {
		if (isEditor || withLocator) {
			openMenuItem = new JMenuItem(OPEN_LABEL);
			openMenuItem.setActionCommand(OPEN_LABEL); 
			openMenuItem.addActionListener(actionListener);
			popup.add(openMenuItem);
		}
			
		if (isEditor || isDisposable) {
			closeMenuItem = new JMenuItem(EXIT_LABEL);
			closeMenuItem.setActionCommand(EXIT_LABEL);
			closeMenuItem.addActionListener(actionListener);
			popup.add(closeMenuItem);
		}
				
		if (editMenuItem != null) {
			editMenuItem.setActionCommand(EDIT_LABEL);
			editMenuItem.addActionListener(actionListener);
			popup.add(editMenuItem);
		}
		
		if (closeMenuItem != null || editMenuItem != null)
			popup.addSeparator();

		if (saveMenuItem != null) {
			saveMenuItem.setActionCommand(SAVE_LABEL);
			saveMenuItem.addActionListener(actionListener);
			popup.add(saveMenuItem);
		}
		
		if (saveAsMenuItem != null) {
			saveAsMenuItem.setActionCommand(SAVE_AS_LABEL);
			saveAsMenuItem.addActionListener(actionListener);
			popup.add(saveAsMenuItem);
		}
		
		if ((saveMenuItem != null || saveAsMenuItem != null) && exertMenuItem != null)
			popup.addSeparator();
		
		if (getContextMenuItem != null) {
			getContextMenuItem.setActionCommand(GET_CONTEXT_LABEL);
			getContextMenuItem.addActionListener(actionListener);
			popup.add(getContextMenuItem);
		}
		
		if (exertMenuItem != null) {
			exertMenuItem.setActionCommand(EXERT_LABEL);
			exertMenuItem.addActionListener(actionListener);
			popup.add(exertMenuItem);
		}

		try {
			if (isURL) {
				editPane = new JEditorPane(source);
			}
			else if (source == null) {
				editPane = new JEditorPane();
			}
			else {
				editPane = new JEditorPane();
				editPane.setText(source);
			}

			editPane.setEditable(editable);
			editPane.addHyperlinkListener(this);
			editPane.addMouseListener(new PopupListener(popup));
			JScrollPane editScroll = new JScrollPane(editPane);

			if (feedbackArea != null) {
				// reuse feedbackPane from the parent editor view
				JScrollPane feedbackScroll = new JScrollPane(feedbackArea);
				add(feedbackScroll, BorderLayout.SOUTH);

				JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
						editScroll, feedbackScroll);
				splitPane.setResizeWeight(.9);

				this.add(splitPane, BorderLayout.CENTER);
			} else {
				add(editScroll, BorderLayout.CENTER);
			}
		} catch (IOException ioe) {
			warnUser("Can't build HTML pane for " + source + ": " + ioe);
		}
	}

	class PopupListener extends MouseAdapter {
		private JPopupMenu popupMenu;

		PopupListener(JPopupMenu popupMenu) {
			this.popupMenu = popupMenu;
		}

		/**
		 * Invoked when a mouse button has been pressed on a component.
		 */
		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}

		/**
		 * Invoked when a mouse button has been released on a component.
		 */
		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}

		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

	}

    private void createRemoteLoggerListener(Mogram target) {
        java.util.List<Map<String, String>> filterMapList = new ArrayList<Map<String, String>>();
        for (String exId : ((ServiceMogram) target).getAllMogramIds()) {
            Map<String, String> map = new HashMap<String, String>();
            map.put(RemoteLogger.KEY_MOGRAM_ID, exId);
            filterMapList.add(map);
        }
        if (!filterMapList.isEmpty()) {
            try {
                listener = new RemoteLoggerListener(filterMapList,
                                                    new PrintStream(new TextAreaPrintStream(feedbackPane)));
            } catch (LoggerRemoteException lre) {
                logger.warn("Remote logging disabled: " + lre.getMessage());
                listener = null;
            }
        }
    }

    class EditActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			String url;
			if (event.getSource() == urlField) {
				url = urlField.getText();
                try {
                    displayUrl(new URL(url));
                } catch (MalformedURLException me) {
                    warnUser("can't load: " + url);
                }
				return;
			}

			String command = event.getActionCommand();
			if (OPEN_LABEL.equals(command)) {
				openFile();
				return;
			} else if (EDIT_LABEL.equals(command)) {
				openEditor(urlField.getText());
				return;
			} else if (EXERT_LABEL.equals(command)) {
				String script = editPane.getText();
				// clearSessions the previous feedback
				feedbackPane.setText("");
				if (model != null) {
					runTaskScript(script);
				} else {
					try {
						scriptExerter = new ServiceScripter(script, System.out, this.getClass().getClassLoader(),
								Sorcer.getWebsterUrl());
						Object target = scriptExerter.interpret();
						// Create RemoteLoggerListener
						Object result = null;
						if (target instanceof Mogram) {
							createRemoteLoggerListener((Mogram) target);
							result = scriptExerter.execute();
						}
						if (result instanceof Mogram) {
							processMogram((Mogram) result);
						} else if (result != null) {
							openOutPanel(result.toString());
                        } else {
							openOutPanel(target.toString());
						}
                    } catch (IOException io) {
                        logger.error("Caught exception while executing script: " + io.getMessage());
                        openOutPanel(StringUtils.stackTraceToString(io));
                    } catch (Throwable th) {
                        openOutPanel(StringUtils.stackTraceToString(th));
                        logger.error("Caught exception while executing script: " + th.getMessage());
                    }
				}
				return;
			} else if (SAVE_LABEL.equals(command)) {
				saveFile();
				return;
			} else if (SAVE_AS_LABEL.equals(command)) {
				saveAsFile();
				return;
			} else if (EXIT_LABEL.equals(command)) {
				// tabbedPane.remove(tabbedPane.getTabCount()-1);
				tabbedPane.remove(EditorView.this);
				if (listener != null)
					listener.destroy();

				return;
			} else if (GET_CONTEXT_LABEL.equals(command)) {
				try {
					getContextFromProvider();
				} catch (RemoteException e) {
					openEditor(SorcerUtil.stackTraceToString(e));
					logger.warn("Error while getting context provider", e);
				}
				return;
			}
			// Clicked "home" button instead of entering URL
			url = "http://sorcersoft.org";
            try {
			    displayUrl(new URL(url));
            } catch (MalformedURLException me) {
                warnUser("can't load: " + url);
            }
		}
	}

	private void openFile() {
		int returnVal = fileChooser.showOpenDialog(EditorView.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (file != null) {
                try {
    				displayUrl(file.toURI().toURL());
                } catch (MalformedURLException me) {
                    warnUser("Can't open file: " + file.getPath() + ": " + me);
                }
			}
		}
	}

	private void saveAsFile() {
		int returnVal = fileChooser.showSaveDialog(EditorView.this);
		BufferedWriter br = null;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (file != null) {
				try {
					String content = editPane.getText();
					br = new BufferedWriter(new FileWriter(file));
					br.write(content);
					br.flush();
					br.close();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this, "File Not Saved",
							"ERROR", JOptionPane.ERROR_MESSAGE);
					logger.warn("Error saving file {}", file, e);
				} finally {
					IOUtils.closeQuietly(br);
				}
			}
		}
	}

	private void runTaskScript(String script) {
		System.out.println("task: \n" + script);

		String serviceType = model.getServiceType();
		String selector = model.getSelector();
		URL[] codebaseURLs = model.getCodebaseURLs();
		URLClassLoader taskClassLoader = new URLClassLoader(codebaseURLs, this.getClass().getClassLoader());

		StringBuilder sb = new StringBuilder();
		sb.append("\nimport ").append(
				serviceType).append(";\n").append("task(sig(\"")
				.append(selector).append("\",").append(serviceType).append(
				".class),\n").append(script).append(");");

		logger.info(">>> executing task script: " + sb.toString());
		try {
			scriptExerter = new ServiceScripter(sb.toString(), System.out, taskClassLoader, Sorcer.getWebsterUrl());
			Object target = scriptExerter.interpret();
			// Create RemoteLoggerListener
			RemoteLoggerListener listener = null;
			Object result = null;
			if (target instanceof Mogram) {
				createRemoteLoggerListener((Mogram) target);
				result = scriptExerter.execute();
			}
			if (result instanceof Routine)
				processMogram((Routine) result);
			else if (result != null) {
				logger.debug("<< executing scrip: " + script);
				logger.debug(">> scrip result: " + script);
				openOutPanel(result.toString());
				showResults(result);
			}
			if (listener != null) listener.destroy();
		} catch (IOException io) {
			logger.error("Caught exception while executing script: " + io.getMessage());
			openOutPanel(StringUtils.stackTraceToString(io));
		} catch (Throwable th) {
			openOutPanel(StringUtils.stackTraceToString(th));
			logger.error("Caught exception while executing script: " + th.getMessage());
		}
	}

	private void processMogram(Mogram mogram) throws MogramException{
		String codebase = System.getProperty("java.rmi.server.codebase");
		logger.debug("Using exertlet codebase: " + codebase);
		if (mogram.getStatus() == Exec.DONE) {
			showResults(mogram);
			return;
		}
		boolean done = false;
		Mogram out = null;
		if (mogram instanceof Model) {
			try {
				out = (Context) ((Model)mogram).getResponse();
			} catch (Exception e) {
				throw new MogramException(e);
			}
			showResults(out);
			return;
		}

		try {
			if (provider != null) {
                Class<?>[] interfaces = provider.getClass().getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    if (interfaces[i] == mogram.getProcessSignature()
                            .getServiceType()) {
                        out = provider.exert(mogram, null);
                        //logger.debug(">>> done by " + provider);
                        done = true;
                        break;
                    }
                }
                if (!done) {
                    logger.debug(">> executing by exert: " + mogram.getName());
                    // inspect class loader tree
                    //com.sun.jini.start.ClassLoaderUtil.displayContextClassLoaderTree();
                    out = mogram.exert();
                }
            } else {
                out = mogram.exert();
            }
		} catch (RemoteException | SignatureException | RoutineException e) {
			openOutPanel(SorcerUtil.stackTraceToString(e));
			logger.warn("Error while processing mogram", e);
			return;
		}
		showResults(out);
	}

	private void showResults(Object mogram)  throws ContextException {
		if (mogram == null) {
			openOutPanel("Failed to compute the sorcer.netlet!");
			return;
		}
		if (mogram instanceof Routine) {
			Routine exertion = (Routine)mogram;
			try {
				if (exertion.getExceptions().size() > 0) {
                    openOutPanel(exertion.getExceptions().toString());
                } else {
                    StringBuilder sb = new StringBuilder(exertion.getContext().toString());
                    if (debug) {
                        sb.append("\n");
                        sb.append(((ServiceRoutine) exertion).getControlInfo().toString());
                    }
                    openOutPanel(sb.toString());
                }
			} catch (RemoteException e) {
				throw new ContextException(e);
			}
		} else {
			openOutPanel(mogram.toString());
		}
	}
	
	private void saveFile() {
		BufferedWriter br = null;
		String fn = source;
		if (source != null && source.length() != 0) {
			int index = source.indexOf("file://");
			if (index == 0)
				fn = source.substring("file://".length());
		}
		File file = new File(fn);
		try {
			String content = editPane.getText();
			br = new BufferedWriter(new FileWriter(file));
			br.write(content);
		} catch (IOException e) {
			logger.warn("Error saving file",e);
			warnUser(e.getMessage());
		} finally {
			IOUtils.closeQuietly(br);
		}
	}

	private void displayUrl(URL url) {
		if (url == null)
			return;

		try {
			editPane.setPage(url);
			if (urlField != null)
				urlField.setText(url.getPath());
		} catch (IOException ioe) {
			warnUser("Can't follow link to " + url + ": " + ioe);
		}
	}

	private void openEditor(String url) {
		editor = new EditorView(url, false, true, true, true, false, model);
		editor.provider = provider;
		editor.setTabbedPane(tabbedPane);
		tabbedPane.addTab("Editor", null, editor, "Editor");
		tabbedPane.setSelectedComponent(editor);
	}

	private void getContextFromProvider() throws RemoteException {
		String script = ((ContextManagement)provider).getContextScript();

		if (script == null || script.length() == 0) {
			script = "No context script availble from the provider: \n" + provider;
		}
		editor = new EditorView(script, false, true, true, true, false, model);
		editor.provider = provider;
		editor.setTabbedPane(tabbedPane);
		tabbedPane.addTab("Editor", null, editor, "Editor");
		tabbedPane.setSelectedComponent(editor);
	}
	
	private void openOutPanel(String text) {
		JTextArea editorFeedbackPane = feedbackPane;
		editor = new EditorView(text, false, false, false, false, true, model, editorFeedbackPane);
		editor.provider = provider;
		editor.setTabbedPane(tabbedPane);
		tabbedPane.addTab("Output", null, editor, "Mogram");
		tabbedPane.setSelectedComponent(editor);
	}

	public void setText(String content) {
		editPane.setContentType("text/html");
		editPane.setText(content);
	}

	public void hyperlinkUpdate(HyperlinkEvent event) {
		if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			try {
				if (delegate != null) {
					tabbedPane.setSelectedComponent(delegate);
					delegate.getHtmlPane().setPage(event.getURL());
					if (delegate.getUrlField() != null)
						delegate.getUrlField().setText(
								event.getURL().toExternalForm());
				} else {
					editPane.setPage(event.getURL());
					if (urlField != null)
						urlField.setText(event.getURL().toExternalForm());
				}
			} catch (IOException ioe) {
				warnUser("Can't follow link to "
						+ event.getURL().toExternalForm() + ": " + ioe);
			}
		}
	}

	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}

	public void setTabbedPane(JTabbedPane tabbedPane) {
		this.tabbedPane = tabbedPane;
	}

	private void warnUser(String message) {
		JOptionPane.showMessageDialog(this, message, "Error",
				JOptionPane.ERROR_MESSAGE);
	}

	public void setDelegate(EditorView delegate) {
		this.delegate = delegate;
	}

	public EditorView getDelegate() {
		return delegate;
	}

	public JTextField getUrlField() {
		return urlField;
	}

	public void setUrlField(JTextField urlField) {
		this.urlField = urlField;
	}

	public JEditorPane getHtmlPane() {
		return editPane;
	}

	public void setHtmlPane(JEditorPane htmlPane) {
		this.editPane = htmlPane;
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private static void createAndShowGUI() {
		// Disable boldface controls.
		UIManager.put("swing.boldMetal", Boolean.FALSE);

		// Create and setValue up the window.
		JFrame frame = new JFrame("Context Path Browser");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// Create and setValue up the content pane.
		EditorView pane = new EditorView("http://localhost", false);
		pane.setOpaque(true); // content panes must be opaque
		frame.setContentPane(pane);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
	
	public void setProvider(Exerter provider) {
		this.provider = provider;
	}
	
	private void setDebug() {
		String debugProperty = System.getProperty("exertlet.ui.debug");
		if (debugProperty!=null && (debugProperty.toLowerCase().equals("true")
				|| debugProperty.toLowerCase().equals("yes")))
			debug = true;
	}
}
