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

/**
 * The ServiceContextException represents an exception that is thrown if an
 * exertion is performed by a service provider failing to process the associated
 * service context correctly due to incorrect context paths, values, or
 * associations.
 * 
 * @author Mike Sobolewski
 */

public class ContextException extends MogramException {

	private static final long serialVersionUID = -1974580626122630036L;
	
	/**
	 * Constructs a new ServiceContextException
	 * 
	 */
	public ContextException() {
		super();
	}

	/**
	 * Constructs a new ServiceContextException with the specified descriptive
	 * message.
	 * 
	 * @param msg
	 *            The string describing the exception.
	 */
	public ContextException(String msg) {
		super(msg);
	}

	/**
	 * Constructs a new ServiceContextException with an embedded exception.
	 * 
	 * @param exception
	 *            embedded exception
	 */
	public ContextException(Exception exception) {
		super(exception);
	}

	/**
	 * Constructs a new ContextException with a message and an embedded exception.
	 * 
	 * @param exception
	 *            embedded exception
	 */
	public ContextException(String msg, Exception exception) {
		super(msg, exception);
	}

}
