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

package sorcer.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;

import sorcer.core.SorcerConstants;

/**
 * EmailCmd is a psedofactory for all email commands. It implements commands
 * SEND_MAIL
 */

public class EmailCmd implements SorcerConstants {
	protected String cmdName;
	protected String[] args;
//	public Object invoker;
	protected ResultSet result;
	protected static String host = null;

	public EmailCmd(String cmdName) {
		this.cmdName = cmdName;
	}

	public EmailCmd(String cmdName, String smtpHost) {
		this.cmdName = cmdName;
		host = smtpHost;
	}

	/**
	 * Sets a target object of the commnad together with an array of command
	 * arguments. The target is a DeafultProtocolsStatement used to access
	 * database. The arguments in the array are as defined in gapp.GApp.
	 */
	public void setArgs(Object target, Object[] args) {
		this.args = (String[]) args;
	}

	public void doIt() {
		try {
			if (cmdName.equals(Integer.toString(SEND_MAIL)))
				sendMail();
		} catch (Exception e) {
			System.err.println("ERROR:" + e.getMessage());
		}
	}

	private void sendMail() throws UnknownHostException {
		if (host == null)
			host = InetAddress.getLocalHost().getHostName();

		String[] to, cc, bcc, lines;
		SMTPSession session;
		String result, sender;
		if (args[MFROM] == null || args[MFROM].length() == 0) {
			System.err.println("ERROR:User's email address must be provided");
			return;
		}

		if (args[MFROM].indexOf('@') > 0)
			sender = args[MFROM];
		else {
			System.err.println("ERROR:" + args[MFROM]
					+ " is not a proper email address");
			return;
		}

		if (sender == null || sender.length() == 0) {
			System.err.println("ERROR:User's email address unknown");
			return;
		}
		if (args[MCC] == null || args[MCC].length() == 0)
			cc = null;
		else
			cc = SorcerUtil.tokenize(args[MCC], ",");

		if (args[MBCC] == null || args[MBCC].length() == 0)
			bcc = null;
		else
			bcc = SorcerUtil.tokenize(args[MBCC], ",");

		if (args[MTEXT] == null || args[MTEXT].length() == 0)
			lines = null;
		else
			lines = SorcerUtil.tokenize(args[MTEXT], "\n\r");

		// Util.debug(this, "lines=" + Util.arrayToString(lines));

		// Util.debug(this, "EmailCmd.args[MTO]=" + args[MTO]);
		to = SorcerUtil.tokenize(args[MTO], ",");

		if (to.length >= 1) {
			// Util.debug(this, "EmailCmd.to=" + to[0]);
			session = new SMTPSession(host, args[MSUBJECT], to[0], sender,
					lines, cc, bcc);
			result = session.send();

			// send to other recipients
			if (to.length > 1) {
				for (int i = 1; i < to.length; i++) {
					// Util.debug(this, "EmailCmd.to=" + to[i]);
					result = result + session.sendMessage(to[i]);
				}
			}

		} else {
			System.err.println("ERROR:No recipient specified");
			return;
		}
		System.out.println("EmailCmd:result=" + result);
	}

	public void undoIt() {
		// do nothing
	}
}
