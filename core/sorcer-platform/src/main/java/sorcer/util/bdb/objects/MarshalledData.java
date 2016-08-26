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

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.Date;

import net.jini.id.Uuid;
import net.jini.io.MarshalledInstance;
import sorcer.service.Identifiable;
import sorcer.service.SecureIdentifiable;

/**
 * A MarshalledData serves as the data in the key/data pair for multiple entities.
 *
 * <p> The MarshalledData class is used both as the storage entry for the
 * data as well as the object binding to the eval.  Because it is used directly
 * as storage data using serial format, it must be Serializable. </p>
 *
 * @author Mike Sobolewski
 */
public class MarshalledData implements Serializable, Identifiable {
	
	static final long serialVersionUID = 1043723312661570197L;
	
	private MarshalledInstance marshalledObject;

	private Uuid id;

	private String name;
	
	private Principal principal;

	private Date dateCreated;

    public MarshalledData(Object object) throws IOException {
    	if (object instanceof Identifiable) {
    		id = (Uuid)((Identifiable)object).getId();
    		name = ((Identifiable)object).getName();
    		if (object instanceof SecureIdentifiable)
    			principal = ((SecureIdentifiable)object).getPrincipal();
    	}
    	if (object instanceof MarshalledInstance)
    		this.marshalledObject = (MarshalledInstance)object;
    	else
    		this.marshalledObject = new MarshalledInstance(object);
    }
    
    public MarshalledInstance getMarshalledObject() {
        return marshalledObject;
    }

    public Principal getPrincipal() {
		return principal;
	}

	public void setPrincipal(Principal principal) {
		this.principal = principal;
	}
	
    public String toString() {
        return "[Marshalled Data =" + marshalledObject + ']';
    }
    
    public Object get() throws IOException, ClassNotFoundException {
    	return marshalledObject.get(false);
    }
	
	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Identifiable#getId()
	 */
	@Override
	public Object getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Identifiable#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
   
}