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

import net.jini.id.Uuid;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import sorcer.Operator;
import sorcer.co.tuple.*;
import sorcer.core.Tag;
import sorcer.core.SorcerConstants;
import sorcer.core.context.*;
import sorcer.core.context.model.ent.*;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.plexus.FiEntry;
import sorcer.core.provider.DatabaseStorer;
import sorcer.core.signature.NetletSignature;
import sorcer.core.signature.ObjectSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.netlet.ServiceScripter;
import sorcer.service.*;
import sorcer.service.Domain;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Functionality.Type;
import sorcer.service.modeling.Valuation;
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
import static sorcer.po.operator.srv;

/**
 * Created by Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class operator extends Operator {

	private static int count = 0;

	public static <T1> Tuple1<T1> x(T1 x1 ){
		return new Tuple1<T1>( x1 );
	}

	public static <T1,T2> Tuple2<T1,T2> x(T1 x1, T2 x2 ){
		return new Tuple2<T1,T2>( x1, x2 );
	}

	public static <T1,T2,T3> Tuple3<T1,T2,T3> x(T1 x1, T2 x2, T3 x3 ){
		return new Tuple3<T1,T2,T3>( x1, x2, x3 );
	}

	public static <T1,T2,T3,T4> Tuple4<T1,T2,T3,T4> x(T1 x1, T2 x2, T3 x3, T4 x4 ){
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

	public static Tie tie(String discipline, String var) {
		return new Tie(discipline, var);
	}

    public static Tie tie(String discipline) {
        return new Tie(discipline, null);
    }

    public static Coupling cplg( String var, String fromDiscipline, String toDiscipline) {
        return new Coupling(tie(fromDiscipline, var), tie(toDiscipline, var));
    }

    public static Coupling cplg(Tie from, Tie to) {
		return new Coupling(from, to);
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

	public static ServiceSignature.Read read(Object... elems) {
		List<Path> pl = new ArrayList(elems.length);
		for (Object o : elems) {
			if (o instanceof String) {
				pl.add(new Path((String)o));
			} else if  (o instanceof Path) {
				pl.add(((Path)o));
			}
		}
		Path[]  pa = new Path[pl.size()];
		return new ServiceSignature.Read(pl.toArray(pa));
	}

	public static ServiceSignature.Write write(Object... elems) {
		List<Path> pl = new ArrayList(elems.length);
		for (Object o : elems) {
			if (o instanceof String) {
				pl.add(new Path((String)o));
			} else if  (o instanceof Path) {
				pl.add(((Path)o));
			}
		}
		Path[]  pa = new Path[pl.size()];
		return new ServiceSignature.Write(pl.toArray(pa));
	}

	public static ServiceSignature.Append append(Object... elems) {
		List<Path> pl = new ArrayList(elems.length);
		for (Object o : elems) {
			if (o instanceof String) {
				pl.add(new Path((String)o));
			} else if  (o instanceof Path) {
				pl.add(((Path)o));
			}
		}
		Path[]  pa = new Path[pl.size()];
		return new ServiceSignature.Append(pl.toArray(pa));
	}

    public static Signature.State state(Object... elems) {
        List<Path> pl = new ArrayList(elems.length);
        for (Object o : elems) {
            if (o instanceof String) {
                pl.add(new Path((String)o));
            } else if  (o instanceof Path) {
                pl.add(((Path)o));
            }
        }
        Path[]  pa = new Path[pl.size()];
        return new Signature.State(pl.toArray(pa));
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

	public static double[] vector(double... elems) {
		return elems;
	}

	public static int[] vector(int... elems) {
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

    public static Path path(String path, Signature.Direction dir) {
        return new Path(path, dir);
    }

	public static Path map(String path, Object info) {
		return new Path(path, info, Path.Type.MAP);
	}

	public static <T> Function<T> init(String domain, String path, T value) {
		Function ent = new Function<T>(path, value);
		ent.annotation(domain);
		ent.setType(Type.DOMAIN_PRED);
		return ent;
	}

	public static <T> Function<T> init(String path, T value) {
		Function ent = new Function<T>(path, value);
		ent.setType(Type.PRED);
		return ent;
	}

	public static <T> Function<T> val(String domain, String path, T value) {
		Function ent = new Function<T>(path, value);
		ent.annotation(domain);
		ent.setType(Type.DOMAIN_CONSTANT);
		return ent;
	}

	public static <T> Value<T> val(String path, T value) {
		Value ent = null;
		if (value instanceof Value) {
            ent = new Value<T>(path, (T) ((Value)value).getOut());
        } else {
            ent = new Value<T>(path, value);
        }
        ent.setType(Type.VAL);
        return ent;
	}

	public static <T> Value<T> val(Path path, T value) {
        Value ent = null;
        if (value instanceof Value) {
            ent = new Value<T>(path.path, (T) ((Value)value).getOut());
            ent.setImpl(value);
        } else {
            ent = new Value<T>(path.path, value);
        }
		ent.annotation(path.info.toString());
		ent.setType(Type.VAL);
		return ent;
	}

	public static Config config(Object path, Setup... entries) {
		Config ent = new Config(path.toString(), entries);
		ent.setValid(false);
		ent.setType(Type.CONFIG);
		return ent;
	}

	public static Setup setup(Object aspect, Context value) {
		Setup ent = new Setup(aspect.toString(), value);
		ent.isValid(false);
//		ent.setType(Type.INPUT);
		return ent;
	}

	public static Setup setup(Object aspect, Entry... entries) throws ContextException {
		return setup(aspect, null, null, entries);
	}

	public static Setup setup(Object aspect, String entryName, Function... entries) throws ContextException {
		return setup(aspect, entryName, null, entries);
	}

	public static Setup setup(Object aspect, String entryName, String fiName, Entry... entries) throws ContextException {
		if (entries != null && entries.length > 0) {
			ServiceContext cxt;
			if (entryName == null)
				cxt = new ServiceContext(aspect.toString());
			else
				cxt = new ServiceContext(entryName);

			if (fiName != null) {
				cxt.setSubjectPath(fiName);
			}

			for (Entry e : entries) {
				cxt.put(e.getName(), e.get());
			}
			cxt.isValid(false);
			return new Setup(aspect.toString(), cxt);
		} else {
			return new Setup(aspect.toString(), null);
		}
	}

    public static Uuid id(Mogram mogram) {
        return mogram.getId();
    }

    public static void setId(Mogram mogram, Uuid id) {
	    mogram.setId(id);
    }

	public static Entry in(Entry... entries) {
		for (Entry entry : entries) {
			entry.setType(Type.INPUT);
		}
		return entries[0];
	}

	public static Entry out(Entry... entries) {
		for (Entry entry : entries) {
			entry.setType(Type.OUTPUT);
		}
		return entries[0];
	}

	public static Entry inout(Entry... entries) {
		for (Entry entry : entries) {
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

    public static ExecDependency dep(String path, Path... paths) {
		return new ExecDependency(path, Arrays.asList(paths));
	}

	public static ExecDependency dep(String path, Conditional condition, Path... paths) {
        ExecDependency de = new ExecDependency(path, condition, Arrays.asList(paths));
        de.setType(Type.CONDITION);
        return de;
	}

	public static ExecDependency dep(String path, Fidelity fi, Path... paths) {
		ExecDependency de = new ExecDependency(path, Arrays.asList(paths));
		de.annotation(fi);
		de.setType(Functionality.Type.FIDELITY);
        return de;
	}

	public static ExecDependency dep(String path, Conditional condition, List<Path> paths) {
        ExecDependency de = new ExecDependency(path, condition, paths);
        de.setType(Type.CONDITION);
        return de;
	}

	public static ExecDependency dep(String path, List<Path> paths) {
		return new ExecDependency(path, paths);
	}


	public static ExecDependency dep(String path, Fidelity fi, List<Path> paths) {
		ExecDependency de = new ExecDependency(path, paths);
		de.annotation(fi);
		de.setType(Functionality.Type.FIDELITY);
		return de;
	}

	public static Value<Object> val(String path) {
		Value ent = new Value(path, null);
		ent.setType(Functionality.Type.VAL);
		return ent;
	}

	public static Object val(Setup ent, String path) throws ContextException {
		return  ent.getContextValue(path);
	}

    public static <T> OutputValue<T> outVal(String path, T value) {
		if (value instanceof String && ((String)value).indexOf('|') > 0) {
			OutputValue oe =  outVal(path, null);
			oe.annotation(value);
			return oe;
		}
		OutputValue ent = new OutputValue(path, value, 0);
		if (value instanceof Class)
			ent.setValClass((Class) value);
		return ent;
	}

	public static <T> OutputValue<T> outVal(String path, T value, String annotation) {
		OutputValue oe =  outVal(path, value);
		oe.annotation(annotation);
		return oe;
	}

	public static class DataEntry<T> extends Entry<T> {
		private static final long serialVersionUID = 1L;

		DataEntry(String path, T value) {
			T v = value;
			if (v == null)
				v = (T) Context.none;

			this.setKey(path);
			this.set(v);
		}
	}

	public static DataEntry data(Object data) {
		return new DataEntry(Context.DSD_PATH, data);
	}

	public static <T> OutputValue<T> outVal(String path, T value, int index) {
		return new OutputValue(path, value, index);
	}

	public static <T> OutputValue<T> dbOutVal(String path, T value) {
		return new OutputValue(path, value, true, 0);
	}

	public static InputValue input(String path) {
		return new InputValue(path, null, 0);
	}

	public static OutputValue outVal(String path) {
		return new OutputValue(path, null, 0);
	}

	public static InputValue inVal(String path) {
		return new InputValue(path, null, 0);
	}

	public static Function at(String path, Object value) {
		return new Function(path, value, 0);
	}

	public static Function at(String path, Object value, int index) {
		return new Function(path, value, index);
	}

	public static <T> InputValue<T> inVal(String path, T value) {
		return new InputValue(path, value, 0);
	}

	public static <T> InputValue<T> dbInVal(String path, T value, String annotation) {
		InputValue<T> ie = new InputValue(path, value, true, 0);
		ie.annotation(annotation);
		return ie;
	}

	public static <T> InputValue<T> dbInVal(String path, T value) {
		return new InputValue(path, value, true, 0);
	}

	public static <T> InputValue<T> inVal(String path, T value, int index) {
		return new InputValue(path, value, index);
	}

	public static <T> InputValue<T> inVal(String path, T value, String annotation) {
		InputValue<T> ie = inVal(path, value);
		ie.annotation(annotation);
		return ie;
	}

	public static <T> InputValue<T> inVal(String path, T value, Class valClass, String annotation) {
		InputValue<T> ie = new InputValue(path, value, 0);
		if (valClass != null)
			ie.setValClass(valClass);
		ie.annotation(annotation);
		return ie;
	}

	public static <T> InputValue<T> inVal(String path, T value, Class valClass) {
		return inVal(path, value, valClass, null);
	}

	public static <S extends Setter> boolean isPersistent(S setter) {
		return setter.isPersistent();
	}

	public static URL storeVal(Object object) throws EvaluationException {
		URL dburl = null;
		Entry entry = null;
		try {
			if (object instanceof Entry) {
				entry = (Entry)	object;
				Object obj = entry.getData();
				if (SdbUtil.isSosURL(obj)) {
					dburl = (URL) obj;
					SdbUtil.update(dburl, entry.getOut());
				}
				else {
					entry.setPersistent(true);
					dburl = SdbUtil.store(entry.getOut());
				}
			}
			entry.setOut(null);
			entry.setImpl(dburl);
			entry.setValid(true);
			return dburl;
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
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

	public static List<String> list(URL url) throws MogramException,
			SignatureException, ContextException {
		return SdbUtil.list(url);
	}

	public static List<String> list(DatabaseStorer.Store store) throws MogramException,
			SignatureException, ContextException {
		return SdbUtil.list(store);
	}

	public static URL delete(Object object) throws MogramException,
			SignatureException, ContextException {
		return SdbUtil.delete(object);
	}

	public static int clear(DatabaseStorer.Store type) throws MogramException,
			SignatureException, ContextException {
		return SdbUtil.clear(type);
	}

	public static int size(Collection collection) {
		return collection.size();
	}

	public static int size(Fi fidelity) {
		return fidelity.getSelects().size();
	}

	public static int size(Model model) {
		return ((ServiceContext)model).size();
	}

	public static int size(Context context) {
		return context.size();
	}

	public static int size(Map map) {
		return map.size();
	}

	public static int size(DatabaseStorer.Store type) throws MogramException,
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
		Value<T> e = new Value<T>(path, value);
		e.setPersistent(true);
		if (SdbUtil.isSosURL(value)) {
			try {
				e.get();
			} catch (ContextException ex) {
				throw new EvaluationException(ex);
			}
		}
		return e;
	}

    public static URL storeVal(Context context, String path) throws EvaluationException {
		URL dburl = null;
		try {
			Object v = context.asis(path);
			if (SdbUtil.isSosURL(v))
				return (URL) v;
			else if (v instanceof Setter && v instanceof Evaluation) {
				Object nv = ((Evaluation)v).asis();
				if (SdbUtil.isSosURL(nv)) {
					return (URL) nv;
				}
				((Setter) v).setPersistent(true);
				((Evaluation)v).evaluate();
				dburl = (URL) ((Evaluation)v).asis();
			} else if (context.asis(path) instanceof Entry) {
				Entry dbe = new Entry(path, context.asis(path));
				dbe.setPersistent(true);
				dbe.get();
				context.putValue(path, dbe);
				dburl = (URL) dbe.asis();
			} else {
				dburl = store(v);
				context.putValue(path, dburl);
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		return dburl;
	}

	public static StrategyEntry strategyEnt(String name, Strategy strategy) {
		return new StrategyEntry(name, strategy);
	}

	public static <K, V> String key(Tuple2<K, V> entry) {
		return entry.getName();
	}

	public static <K, V> String path(Tuple2<K, V> entry) {
		return entry.getName();
	}

	public static <K, V> V val(Tuple2<K, V> entry) {
		return entry.value();
	}

	public static <V> V val(Entry<V> entry) throws ContextException {
		return entry.getData();
	}

	public static <V> String key(Entry<V> entry) {
		return entry.getName();
	}

	public static <V> String path(Entry<V> entry) {
		return entry.getName();
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

	public static OutType out(Type type) {
		return new OutType(type);
	}

	public static FilterId fId(String id) {
		return filterId(id);
	}

	public static FilterId filtId(String id) {
		return filterId(id);
	}

	public static FilterId filterId(String id) {
		return fId(id, null);
	}

	public static FilterId fId(String id, Object info) {
		return new FilterId(id, info);
	}

    public static String fi(Object object) {
	    if (object instanceof Function) {
	        return ((Function)object).fiName();
        } else {
            return object.toString();
        }
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
        entry.setValid(true);
        return entry;
    }

    public static Setup setValue(Setup entry, String contextPath, Object value) throws ContextException {
        entry.setEntry(contextPath, value);
        return entry;
    }

    public static Setup setValue(Setup entry, Value... entries) throws ContextException {
        for (Value e :  entries) {
            entry.setEntry(e.getName(), e.get());
        }
        return entry;
    }

    public static Object value(DataTable table, int row, int column) {
		return table.getValueAt(row, column);
	}

	public static <T> T v(Context<T> context, String path,
							  Arg... args) throws ContextException {
		return value(context, path, args);
	}

	public static <T> T value(Context<T> context, String path,
							  Arg... args) throws ContextException {
		try {
			T out = null;
			Object obj = context.get(path);
			if (obj != null) {
				if (obj instanceof Number || obj instanceof Number
						|| obj.getClass().isArray() || obj instanceof Collection) {
					out = (T) obj;
				} else if (obj instanceof Value) {
					out = (T) ((Value) obj).getData();
				} else if (obj instanceof Proc) {
                    out = (T) ((Proc) obj).evaluate(args);
                } else if (SdbUtil.isSosURL(obj)) {
					out = (T) ((URL) obj).getContent();
				} else if (((ServiceContext) context).getType().equals(Functionality.Type.MADO)) {
					out = (T) ((ServiceContext) context).getEvalValue(path);
				} else if (obj instanceof Srv && ((Srv) obj).asis() instanceof EntryCollable) {
					Entry entry = ((EntryCollable) ((Srv) obj).asis()).call((Model) context);
					out = (T) entry.asis();
				} else {
					// linked contexts and other special case of ServiceContext
					out = context.getValue(path, args);
				}
			} else {
				// linked contexts and other special case of ServiceContext
				out = context.getValue(path, args);
			}
			if (context instanceof Model && ((ModelStrategy)context.getMogramStrategy()).getOutcome() != null) {
				((ModelStrategy)context.getMogramStrategy()).getOutcome().putValue(path, out);
			}
			return out;
		} catch (MogramException | IOException e) {
			throw new ContextException(e);
		}
	}

	public static Object eval(Domain domain, String path, Arg... args) throws ContextException {
		return value((Context)domain, path, args);
	}

	public static <T> T value(Valuation<T> valuation) throws ContextException {
		return valuation.valuate();
	}

	public static <T> T value(Context<T> context, Arg... args)
			throws ContextException {
		try {
			synchronized (context) {
				return (T) ((ServiceContext)context).getValue(args);
			}
		} catch (Exception e) {
			throw new ContextException(e);
		}
	}

	public static Object value(Context context, String domain, String path) throws ContextException {
		if (((ServiceContext)context).getType().equals(Functionality.Type.MADO)) {
			return ((ServiceContext)context.getDomain(domain)).getEvalValue(path);
		} else {
			try {
				return context.getDomain(domain).getValue(path);
			} catch (RemoteException e) {
				throw new ContextException(e);
			}
		}
	}

	public static Object value(Arg[] args, String path) throws EvaluationException, RemoteException {
		for (Arg arg : args) {
			if (arg instanceof sorcer.service.Callable && arg.getName().equals(path))
				return ((sorcer.service.Callable)arg).call(args);
		}
		return null;
	}

	public static <T extends Object> ListContext<T> listContext(T... elems)
			throws ContextException {
		ListContext<T> lc = new ListContext<T>();
		for (int i = 0; i < elems.length; i++) {
			lc.add(elems[i]);
		}
		return lc;
	}

    public static Map<Object, Object> dictionary(Tuple2... entries) {
        Map<Object, Object> map = new HashMap<Object, Object>();
        for (Tuple2 entry : entries) {
            map.put(entry.getName(), entry.value());
        }
        return map;
    }

	public static Map<Object, Object> dictionary(Entry... entries) {
		Map<Object, Object> map = new HashMap<Object, Object>();
		for (Entry entry : entries) {
			map.put(entry.getName(), entry.getImpl());
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
			map.put(entry.key(), entry.value());
		}
		return map;
	}

	public static <K, V> Map<K, V> map(Association<K, V>... entries) throws ContextException {
		Map<K, V> map = new HashMap<K, V>();
		for (Association<K, V> entry : entries) {
			map.put(entry.getKey(), entry.getData());
		}
		return map;
	}

	public static Object rasis(Entry entry)
			throws ContextException {
		String path = entry.getName();
		Object o = asis(entry);
		while (o instanceof Function && ((Entry)o).getKey().equals(path)) {
			o = asis((Function)o);;
		}
		return o;
	}

	public static Object get(Function entry) throws ContextException {
		return rasis(entry);
	}

	public static Object impl(Entry entry)
			throws ContextException {
		return entry.getImpl();
	}

	public static Object impl(Model context, String path)
			throws ContextException {
		return impl((ServiceContext) context,  path);
	}

	public static Object rimpl(Context context, String path)
			throws ContextException {
		Object obj = context.get(path);
		if (obj instanceof Entry) {
			return ((Entry)context.get(path)).getImpl();
		} else {
			return null;
		}
	}

	public static Object impl(Context context, String path)
			throws ContextException {
		Object obj = context.get(path);
		if (obj instanceof Association) {
			return ((Entry)obj).getImpl();
		} else {
			return obj;
		}
	}

	public static Object asis(Entry entry)
			throws ContextException {
		return entry.asis();
	}

	public static Object asis(Function entry)
			throws ContextException {
		return entry.asis();
	}

	public static Object rasis(Context context, String path)
			throws ContextException {
		Object o = context.asis(path);
		if (o instanceof Function)
			return rasis((Function)o);
		else
			return o;
	}

	public static Object asis(Context context, String path)
			throws ContextException {
		return context.get(path);
	}

	public static Object asis(Model model, String path)
			throws ContextException {
		return  model.asis(path);
	}

    public static Object asis(Mappable mappable, String path)
            throws ContextException {
        return  mappable.asis(path);
    }

    public static Copier copier(Domain fromContext, Arg[] fromEntries,
                                Domain toContext, Arg[] toEntries) throws EvaluationException {
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

    public static Map<String, List<ExecDependency>> dependencies(Domain model) {
         return ((ServiceContext)model).getMogramStrategy().getDependentPaths();
    }

	public static Dependency dependsOn(Dependency dependee,  Evaluation... dependers) throws ContextException {
        String path = null;
		for (Evaluation d : dependers) {
            path = ((Identifiable)d).getName();
            if (path != null && path.equals("self")) {
                ((Entry)d).setKey(((Domain) dependee).getName());
            }
            if (d instanceof ExecDependency && ((ExecDependency)d).getType().equals(Type.CONDITION)) {
                ((ExecDependency)d).getCondition().setConditionalContext((Context)dependee);
                }
			if (!dependee.getDependers().contains(d)) {
                dependee.getDependers().add(d);
            }
		}
		if (dependee instanceof Domain && dependers.length > 0 && dependers[0] instanceof ExecDependency) {
			Map<String, List<ExecDependency>> dm = ((ModelStrategy)((Domain) dependee).getMogramStrategy()).getDependentPaths();
			for (Evaluation e : dependers) {
				path = ((Identifiable)e).getName();
				if (dm.get(path) != null) {
                    if (!dm.get(path).contains(e)) {
                        ((List) dm.get(path)).add(e);
                    }
				} else {
					List<ExecDependency> del = new ArrayList();
					del.add((ExecDependency)e);
					dm.put(path, del);
				}
			}
		}
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

	public static <T> T build(ServiceMogram mogram) throws SignatureException {
		return mogram.getInstance();
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

	public static Domain model(Signature signature) throws SignatureException {
		Object model = instance(signature);
		if (!(model instanceof Domain)) {
			throw new SignatureException("Signature does not specify te Domain: " + signature);
		}
		if (model instanceof Model) {
			((Model)model).setBuilder(signature);
		}
		return (Domain) model;
	}

	public static Mogram instance(Mogram mogram, Arg... args) throws SignatureException, MogramException {
		Signature builder = mogram.getBuilder(args);
		if (builder == null) {
			throw new SignatureException("No signature builder for: " + mogram.getName());
		}
		Mogram mog = (Mogram) sorcer.co.operator.instance(builder);
		mog.setBuilder(builder);
		Tag name = (Arg.getName(args));
		if (name != null)
			mog.setName(name.getName());
		return mog;
	}

	public static Tag tag(Object object) {
		return new Tag(object.toString());
	}

	public static String name(Object identifiable) {
		if (identifiable instanceof Identifiable) {
			return ((Identifiable) identifiable).getName();
		} else if (identifiable instanceof Arg) {
			return ((Arg) identifiable).getName();
		} else {
			return identifiable.toString();
		}
	}

	public static URL url(String urlName) throws MalformedURLException {
		return new URL(urlName);
	}

}