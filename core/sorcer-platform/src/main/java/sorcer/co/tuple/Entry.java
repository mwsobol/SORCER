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

import java.net.URL;

import sorcer.service.Active;
import sorcer.service.Arg;
import sorcer.service.Context;
import sorcer.service.Evaluation;
import sorcer.service.Identifiable;
import sorcer.service.Setter;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class Entry<T> extends Tuple2<String, T> implements Arg, Identifiable, Setter, Evaluation<T>, Active<T> {
	private static final long serialVersionUID = 5168783170981015779L;
	
	public int index;
	
	protected String annotation;

	public Entry() {
	}

	public Entry(String path) {
		_1 = path;
	}
	
	public Entry(String path, T value) {
		T v = value;
		if (v == null)
			v = (T)Context.none;

		_1 = path;
		if (v instanceof URL) {
			isPersistent = true;
		}
		this._2 = v;
	}
	
	public Entry(String path, T value, int index) {
		T v = value;
		if (v == null)
			v = (T)Context.none;

		_1 = path;
		this._2 = v;
		this.index = index;
	}

	public Entry(String path, T value, String association) {
		T v = value;
		if (v == null)
			v = (T)Context.none;

		_1 = path;
		this._2 = v;
		this.annotation = association;
	}
	
	public int index() {
		return index;
	}

	public String annotation() {
		return annotation;
	}
	
	public void annotation(String annotation) {
		this.annotation = annotation;
	}
	
	public boolean isAnnotated() {
		return annotation != null;
	}
	
	@Override
	public int hashCode() {
		int hash = _1.length() + 1;
		return hash = hash * 31 + _1.hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		if ((object instanceof Entry<?>)
				&& ((Entry<T>) object)._1.equals(_1))
			return true;
		else
			return false;
	}
	@Override
	public String toString() {
		return "[" + _1 + ":" + _2 + ":" + index + "]";
	}

}