/*
 * Copyright 2015 the original author or authors.
 * Copyright 2015 SorcerSoft.org.
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

import sorcer.service.Context;
import sorcer.core.context.model.ent.Entry;
import sorcer.service.Signature;

/**
 * Created by Mike Sobolewski
 */
public class SignatureEntry extends Entry<Signature> {

    private static final long serialVersionUID = 1L;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private Context context;

    public SignatureEntry(String path, Signature value) {
        super(path, value);
    }

    public SignatureEntry(String path, Signature value, Context context) {
        key = path;
        impl = value;
        this.context = context;
    }

}