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

package sorcer.core.invoker;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import sorcer.core.context.model.ent.Proc;
import sorcer.core.context.model.ent.EntryModel;
import sorcer.service.Arg;
import sorcer.service.ArgSet;
import sorcer.service.Context;
import sorcer.service.InvocationException;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CallableInvoker<T> extends ServiceInvoker<T> {

	private static final long serialVersionUID = 7263463730257328563L;

	public static int POOL_SIZE = 3;
	
	private static ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
	
	private Callable callable;
	
	private Future<T> future;
	
	{
		defaultName = "collable-";
	}
	
	public CallableInvoker(EntryModel context) {
		super(context);
	}
	
	public CallableInvoker(EntryModel context, Callable callable, Proc... procEntries) {
		super(context);
		this.callable = callable;
		this.args = new ArgSet(procEntries);
	}

	public CallableInvoker(String name, Callable callable, Proc... procEntries) {
		super(name);
		this.callable = callable;
		this.args = new ArgSet(procEntries);
	}
	
	@Override
	public T invoke(Context context, Arg... args)
			throws RemoteException, InvocationException {
		try {
			if (context != null) {
				invokeContext.append(context);
			}
			future = pool.submit(callable);
			return future.get();
		} catch (Exception e) {
			throw new InvocationException(e);
		}
	}
	
	@Override
	public T invoke(Arg... entries) throws RemoteException,
			InvocationException {
		return invoke((Context) null, entries);
	}

}
