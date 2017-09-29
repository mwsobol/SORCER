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

package sorcer.co.tuple;

import sorcer.core.context.model.ent.Value;
import sorcer.service.*;

@SuppressWarnings("unchecked")
public class AnnotatedValue<T> extends Value<T> implements Arg {
	
	private static final long serialVersionUID = 1L;

	public AnnotatedValue() {
	}

	public AnnotatedValue(String path, String anotation, T value) {
		key = path;
		impl = value;
		this.annotation = anotation;
	}
		
	public AnnotatedValue(String path, T value) {
		T v = value;
		if (v == null)
			v = (T)Context.none;

		key = path;
		this.impl = v;
	}
	
	public AnnotatedValue(String path, T value, int index) {
		T v = value;
		if (v == null)
			v = (T)Context.none;

		key = path;
		this.impl = v;
		this.index = index;
	}

    @Override
	public String toString() {
		return "[" + key + ":" + impl + ":" + annotation + "]";
	}
}