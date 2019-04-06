/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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
 * A Paradigm instance can return exact data or evaluated (reevaluated -
 * of Evaluation type). If isModeling is true that returned values of the
 * Evaluation type are evaluated, otherwise returned as is.
 */
public interface Paradigmatic {

	/**
	 * Returns true if this instance is a model, otherwise false
	 * 
	 * @return true if this instance is a model
	 */
	public boolean isModeling();

	/**
	 * <p>
	 * Assign the modeling mode of instance of this type.
	 * </p>
	 * 
	 * @param isModeling
	 */
	public void setModeling(boolean isModeling);

}
