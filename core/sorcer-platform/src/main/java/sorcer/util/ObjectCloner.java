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

package sorcer.util;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.util.UUID;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.io.MarshalledInstance;
import sorcer.service.Exertion;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;
import sorcer.service.Task;

public class ObjectCloner {

	public static Object clone(Object o) {
		try {
			return new MarshalledObject<Object>(o).get();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object cloneWithNewIDs(Object o) {
		Object obj = null;
		try {
			obj = new MarshalledObject<Object>(o).get();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return renewIDs(obj);
	}

	public static Object cloneAnnotated(Object o) {
		try {
			return new MarshalledInstance(o).get(false);
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}

	public static Object cloneAnnotatedWithNewIDs(Object o) {
		Object obj = null;
		try {
			obj = new MarshalledInstance(o).get(false);
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return renewIDs(obj);
	}

	private static Object renewIDs(Object obj) {
		if (obj instanceof Job) {
			Uuid id = UuidFactory.generate();
			((ServiceExertion) obj).setId(UuidFactory.generate());
			for (Exertion each : ((Job) obj).getExertions()) {
				((ServiceExertion) each).setParentId(id);
				renewIDs(each);
			}
		} else if (obj instanceof Task) {
			((ServiceExertion) obj).setId(UuidFactory.generate());
		}
		return obj;
	}
}
