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

import sorcer.service.Arg;
import sorcer.service.ServiceException;

import java.io.Serializable;
import java.rmi.RemoteException;

public class Tuple1<T1> implements Serializable, Tuple,  Arg {
	private static final long serialVersionUID = -5484669816928328247L;
	public T1 _1 = null;

	public Tuple1(T1 x1) {
		_1 = x1;
	}
	
	@Override
	public String toString() {
		return "[" + _1 + "]";
	}
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof Tuple1) {
			Tuple1<?> uni = (Tuple1<?>) object;
			if (_1.equals(uni._1))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 1 * 31 + _1.hashCode();
	}

	@Override
	public String getName() {
		return toString();
	}

	@Override
	public Object execute(Arg... args) throws ServiceException, RemoteException {
		return _1;
	}
}