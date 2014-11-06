package sorcer.eol.collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.co.tuple.Entry;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.invoker.GroovyInvoker;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.Context;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.util.Table;

import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.co.operator.list;
import static sorcer.co.operator.path;
import static sorcer.co.operator.persistent;
import static sorcer.co.operator.put;
import static sorcer.co.operator.set;
import static sorcer.co.operator.store;
import static sorcer.co.operator.url;
import static sorcer.co.operator.value;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.add;
import static sorcer.eo.operator.asis;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.put;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.add;
import static sorcer.po.operator.*;
import static sorcer.po.operator.set;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/eol")
public class CollectionOperators {
	private final static Logger logger = Logger.getLogger(CollectionOperators.class.getName());

	
	@Test
	public void arrayOperator() throws Exception {
		
		Double[] da = array(1.1, 2.1, 3.1);
		assertArrayEquals(da, new Double[] { 1.1, 2.1, 3.1 } );
		
		Object[] oa = array(array(1.1, 2.1, 3.1),  4.1,  array(11.1, 12.1, 13.1));		
		assertArrayEquals((Double[])oa[0], array(1.1, 2.1, 3.1));
		assertEquals(oa[1], 4.1);
		assertArrayEquals((Double[])oa[2], array(11.1, 12.1, 13.1));
		
	}

	
	@Test
	public void listOperator() throws Exception {
		
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
	public void setOperator() throws Exception {
		
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

		assertEquals(isPersistent(e), false);
		assertTrue(asis(e) instanceof Double);
		assertTrue(value(e).equals(10.0));
		assertTrue(asis(e).equals(10.0));
		
		// make the entry persistent
		// value is not yet persisted
		persistent(e);
		
		assertEquals(isPersistent(e), true);
		assertFalse(asis(e) instanceof URL);
		assertTrue(value(e).equals(10.0));
		assertTrue(asis(e) instanceof URL);
		
		put(e, 50.0);
		assertTrue(value(e).equals(50.0));
		assertTrue(asis(e) instanceof URL);
		
		Entry se = strategyEnt("j1/j2", strategy(Access.PULL, Flow.PAR));
		assertEquals(flow(se), Flow.PAR);
		assertEquals(access(se), Access.PULL);
		
		// store value of the entry
		e = ent("arg/x1", 100.0);
		store(e);
		assertEquals(isPersistent(e), true);
		assertTrue(asis(e) instanceof URL);
		assertTrue(value(e).equals(100.0));
		
	}
	
	
	@Test
	public void dbEntryOperator() throws Exception {
		
		// create an entry
		Entry<Double> e = ent("x1", 10.0);
		assertTrue(value(e).equals(10.0));
		assertTrue(asis(e).equals(10.0));
		
		// make it a persistent entry
		// 'url' operator makes the entry persistent		
		URL valUrl = url(e);
		assertTrue(value(e).equals(10.0));
		assertTrue(asis(e) instanceof URL);
		
		// create a persistent entry
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
		Par<Double> dbp1 = persistent(par("design/in", 25.0));
		Par<String> dbp2 = dbPar("url/sobol", "http://sorcersoft.org/sobol");

		assertFalse(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);
		
		assertTrue(value(dbp1).equals(25.0));
		assertEquals(value(dbp2), "http://sorcersoft.org/sobol");
		
		assertTrue(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);

		// store par args in the data store
		Par p1 = store(par("design/in", 30.0));
		Par p2 = store(par("url/sorcer", "http://sorcersoft.org"));
		
		assertEquals(value(url(p1)), 30.0);
		assertEquals(value(url(p2)), "http://sorcersoft.org");
	}
	
	
	@Test
	public void mapOperator() throws Exception {
		
		Map<Object, Object> map1 = dictionary(ent("name", "Mike"), ent("height", 174.0));
				
		Map<String, Double> map2 = map(ent("length", 248.0), ent("width", 2.0), ent("height", 17.0));
		
		// keys and values of entries
		String k = key(ent("name", "Mike"));
		
		Entry<Double> de = ent("height", 174.0);
		Double v = value(de);

//		Double v = value(entry("height", 174.0));
		assertEquals(k, "name");
		assertTrue(v.equals(174.0));
		
		// casts are needed for dictionary: Map<Object, Object>
		k = (String)map1.get("name");
		v = (Double)map1.get("height");
		assertEquals(k, "Mike");
		assertTrue(v.equals(174.0));
		
		// casts are NOT needed for map: Map<K, V>
		v = map2.get("length");
		assertTrue(v.equals(248.0));
		
		// check map keys
		assertEquals(map1.keySet(), set("name", "height"));
		// check map values
		assertTrue(map1.values().contains("Mike"));
		assertTrue(map1.values().contains(174.0));
		
	}
	
	
	@Test
	public void contextOperator() throws Exception {
		
		Context<Double> cxt = context(ent("arg/x1", 1.1), ent("arg/x2", 1.2), 
				 ent("arg/x3", 1.3), ent("arg/x4", 1.4), ent("arg/x5", 1.5));
		
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
		assertTrue(((Object)get(cxt, "arg/x7")) instanceof ServiceInvoker);
		
		// aliasing entries
		put(cxt, ent("arg/x6", ent("overwrite", 20.0)));
		assertTrue(value(cxt, "arg/x6").equals(20.0));
		
		// aliasing pars
		put(cxt, ent("arg/x6", par("overwrite", 40.0)));
		assertTrue(value(cxt, "arg/x6").equals(40.0));

		// but no direct evaluations
		Object obj = value(cxt, "arg/x7");
		logger.info("obj: " + obj);
		assertTrue(obj.getClass() == GroovyInvoker.class);

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
	
}
