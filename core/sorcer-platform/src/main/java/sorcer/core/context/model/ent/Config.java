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

import sorcer.service.ContextException;
import sorcer.service.Setup;

import java.util.ArrayList;
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
		_2 = new ArrayList(setups.length);
		for (Setup s :setups) {
			_2.add(s);
		}
	}

	public List<Setup> getSetups() {
		return _2;
	}

	public void add(Setup setup) throws ContextException {
		_2.add(setup);
		setValid(false);
	}

	public void addAll(List<Setup> list) throws ContextException {
		_2.addAll(list);
		setValid(false);
	}

	public Setup remove(String name) throws ContextException {
		Setup deleted = null;
		for (Setup s : _2) {
			if (s.getName().equals(name)) {
				deleted = s;
			}
		}
		_2.remove(deleted);
		return deleted;
	}

}
