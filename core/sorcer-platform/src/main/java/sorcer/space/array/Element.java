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

package sorcer.space.array;

import net.jini.core.entry.Entry;

public class Element implements Entry {
	public String name;
	public Integer index;
	public Object data;

	public Element() {
	}

	public Element(String name, int index, Object data) {
		this.name = name;
		this.index = new Integer(index);
		this.data = data;
	}

	public Element(String name) {
		this.name = name;

	}

	public Element(String name, Object data) {
		this.name = name;

		this.data = data;
	}
}
