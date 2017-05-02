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

package sorcer.core.context.model.ent;

import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Evaluator;
import sorcer.service.SetterException;

/**
 * Created by Mike Sobolewski on 12/9/16.
 */
public class Setup extends Entry<Context> {

	private Evaluator evaluator;

	public Setup(String path) {
		_1 = path;
	}

	public Setup(String path, Context context) {
		_1 = path;
		_2 = context;
	}

	public void setValue(Context value) {
		_2 = value;
		isValid(false);
	}

	public void setEntry(String path, Object value) throws SetterException {
		try {
			_2.putValue(path, value);
		} catch (ContextException e) {
			throw new SetterException(e);
		}
		isValid(false);
	}

	public Object getContextValue(String path) throws ContextException {
		return _2.getValue(path);
	}

	public Evaluator getEvaluator() {
		return evaluator;
	}

	public void setEvaluator(Evaluator evaluator) {
		this.evaluator = evaluator;
	}

}
