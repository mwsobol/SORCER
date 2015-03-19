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

package sorcer.util;

import sorcer.service.ModelResponse;

import java.util.List;


/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Response extends Table implements ModelResponse {

	private static final long serialVersionUID = 227568394484135275L;
	
	public Response() {
		super(1, 0);
		name = "Response";
	}
	
	public Response(List<String> names) {
		super(names, 1);
	}
	
	public Response(List<String> names, List rowData) {
		super(names, 1);
		addRow(rowData);
	}

	public List<String> getNames() {
		return getColumnIdentifiers();
	}
	
	public Object getValue(String name) {
		List<String> cns = getColumnNames();
		for (int i = 0; i < cns.size(); i++) {
			if (name.equals(cns.get(i))) {
				return dataList.get(0).get(i);
			}
		}
		return null;
	}
	
	public List getValues() {
		return getRow(0);
	}

	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName() + ": " + name +"\n");
		List<String> cns = getColumnNames();
		if (cns != null) {
			sb.append(cns);
			sb.append("\n").append(dataList.get(0));
		}
		return sb.toString();
	}
}
