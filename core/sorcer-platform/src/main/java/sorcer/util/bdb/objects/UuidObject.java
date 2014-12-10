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

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.SecureIdentifiable;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;

/**
 * A UuidObject serves as the Uuid/Object pair for persisted entities.
 *
 * @author Mike Sobolewski
 */
public class UuidObject implements SecureIdentifiable, Serializable {
	
	static final long serialVersionUID = -4412759790550308922L;
	
	private String name;
	
	private Uuid id;
	   
	private SorcerPrincipal principal;
	
	private String description;

	private Object object;

	private Date dateCreated;

	public UuidObject(Object object) {
		this(object, "");
	}
	  
	public UuidObject(Uuid uuid, Object object) {
		this(object, "");
		id = uuid;
	}
	
    public UuidObject(Object object, String description) {
		name = ""+object;
        this.object = object;
        principal = new SorcerPrincipal(System.getProperty("user.name"));
		principal.setId(principal.getName());
		this.description = description;
		dateCreated = new Date();
		if (id == null)
			id = UuidFactory.generate();
	}
    
    public final Uuid getId() {
        return id;
    }

    public final Object getObject() {
        return object;
    }
    
    public void setId(Uuid id) {
        this.id = id;
    }
    
    public String toString() {
        return "[id: " + id + " object: " + object + ']';
    }

	/* (non-Javadoc)
	 * @see sorcer.service.Identifiable#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.SecureIdentifiable#getPrincipal()
	 */
	@Override
	public Principal getPrincipal() {
		return principal;
	}
	
	public Date getDateCreated() {
		return dateCreated;
	}
}
