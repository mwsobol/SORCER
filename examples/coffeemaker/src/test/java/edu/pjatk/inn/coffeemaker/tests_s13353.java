package edu.pjatk.inn.coffeemaker;
import edu.pjatk.inn.coffeemaker.impl.CoffeeMaker;
import edu.pjatk.inn.coffeemaker.impl.Inventory;
import edu.pjatk.inn.coffeemaker.impl.Recipe;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.service.ContextException;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class tests_s13353 {
    private final static Logger logger = LoggerFactory.getLogger(CoffeeMakerTest.class);

    private CoffeeMaker coffeeMaker;
    private Inventory inventory;
    private Recipe espresso, mocha, macchiato, americano;

    @Before
    public void setUp() throws ContextException {
        coffeeMaker = new CoffeeMaker();
        inventory = coffeeMaker.checkInventory();
    }

    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: 5
//	Milk: 3
//	Sugar: 7
//	Chocolate: 2
    public void addInventory1() throws Exception {
        assertFalse(coffeeMaker.addInventory(5,3,7,2));
    }

    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: -1
    public void addInventory2 () throws Exception {
        coffeeMaker.addInventory(-1,0,0,0);
    }

    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: 5
//	Milk: -1
    public void  addInventory3 () throws Exception {
        coffeeMaker.addInventory(5,-1,0,0);
    }

    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: 5
//	Milk: 3
//	Sugar: -1
    public void addInventory4 () throws  Exception {
        assertFalse(coffeeMaker.addInventory(5,3,-1,0));
    }

    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: 5
//	Milk: 3
//	Sugar: 7
//	Chocolate: -1
    public void addInventory5 () throws Exception {
        coffeeMaker.addInventory(5,3,7,-1);
    }

/*    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: a
    public void addInventory6 () throws Exception {
        coffeeMaker.addInventory(Integer.parseInt("a"),0,0,0);
    }*/

/*    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: 5
//	Milk: a
    public void addInventory7 () throws Exception {
        coffeeMaker.addInventory(5,Integer.parseInt("a"),0,0);
    }*/
}

