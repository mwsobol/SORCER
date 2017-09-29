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

import sorcer.core.context.model.ent.Value;
import sorcer.service.Signature;
import sorcer.service.modeling.Functionality;

public class InoutValue<T> extends Value<T> {

    private static final long serialVersionUID = 1L;

    public InoutValue() {
        super();
        annotation = Signature.Direction.INOUT;
        type = Functionality.Type.INOUT;
    }
    public InoutValue(String path) {
        this(path, null, false, 0);
    }

    public InoutValue(String path, T value) {
        this(path, value, false, 0);
    }

    public InoutValue(String path, T value, int index) {
        this(path, value, false, index);
    }


    public InoutValue(String path, T value, boolean isPersistent, int index) {
        super(path);
        this.impl = value;
        this.isPersistent = isPersistent;
        this.index = index;
        type = Functionality.Type.INOUT;
        annotation = Signature.Direction.INOUT;
    }
}