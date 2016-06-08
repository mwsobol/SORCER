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

package sorcer.util;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.DatabaseStorer;
import sorcer.core.provider.Provider;
import sorcer.core.provider.StorageManagement;
import sorcer.service.Accessor;
import sorcer.service.Context;
import sorcer.service.EvaluationException;
import sorcer.service.Signature;
import sorcer.util.url.sos.SdbUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Collections;

/**
 * This is a Java class that is an implementation of table of objects that uses a 
 * <code>List</code> of <code>Lists</code> to store the cell data objects. It 
 * implements the interface <code>Serializable</code>.
 * 
 * @author Mike Sobolewski
 * @see List
 * @see Serializable
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Table implements ModelTable {
	/** Serial version user identification number */
	static final long serialVersionUID = -1968282524723965792L;
	/** Logger */
	protected static Logger logger = LoggerFactory.getLogger(Table.class.getName());
	/** Encoding for the table */
	protected static String ENCODING = "UTF-8";
	/**
	 * The <code>List</code> of <code>Lists</code> of <code>Object</code>
	 * values.
	 */
	/* Unique identifier */
	protected Uuid id;

	protected String name = "Data Table";
	/*  Enumeration type for length units */

	private String fiColumnName = "fis";

	public enum LengthUnits {
		FEET, INCH, METER
	}

	/* Enumeration type for table cell */
	public enum Cell {
		STRING, FLOAT, DOUBLE, INTEGER, LONG, SERIALIZED, MARSHALED
	}

	/* List of List that defines a table */
	protected List<List<?>> dataList;

	// Input table properties
	/* Input table file name */
	protected String inputFileName;
	/* Input table URL */
	protected URL inputTableURL;
	/* Input table delimiter */
	protected String inputTableDelimiter;

	// Output table properties
	/* Output table file name */
	protected String outputFileName;
	/* Output table URL */
	protected URL outputTableURL;
	/* Output table delimiter */
	protected String outputTableDelimiter = " ";
	/* output table storage signature 
	 * a signature in the format 'sos://self' indicates
	 * self storage, the storage by the parametric model itself.
	 */
	protected Signature outputStorageSignature;
	/* A service provider of the parametric model bean*/
	transient protected Provider provider;
	
	/** 
	 * Flag indicating whether to include the header in a write operation
	 * default value is true 
	 */
	protected boolean includeHeaderInWrite = true;
	///** List of strings containing response names the default value is null */
	//private List<String> responseNames = null;

	/** The <code>List</code> of column identifiers. */
	protected List<String> columnIdentifiers;

	/** The <code>List</code> of row identifiers. */
	protected List<Object> rowIdentifiers;

	protected boolean lazy = false;

	//protected Cell cellType = Cell.DOUBLE;
	protected Cell cellType = Cell.STRING;
	
	protected static int count = 0;

	/**
	 * Constructs a <code>Table</code> which is a table of zero columns and zero
	 * rows.
	 */
	public Table() {
		this(0, 0);
		name = "undefined" + count++;
	}

	protected static List<?> newList(int size) {
		List<?> l = new ArrayList<Object>(size);
		return Collections.synchronizedList(l);
	}

	private static List<?> newList() {
		List<?> l = new ArrayList<Object>();
		return Collections.synchronizedList(l);
	}

	/**
	 * Constructs a <code>Table</code> with <code>rowCount</code> and
	 * <code>columnCount</code> of <code>null</code> object values.
	 * 
	 * @param rowCount
	 *            the number of rows the table holds
	 * @param columnCount
	 *            the number of columns the table holds
	 * 
	 * @see #setValueAt
	 */
	public Table(int rowCount, int columnCount) {
		this(newList(columnCount), rowCount);
	}

	/**
	 * Constructs a <code>Table</code> with as many columns as there are
	 * elements in <code>columnNames</code> and <code>rowCount</code> of
	 * <code>null</code> object values. Each column's name will be taken from
	 * the <code>columnNames</code> list.
	 * 
	 * @param columnNames
	 *            <code>list</code> containing the names of the new columns; if
	 *            this is <code>null</code> then the model has no columns
	 * @param rowCount
	 *            the number of rows the table holds
	 * @see #setDataList
	 * @see #setValueAt
	 */
	public Table(List<?> columnNames, int rowCount) {
		id = UuidFactory.generate();
		setDataList(newList(rowCount), columnNames);
	}

	/**
	 * Constructs a <code>Table</code> with as many columns as there are
	 * elements in <code>columnNames</code> and <code>rowCount</code> of
	 * <code>null</code> object values. Each column's name will be taken from
	 * the <code>columnNames</code> array.
	 * 
	 * @param columnNames
	 *            <code>array</code> containing the names of the new columns; if
	 *            this is <code>null</code> then the model has no columns
	 * @param rowCount
	 *            the number of rows the table holds
	 * @see #setDataList
	 * @see #setValueAt
	 */
	public Table(Object[] columnNames, int rowCount) {
		this(convertToList(columnNames), rowCount);
	}

	/**
	 * Constructs a <code>Table</code> and initializes the table by passing
	 * <code>data</code> and <code>columnNames</code> to the
	 * <code>setDataList</code> method.
	 * 
	 * @param data
	 *            the data of the table, a <code>List</code> of
	 *            <code>List</code>s of <code>Object</code> values
	 * @param columnNames
	 *            <code>list</code> containing the names of the new columns
	 * @see #getDataList
	 * @see #setDataList
	 */
	public Table(List<List<?>> data, List<?> columnNames) {
		setDataList(data, columnNames);
	}

	/**
	 * Constructs a <code>DefaultTableModel</code> and initializes the table by
	 * passing <code>data</code> and <code>columnNames</code> to the
	 * <code>setDataList</code> method. The first index in the
	 * <code>Object[][]</code> array is the row index and the second is the
	 * column index.
	 * 
	 * @param data
	 *            the data of the table
	 * @param columnNames
	 *            the names of the columns
	 * @see #getDataList
	 * @see #setDataList
	 */
	public Table(Object[][] data, Object[] columnNames) {
		id = UuidFactory.generate();
		setDataList(data, columnNames);
	}

	/**
	 * Returns the <code>List</code> of <code>Lists</code> that contains the
	 * table's data values. The lists contained in the outer list are each a
	 * single row of values. In other words, to get to the cell at row 1, column
	 * 5:
	 * <p>
	 * 
	 * <code>((List)getDataList().get(1)).get(5);</code>
	 * <p>
	 * 
	 * @return the List of list containing the tables data values
	 *
	 * @see #setDataList
	 */
	public List getDataList() {
		return dataList;
	}

	public List getRow(int rowIndex) {
		return dataList.get(rowIndex);
	}

	public List getRow(String rowName) {
		return dataList.get(rowIndexOf(rowName));
	}

	public List getColumn(int colIndex){
		if (colIndex < 0)
			return null;

		int rowCount = getRowCount();
		List colList = new ArrayList();
		for (int i = 0; i < rowCount; i++){
			List<?> rowi = getRow(i);
			// fill in with null missing elements
			if (colIndex >= rowi.size())
				colList.add(null);
			else
				colList.add(rowi.get(colIndex));
		}
		return colList;
	}

	public List getColumn(String columnName){
		int column = columnIndexOf(columnName);
		return getColumn(column);
	}
	
	public List getColumnWithIdThatContainsString(String string) {
		for (String colId:columnIdentifiers) {
			if (colId.contains(string)) 
				return getColumn(colId);
		}
		return null;
	}

	public int getColumnIndexWithIdThatContainsString(String string) {
		int colIdx = 0;
		if (getColumnName(colIdx).contains(string)) return colIdx;
		colIdx++;
		return 0;
	}
	
	protected static List nonNullList(List l) {
		return (l != null) ? l : newList();
	}

	public void setDataList(List dataList) {
		this.dataList = nonNullList(dataList);
	}

	/**
	 * Replaces the current <code>dataList</code> instance variable with the new
	 * <code>List</code> of rows, <code>dataList</code>. Each row is represented
	 * in <code>dataList</code> as a <code>List</code> of <code>Object</code>
	 * values. <code>columnIdentifiers</code> are the names of the new columns.
	 * The first name in <code>columnIdentifiers</code> is mapped to column 0 in
	 * <code>dataList</code>. Each row in <code>dataList</code> is adjusted to
	 * match the number of columns in <code>columnIdentifiers</code> either by
	 * truncating the <code>List</code> if it is too long, or adding
	 * <code>null</code> values if it is too short.
	 * <p>
	 * Note that passing in a <code>null</code> value for <code>dataList</code>
	 * results in unspecified behavior, possibly an exception.
	 * 
	 * @param dataList
	 *            the new data List
	 * @param columnIdentifiers
	 *            the names of the columns
	 * @see #getDataList
	 */
	public void setDataList(List dataList, List columnIdentifiers) {
		this.dataList = nonNullList(dataList);
		this.columnIdentifiers = nonNullList(columnIdentifiers);
	}


	/**
	 * Replaces the value in the <code>dataList</code> instance variable with
	 * the values in the array <code>dataList</code>. The first index in the
	 * <code>Object[][]</code> array is the row index and the second is the
	 * column index. <code>columnIdentifiers</code> are the names of the new
	 * columns.
	 * 
	 * @param dataList
	 *            the new data list
	 * @param columnIdentifiers
	 *            the names of the columns
	 * @see #setDataList(List, List)
	 */
	public void setDataList(Object[][] dataList, Object[] columnIdentifiers) {
		setDataList(convertToList(dataList), convertToList(columnIdentifiers));
	}

	/**
	 * Adds a row to the end of the model. The new row will contain
	 * <code>null</code> values unless <code>rowData</code> is specified.
	 * Notification of the row being added will be generated.
	 * 
	 * @param rowData
	 *            optional data of the row being added
	 */
	public void addRow(List rowData) {
		insertRow(getRowCount(), rowData);
	}

	/**
	 * Adds a row to the end of the model. The new row will contain
	 * <code>null</code> values unless <code>rowData</code> is specified.
	 * Notification of the row being added will be generated.
	 * 
	 * @param rowData
	 *            optional data of the row being added
	 */
	public void addRow(Object[] rowData) {
		addRow(convertToList(rowData));
	}

	/**
	 * Inserts a row at <code>row</code> in the model. The new row will contain
	 * <code>null</code> values unless <code>rowData</code> is specified.
	 * Notification of the row being added will be generated.
	 * 
	 * @param row
	 *            the row index of the row to be inserted
	 * @param rowData
	 *            optional data of the row being added
	 * @exception ArrayIndexOutOfBoundsException
	 *                if the row was invalid
	 */
	public void insertRow(int row, List rowData) {
		dataList.add(row, rowData);
	}

	/**
	 * Inserts a row at <code>row</code> in the model. The new row will contain
	 * <code>null</code> values unless <code>rowData</code> is specified.
	 * Notification of the row being added will be generated.
	 * 
	 * @param row
	 *            the row index of the row to be inserted
	 * @param rowData
	 *            optional data of the row being added
	 * @exception ArrayIndexOutOfBoundsException
	 *                if the row was invalid
	 */
	public void insertRow(int row, Object[] rowData) {
		insertRow(row, convertToList(rowData));
	}

	private static int gcd(int i, int j) {
		return (j == 0) ? i : gcd(j, i % j);
	}

	private static void rotate(List l, int a, int b, int shift) {
		int size = b - a;
		int r = size - shift;
		int g = gcd(size, r);
		for (int i = 0; i < g; i++) {
			int to = i;
			Object tmp = l.get(a + to);
			for (int from = (to + r) % size; from != i; from = (to + r) % size) {
				l.set(a + to, l.get(a + from));
				to = from;
			}
			l.set(a + to, tmp);
		}
	}

	/**
	 * Moves one or more rows from the inclusive range <code>start</code> to
	 * <code>end</code> to the <code>to</code> position in the model. After the
	 * move, the row that was at index <code>start</code> will be at index
	 * <code>to</code>. This method will send a <code>tableChanged</code>
	 * notification message to all the listeners.
	 * <p>
	 * 
	 * <pre>
	 *  Examples of moves:
	 *  <p>
	 *  1. moveRow(1,3,5);
	 *          a|B|C|D|e|f|g|h|i|j|k   - before
	 *          a|e|f|g|h|B|C|D|i|j|k   - after
	 *  <p>
	 *  2. moveRow(6,7,1);
	 *          a|b|c|d|e|f|G|H|i|j|k   - before
	 *          a|G|H|b|c|d|e|f|i|j|k   - after
	 *  <p>
	 * </pre>
	 * 
	 * @param start
	 *            the starting row index to be moved
	 * @param end
	 *            the ending row index to be moved
	 * @param to
	 *            the destination of the rows to be moved
	 * @exception ArrayIndexOutOfBoundsException
	 *                if any of the elements would be moved out of the table's
	 *                range
	 * 
	 */
	public void moveRow(int start, int end, int to) {
		int shift = to - start;
		int first, last;
		if (shift < 0) {
			first = to;
			last = end;
		} else {
			first = start;
			last = to + end - start;
		}
		rotate(dataList, first, last + 1, shift);
	}

	/**
	 * Removes the row at <code>row</code> from the model. Notification of the
	 * row being removed will be sent to all the listeners.
	 * 
	 * @param row
	 *            the row index of the row to be removed
	 * @exception ArrayIndexOutOfBoundsException
	 *                if the row was invalid
	 */
	public void removeRow(int row) {
		dataList.remove(row);
		if (rowIdentifiers != null) rowIdentifiers.remove(row);
	}

	//
	// Manipulating columns
	// 

	/**
	 * Replaces the column identifiers in the model. If the number of
	 * <code>newIdentifier</code>s is greater than the current number of
	 * columns, new columns are added to the end of each row in the model. If
	 * the number of <code>newIdentifier</code>s is less than the current number
	 * of columns, all the extra columns at the end of a row are discarded.
	 * <p>
	 *
	 * @param columnIdentifiers
	 *            list of column identifiers. If <code>null</code>, set the
	 *            model to zero columns
	 */
	public void setColumnIdentifiers(List columnIdentifiers) {
		this.columnIdentifiers = nonNullList(columnIdentifiers);
	}

	public void setRowIdentifiers(List rowIdentifiers) {
		this.rowIdentifiers = rowIdentifiers;
	}

	/**
	 * Replaces the column identifiers in the model. If the number of
	 * <code>newIdentifier</code>s is greater than the current number of
	 * columns, new columns are added to the end of each row in the model. If
	 * the number of <code>newIdentifier</code>s is less than the current number
	 * of columns, all the extra columns at the end of a row are discarded.
	 * <p>
	 *
	 * @param newIdentifiers
	 *            array of column identifiers. If <code>null</code>, set the
	 *            model to zero columns
	 */
	public void setColumnIdentifiers(Object[] newIdentifiers) {
		setColumnIdentifiers(convertToList(newIdentifiers));
	}

	/**
	 * Sets the number of columns in the model. If the new size is greater than
	 * the current size, new columns are added to the end of the model with
	 * <code>null</code> cell values. If the new size is less than the current
	 * size, all columns at index <code>columnCount</code> and greater are
	 * discarded.
	 *
	 * @param columnCount
	 *            the new number of columns in the model
	 */
	public void setColumnCount(int columnCount) {
		for (int i = 0; i < getRowCount(); i++) {
			if (dataList.get(i) == null) {
				dataList.set(i, newList());
			}
		}
	}

	/**
	 * Adds a column to the model. The new column will have the identifier
	 * <code>columnName</code>, which may be null. This method will send a
	 * <code>tableChanged</code> notification message to all the listeners. This
	 * method is a cover for <code>addColumn(Object, List)</code> which uses
	 * <code>null</code> as the data list.
	 * 
	 * @param columnName
	 *            the identifier of the column being added
	 */
	public void addColumn(String columnName) {
		addColumn(columnName, (List) null);
	}

	/**
	 * Adds a column to the model at the specifed colID. The new column will have the identifier
	 * <code>columnName</code>, which may be null. <code>columnData</code> is
	 * the optional list of data for the column. If it is <code>null</code> the
	 * column is filled with <code>null</code> values. Otherwise, the new data
	 * will be added to model starting with the first element going to row 0,
	 * etc. This method will send a <code>tableChanged</code> notification
	 * message to all the listeners.
	 * 
	 * @param columnName
	 *            the identifier of the column being added
	 * @param columnData
	 *            optional data of the column being added
	 */
	public void addColumn(String columnName, List columnData, int colID) {
		columnIdentifiers.add(colID, columnName);
		if (columnData != null) {
			int columnSize = columnData.size();
			for (int i = 0; i < columnSize; i++) {
				List row = (List) dataList.get(i);
				row.add(colID, columnData.get(i));
			}
		}

	}

	/**
	 * Remove a column to the model at the specified colID. All row data is shifted to the left.
	 * 
	 * @param colID
	 *            the index of the column being deleted
	 * @throws EvaluationException 
	 */
	public void removeColumn(int colID) throws EvaluationException {
		int rowSize = getRowCount();
	
			for (int i = 0; i < rowSize; i++) {
				List row = (List) dataList.get(i);
				row.remove(colID);
			}
			columnIdentifiers.remove(colID);
	}
	
	/**
	 * Adds a column to the model. The new column will have the identifier
	 * <code>columnName</code>, which may be null. <code>columnData</code> is
	 * the optional list of data for the column. If it is <code>null</code> the
	 * column is filled with <code>null</code> values. Otherwise, the new data
	 * will be added to model starting with the first element going to row 0,
	 * etc. This method will send a <code>tableChanged</code> notification
	 * message to all the listeners.
	 * 
	 * @param columnName
	 *            the identifier of the column being added
	 * @param columnData
	 *            optional data of the column being added
	 */
	public void addColumn(String columnName, List columnData) {
		columnIdentifiers.add(columnName);
		if (columnData != null) {
			int columnSize = columnData.size();
			int newColumn = getColumnCount() - 1;
			for (int i = 0; i < columnSize; i++) {
				if (dataList == null) {
					dataList = Collections.synchronizedList(new ArrayList<List<?>>());		
				}
				if (dataList.size() <= i) {
					dataList.add(new ArrayList());
				}
				List row = (List) dataList.get(i);
				row.add(newColumn, columnData.get(i));
			}
		}
	}

	/**
	 * Adds a column to the model. The new column will have the identifier
	 * <code>columnName</code>. <code>columnData</code> is the optional array of
	 * data for the column. If it is <code>null</code> the column is filled with
	 * <code>null</code> values. Otherwise, the new data will be added to model
	 * starting with the first element going to row 0, etc. This method will
	 * send a <code>tableChanged</code> notification message to all the
	 * listeners.
	 *
	 * @see #addColumn(String, List)
	 */
	public void addColumn(String columnName, Object[] columnData) {
		addColumn(columnName, convertToList(columnData));
	}

	/**
	 * Returns the number of rows in this data table.
	 * 
	 * @return the number of rows in the model
	 */
	public int getRowCount() {
		if (dataList == null)
			return 0;
		else
			return dataList.size();
	}

	/**
	 * Returns the number of columns in this data table.
	 * 
	 * @return the number of columns in the model
	 */
	public int getColumnCount() {
		return columnIdentifiers.size();
	}

	/**
	 * Returns the column name.
	 * 
	 * @return a name for this column using the string value of the appropriate
	 *         member in <code>columnIdentifiers</code>. If
	 *         <code>columnIdentifiers</code> does not have an entry for this
	 *         index, returns the default name provided by the superclass.
	 */
	public String getColumnName(int column) {
		Object id = columnIdentifiers.get(column);
		return id.toString();
	}

	public List<String> getColumnNames() {
		return columnIdentifiers;
	}

	public List<String> getRowNames() {
		List<String> names = new ArrayList<String>();
		for (Object id : rowIdentifiers)
			names.add(""+id);
		return names;
	}

	/**
	 * Returns an attribute value for the cell at <code>row</code> and
	 * <code>column</code>.
	 * 
	 * @param row
	 *            the row whose value is to be queried
	 * @param column
	 *            the column whose value is to be queried
	 * @return the value Object at the specified cell
	 * @exception ArrayIndexOutOfBoundsException
	 *                if an invalid row or column was given
	 */
	public Object getValueAt(int row, int column) {
		List rowList = (List) dataList.get(row);
		return rowList.get(column);
	}

	public Object getValueAt(int row, String colName) {
		int column = columnIndexOf(colName);
		List rowList = (List) dataList.get(row);
		return rowList.get(column);
	}


	public Double getValueAtAsDouble(int row, int column) {
		Object val = getValueAt(row, column);
		if (val instanceof Double)
			return (Double) val;
		else
			return new Double(val.toString());
	}
	public Double[] getMinMaxValuesAsDouble( String columnName) {
		return getMinMaxValuesAsDouble(columnIndexOf(columnName));
	}
	public Double[] getMinMaxValuesAsDouble(int colIndex){
		Double[] minmax = new Double[2];
		minmax[0]= getValueAtAsDouble(0,colIndex);
		minmax[1]=minmax[0];
		for (int i = 1; i<getRowCount();i++){
			Double cval = getValueAtAsDouble(i,colIndex);
			if (cval > minmax[1])minmax[1]=cval;
			if (cval < minmax[0])minmax[0]=cval;
		}
		return minmax;
	}
	public Double getValueAtAsDouble(int row, String columnName) {
		return getValueAtAsDouble(row, columnIndexOf(columnName));
	}

	public Table getValuesSuchThat(String colName1, String col1Value, 
			String colName2, String col2Value){
		Table qTable = new Table();
		int col1Idx = columnIndexOf(colName1);
		int col2Idx = columnIndexOf(colName2);

		if (columnIdentifiers != null)qTable.setColumnIdentifiers(columnIdentifiers);
		if (rowIdentifiers != null) qTable.setRowIdentifiers(rowIdentifiers);
		for (int i=0; i<getRowCount(); i++) {
			List<?>cRow = getRow(i);
			if (cRow.get(col1Idx).toString().equals(col1Value) &&
					cRow.get(col2Idx).toString().equals(col2Value)){
				qTable.addRow(cRow);
			}
		}	
		return qTable;
	}

	public boolean hasValueSuchThat(String colName1, String col1Value, 
			String colName2, String col2Value){
		int col1Idx = columnIndexOf(colName1);
		int col2Idx = columnIndexOf(colName2);

		for (int i=0; i<getRowCount(); i++) {
			List<?>cRow = getRow(i);
//			logger.info("cRow.get(col1Idx).toString() >"+cRow.get(col1Idx).toString()+"< val1 >"+col1Value+"< "+cRow.get(col1Idx).toString().equals(col1Value));
//			logger.info("cRow.get(col2Idx).toString() >"+cRow.get(col2Idx).toString()+"< val2 >"+col2Value+"< "+cRow.get(col2Idx).toString().equals(col2Value));
			if (cRow.get(col1Idx).toString().equals(col1Value) &&
					cRow.get(col2Idx).toString().equals(col2Value)){
//				logger.info("found match "+col1Value+" "+col2Value);
				return true;
			}
		}	
		return false;
	}
	
	public Table getValuesSuchThat(String colName1, String col1Value){
		Table qTable = new Table();
		int col1Idx = columnIndexOf(colName1);
		if (col1Idx < 0) return qTable;
		if (columnIdentifiers != null)qTable.setColumnIdentifiers(columnIdentifiers);
		if (rowIdentifiers != null) qTable.setRowIdentifiers(rowIdentifiers);
		for (int i=0; i<getRowCount(); i++) {
			List<?>cRow = getRow(i);
			if (cRow.get(col1Idx).equals(col1Value) ){
				qTable.addRow(cRow);
			}
		}	
		return qTable;
	}

	public Table getValuesSuchThat(String colName1, Integer col1Value){
		Table qTable = new Table();
		int col1Idx = columnIndexOf(colName1);

		if (columnIdentifiers != null)qTable.setColumnIdentifiers(columnIdentifiers);
		if (rowIdentifiers != null) qTable.setRowIdentifiers(rowIdentifiers);
		//		logger.info("col1Idx = "+col1Idx+" for colName = "+colName1);
		//		logger.info("type of colIdx = "+getRow(0).get(col1Idx).getClass().getName());
		for (int i=0; i<getRowCount(); i++) {
			List<?>cRow = getRow(i);
			//			logger.info("cRow Value = "+cRow.get(col1Idx)+" inValue = >"+col1Value+"< is Mathch "+cRow.get(col1Idx).equals(col1Value) );
			if (cRow.get(col1Idx).equals(col1Value) ){
				qTable.addRow(cRow);
			}
		}	
		return qTable;
	}

	public int columnIndexOf(String columnName) {
		return columnIdentifiers.indexOf(columnName);
	}

	public int rowIndexOf(String variableName) {
		return rowIdentifiers.indexOf(variableName);
	}

	public Object getValue(String rowName, String columnName) {
		int row = rowIndexOf(rowName);
		int column = columnIndexOf(columnName);
		List rowList = (List) dataList.get(row);
		return rowList.get(column);
	}

//	public Object getValue(Object rowName, String columnName) {
//		int row = rowIndexOf(rowName);
//		int column = columnIndexOf(columnName);
//		List rowList = (List) dataList.get(row);
//		return rowList.get(column);
//	}

	public int rowIndexOf(Object variableName) {
		return rowIdentifiers.indexOf(variableName);
	}

	
	public Object getValue(int row, String columnName) {
		int column = columnIndexOf(columnName);
		List rowList = (List) dataList.get(row);
		return rowList.get(column);
	}

	/**
	 * Sets the object value for the cell at <code>column</code> and
	 * <code>row</code>. <code>aValue</code> is the new value.
	 * 
	 * @param aValue
	 *            the new value; this can be null
	 * @param row
	 *            the row whose value is to be changed
	 * @param column
	 *            the column whose value is to be changed
	 * @exception ArrayIndexOutOfBoundsException
	 *                if an invalid row or column was given
	 */
	public void setValueAt(Object aValue, int row, int column) {
		List rowList = (List) dataList.get(row);
		rowList.set(column, aValue);
	}

	/**
	 * Returns a list that contains the same objects as the array.
	 * 
	 * @param anArray
	 *            the array to be converted
	 * @return the new list; if <code>anArray</code> is <code>null</code>,
	 *         returns <code>null</code>
	 */
	protected static List convertToList(Object[] anArray) {
		if (anArray == null) {
			return null;
		}
		List l = new ArrayList(anArray.length);
		for (int i = 0; i < anArray.length; i++) {
			l.add(anArray[i]);
		}
		return l;
	}

	/**
	 * Returns a list of lists that contains the same objects as the array.
	 * 
	 * @param anArray
	 *            the double array to be converted
	 * @return the new list of list; if <code>anArray</code> is
	 *         <code>null</code>, returns <code>null</code>
	 */
	protected static List convertToList(Object[][] anArray) {
		if (anArray == null) {
			return null;
		}
		List l = new ArrayList(anArray.length);
		for (int i = 0; i < anArray.length; i++) {
			l.add(convertToList(anArray[i]));
		}
		return l;
	}

	public void writeToFile() throws EvaluationException {
		if (outputFileName != null) {
			writeToFile(new File(outputFileName));
		} else {
			throw new EvaluationException(
					"No output file specified for the parametric table: "
							+ name);
		}
	}

	public void write() throws EvaluationException {
		if (outputFileName != null) {
			writeToFile();
		}
		if (outputTableURL != null) {
			writeToURL();
		}
		return;
	}

	public void setIncludeHeaderInWrite(boolean includeHeaderInWrite) {
		this.includeHeaderInWrite = includeHeaderInWrite;
	}

	public void writeToFile(File file) throws EvaluationException {
		//		logger.info("writeToFile(): file = " + file);
		//		logger.info("writeToFile(): outDelimiter = " + outDelimiter);
		PrintWriter pw;
		try {
			pw = new PrintWriter(new FileWriter(file));

			if (includeHeaderInWrite) {
				List<String> names = getColumnNames();
				for (int i = 0; i < names.size() - 1; i++) {
					pw.print(names.get(i));
					pw.print(outputTableDelimiter);
					//logger.info("writeToFile(): names.get(i) = " + names.get(i));

				}
				pw.print(names.get(names.size() - 1));
				pw.println();
			}

			for (List row : dataList) {
				for (int i = 0; i < row.size() - 1; i++) {
					pw.print(row.get(i));
					pw.print(outputTableDelimiter);
				}
				pw.print(row.get(row.size() - 1));
				pw.println();
			}
			pw.flush();
			pw.close();
		} catch (IOException e) {
			throw new EvaluationException("No directory for writing a table: " + file, e);
		}
	}

	public URL writeToURL() throws EvaluationException {
		if (outputTableURL != null) {
			return writeToURL(outputTableURL);
		} else {
			throw new EvaluationException(
					"No output URL specified for the parametric table: " + name);
		}
	}

	public URL writeToURL(URL url) throws EvaluationException {
		if (url.getProtocol().equals("sos") || outputStorageSignature != null) {
			return writeToSdbURL(url);
		} else 
			return writeToHttpURL(url);
	}
	
	public URL writeToHttpURL(URL url) throws EvaluationException {
		HttpURLConnection con = null;;
		PrintWriter pw = null;
		BufferedReader is = null;
		logger.info("wrtite table to: " + url);
		try {
			con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setRequestMethod("PUT");
			con.setRequestProperty("Content-Type", "text/plain");

			pw = new PrintWriter(con.getOutputStream());
			// write the parametric table
			List<String> names = getColumnNames();
			for (int i = 0; i < names.size() - 1; i++) {
				pw.print(names.get(i));
				pw.print(outputTableDelimiter);
			}
			pw.print(names.get(names.size() - 1));
			pw.println();
			for (List row : dataList) {
				for (int i = 0; i < row.size() - 1; i++) {
					pw.print(row.get(i));
					pw.print(outputTableDelimiter);
				}
				pw.print(row.get(row.size() - 1));
				pw.println();
			}
			pw.flush();
			is = new BufferedReader(new InputStreamReader(con
					.getInputStream()));

			// empty server's input stream
			String line;
			while ((line = is.readLine()) != null) {
				logger.debug("server reply: " + line);
			}

			int rc = con.getResponseCode();
			String msg = con.getResponseMessage();
			if (rc == 200) {
				logger.debug("response message: " + msg + " at: " + url);
			} else if (rc == 201) {
				logger.debug("response message: " + msg + " at: " + url);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new EvaluationException(ex);
		} finally {
			try {
				if (pw != null)
					pw.close();
				if (is != null)
					is.close();
				con.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return url;
	}
	
	public URL writeToSdbURL(URL url) throws EvaluationException {
		try {
			Context cxt = SdbUtil.getStoreContext(this);
			if (outputStorageSignature != null) {
				if (outputStorageSignature.getServiceType() == DatabaseStorer.class) {
					DatabaseStorer objectStore = ((DatabaseStorer) Accessor.get().getService(outputStorageSignature));
					outputTableURL = (URL)objectStore.contextStore(cxt).getValue("object/url");

				} else {
					StorageManagement objectStore = ((StorageManagement) Accessor.get().getService(outputStorageSignature));
					outputTableURL = (URL)objectStore.contextStore(cxt).getValue("object/url");
				}
			} else if (url.getHost().equals(Signature.SELF)) {
				outputTableURL = ((DatabaseStorer) provider).storeObject(this);				
			} else {
				String serviceType = url.getHost();
				String providerName = url.getPath();
				StorageManagement objectStore = ((StorageManagement) Accessor.get().getService(providerName.substring(1),
                                                                                               Class.forName(serviceType)));
				outputTableURL = (URL) objectStore.contextStore(cxt).getValue("object/url");
			}
		} catch (Exception e) {
			//e.printStackTrace();
			throw new EvaluationException(e);
		}
		return outputTableURL;
	}
	
	public Map<String, Object> getRowMap(String rowName) {
		int rowIndex = rowIndexOf(rowName);
		List<?> rowData = dataList.get(rowIndex);
		Map<String, Object> mapRow = new HashMap<String, Object>(getColumnNames().size());
		for (int i = 0; i < getColumnNames().size(); i++) {
			mapRow.put(getColumnNames().get(i), rowData.get(i));
		}
		return mapRow;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName() + ": " + name +"\n");
		List<String> cns = getColumnNames();
		if (cns != null && cns.size() > 0)
			sb.append(cns);
		if (dataList != null && dataList.size() > 0) {
			sb.append("\nfirst 100 rows: " + dataList.size());
			// print up to 100 rows only
			int rc = 100;
			if (dataList.size() < 100) {
				rc = dataList.size();
			}
			for (int i = 0; i < rc; i++) {
				if (rowIdentifiers != null) {
					sb.append("\n").append(rowIdentifiers.get(i)).append("\t");
					sb.append(dataList.get(i));
				} else {
					sb.append("\n").append(dataList.get(i));
				}
			}
		}
		sb.append("\n...\ntotal row count: " + dataList.size());
		sb.append("\n");
		describe(sb);
		return sb.toString();
	}

	/**
	 * Describe the table contents and table input and output sources.
	 * 
	 * @return a table description
	 */
	public String describe() {
		StringBuilder sb = null;
		if (dataList != null && dataList.size() > 0)
			sb = new StringBuilder(toString());
		else
			sb = new StringBuilder();
		return describe(sb);
	}

	/**
	 * Describe the table input and output sources.
	 * 
	 * @return a table in/out sources description
	 */
	public String describe(StringBuilder sb) {
		sb.append("parametric table: " + (inputFileName != null ? inputFileName : "")
				+ (inputTableURL != null ? inputTableURL : "" ));
		sb.append("|").append(inputTableDelimiter != null ? "`" +  inputTableDelimiter + "`": "");
		sb.append("\nresponse table: " + (outputFileName != null ? outputFileName : "" )
				+ (outputTableURL != null ? outputTableURL : ""));
		sb.append("|").append(outputTableDelimiter != null ? "`" + outputTableDelimiter + "`" : "");
		//sb.append("table responses: ").append(responseNames);
		return sb.toString();
	}


	public Iterator iterator() {
		return dataList.iterator();
	}

	/**
	 * <p>
	 * Returns the output file for this table.
	 * </p>
	 * 
	 * @return the inputFile
	 */
	public String getInputFile() {
		return inputFileName;
	}

	/**
	 * <p>
	 * Sets the output file for this table.
	 * </p>
	 * 
	 * @param inputFile
	 *            the inputFile to set
	 */
	public void setInputFile(String inputFile) {
		this.inputFileName = inputFile;
	}

	/**
	 * <p>
	 * Returns the output file for this table.
	 * </p>
	 * 
	 * @return the outputFile
	 */
	public String getOutputFile() {
		return outputFileName;
	}

	/**
	 * <p>
	 * Sets the output file for this table.
	 * </p>
	 * 
	 * @param outputFile
	 *            the outputFile to set
	 */
	public void setOutputFile(String outputFile) {
		this.outputFileName = outputFile;
	}

	/**
	 * <p>
	 * Sets the output URL for this table.
	 * </p>
	 * 
	 * @return the outputURL
	 */
	public URL getOutputURL() {
		return outputTableURL;
	}

	/**
	 * <p>
	 * Sets the output URL for this table.
	 * </p>
	 * 
	 * @param outputURL
	 *            the outputURL to set
	 */
	public void setOutputURL(URL outputURL) {
		this.outputTableURL = outputURL;
	}

	/**
	 * <p>
	 * Sets the input URL for this table.
	 * </p>
	 * 
	 * @return the inputURL
	 */
	public URL getInputURL() {
		return inputTableURL;
	}

	/**
	 * <p>
	 * Sets the input URL for this table.
	 * </p>
	 * 
	 * @param inputURL
	 *            the inputURL to set
	 */
	public void setInputURL(URL inputURL) {
		this.inputTableURL = inputURL;
	}

	public void setOutput(String location) throws MalformedURLException {
		if (location != null) {
			if (location.startsWith("http://") || location.startsWith("sos://"))
				outputTableURL = new URL(location);
			else
				outputFileName = location;
		}
	}

	public void setInput(String location) throws MalformedURLException {
		setInput(location, null);
	}

	public void setInput(String location, String inDelimiter) throws MalformedURLException {
		if (location != null) {
			if (location.startsWith("http://") || location.startsWith("sos://"))
				inputTableURL = new URL(location);
			else
				inputFileName = location;
		}
		if (inDelimiter != null)
			this.inputTableDelimiter = inDelimiter;
	}

	/**
	 * <p>
	 * Returns the input delimiter for this table.
	 * </p>
	 * 
	 * @return the inDelimiter
	 */
	public String getInDelimiter() {
		return inputTableDelimiter;
	}

	/**
	 * <p>
	 * Sets the input delimiter for this table.
	 * </p>
	 * 
	 * @param inDelimiter
	 *            the inDelimiter to set
	 */
	public void setInDelimiter(String inDelimiter) {
		this.inputTableDelimiter = inDelimiter;
	}

	/**
	 * <p>
	 * Returns the output delimiter for this table.
	 * </p>
	 * 
	 * @return the outDelimiter
	 */
	public String getOutDelimiter() {
		return outputTableDelimiter;
	}

	/**
	 * <p>
	 * Sets the output delimiter for this table.
	 * </p>
	 * 
	 * @param outDelimiter
	 *            the outDelimiter to set
	 */
	public void setOutDelimiter(String outDelimiter) {
		this.outputTableDelimiter = outDelimiter;
	}

	/**
	 * <p>
	 * Returns the name of this table.
	 * </p>
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * <p>
	 * Assigns the name for this table.
	 * </p>
	 * 
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	public void setOutput(String out, String outDelimiter)
			throws IOException {
		if (out != null) {
			if (out.startsWith("http://") || out.startsWith("sos://"))
				outputTableURL = new URL(out);
			else
				outputFileName = out;

			this.outputTableDelimiter = outDelimiter;
		}
	}

	public List<String> getColumnIdentifiers(){
		return columnIdentifiers;
	}

	public void setRowIdentifier(String rowIdentifier, int index) {
		if (index >= rowIdentifiers.size()) rowIdentifiers.add(rowIdentifier);
		else rowIdentifiers.add(index, rowIdentifier);
	}

	public List<Object> getRowIdentifiers(){
		return rowIdentifiers;
	}

	public List<?> getValues(String colName) {
		int i = rowIdentifiers.indexOf(colName);
		return dataList.get(i);
	}	

	public Uuid getId() {
		return id;
	}

	public void setId(Uuid id) {
		this.id = id;
	}
	
	/**
	 * @param col1Name
	 */
	public void setRowIdentifiers(String col1Name) {
		rowIdentifiers = new ArrayList<Object>();
		for (int i = 0; i < getRowCount(); i++)
			rowIdentifiers.add(getValue(i, col1Name));
	}

	/**
	 * <p>
	 * A flag for lazy table population with data from a URL or file.
	 * </p>
	 * 
	 * @return the lazy true for lazy table population 
	 */
	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public Cell getCellType() {
		return cellType;
	}

	public void setCellType(Cell cellType) {
		this.cellType = cellType;
	}
	
	public Signature getOutputStorageSignature() {
		return outputStorageSignature;
	}

	public void setOuputStorageSignature(Signature storageSignature) {
		this.outputStorageSignature = storageSignature;
	}

	public boolean isOutTableStored() {
		if (outputTableURL.getProtocol().equals("sos"))
			return true;
		else if (outputStorageSignature != null)
			return true;
		
		return false;
	}

	public String getFiColumnName() {
		return fiColumnName;
	}

	public void setFiColumnName(String fiColumnName) {
		this.fiColumnName = fiColumnName;
	}

	public Table trimFidelities() throws EvaluationException {
		removeColumn(columnIdentifiers.indexOf(fiColumnName));
		columnIdentifiers.remove(fiColumnName);
		return this;
	}

	@Override
	public boolean equals(Object table) {
		if (table instanceof Table) {
			if (dataList.size() != ((Table) table).dataList.size())
				return false;

			for (int i = 0; i < dataList.size(); i++) {
				if (!dataList.get(i).equals(((Table) table).dataList.get(i))) {
					return false;
				}
			}
			return true;
		} else
			return false;
	}
	
	public boolean isEmpty() {
		if (dataList == null)
			return true;
		else {
			for (List l : dataList) {
				if (l.size() != 0)
					return false;
			}
			return true;
		}
	}
	
	public void clearData() {
		if (dataList == null)
			return;
		else {
			dataList.clear();
		}
	}

	public void writeFormattedToFile(int numFieldsPerRecord,
			String fieldFormat, ArrayList<Integer> colIds,
			ArrayList<Integer> arrayIndices, File outFile) throws IOException {
		FileWriter fw = null;
		boolean append = true;
		fw = new FileWriter(outFile, append);
		Formatter outFileFM = new Formatter(fw);

		int nw = 1;
		logger.info("rowCount = "+getRowCount());
		for (int i = 0; i < getRowCount(); i++) {
			List rowi = getRow(i);
			for (int j = 0; j < rowi.size(); j++) {
				if (colIds.indexOf(j) >= 0) {
					// check if entry is an Array. If so unwind it
					if (rowi.get(j) instanceof Double[]) {
						Double[] rowVi = (Double[]) rowi.get(j);
						for (int k = 0; k < rowVi.length; k++) {
							// check if this index in the array is to be written
							if (arrayIndices.indexOf(k) >= 0) {
								outFileFM.format(fieldFormat, rowVi[k]);
								nw++;
								if (nw > numFieldsPerRecord) {
									outFileFM.format("%n");
									nw = 1;
								}
							}
						}
					} else {
						outFileFM.format(fieldFormat, rowi.get(j));
						nw++;
						if (nw > numFieldsPerRecord) {
							outFileFM.format("%n");
							nw = 1;
						}
					}
				}
			}
			
		}
		outFileFM.flush();
		outFileFM.close();
	}
}
