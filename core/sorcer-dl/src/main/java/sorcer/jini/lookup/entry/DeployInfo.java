/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

import net.jini.core.entry.Entry;

/**
 * Provides information on deployment attributes.
 *
 * @author Dennis Reedy
 */
public class DeployInfo implements Entry {
   private static final long serialVersionUID = 1l;
   public String type;
   public String unique;
   public Integer idle;
   
   public DeployInfo() {	   
   }
   
   public DeployInfo(final String type, final String unique, final Integer idle) {
	   this.type = type;
	   this.unique = unique;
	   this.idle = idle;
   }
   
   public String toString() {
	   return String.format("%s, %s, idle: %d", type, unique, idle);
   }
   
   
}
