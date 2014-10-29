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

import java.io.PrintStream;
import java.net.InetAddress;

import sun.net.smtp.SmtpClient;

/**
 * The SMTPSession class allows to send email messages using SmtpClient
 */
public class SMTPSession {
	public String host;
	public String subject;
	public String recipient;
	public String sender;
	public String[] cc = null; // carbon copy recipients
	public String[] bcc = null; // blind carbon copy recipients
	public String[] message;

	/**
	 * The default constructor will require you to set up all necessary
	 * information later
	 */
	public SMTPSession() {
		// do nothing
	}

	/**
	 * Create a new SMTP session object. The constructor will not send a
	 * message, this has to be invoked separately.
	 * 
	 * @param host
	 *            the hostname of the SMTP server host
	 * @param subject
	 *            the message subject
	 * @param recipient
	 *            the recipients email address
	 * @param sender
	 *            the senders email address
	 * @param message
	 *            the String array containing the message to be delivered
	 */
	public SMTPSession(String host, String subject, String recipient,
			String sender, String[] message) {
		this.host = host;
		this.subject = subject;
		this.recipient = recipient;
		this.message = message;
		this.sender = sender;
	}

	/**
	 * Create a new SMTP session object. The constructor will not send a
	 * message, this has to be invoked separately.
	 * 
	 * @param host
	 *            the hostname of the SMTP server host
	 * @param subject
	 *            the message subject
	 * @param recipient
	 *            the recipients email address
	 * @param sender
	 *            the senders email address
	 * @param message
	 *            the String array containing the message to be delivered
	 * @param cc
	 *            the String array containing the recipients of carbon copy
	 */
	public SMTPSession(String host, String subject, String recipient,
			String sender, String[] message, String[] cc) {
		this(host, subject, recipient, sender, message);
		this.cc = cc;
	}

	/**
	 * Create a new SMTP session object. The constructor will not send a
	 * message, this has to be invoked separately.
	 * 
	 * @param host
	 *            the hostname of the SMTP server host
	 * @param subject
	 *            the message subject
	 * @param recipient
	 *            the recipients email address
	 * @param sender
	 *            the senders email address
	 * @param message
	 *            the String array containing the message to be delivered
	 * @param cc
	 *            the String array containing the recipients of carbon copy
	 * @param bcc
	 *            the String array containing the recipients of blind carbon
	 *            copy
	 */
	public SMTPSession(String host, String subject, String recipient,
			String sender, String[] message, String[] cc, String[] bcc) {
		this(host, subject, recipient, sender, message, cc);
		this.bcc = bcc;
	}

	/**
	 * Send the message to the session recipient only using the SMTP protocol.
	 */
	public String sendMessage() {
		boolean succeed = false;
		PrintStream ps = null;
		SmtpClient sendmail = null;
		if ((sender.length() > 0) && (recipient.length() > 0)) {
			try {
				// open smtp connection
				sendmail = new SmtpClient(host);
				sendmail.from(sender);
				sendmail.to(recipient);
				// get printstream
				ps = sendmail.startMessage();
				succeed = true;
			} catch (Exception e) {
				e.printStackTrace();
				System.out
						.println("Couldn't reach you through "
								+ host
								+ ", trying your local"
								+ "machine instead.  You probably are behind a firewall.");
			}
			if (!succeed) {
				try { // try again, this time, to localhost
					// open smtp connection
					sendmail = new SmtpClient(InetAddress.getLocalHost()
							.getHostName());
					sendmail.from(sender);
					sendmail.to(recipient);
					// get printstream
					ps = sendmail.startMessage();
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("There was an error sending your mail.");
					// if we bomb this time, give up
					return "ERROR: There was an error sending you mail.";
				}
			}

			try {
				// send headers.
				ps.println("From: " + sender);
				ps.println("To: " + recipient);
				ps.println("Subject: " + subject);

				if (cc != null) {
					StringBuffer ccRecipints = new StringBuffer();
					for (int i = 0; i < cc.length - 1; i++) {
						ccRecipints.append(cc[i]);
						ccRecipints.append(", ");
					}
					ccRecipints.append(cc[cc.length - 1]);
					ps.println("Cc: " + ccRecipints);
				}

				// ps.print("\r\n"); //header area delimiter
				ps.print("\n"); // header area delimiter

				// now send data to it
				if (message != null) {
					for (int i = 0; i < message.length; i++)
						ps.println(message[i]);
				}
				ps.flush();
				ps.close();
				sendmail.closeServer();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("There was an error sending mail to "
						+ recipient);
				return "ERROR: There was an error sending mail to " + recipient;
			}
		} else {
			System.out
					.println("You need to enter both recipient and sender email addresses");
			return "ERROR: You need to enter both recipient and sender email addresses";
		}
		return "Email sent successfully";
	}

	/**
	 * Send the message to the argument recipient only using the existing
	 * SMTPSession context.
	 * 
	 * @param recipient
	 *            the recipients email address
	 */
	public String sendMessage(String recipient) {
		this.recipient = recipient;
		return sendMessage();
	}

	/**
	 * Send the message to the session recipient, cc and bcc recipients using
	 * the existing SMTPSession context.
	 */
	public String send() {
		StringBuffer results = new StringBuffer();
		String res = sendMessage();
		if (res.startsWith("ERROR")) {
			results.append(res);
			results.append("\r\n");
		}

		if (cc != null) {
			for (int i = 0; i < cc.length; i++) {
				res = sendMessage(cc[i]);
				if (res.startsWith("ERROR")) {
					results.append(res);
					results.append("\r\n");
				}
			}
		}

		if (bcc != null) {
			for (int i = 0; i < bcc.length; i++) {
				res = sendMessage(bcc[i]);
				if (res.startsWith("ERROR")) {
					results.append(res);
					results.append("\r\n");
				}
			}
		}
		if (!res.startsWith("ERROR"))
			results.append(res);
		return results.toString();
	}
}
