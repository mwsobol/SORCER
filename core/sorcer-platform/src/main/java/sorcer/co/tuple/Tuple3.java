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
package sorcer.co.tuple;


import sorcer.service.ServiceFidelity;

public class Tuple3<T1, T2, T3> extends Tuple2<T1, T2> {

	private static final long serialVersionUID = 8462677030360698197L;

	public T3 _3 = null;

	public Tuple3(T1 x1, T2 x2) {
		super(x1, x2);
	}
	
	public Tuple3(T1 x1, T2 x2, T3 x3) {
		super(x1, x2);
		_3 = x3;
	}
	
	public ServiceFidelity fidelity() {
		return (ServiceFidelity)_3;
	}
	
	@Override
	public String toString() {
		return "[" + _1 + "|" + _2 + "|" + _3 + "]";
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Tuple3) {
			Tuple3<?, ?, ?> triplet = (Tuple3<?, ?, ?>) object;
			if (_1.equals(triplet._1) && _2.equals(triplet._2)
					&& _3.equals(triplet._3))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 3 * 31 + _1.hashCode() + _2.hashCode() + _3.hashCode();
	}
}