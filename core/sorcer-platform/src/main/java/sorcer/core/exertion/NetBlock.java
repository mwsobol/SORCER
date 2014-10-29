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

package sorcer.core.exertion;

import java.rmi.RemoteException;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.core.context.ServiceContext;
import sorcer.core.provider.Concatenator;
import sorcer.core.provider.exerter.ExertionDispatcher;
import sorcer.core.signature.NetSignature;
import sorcer.security.util.Auth;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.Block;
import sorcer.service.Context;
import sorcer.service.Evaluation;
import sorcer.service.ExertionException;
import sorcer.service.Invocation;
import sorcer.service.ServiceExertion;
import sorcer.service.Signature.Type;
import sorcer.service.SignatureException;

public class NetBlock extends Block implements Evaluation<Object>, Invocation<Object> {

	private static final long serialVersionUID = 3420416993635766567L;
	
	public NetBlock() throws SignatureException  {
		super(null, new NetSignature("service", Concatenator.class, Type.SRV));
	}

	public NetBlock(String name) throws SignatureException {
		super(name, new NetSignature("service", Concatenator.class, Type.SRV));
	}
	
	public NetBlock(String name, Context context)
			throws SignatureException {
		this(name);
		if (context != null)
			this.dataContext = (ServiceContext) context;
	}
	
	public NetBlock(SorcerPrincipal principal) throws ExertionException {
		this("undefined" + count++, principal);
	}

	public NetBlock(String name, SorcerPrincipal principal)
			throws ExertionException {
		super(name);
		if (principal != null)
			subject = Auth.createSubject(principal);
		setPrincipal(principal);
	}

	public static ServiceExertion getTemplate() {
		NetBlock temp = null;
		try {
			temp = new NetBlock();
		} catch (SignatureException e) {
			// ignore it
		}
		return temp;
	}

	public Block doBlock(Transaction txn) throws ExertionException,
			SignatureException, RemoteException, TransactionException {
		ExertionDispatcher se = new ExertionDispatcher(this);
		return (Block)se.exert(txn, null);
	}
	
}
