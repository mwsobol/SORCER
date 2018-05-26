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

import sorcer.service.Arg;
import sorcer.service.ContextException;
import sorcer.service.Response;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Row extends DataTable implements Response {

	private static final long serialVersionUID = 227568394484135275L;
	
	public Row() {
		super(1, 0);
		name = "Response";
	}
	
	public Row(List<String> name) {
		super(name, 1);
	}
	
	public Row(List<String> name, List rowData) {
		super(name, 1);
		addRow(rowData);
	}

	public Row(List<String> names, String rowData, String delimeter) {
		super(names, 1);
		String[] tokens = SorcerUtil.getTokens(rowData, delimeter);
		List<Double> doubles = new ArrayList<>();
		for (String token : tokens) {
			doubles.add(new Double(token));
		}
		cellType = Cell.DOUBLE;
		addRow(doubles);
	}

	public List<String> getNames() {
		return getColumnIdentifiers();
	}
	
@Override
public Object getValue(String name, Arg... args) throws ContextException, RemoteException {
		int index = Arg.selectIndex(args);
		List<String> cns = getColumnNames();
		for (int i = 0; i < cns.size(); i++) {
			if (name.equals(cns.get(i))) {
				if (index >=0) {
					return dataList.get(index).get(i);
				} else {
					return dataList.get(0).get(i);
				}
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

	public boolean compareTo(Object table) {
		return compareTo(table, 0.01);
	}

	public boolean compareTo(Object table, double delta) {
		if (dataList.size() != ((DataTable) table).dataList.size())
			return false;

		List row = dataList.get(0);
		if (table instanceof DataTable) {
			if (cellType == Cell.DOUBLE) {
				for (int i = 0; i < row.size(); i++) {
					if (row.get(i) instanceof Double) {
						Object x = row.get(i);
						Object y = ((DataTable) table).dataList.get(0).get(i);
						if (Math.abs((double) x - (double) y) > delta) {
							return false;
						}
					} else {
						if (!row.get(i).equals(((DataTable) table).dataList.get(0).get(i))) {
							return false;
						}
					}
				}
			} else {
				if (!row.equals(((DataTable) table).dataList.get(0))) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

}
