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
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class EventTableView extends JPanel{
	
	private EventTableModel _model=new EventTableModel();
	
	public EventTableView(int max){
		setMaxSize(max);
		setLayout( new BorderLayout());
		//add( new JLabel("Events"),BorderLayout.NORTH);
		JTable table=new JTable(_model);
		table.setDefaultRenderer(Object.class, new EventCellRenderer());
		
		add( new JScrollPane(table),BorderLayout.CENTER);
	}
	public void setMaxSize(int max){
		_model.setMaxSize(max);
	}
	public void update(Object [] newData){
		_model.add(newData);
		
	}
	public void setFilter(String lusName){
		_model.setFilter(lusName);
	}
	private class EventCellRenderer extends DefaultTableCellRenderer{
		public Component getTableCellRendererComponent(
			JTable table, Object value,boolean isSelected, boolean hasFocus,
				int row, int column) {
			
			JLabel lab=(JLabel)super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
			lab.setIcon(null);
			if(column==0){
				String type=_model.getValueAt(row,2).toString();
				
				if(type.startsWith("NO") || type.equals("MATCH_MATCH")){
					lab.setIcon(TreeRenderer._startedIcon);
				}else{
					lab.setIcon(TreeRenderer._stoppedIcon);
				}
			}
			return lab;
		}
	}
		private class EventTableModel extends AbstractTableModel {
			
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
				//we ignore the first column as this is the LUS key
				col++;
				
				if(row >= _data.size())
					return "row out of bounds";
				Object rowData[] = (Object[])_data.get(row);
				if(col >= rowData.length)
					return "";
				else
				return rowData[col];
			}
			
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				
				return false;
			}
			
			private java.util.List _data=new ArrayList();
			private java.util.List _allData=new ArrayList();
			private String lusFilter="";
			private int _maxSize;
			
			private Object _cols[] = {
				"Time", "Service","Type","Service ID"
			};
			void setMaxSize(int max){
				_maxSize=max;
			}
			void setFilter(String lusName){
				lusFilter=lusName;
				_data=new ArrayList();
				int n=_allData.size();
				for(int i=0;i<n;i++){
					Object [] row=(Object[])_allData.get(i);
					if(row[0].equals(lusName)){
						_data.add(row);
					}
				}
			}
			void add(Object [] newData) {
				_allData.add(newData);
				if(_allData.size()>_maxSize){
					_allData.remove(0);
				}
				//now apply any filterings
				if(newData[0].equals(lusFilter)){
					_data.add(newData);
					if(_data.size()>_maxSize){
						_data.remove(0);
					}
				}
				fireTableDataChanged();
				//System.out.println("Table data changed!!!");
				
			}
		}
	}
	
