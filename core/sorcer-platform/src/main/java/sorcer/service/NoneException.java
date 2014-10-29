/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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
 * The NoneException represents an exception that is thrown if a
 * Context value is Context.none (undefined or null).
 * 
 * @author Mike Sobolewski
 */

public class NoneException extends ContextException {

	private static final long serialVersionUID = -6386400235403263904L;

	/**
	 * Constructs a new ServiceContextException
	 * 
	 */
	public NoneException() {
		super();
	}

	/**
	 * Constructs a new NoneException with the specified descriptive
	 * message.
	 * 
	 * @param msg
	 *            The string describing the exception.
	 */
	public NoneException(String msg) {
		super(msg);
	}

	/**
	 * Constructs a new NoneException with an embedded exception.
	 * 
	 * @param exception
	 *            embedded exception
	 */
	public NoneException(Exception exception) {
		super(exception);
	}

	/**
	 * Constructs a new NoneException with a message and an embedded exception.
	 * 
	 * @param exception
	 *            embedded exception
	 */
	public NoneException(String msg, Exception exception) {
		super(msg, exception);
	}

}
