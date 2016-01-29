package edu.pjatk.inn.coffeemaker;

import edu.pjatk.inn.coffeemaker.impl.CoffeeMaker;
import edu.pjatk.inn.coffeemaker.impl.Inventory;
import edu.pjatk.inn.coffeemaker.impl.ScannerImpl;
import edu.pjatk.inn.coffeemaker.impl.Recipe;
import edu.pjatk.inn.requestor.CoffeemakerRequestor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.provider.Jobber;
import sorcer.service.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;

@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class AddRecipeTest_s13261 {
    private final static Logger logger = LoggerFactory.getLogger(AddRecipeTest_s13261.class);

    private CoffeeMaker coffeeMaker;
    private ScannerImpl qr;
    private Inventory inventory;
    private Recipe mocha, latte, coffee, hotChoco;

    @Before
    public void setUp() throws ContextException {
        coffeeMaker = new CoffeeMaker();
        inventory = coffeeMaker.checkInventory();

        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(60);
        mocha.setAmtCoffee(3);
        mocha.setAmtMilk(2);
        mocha.setAmtSugar(2);
        mocha.setAmtChocolate(3);

        latte = new Recipe();
        latte.setName("Latte");
        latte.setPrice(60);
        latte.setAmtCoffee(3);
        latte.setAmtMilk(3);
        latte.setAmtSugar(2);
        latte.setAmtChocolate(0);

        hotChoco = new Recipe();
        hotChoco.setName("Hot Chocolate");
        hotChoco.setPrice(60);
        hotChoco.setAmtCoffee(0);
        hotChoco.setAmtMilk(2);
        hotChoco.setAmtSugar(2);
        hotChoco.setAmtChocolate(3);

        coffee = new Recipe();
        coffee.setName("Coffee");
        coffee.setPrice(50);
        coffee.setAmtCoffee(3);
        coffee.setAmtMilk(1);
        coffee.setAmtSugar(1);
        coffee.setAmtChocolate(0);
    }

    @Test
    //Coffee successfully added.
    public void addRecipe1() {
        coffeeMaker.addRecipe(coffee);
        assertEquals(coffeeMaker.getRecipeForName("Coffee").getName(), "Coffee");
    }

    @Test
    //Coffee could not be added.
    public void addRecipe2() {
        coffeeMaker.addRecipe(coffee);
        assertFalse(coffeeMaker.addRecipe(coffee));
    }

    @Test
    //Mocha could not be added. Price can not be negative.
    public void addRecipe3() {
        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(-50);
        assertFalse(coffeeMaker.addRecipe(mocha));
    }

    @Test
    //Mocha could not be added. Units of coffee can not be negative.
    public void addRecipe4() {
        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(60);
        mocha.setAmtCoffee(-3);
        assertFalse(coffeeMaker.addRecipe(mocha));
    }

    @Test
    //Mocha could not be added. Units of milk can not be negative.
    public void addRecipe5() {
        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(60);
        mocha.setAmtCoffee(3);
        mocha.setAmtMilk(-2);
        assertFalse(coffeeMaker.addRecipe(mocha));
    }

    @Test
    //Mocha could not be added. Units of sugar can not be negative.
    public void addRecipe6() {
        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(60);
        mocha.setAmtCoffee(3);
        mocha.setAmtMilk(2);
        mocha.setAmtSugar(-2);
        assertFalse(coffeeMaker.addRecipe(mocha));
    }

    @Test
    //Mocha could not be added. Units of chocolate can not be negative.
    public void addRecipe7() {
        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(60);
        mocha.setAmtCoffee(3);
        mocha.setAmtMilk(2);
        mocha.setAmtSugar(2);
        mocha.setAmtChocolate(-3);
        assertFalse(coffeeMaker.addRecipe(mocha));
    }


    @Test
    //Please input an integer.
    public void addRecipe8() {
        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(Integer.getInteger("a"));
        assertFalse(coffeeMaker.addRecipe(mocha));
    }

    @Test
    //Please input an integer.
    public void addRecipe9() {
        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(60);
        mocha.setAmtCoffee(Integer.getInteger("a"));
        assertFalse(coffeeMaker.addRecipe(mocha));
    }

    @Test
    //Please input an integer.
    public void addRecipe10() {
        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(60);
        mocha.setAmtCoffee(3);
        mocha.setAmtMilk(Integer.getInteger("a"));
        assertFalse(coffeeMaker.addRecipe(mocha));
    }

    @Test
    //Please input an integer.
    public void addRecipe11() {
        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(60);
        mocha.setAmtCoffee(3);
        mocha.setAmtMilk(2);
        mocha.setAmtSugar(Integer.getInteger("a"));
        assertFalse(coffeeMaker.addRecipe(mocha));
    }

    @Test
    //Please input an integer.
    public void addRecipe12() {
        mocha = new Recipe();
        mocha.setName("Mocha");
        mocha.setPrice(60);
        mocha.setAmtCoffee(3);
        mocha.setAmtMilk(2);
        mocha.setAmtSugar(2);
        mocha.setAmtChocolate(Integer.getInteger("a"));
        assertFalse(coffeeMaker.addRecipe(mocha));
    }

    @Test
    //Coffee successfully added.
    public void addRecipe13() {
        assertTrue(coffeeMaker.addRecipe(mocha));
    }

    @Test
    //Coffee successfully added.
    public void addRecipe14() {
        assertTrue(coffeeMaker.addRecipe(mocha));
        assertTrue(coffeeMaker.addRecipe(latte));
    }

    @Test
    //Coffee successfully added.
    public void addRecipe15() {
        assertTrue(coffeeMaker.addRecipe(mocha));
        assertTrue(coffeeMaker.addRecipe(latte));
        assertTrue(coffeeMaker.addRecipe(hotChoco));
    }

    @Test
    //Successfully deleted
    public void deleteRecipe1() {
        coffeeMaker.addRecipe(coffee);
        assertTrue(coffeeMaker.deleteRecipe(coffee));
    }

    @Test
    //There are no recipes to delete
    public void deleteRecipe2() {
        assertFalse(coffeeMaker.deleteRecipe(coffee));
    }


    @Test
    //There are no recipes to delete
    public void addQR() throws Exception {
        Exertion cmt = task(sig("scan", ScannerImpl.class),
                context("scanner", inEnt("code", "c2,m1,s1,ch0,p32,Cappuccino")));


        logger.info("Context: " + context(exert(cmt)) + "\n" + get(context(exert(cmt)), ScannerImpl.RESULT_STATUS));
    }

    @Test
    //There are no recipes to delete
    public void chekIf() throws Exception {
        Exertion cmt = task(sig("scan", ScannerImpl.class),
                context("scanner", inEnt("code", "c1")));

        if (get(context(exert(cmt)), ScannerImpl.RESULT_STATUS).equals(true)) {
            logger.info("checkIf", "cooool");
            logger.info("checkIf", get(context(cmt), ScannerImpl.RESULT_PATH));

        } else {
            logger.info("checkIf", "Baaadd");
            logger.info("checkIf", get(context(cmt), ScannerImpl.RESULT_PATH));
        }
    }

    @Test
    public void test() throws Exception {
        String v1 = "c1,m1,s2,ch0,p123,latte";
        int v2 = 200;

        Context cxt = context("scanner", inEnt("code", v1));
        Task scanner = task("hello scanner", sig("scan", ScannerImpl.class), cxt);

        Task coffee = task("coffee", sig("makeCoffee", CoffeeMaker.class), context(
                ent("recipe/name", "latte"),
                ent("coffee/paid", 200),
                ent("coffee/change"),
                ent("recipe")));

        Job drinkCoffee = job(scanner, coffee,
                pipe(outPoint(scanner, "result/value"), inPoint(coffee, "recipe")));

    }
}