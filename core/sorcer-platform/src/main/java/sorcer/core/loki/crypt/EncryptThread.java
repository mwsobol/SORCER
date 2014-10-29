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

package sorcer.core.loki.crypt;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * The encryption thread is sparked by an encryption manager for the processing
 * of cipher text which is written back to the manager via an output pipe
 * 
 * @author Daniel Kerr
 */

class EncryptThread extends Thread {
	// --------------------------------------------------------------------------

	/** cipher */
	private Cipher myCipher;
	/** writer */
	private PrintWriter myOut;
	/** reader */
	private Scanner myScan;

	// --------------------------------------------------------------------------

	/**
	 * @param out
	 *            output pipe for writing
	 * @param encryptCipher
	 *            cipher to use for encryption
	 * @param data
	 *            cipher text byte array to encrypt
	 */
	public EncryptThread(Writer out, Cipher encryptCipher, byte[] data) {
		this.init(out, encryptCipher, new StringReader(new String(data)));
	}

	/**
	 * @param out
	 *            output pipe for writing
	 * @param encryptCipher
	 *            cipher to use for encryption
	 * @param data
	 *            cipher text string to encrypt
	 */
	public EncryptThread(Writer out, Cipher encryptCipher, String data) {
		this.init(out, encryptCipher, new StringReader(data));
	}

	// --------------------------------------------------------------------------

	/**
	 * @param out
	 *            output pipe for writing
	 * @param encryptCipher
	 *            cipher to use for encryption
	 * @param in
	 *            input pipe for reading
	 */
	private void init(Writer out, Cipher encryptCipher, Reader in) {
		myCipher = encryptCipher;
		myOut = new PrintWriter(out);
		myScan = new Scanner(in);

		this.start();
	}

	// --------------------------------------------------------------------------

	/**
	 * <code>data</code> is processed and encrypted by the local cipher object (
	 * <code>myCipher</code>) and is written to the local writer object (
	 * <code>myOut</code>)
	 * <p>
	 * Once complete the thread closes
	 */
	public void run() {
		while (myScan.hasNextLine()) {
			try {
				byte[] cText = myScan.nextLine().getBytes();
				byte[] ciph = myCipher.doFinal(cText);
				// -------------------------------------
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < (ciph.length - 1); ++j) {
					sb.append(ciph[j]);
					sb.append(",");
				}
				sb.append(ciph[ciph.length - 1]);
				// -------------------------------------
				myOut.println(sb.toString());
				myOut.flush();
			} catch (BadPaddingException e) {
				System.out.println(e.toString());
			} catch (IllegalBlockSizeException e) {
				System.out.println(e.toString());
			}
		}
	}

	// --------------------------------------------------------------------------
}