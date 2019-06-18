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

import sorcer.core.Index;
import sorcer.core.Tag;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Any named configuration parameter execute in particular a free variable.
 */

public interface Arg extends Serializable, Service {

	public String getName();

	public static Domain selectDomain(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Domain)
				return (Domain) arg;
		}
		return null;
	}

	public static Context selectContext(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Context)
				return (Context) arg;
		}
		return null;
	}

	public static Routine selectRoutine(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Routine)
				return (Routine) arg;
		}
		return null;
	}

	public static Mogram selectMogram(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Mogram)
				return (Mogram) arg;
		}
		return null;
	}

	public static List<Fidelity> selectFidelities(Arg[] args) {
		List<Fidelity> fiList = new ArrayList<>();
		for (Arg arg : args) {
			if (arg instanceof Fidelity)
				fiList.add((Fidelity) arg);
		}
		return fiList;
	}

	public static Service selectService(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Service)
				return (Service) arg;
		}
		return null;
	}

	public static Path selectPath(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Path)
				return (Path) arg;
		}
		return null;
	}

	public static Index selectIndex(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Index)
				return (Index) arg;
		}
		return null;
	}

	public static Integer selectIndexInt(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Index)
				return ((Index) arg).getIndex();
		}
		return -1;
	}

	public static Paths selectPaths(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Paths)
				return (Paths) arg;
		}
		return null;
	}

	public static Object get(Arg[] args, String path) throws EvaluationException, RemoteException {
		for (Arg arg : args) {
			if (arg instanceof Callable && arg.getName().equals(path)) {
				return ((Callable) arg).call(args);
			}
		}
		return null;
	}

	public static Object set(Arg[] args, String path, Object value) throws SetterException, RemoteException {
		for (Arg arg : args) {
			if (arg instanceof Callable && arg.getName().equals(path))
				((Setter) arg).setValue(value);
		}
		return value;
	}

	public static Service selectService(Arg[] args, String name) {
		for (Arg arg : args) {
			if (arg instanceof Service && arg.getName().equals(name))
				return (Service) arg;
		}
		return null;
	}

	public static Context.RequestReturn getReturnPath(Arg... args) {
		for (Arg a : args) {
			if (a instanceof Context.RequestReturn)
				return (Context.RequestReturn) a;
		}
		return null;
	}

	public static Tag getName(Arg[] args) {
		for (Arg arg : args) {
			if (arg instanceof Tag)
				return (Tag) arg;
		}
		return null;
	}

	public static String[] asStrings(Arg[] args) {
		String[] argsAsStrings = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			argsAsStrings[i] = args[i].toString();
		}
		return argsAsStrings;
	}

}


