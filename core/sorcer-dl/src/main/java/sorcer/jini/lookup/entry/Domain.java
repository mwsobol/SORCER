/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.jini.lookup.entry;


import net.jini.entry.AbstractEntry;

/**
 * The domain of a service as used by users.  A service may have
 * multiple domains.
 */
public class Domain extends AbstractEntry {
    private static final long serialVersionUID = 2743215148071307201L;

    /**
     * Construct an empty instance of this class.
     */
    public Domain() {
    }

    /**
     * Construct an instance of this class, with all fields
     * initialized appropriately.
     *
     * @param name  the value of the name
     */
    public Domain(String name) {
	this.name = name;
    }

    /**
     * The name itself.
     *
     * @serial
     */
    public String name;
}
