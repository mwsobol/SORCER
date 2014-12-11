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

package sorcer.ssb.tools.plugin.browser;

import java.net.URL;

public class CodebaseNode {
	private URL codebase;
	private String name;
	private boolean isClasspathJar;

	public CodebaseNode(URL url) {
		codebase = url;
	}

	public CodebaseNode(URL url, boolean cpJar) {
		codebase = url;
		isClasspathJar = cpJar;
	}

	public CodebaseNode(String n) {
		name = n;
	}

	public boolean isClasspathJar() {
		return isClasspathJar;
	}

	public String toString() {
		if (codebase != null) {
			String url = codebase.toExternalForm();
			if (isClasspathJar) {
				return url.substring(url.lastIndexOf("/") + 1);
			} else {
				return url;
			}

		} else {
			return name;
		}
	}

	/**
	 * getCodebase
	 * 
	 * @return java.net.URL
	 */
	public URL getCodebase() {
		return codebase;
	}

	/**
	 * setCodebase
	 * 
	 * @param codebase
	 */
	public void setCodebase(URL codebase) {
		this.codebase = codebase;
	}

	/**
	 * getName
	 * 
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * setName
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	public boolean sameURL(URL other) {
		return codebase.equals(other);
	}
}
