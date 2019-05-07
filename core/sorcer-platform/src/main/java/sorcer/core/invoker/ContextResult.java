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

import sorcer.core.context.ControlContext;
import sorcer.core.context.ServiceContext;
import sorcer.service.ContextException;
import sorcer.service.Routine;
import sorcer.util.exec.ExecUtils.CmdResult;

/**
 * @author Mike Sobolewski
 */

@SuppressWarnings("unchecked")
public class ContextResult<Objec> extends ServiceContext<Object> {

	private static final long serialVersionUID = 2716822310344630839L;
	
	public ContextResult(Object result, Routine exertion) throws ContextException {
		putValue("exit/eval", 0);
		putOutValue("result/out", result);
		putErrValue("result/err", ((ControlContext)exertion.getControlContext()).getExceptions());
		putValue("invoked/method", exertion.getProcessSignature());
	}
	
	public ContextResult(CmdResult result) throws ContextException {
		putValue("exit/eval", result.getExitValue());
		putOutValue("result/out", result.getOut());
		putErrValue("result/err", result.getErr());
		putValue("invoked/method", result.getErr());
	}
		
	public ContextResult(MethodInvoker<?> invoker) throws ContextException {
		putValue("exit/eval", 0);
		putValue("invoker/fiType", invoker.getSelector());
		putValue("invoked/method", invoker.getSelector());
		putValue("method/parameters", invoker.getParameters());
		putValue("method/parameter/types", invoker.getParameterTypes());
		//putValue("method/invokeContext", invoker.getInvokeContext());
	}
	
	public void setResult(Object object) throws ContextException {
		putOutValue("result/out", object);
	}
	
	public int getExitState() throws ContextException {
		return (Integer)getValue("exit/eval");
	}

	public Object getExitValue() throws ContextException {
		return getValue("result/out");
	}

	public String getErr() throws ContextException {
		return (String)getValue("result/err");
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Context result [out:\n");
		try {
			sb.append(getOut()).append("\nerr: ").append(getErr())
					.append("\nexitValue: ").append(getExitValue()).append("]");
		} catch (ContextException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
}
