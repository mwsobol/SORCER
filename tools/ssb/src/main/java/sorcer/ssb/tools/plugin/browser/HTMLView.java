/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

package sorcer.ssb.tools.plugin.browser;

/*
 *
 * Created on 04 June 2002, 10:11
 */

/**
 *
 * @author  Phil
 * @version 
 */
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

public class HTMLView extends JPanel implements HyperlinkListener {

	// private static final Dimension PREF_SIZE=new Dimension(200,100);
	private JEditorPane _jep;
	private URL _startUrl;
	private ArrayList _history = new ArrayList();
	private int _cursor = 0;
	// private JMenuItem _back=new JMenuItem("<< Back");
	// private JMenuItem _fwd=new JMenuItem("Forward >>");
	private JScrollPane _sp;
	private URL _lastUrl;

	/** Creates new HTMLView */
	public HTMLView(String fileName) throws Exception {

		ClassLoader cl = getClass().getClassLoader();
		URL url = cl.getResource(fileName);
		init(url);

	}

	public HTMLView(URL url) throws Exception {

		_startUrl = url;

		init(url);

		// setFrameIcon(TreeRenderer._htmlFileIcon);
	}

	public void addKeyListener(KeyListener kl) {
		_jep.addKeyListener(kl);
	}

	public void update(URL newUrl) throws Exception {

		removeAll();
		init(newUrl);
		invalidate();
		if (getParent() != null) {
			getParent().validate();
		}
	}

	private void init(final URL url) throws Exception {
		setLayout(new BorderLayout());
		_history.add(url);
		_jep = new JEditorPane(url);

		_jep.setEditable(false);

		// HTMLDocument doc=(HTMLDocument)_jep.getDocument();
		// HTMLDocument.Iterator iter=doc.getIterator(HTML.Tag.SCRIPT);

		// System.out.println(iter.getStartOffset());

		_jep.addHyperlinkListener(this);

		_sp = new JScrollPane(_jep);
		add(createToolbar(), BorderLayout.NORTH);
		add(_sp, BorderLayout.CENTER);
		load(url);
	}

	private JComponent createToolbar() {

		Action homeAction = new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				_cursor = 0;
				URL url = (URL) _history.get(_cursor);
				load(url);

			}
		};
		Action backAction = new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				if (_cursor == 0) {
					_cursor = _history.size();
				}
				_cursor--;
				URL url = (URL) _history.get(_cursor);
				// System.out.println("<< "+_cursor+" "+url);
				load(url);
			}
		};
		Action fwdAction = new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				_cursor++;
				if (_cursor > _history.size() - 1) {
					_cursor = 0;
				}
				URL url = (URL) _history.get(_cursor);
				load(url);
				// System.out.println(">> "+_cursor+" "+url);
			}
		};
		/*
		 * JPanel p=new JPanel(); p.setLayout( new FlowLayout(FlowLayout.LEFT));
		 * p.add(_back); p.add(_fwd); return p;
		 */
		JToolBar tb = new JToolBar();
		tb.setFloatable(false);

		JButton home = tb.add(homeAction);
		home.setToolTipText("Home");
		home.setIcon(TreeRenderer._homeIcon);
		tb.addSeparator();
		JButton back = tb.add(backAction);
		back.setToolTipText("<< back");
		back.setIcon(TreeRenderer._backIcon);
		JButton fwd = tb.add(fwdAction);
		fwd.setToolTipText("Foward >>");
		fwd.setIcon(TreeRenderer._fwdIcon);
		return tb;
	}

	// public Dimension getPreferredSize(){
	// return PREF_SIZE;
	// }
	public void hyperlinkUpdate(HyperlinkEvent e) {

		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			URL newUrl = e.getURL();
			// if this is a change from frames to no frames
			// then zap the whole document - assume that when the orginal url is
			// selected then reload
			if (newUrl.equals(_startUrl)) {
				_jep = new JEditorPane();
				load(_startUrl);
				return;
			}
			JEditorPane pane = (JEditorPane) e.getSource();
			if (e instanceof HTMLFrameHyperlinkEvent) {
				HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
				HTMLDocument doc = (HTMLDocument) pane.getDocument();
				doc.processHTMLFrameHyperlinkEvent(evt);

			} else {
				try {

					if (_lastUrl == null || !newUrl.equals(_lastUrl)) {
						// might need to move the line below out of this
						// condition check
						pane.setPage(newUrl);

						_lastUrl = newUrl;
						_cursor++;
						if (_cursor < _history.size()) {
							_history.add(_cursor, newUrl);
							// System.out.println("Inserting page at "+_cursor+" "+newUrl);
							// _fwd.setEnabled(true);
						} else {
							_history.add(newUrl);
							// System.out.println("Added page at "+_cursor+" "+newUrl);
						}
					}

				} catch (IOException ex) {
					pane.setText("<html><body>Unable to load file " + newUrl
							+ "</body></html>");
				}
			}

		}
	}

	private void load(final URL url) {

		remove(_sp);
		try {
			_jep.setEditable(false);
			_jep.addHyperlinkListener(this);

			_sp = new JScrollPane(_jep);

			add(_sp, BorderLayout.CENTER);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						_jep.setPage(url);
						invalidate();
						if (getParent() != null) {
							getParent().validate();
						}

					} catch (Exception ex) {

						try {
							_jep.setText("Error loading " + url);
						} catch (Exception e) {
						}
					}
				}
			});

		} catch (Exception ex) {
			add(new JLabel("Unable to display " + url));
		}

	}
	/*
	 * private class HyperJump implements Runnable { private URL url;
	 * 
	 * public HyperJump(URL file) { //System.out.println(file); url=file; }
	 * public void run() {
	 * 
	 * try { String html=parseHTML(url); _jep.setText(html);
	 * setTitle(url.toString()); //_jep.setPage(url); } catch (Exception ex) {
	 * System.out.println(ex); } finally { _jep.invalidate(); validate(); } }
	 * 
	 * }
	 */
}
