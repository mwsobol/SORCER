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

package sorcer.security.sign;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * <p>
 * Interface for making a signed ServiceTask. SignedServiceTask is sent over the
 * communication channel to the provider where ServiceTask is retrieved from it
 * and the SignedServiceTask object is saved in the database along with the
 * prinicipal name sent in context.
 * 
 * @see SignedTaskInterface
 */

public interface SignedTaskInterface {
	/**
	 * Returns encrypted ServiceTask.
	 * 
	 * @return array containing signed bytes
	 */
	public byte[] getProcessByteSignature();

	/**
	 * Sets the signature and the object whose signature is being sent
	 * 
	 * @param signature
	 *            of ServiceTask object
	 * @param object
	 *            whose signature is being saved
	 *@exception IOException
	 *                if the object could not be accessed
	 */
	public void setSignature(byte[] signature, MarshalledObject mobject)
			throws IOException;

	/**
	 * Returns saved ServiceTask.
	 * 
	 * @return object which is an instance of ServiceTask
	 * @exception IOException
	 *                if the object could not be accessed
	 * @exception ClassNotFoundException
	 *                if the class for object could not be found
	 */
	public Object getObject() throws IOException, ClassNotFoundException;

	/**
	 * Used to verify the signature. Uses PublicKey supplied to decrypt
	 * signature and matches it withe object
	 * 
	 * @param publickey
	 *            of the key pair whose private key was used to encrypt the
	 *            object
	 * @param signature
	 *            of the encrypted object
	 * @exception IOException
	 *                if the object could not be accessed
	 * @exception ClassNotFoundException
	 *                if the class for object could not be found
	 * @exception SignatureExceptiono
	 *                if the signature is not of right format
	 * @exception InvalidKeyException
	 *                if the key supplied is not of right format
	 */
	public boolean verify(PublicKey publickey, Signature signature1)
			throws InvalidKeyException, SignatureException, IOException,
			ClassNotFoundException;
}
