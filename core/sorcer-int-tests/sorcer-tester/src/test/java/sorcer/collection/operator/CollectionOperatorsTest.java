package sorcer.collection.operator;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ListContext;
import sorcer.eo.operator;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;
import sorcer.util.DataTable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.po.operator.ent;
import static sorcer.so.operator.eval;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked" })
public class CollectionOperatorsTest {
	private final static Logger logger = LoggerFactory.getLogger(CollectionOperatorsTest.class);
	
	@Test
	public void arrayOperatorTest() throws EvaluationException {
        Double[] da = array(1.1, 2.1, 3.1);
        assertArrayEquals(da, new Double[] { 1.1, 2.1, 3.1 } );

        Object[] oa = array(array(1.1, 2.1, 3.1),  4.1,  array(11.1, 12.1, 13.1));
        assertArrayEquals((Double[])oa[0], array(1.1, 2.1, 3.1));
        assertEquals(oa[1], 4.1);
        assertArrayEquals((Double[])oa[2], array(11.1, 12.1, 13.1));
	}

	@Test
	public void listOperatorTest() throws EvaluationException {
		List<Object> o_list = list(list(1.1, 2.1, 3.1),  4.1,  list(11.1, 12.1, 13.1));
		
		List<Double> d_list = (List<Double>)o_list.get(0);
		assertEquals(d_list, Arrays.asList(array(1.1, 2.1, 3.1)));
		
		assertEquals(o_list.get(0), list(1.1, 2.1, 3.1));
		assertEquals(o_list.get(1), 4.1);
		assertEquals(o_list.get(2), list(11.1, 12.1, 13.1));
	}

	@Test
	public void mapOperatorTest() throws Exception {
		Map<Object, Object> map1 = dictionary(ent("name", "Mike"), ent("height", 174.0));
				
		Map<String, Double> map2 = map(ent("length", 248.0), ent("width", 2.0), ent("height", 17.0));
		
		// keys and values of args
		String k = key(val("name", "Mike"));
		Double v = eval(val("height", 174.0));
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
		assertEquals(map1.keySet(), bag("name", "height"));
		// check map values
		assertTrue(map1.values().contains("Mike"));
		assertTrue(map1.values().contains(174.0));
	}
	
	@Test
	public void bagOperatorTest() throws EvaluationException {
		// the bag operator creates instances of java.util.Set
		Set<Object> set = bag("name", "Mike", "name", "Ray", ent("height", 174));
		assertEquals(set.size(), 4);
		assertEquals(ent("height", 174), ent("height", 174));
		assertTrue(set.contains(ent("height", 174)));
	}

	@Test
	public void tableOperatorTest() throws Exception {
		DataTable table = dataTable(
				list(1.1, 1.2, 1.3, 1.4, 1.5),
				list(2.1, 2.2, 2.3, 2.4, 2.5),
				list(3.1, 3.2, 3.3, 3.4, 3.5));
		
		table.setColumnIdentifiers(list("x1", "x2", "x3", "x4", "x5"));
		table.setRowIdentifiers(list("f1", "f2", "f3"));
		//logger.info("dataTable: " + dataTable);
		assertEquals(table.getRowCount(), 3);
		assertEquals(table.getColumnCount(), 5);
		
		assertEquals(table.getRowNames(), list("f1", "f2", "f3"));
		assertEquals(table.getColumnNames(), list("x1", "x2", "x3", "x4", "x5"));
		assertEquals(table.getRowMap("f2"), map(ent("x1", 2.1), ent("x2", 2.2), ent("x3", 2.3), ent("x4", 2.4), ent("x5",2.5)));
	}
	
	@Test
	public void listContextOperatorTest() throws ContextException {
		ListContext<Double> context = listContext(1.1, 1.2, 1.3, 1.4, 1.5);
		//logger.info(" index 1: " + context.get(1));
		assertEquals(context.get(1), 1.2);
		context.putValue(1, 5.0);
		assertEquals(context.get(1), 5.0);
		//logger.info("context path 1: " + context.pathFor(1));
		assertEquals(context.pathFor(1), "element[1]");
		//logger.info("list context: " + context);
		//logger.info("elements: " + context.getElements());
		//context.putValue("element[1]", 10.0);
		assertEquals(context.values(), list(1.1, 5.0, 1.3, 1.4, 1.5));
		context.set(1, 20.0);
		assertEquals(20.0, context.get(1));
		assertEquals(context.add(30.0), true);
		assertEquals(context.get(5), 30.0);
	}
	
	
//	@SuppressWarnings("rawtypes")
//	@Test
//	public void entriesTest() throws ContextException {
//		Tuple2 e2 = proc("x1", 10.0);
//		//logger.info("tuple e2: " + e2);
//		assertEquals("x1", e2.key());
//		assertEquals(10.0, e2.eval());
//		
//		Tuple3 e3a = proc("x1", 10.0, efFi("evaluator", "filter"));
//		//logger.info("tuple e3a: " + e3a);
//		assertEquals(efFi("evaluator", "filter").getEvaluatorName(), ((VarFidelityInfo)e3a.fidelity()).getEvaluatorName());
//		
//		FidelityEntry e3b = proc("x1", efFi("evaluator", "filter"));
//		//logger.info("tuple e3b: " + e3b);
//		assertEquals(efFi("evaluator", "filter").getFilterName(), ((VarFidelityInfo)e3b.fidelity()).getFilterName());
//		
//		StrategyEntry se = proc("j1/j2", strategy(Access.PULL, Flow.PAR));
//		//logger.info("tuple se: " + se);
//		assertEquals(se.strategy().getFlowType(), Flow.PAR);
//		assertEquals(se.strategy().getAccessType(), Access.PULL);
//
//	}
}
