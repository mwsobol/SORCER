/*
 *
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sorcer.service.*;

/**
 * The alternative Invoker that executes sequentially a collection optional
 * invokers. It invokes the first optInvoker in the collection such that its
 * condition is true.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AltInvoker<V> extends ServiceInvoker<V> implements ConditionalInvocation {
	
	protected List<OptInvoker> optInvokers;
	
	
	public AltInvoker(String name, Evaluator... optInvokers) {
		super(name);
		List<OptInvoker> options = new ArrayList<>();
		for (Evaluator ev : optInvokers) {
			options.add((OptInvoker)ev);
		}
		this.optInvokers = options;
	}

	public AltInvoker(String name, List<OptInvoker> optInvokers) {
		super(name);
		this.optInvokers = optInvokers;
	}
	
	@Override
	public V evaluate(Arg... args) throws EvaluationException {
		
		for (OptInvoker opt : optInvokers) {
			try {
				if (opt.getCondition() != null) {
					opt.getCondition().setStatus(null);
					if (opt.getCondition().getConditionalContext() == null) {
						opt.getCondition().setConditionalContext(invokeContext);
					} else {
						opt.getCondition().getConditionalContext().append(invokeContext);
					}

					if (opt.target.getInvokeContext() == null) {
						opt.target.setInvokeContext(invokeContext);
					} else {
						opt.target.getInvokeContext().append(invokeContext);
					}
				}
				if (opt.isTrue()) {
					return (V) opt.evaluate(args);
				}
			} catch (Exception e) {
				throw new EvaluationException(e);
			}
		}
		return null;

	}
	
	public OptInvoker getInvoker(int index) {
		return optInvokers.get(index);
	}

	@Override
	public Condition getCondition() {
		return null;
	}
}
