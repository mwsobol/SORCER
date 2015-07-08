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
 * Created Sat Mar 05 06:45:27 GMT 2005
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;

class MethodListView extends JPanel {
	private JFrame _frame;
	private final JTable table;
	private int NODE_COLUMN = 0;

	MethodListView(JFrame frame, DefaultMutableTreeNode parent) {
		_frame = frame;
		setLayout(new BorderLayout());

		JLabel title = new JLabel("Methods");
		Font font = title.getFont();
		title
				.setFont(new Font(font.getFamily(), Font.BOLD,
						font.getSize() + 1));
		title.setIcon(TreeRenderer._sidIcon);
		add(title, BorderLayout.NORTH);

		int nKids = parent.getChildCount();
		Object[][] data = new Object[nKids][2];
		for (int i = 0; i < nKids; i++) {
			PropertiesNode pn = (PropertiesNode) ((DefaultMutableTreeNode) parent
					.getChildAt(i)).getUserObject();
			data[i][NODE_COLUMN] = pn;
		}
		DefaultTableModel model = new DefaultTableModel() {
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};
		model.setDataVector(data, new String[] { "Method", "Return value" });

		table = new JTable(model);
		table.setDefaultRenderer(Object.class, new MethodCellRenderer());

		JScrollPane sp = new JScrollPane(table);
		add(sp, BorderLayout.CENTER);

		JPanel ctrls = new JPanel();
		add(ctrls, BorderLayout.SOUTH);

		final JButton exec = new JButton("Invoke");
		ctrls.add(exec);
		exec.setEnabled(false);

		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				int selected = table.getSelectedRow();
				if (selected != -1) {
					PropertiesNode node = (PropertiesNode) table.getValueAt(
							selected, NODE_COLUMN);
					Method method = (Method) node.getAdditionalData();
					// if(method.getName().startsWith("get")){
					Class[] params = method.getParameterTypes();
					if (params.length == 0) {
						exec.setEnabled(!method.getReturnType().getName()
								.equals("void"));
					} else {
						exec.setEnabled(false);
					}
				}
			}
		});

		exec.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				Component gp = _frame.getGlassPane();

				try {

					gp.setVisible(true);
					gp.setCursor(Cursor.getPredefinedCursor(3));

					TableModel model = table.getModel();
					int[] selectedRows = table.getSelectedRows();
					int nRows = selectedRows.length;

					Object[] noParams = new Object[] {};

					for (int i = 0; i < nRows; i++) {
						PropertiesNode node = (PropertiesNode) model
								.getValueAt(selectedRows[i], NODE_COLUMN);
						Object proxy = node.getProxy();

						Method method = (Method) node.getAdditionalData();
						// if(method.getName().startsWith("get")){
						Class[] params = method.getParameterTypes();
						if (params.length == 0) {

							try {
								method.setAccessible(true);

								Object result = method.invoke(proxy, noParams);

								if (result == null) {
									result = "null";
								}

								// result=MethodResultFilter.apply(method,result);

								Class resClazz = result.getClass();
								Object[] data = null;
								if (resClazz.isArray()) {
									data = (Object[]) result;

								} else {
									data = new Object[] { result };
								}
								model.setValueAt(parseArray(data),
										selectedRows[i], 1);
							} catch (Exception ex) {

								ex.printStackTrace();
							}
						}
						// }
					}
				} finally {
					gp.setCursor(Cursor.getDefaultCursor());
					gp.setVisible(false);
				}
			}
		});
	}

	private String parseArray(Object[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length - 1; i++) {
			if (data[i] != null) {
				buf.append(data[i].toString());
				buf.append(" ");
			}
		}
		if (data.length > 0 && data[data.length - 1] != null) {
			buf.append(data[data.length - 1].toString());
		}
		return buf.toString();
	}

	private class MethodCellRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {

			JLabel lab = (JLabel) super.getTableCellRendererComponent(table,
					value, isSelected, hasFocus, row, column);
			lab.setIcon(null);
			lab.setForeground(Color.black);
			if (column == NODE_COLUMN) {
				PropertiesNode pn = (PropertiesNode) table.getValueAt(row,
						NODE_COLUMN);
				Method method = (Method) pn.getAdditionalData();
				lab.setText(LusTree.parseMethod(method));
				Class[] params = method.getParameterTypes();
				if (!(params.length == 0 && !method.getReturnType().getName()
						.equals("void"))) {
					lab.setForeground(Color.lightGray);
				}

			}
			return lab;
		}
	}
}
