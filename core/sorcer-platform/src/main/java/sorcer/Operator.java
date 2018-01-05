/*
 * Copyright 2017 the original author or authors.
 * Copyright 2017 SorcerSoft.org.
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
package sorcer;

import sorcer.service.*;
import sorcer.service.modeling.Data;

import java.rmi.RemoteException;

/**
 * Created by Mike  Sobolewski on 5/4/17.
 */
public class Operator implements Request {
	private static Operator op = new Operator();

	public static Operator getInstance() {
		return op;
	}

	protected Operator() {
	}

	@Override
	public Object execute(Arg... args) throws ServiceException, RemoteException {

		if (args.length == 1 && args[0] instanceof Signature) {
			// requestor services
			Signature rs = (Signature) args[0];
			return rs.execute(rs);
		} else {
			throw new ServiceException("invalid service arguments");
		}
	}

	@Override
	public Data act(Arg... args) throws ServiceException, RemoteException {
		return null;
	}

	@Override
	public Data act(String entryName, Arg... args) throws ServiceException, RemoteException {
		return null;
	}

}
