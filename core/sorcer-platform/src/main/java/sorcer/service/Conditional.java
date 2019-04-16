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


@SuppressWarnings("rawtypes")
public interface Conditional {

	/**
	 * The isTrue method is responsible for evaluating the condition component of
	 * the Conditonal. Thus returning the boolean eval true or false.
	 * 
	 * @return boolean true or false depending on the condition
	 * @throws RoutineException
	 *             if there is any problem within the isTrue method.
	 * @throws ContextException 
	 */
	public boolean isTrue() throws ContextException;
	
	public Context getConditionalContext();
	
	public void setConditionalContext(Context context);
	
}