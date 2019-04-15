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
 * The <code>RoutineError</code> represents an error that is thrown
 * if an exertion is performed by a service provider failing to compute it
 * correctly. A complementary related throwable and/or ill behaving exertion can
 * be embedded into this exception.
 * 
 * @author Mike Sobolewski
 */
public class RoutineError extends Error {

//	private static final long serialVersionUID = 3961573000741782514L;

	/**
	 * The exertion relevant to this exception.
	 */
	private Routine exertion;

	public RoutineError() {
	}

	public RoutineError(Routine exertion) {
		this.exertion = exertion;
	}

	public RoutineError(String msg) {
		super(msg);
	}

	public RoutineError(Exception e) {
		super(e);
	}
	
	/**
	 * Constructs a <code>RoutineError</code> with the specified detailed
	 * message and the relevant exertion.
	 * 
	 * @param message
	 *            the detailed message
	 * @param exertion
	 *            the embedded exertion
	 */
	public RoutineError(String message, Routine exertion) {
		super(message);
		this.exertion = exertion;
	}

	public RoutineError(String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * Constructs a <code>RoutineError</code> with the specified detail
	 * message and nested exception.
	 * 
	 * @param message
	 *            the detailed message
	 * @param cause
	 *            the nested throwable cause
	 */
	public RoutineError(String message, Routine exertion, Throwable cause) {
		super(message, cause);
		this.exertion = exertion;
	}

	/**
	 * Returns the embedded exertion causing this exception.
	 * 
	 * @return embedded exertion
	 */
	public Routine getExertion() {
		return exertion;
	}
	
}
