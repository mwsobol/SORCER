package sorcer.sml.collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.co.tuple.*;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.util.Table;

import java.io.Serializable;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.co.operator.path;
import static sorcer.co.operator.persistent;
import static sorcer.co.operator.put;
import static sorcer.co.operator.set;
import static sorcer.co.operator.value;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.add;
import static sorcer.eo.operator.asis;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.put;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.add;
import static sorcer.po.operator.asis;
import static sorcer.po.operator.*;
import static sorcer.po.operator.set;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class CollectionOperators {
	private final static Logger logger = Logger.getLogger(CollectionOperators.class.getName());


	@Test
		public void tuplesOfTypedObjects() throws Exception {


		Tuple1 t1 = tuple("Mike");

		Entry ent = ent("Mike", "Sobolewski");

		Tuple2<String, String> t2 = tuple("Mike", "Sobolewski");

		Tuple3 t3 = tuple("Mike", "Sobolewski", "SORCER");

		Tuple4<String, String, String, Integer> t4 = tuple("Mike", "Sobolewski", "SORCER", 2014);

		Tuple5 t5 = tuple("Mike", "Sobolewski", "SORCER", 2014, "AFRL/WPAFB");

		Tuple6 t6 = tuple("Mike", "Sobolewski", "SORCER", 2010, "TTU", "AFRL/WPAFB");


		assertTrue(ent instanceof Tuple2);

		// no casting required
		String last = t4._2;
		int year = t4._4;

		// casting required
		String first = (String)t5._1;

		assertEquals(t1._1, t2._1);
		assertEquals(t2._2, t3._2);
		assertEquals(t3._3, t4._3);
		assertEquals(t4._4, t5._4);
		assertEquals(t5._5, t6._6);

		// for shot you can use the x operator for tupele

		Tuple2<Integer, Double> z = x(12, 12.0);
		int z1 = z._1;
		double z2 = z._2;
		assertTrue((z1 * z2) == 144.0);

	}


	@Test
	public void genericArrayOperator() throws Exception {
		
		Double[] da = array(1.1, 2.1, 3.1);
		assertArrayEquals(da, new Double[] { 1.1, 2.1, 3.1 } );
		
		Object[] oa = array(array(1.1, 2.1, 3.1),  4.1,  array(11.1, 12.1, 13.1));		
		assertArrayEquals((Double[])oa[0], array(1.1, 2.1, 3.1));
		assertEquals(oa[1], 4.1);
		assertArrayEquals((Double[])oa[2], array(11.1, 12.1, 13.1));
		
	}

	
	@Test
	public void genericListOperator() throws Exception {
		
		// the list operator creates an instance of ArrayList
		List<Object> l = list(list(1.1, 2.1, 3.1),  4.1,  list(11.1, 12.1, 13.1));
		
		List<Double> l0 = (List<Double>)l.get(0);
		assertEquals(l0, list(array(1.1, 2.1, 3.1)));
			
		assertEquals(l.get(0), list(1.1, 2.1, 3.1));
		assertEquals(l.get(1), 4.1);
		assertEquals(l.get(2), list(11.1, 12.1, 13.1));
		
		assertTrue(Arrays.equals(array(list(1.1, 2.1, 3.1)), array(1.1, 2.1, 3.1)));
		
	}
	
	
	@Test
	public void genericSetOperator() throws Exception {
		
		// the set operator creates instances of java.util.Set
		Set<Serializable> s = set("name", "Mike", "name", "Ray", tuple("height", 174));
		assertEquals(s.size(), 4);
		assertEquals(tuple("height", 174)._1, "height");
		assertEquals((int)tuple("height", 174)._2, 174);

		assertTrue(s.contains(tuple("height", 174)));
		
	}
	
	
	@Test
	public void tableOperator() throws Exception {
		
		Table t = table(
				row(1.1, 1.2, 1.3, 1.4, 1.5),
				row(2.1, 2.2, 2.3, 2.4, 2.5),
				row(3.1, 3.2, 3.3, 3.4, 3.5));
		
		columnNames(t, list("x1", "x2", "x3", "x4", "x5"));
		rowNames(t, list("f1", "f2", "f3"));
		//logger.info("table: " + table);
		assertEquals(rowSize(t), 3);
		assertEquals(columnSize(t), 5);
		
		assertEquals(rowNames(t), list("f1", "f2", "f3"));
		assertEquals(columnNames(t), list("x1", "x2", "x3", "x4", "x5"));
		assertEquals(rowMap(t, "f2"), map(ent("x1", 2.1), ent("x2", 2.2), 
				ent("x3", 2.3), ent("x4", 2.4), ent("x5",2.5)));
		assertEquals(value(t, "f2", "x2"), 2.2);
		assertEquals(value(t, 1, 1), 2.2);
		
	}


	@Test
	public void entryOperator() throws Exception {
		
		Entry<Double> e = ent("arg/x1", 10.0);
		assertEquals("arg/x1", key(e));
		// a path is a String - usually a sequence of attributes
		assertEquals("arg/x1", path(e));

		assertFalse(isPersistent(e));
		assertTrue(asis(e) instanceof Double);
		assertTrue(value(e).equals(10.0));
		assertTrue(asis(e).equals(10.0));

		// make the entry persistent
		// value is not yet persisted
		persistent(e);
		assertTrue(isPersistent(e));
		assertFalse(asis(e) instanceof URL);
		assertTrue(value(e).equals(10.0));
		assertTrue(asis(e) instanceof URL);
		put(e, 50.0);
		assertTrue(value(e).equals(50.0));
		assertTrue(asis(e) instanceof URL);

		// create service strategy entry
		Entry se1 = strategyEnt("j1/j2",
				strategy(Strategy.Access.PULL, Strategy.Flow.PAR));
		assertEquals(flow(se1), Strategy.Flow.PAR);
		assertEquals(access(se1), Strategy.Access.PULL);

		// store the argument of entry (parameter)
		URL se1Url = storeArg(se1);
		Strategy st1 = (Strategy)content(se1Url);
		assertTrue(isPersistent(se1));
		assertTrue(asis(se1) instanceof URL);
		assertTrue(flow(se1).equals(flow(st1)));
		assertTrue(access(se1).equals(access(st1)));

		// store an object
		URL se2Url = store(value(se1));
		Strategy st2 = (Strategy)content(se1Url);
		assertTrue(flow(se1).equals(flow(st2)));
		assertTrue(access(se1).equals(access(st2)));
		
	}
	
	
	@Test
	public void dbEntryOperator() throws Exception {
		
		// create a persistent entry
		Entry<Double> de = dbEnt("x3", 110.0);
		assertFalse(asis(de) instanceof URL);
		assertTrue(value(de).equals(110.0));
		assertTrue(asis(de) instanceof URL);

		// create an entry
		Entry<Double> e = ent("x1", 10.0);
		assertTrue(value(e).equals(10.0));
		assertTrue(asis(e).equals(10.0));
		assertFalse(asis(e) instanceof URL);
				
		// make a persistent entry
		// 'storeArg' operator makes the entry value persisted
		URL valUrl = storeArg(e);
		assertTrue(value(e).equals(10.0));
		assertTrue(asis(e) instanceof URL);
		
		// create a persistent entry with URL
		Entry<?> urle = dbEnt("x2", valUrl);
		assertTrue(value(urle).equals(10.0));
		assertTrue(asis(urle) instanceof URL);
		
		// assign a given URL
		Entry<Object> dbe = dbEnt("y1");
		put(dbe, valUrl);
		assertTrue(value(dbe).equals(10.0));
		assertTrue(asis(dbe) instanceof URL);
		
	}
	
	
	@Test
	public void parOperator() throws Exception {
		
		Par add = par("add", invoker("x + y", pars("x", "y")));
		Context<Double> cxt = context(ent("x", 10.0), ent("y", 20.0));
		logger.info("par value: " + value(add, cxt));
		assertTrue(value(add, cxt).equals(30.0));

		cxt = context(ent("x", 20.0), ent("y", 30.0));
		add = par(cxt, "add", invoker("x + y", pars("x", "y")));
		logger.info("par value: " + value(add));
		assertTrue(value(add).equals(50.0));

	}
	
	
	@Test
	public void dbParOperator() throws Exception {

		// persist values (arguments) of pars
		Par<Double> dbp1 = persistent(par("design/in", 25.0));
		Par<String> dbp2 = dbPar("url/sobol", "http://sorcersoft.org/sobol");

		assertFalse(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);
		
		assertTrue(value(dbp1).equals(25.0));
		assertEquals(value(dbp2), "http://sorcersoft.org/sobol");
		
		assertTrue(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);

		// store pars, not their arguments) in the data store
		URL p1Url = store(par("design/in", 30.0));
		URL p2Url = store(par("url/sorcer", "http://sorcersoft.org"));
		
		assertEquals(value((Par)content(p1Url)), 30.0);
		assertEquals(value((Par)content(p2Url)), "http://sorcersoft.org");

	}
	
	
	@Test
	public void mapOperator() throws Exception {
		
		Map<Object, Object> map1 = dictionary(ent("name", "Mike"), ent("height", 174.0));
				
		Map<String, Double> map2 = map(ent("length", 248.0), ent("screen/width", 27.0), ent("screen/height", 12.0));
		
		// keys and values of entries
		assertEquals(key(ent("name", "Mike")), "name");
		assertEquals(value(ent("name", "Mike")), "Mike");
		// when using namespaces use path for the name of context (map) variables
		assertEquals(path(ent("screen/height", 12.0)), "screen/height");

		assertEquals(keyValue(map1, "name"), "Mike");
		assertEquals(keyValue(map1, "height"), 174.0);

		assertTrue(key(ent("width", 2.0)).equals("width"));
		assertTrue(value(ent("width", 2.0)).equals(2.0));

		assertEquals(keyValue(map1, "name"), "Mike");
		assertEquals(keyValue(map1, "height"), 174.0);

		assertTrue(pathValue(map2, "screen/width").equals(27.0));
		assertTrue(pathValue(map2, "screen/height").equals(12.0));

		// Java API
		assertEquals(map1.keySet(), set("name", "height"));
		// check map values
		assertTrue(map1.values().contains("Mike"));
		assertTrue(map1.values().contains(174.0));
		
	}
	
	
	@Test
	public void contextOperator() throws Exception {
		
		Context cxt = context(
				ent("arg/x1", 1.1),
				ent("arg/x2", 1.2),
				ent("arg/x3", 1.3),
				ent("arg/x4", 1.4),
				ent("arg/x5", 1.5));

		add(cxt, ent("arg/x6", 1.6));
		add(cxt, ent("arg/x7", invoker("x1 + x3", ents("x1", "x3"))));

		assertTrue(value(cxt, "arg/x1").equals(1.1));
		assertTrue(get(cxt, "arg/x1").equals(1.1));
		assertTrue(asis(cxt, "arg/x1").equals(1.1));

		add(cxt, ent("arg/x1", 1.0));
		assertTrue(get(cxt, "arg/x1").equals(1.0));

		add(cxt, ent("arg/x3", 3.0));
		assertTrue(get(cxt, "arg/x3").equals(3.0));

		Context<Double> subcxt = context(cxt, list("arg/x4", "arg/x5"));
		logger.info("subcontext: " + subcxt);
		assertNull(get(subcxt, "arg/x1"));
		assertNull(get(subcxt, "arg/x2"));
		assertNull(get(subcxt, "arg/x3"));
		assertTrue(get(cxt, "arg/x4").equals(1.4));
		assertTrue(get(cxt, "arg/x5").equals(1.5));
		assertTrue(get(cxt, "arg/x6").equals(1.6));
		assertTrue((Object) get(cxt, "arg/x7") instanceof ServiceInvoker);

		// aliasing entries with reactive value entries - rvEnt
		put(cxt, rvEnt("arg/x6", ent("overwrite", 20.0)));
		assertTrue(value(cxt, "arg/x6").equals(20.0));
		urvEnt(cxt, "arg/x6");
		assertTrue(value((Evaluation)value(cxt, "arg/x6")).equals(20.0));
		rrvEnt(cxt, "arg/x6");
		assertTrue(value(cxt, "arg/x6").equals(20.0));

		// aliasing pars, pars are always reactive
		put(cxt, ent("arg/x6", par("overwrite", 40.0)));
		assertTrue(value(cxt, "arg/x6").equals(40.0));

		// repeatedly reactive evaluations
		assertTrue((Object) get(cxt, "arg/x7") instanceof ServiceInvoker);
		rrvEnt(cxt, "arg/x7");
		assertEquals(2.4, (Double) value(cxt, "arg/x7"), 0.0000001);

	}
	

	@Test
	public void contextModeling() throws Exception {
		
		Context<Double> cxt = entModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0), 
				 ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));
		
		add(cxt, ent("arg/x6", 6.0));
		assertTrue(value(cxt, "arg/x6").equals(6.0));
			
		put(cxt, ent("arg/x6", ent("overwrite", 20.0)));
		assertTrue(value(cxt, "arg/x6").equals(20.0));
		
		// model with invoker
		add(cxt, ent("arg/x7", invoker("x1 + x3", ents("x1", "x3"))));	
		assertTrue(value(cxt, "arg/x7").equals(4.0));
		
		// model with local service entry, own arguments
		add(cxt, ent("arg/x8", service(sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("result/y")))));
		assertTrue(value(cxt, "arg/x8").equals(100.0));
		
		// model with local service entry, no arguments
		add(cxt, ent("arg/x9", service(sig("multiply", MultiplierImpl.class),
			cxt("add", inEnt("arg/x1"), inEnt("arg/x2"), result("result/y")))));
		assertTrue(value(cxt, "arg/x9").equals(2.0));
	}
	
	
	@Test
	public void parModeling() throws Exception {
		
		ParModel pm = parModel("par-model", ent("John/weight", 180.0));
		add(pm, par("x", 10.0), ent("y", 20.0));
		add(pm, invoker("add", "x + y", pars("x", "y")));

//		logger.info("adder value: " + value(pm, "add"));
		assertEquals(value(pm, "John/weight"), 180.0);
		assertEquals(value(pm, "add"), 30.0);
		set(pm, "x", 20.0);
		assertEquals(value(pm, "add"), 40.0);
		
	}

	@Test
	public void serviceMogramming() throws RemoteException,
			ContextException, ExertionException, SignatureException {

		Service c4 = context("multiply", inEnt("arg/x1"), inEnt("arg/x2"),
				outEnt("result/y"));

		Service c5 = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				outEnt("result/y"));

		Service t3 = srv("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1", null), inEnt("arg/x2"),
						outEnt("result/y")));

		Service t4 = srv("t4", sig("multiply", MultiplierImpl.class), c4);

		Service t5 = srv("t5", sig("add", AdderImpl.class), c5);

		Service j1 = srv("j1", sig("service", ServiceJobber.class),
				srv("j2", t4, t5, sig("service", ServiceJobber.class)),
				t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));


		// context and exertion parameters
		Par x1p = par("x1p", "arg/x1", c4);
		Par x2p = par("x2p", "arg/x2", c4);
		Par j1p = par("j1p", "j1/t3/result/y", j1);

		// par model with contexts and exertion
		ParModel pc = parModel(x1p, x2p, j1p);

		// setting context arguments
		set(x1p, 10.0);
		set(x2p, 50.0);

		// update par references
		Service j2 = exert(j1);
		Service c4s = taskContext("j1/t4", j2);

		// get service j2 direct result value
		assertEquals(get(j2, "j1/t3/result/y"), 400.0);
		// get service par j1p value
		assertEquals(value(j1p), 400.0);

		// set job parameter value
		set(j1p, 1000.0);
		assertEquals(value(j1p), 1000.0);

		// execute original service and get its par value
		exert(j1);
		// j1p is the alias to context value of j1 at j1/t3/result/y
		assertEquals(value(pc, "j1p"), 400.0);

	}

}
