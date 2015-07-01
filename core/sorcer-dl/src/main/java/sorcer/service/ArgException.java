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


public class ArgException extends EvaluationException {

	private static final long serialVersionUID = 5987271488623420213L;
	private String argName = null;
	private Exception exception = null;
	
	public ArgException() {
	}
	/**
	 * Constructs a new ParException with an embedded exception.
	 * 
	 * @param exception
	 *            embedded exception
	 */
	public ArgException(Exception exception) {
		super(exception);
	}
	
	public ArgException(String msg, Exception e) {
		super(msg);
		e.printStackTrace();
	}

	public ArgException(String msg) {
		super(msg);
	}
	
	public ArgException(String msg, String parName, Exception exception) {
		super(msg);
		this.argName = parName;
		this.exception = exception;
	}
	
	public String getArgName() {
		return argName;
	}
	
	public Exception getException() {
		return exception;
	}
}
