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

import sorcer.core.context.model.ent.Entry;

public class TagEntry<T> extends Entry<T> {

    private static final long serialVersionUID = 1L;

    public TagEntry(String association) {
        super();
        annotation = association;
    }

    public TagEntry(String path, T value) {
        this(path, value, null);
    }

    public TagEntry(String path, T value, String association) {
        super(path, value);
        annotation = association;
    }

}