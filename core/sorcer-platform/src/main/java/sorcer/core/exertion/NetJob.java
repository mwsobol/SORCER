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

package sorcer.core.exertion;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.security.util.Auth;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.*;

import java.rmi.RemoteException;

public class NetJob extends Job implements Invocation<Object> {

	private static final long serialVersionUID = 7060442151643838169L;

	public NetJob() {
		this("net job-" + count++);
	}

	public NetJob(String name) {
		super(name);
	}

    public NetJob(String name, Signature... signatures) {
        super(name);
        if (multiFi != null) {
            multiFi.getSelects().clear();
            multiFi.setSelect(null);
        }
        addSignature(signatures);
    }
	
	public NetJob(SorcerPrincipal principal) throws ExertionException {
		this("undefined" + count++, principal);
	}

	public NetJob(String name, SorcerPrincipal principal)
			throws ExertionException {
		super(name);
		if (principal != null)
			subject = Auth.createSubject(principal);
		setPrincipal(principal);
	}

	public static ServiceRoutine getTemplate() {
		NetJob temp = new NetJob();
		return temp;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Job#doJob(net.jini.core.transaction.Transaction)
	 */
	@Override
	public Job doJob(Transaction txn) throws MogramException,
			SignatureException, RemoteException, TransactionException {
		ServiceShell se = new ServiceShell(this);
		return (Job)se.exert(txn, null);
	}
	
}
