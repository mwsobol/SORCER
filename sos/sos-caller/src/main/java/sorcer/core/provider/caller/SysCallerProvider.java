/*
 * Copyright 2016 the original author or authors.
 * Copyright 2016 SorcerSoft.org.
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

package sorcer.core.provider.caller;

import com.sun.jini.start.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.model.ent.SysCall;
import sorcer.core.provider.SysCaller;
import sorcer.core.provider.ServiceExerter;
import sorcer.service.Context;
import sorcer.service.ContextException;
import java.rmi.RemoteException;

@SuppressWarnings("unchecked")
public class SysCallerProvider extends ServiceExerter implements SysCaller {

	private static Logger logger = LoggerFactory.getLogger(SysCallerProvider.class);

	public SysCallerProvider() throws Exception {
		// do nothing
	}

	public SysCallerProvider(String[] args, LifeCycle lifeCycle) throws Exception {
		super(args, lifeCycle);
	}

	@Override
	public Context exec(Context context) throws ContextException, RemoteException {
        String name = (String)context.getValue("key");
		if (name == null)
			name = context.getName();
        SysCall caller = new SysCall(name, context);
        return caller.evaluate();
	}
}
