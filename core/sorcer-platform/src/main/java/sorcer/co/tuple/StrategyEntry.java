/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

package sorcer.co.tuple;

import sorcer.service.Arg;
import sorcer.core.context.model.ent.Entry;
import sorcer.service.ContextException;
import sorcer.service.Strategy;

import java.io.IOException;
import java.net.URL;

public class StrategyEntry extends Entry<Strategy> implements Arg {

	private static final long serialVersionUID = 1L;

	public StrategyEntry(String path, Strategy strategy) {
		super(path, strategy);
		out = strategy;
		isValid = true;
	};

	public StrategyEntry(String path, URL strategy) {
		super(path);
		impl = strategy;
		out = null;
		isValid = true;
	};

	@Override
	public Strategy getData(Arg... args) throws ContextException {
		if (isValid && out != null) {
			return out;
		} else {
			try {
				out = (Strategy) ((URL) impl).getContent();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		isValid = true;
		return out;
	}

	public void setStrategy(Strategy strategy) {
		out = strategy;
		isValid = false;
	}
}