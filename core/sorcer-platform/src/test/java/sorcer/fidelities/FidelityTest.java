package sorcer.fidelities;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.plexus.FiMap;
import sorcer.service.FidelityList;
import sorcer.util.Table;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;


/**
 * Created by Mike Sobolewski on 6/7/16.
 */
public class FidelityTest {

	private static Logger logger = LoggerFactory.getLogger(FidelityTest.class);

	@Test
	public void fisToString() {

		FidelityList fl1 = fis(fi("atX", "x1"));
		logger.info("as String: " + fl1);
		assertEquals(fl1.toString(), "fis(fi(\"atX\", \"x1\"))");

		FidelityList fl2 = fis(fi("atX", "x1"), fi("atY", "y2"));
		logger.info("as String: " + fl2);
		assertEquals(fl2.toString(), "fis(fi(\"atX\", \"x1\"), fi(\"atY\", \"y2\"))");

		FidelityList fl3 = fis(fi("atZ", "z2"), fl1, fl2);
		logger.info("as String: " + fl3);
		assertEquals(fl3.toString(), "fis(fi(\"atZ\", \"z2\"), fi(\"atX\", \"x1\"), fi(\"atX\", \"x1\"), fi(\"atY\", \"y2\"))");
	}

	@Test
	public void fiMap() {

		FiMap fm = new FiMap();
		fm.add(fiEnt(2, fis(fi("atX", "x1"))));
		fm.add(fiEnt(5, fis(fi("atX", "x1"), fi("atY", "y2"))));
		logger.info("fi map: " + fm);

		assertEquals(fm.get(2), fis(fi("atX", "x1")));
		assertEquals(fm.get(5), fis(fi("atX", "x1"), fi("atY", "y2")));
	}

	@Test
	public void populateMap() {
		FiMap fm = new FiMap();
		fm.add(fiEnt(2, fis(fi("atX", "x1"))));
		fm.add(fiEnt(5, fis(fi("atX", "x1"), fi("atY", "y2"))));
		fm.populateFidelities(10);
		assertEquals(fm.size(), 9);
		assertEquals(fm.get(10), fis(fi("atX", "x1"), fi("atY", "y2")));
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

		Table fiTable = appendFidelities(dataTable,
				fiEnt(1, fis(fi("atX", "x1"))),
				fiEnt(3, fis(fi("atX", "x1"), fi("atY", "y2"))));

		logger.info("fi table: " + fiTable);
        FiMap fiMap = new FiMap(dataTable);
        fiMap.populateFidelities(dataTable.getRowCount()-1);
        logger.info("fi map populated: " + fiMap);
        assertEquals(fiMap.get(0), null);
        assertEquals(fiMap.get(1), fis(fi("atX", "x1")));
        assertEquals(fiMap.get(2), fis(fi("atX", "x1")));
        assertEquals(fiMap.get(4), fis(fi("atX", "x1"), fi("atY", "y2")));
        assertEquals(fiMap.get(5), fis(fi("atX", "x1"), fi("atY", "y2")));
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

        Table fiTable = populateFidelities(dataTable,
                fiEnt(1, fis(fi("atX", "x1"))),
                fiEnt(3, fis(fi("atX", "x1"), fi("atY", "y2"))));

        logger.info("fi table: " + fiTable);
        FiMap fiMap = new FiMap(dataTable);
        logger.info("fi map: " + fiMap);
        assertEquals(fiMap.get(0), null);
        assertEquals(fiMap.get(1), fis(fi("atX", "x1")));
        assertEquals(fiMap.get(2), fis(fi("atX", "x1")));
        assertEquals(fiMap.get(4), fis(fi("atX", "x1"), fi("atY", "y2")));
        assertEquals(fiMap.get(5), fis(fi("atX", "x1"), fi("atY", "y2")));
    }

    @Test
	public void selectFiMap() {
		Table dataTable = table(header("span", "fis"),
				row(110.0,  fis(fi("tip/displacement", "astros"))),
				row(120.0),
				row(130.0,  fis(fi("tip/displacement", "nastran"))),
				row(140.0));


		FiMap fiMap = new FiMap(dataTable);
		logger.info("fi map: " + fiMap);
		assertEquals(fiMap.size(), 4);
		assertEquals(fiMap.get(1), null);
		assertEquals(fiMap.get(3), null);

		fiMap.populateFidelities(dataTable.getRowCount()-1);
		logger.info("fi map populated: " + fiMap);
		assertEquals(fiMap.get(1), fis(fi("tip/displacement", "astros")));
		assertEquals(fiMap.get(3), fis(fi("tip/displacement", "nastran")));
	}
}
