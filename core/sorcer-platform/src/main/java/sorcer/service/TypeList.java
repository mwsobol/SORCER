/*
 * Copyright 2016 the original author or authors.
 * Copyright 2016 SorcerSoft.org.
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
package sorcer.service;

import java.util.ArrayList;

/**
 * Created by Mike Sobolewski on 5/23/16.
 */
public class TypeList extends ArrayList<Class> implements Arg {

    static final long serialVersionUID = 1L;

    private static int count = 0;

    String name;

    public TypeList() {
        super();
        count++;
    }

    public TypeList(int size) {
        super(size);
    }

    public TypeList(Class... types) {
        super();
        for (Class cl : types) {
            add(cl);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public Class[] toTypeArray() {
        Class[] ta = new Class[this.size()];
        this.toArray(ta);
        return ta;
    }

}
