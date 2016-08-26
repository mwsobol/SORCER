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

package sorcer.core;

import sorcer.service.Arg;

import java.io.Serializable;

/**
 * Created by Mike Sobolewski
 */
public class Name implements Arg, Serializable, Comparable {
    
    private String name;

    public Name(Object name) {
        this.name = name.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Name)
            return this.name.equals(((Name)object).getName());
        else
            return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }


    @Override
    public int compareTo(Object o) {
        if (o == null)
            throw new NullPointerException();
        if (o instanceof Name)
            return name.compareTo(((Name)o).getName());
        else
            return -1;
    }
}
