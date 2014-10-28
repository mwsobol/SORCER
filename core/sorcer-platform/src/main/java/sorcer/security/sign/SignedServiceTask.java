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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.List;

import sorcer.core.exertion.NetTask;
import sorcer.service.Signature;

/**
 * <p>
 * Any task that needs to be saved in a database is sent by encapsulating it in
 * a SignedServiceTask. SignedServiceTask is the implementation of
 * SignedTaskInterface. At the provider if a SignedServiceTask is received,
 * ServiceTask from it is extracted and is used to perform all the method
 * execution. SignedServiceTask is sent to Auditor service along with the
 * subject that is sent with this SignedServiceTask from Service UI to provider.
 * That subject contains the principal name that is used to save this task in
 * database.
 * 
 * @see SignedTaskInterface
 */

public final class SignedServiceTask extends NetTask implements
		SignedTaskInterface, Serializable {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	// private byte content[];
	/**
	 * encrypted object
	 */
	private byte signatureBytes[];

	/**
	 * actual object
	 */
	private MarshalledObject mobject;

	/**
	 * algorithm used to encrypt object
	 */
	// private String thealgorithm;

	/**
	 * hash of object
	 */
	// private byte[] hash;

	// private String password;
	// private Serializable object;

	/**
	 * Constructor to an instance of SignedServiceTask
	 * 
	 * @param name
	 *            task name
	 * @param description
	 *            task description
	 * @param methods
	 *            array of ServiceMethods that need to be executed
	 */
//	public SignedServiceTask(String name, String description,
//			List<Signature> signatures) {
//		super(name, description, signatures);
//	}

	/**
	 * Returns encrypted ServiceTask.
	 * 
	 * @return array containing signed bytes
	 */
	public void setSignature(byte[] signature, MarshalledObject mobject)
			throws IOException {
		this.signatureBytes = signature;
		this.mobject = mobject;
	}

	/**
	 * Sets the signature and the object whose signature is being sent
	 * 
	 * @param signatureBytes
	 *            of ServiceTask object
	 * @param object
	 *            whose signature is being saved
	 * @exception IOException
	 *                if the object could not be accessed
	 */
	public byte[] getProcessByteSignature() {
		byte abyte0[] = (byte[]) signatureBytes.clone();
		return abyte0;
	}

	/**
	 * Returns saved ServiceTask.
	 * 
	 * @return object which is an instance of ServiceTask
	 * @exception IOException
	 *                if the object could not be accessed
	 * @exception ClassNotFoundException
	 *                if the class for object could not be found
	 */
	public Object getObject() throws IOException, ClassNotFoundException {
		System.out.println("******************************" + mobject.get());
		return mobject.get();
	}

	/**
	 * Used to verify the signature. Uses PublicKey supplied to decrypt
	 * signature and matches it withe object
	 * 
	 * @param publickey
	 *            of the key pair whose private key was used to encrypt the
	 *            object
	 * @param signatureBytes
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
	public boolean verify(PublicKey publickey,
			java.security.Signature signature1) throws InvalidKeyException,
			SignatureException, IOException, ClassNotFoundException {
		ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
		ObjectOutputStream objectoutputstream = new ObjectOutputStream(
				bytearrayoutputstream);
		objectoutputstream.writeObject(mobject.get());
		objectoutputstream.flush();
		objectoutputstream.close();
		byte[] content = bytearrayoutputstream.toByteArray();
		signature1.initVerify(publickey);
		signature1.update(content);
		return signature1.verify(signatureBytes);
	}

}
