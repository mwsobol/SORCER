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

package sorcer.service;

public class SignatureException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private Signature signature;
	
	public SignatureException() {
	}

	public SignatureException(String msg) {
		super(msg);
	}
	
	public SignatureException(Signature signature) {
		this.signature = signature;
	}

	public SignatureException(String msg, Signature signature) {
		super(msg);
		this.signature = signature;
	}
	
	public SignatureException(Exception exception) {
		super(exception);
	}

	public SignatureException(String msg, Exception e) {
		super(msg, e);
		
	}
	
	public SignatureException(String msg, Signature signature, Exception e) {
		super(msg, e);
		this.signature = signature;
	}
	
	public Signature getSignature() {
		return signature;
	}
}
