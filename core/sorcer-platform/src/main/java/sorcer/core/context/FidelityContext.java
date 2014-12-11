/*
 * Copyright 2014 the original author or authors.
 * Copyright 2014 SorcerSoft.org.
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

package sorcer.core.context;


import java.util.HashMap;

import sorcer.service.Arg;
import sorcer.service.FidelityInfo;

/**
 * @author Mike Sobolewski
 */
public class FidelityContext extends HashMap<String, FidelityInfo> implements Arg {

	static final long serialVersionUID = 6416935408459975973L;
	
	String name;
	
	public FidelityContext() {
		super();
	}
	
	public FidelityContext(String name) {
		super();
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Arg#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

}
