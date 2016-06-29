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

import groovy.lang.Closure;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import sorcer.co.tuple.*;
import sorcer.core.context.Copier;
import sorcer.core.context.ListContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.plexus.FiEntry;
import sorcer.core.plexus.MorphFidelity;
import sorcer.core.plexus.MultiFiRequest;
import sorcer.core.provider.DatabaseStorer;
import sorcer.core.signature.NetletSignature;
import sorcer.core.signature.ObjectSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.netlet.ScriptExerter;
import sorcer.service.*;
import sorcer.service.Signature.ReturnPath;
import sorcer.service.modeling.Model;
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

	public static Path file(String filename) {
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

	public static <T1, T2, T3> Tuple3<T1, T2, T3> triplet(T1 x1, T2 x2, T3 x3) {
		return new Tuple3<T1, T2, T3>(x1, x2, x3);
	}

    public static Entry ent(Model model, String path) throws ContextException {
        return new Entry(path, model.asis(path));
    }

	public static <T extends Arg> Srv ent(String name, MorphFidelity<T> fidelity) {
		fidelity.setPath(name);
		fidelity.getFidelity().setPath(name);
		return srv(name, fidelity);
	}

	public static Srv ent(String name, ServiceFidelity<Signature> fidelity) {
		return srv(name, fidelity);
	}

	public static Srv ent(ServiceFidelity<Signature> fidelity) {
		return srv(fidelity);
	}

	public static Srv srv(ServiceFidelity<Signature> fidelity) {
		Srv service = new Srv(fidelity.getName(), fidelity);
		return service;
	}

	public static <T extends Arg> Srv srv(String name, ServiceFidelity<T> fidelity) {
		Srv service = new Srv(name, fidelity);
		return service;
	}

	public static Srv srv(String name, MorphFidelity<Signature> fidelity) {
		Srv service = new Srv(name, fidelity);
		return service;
	}

    public static Srv srv(String name, Identifiable item) {
        return srv(name,  item,  null);
    }

    public static Srv srv(Identifiable item, Context context) {
        return srv(null,  item,  context);
    }

	public static Srv srv(String name, Identifiable item, Context context, Arg... args) {
		String srvName = item.getName();
		Srv srv = null;
		if (name != null)
			srvName = name;

		if (item instanceof Signature) {
			srv = new Srv(srvName,
					new SignatureEntry(item.getName(), (Signature) item, context));
		} else if (item instanceof Mogram) {
			srv = new Srv(srvName,
					new MogramEntry(item.getName(), (Mogram) item));
		} else {
			srv = new Srv(srvName, item);
		}
		try {
			srv.substitute(args);
		} catch (SetterException e) {
			e.printStackTrace();
		}
		return srv;
	}

    public static Srv srv(Identifiable item) {
		return srv(null, item);
	}

	public static Srv srv(String name, String path, Model model) {
		return new Srv(path, model, name);
	}

	public static Srv srv(String name, String path, Model model, Type type) {
		return new Srv(path, model, name, type);
	}

	public static Srv aka(String name, String path) {
		return new Srv(path, null, name);
	}

	public static Srv alias(String name, String path) {
		return new Srv(path, null, name);
	}

	public static <T> Entry<T> ent(String path, T value) {
		if (value instanceof Invocation) {
			return new Par<T>(path, value);
		} else if (value instanceof Evaluation) {
			return new Entry<T>(path, value);
		} else if (value instanceof ServiceFidelity) {
			return (Entry<T>) new Srv(path, value);
		} else if (value instanceof MultiFiRequest) {
			try {
				((MultiFiRequest)value).setUnifiedName(path);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return (Entry<T>) new Srv(path, value);
		} else if (value instanceof List && ((List)value).get(0) instanceof Path) {
			return (Entry<T>) new DependencyEntry(path, (List)value);
		} else {
			return new Entry<T>(path, value);
		}
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

	public static Srv lambda(String path, Service service, sorcer.eo.operator.Args args) {
		return new Srv(path, path, service, args.argsToStrings());
	}

	public static Srv lambda(String path, Service service,  String name, sorcer.eo.operator.Args args) {
		return new Srv(name, path, service,  args.argsToStrings());
	}

	public static Srv lambda(String path, String name, Client client) {
		return new Srv(name, path, client);
	}

	public static <T> Srv lambda(String path, Callable<T> call) {
		return new Srv(path, call);
	}

	public static <T> Srv lambda(String path, ValueCallable<T> call) {
		return new Srv(path, call);
	}
	public static <T> Srv lambda(String path, ValueCallable<T> call, sorcer.eo.operator.Args args) {
		return new Srv(path, call, args.argsToStrings());
	}

	public static <T> Srv lambda(String path, ValueCallable<T> lambda, Context context) throws InvocationException {
		return new Srv(path, invoker(lambda, context));
	}

	public static <T> Srv lambda(String path, ValueCallable<T> lambda, sorcer.eo.operator.Args args, Context context)
			throws InvocationException {
		return new Srv(path, invoker(lambda, context), args.argsToStrings());
	}

	public static <T> Srv lambda(String path, EntryCollable<T> call) {
		return new Srv(path, call);
	}

	public static <T> Srv lambda(String path, ValueCallable<T> call, ReturnPath returnPath) {
		return new Srv(path, call, returnPath);
	}

	public static boolean isSorcerLambda(Class clazz) {
		Class[] types = { EntryCollable.class, ValueCallable.class, Client.class,
				ConditionCollable.class, Callable.class };
		for (Class cl : types) {
			if (clazz == cl) {
				return true;
			}
		}
		return false;
	}

	public static Srv ent(String path, Closure call) {
		return new Srv(path, call);
	}

	public static Srv cxtEnt(String path, EntryCollable call) {
		return new Srv(path, call);
	}

	public static Srv xrtEnt(String path, ContextCallable call) {
		return new Srv(path, call);
	}

	public static Srv ent(ServiceInvoker invoker) {
		return new Srv(invoker.getName(), invoker);
	}

	public static Par ent(String path, Invocation invoker) {
		return new Par(path, invoker);
	}

    public static Srv ent(Signature sig, Context context) {
        return srv(sig, context);
    }

    public static Srv ent(String name, Signature sig, Context context, Arg... args) {
        return srv(name, sig, context, args);
    }

	public static Srv ent(Signature sig) {
		return srv(sig);
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


	public static Entry<Object>  ent(String path) {
		return new Entry<Object>(path, null);
	}

	public static <T> Entry<T> put(Entry<T> entry, T value)
			throws SetterException, RemoteException {
		entry.setValue(value);
		return entry;
	}

	public static <T> OutputEntry<T> outEnt(String path, T value) {
		if (value instanceof String && ((String)value).indexOf('|') > 0) {
			OutputEntry oe =  outEnt(path, null);
			oe.annotation(value);
			return oe;
		}
		OutputEntry ent = new OutputEntry(path, value, 0);
		if (value instanceof Class)
			ent.setValClass((Class) value);
		return ent;
	}

	public static <T> OutputEntry<T> outEnt(String path, T value, String annotation) {
		OutputEntry oe =  outEnt(path, value);
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

	public static <T> OutputEntry<T> outEnt(String path, T value, int index) {
		return new OutputEntry(path, value, index);
	}

	public static <T> OutputEntry<T> dbOutEnt(String path, T value) {
		return new OutputEntry(path, value, true, 0);
	}

	public static InputEntry input(String path) {
		return new InputEntry(path, null, 0);
	}

	public static OutputEntry outEnt(String path) {
		return new OutputEntry(path, null, 0);
	}

	public static InputEntry inEnt(String path) {
		return new InputEntry(path, null, 0);
	}

	public static Entry at(String path, Object value) {
		return new Entry(path, value, 0);
	}

	public static Entry at(String path, Object value, int index) {
		return new Entry(path, value, index);
	}

	public static <T> InputEntry<T> inEnt(String path, T value) {
		return new InputEntry(path, value, 0);
	}

	public static <T> InputEntry<T> dbInEnt(String path, T value, String annotation) {
		InputEntry<T> ie = new InputEntry(path, value, true, 0);
		ie.annotation(annotation);
		return ie;
	}

	public static <T> InputEntry<T> dbInEnt(String path, T value) {
		return new InputEntry(path, value, true, 0);
	}

	public static <T> InputEntry<T> inEnt(String path, T value, int index) {
		return new InputEntry(path, value, index);
	}

	public static <T> InputEntry<T> inEnt(String path, T value, String annotation) {
		InputEntry<T> ie = inEnt(path, value);
		ie.annotation(annotation);
		return ie;
	}

	public static <T> InputEntry<T> inEnt(String path, T value, Class valClass, String annotation) {
		InputEntry<T> ie = new InputEntry(path, value, 0);
		if (valClass != null)
			ie.setValClass(valClass);
		ie.annotation(annotation);
		return ie;
	}

	public static <T> InputEntry<T> inEnt(String path, T value, Class valClass) {
		return inEnt(path, value, valClass, null);
	}

	public static InputEntry inoutEnt(String path) {
		return new InputEntry(path, null, 0);
	}

	public static <T> InoutEntry<T> inoutEnt(String path, T value) {
		return new InoutEntry(path, value, 0);
	}

	public static <T> InoutEntry<T> inoutEnt(String path, T value, int index) {
		return new InoutEntry(path, value, index);
	}

	public static <T> InoutEntry<T> inoutEnt(String path, T value, String annotation) {
		InoutEntry<T> ie = inoutEnt(path, value);
		ie.annotation(annotation);
		return ie;
	}

	public static <T> TagEntry<T> ent(String path, T value, String association) {
		return new TagEntry(path, value, association);
	}

	public static Entry setValue(Entry entry, Object value)
			throws ContextException {
		try {
			entry.setValue(value);
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		return entry;
	}

	public static Context setValue(Context context, String path, Object value) throws ContextException {
		context.putValue(path, value);
		return context;
	}

	public static <S extends Setter> boolean isDB(S setter) {
		return isPersistent(setter);
	}

	public static <S extends Setter> boolean isDb(S setter) {
		return isPersistent(setter);
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
						if (object instanceof Par) {
							// its value is now persisted
							((Par)object)._2 = null;
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
		if (object instanceof Par)
			((Par) object).setDbURL(dbUrl);
		else if (object instanceof ServiceContext)
			((ServiceContext) object).setDbUrl("" + dbUrl);
		else
			throw new MalformedURLException("Can not set URL to: " + object);
	}

	public static URL dbURL(Object object) throws MalformedURLException {
		if (object instanceof Par)
			return ((Par) object).getDbURL();
		else if (object instanceof ServiceContext)
			return new URL(((ServiceContext) object).getDbUrl());
		return null;
	}

	public static Object retrieve(URL url) throws IOException {
		return url.getContent();
	}

	public static URL update(Object object) throws MogramException,
			SignatureException, ContextException {
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

	public static int size(Model context) {
		return ((ServiceContext)context).size();
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

	public static <T> Entry<T> dbEnt(String path) {
		Entry<T> e = new Par<T>(path);
		e.setPersistent(true);
		return e;
	}

	public static <T> Entry<T> dbEnt(String path, T value) throws EvaluationException {
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

	public static Arg[] ents(String... entries)
			throws ContextException {
		ArgSet as = new ArgSet();
		for (String name : entries) {
			as.add(new Entry(name, Context.none));
		}
		return as.toArray();
	}

	public static Arg[] ents(Entry... entries)
			throws ContextException {
		ArgSet as = new ArgSet();
		for (Entry e : entries) {
			as.add(e);
		}
		return as.toArray();
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

	public static Table fiColumnName(Table table, String name) {
		table.setFiColumnName(name);
		return table;
	}

	public static ModelTable populateFidelities(ModelTable table, FiEntry... entries) {
		Table impl = (Table)table;
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
		Table impl = (Table)table;
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
		if (entry instanceof Par) {
			if (entry._2 != null && entry.isValid())
				return entry._2;
			else
				return entry.value();
		}
		else
			return entry._2;
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

    public static Copier copier(Context fromContext, Arg[] fromEntries,
                                Context toContext, Arg[] toEntries) throws EvaluationException {
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

//    public static List<String> paths(String... paths) {
//        List<String> list = new ArrayList<>();
//        Collections.addAll(list, paths);
//        return list;
//    }

	public static List<String> paths(Context context) throws ContextException {
		return context.getPaths();
	}

	public static void remove(ServiceContext parModel, String... paths)
			throws RemoteException, ContextException {
		for (String path : paths)
			parModel.getData().remove(path);
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
					ScriptExerter se = new ScriptExerter(System.out, null, Sorcer.getWebsterUrl(), true);
					se.readFile(new File(source));
					return se.parse();
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

	public static URL url(String urlName) throws MalformedURLException {
		return new URL(urlName);
	}
}