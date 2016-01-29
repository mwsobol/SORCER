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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class tests_s13353 {
    private final static Logger logger = LoggerFactory.getLogger(CoffeeMakerTest.class);

    private CoffeeMaker coffeeMaker;
    private Inventory inventory;
    private Recipe Recipe;

    @Before
    public void setUp() throws ContextException {
        coffeeMaker = new CoffeeMaker();
        inventory = coffeeMaker.checkInventory();


        edu.pjatk.inn.coffeemaker.impl.Recipe mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(60);
        mocha.setAmtCoffee(3);
        mocha.setAmtMilk(2);
        mocha.setAmtSugar(2);
        mocha.setAmtChocolate(3);

        edu.pjatk.inn.coffeemaker.impl.Recipe latte = new Recipe();
        latte.setName("Latte");
        latte.setPrice(60);
        latte.setAmtCoffee(3);
        latte.setAmtMilk(3);
        latte.setAmtSugar(2);
        latte.setAmtChocolate(0);

        edu.pjatk.inn.coffeemaker.impl.Recipe hotChocolate = new Recipe();
        hotChocolate.setName("Hot Chocolate");
        hotChocolate.setPrice(60);
        hotChocolate.setAmtCoffee(0);
        hotChocolate.setAmtMilk(2);
        hotChocolate.setAmtSugar(2);
        hotChocolate.setAmtChocolate(3);

        edu.pjatk.inn.coffeemaker.impl.Recipe coffee = new Recipe();
        coffee.setName("Coffee");
        coffee.setPrice(50);
        coffee.setAmtCoffee(3);
        coffee.setAmtMilk(1);
        coffee.setAmtSugar(1);
        coffee.setAmtChocolate(0);

    }

    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: 5
//	Milk: 3
//	Sugar: 7
//	Chocolate: 2
    public void addInventory1() throws Exception {
        assertTrue (coffeeMaker.addInventory(5,3,7,2));
    }

    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: -1
    public void addInventory2 () throws Exception {
        assertFalse(coffeeMaker.addInventory(-1,0,0,0));
    }

    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: 5
//	Milk: -1
    public void  addInventory3 () throws Exception {
        assertFalse(coffeeMaker.addInventory(5,-1,0,0));
    }

    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: 5
//	Milk: 3
//	Sugar: -1
    public void addInventory4 () throws  Exception {
        assertFalse(coffeeMaker.addInventory(5,3,-1,0));  ////BUG should be amtSugar < 0 and cast to (int)  --- 169 line
    }

    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: 5
//	Milk: 3
//	Sugar: 7
//	Chocolate: -1
    public void addInventory5 () throws Exception {
        assertFalse(coffeeMaker.addInventory(5,3,7,-1));
    }

/*    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: a
    public void addInventory6 () throws Exception {
        assertFalse(coffeeMaker.addInventory(Integer.parseInt("a"),0,0,0));
    }*/

/*    @Test
//	Precondition: Run CoffeeMaker
//	Enter: 4
//	Coffee: 5
//	Milk: a
    public void addInventory7 () throws Exception {
        assertFalse(coffeeMaker.addInventory(5,Integer.parseInt("a"),0,0));
    }*/
}

