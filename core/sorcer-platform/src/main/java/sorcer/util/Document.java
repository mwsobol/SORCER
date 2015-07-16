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

package sorcer.util;

import java.io.Serializable;

/**
 * Class that represents a document Object
 */
public class Document implements Serializable {

	private String path = null;
	private String id = null;

	// If the currentVersionID and versionID are not in sync, then the document
	// object is out of date.
	private String versionID = null;
	private String versionName = null;
	// Current version for this document
	private String currentVersionID = null;

	private String name = null;
	private String accessName = null;

	public Document(String path, String id, String versionID,
			String currentVersionID, String versionName, String name,
			String accessName) {
		this.path = path;
		this.id = id;
		this.versionID = versionID;
		this.currentVersionID = currentVersionID;
		this.versionName = versionName;
		this.name = name;
		this.accessName = accessName;
	}

	public String toString() {
		return new StringBuffer("fiperdoc://").append(path.replace('#', '/'))
				.append("/").append(name).append("(").append(versionName)
				.append(")").toString();
	}

	public String getVersionID() {
		return versionID;
	}

	public boolean isOutOfDate() {
		return (versionID == currentVersionID);
	}

	public String getAccessName() {
		return accessName;
	}

	public String asString() {
		return new StringBuffer("fiperdoc://").append(versionID).toString();
	}
}
