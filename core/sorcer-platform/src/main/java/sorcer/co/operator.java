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
package sorcer.co;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sorcer.co.tuple.Entry;
import sorcer.co.tuple.FidelityEntry;
import sorcer.co.tuple.InoutEntry;
import sorcer.co.tuple.InputEntry;
import sorcer.co.tuple.OutputEntry;
import sorcer.co.tuple.StrategyEntry;
import sorcer.co.tuple.Tuple1;
import sorcer.co.tuple.Tuple2;
import sorcer.co.tuple.Tuple3;
import sorcer.co.tuple.Tuple4;
import sorcer.co.tuple.Tuple5;
import sorcer.co.tuple.Tuple6;
import sorcer.core.context.ListContext;
import sorcer.core.context.model.par.Par;
import sorcer.service.Arg;
import sorcer.service.ArgSet;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Evaluation;
import sorcer.service.EvaluationException;
import sorcer.service.FidelityInfo;
import sorcer.service.Setter;
import sorcer.service.SetterException;
import sorcer.service.Strategy;
import sorcer.util.Loop;
//import sorcer.vfe.filter.TableReader;
//import sorcer.vfe.util.Response;
import sorcer.util.Table;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class operator {

	private static int count = 0;

	public static <T1> Tuple1<T1> x(T1 x1 ){
		return new Tuple1<T1>( x1 );
	}
	
	public static <T1> Tuple1<T1> tuple(T1 x1 ){
		return new Tuple1<T1>( x1 );
	}
	
	public static <T1,T2> Tuple2<T1,T2> x(T1 x1, T2 x2 ){
		return new Tuple2<T1,T2>( x1, x2 );
	}
	
	public static <T1,T2> Tuple2<T1,T2> tuple(T1 x1, T2 x2 ){
		return new Tuple2<T1,T2>( x1, x2 );
	}
	
	public static <T1,T2,T3> Tuple3<T1,T2,T3> x(T1 x1, T2 x2, T3 x3 ){
		return new Tuple3<T1,T2,T3>( x1, x2, x3 );
	}
	
	public static <T1,T2,T3> Tuple3<T1,T2,T3> tuple(T1 x1, T2 x2, T3 x3 ){
		return new Tuple3<T1,T2,T3>( x1, x2, x3 );
	}
	
	public static <T1,T2,T3,T4> Tuple4<T1,T2,T3,T4> x(T1 x1, T2 x2, T3 x3, T4 x4 ){
		return new Tuple4<T1,T2,T3,T4>( x1, x2, x3, x4 );
	}
	
	public static <T1,T2,T3,T4> Tuple4<T1,T2,T3,T4> tuple(T1 x1, T2 x2, T3 x3, T4 x4 ){
		return new Tuple4<T1,T2,T3,T4>( x1, x2, x3, x4 );
	}
	
	public static <T1,T2,T3,T4,T5> Tuple5<T1,T2,T3,T4,T5> x(T1 x1, T2 x2, T3 x3, T4 x4, T5 x5 ){
		return new Tuple5<T1,T2,T3,T4,T5>( x1, x2, x3, x4, x5 );
	}
	
	public static <T1,T2,T3,T4,T5> Tuple5<T1,T2,T3,T4,T5> tuple(T1 x1, T2 x2, T3 x3, T4 x4, T5 x5 ){
		return new Tuple5<T1,T2,T3,T4,T5>( x1, x2, x3, x4, x5 );
	}
	
	public static <T1,T2,T3,T4,T5,T6> Tuple6<T1,T2,T3,T4,T5,T6> x(T1 x1, T2 x2, T3 x3, T4 x4, T5 x5, T6 x6 ){
		return new Tuple6<T1,T2,T3,T4,T5,T6>( x1, x2, x3, x4, x5, x6 );
	}
	
	public static <T1,T2,T3,T4,T5,T6> Tuple6<T1,T2,T3,T4,T5,T6> tuple(T1 x1, T2 x2, T3 x3, T4 x4, T5 x5, T6 x6 ){
		return new Tuple6<T1,T2,T3,T4,T5,T6>( x1, x2, x3, x4, x5, x6 );
	}
	
	public static String[] from(String... elems) {
		return elems;
	}
	
	public static <T> T[] array(T... elems) {
		return elems;
	}
	
	public static Arg[] args(Arg... elems) {
		return elems;
	}
	
	public static Set<Object> bag(Object... elems) {
		return new HashSet<Object>(list(elems));
	}

	public static <T> Set<T> set(T... elems) {
		return new HashSet<T>(list(elems));
	}
	
	public static <T> List<T> list(T... elems) {
		List<T> out = new ArrayList<T>(elems.length);
		for (T each : elems) {
			out.add(each);
		}
		return out;
	}

	public static List<Object> row(Object... elems) {
		return Arrays.asList(elems);
	}
	
	public static List<Object> values(Object... elems) {
		List<Object> list = new ArrayList<Object>();
		for(Object o: elems) {
			list.add(o);
		}
		return list;
	}

	public static List<String> header(String... elems) {
		List<String> out = new Header<String>(elems.length);
		for (String each : elems) {
			out.add(each);
		}
		return out;
	}

	public static List<String> names(String... elems) {
		List<String> out = new ArrayList<String>(elems.length);
		for (String each : elems) {
			out.add(each);
		}
		return out;
	}

	public static List<String> names(List<String>... nameLists) {
		List<String> out = new ArrayList<String>();
		for (List<String> each : nameLists) {
			out.addAll(each);
		}
		return out;
	}
	
	public static <T1, T2> Tuple2<T1, T2> duo(T1 x1, T2 x2) {
		return new Tuple2<T1, T2>(x1, x2);
	}

	public static <T1, T2, T3> Tuple3<T1, T2, T3> triplet(T1 x1, T2 x2, T3 x3) {
		return new Tuple3<T1, T2, T3>(x1, x2, x3);
	}

	public static <T2> Entry<T2> entry(String x1, T2 x2) {
		return new Entry<T2>(x1, x2);
	}
	
	public static <T2> Entry<T2> entry(String x1) {
		return new Entry<T2>(x1, null);
	}
	
	public static <T2> Entry<T2> put(Entry<T2> entry, T2 value)
			throws SetterException, RemoteException {
		entry.setValue(value);
		return entry;
	}

	public static <T> OutputEntry<T> outEntry(String path, T value) {
		return new OutputEntry(path, value, 0);
	}

	public static <T> OutputEntry<T> outEntry(String path, T value, String annotation) {
		OutputEntry oe =  outEntry(path, value);
		oe.annotation(annotation);
		return oe;
	}

	public static class DataEntry<T2> extends Tuple2<String, T2> {
		private static final long serialVersionUID = 1L;

		DataEntry(String path, T2 value) {
			T2 v = value;
			if (v == null)
				v = (T2) Context.none;

			this._1 = path;
			this._2 = v;
		}
	}

	public static DataEntry data(Object data) {
		return new DataEntry(Context.DSD_PATH, data);
	}
	
	public static <T> OutputEntry<T> outEntry(String path, T value, int index) {
		return new OutputEntry(path, value, index);
	}

	public static <T> OutputEntry<T> dbOutEntry(String path, T value) {
		return new OutputEntry(path, value, true, 0);
	}

	public static InputEntry input(String path) {
		return new InputEntry(path, null, 0);
	}

	public static OutputEntry outEntry(String path) {
		return new OutputEntry(path, null, 0);
	}

	public static InputEntry inEntry(String path) {
		return new InputEntry(path, null, 0);
	}

	public static Entry at(String path, Object value) {
		return new Entry(path, value, 0);
	}

	public static Entry at(String path, Object value, int index) {
		return new Entry(path, value, index);
	}

	public static <T> InputEntry<T> inEntry(String path, T value) {
		return new InputEntry(path, value, 0);
	}

	public static <T> InputEntry<T> dbInEntry(String path, T value, String annotation) {
		InputEntry<T> ie = new InputEntry(path, value, true, 0);
		ie.annotation(annotation);
		return ie;
	}
	
	public static <T> InputEntry<T> dbInEntry(String path, T value) {
		return new InputEntry(path, value, true, 0);
	}

	public static <T> InputEntry<T> inEntry(String path, T value, int index) {
		return new InputEntry(path, value, index);
	}

	public static <T> InputEntry<T> inEntry(String path, T value, String annotation) {
		InputEntry<T> ie = inEntry(path, value);
		ie.annotation(annotation);
		return ie;
	}
	
	public static InputEntry inoutEntry(String path) {
		return new InputEntry(path, null, 0);
	}

	public static <T> InoutEntry<T> inoutEntry(String path, T value) {
		return new InoutEntry(path, value, 0);
	}

	public static <T> InoutEntry<T> inoutEntry(String path, T value, int index) {
		return new InoutEntry(path, value, index);
	}
	
	public static <T> InoutEntry<T> inoutEntry(String path, T value, String annotation) {
		InoutEntry<T> ie = inoutEntry(path, value);
		ie.annotation(annotation);
		return ie;
	}
	
	public static <T> Entry<T> entry(String path, T value, String association) {
		return new Entry<T>(path, value, association);
	}
	
	public static Par persistent(Par par) {
		par.setPersistent(true);
		return par;
	}
	
	public static <S extends Setter> boolean isPersistent(S setter) {
			return setter.isPersistent();
	}
	
	public static <S extends Setter> S store(S setter)
			throws EvaluationException, RemoteException {
		if (!setter.isPersistent()) {
			setter.setPersistent(true);
			((Evaluation) setter).getValue();
		}
		return setter;
	}
	
	public static <T2> Entry<T2> persistent(Entry<T2> entry) {
		entry.setPersistent(true);
		return entry;
	}
	
	public static <T2> Entry<T2> dbEntry(String path) {
		Entry<T2> e = new Entry<T2>(path);
		e.setPersistent(true);
		return e;
	}
	
	public static <T2> Entry<T2> dbEntry(String path, T2 value) {
		Entry<T2> e = new Entry<T2>(path, value);
		e.setPersistent(true);
		return e;
	}
	
	public static Arg[] entries(String... entries)
			throws ContextException {
		ArgSet as = new ArgSet();
		for (String name : entries) {
			as.add(new Entry(name, Context.none));
		}
		return as.toArray();
	}
	
	public static Arg[] entries(Entry... entries)
			throws ContextException {
		ArgSet as = new ArgSet();
		for (Entry e : entries) {
			as.add(e);
		}
		return as.toArray();
	}

	public static URL url(Evaluation entry) throws EvaluationException {
		URL dburl = null;
		try {
			Object obj = entry.asis();
			if (obj instanceof URL)
				dburl = (URL) obj;
			else {
				if (entry instanceof Setter) {
					((Setter) entry).setPersistent(true);
					entry.getValue();
					dburl = (URL) entry.asis();
				}
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		return dburl;
	}
	
	public static FidelityEntry entry(String x1, FidelityInfo x3) {
		return new FidelityEntry(x1, x3);
	}
	
	public static StrategyEntry strategyEntry(String x1, Strategy strategy) {
		return new StrategyEntry(x1, strategy);
	}
	
	public static <T1, T2> T1 key(Tuple2<T1, T2> entry) {
		return entry._1;
	}
	
	public static <T2> String path(Tuple2<String, T2> entry) {
		return entry._1;
	}
	
	public static <T1, T2> T2 value(Tuple2<T1, T2> entry) throws EvaluationException {
		try {
			return entry.getValue();
		} catch (RemoteException e) {
			throw new EvaluationException(e);
		}
	}
		
	public static <T extends List<?>> Table table(T... elems) {
		int rowCount = elems.length;
		int columnCount = ((List<?>) elems[0]).size();
		Table out = new Table(rowCount, columnCount);
		for (int i = 0; i < rowCount; i++) {
			if (elems[i] instanceof Header) {
				out.setColumnIdentifiers(elems[0]);
			} else {
				out.addRow((List<?>) elems[i]);
			}
		}
		return out;
	}

	public static void rowNames(Table table, List rowIdentifiers) {
		table.setRowIdentifiers(rowIdentifiers);
	}
	
	public static List<String> rowNames(Table table) {
		return table.getRowNames();
	}
	
	
	public static void columnNames(Table table, List columnIdentifiers) {
		table.setColumnIdentifiers(columnIdentifiers);
	}
	
	public static List<String> columnNames(Table table) {
		return table.getColumnNames();
	}
	
	public static int rowSize(Table table) {
		return table.getRowCount();
	}
			
	public static int columnSize(Table table) {
		return table.getColumnCount();
	}
	
	public static Map<String, Object> rowMap(Table table, String rowName) {
		return table.getRowMap(rowName);
	}
	
	public static Object value(Table table, String rowName, String columnName) {
		return table.getValue(rowName, columnName);
	}
	
	public static Object value(Table table, int row, int column) {
		return table.getValueAt(row, column);
	}
	
	public static <T extends Object> ListContext<T> listContext(T... elems)
			throws ContextException {
		ListContext<T> lc = new ListContext<T>();
		for (int i = 0; i < elems.length; i++) {
			lc.add(elems[i]);
		}
		return lc;
	}

	public static Map<Object, Object> dictionary(Tuple2<?, ?>... entries) {
		Map<Object, Object> map = new HashMap<Object, Object>();
		for (Tuple2<?, ?> entry : entries) {
			map.put(entry._1, entry._2);
		}
		return map;
	}
	
	public static <K, V> Map<K, V> map(Tuple2<K, V>... entries) {
		Map<K, V> map = new HashMap<K, V>();
		for (Tuple2<K, V> entry : entries) {
			map.put(entry._1, entry._2);
		}
		return map;
	}

	public static Loop loop(int to) {
		Loop loop = new Loop(to);
		return loop;
	}

	public static Loop loop(int from, int to) {
		Loop loop = new Loop(from, to);
		return loop;
	}
	
	public static Loop loop(String template, int to) {
		Loop loop = new Loop(template, 1, to);
		return loop;
	}
	
	public static Loop loop(List<String> templates, int to) {
		Loop loop = new Loop(templates, to);
		return loop;
	}
	
	public static Loop loop(String template, int from, int to) {
		Loop loop = new Loop(template, from, to);
		return loop;
	}
	
	public static List<String> names(Loop loop, String prefix) {
		return loop.getNames(prefix);
	}
		
	public static String[] names(String name, int size, int from) {
		List<String> out = new ArrayList<String>();
		for (int i = from - 1; i < from + size - 1; i++) {
			out.add(name + (i + 1));
		}
		String[] names = new String[size];
		out.toArray(names);
		return names;
	}
	
	private static String getUnknown() {
		return "unknown" + count++;
	}
	
	private static String getUnknown(String name) {
		return name + count++;
	}
	
	public static class Header<T> extends ArrayList<T> {
		private static final long serialVersionUID = 1L;

		public Header() {
			super();
		}
		
		public Header(int initialCapacity) {
			super(initialCapacity);
		}
	}
}