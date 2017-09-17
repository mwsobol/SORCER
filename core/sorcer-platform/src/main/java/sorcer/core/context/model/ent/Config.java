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
		key = path;
	}

	public Config(String path, List<Setup> setups) {
		key = path;
		item = setups;
	}

	public Config(String path, Setup[] setups) {
		key = path;
		item = new ArrayList(setups.length);
		for (Setup s :setups) {
			item.add(s);
		}
	}

	public List<Setup> getSetups() {
		return item;
	}

	public void add(Setup setup) throws ContextException {
		item.add(setup);
		isValid(false);
	}

	public void addAll(List<Setup> list) throws ContextException {
		item.addAll(list);
		isValid(false);
	}

	public Setup remove(String name) throws ContextException {
		Setup deleted = null;
		for (Setup s : item) {
			if (s.getName().equals(name)) {
				deleted = s;
			}
		}
		item.remove(deleted);
		return deleted;
	}

}
