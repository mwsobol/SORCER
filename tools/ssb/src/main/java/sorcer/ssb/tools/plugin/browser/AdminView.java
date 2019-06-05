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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.CellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.lookup.entry.ServiceControlled;
import net.jini.lookup.entry.UIDescriptor;
import net.jini.security.ProxyPreparer;
import sorcer.ssb.jini.studio.TiledDesktopPane;

import com.sun.jini.admin.DestroyAdmin;

public class AdminView {

	private JInternalFrame _jif[];
	private Object _proxy;
	private Object _adminProxy;
	private JoinAdmin _joinAdmin;
	private JComponent _view;
	private String _name;
	private ServiceBrowserUI _browser;
	public static boolean DESTROYABLE = true;
		
	public AdminView(final Object proxy, String name,
			ProxyPreparer adminPreparer, ServiceBrowserUI browser)
			throws Exception {
		_proxy = proxy;
		_name = name;

		_browser = browser;
		// setLayout( new BorderLayout() );

		Administrable admin = (Administrable) _proxy;
		_adminProxy = adminPreparer.prepareProxy(admin.getAdmin());
		if (_adminProxy instanceof JoinAdmin) {
			_joinAdmin = (JoinAdmin) _adminProxy;
		}

		/*
		 * ServiceBrowserUI._logger.info("Getting attributes");
		 * 
		 * Entry [] atts=_joinAdmin.getLookupAttributes(); LookupLocator []
		 * locs=_joinAdmin.getLookupLocators(); String []
		 * grps=_joinAdmin.getLookupGroups();
		 * 
		 * ServiceBrowserUI._logger.info("Adding panel");
		 * 
		 * add(
		 * getRuntimePanel(atts,locs,grps,admin.getAdmin()),BorderLayout.CENTER
		 * );
		 * 
		 * ServiceBrowserUI._logger.info("exit ctor");
		 */

	}

	public JComponent makeGUI() throws Exception {
		if (_joinAdmin == null) {
			return createFrame(getDestroyPanel(), "Destroy", 0);
		}
		Entry[] atts = _joinAdmin.getLookupAttributes();
		LookupLocator[] locs = _joinAdmin.getLookupLocators();
		String[] grps = _joinAdmin.getLookupGroups();
		_view = getRuntimePanel(atts, locs, grps, _adminProxy);
		return _view;
	}

	private JPanel createPropertiesView(Object data[], final Class type) {
		if (data == null) {
			data = new Object[] {};
		}
		final ListTableModel model = new ListTableModel(data, type);
		final JTable list = new JTable(model);
		if (data.length > 0) {
			list.setRowSelectionInterval(0, 0);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		JScrollPane sp = new JScrollPane(list);
		sp.setColumnHeader(null);
		JPanel view = new JPanel();
		view.setLayout(new BorderLayout());
		JPanel ctrls = new JPanel();
		JButton add = new JButton("  Add  ");
		JButton del = new JButton("Delete ");
		JButton apply = new JButton(" Apply ");
		ctrls.add(add);
		ctrls.add(del);
		ctrls.add(apply);
		model.setButtons(del, apply);

		add.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				model.addRow();

				list.editCellAt(model.getRowCount() - 1, 0);
			}

		});
		del.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int row = list.getSelectedRow();
				if (row != -1) {

					CellEditor editor = list.getCellEditor();
					if (editor != null) {
						editor.stopCellEditing();
					}
					model.deleteRow(row);
				}
			}

		});
		apply.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				CellEditor editor = list.getCellEditor();
				if (editor != null) {
					editor.stopCellEditing();
				}
				SwingUtilities.invokeLater(_browser.wrap(new Runnable() {
					public void run() {
						doUpdate(model, type);
					}
				}));
			}

		});
		view.add(sp, "Center");
		view.add(ctrls, "South");

		return view;
	}

	private void doUpdate(ListTableModel model, Class type) {
		try {
			// ServiceInfo si = _serviceInfo;
			// ServiceInfo newSi = null;
			int size = model.getRowCount();
			if (type.equals(LookupLocator.class)) {
				LookupLocator locs[] = new LookupLocator[size];
				for (int i = 0; i < size; i++) {
					Object value = model.getValueAt(i, 0);
					if (value instanceof LookupLocator)
						locs[i] = (LookupLocator) value;
					else
						locs[i] = new LookupLocator(value.toString());
				}
				_joinAdmin.setLookupLocators(locs);
				// _serviceInfo._locs=locs;
				// newSi = new ServiceInfo(si._sid, si._name, si._atts, locs,
				// si._grps);
			} else {
				String grps[] = new String[size];
				for (int i = 0; i < size; i++) {
					Object value = model.getValueAt(i, 0);
					grps[i] = value.toString();
				}
				_joinAdmin.setLookupGroups(grps);
				// _serviceInfo._grps=grps;
				// newSi = new ServiceInfo(si._sid, si._name, si._atts,
				// si._locs, grps);
			}

			// _runtimeNode.updateServiceInfo(newSi);
		} catch (Exception ex) {
			ex.printStackTrace();
			String msg = ex.getMessage();
			int pos = msg.indexOf("\n");
			if (pos != -1) {
				msg = msg.substring(0, pos);
			}
			JOptionPane.showMessageDialog(_view, msg, "SSB",
					JOptionPane.ERROR_MESSAGE);
			;
		}
	}

	private JDesktopPane getRuntimeIfPanel(Entry[] atts, LookupLocator[] locs,
			String[] grps, Object adminProxy) {

		ServiceBrowserUI._logger.debug("start");

		JComponent c1 = new AttribsPanel(atts, false);
		JComponent c2 = createPropertiesView(locs,
				net.jini.core.discovery.LookupLocator.class);
		JComponent c3 = createPropertiesView(grps, java.lang.String.class);
		int count = 0;
		int destroy = canDestroy(adminProxy) ? 1 : 0;

		_jif = new JInternalFrame[3 + destroy];
		_jif[2] = createFrame(c1, "Lookup Attributes", count++);
		_jif[1] = createFrame(c3, "Lookup Groups", count++);
		_jif[0] = createFrame(c2, "Lookup Locators", count++);
		if (destroy != 0) {
			_jif[3] = _jif[0];
			// if(isLifeCycle){
			// _jif[0]= createFrame( getLifeCyclePanel(), "LifeCycle",count++);
			// }else{
			_jif[0] = createFrame(getDestroyPanel(), "Destroy", count++);
			// }
		}
		JDesktopPane mdiPane = new TiledDesktopPane(_jif);
		mdiPane.setOpaque(false);
		mdiPane.setBackground(Color.lightGray);

		for (int i = 0; i < _jif.length; i++)
			mdiPane.add(_jif[i], JLayeredPane.PALETTE_LAYER);

		ServiceBrowserUI._logger.debug("end");

		return mdiPane;
	}

	private JSplitPane getRuntimePanel(Entry[] atts, LookupLocator[] locs,
			String[] grps, Object adminProxy) {

		ServiceBrowserUI._logger.debug("start");

		JComponent c1 = new AttribsPanel(atts, false);
		JComponent c2 = createPropertiesView(locs,
				net.jini.core.discovery.LookupLocator.class);
		JComponent c3 = createPropertiesView(grps, java.lang.String.class);

		TitledBorder title;
		title = BorderFactory.createTitledBorder("Lookup Attributes");
		c1.setBorder(title);
		title = BorderFactory.createTitledBorder("Lookup Groups");
		c3.setBorder(title);
		title = BorderFactory.createTitledBorder("Lookup Locators");
		c2.setBorder(title);

		int destroy = canDestroy(adminProxy) ? 1 : 0;

		JPanel c4 = new JPanel();
		c4.setLayout(new GridLayout(0, 1));
		c4.add(c3);
		c4.add(c2);
		if (destroy != 0) {
			JPanel dp = getDestroyPanel();
			title = BorderFactory.createTitledBorder("Destroy");
			dp.setBorder(title);
			c4.add(dp);
		}

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, c1,
				c4);
		splitPane.setOneTouchExpandable(true);
		Dimension minimumSize = new Dimension(0, 0);
		c1.setMinimumSize(new Dimension(300, 400));
		c4.setMinimumSize(minimumSize);
		splitPane.setDividerLocation(0.75);
		// splitPane.setResizeWeight(0.75);

		ServiceBrowserUI._logger.debug("end");
		// splitPane.resetToPreferredSizes();
		return splitPane;
	}

	private JInternalFrame createFrame(JComponent comp, String title, int num) {
		JInternalFrame mdiWin = new JInternalFrame();
		mdiWin.setTitle(title);
		mdiWin.getContentPane().add(comp, "Center");
		mdiWin.setResizable(true);
		mdiWin.setVisible(true);
		mdiWin.setFrameIcon(TreeRenderer._luAttsIcon);
		return mdiWin;
	}

	public static boolean canDestroy(Object adminProxy) {
		if (adminProxy instanceof DestroyAdmin) {
			return true;
		}		
		return false;
	}

	/*
	 * private JComponent getLifeCyclePanel(){ JPanel p=new JPanel();
	 * p.setLayout( new BorderLayout() );
	 * 
	 * ArrayList list=new ArrayList(); try{ list.add( new
	 * Object[]{"Clock Offet",""+lcAdmin.getClockOffset()}); list.add( new
	 * Object[]{"Service State",""+lcAdmin.getServiceState()}); }catch(Exception
	 * ex){ list.add( new Object[]{"Exception",""+ex}); } list.add( new
	 * Object[]{"Retire 1","0"}); list.add( new Object[]{"Retire 2","0"});
	 * 
	 * final LifeCycleTableModel model=new LifeCycleTableModel(list);
	 * 
	 * JTable dataTable=new JTable(model); p.add(new
	 * JScrollPane(dataTable),BorderLayout.CENTER);
	 * 
	 * JPanel ctrls=new JPanel(); JButton destroy=new JButton("Destroy");
	 * 
	 * destroy.addActionListener( new ActionListener(){ public void
	 * actionPerformed(ActionEvent evt){ SwingUtilities.invokeLater( new
	 * Runnable(){
	 * 
	 * public void run(){ try{
	 * 
	 * lcAdmin.destroy(); Container parent=_view.getParent(); if(parent!=null){
	 * parent.remove(_view); } //remove from parent view //removeFromParent();
	 * }catch(Exception ex){ ex.printStackTrace(); //ta.setText(""+ex); } } });
	 * } });
	 * 
	 * JButton retire=new JButton("Retire"); retire.addActionListener( new
	 * ActionListener(){ public void actionPerformed(ActionEvent evt){ try{
	 * Object r1=model.getValueAt(2,1); Object r2=model.getValueAt(3,1); long
	 * l1=Long.parseLong(r1.toString()); long l2=Long.parseLong(r2.toString());
	 * 
	 * lcAdmin.retire(l1,l2);
	 * 
	 * }catch(Exception ex){ JOptionPane.showMessageDialog(_view,ex); } } });
	 * ctrls.add(retire); ctrls.add(destroy); p.add( ctrls, BorderLayout.SOUTH);
	 * return p; }
	 */
	private JPanel getDestroyPanel() {

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());

		final JTextArea ta = new JTextArea();
		if (SorcerServiceBrowser._theme != null) {
			ta.setFont(SorcerServiceBrowser._theme.getControlTextFont());
		} else {
			ta.setFont(p.getFont());
		}

		ta.setEditable(false);
		ta.setText(_name);
		// String msg="This option will stop the service";

		// ta.setText( msg );
		p.add(new JScrollPane(ta), BorderLayout.CENTER);
		JPanel ctrls = new JPanel();
		JButton destroy = new JButton("Destroy");
		destroy.setEnabled(DESTROYABLE);

		destroy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				ta.setText("Destroying service...");
				SwingUtilities.invokeLater(_browser.wrap(new Runnable() {

					public void run() {
						try {							
							DestroyAdmin destroyAdmin = (DestroyAdmin) _adminProxy;
							destroyAdmin.destroy();
							ta.setText("Destroy() executed OK");
							/*
							 * //disabled for Virgil Container
							 * parent=_view.getParent(); if(parent!=null){
							 * parent.remove(_view); }
							 */
							// remove from parent view
							// removeFromParent();
						} catch (Throwable ex) {
							Throwable cause = ex.getCause();
							ex.printStackTrace();
							if (cause != null) {
								ex = cause;
							}
							ta.setText("" + ex.getMessage());
						}
					}
				}));

			}
		});

		ctrls.add(destroy);
		p.add(ctrls, BorderLayout.SOUTH);
		return p;
	}

	private Object[] getUIRoles(Entry atts[]) {
		java.util.List list = new ArrayList();
		for (int i = 0; atts != null && i < atts.length; i++)
			if (atts[i] instanceof UIDescriptor) {
				UIDescriptor desc = (UIDescriptor) atts[i];
				if (desc.role != null)
					list.add(desc.role);
			}

		return list.toArray();
	}

	private class ListTableModel extends AbstractTableModel {

		private JButton _del;
		private JButton _apply;
		private java.util.List _data;
		private Class _type;

		ListTableModel(Object data[], Class type) {
			_data = new ArrayList();
			_type = type;
			for (int i = 0; i < data.length; i++)
				_data.add(data[i]);

		}

		void setButtons(JButton del, JButton apply) {
			_del = del;
			_apply = apply;
			_apply.setEnabled(false);
			_del.setEnabled(_data.size() > 0);

		}

		void addRow() {
			_data.add("");
			_apply.setEnabled(true);
			_del.setEnabled(true);
			fireTableDataChanged();
		}

		void deleteRow(int row) {
			_data.remove(row);
			_apply.setEnabled(true);
			_del.setEnabled(_data.size() > 0);
			fireTableDataChanged();
			doUpdate(this, _type);
		}

		public String getColumnName(int col) {
			return "";
		}

		public int getColumnCount() {
			return 1;
		}

		public int getRowCount() {
			return _data.size();
		}

		public Object getValueAt(int row, int col) {
			return _data.get(row);
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			_data.set(rowIndex, aValue);
			doUpdate(this, _type);
			_apply.setEnabled(false);
			_del.setEnabled(_data.size() > 0);
		}

	}

	public static class AttsTableModel extends AbstractTableModel {

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
			/*
			 * if(columnIndex==0){ return false; } Object []
			 * info=(Object[])_fields.getValue(rowIndex); Boolean
			 * isEditable=(Boolean)info[2]; return isEditable.booleanValue();
			 */
			return false;
		}

		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			Object rowData[] = (Object[]) _data.get(rowIndex);
			rowData[columnIndex] = aValue;
			_dirty[rowIndex] = true;
		}

		public boolean isDirty(int rowIndex) {
			return _dirty[rowIndex];
		}

		public String getFieldAt(int rowIndex) {
			Object[] info = (Object[]) _fields.get(rowIndex);

			return (String) info[0];
		}

		private java.util.List _data;
		private java.util.List _fields;
		private boolean[] _dirty;

		private Object _cols[] = { "Entry", "Value" };

		public AttsTableModel(java.util.List data, java.util.List fields) {
			_data = data;
			_fields = fields;
			_dirty = new boolean[_fields.size()];
		}
	}

	static class AttribsPanel extends JPanel {

		AttsTableModel _model;
		ArrayList _uiDescriptors;
		Entry atts[];

		AttribsPanel(Entry attributes[], boolean isServiceUI) {
			// setSize(300, 400);
			atts = attributes;

			_uiDescriptors = new ArrayList();
			java.util.List data = new ArrayList();
			java.util.List fields = new ArrayList();
			for (int j = 0; j < atts.length; j++) {
				boolean include = false;
				if (atts[j] instanceof UIDescriptor)
					include = isServiceUI;
				else
					include = !isServiceUI;
				/*
				 * if(atts[j] instanceof Multitype) { Multitype st =
				 * (Multitype)atts[j]; java.awt.Image image = st.getIcon(1);
				 * if(image != null){ // _serviceIcon = new ImageIcon(image);
				 * //System.out.println("Setting service icon"); } }
				 */
				if (include) {
					Class eClass = atts[j].getClass();
					String className = eClass.getName();
					Field f[] = eClass.getFields();
					for (int k = 0; k < f.length; k++) {
						String fName = f[k].getName();
						String displayName = className.substring(className
								.lastIndexOf(".") + 1, className.length());
						displayName = displayName + "." + fName;
						try {
							Object value = f[k].get(atts[j]);
							if (value != null) {
								data.add(((Object) (new Object[] {
												displayName,
												(value.getClass().isArray() ? Util
														.arrayToString(value)
														: value) })));
								// now store the actual class,field,isEditable
								boolean isEditable = !ServiceControlled.class
										.isAssignableFrom(eClass);
								fields.add(new Object[] { className, fName,
										new Boolean(isEditable) });
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

				}
				_model = new AttsTableModel(data, fields);
				JTable table = new JTable(_model);
				JScrollPane sp = new JScrollPane(table);
				setLayout(new BorderLayout());
				add(sp, BorderLayout.CENTER);
				if (isServiceUI) {
					JPanel ctrls = new JPanel();
					JButton run = new JButton("Run service UI");
					ctrls.add(run);
					add(ctrls, "South");
				} else {
					// NOT COMPLETE FOR EE 1.0
					/*
					 * //add an Apply button JButton apply=new JButton("Apply");
					 * apply.addActionListener( new ActionListener(){ public
					 * void actionPerformed(ActionEvent evt){ int
					 * nRows=_model.getRowCount(); for(int i=0;i<nRows;i++){
					 * if(_model.isDirty(i)){
					 * 
					 * } } } }); JPanel ctrls=new JPanel(); ctrls.add(apply);
					 * add(ctrls, BorderLayout.SOUTH);
					 */
				}

			}
		}

	}
	/*
	 * private class LifeCycleTableModel extends AbstractTableModel {
	 * 
	 * public String getColumnName(int col) { return _cols[col].toString(); }
	 * 
	 * public int getColumnCount() { return _cols.length; }
	 * 
	 * public int getRowCount() { return _data.size(); }
	 * 
	 * public Object getValueAt(int listing, int col) { if(listing >= _data.size())
	 * return "listing out of bounds"; Object rowData[] = (Object[])_data.getValue(listing);
	 * if(col >= rowData.length) return ""; else return rowData[col]; }
	 * 
	 * public boolean isCellEditable(int rowIndex, int columnIndex) {
	 * 
	 * return rowIndex>1; }
	 * 
	 * 
	 * private java.util.List _data;
	 * 
	 * 
	 * private Object _cols[] = { "Property", "Value" };
	 * 
	 * public LifeCycleTableModel(java.util.List data) { _data = data;
	 * 
	 * 
	 * } }
	 */
}
