package sorcer.sml.collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.co.tuple.*;
import sorcer.core.Tag;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.Function;
import sorcer.core.context.model.ent.Prc;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.ent;
import sorcer.service.modeling.func;
import sorcer.service.modeling.val;
import sorcer.util.Runner;
import sorcer.util.DataTable;

import java.io.Serializable;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.co.operator.asis;
import static sorcer.co.operator.map;
import static sorcer.co.operator.path;
import static sorcer.co.operator.persistent;
import static sorcer.co.operator.set;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.print;
import static sorcer.eo.operator.put;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class CollectionOperators {
	private final static Logger logger = LoggerFactory.getLogger(CollectionOperators.class);


	@Test
	public void slotsAndTuples() throws Exception {

		Slot s1 = slot("Mike");

		Slot s2 = slot("Mike", "Sobolewski");

		Tuple2 t2 = x("Mike", "Sobolewski");

		Tuple3 t3 = x("Mike", "Sobolewski", "SORCER");

		Tuple4<String, String, String, Integer> t4 = x("Mike", "Sobolewski", "SORCER", 2014);

		Tuple5 t5 = x("Mike", "Sobolewski", "SORCER", 2014, "AFRL/WPAFB");

		Tuple6 t6 = x("Mike", "Sobolewski", "SORCER", 2010, "TTU", "AFRL/WPAFB");

		assertTrue(s1 instanceof Slot);
		assertTrue(t2 instanceof Tuple2);
		assertTrue(t3 instanceof Tuple3);

		// no casting required
		String last = t4._2;
		int year = t4._4;

		// casting required
		String first = (String)t5._1;

		assertEquals(s1.key(), "Mike");
		assertEquals(s2.getValue(), "Sobolewski");

		assertEquals(t2._1, t2._1);
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
	public void valuesAndSubroutines() throws Exception {

		val v1 = val("x", 30.0);
		assertEquals(get(v1), 30.0);

		func p2 = prc("x", 20.0);
		assertEquals(exec(p2), 20.0);

		ent p1 = prc("x", 10.0);
		assertEquals(exec(p1), 10.0);

		Date td = new Date();
		ent d1 = prc("x", td);
		assertEquals(exec(d1), td);

	}


	@Test
	public void genericArrayOperator() throws Exception {

		Double[] da = array(1.1, 2.1, 3.1);
		assertArrayEquals(da, new Double[] { 1.1, 2.1, 3.1 } );

		Object[] oa = objects(array(1.1, 2.1, 3.1),  4.1,  array(11.1, 12.1, 13.1));
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
		Set<Serializable> s = set("key", "Mike", "key", "Ray", x("height", 174));
		assertEquals(s.size(), 4);
		assertEquals(x("height", 174)._1, "height");
		assertEquals((int) x("height", 174)._2, 174);

		assertTrue(s.contains(x("height", 174)));

	}

	@Test
	public void tableOperator() throws Exception {

		DataTable t = dataTable(
				list(1.1, 1.2, 1.3, 1.4, 1.5),
				list(2.1, 2.2, 2.3, 2.4, 2.5),
				list(3.1, 3.2, 3.3, 3.4, 3.5));

		columnNames(t, list("x1", "x2", "x3", "x4", "x5"));
		rowNames(t, list("f1", "f2", "f3"));
		//logger.info("dataTable: " + dataTable);
		assertEquals(rowSize(t), 3);
		assertEquals(columnSize(t), 5);

		assertEquals(rowNames(t), list("f1", "f2", "f3"));
		assertEquals(columnNames(t), list("x1", "x2", "x3", "x4", "x5"));
		assertEquals(rowMap(t, "f2"), map(x("x1", 2.1), x("x2", 2.2),
				x("x3", 2.3), x("x4", 2.4), x("x5",2.5)));
		assertEquals(get(t, "f2", "x2"), 2.2);
		assertEquals(get(t, 1, 1), 2.2);

	}

	@Test
	public void dbValAndStoreValOperators() throws Exception {

		// create a persistent entry
		Entry<Double> de = dbVal("x3", 110.0);
		assertFalse(impl(de) instanceof URL);
		assertTrue(exec(de).equals(110.0));
		assertTrue(impl(de) instanceof URL);

		// create an entry
		Entry<Double> e = ent("x1", 10.0);
		assertTrue(exec(e).equals(10.0));
		assertTrue(asis(e).equals(10.0));
		assertFalse(asis(e) instanceof URL);

		// store the valuate of entry
		URL valUrl = storeVal(e);
		assertTrue(exec(e).equals(10.0));
		assertTrue(impl(e) instanceof URL);

		// create a persistent entry with URL
		Entry urle = dbVal("x2", valUrl);
		assertTrue(exec(urle).equals(10.0));
		assertTrue(impl(urle) instanceof URL);

		// assign a given URL
		Entry<Object> dbe = dbVal("y1", 1.0);
		setImpl(dbe, valUrl);
		assertTrue(exec(dbe).equals(10.0));
		assertTrue(impl(dbe) instanceof URL);

	}

	@Test
	public void entOperator() throws Exception {

		Entry add = ent("add", invoker("x + y", args("x", "y")));
		Context<Double> cxt = context(ent("x", 10.0), ent("y", 20.0));
//		logger.info("eval: " + eval(add, cxt));
		assertTrue(exec(add, cxt).equals(30.0));

		cxt = context(ent("x", 20.0), ent("y", 30.0));
		add = ent("add", invoker("x + y", args("x", "y")), cxt);
//		logger.info("prc eval: " + eval(add));
		assertTrue(exec(add).equals(50.0));

	}

	@Test
	public void procValEntOperator() throws Exception {

		Prc add = prc("add", invoker("x + y", args("x", "y")));
		Context<Double> cxt = context(val("x", 10.0), val("y", 20.0));
		logger.info("eval: " + exec(add, cxt));
		assertTrue(exec(add, cxt).equals(30.0));

		cxt = context(ent("x", 20.0), ent("y", 30.0));
		add = prc("add", invoker("x + y", args("x", "y")), cxt);
		logger.info("prc eval: " + exec(add));
		assertTrue(exec(add).equals(50.0));

	}

	@Test
	public void persistentOperator() throws Exception {

		// persist values of args
        Function dbp2 = prc("url/sobol", "http://sorcersoft.org/sobol");
		persistent(dbp2);

		assertFalse(asis(dbp2) instanceof URL);
		assertEquals(exec(dbp2), "http://sorcersoft.org/sobol");
		assertTrue(impl(dbp2) instanceof URL);

		// store args, not their arguments) in the data store
		URL p1Url = store(val("design/in", 30.0));
		URL p2Url = store(val("url/sorcer", "http://sorcersoft.org"));

		assertEquals(exec((Entry)content(p1Url)), 30.0);
		assertEquals(exec((Entry)content(p2Url)), "http://sorcersoft.org");

	}

	@Test
	public void dbEntOperator() throws Exception {

		Entry<Double> e = dbEnt("arg/x1", 10.0);
		assertEquals("arg/x1", key(e));
		// a path is a String - usually a sequence of attributes
		assertEquals("arg/x1", path(e));

		assertTrue(exec(e).equals(10.0));
		assertTrue(isPersistent(e));
		assertTrue(exec(e).equals(10.0));
		assertTrue(asis(e) instanceof URL);
		setValue(e, 50.0);
		assertTrue(exec(e).equals(50.0));
		assertTrue(asis(e) instanceof URL);

		// create service strategy entry
		Entry se1 = strategyEnt("j1/j2",
			strategy(Strategy.Access.PULL, Strategy.Flow.PAR));
		assertEquals(flow(se1), Strategy.Flow.PAR);
		assertEquals(access(se1), Strategy.Access.PULL);

		// store the valuate of the entry (parameter)
		URL se1Url = storeVal(se1);
		Strategy st1 = (Strategy)content(se1Url);
		assertTrue(isPersistent(se1));
		assertTrue(impl(se1) instanceof URL);
		assertTrue(flow(se1).equals(flow(st1)));
		assertTrue(access(se1).equals(access(st1)));

		// store an object
		store(exec(se1));
		Strategy st2 = (Strategy)content(se1Url);
		assertTrue(flow(se1).equals(flow(st2)));
		assertTrue(access(se1).equals(access(st2)));

	}

	@Test
	public void mapOperator() throws Exception {

		Map<Object, Object> map1 = dictionary(x("key", "Mike"), x("height", 174.0));

		Map<String, Double> map2 = map(x("length", 248.0), x("screen/width", 27.0), x("screen/height", 12.0));

		// keys and values of args
		assertEquals(key(x("key", "Mike")), "key");
		assertEquals(val(x("key", "Mike")), "Mike");
		// when using namespaces use path for the key of context (map) variables
		assertEquals(path(x("screen/height", 12.0)), "screen/height");

		assertEquals(keyValue(map1, "key"), "Mike");
		assertEquals(keyValue(map1, "height"), 174.0);

		assertTrue(key(x("width", 2.0)).equals("width"));
		assertTrue(val(x("width", 2.0)).equals(2.0));

		assertEquals(keyValue(map1, "key"), "Mike");
		assertEquals(keyValue(map1, "height"), 174.0);

		assertTrue(pathValue(map2, "screen/width").equals(27.0));
		assertTrue(pathValue(map2, "screen/height").equals(12.0));

		// Java API
		assertEquals(map1.keySet(), set("key", "height"));
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
		assertTrue(asis(cxt, "arg/x7") instanceof Invocation);

		// reusing valuate entries
		put(cxt, val("arg/x6", val("overwrite", 40.0)));
		assertTrue(value(cxt, "arg/x6").equals(40.0));

		// eval of functional entries in DataContexts is not possible
		// use models (active contexts) created with the model operator
		assertTrue(asis(cxt, "arg/x7") instanceof Invocation);
		logger.info("x7a: " + value(cxt, "arg/x7"));
		logger.info("x7b: " + value(cxt, "arg/x7"));
		assertTrue(exec(cxt, "arg/x7").equals(4.0));
		assertTrue(value(cxt, "arg/x7").equals(4.0));

	}

	@Test
	public void procModeling() throws Exception {

		Model pm = model("prc-model", prc("John/weight", 180.0));
		add(pm, ent("x", 10.0), ent("y", 20.0));
		add(pm, invoker("add", "x + y", args("x", "y")));

		assertEquals(exec(pm, "John/weight"), 180.0);
		assertEquals(exec(pm, "add"), 30.0);
		setValue(pm, "x", 20.0);
		assertEquals(exec(pm, "add"), 40.0);

	}

	@Test
	public void runClosure() throws Exception {

		Runnable r = () -> {
			try {
				System.out.println("context: " + context(val("x", 10)));
			} catch (ContextException e) {
				e.printStackTrace();
			}
		};

		r.run();

		new Thread(r).start();
	}

	@Test
	public void callClosure() throws Exception {
		// invoke run using Lambda expression
		run(args -> System.out.println("Closing with: " + args[0].getName()),
				new Tag("Hello"));

		// invoke run using  Lambda object matched to interface
		Runner r = args -> {
			try {
				print(exert(context(val("x", 10)), args));
			} catch (MogramException e) {
				e.printStackTrace();
			}
		};

		r.exec(ent("x", "Hello"));

		run(r, ent("x", "Hello"));

	}
}
