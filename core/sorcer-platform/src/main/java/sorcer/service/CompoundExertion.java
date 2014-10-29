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

import java.util.List;

import sorcer.core.context.FidelityContext;


/**
 * @author Mike Sobolewski
 */
public interface CompoundExertion {
	

	public boolean isCompound();

	public int size();

	/**
	 * Returns the exertion at the specified index.
	 */
	public Exertion get(int index);
	
	/**
	 * Replaces the exertion at the specified position in this list with the
     * specified element.
	 */
	public void setExertionAt(Exertion ex, int i);
	
	public void setExertions(List<Exertion> exertions);
	
	public List<Exertion> getExertions();
		
	public void remove(int index) throws ContextException;
	
	public List<Exertion> getAllExertions();
	
	public Exertion getChild(String childName);
	
	public void applyFidelityContext(FidelityContext fiContext) throws ExertionException;
}
