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
package sorcer.ui.exertlet;

import groovy.lang.GroovyShell;
import net.jini.core.transaction.TransactionException;
import sorcer.core.provider.Provider;
import sorcer.netlet.ScriptExerter;
import sorcer.service.*;
import sorcer.ui.util.JIconButton;
import sorcer.ui.util.WindowUtilities;
import sorcer.util.Sorcer;
import sorcer.util.SorcerUtil;
import sorcer.util.StringUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
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
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static sorcer.util.StringUtils.tName;

/**
 * HTML file browser and file editor
 */
public class EditorView extends JPanel implements HyperlinkListener {
	static final long serialVersionUID = 4473215204301624571L;
	
	private final static Logger logger = LoggerFactory.getLogger(EditorView.class
			.getName());
	private static boolean debug = false;
	private final JFileChooser fileChooser = new JFileChooser(
			System.getProperty("sorcer.home"));
	private JIconButton homeButton;
	private JButton editButton, saveButton, openButton,
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
	private JMenuItem openMenuItem, editMenuItem, saveMenuItem, saveAsMenuItem, getContextMenuItem, exertMenuItem, closeMenuItem;
	private Provider provider;
	private GroovyShell shell;
	private EditorViewSignature model;
	private static StringBuilder staticImports;
    private ScriptExerter scriptExerter;

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
	
	/** Creates new editor JPanel */
	public EditorView(String input, boolean withLocator, boolean editable,
			boolean withEditing, boolean isEditor, boolean isDisposable, EditorViewSignature model) {
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
		// get static imports for exertlets
		if (staticImports == null) {
			staticImports = readTextFromJar("static-imports.txt");
			//System.out.println("get staticImports: " + staticImports.toString());
		}
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
//			if (withEditing) {
//				editButton = new JButton(EDIT_BUTTON_LABEL);
//				editButton.setActionCommand(EDIT_BUTTON_LABEL);
//				editButton.addActionListener(actionListener);
//				editMenuItem = new JMenuItem(EDIT_BUTTON_LABEL);
//				topPanel.add(editButton);
//				add(topPanel, BorderLayout.NORTH);
//			}
//			exertButton = new JButton(EXERT_BUTTON_LABEL);
//			exertButton.setActionCommand(EXERT_BUTTON_LABEL);
//			exertButton.addActionListener(actionListener);
//			topPanel.add(exertButton);
			exertMenuItem = new JMenuItem(EXERT_LABEL);
		}

		if (isEditor || isDisposable) {
//			exitButton = new JButton(EXIT_BUTTON_LABEL);
//			exitButton.setActionCommand(EXIT_BUTTON_LABEL);
//			exitButton.addActionListener(new BtnActionListener());
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
			//bpanel.add(exitButton);
			bpanel.add(saveAsButton);
			if (!isDisposable) {
				bpanel.add(saveButton);
				bpanel.add(exertButton);
			}
			add(bpanel, BorderLayout.SOUTH);
		}

		JPopupMenu popup = new JPopupMenu("Netlet Editor");
	
		if (isEditor || !isDisposable || withLocator) {
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

			// change font size in HTML docs
			// SimpleAttributeSet attr = new SimpleAttributeSet();
			// attr.addAttribute(StyleConstants.FontSize, new Integer(10));
			// htmlPane.getStyledDocument().setCharacterAttributes(0,htmlPane.
			// getDocument
			// ().getText(0,htmlPane.getDocument().getLength()),attr,false);

			// HTMLEditorKit kit = new HTMLEditorKit();
			// MutableAttributeSet set = kit.getInputAttributes();
			// HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
			// StyleConstants.setFontSize(set, 8);
			// doc.setCharacterAttributes(0, doc.getLength(), set, false);
			// htmlPane.setEditorKit(kit);

			editPane.addMouseListener(new PopupListener(popup));
			JScrollPane scrollPane = new JScrollPane(editPane);
			add(scrollPane, BorderLayout.CENTER);
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
				if (model != null) {
					runTaskScript(script);
				} else {
					StringBuilder sb = new StringBuilder(
							staticImports.toString());
					Scanner scanner = new Scanner(script);
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine().trim();
						if (line.length() > 0 && line.charAt(0) != '#') {
							sb.append(line);
						}
						sb.append("\n");
					}
					logger.debug(">>> executing script: " + sb.toString());
                    try {
						scriptExerter = new ScriptExerter(sb.toString(), System.out,this.getClass().getClassLoader(), Sorcer.getWebsterUrl().toString());
                        scriptExerter.parse();
                        Object result = scriptExerter.execute();
                        if (result instanceof Exertion)
                            processExerion((Exertion) result);
                        else if (result != null) {
                            openOutPanel(result.toString());
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
				return;
			} else if (GET_CONTEXT_LABEL.equals(command)) {
				try {
					getContextFromProvider();
				} catch (RemoteException e) {
					openEditor(SorcerUtil.stackTraceToString(e));
					e.printStackTrace();
				}
				return;
			}
			// Clicked "home" button instead of entering URL
			// url = initialURL;
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
		BufferedWriter br = null;
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
				//logger.info("Saving edited file as: " + file);
				try {
					String content = editPane.getText();
					br = new BufferedWriter(new FileWriter(file));
					br.write(content);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this, "File Not Saved",
							"ERROR", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				} finally {
					if (br != null)
						try {
							br.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
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
		sb.append(staticImports.toString()).append("\nimport ").append(
				serviceType).append(";\n").append("task(sig(\"")
				.append(selector).append("\",").append(serviceType).append(
						".class),\n").append(script).append(");");

		logger.info(">>> executing task script: " + sb.toString());
		try {
			scriptExerter = new ScriptExerter(sb.toString(), System.out, taskClassLoader, Sorcer.getWebsterUrl().toString());
			scriptExerter.parse();
			Object result = scriptExerter.execute();
			if (result instanceof Exertion)
				processExerion((Exertion) result);
			else if (result != null) {
				openOutPanel(result.toString());
			}
		} catch (IOException io) {
			logger.error("Caught exception while executing script: " + io.getMessage());
			openOutPanel(StringUtils.stackTraceToString(io));
		} catch (Throwable th) {
			openOutPanel(StringUtils.stackTraceToString(th));
			logger.error("Caught exception while executing script: " + th.getMessage());
		}
	}

	private void processExerion(Exertion exertion) throws ContextException{
		String codebase = System.getProperty("java.rmi.server.codebase");
		logger.debug("Using exertlet codebase: " + codebase);
		
		if (((ServiceExertion) exertion).getStatus() == Exec.DONE) {
		//logger.debug(">>> done by Groovy Shell");
		showResults(exertion);
		return;
		}
		Exertion out = null;
		boolean done = false;
		try {
			Class<?>[] interfaces = provider.getClass().getInterfaces();
			//logger.debug(">>> signature: " + exertion.getProcessSignature());
			//logger.debug(">>> interfaces: " + Arrays.toString(interfaces));
			for (int i = 0; i < interfaces.length; i++) {
				if (interfaces[i] == exertion.getProcessSignature()
						.getServiceType()) {
					out = provider.service(exertion, null);
					//logger.debug(">>> done by " + provider);
					done = true;
					break;
				}
			}
			if (!done) {
				logger.debug(">> executing by exert: " + exertion.getName());
				// inspect class loader tree
				com.sun.jini.start.ClassLoaderUtil.displayContextClassLoaderTree();
//				com.sun.jini.start.ClassLoaderUtil.displayClassLoaderTree(exertion
//						 .getClass().getClassLoader());

				out = exertion.exert();
				//logger.debug(">>> done by SSB");
			}
		} catch (RemoteException e) {
			openOutPanel(SorcerUtil.stackTraceToString(e));
			e.printStackTrace();
			return;
		} catch (TransactionException e) {
			openOutPanel(SorcerUtil.stackTraceToString(e));
			e.printStackTrace();
			return;
		} catch (ExertionException e) {
			openOutPanel(SorcerUtil.stackTraceToString(e));
			e.printStackTrace();
			return;
		}
		showResults(out);
	}

	private void showResults(Exertion exertion)  throws ContextException {
		if (exertion == null) {
			openOutPanel("Failed to process the exertlet!");
			return;
		}
		if (exertion.getExceptions().size() > 0) {
			openOutPanel(exertion.getExceptions().toString());
		}
		else {
			StringBuilder sb = new StringBuilder(exertion.getContext().toString());
			if (debug) {
				sb.append("\n");
				sb.append(((ServiceExertion)exertion).getControlInfo().toString());
			}
			openOutPanel(sb.toString());
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
		//logger.info("Saving edited file: " + fn);
		File file = new File(fn);
		try {
			String content = editPane.getText();
			br = new BufferedWriter(new FileWriter(file));
			br.write(content);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
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
		if (model != null)
			editor = new EditorView(url, false, true, true, true, false, model);
		else
			editor = new EditorView(url, false, true, true, true, false);
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
		if (model != null)
			editor = new EditorView(script, false, true, true, true, false, model);
		else
			editor = new EditorView(script, false, true, true, true, false);
		editor.provider = provider;
		editor.setTabbedPane(tabbedPane);
		tabbedPane.addTab("Editor", null, editor, "Editor");
		tabbedPane.setSelectedComponent(editor);
	}
	
	private void openOutPanel(String text) {
		if (model != null)
			editor = new EditorView(text, false, false, false, false, true, model);
		else
			editor = new EditorView(text, false, false, false, false, true);
		editor.provider = provider;
		editor.setTabbedPane(tabbedPane);
		tabbedPane.addTab("Output", null, editor, "Exertion");
		tabbedPane.setSelectedComponent(editor);
	}

	public void setText(String content) {
		//logger.info("content type: " + htmlPane.getContentType());
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

	class Hyperactive implements HyperlinkListener {

		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				JEditorPane pane = (JEditorPane) e.getSource();
				if (e instanceof HTMLFrameHyperlinkEvent) {
					HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
					HTMLDocument doc = (HTMLDocument) pane.getDocument();
					doc.processHTMLFrameHyperlinkEvent(evt);
				} else {
					try {
						pane.setPage(e.getURL());
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
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

	private StringBuilder readTextFromJar(String filename) {
		InputStream is = null;
		BufferedReader br = null;
		String line;
		StringBuilder sb = new StringBuilder();;

		try {
			is = getClass().getResourceAsStream(filename);
			if (is != null) {
				br = new BufferedReader(new InputStreamReader(is));
				while (null != (line = br.readLine())) {
					sb.append(line);
					sb.append("\n");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
				if (is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb;
	}
	
	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private static void createAndShowGUI() {
		// Disable boldface controls.
		UIManager.put("swing.boldMetal", Boolean.FALSE);

		// Create and set up the window.
		JFrame frame = new JFrame("Context Path Browser");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create and set up the content pane.
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
	
	public void setProvider(Provider provider) {
		this.provider = provider;
	}
	
	private void setDebug() {
		String debugProperty = System.getProperty("exertlet.ui.debug");
		if (debugProperty!=null && (debugProperty.toLowerCase().equals("true")
				|| debugProperty.toLowerCase().equals("yes")))
			debug = true;
	}
}
