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

import sorcer.core.Tag;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Any named configuration parameter value in particular a free variable.
 */

public interface Arg extends Serializable {
	
	public String getName();

	public static Domain getServiceModel(Arg[] args) {
		  for (Arg arg : args) {
			  if (arg instanceof Domain)
			   return (Domain)arg;
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

	public static Path getPath(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Path)
				return (Path)arg;
		}
		return null;
	}

	public static Signature.Paths getPaths(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Signature.Paths)
				return (Signature.Paths)arg;
		}
		return null;
	}

	public static Object getValue(Arg[] args, String path) throws EvaluationException, RemoteException {
		for (Arg arg : args) {
			if (arg instanceof Callable && arg.getName().equals(path))
				return ((Callable)arg).call(args);
		}
		return null;
	}

	public static void setArgValue(Arg[] args, String path, Object value) throws SetterException, RemoteException {
		for (Arg arg : args) {
			if (arg instanceof Callable && arg.getName().equals(path))
				((Setter)arg).setValue(value);
		}
	}

	public static Callable getEntry(Arg[] args, String name) {
		for (Arg arg : args) {
			if (arg instanceof Callable && arg.getName().equals(name))
				return (Callable)arg;
		}
		return null;
	}

	public static Signature.ReturnPath getReturnPath(Arg... args) {
		for (Arg a : args) {
			if (a instanceof Signature.ReturnPath)
				return (Signature.ReturnPath) a;
		}
		return null;
	}

	public static Tag getName(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Tag)
				return (Tag)arg;
		}
		return null;
	}

	public static String[] asStrings(Arg[] args) {
		String[] argsAsStrings = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			argsAsStrings[i] = argsAsStrings[i].toString();
		}
		return argsAsStrings;
	}
}


