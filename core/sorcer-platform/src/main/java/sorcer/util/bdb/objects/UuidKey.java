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

package sorcer.util.bdb.objects;

import java.io.Serializable;

import net.jini.id.Uuid;

/**
 * A Uuid key serves as the key in the key/data pair for multiple entities.
 *
 * <p> The UuidKey class is used both as the storage entry for the
 * key as well as the object binding to the key.  Because it is used directly
 * as storage data using serial format, it must be Serializable. </p>
 *
 * @author Mike Sobolewski
 */
public class UuidKey implements Serializable {

	static final long serialVersionUID = -2430928249419481058L;
	
	private Uuid id;
   
    public UuidKey(Uuid cookie) {
        this.id = cookie;
    }

    public final Uuid getId() {
        return id;
    }
    
    public final int hashCode() {
    	return id.hashCode();
    }
    
	public final boolean equals(Object obj) {
		if (obj instanceof UuidKey) {
			Uuid other = ((UuidKey) obj).getId();
			return other.equals(id);
		} else {
			return false;
		}
	}
	
    public String toString() {
        return "[Uuid Key=" + id + ']';
    }
}
