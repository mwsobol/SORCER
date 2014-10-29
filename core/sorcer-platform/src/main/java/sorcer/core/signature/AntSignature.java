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

package sorcer.core.signature;

import java.io.File;

public class AntSignature extends ServiceSignature {

	static final long serialVersionUID = -1L;

	private File build;
	

	public AntSignature(String selector, File build) {
		this.selector = selector;
		this.build = build;
	}
	
	/**
	    <p> Returns the build file for this signature. </p>
	   
	    @return the evaluation
	 */
	public File getBuildFile() {
		return build;
	}
	
	public String toString() {
		return "ant build: " + build + " target: " + selector;
	}
}
