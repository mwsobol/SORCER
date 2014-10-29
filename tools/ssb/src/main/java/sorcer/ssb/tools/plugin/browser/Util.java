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

import java.lang.reflect.Array;

public class Util {
	/**
	 * Returns a string representation of recursive arrays of any component
	 * type. in the form [e1,...,ek]
	 */
	public static String arrayToString(Object array) {
		if (array == null)
			return "null";
		else if (!array.getClass().isArray()) {
			return array.toString();
		}
		int length = Array.getLength(array);
		if (length == 0)
			return "[no elements]";

		StringBuffer buffer = new StringBuffer("[");
		int last = length - 1;
		Object obj;
		for (int i = 0; i < length; i++) {
			obj = Array.get(array, i);
			if (obj == null)
				buffer.append("null");
			else if (obj.getClass().isArray())
				buffer.append(arrayToString(obj));
			else
				buffer.append(obj);

			if (i == last)
				buffer.append("]");
			else
				buffer.append(",");
		}
		return buffer.toString();
	}
}