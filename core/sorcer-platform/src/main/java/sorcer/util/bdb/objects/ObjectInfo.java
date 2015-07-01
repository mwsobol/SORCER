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

import java.net.URL;
import java.security.Principal;
import java.util.Date;

import net.jini.id.Uuid;
import sorcer.core.provider.DatabaseStorer.Store;


/**
 * RecordInfo defines the object record format for SORCER data storage services.
 * 
 * @author Mike Sobolewski
 */
public class ObjectInfo {
	
	public Store type;
	public Uuid uuid;
	public Principal principal;
	public String info;
	public Date dateCreated;
	public URL url;
	
	public String describe() {
		StringBuilder sb = new StringBuilder();
		sb.append(uuid).append(" ").append(principal.getName()).append(" ").append(dateCreated);
		sb.append("\n").append(info).append("\n").append(url);
		return sb.toString();
	}
}
