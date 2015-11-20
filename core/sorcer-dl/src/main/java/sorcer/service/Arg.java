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

package sorcer.service;

import sorcer.service.modeling.Model;

/**
 * Any named input value in particular a free variable. 
 */

public interface Arg {
	
	public String getName();

	public static Context getContext(Arg[] args) {
		  for (Arg arg : args) {
			  if (arg instanceof Context)
			   return (Context)arg;
		  }
		return null;
	}

	public static Exertion getExertion(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Exertion)
				return (Exertion)arg;
		}
		return null;
	}

	public static Model getModel(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Model)
				return (Model)arg;
		}
		return null;
	}

	public static Mogram getMogram(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Mogram)
				return (Mogram)arg;
		}
		return null;
	}

	public static Service getService(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Service)
				return (Service)arg;
		}
		return null;
	}
}


