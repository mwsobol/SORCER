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

import sorcer.core.context.model.ent.Function;
import sorcer.service.Arg;
import sorcer.service.Entry;
import sorcer.service.Strategy;

import java.io.IOException;
import java.net.URL;

public class StrategyEntry extends Entry<String, Strategy> implements Arg {

	private static final long serialVersionUID = 1L;

	private URL url;

	public StrategyEntry(String path, Strategy strategy) {
		super(path);
		item = strategy;
	};

	public StrategyEntry(String path, URL strategy) {
		super(path);
		url = strategy;
	};

	public void setStrategy(URL strategy) {
		url = strategy;
	}

	public Strategy strategy() {
		if (url != null) {
			try {
				return (Strategy) url.getContent();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return item;
	}
}