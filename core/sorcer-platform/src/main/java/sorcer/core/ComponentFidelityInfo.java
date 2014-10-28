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

package sorcer.core;

import java.util.Arrays;

import sorcer.service.FidelityInfo;

/**
 * @author Mike Sobolewski
 */
public class ComponentFidelityInfo extends FidelityInfo {


	// component exertion path
	protected String path;

	public ComponentFidelityInfo() {
		// fidelityName undefined
	}

	public ComponentFidelityInfo(String fidelityName, String path) {
		this.name = fidelityName;
		this.path = path;
	}

	public ComponentFidelityInfo(String fidelityName, String path, String... selectors) {
		this(fidelityName, path);
		this.selectors = selectors;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String fidelityPath) {
		this.path = fidelityPath;
	}
	
	@Override
	public String toString() {
		return "Fidelity: " + name +"@" + path 
				+ (selectors == null ? "" : ":" + " with: " + Arrays.toString(selectors));
	}

}
