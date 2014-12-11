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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;

/**
 * The encryption manager class implements block, stream, and threaded pipe
 * encryption using DES, and AES encryption.
 * 
 * @author Daniel Kerr
 */

public class EncryptionManager implements EncryptionManagement {
	// ----------------------------------------------------------------

	/** buffer */
	private byte[] buf = new byte[1024];
	/** cipher for encryption */
	private Cipher encryptionCipher;
	/** cipher for decryption */
	private Cipher decryptionCipher;

	/** class logger */
	private final static Logger logger = Logger
			.getLogger(EncryptionManager.class.getName());

	// ----------------------------------------------------------------

	/**
	 * Uses the <code>keyAgree</code> Key Agreement to create and initialize
	 * both the encryption and decryption ciphers.
	 * 
	 * @param keyAgree
	 *            the shared key key agreement for the creation of a common
	 *            cipher
	 */
	public void init(KeyAgreement keyAgree) {
		try {
			Key myKey = keyAgree.generateSecret("DES");

			encryptionCipher = Cipher.getInstance("DES");
			encryptionCipher.init(Cipher.ENCRYPT_MODE, myKey);

			decryptionCipher = Cipher.getInstance("DES");
			decryptionCipher.init(Cipher.DECRYPT_MODE, myKey);
		} catch (InvalidKeyException e) {
			logger.severe(e.toString());
		} catch (NoSuchAlgorithmException e) {
			logger.severe(e.toString());
		} catch (NoSuchPaddingException e) {
			logger.severe(e.toString());
		}
	}

	// ----------------------------------------------------------------

	/**
	 * @param plaintext
	 *            string containing text to be encrypted
	 * @return <code>plaintext</code> encrypted to a byte array
	 */
	public byte[] encrypt(String plaintext) {
		return encrypt(plaintext.getBytes());
	}

	/**
	 * @param plaintext
	 *            byte array containing text to be encrypted
	 * @return <code>plaintext</code> encrypted to a byte array
	 */
	public byte[] encrypt(byte[] plaintext) {
		try {
			return encryptionCipher.doFinal(plaintext);
		} catch (BadPaddingException e) {
			logger.severe(e.toString());
			return e.toString().getBytes();
		} catch (IllegalBlockSizeException e) {
			logger.severe(e.toString());
			return e.toString().getBytes();
		}
	}

	/**
	 * @param in
	 *            input stream to be encrypted
	 * @param out
	 *            output stream containing encrypted cipher text
	 */
	public void encryptStream(InputStream in, OutputStream out) {
		try {
			out = new CipherOutputStream(out, encryptionCipher);

			int numRead;
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}

			out.close();
		} catch (java.io.IOException e) {
		}
	}

	/**
	 * @param plaintext
	 *            byte array containing text to be encrypted
	 * @return Reader to the cipher text pipe
	 */
	public Reader encryptPipe(byte[] plaintext) {
		try {
			PipedWriter enOut = new PipedWriter();
			PipedReader enIn = new PipedReader(enOut);

			EncryptThread et = new EncryptThread(enOut, encryptionCipher,
					plaintext);
			et.join();

			return enIn;
		} catch (InterruptedException e) {
		} catch (IOException e) {
		}

		return null;
	}

	// ----------------------------------------------------------------

	/**
	 * @param ciphertext
	 *            string containing ciphertext to be decrypted
	 * @return <code>ciphertext</code> decrypted to a byte array
	 */
	public byte[] decrypt(String ciphertext) {
		return decrypt(ciphertext.getBytes());
	}

	/**
	 * @param ciphertext
	 *            byte array containing ciphertext to be decrypted
	 * @return <code>ciphertext</code> decrypted to a byte array
	 */
	public byte[] decrypt(byte[] ciphertext) {
		try {
			return decryptionCipher.doFinal(ciphertext);
		} catch (BadPaddingException e) {
			logger.severe(e.toString());
			return e.toString().getBytes();
		} catch (IllegalBlockSizeException e) {
			logger.severe(e.toString());
			return e.toString().getBytes();
		}
	}

	/**
	 * @param in
	 *            input stream to be decrypted
	 * @param out
	 *            output stream containing decrypted plain text
	 */
	public void decryptStream(InputStream in, OutputStream out) {
		try {
			in = new CipherInputStream(in, decryptionCipher);

			int numRead;
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}

			out.close();
		} catch (java.io.IOException e) {
		}
	}

	/**
	 * @param ciphertext
	 *            byte array containing ciphertext to be decrypted
	 * @return Reader to the plain text pipe
	 */
	public Reader decryptPipe(byte[] ciphertext) {
		try {
			PipedWriter deOut = new PipedWriter();
			PipedReader deIn = new PipedReader(deOut);

			DecryptThread dt = new DecryptThread(deOut, decryptionCipher,
					ciphertext);
			dt.join();

			return deIn;
		} catch (InterruptedException e) {
		} catch (IOException e) {
		}

		return null;
	}

	// ----------------------------------------------------------------
}