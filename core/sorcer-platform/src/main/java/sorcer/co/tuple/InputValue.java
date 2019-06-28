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
import sorcer.util.url.sos.SdbUtil;

public class InputValue<T> extends Value<T> {

	private static final long serialVersionUID = 1L;

    public InputValue(String path) {
        this(path, null, false, 0);
    }

    public InputValue(String path, T value) {
        this(path, value, false, 0);
    }

    public InputValue(String path, T value, int index) {
        this(path, value, false, index);
    }


    public InputValue(String path, Object item, boolean isPersistent, int index) {
        super(path, item);
        this.isPersistent = isPersistent;
        if (SdbUtil.isSosURL(item)) {
            impl = (T) item;
            out = null;
        }
        this.index = index;
        annotation = Signature.Direction.IN;
        type = Functionality.Type.INPUT;
    }
}