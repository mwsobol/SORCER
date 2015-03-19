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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;

import net.jini.core.lookup.ServiceID;
import sorcer.ssb.tools.plugin.browser.graph.Chart;

public class MulticastView extends JFrame {

	private JEditorPane _output = new JEditorPane();
	// private JLabel _status=new JLabel("Listening....");
	private Thread _updater1;
	private Thread _updater2;
	private long _lastModifed;
	private int _reqCount = 0;
	private int _annCount = 0;
	private Chart _chart = new Chart();
	private int _reqDataIndex = -1;
	private int _annDataIndex = -1;
	private ArrayList _reqHistory = new ArrayList();
	private ArrayList _annHistory = new ArrayList();
	private MutableAttributeSet _reqColor;
	private MutableAttributeSet _annColor;

	public MulticastView() {
		super("Multicast Monitor");

		_reqColor = new SimpleAttributeSet();
		StyleConstants.setForeground(_reqColor, Chart.chartColor[0]);

		_annColor = new SimpleAttributeSet();
		StyleConstants.setForeground(_annColor, Chart.chartColor[1]);
		_output.setEditorKit(new StyledEditorKit());

		_output.setEditable(false);
		setIconImage(TreeRenderer._frameIcon.getImage());

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				_updater1.interrupt();
				_updater2.interrupt();
				dispose();
			}
		});
		JScrollPane sp = new JScrollPane(_output);
		sp.setMinimumSize(new Dimension(100, 50));

		JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _chart,
				sp);

		splitter.setDividerSize(3);
		splitter.setDividerLocation(0.5);

		getContentPane().add(splitter, BorderLayout.CENTER);
		// getContentPane().add( _status,BorderLayout.SOUTH);

		_updater1 = new Thread() {
			public void run() {
				listen("224.0.1.84", 0);
			}
		};
		_updater2 = new Thread() {
			public void run() {
				listen("224.0.1.85", 1);
			}
		};
		_updater1.start();
		_updater2.start();
		_output.setText("Waiting for multicast requests & announcements...\n");

	}

	private void addMessage(String msg, MutableAttributeSet atts) {

		// _status.setText("Requests="+_reqCount+" Announcements="+_annCount);
		_reqHistory.add(new Integer(_reqCount));
		_annHistory.add(new Integer(_annCount));
		if (_reqHistory.size() > 200) {
			_reqHistory.remove(0);
		}
		if (_annHistory.size() > 200) {
			_annHistory.remove(0);
		}
		try {
			// if(_reqCount>0){

			int nr = _reqHistory.size();

			double[] rd = new double[nr];
			for (int i = 0; i < rd.length; i++) {
				rd[i] = ((Integer) _reqHistory.get(i)).doubleValue();
			}
			String[] labels = new String[nr];
			for (int i = 0; i < nr; i++) {
				labels[i] = "";
			}
			if (_reqDataIndex == -1) {

				_reqDataIndex = _chart.addData("Requests", rd, labels);
			} else {
				_chart.setDataAt(_reqDataIndex, "Requests", rd, labels, null);
			}
			// }
			// if(_annCount>0){
			int na = _annHistory.size();
			double[] ad = new double[na];
			for (int i = 0; i < ad.length; i++) {
				ad[i] = ((Integer) _annHistory.get(i)).doubleValue();
			}

			labels = new String[na];
			for (int i = 0; i < na; i++) {
				labels[i] = "";
			}
			if (_annDataIndex == -1) {

				_annDataIndex = _chart.addData("Announcements", ad, labels);
			} else {
				_chart.setDataAt(_annDataIndex, "Announcements", ad, labels,
						null);
			}
			// }
			_chart.repaint();
		} catch (Exception ex) {
			System.err.println("Caught Exception: " + ex.getClass().getName()
					+ "; Msg: " + ex.getMessage());
			ex.printStackTrace();
		}

		final Document doc = _output.getDocument();
		final String out = msg + "\n";
		// SwingUtilities.invokeLater(new Runnable(){
		// public void run(){

		try {
			int docLen = doc.getLength();
			doc.insertString(doc.getLength(), out, atts);

			_output.setCaretPosition(doc.getLength());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// }
		// });

		// System.out.println(msg);
	}

	private void listen(final String address, final int type) {
		try {

			MulticastSocket socket = new MulticastSocket(4160);

			// System.out.println("Multicast address="+address);

			socket.joinGroup(InetAddress.getByName(address));

			while (!Thread.currentThread().isInterrupted()) {

				// set timeout on socket to allow for interrupts to be detected
				socket.setSoTimeout(2000);
				byte[] buf = new byte[512];

				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				try {
					socket.receive(packet);

					// System.out.println("Received packet "+type);

					if (type == 0) {
						processAnn(buf);
					} else {
						processReq(buf);
					}

				} catch (IOException ex) {
					// ex.printStackTrace();
					// timeout on socket, so let's go around again
					continue;
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			addMessage("Thread interrupted", null);
			socket.close();

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	private void processAnn(byte[] buf) throws Throwable {

		ByteArrayInputStream bias = new ByteArrayInputStream(buf);
		DataInputStream dis = new DataInputStream(bias);
		int ver = dis.readInt();
		int slen = dis.readUnsignedShort();

		if (slen == 0) {
			// System.out.println("Missing host protocol version="+ver);
			return;
		}

		String host = "unknown host";

		byte[] sb = null;

		sb = new byte[slen];
		dis.readFully(sb);
		host = new String(sb);

		int port = dis.readInt();

		ServiceID serviceID = new ServiceID(dis);

		int ngroups = dis.readInt();
		String[] grps = new String[ngroups];
		StringBuffer gbuf = new StringBuffer();
		if (ngroups == 0) {
			gbuf.append("ALL_GROUPS");
		}
		for (int i = 0; i < ngroups; i++) {
			slen = dis.readUnsignedShort();

			if (slen == 0) {
				grps[i] = "?";

			} else {

				sb = new byte[slen];
				dis.readFully(sb);
				grps[i] = new String(sb);
				if (grps[i].length() == 0) {
					grps[i] = "PUBLIC";
				}
				gbuf.append(grps[i]);
				if (i < ngroups - 1) {
					gbuf.append(" ");
				}
			}
		}

		dis.close();
		_annCount++;
		StringBuffer msg = new StringBuffer();
		msg.append(new java.util.Date());
		msg.append("\n");
		msg.append("Host: ");
		msg.append(host);
		msg.append(" Port: ");
		msg.append(port);

		msg.append("\n");
		msg.append("ServiceID: ");
		msg.append(serviceID);
		msg.append("\n");
		msg.append("Groups: ");

		msg.append(gbuf.toString());
		msg.append("\n--------------------");
		addMessage(msg.toString(), _annColor);

		// addMessage(ver+" "+host+" "+port+" "+serviceID+" "+gbuf.toString(),_annColor);
	}

	private void processReq(byte[] buf) throws Throwable {

		ByteArrayInputStream bias = new ByteArrayInputStream(buf);
		DataInputStream dis = new DataInputStream(bias);
		int ver = dis.readInt();
		// int slen=dis.readUnsignedShort();
		// byte [] sb=new byte[slen];
		// dis.readFully(sb);
		// String host=new String(sb);

		// System.out.println("ver="+ver);

		int port = dis.readInt();

		// System.out.println("port="+port);

		int nsids = dis.readInt();

		// System.out.println("nsids="+nsids);
		if (nsids > 5) {
			return;
		}

		ServiceID[] sids = new ServiceID[nsids];
		StringBuffer sbuf = new StringBuffer();
		for (int i = 0; i < nsids; i++) {
			sids[i] = new ServiceID(dis);
			sbuf.append(sids[i]);
			if (i < nsids - 1) {
				sbuf.append(" ");
			}
		}

		int ngroups = dis.readInt();

		// System.out.println("ngroups="+ngroups);

		String[] grps = new String[ngroups];
		StringBuffer gbuf = new StringBuffer();
		if (ngroups == 0) {
			gbuf.append("ALL_GROUPS");
		}
		for (int i = 0; i < ngroups; i++) {
			int slen = dis.readUnsignedShort();
			if (slen == 0) {
				grps[i] = "?";
				continue;
			}
			byte[] sb = new byte[slen];
			dis.readFully(sb);
			grps[i] = new String(sb);
			if (grps[i].length() == 0) {
				grps[i] = "PUBLIC";
			}
			gbuf.append(grps[i]);
			if (i < ngroups - 1) {
				gbuf.append(" ");
			}
		}

		dis.close();
		_reqCount++;

		StringBuffer msg = new StringBuffer();
		msg.append(new java.util.Date());
		msg.append("\n");
		msg.append("Port: ");
		msg.append(port);

		msg.append("\n");
		msg.append("Heard from: ");
		msg.append(sbuf.toString());
		msg.append("\n");
		msg.append("Groups: ");
		msg.append(gbuf.toString());
		msg.append("\n--------------------");
		addMessage(msg.toString(), _reqColor);

		// addMessage(ver+" "+port+" "+sbuf.toString()+" "+gbuf.toString(),_reqColor);
	}
}
