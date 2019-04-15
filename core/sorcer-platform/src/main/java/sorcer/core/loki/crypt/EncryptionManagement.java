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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import javax.crypto.KeyAgreement;

/**
 * The encryption management interface describes the standard methods for both
 * encryption and decryption. It outlines both block, stream, and threaded pipe
 * cryptography methods, and expands these to allow for both byte array and
 * string data.
 * 
 * @author Daniel Kerr
 */

public interface EncryptionManagement {
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @param keyAgree
	 *            the shared key key agreement for the creation of a common
	 *            cipher
	 */
	public void init(KeyAgreement keyAgree);

	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @param plaintext
	 *            string containing text to be encrypted
	 * @return <code>plaintext</code> encrypted to a byte array
	 */
	public byte[] encrypt(String plaintext);

	/**
	 * @param plaintext
	 *            byte array containing text to be encrypted
	 * @return <code>plaintext</code> encrypted to a byte array
	 */
	public byte[] encrypt(byte[] plaintext);

	/**
	 * @param in
	 *            input stream to be encrypted
	 * @param out
	 *            output stream containing encrypted cipher text
	 */
	public void encryptStream(InputStream in, OutputStream out);

	/**
	 * @param plaintext
	 *            byte array containing text to be encrypted
	 * @return Reader to the cipher text pipe
	 */
	public Reader encryptPipe(byte[] plaintext);

	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @param ciphertext
	 *            string containing ciphertext to be decrypted
	 * @return <code>ciphertext</code> decrypted to a byte array
	 */
	public byte[] decrypt(String ciphertext);

	/**
	 * @param ciphertext
	 *            byte array containing ciphertext to be decrypted
	 * @return <code>ciphertext</code> decrypted to a byte array
	 */
	public byte[] decrypt(byte[] ciphertext);

	/**
	 * @param in
	 *            input stream to be decrypted
	 * @param out
	 *            output stream containing decrypted plain text
	 */
	public void decryptStream(InputStream in, OutputStream out);

	/**
	 * @param ciphertext
	 *            byte array containing ciphertext to be decrypted
	 * @return Reader to the plain text pipe
	 */
	public Reader decryptPipe(byte[] ciphertext);

	// ------------------------------------------------------------------------------------------------------------
}