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
 * The decryption thread is sparked by an encryption manager for the processing
 * of cipher text which is written back to the manager via an output pipe
 * 
 * @author Daniel Kerr
 */

class DecryptThread extends Thread {
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
	 * @param decryptCipher
	 *            cipher to use for decryption
	 * @param data
	 *            cipher text byte array to decrypt
	 */
	public DecryptThread(Writer out, Cipher decryptCipher, byte[] data) {
		this.init(out, decryptCipher, new StringReader(new String(data)));
	}

	/**
	 * @param out
	 *            output pipe for writing
	 * @param decryptCipher
	 *            cipher to use for decryption
	 * @param data
	 *            cipher text string to decrypt
	 */
	public DecryptThread(Writer out, Cipher decryptCipher, String data) {
		this.init(out, decryptCipher, new StringReader(data));
	}

	// --------------------------------------------------------------------------

	/**
	 * @param out
	 *            output pipe for writing
	 * @param decryptCipher
	 *            cipher to use for decryption
	 * @param in
	 *            input pipe for reading
	 */
	private void init(Writer out, Cipher decryptCipher, Reader in) {
		myCipher = decryptCipher;
		myOut = new PrintWriter(out);
		myScan = new Scanner(in);

		this.start();
	}

	// --------------------------------------------------------------------------

	/**
	 * <code>data</code> is processed and decrypted by the local cipher object (
	 * <code>myCipher</code>) and is written to the local writer object (
	 * <code>myOut</code>)
	 * <p>
	 * Once complete the thread closes
	 */
	public void run() {
		while (myScan.hasNextLine()) {
			try {
				// ---------------------------------------------
				String[] strArr = myScan.nextLine().split(",");
				byte[] cText = new byte[strArr.length];
				for (int j = 0; j < strArr.length; ++j) {
					cText[j] = Byte.valueOf(strArr[j]);
				}
				// ---------------------------------------------
				byte[] ciph = myCipher.doFinal(cText);

				try {
					myOut.println(new String(ciph, "UTF8"));
					myOut.flush();
				} catch (Exception e) {
					myOut.println("Problem With Encoding");
				}
			} catch (BadPaddingException e) {
				System.out.println(e.toString());
			} catch (IllegalBlockSizeException e) {
				System.out.println(e.toString());
			}
		}
	}

	// --------------------------------------------------------------------------
}