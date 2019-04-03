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

package sorcer.service.modeling;

import sorcer.service.ContextException;

/**
 * @author Mike Sobolewski, Ray Kolonay
 */
public interface Variability {

	public Functionality getVar(String varName) throws ContextException;

	public Functionality getInvariantVar(String varName)
			throws ContextException;

	public Functionality getConstantVar(String varName)
			throws ContextException;

	public Functionality getLinkedVar(String varName) throws ContextException;

	public Functionality getOutputVar(String varName) throws ContextException;

	public Functionality getConstraintVar(String varName)
			throws ContextException;

	public Functionality getObjectiveVar(String varName)
			throws ContextException;

}
