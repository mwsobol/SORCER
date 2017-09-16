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

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.context.model.ent.Function;
import sorcer.service.*;

import java.io.Serializable;
import java.rmi.RemoteException;

@SuppressWarnings("unchecked")
public class Tuple2<T1, T2>  implements Serializable,  Arg {

	private  static final long serialVersionUID = -6519678282532888568L;

	public T1 _1 = null;

	public T2 _2 = null;

	public Tuple2(T1 x1, T2 x2) {
		_1 = x1;
		_2 = x2;
	}

	public T1 key() {
	    return _1;
    }

    public T2 value() {
	    return _2;
    }

    @Override
    public int hashCode() {
        return 2 * 31 + _1.hashCode() + _2.hashCode();
    }

    @Override
    public String getName() {
        return _1.toString();
    }

	@Override
	public boolean equals(Object object) {
		if (object instanceof Tuple2) {
			Tuple2<?,?> pair = (Tuple2<?,?>) object;
			if (_1.equals(pair._1) && _2.equals(pair._2))
				return true;
		}
		return false;
	}

}