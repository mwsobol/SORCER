package sorcer.fidelities;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.plexus.FiMap;
import sorcer.core.service.Projection;
import sorcer.eo.operator;
import sorcer.service.EvaluationException;
import sorcer.service.Fidelity;
import sorcer.service.FidelityList;
import sorcer.service.ServiceFidelity;
import sorcer.util.ModelTable;
import sorcer.util.Table;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.expr;


/**
 * Created by Mike Sobolewski on 6/7/16.
 */
public class FidelityTest {

	private static Logger logger = LoggerFactory.getLogger(FidelityTest.class);

    @Test
    public void projectionOfToString() {

        Projection fl1 = po(fi("x1", "atX"));
        logger.info("as String: " + fl1);
        assertEquals(fl1.toString(), "fis(fi(\"x1\", \"atX\"))");

        Projection fl2 = po(fi("x1", "atX"), fi("y2", "atY"));
        logger.info("as String: " + fl2);
        assertEquals(fl2.toString(), "fis(fi(\"x1\", \"atX\"), fi(\"y2\", \"atY\"))");

    }

    @Test
    public void fisToString() {

        FidelityList fl1 = fis(fi("x1", "atX"));
        logger.info("as String fl1: " + fl1);
        assertEquals(fl1.toString(), "fis(fi(\"x1\", \"atX\"))");

        FidelityList fl2 = fis(fi("x1", "atX"), fi("y2", "atY"));
        logger.info("as String fl2: " + fl2);
		logger.info("as String fl3: " + "fis(fi(\"x1\", \"atX\"), fi(\"y2\", \"atY\"))");

		assertEquals(fl2.toString(), "fis(fi(\"x1\", \"atX\"), fi(\"y2\", \"atY\"))");

    }

	@Test
	public void fiMap() {

		FiMap fm = new FiMap();
		fm.add(fiEnt(2, fis(fi("atX", "x1"))));
		fm.add(fiEnt(5, fis(fi("atX", "x1"), fi("atY", "y2"))));
		logger.info("fi map: " + fm);

		assertEquals(fm.get(2), fiList(fi("atX", "x1")));
		assertEquals(fm.get(5), fiList(fi("atX", "x1"), fi("atY", "y2")));
	}

	@Test
	public void fidelityTable() {
		Table dataTable = table(header("span"),
				row(110.0),
				row(120.0),
				row(130.0),
				row(140.0),
				row(150.0),
				row(160.0));

		ModelTable fiTable = appendFidelities(dataTable,
				fiEnt(1, fis(fi("atX", "x1"))),
				fiEnt(3, fis(fi("atX", "x1"), fi("atY", "y2"))));

		logger.info("fi table: " + fiTable);
        FiMap fiMap = new FiMap(dataTable);
        fiMap.populateFidelities(dataTable.getRowCount()-1);
        logger.info("fi map populated: " + fiMap);
        assertEquals(fiMap.get(0), null);
        assertEquals(fiMap.get(1), fiList(fi("atX", "x1")));
        assertEquals(fiMap.get(2), fiList(fi("atX", "x1")));
        assertEquals(fiMap.get(4), fiList(fi("atX", "x1"), fi("atY", "y2")));
        assertEquals(fiMap.get(5), fiList(fi("atX", "x1"), fi("atY", "y2")));
    }

    @Test
    public void populateFidelityTable() {
        Table dataTable = table(header("span"),
                row(110.0),
                row(120.0),
                row(130.0),
                row(140.0),
                row(150.0),
                row(160.0));

        ModelTable fiTable = populateFidelities(dataTable,
                fiEnt(1, fis(fi("atX", "x1"))),
                fiEnt(3, fis(fi("atX", "x1"), fi("atY", "y2"))));

        logger.info("fi table: " + fiTable);
        FiMap fiMap = new FiMap(dataTable);
        logger.info("fi map: " + fiMap);
        assertEquals(fiMap.get(0), null);
        assertEquals(fiMap.get(1), fiList(fi("atX", "x1")));
        assertEquals(fiMap.get(2), fiList(fi("atX", "x1")));
        assertEquals(fiMap.get(4), fiList(fi("atX", "x1"), fi("atY", "y2")));
        assertEquals(fiMap.get(5), fiList(fi("atX", "x1"), fi("atY", "y2")));
    }

    @Test
	public void selectFiMap() {
		Table dataTable = table(header("span", "fis"),
				row(110.0,  fiList(fi("tip/displacement", "astros"))),
				row(120.0),
				row(130.0,  fiList(fi("tip/displacement", "nastran"))),
				row(140.0));


		FiMap fiMap = new FiMap(dataTable);
		logger.info("fi map: " + fiMap);
		assertEquals(fiMap.size(), 4);
		assertEquals(fiMap.get(1), null);
		assertEquals(fiMap.get(3), null);

		fiMap.populateFidelities(dataTable.getRowCount()-1);
		logger.info("fi map populated: " + fiMap);
		assertEquals(fiMap.get(1), fiList(fi("tip/displacement", "astros")));
		assertEquals(fiMap.get(3), fiList(fi("tip/displacement", "nastran")));
	}

	@Test
	public void getFiList() throws EvaluationException {
		String fis = "fis(fi('tip/displacement', 'astros'))";

		FidelityList fl = (FidelityList) eval(expr(fis));
		logger.info("fi map populated: " + fl);
		assertTrue(fl.equals(fis(fi("tip/displacement", "astros"))));
	}

	@Test
	public void getProjection() throws EvaluationException {
		String fis = "po(fi('astros', 'tip/displacement'))";

		Projection fl = (Projection) eval(expr(fis));
		logger.info("fi map populated: " + fl);
   		assertTrue(fl.equals(po(fi("astros", "tip/displacement"))));
	}

	@Test
	public void projectionTests() throws Exception {
		ServiceFidelity<Fidelity> sFi1 = fi("job1", fi("net", "j1/j2"),
				fi("object2", "j1/t3"), fi("object2", "j1/j2/t4"), fi("object2", "j1/j2/t5"));

		ServiceFidelity<Fidelity> sFi2 = fi("job2", fi("net", "j1/j2"),
				fi("object2", "j1/t3"), fi("object2", "j1/j2/t4"), fi("object2", "j1/j2/t5"), sFi1);

		ServiceFidelity<Fidelity> sFi3 = fi("job3", fi("net", "j1/j2"),
				fi("object2", "j1/t3"), fi("object2", "j1/j2/t4"), fi("object2", "j1/j2/t5"), sFi1, sFi2);

		Projection p1 = po(sFi1);
		logger.info("projection: " + p1);

		Projection p2 = po(sFi2);
		logger.info("projection: " + p2);

		Projection p3 = po(sFi3);
		logger.info("projection: " + p3);

		List<Fidelity> job1Fis1 = p1.getFidelities("job1");
		logger.info("job1Fis1: " + job1Fis1);

		List<Fidelity> job1Fis2 = p2.getFidelities("job2");
		logger.info("job1Fis2: " + job1Fis2);
		assertTrue(job1Fis1.equals(job1Fis2));

		List<Fidelity> job1Fis3 = p3.getFidelities("job3");
		logger.info("job1Fis3: " + job1Fis3);
		assertTrue(job1Fis1.equals(job1Fis3));

		List<Fidelity> job2Fis2 = p2.getFidelities("job2");
		logger.info("job2Fis2: " + job2Fis2);

		List<Fidelity> job2Fis3 = p3.getFidelities("job2");
		logger.info("job2Fis3: " + job2Fis3);
		assertTrue(job2Fis2.equals(job2Fis3));
	}

}
