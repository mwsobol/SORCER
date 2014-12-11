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
 * Created Sun Mar 06 08:27:31 GMT 2005
 */
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

public class JarView extends JPanel implements Runnable {

	private URL url;
	private JTabbedPane tabbedPane = new JTabbedPane();
	private String[] classpathURLs;
	private boolean done;
	private Object lock = new Object();

	public JarView(URL codebase) {
		url = codebase;

		setLayout(new BorderLayout());
		add(tabbedPane, BorderLayout.CENTER);

		JLabel title = new JLabel(url.toString());
		Font font = title.getFont();
		title.setFont(new Font(font.getFamily(), Font.BOLD, font.getSize() + 1));
		title.setIcon(TreeRenderer._jarIcon);
		add(title, BorderLayout.NORTH);
		if (!url.toString().endsWith("/")) {
			new Thread(this).start();
		}
	}

	public void run() {
		try {
			File jarDir = new File("tmp");
			jarDir.mkdirs();
			File tmpJar = new File(jarDir.getAbsolutePath() + File.separator+ "tmp.jar");
			FileOutputStream fos = new FileOutputStream(tmpJar);

			InputStream is = url.openStream();
			byte[] b = new byte[4096];
			int nb = is.read(b);
			while (nb != -1) {
				fos.write(b, 0, nb);
				nb = is.read(b);
			}
			is.close();
			fos.close();

			final Map metaContents = new HashMap();

			StringBuffer buf = new StringBuffer();
			JarFile jarFile = new JarFile(tmpJar);
			// ArrayList list=new ArrayList();
			Enumeration iter = jarFile.entries();
			while (iter.hasMoreElements()) {
				JarEntry je = (JarEntry) iter.nextElement();
				String fname = je.getName();
				String fnameLc = fname.toUpperCase();
				// list.add(je.getName());
				buf.append(fname);
				buf.append("\n");
				if (!je.isDirectory() && fnameLc.startsWith("META-INF")) {
					try {
						String info = getMetaInfo(jarFile.getInputStream(je));
						String title = fname
								.substring(fname.lastIndexOf("/") + 1);
						metaContents.put(title, info);
						if (title.toLowerCase().equals("manifest.mf")) {
							setClasspathUrls(jarFile.getInputStream(je));
						}
					} catch (Exception ex) {
						System.err.println("Caught Exception: "
								+ ex.getClass().getName() + "; Msg: "
								+ ex.getMessage());
					}

				}
			}
			jarFile.close();

			final JEditorPane output = new JEditorPane();
			output.setEditable(false);
			output.setText(buf.toString());
			/*
			 * Object [] array=new Object[list.size()]; list.toArray(array);
			 * 
			 * final JList jlist=new JList(array);
			 */
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					tabbedPane.add("Contents", new JScrollPane(output));
					Iterator iter = metaContents.keySet().iterator();
					while (iter.hasNext()) {
						String name = (String) iter.next();
						String contents = (String) metaContents.get(name);
						JEditorPane ep = new JEditorPane();
						ep.setEditable(false);
						ep.setText(contents);
						tabbedPane.add(name, new JScrollPane(ep));
					}
					getParent().validate();
				}
			});

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			synchronized (lock) {
				done = true;
				lock.notifyAll();
			}
		}
	}

	private String getMetaInfo(InputStream is) throws Exception {
		StringBuffer buf = new StringBuffer();
		byte b[] = new byte[2048];
		for (int nBytes = is.read(b); nBytes != -1; nBytes = is.read(b)) {
			buf.append(new String(b, 0, nBytes));
		}
		is.close();
		return buf.toString();
	}

	/*
	 * private void setClasspathUrls(String manifest){
	 * 
	 * String TAG="Class-Path: "; String [] ln=manifest.split("\n"); for(int
	 * i=0;i<ln.length;i++){ if(ln[i].startsWith(TAG)){ //check that there is
	 * actually an entry here if(TAG.length()+1<ln[i].length()){
	 * 
	 * String urls=ln[i].substring(TAG.length());
	 * 
	 * System.out.println(ln[i]);
	 * 
	 * classpathURLs=urls.split(" "); return; } } }
	 * 
	 * }
	 */
	private void setClasspathUrls(InputStream is) {

		try {
			Manifest manifest = new Manifest(is);

			Attributes.Name cpName = new Attributes.Name("Class-Path");

			Attributes atts = manifest.getMainAttributes();
			Iterator iter = atts.keySet().iterator();
			while (iter.hasNext()) {
				Attributes.Name att = (Attributes.Name) iter.next();
				if (att.equals(cpName)) {
					String cp = atts.getValue(cpName);

					System.out.println(cpName + " " + cp);

					classpathURLs = cp.split(" ");
					break;
				}

			}

		} catch (Exception ex) {
			System.err.println("Caught Exception: " + ex.getClass().getName()
					+ "; Msg: " + ex.getMessage());
			ex.printStackTrace();
		}

	}

	public String[] getClasspathURLs() {
		synchronized (lock) {
			while (!done) {
				try {
					lock.wait();
				} catch (Exception ex) {
					System.out.println("Jar view lock interrupted");
					break;
				}

			}
		}
		return classpathURLs;

	}
}
