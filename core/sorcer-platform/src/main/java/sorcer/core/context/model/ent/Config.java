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

import java.util.Arrays;
import java.util.List;

/**
 * Created by Mike Sobolewski on 04/25/17.
 */
public class Config extends Entry<List<Setup>> {

	public Config(String path) {
		_1 = path;
	}

	public Config(String path, List<Setup> setups) {
		_1 = path;
		_2 = setups;
	}

	public Config(String path, Setup[] setups) {
		_1 = path;
		_2 = Arrays.asList(setups);
	}

	public void add(Setup setup) throws ContextException {
		_2.add(setup);
		isValid(false);
	}

	public Object remove(String name) throws ContextException {
		for (Setup s : _2) {
			if (s.getName().equals(name)) {
				_2.remove(s);
			}
		}
		return _2.remove(name);
	}

}
