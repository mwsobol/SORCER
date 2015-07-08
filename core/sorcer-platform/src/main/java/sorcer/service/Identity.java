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
package sorcer.service;

import java.io.Serializable;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;

/**
 * Abstract Java class Identity 
 *
 */
@SuppressWarnings("serial")
public abstract class Identity implements Serializable, Identifiable {

	protected Uuid id;

	protected String name;

	//Value used for default finite difference calculations
	protected Object characteristicValue;

	protected boolean hasCharacteristicValue = false;

	/**
	 *  Identity - Class constructor
	 */
	public Identity() {
		id = UuidFactory.generate();
	}
	
	/**
	 * getId - Returns id
	 * @return Uuid
	 */
	public Uuid getId() {
		return id;
	}

	/**
	 * Assign a unique identifier
	 */
	public void setId(Uuid uuid) {
		 this.id = uuid;;
	}
	
	/**
	 * getName - Returns name
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * setName - Method that allows setting of the name of a Identity Object
	 * @param name -Name of the Identity Object
	 */
	public void setName(String name) {
		this.name = name;
	}

	public Object getCharacteristicValue() {
		return characteristicValue;
	}

	public void setCharacteristicValue(Object characteristicValue) {
		this.characteristicValue = characteristicValue;
		hasCharacteristicValue = true;
	}

	public String toString() {
		return "name: " + name +  ", id: " + id;
	}
}
