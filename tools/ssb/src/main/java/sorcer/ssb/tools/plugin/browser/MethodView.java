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

package sorcer.ssb.tools.plugin.browser;

/*
 *
 * Created on 27 May 2002, 06:59
 */

/**
 *
 * @author  Phil
 * @version
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

class MethodView extends JPanel implements ActionListener {

	private Method _method;
	private Object _proxy;
	private MethodTableModel _model;
	private JTable _table;
	private JButton _exec;

	MethodView(PropertiesNode node) throws Exception {

		_proxy = node.getProxy();

		_method = (Method) node.getAdditionalData();

		Class retType = _method.getReturnType();
		Class[] params = _method.getParameterTypes();
		String name = _method.getName();
		int nRows = params.length + 2;

		Object[][] data = new Object[nRows][3];

		data[0][0] = "Return type";
		data[0][1] = retType.getName();
		data[1][0] = "Method name";
		data[1][1] = name;

		boolean executable = true;
		for (int i = 0; i < params.length; i++) {
			data[2 + i][0] = "Param " + i;
			data[2 + i][1] = params[i].getName();

			if (executable) {
				// once false keep false
				executable = checkClass(params[i]);
			}
		}
		String[] cols = new String[] { "Property", "Value" };
		if (executable && params.length > 0) {
			cols = new String[] { "Property", "Value", "Input" };
		}
		_model = new MethodTableModel(data, cols);

		setLayout(new BorderLayout());
		JLabel title = new JLabel(node.toString());
		Font font = title.getFont();
		title
				.setFont(new Font(font.getFamily(), Font.BOLD,
						font.getSize() + 1));
		title.setIcon(TreeRenderer._sidIcon);
		add(title, BorderLayout.NORTH);

		_table = new JTable(_model);
		// _table.getColumn("Property").setPreferredWidth(80);
		// _table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		JScrollPane sp = new JScrollPane(_table);
		add(sp, BorderLayout.CENTER);
		// create the execute button
		JPanel ctrls = new JPanel();
		add(ctrls, BorderLayout.SOUTH);

		_exec = new JButton("Invoke");
		ctrls.add(_exec);
		_exec.setEnabled(false);

		if (executable) {
			_exec.setEnabled(true);
			_exec.addActionListener(this);
		}

	}

	public void actionPerformed(ActionEvent e) {
		Frame frame = JOptionPane.getFrameForComponent(this);

		try {

			Object[] params = getParams();

			_method.setAccessible(true);

			Object result = _method.invoke(_proxy, params);
			// special filters for handling known cases such as JoinAdmin
			// could be exended to load additional filters from properties file
			// result=MethodResultFilter.apply(_method,result);

			if (result != null) {
				Class resClazz = result.getClass();
				Object[] data = null;
				if (resClazz.isArray()) {
					data = (Object[]) result;
				} else {
					data = new Object[] { result };
				}

				if (data != null && data.length > 0) {
					int nResults = data.length;

					Object[][] tableData = new Object[nResults][2];
					tableData[0][0] = "Return value";
					for (int i = 0; i < nResults; i++) {
						tableData[i][1] = data[i];
					}
					_model.update(tableData);
					_exec.setEnabled(false);
					_table.setRowSelectionInterval(0, nResults - 1);
					// _table.setSelectionBackground(Color.green);
					_table.setSelectionForeground(Color.black);

				}
			} else {
				if (_method.getReturnType().getName().equals("void")) {
					JOptionPane.showMessageDialog(frame,
							"Method invoked successfully\n", "SSB",
							JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane
							.showMessageDialog(
									frame,
									"Method invoked successfully, but returned a null value",
									"SSB", JOptionPane.INFORMATION_MESSAGE);
				}

			}
		} catch (InvocationTargetException ex) {
			ex.printStackTrace();
			Object msg = ex.getTargetException().getMessage();
			if (ex == null) {
				msg = ex;
			}
			JOptionPane.showMessageDialog(frame, msg, "SSB",
					JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception ex) {
			ex.printStackTrace();
			Object msg = ex.getMessage();
			if (ex == null) {
				msg = ex;
			}
			JOptionPane.showMessageDialog(frame, msg, "SSB",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private boolean checkClass(Class c) {
		Class toCheck = c;
		if (c.isArray()) {
			toCheck = c.getComponentType();
		}
		if (toCheck.isPrimitive()) {
			return true;
		}
		if (toCheck.equals(java.lang.String.class)) {
			return true;
		}
		return false;

	}

	private Object[] getParams() throws Exception {

		Class[] stringCtor = new Class[] { java.lang.String.class };
		// java.lang.reflect.Constructor ctor=c.getConstructor(new
		// Class[]{java.lang.String.class});
		Class[] params = _method.getParameterTypes();
		Object[] objectParams = new Object[params.length];
		for (int i = 0; i < params.length; i++) {

			Object val = _model.getValueAt(2 + i, 2);
			if (val == null) {
				throw new Exception("Missing parameter value " + i);
			}

			Class paramClass = params[i];
			if (paramClass.isArray()) {
				paramClass = params[i].getComponentType();
			}
			if (paramClass.isPrimitive()) {
				paramClass = getWrapperClass(params[i]);
			}
			Constructor ctor = paramClass.getConstructor(stringCtor);

			if (params[i].isArray()) {
				// parse the table value which is in CSV format
				java.util.List arrayElements = new java.util.ArrayList();
				StringTokenizer tok = new StringTokenizer(val.toString(), ",");
				while (tok.hasMoreTokens()) {
					String paramValue = tok.nextToken();
					Object instance = ctor
							.newInstance(new Object[] { paramValue });
					arrayElements.add(instance);
				}
				int arraySize = arrayElements.size();

				Object[] array = (Object[]) Array.newInstance(params[i]
						.getComponentType(), arraySize);
				for (int e = 0; e < arraySize; e++) {
					Array.set(array, e, arrayElements.get(e));
				}
				objectParams[i] = array;

			} else {

				objectParams[i] = ctor.newInstance(new Object[] { val });
			}
		}
		return objectParams;
	}

	private Class getWrapperClass(Class primitive) throws Exception {

		if (primitive.equals(int.class)) {
			return Integer.class;
		}
		if (primitive.equals(long.class)) {
			return Long.class;
		}
		if (primitive.equals(float.class)) {
			return Float.class;
		}
		if (primitive.equals(double.class)) {
			return Double.class;
		}
		throw new Exception(primitive.getName() + " not supported");
	}

	private class MethodTableModel extends AbstractTableModel {

		private Object[][] _data;
		private Object[] _cols;

		MethodTableModel(Object[][] data, Object[] cols) {
			_data = data;
			_cols = cols;
		}

		void update(Object[][] data) {
			_data = data;
			fireTableDataChanged();
		}

		// tablemodel
		public String getColumnName(int col) {
			return _cols[col].toString();
		}

		public int getColumnCount() {
			return _cols.length;
		}

		public int getRowCount() {
			return _data.length;
		}

		public Object getValueAt(int row, int col) {
			if (row >= _data.length || col >= _data[0].length) {
				// happens when executing method with input & return values
				return null;
			}
			return _data[row][col];
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return rowIndex > 1;
		}

		public void setValueAt(Object value, int row, int col) {
			_data[row][col] = value;
			fireTableDataChanged();
		}
	}
}
