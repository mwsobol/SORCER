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

package sorcer.ui.provider;

import javax.swing.table.AbstractTableModel;

public class PropertiesModel extends AbstractTableModel {

	public String getColumnName(int col) {
		return _cols[col].toString();
	}

	public int getColumnCount() {
		return _cols.length;
	}

	public int getRowCount() {
		return _data.size();
	}

	public Object getValueAt(int row, int col) {
		if (row >= _data.size())
			return "row out of bounds";
		Object rowData[] = (Object[]) _data.get(row);
		if (col >= rowData.length)
			return "";
		else
			return rowData[col];
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {

		return false;
	}

	private java.util.List _data;
	private java.util.List _fields;

	private Object _cols[] = { "Entry", "Value" };

	public PropertiesModel(java.util.List data, java.util.List fields) {
		_data = data;
		_fields = fields;

	}

	void update(java.util.List data, java.util.List fields) {
		_data = data;
		_fields = fields;
		fireTableDataChanged();
		// System.out.println("Table data changed!!!");

	}

}
