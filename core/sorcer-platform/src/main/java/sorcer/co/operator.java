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

import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import sorcer.co.tuple.*;
import sorcer.core.SorcerConstants;
import sorcer.core.context.Copier;
import sorcer.core.context.ListContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.ContextEntry;
import sorcer.core.context.model.ent.Proc;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.plexus.FiEntry;
import sorcer.core.provider.DatabaseStorer;
import sorcer.core.signature.NetletSignature;
import sorcer.core.signature.ObjectSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.netlet.ServiceScripter;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.ContextModel;
import sorcer.service.modeling.Variability;
import sorcer.service.modeling.Variability.Type;
import sorcer.util.*;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.SdbUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import java.util.Collections;
import java.util.concurrent.Callable;

import static sorcer.po.operator.invoker;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class operator {

	private static int count = 0;

	public static <T1> Tuple1<T1> x(T1 x1 ){
		return new Tuple1<T1>( x1 );
	}

	public static <T1> Tuple1<T1> t(T1 x1 ){
		return new Tuple1<T1>( x1 );
	}

	public static <T1,T2> Tuple2<T1,T2> x(T1 x1, T2 x2 ){
		return new Tuple2<T1,T2>( x1, x2 );
	}

	public static <T1,T2> Tuple2<T1,T2> t(T1 x1, T2 x2 ){
		return new Tuple2<T1,T2>( x1, x2 );
	}

	public static <T1,T2> Tuple2<T1,T2> kv(T1 x1, T2 x2 ){
		return new Tuple2<T1,T2>( x1, x2 );
	}

	public static <T1,T2,T3> Tuple3<T1,T2,T3> x(T1 x1, T2 x2, T3 x3 ){
		return new Tuple3<T1,T2,T3>( x1, x2, x3 );
	}

	public static <T1,T2,T3> Tuple3<T1,T2,T3> t(T1 x1, T2 x2, T3 x3 ){
		return new Tuple3<T1,T2,T3>( x1, x2, x3 );
	}

	public static <T1,T2,T3,T4> Tuple4<T1,T2,T3,T4> x(T1 x1, T2 x2, T3 x3, T4 x4 ){
		return new Tuple4<T1,T2,T3,T4>( x1, x2, x3, x4 );
	}

	public static <T1,T2,T3,T4> Tuple4<T1,T2,T3,T4> t(T1 x1, T2 x2, T3 x3, T4 x4 ){
		return new Tuple4<T1,T2,T3,T4>( x1, x2, x3, x4 );
	}

	public static <T1,T2,T3,T4,T5> Tuple5<T1,T2,T3,T4,T5> x(T1 x1, T2 x2, T3 x3, T4 x4, T5 x5 ){
		return new Tuple5<T1,T2,T3,T4,T5>( x1, x2, x3, x4, x5 );
	}

	public static <T1,T2,T3,T4,T5> Tuple5<T1,T2,T3,T4,T5> t(T1 x1, T2 x2, T3 x3, T4 x4, T5 x5 ){
		return new Tuple5<T1,T2,T3,T4,T5>( x1, x2, x3, x4, x5 );
	}

	public static <T1,T2,T3,T4,T5,T6> Tuple6<T1,T2,T3,T4,T5,T6> x(T1 x1, T2 x2, T3 x3, T4 x4, T5 x5, T6 x6 ){
		return new Tuple6<T1,T2,T3,T4,T5,T6>( x1, x2, x3, x4, x5, x6 );
	}

	public static <T1,T2,T3,T4,T5,T6> Tuple6<T1,T2,T3,T4,T5,T6> t(T1 x1, T2 x2, T3 x3, T4 x4, T5 x5, T6 x6 ){
		return new Tuple6<T1,T2,T3,T4,T5,T6>( x1, x2, x3, x4, x5, x6 );
	}

	public static <T> List<T> inCotextValues(Context<T> context) throws ContextException {
		return ((ServiceContext)context).getInValues();
	}

	public static <T> List<T> inContextPaths(Context<T> context) throws ContextException {
		return ((ServiceContext)context).getInPaths();
	}

	public static <T> List<T> outContextValues(Context<T> context) throws ContextException {
		return ((ServiceContext)context).getOutValues();
	}

	public static <T> List<T> outContextPaths(Context<T> context) throws ContextException {
		return ((ServiceContext)context).getOutPaths();
	}

	public static ServiceSignature.Out outPaths(Object... elems) {
        List<Path> pl = new ArrayList(elems.length);
        for (Object o : elems) {
            if (o instanceof String) {
                pl.add(new Path((String)o));
            } else if  (o instanceof Path) {
                pl.add(((Path)o));
            }
        }
        Path[]  pa = new Path[pl.size()];
        return new ServiceSignature.Out(pl.toArray(pa));
	}

	public static ServiceSignature.In inPaths(Object... elems) {
        List<Path> pl = new ArrayList(elems.length);
        for (Object o : elems) {
            if (o instanceof String) {
                pl.add(new Path((String)o));
            } else if  (o instanceof Path) {
                pl.add(((Path)o));
            }
        }
        Path[]  pa = new Path[pl.size()];
		return new ServiceSignature.In(pl.toArray(pa));
	}

	public static Path filePath(String filename) {
		if(Artifact.isArtifact(filename)) {
			try {
				URL url = ResolverHelper.getResolver().getLocation(filename, "ntl");
				File file = new File(url.toURI());
				return new Path(file.getPath());
			} catch (ResolverException | URISyntaxException e) {
				e.printStackTrace();
			}
		}
		return new Path (filename);
	}

	public static Object[] typeArgs(Object... args) {
		return args;
	}

	public static <T> T[] array(T... elems) {
		return elems;
	}

	public static Object[] objects(Object... elems) {
		return elems;
	}

	public static <T> T[] array(List<T> list) {
		T[] na = (T[]) Array.newInstance(list.get(0).getClass(), list.size());
		return list.toArray(na);

	}

	public static Set<Object> bag(Object... elems) {
		return new HashSet<Object>(list(elems));
	}

	public static <T> Set<T> set(T... elems) {
		return new HashSet<T>(list(elems));
	}

	public static <T> List<T> list(T... elems) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, elems);
		return list;
	}

	public static List<Object> row(Object... elems) {
        List<Object> list = new ArrayList<>();
        Collections.addAll(list, elems);
        return list;
	}

	public static List<Object> values(Response response) {
		return response.getValues();
	}

	public static List<String> names(Response response) {
		return response.getNames();
	}

	public static List<Object> values(Object... elems) {
        List<Object> list = new ArrayList<>();
        Collections.addAll(list, elems);
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
        List<String> list = new ArrayList<>();
        Collections.addAll(list, elems);
		return list;
	}

	public static List<String> names(List<String>... nameLists) {
		List<String> out = new ArrayList<String>();
		for (List<String> each : nameLists) {
			out.addAll(each);
		}
		return out;
	}

	public static <T1, T2> Tuple2<T1, T2> assoc(T1 x1, T2 x2) {
		return new Tuple2<T1, T2>(x1, x2);
	}

	public static String path(List<String> attributes) {
		if (attributes.size() == 0)
			return null;
		if (attributes.size() > 1) {
			StringBuilder spr = new StringBuilder();
			for (int i = 0; i < attributes.size() - 1; i++) {
				spr.append(attributes.get(i)).append(SorcerConstants.CPS);
			}
			spr.append(attributes.get(attributes.size() - 1));
			return spr.toString();
		}
		return attributes.get(0);
	}

	public static Path path(String path) {
		return new Path(path);
	}

	public static Path path(String path, Object info) {
		return new Path(path, info);
	}

	public static Path map(String path, Object info) {
		return new Path(path, info, Path.Type.MAP);
	}

	public static <T> Entry<T> val(Path path, T value) {
		Entry ent = new Entry<T>(path.path, value);
		ent.annotation(path.info.toString());
		ent.setType(Type.INPUT);
		return ent;
	}

	public static <T> Entry<T> val(String path, T value) {
		Entry ent = new Entry<T>(path, value);
		ent.setType(Type.INPUT);
		return ent;
	}

	public static ContextEntry contextVal(String path, Context value) {
		ContextEntry ent = new ContextEntry(path, value);
		ent.isValid(false);
		ent.setType(Type.INPUT);
		return ent;
	}

	public static ContextEntry cxtVal(String path, Context value) {
		return contextVal(path, value) ;
	}

	public static ContextEntry contextVal(String path, Entry... entries) throws ContextException {
		ServiceContext cxt = new ServiceContext();
		for (Entry e : entries) {
			cxt.put((String) e._1, e.get());
		}
		cxt.isValid(false);
		return contextVal(path, cxt) ;
	}

	public static ContextEntry cxtVal(String path, Entry... entries) throws ContextException {
		return contextVal(path, entries);
	}

	public static Entry in(Entry... entries) {
		for (Entry  entry : entries) {
			entry.setType(Type.INPUT);
		}
		return entries[0];
	}

	public static Entry out(Entry... entries) {
		for (Entry  entry : entries) {
			entry.setType(Type.OUTPUT);
		}
		return entries[0];
	}

	public static Entry inout(Entry... entries) {
		for (Entry  entry : entries) {
			entry.setType(Type.INOUT);
		}
		return entries[0];
	}

	public static Object annotation(Entry entry) {
        return entry.annotation();
    }

	public static Signature.Direction direction(Entry entry) {
		Object ann = entry.annotation();
		if (ann instanceof String)
			return Signature.Direction.fromString((String) ann);
		else
			return (Signature.Direction) ann;
	}

    public static boolean isSorcerLambda(Class clazz) {
		Class[] types = { EntryCollable.class, ValueCallable.class, Client.class,
				ConditionCallable.class, Callable.class };
		for (Class cl : types) {
			if (clazz == cl) {
				return true;
			}
		}
		return false;
	}

    public static DependencyEntry dep(String path, Path... paths) {
		return new DependencyEntry(path, Arrays.asList(paths));
	}

	public static DependencyEntry dep(String path, List<Path> paths) {
		return new DependencyEntry(path, paths);
	}

	public static DependencyEntry[] deps(DependencyEntry... dependencies) {
		return dependencies;
	}

	public static <T> Entry<T> rvEnt(String path, T value) {
		Entry<T> e = new Entry<T>(path, value);
		return e.setReactive(true);
	}

	public static <T> Entry<T> urvEnt(String path, T value) {
		Entry<T> e = new Entry<T>(path, value);
		return e.setReactive(false);
	}


	public static <T> Reactive<T> rrvEnt(Context<T> cxt, String path) throws ContextException {
		T obj = cxt.asis(path);
		if (obj instanceof Reactive)
			return ((Reactive<T>) obj).setReactive(true);
		else
			throw new ContextException("No Entry at path: " + path);
	}

	public static <T> Reactive<T> urvEnt(Context<T> cxt, String path) throws ContextException {
		T obj = cxt.asis(path);
		if (obj instanceof Reactive)
			return ((Reactive<T>) obj).setReactive(false);
		else
			throw new ContextException("No Entry at path: " + path);
	}

	public static Entry<Object>  val(String path) {
		Entry ent = new Entry(path, null);
		ent.setType(Variability.Type.VAL);
		return ent;
	}

	public static Object val(Entry ent, String path) throws ContextException {
		return  ((ContextEntry)ent).getContextValue(path);
	}

    public static <T> OutputEntry<T> outVal(String path, T value) {
		if (value instanceof String && ((String)value).indexOf('|') > 0) {
			OutputEntry oe =  outVal(path, null);
			oe.annotation(value);
			return oe;
		}
		OutputEntry ent = new OutputEntry(path, value, 0);
		if (value instanceof Class)
			ent.setValClass((Class) value);
		return ent;
	}

	public static <T> OutputEntry<T> outVal(String path, T value, String annotation) {
		OutputEntry oe =  outVal(path, value);
		oe.annotation(annotation);
		return oe;
	}

	public static class DataEntry<T> extends Entry<T> {
		private static final long serialVersionUID = 1L;

		DataEntry(String path, T value) {
			T v = value;
			if (v == null)
				v = (T) Context.none;

			this._1 = path;
			this._2 = v;
		}
	}

	public static DataEntry data(Object data) {
		return new DataEntry(Context.DSD_PATH, data);
	}

	public static <T> OutputEntry<T> outVal(String path, T value, int index) {
		return new OutputEntry(path, value, index);
	}

	public static <T> OutputEntry<T> dbOutVal(String path, T value) {
		return new OutputEntry(path, value, true, 0);
	}

	public static InputEntry input(String path) {
		return new InputEntry(path, null, 0);
	}

	public static OutputEntry outVal(String path) {
		return new OutputEntry(path, null, 0);
	}

	public static InputEntry inVal(String path) {
		return new InputEntry(path, null, 0);
	}

	public static Entry at(String path, Object value) {
		return new Entry(path, value, 0);
	}

	public static Entry at(String path, Object value, int index) {
		return new Entry(path, value, index);
	}

	public static <T> InputEntry<T> inVal(String path, T value) {
		return new InputEntry(path, value, 0);
	}

	public static <T> InputEntry<T> dbInVal(String path, T value, String annotation) {
		InputEntry<T> ie = new InputEntry(path, value, true, 0);
		ie.annotation(annotation);
		return ie;
	}

	public static <T> InputEntry<T> dbInVal(String path, T value) {
		return new InputEntry(path, value, true, 0);
	}

	public static <T> InputEntry<T> inVal(String path, T value, int index) {
		return new InputEntry(path, value, index);
	}

	public static <T> InputEntry<T> inVal(String path, T value, String annotation) {
		InputEntry<T> ie = inVal(path, value);
		ie.annotation(annotation);
		return ie;
	}

	public static <T> InputEntry<T> inVal(String path, T value, Class valClass, String annotation) {
		InputEntry<T> ie = new InputEntry(path, value, 0);
		if (valClass != null)
			ie.setValClass(valClass);
		ie.annotation(annotation);
		return ie;
	}

	public static <T> InputEntry<T> inVal(String path, T value, Class valClass) {
		return inVal(path, value, valClass, null);
	}

    public static <T> Entry<T> setValue(Entry<T> entry, T value) throws ContextException {
		try {
			entry.setValue(value);
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		if (entry instanceof Proc) {
			Proc procEntry = (Proc)entry;
			if (procEntry.getScope() != null && procEntry.getContextable() == null) {
				procEntry.getScope().putValue(procEntry.getName(), value);
			}
		}
		entry.isValid(false);
		return entry;
	}

	public static ContextEntry setValue(Entry entry, String contextPath, Object value) throws ContextException {
		((ContextEntry)entry).setValue(contextPath, value);
		return (ContextEntry)entry;
	}

	public static ContextEntry setValue(Entry entry, Entry... entries) throws ContextException {
		for (Entry e :  entries) {
				((ContextEntry) entry).setValue(e.getName(), e.get());
		}
		return (ContextEntry)entry;
	}

	public static <S extends Setter> boolean isPersistent(S setter) {
		return setter.isPersistent();
	}

	public static URL storeArg(Object object) throws EvaluationException {
		URL dburl = null;
		try {
			if (object instanceof Evaluation) {
				Evaluation entry = (Evaluation)	object;
				Object obj = entry.asis();
				if (SdbUtil.isSosURL(obj))
					dburl = (URL) obj;
				else {
					if (entry instanceof Setter) {
						((Setter) entry).setPersistent(true);
						dburl = SdbUtil.store(obj);
						((Setter)entry).setValue(dburl);
						if (object instanceof Proc) {
							// its eval is now persisted
							((Proc)object)._2 = null;
						}
						return dburl;
					}
				}
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}	return dburl;
	}


	public static URL store(Object object) throws EvaluationException {
		try {
			if (object instanceof UuidObject)
				return SdbUtil.store(object);
			else  {
				return SdbUtil.store(new UuidObject(object));
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
	}

	public static URL dbURL() throws MalformedURLException {
		return new URL(Sorcer.getDatabaseStorerUrl());
	}

	public static URL dsURL() throws MalformedURLException {
		return new URL(Sorcer.getDataspaceStorerUrl());
	}

	public static void dbURL(Object object, URL dbUrl)
			throws MalformedURLException {
		if (object instanceof Proc)
			((Proc) object).setDbURL(dbUrl);
		else if (object instanceof ServiceContext)
			((ServiceContext) object).setDbUrl("" + dbUrl);
		else
			throw new MalformedURLException("Can not set URL to: " + object);
	}

	public static URL dbURL(Object object) throws MalformedURLException {
		if (object instanceof Proc)
			return ((Proc) object).getDbURL();
		else if (object instanceof ServiceContext)
			return new URL(((ServiceContext) object).getDbUrl());
		return null;
	}

	public static Object retrieve(URL url) throws IOException {
		return url.getContent();
	}

	public static URL update(Object object) throws MogramException,
			SignatureException {
		return SdbUtil.update(object);
	}

	public static List<String> list(URL url) throws ExertionException,
			SignatureException, ContextException {
		return SdbUtil.list(url);
	}

	public static List<String> list(DatabaseStorer.Store store) throws ExertionException,
			SignatureException, ContextException {
		return SdbUtil.list(store);
	}

	public static URL delete(Object object) throws ExertionException,
			SignatureException, ContextException {
		return SdbUtil.delete(object);
	}

	public static int clear(DatabaseStorer.Store type) throws ExertionException,
			SignatureException, ContextException {
		return SdbUtil.clear(type);
	}

	public static int size(Collection collection) {
		return collection.size();
	}

	public static int size(ContextModel model) {
		return ((ServiceContext)model).size();
	}

	public static int size(Context context) {
		return context.size();
	}

	public static int size(Map map) {
		return map.size();
	}

	public static int size(DatabaseStorer.Store type) throws ExertionException,
			SignatureException, ContextException {
		return SdbUtil.size(type);
	}

	public static <T extends Entry> T persistent(T entry) {
		entry.setPersistent(true);
		return entry;
	}

	public static <T> Entry<T> dbVal(String path) {
		Entry<T> e = new Proc<T>(path);
		e.setPersistent(true);
		return e;
	}

	public static <T> Entry<T> dbVal(String path, T value) throws EvaluationException {
		Entry<T> e = new Entry<T>(path, value);
		e.setPersistent(true);
		if (SdbUtil.isSosURL(value)) {
			try {
				e.getValue();
			} catch (RemoteException ex) {
				throw new EvaluationException(ex);
			}
		}
		return e;
	}

    public static URL storeArg(Context context, String path) throws EvaluationException {
		URL dburl = null;
		try {
			Object v = context.asis(path);
			if (v instanceof URL)
				return (URL) v;
			else if (v instanceof Setter && v instanceof Evaluation) {
				Object nv = ((Evaluation)v).asis();
				if (nv instanceof URL)
					return (URL) nv;
				((Setter) v).setPersistent(true);
				((Evaluation)v).getValue();
				dburl = (URL) ((Evaluation)v).asis();
//			}
//			else {
//				Entry dbe = new Entry(path, context.asis(path));
//				((Setter)dbe).setPersistent(true);
//				dbe.getValue();
//				context.putValue(path, dbe);
//				dburl = (URL) dbe.asis();
			} else {
				dburl = store(v);
				context.putValue(path, dburl);
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		return dburl;
	}

	public static StrategyEntry strategyEnt(String x1, Strategy strategy) {
		return new StrategyEntry(x1, strategy);
	}

	public static <T1, T2> T1 key(Tuple2<T1, T2> entry) {
		return entry._1;
	}

	public static <T2> String path(Tuple2<String, T2> entry) {
		return entry._1;
	}

	public static <T extends List> DataTable dataTable(T... elems) {
		int rowCount = elems.length;
		int columnCount = elems[0].size();
		DataTable out = new DataTable(rowCount, columnCount);
		for (int i = 0; i < rowCount; i++) {
			if (elems[i] instanceof Header) {
				out.setColumnIdentifiers(elems[0]);
			} else {
				out.addRow((List<?>) elems[i]);
			}
		}
		return out;
	}

	public static DataTable fiColumnName(DataTable table, String name) {
		table.setFiColumnName(name);
		return table;
	}

	public static ModelTable populateFidelities(ModelTable table, FiEntry... entries) {
		DataTable impl = (DataTable)table;
		List fiColumn = impl.getColumn(impl.getFiColumnName());
		if (fiColumn == null) {
			fiColumn = new ArrayList(impl.getRowCount());
			while(fiColumn.size() < impl.getRowCount())
				fiColumn.add(null);
		}
		for (FiEntry fiEnt : entries) {
			fiColumn.set(fiEnt.getIndex(), fiEnt.getFidelities());
		}

        for (int i = 0; i < fiColumn.size()-1; i++) {
            if (fiColumn.get(i) != null && fiColumn.get(i + 1) == null) {
                fiColumn.set(i + 1, fiColumn.get(i));
            }
        }

        impl.addColumn(impl.getFiColumnName(), fiColumn);
        return impl;
	}

	public static ModelTable appendFidelities(ModelTable table, FiEntry... entries) {
		DataTable impl = (DataTable)table;
		List fiColumn = impl.getColumn(impl.getFiColumnName());
		if (fiColumn == null) {
			fiColumn = new ArrayList(impl.getRowCount());
			while(fiColumn.size() < impl.getRowCount())
				fiColumn.add(null);
		}
		for (FiEntry fiEnt : entries) {
			fiColumn.set(fiEnt.getIndex(), fiEnt.getFidelities());
		}
		impl.addColumn(impl.getFiColumnName(), fiColumn);
		return impl;
	}

	public static void rowNames(DataTable table, List rowIdentifiers) {
		table.setRowIdentifiers(rowIdentifiers);
	}

	public static List<String> rowNames(DataTable table) {
		return table.getRowNames();
	}


	public static void columnNames(DataTable table, List columnIdentifiers) {
		table.setColumnIdentifiers(columnIdentifiers);
	}

	public static List<String> columnNames(DataTable table) {
		return table.getColumnNames();
	}

	public static int rowSize(DataTable table) {
		return table.getRowCount();
	}

	public static int columnSize(DataTable table) {
		return table.getColumnCount();
	}

	public static Map<String, Object> rowMap(DataTable table, String rowName) {
		return table.getRowMap(rowName);
	}

	public static Object value(DataTable table, String rowName, String columnName) {
		return table.getValue(rowName, columnName);
	}

	public static Object value(DataTable table, int row, int column) {
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

	public static <T extends Identifiable> Pool<String, T> pool(Fi.Type type, T... entries) {
		Pool<String, T> map = new Pool<>();
		map.setFiType(type);
		for (T entry : entries) {
			map.put(entry.getName(), entry);
		}
		return map;
	}

	public static <T extends Identifiable> Pool<String, T> pool(T... entries) {
		Pool<String, T> map = new Pool<>();
		for (T entry : entries) {
			map.put(entry.getName(), entry);
		}
		return map;
	}

	public static <K, V> Pool<K, V> entPool(Fi.Type type, Tuple2<K, V>... entries) {
		Pool<K, V> map = new Pool<>();
		map.setFiType(type);
		for (Tuple2<K, V> entry : entries) {
			map.put(entry._1, entry._2);
		}
		return map;
	}

	public static <K, V> Pool<K, V> entPool(Tuple2<K, V>... entries) {
		Pool<K, V> map = new Pool<>();
		for (Tuple2<K, V> entry : entries) {
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

	public static Object rasis(Entry entry)
			throws ContextException {
		String path = entry.path();
		Object o = asis(entry);
		while (o instanceof Entry && ((Entry)o)._1.equals(path)) {
			o = asis((Entry)o);;
		}
		return o;
	}

	public static Object get(Entry entry) throws ContextException {
		return rasis(entry);
	}

	public static Object asis(Entry entry)
			throws ContextException {
		try {
			return entry.asis();
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
	}

	public static <T> T asis(Context<T> context, String path)
			throws ContextException {
		return  context.asis(path);
	}

	public static <T> T rasis(Context<T> context, String path)
			throws ContextException {
		Object o = context.asis(path);
		if (o instanceof Entry)
			return (T)rasis((Entry)o);
		else
			return (T)o;
	}

	public static <T> T asis(Model model, String path)
			throws ContextException {
		return  ((ServiceContext<T>)model).asis(path);
	}

    public static <T> T asis(Mappable<T> mappable, String path)
            throws ContextException {
        return  mappable.asis(path);
    }

    public static Copier copier(Model fromContext, Arg[] fromEntries,
								Model toContext, Arg[] toEntries) throws EvaluationException {
        return new Copier(fromContext, fromEntries, toContext, toEntries);
    }

	public static List<Path> paths(Object... paths) {
		List<Path> list = new ArrayList<>();
		for (Object o : paths) {
			if (o instanceof String) {
				list.add(new Path((String) o));
			} else if (o instanceof Path) {
				list.add((Path) o);
			} else if (o instanceof Identifiable) {
				list.add(new Path(((Identifiable)o).getName()));
			}
		}
		return list;
	}

	public static List<String> paths(Context context) throws ContextException {
		return context.getPaths();
	}

	public static void remove(ServiceContext parModel, String... paths)
			throws RemoteException, ContextException {
		for (String path : paths)
			parModel.getData().remove(path);
	}

	public static Model dependsOn(ContextModel model, Entry... entries) {
		return dependsOn(model, entries);
	}

	public static Model dependsOn(Model model, Entry... entries) {
        Map<String, List<Path>> dm = ((ServiceContext)model).getMogramStrategy().getDependentPaths();
        String path = null;
        Object dependentPaths = null;
        for (Entry e : entries) {
            dependentPaths = e.value();
            if (dependentPaths instanceof List) {
                path = e.getName();
                dependentPaths =  e.value();
                dm.put(path, (List<Path>) dependentPaths);
            }
        }
		return model;
    }

    public static Map<String, List<Path>> dependencies(Model model) {
         return ((ServiceContext)model).getMogramStrategy().getDependentPaths();
    }
    
    public static Dependency dependsOn(Dependency dependee,  Evaluation... dependers) throws ContextException {
        for (Evaluation d : dependers)
            	dependee.getDependers().add(d);
        
        return dependee;
    }

	public static Dependency dependsOn(Dependency dependee, Context scope, Evaluation... dependers)
			throws ContextException {
		if (dependee instanceof Scopable) {
			Context context = null;
			context = ((Mogram) dependee).getScope();
			if (context == null)
				((Mogram) dependee).setScope(scope);
			else
				context.append(scope);
		}
		return dependsOn(dependee, dependers);
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

	/**
	 * Returns an instance by class method initialization with a service
	 * context.
	 *
	 * @param signature
	 * @return object created
	 * @throws SignatureException
	 */
	public static Object instance(ObjectSignature signature, Context context)
			throws SignatureException {
		return signature.build(context);
	}

	/**
	 * Returns an instance by constructor method initialization or by
	 * instance/class method initialization.
	 *
	 * @param signature
	 * @return object created
	 * @throws SignatureException
	 */
	public static Object instance(Signature signature)
			throws SignatureException {
		if (signature instanceof NetletSignature) {
			String source = ((NetletSignature) signature).getServiceSource();
			if (source != null) {
				try {
					ServiceScripter se = new ServiceScripter(System.out, null, Sorcer.getWebsterUrl(), true);
					se.readFile(new File(source));
					return se.interpret();
				} catch (Throwable e) {
					throw new SignatureException(e);
				}
			} else {
				throw new SignatureException("missing netlet filename");
			}
		} else if ((signature.getSelector() == null
				&& ((ObjectSignature) signature).getInitSelector() == null)
				|| (signature.getSelector() != null && signature.getSelector().equals("new"))
				|| (((ObjectSignature) signature).getInitSelector() != null
				&& ((ObjectSignature) signature).getInitSelector().equals("new")))
			return ((ObjectSignature) signature).newInstance();
		else
			return ((ObjectSignature) signature).initInstance();
	}

	public static Object created(Signature signature) throws SignatureException {
		return instance(signature);
	}

	public static Model model(Signature signature) throws SignatureException {
		Object model = instance(signature);
		if (!(model instanceof Model)) {
			throw new SignatureException("Signature does not specify te Model: " + signature);
		}
		if (model instanceof ContextModel) {
			((ContextModel)model).setBuilder(signature);
		}
		return (Model) model;
	}

	public static URL url(String urlName) throws MalformedURLException {
		return new URL(urlName);
	}
}