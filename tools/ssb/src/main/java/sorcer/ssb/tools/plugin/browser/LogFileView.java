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
 * Created Mon Nov 22 06:24:43 GMT 2004
 */
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;

public class LogFileView extends JFrame {
	private JTextArea _output = new JTextArea();
	private Thread _updater;
	private long _lastModifed;

	public LogFileView() throws Exception {
		super("Log file");

		_output.setEditable(false);
		setIconImage(TreeRenderer._frameIcon.getImage());
		refresh();
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				if (_updater != null) {
					_updater.interrupt();
				}
				dispose();
			}
		});

		getContentPane().add(new JScrollPane(_output), BorderLayout.CENTER);
		_updater = new Thread() {
			public void run() {
				while (!isInterrupted()) {

					try {
						sleep(1000);
						refresh();
					} catch (InterruptedException ex) {
						// System.out.println("Updater thread interrupted, exiting");
						return;
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(LogFileView.this, ex,
								"SSB", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
			}
		};
		_updater.start();
	}

	private void refresh() throws Exception {

		System.out.flush();

		File f = new File(ServiceBrowserConfig.LOG_FILE);
		long lastModified = f.lastModified();
		if (lastModified == _lastModifed) {
			// return;
			// System.out.println("Log file not modified");
		}
		_lastModifed = lastModified;
		final String txt = loadTextFile(ServiceBrowserConfig.LOG_FILE);
		final Document doc = _output.getDocument();
		if (txt.length() == doc.getLength()) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					_output.setText(txt);

					_output.setCaretPosition(doc.getLength());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

	}

	private String loadTextFile(String src) throws Exception {

		FileReader fis = new FileReader(src);
		BufferedReader br = new BufferedReader(fis);
		StringBuffer buf = new StringBuffer();
		String ln = br.readLine();
		while (ln != null) {
			buf.append(ln);
			buf.append("\n");
			ln = br.readLine();
		}
		br.close();
		return buf.toString();

	}
}
