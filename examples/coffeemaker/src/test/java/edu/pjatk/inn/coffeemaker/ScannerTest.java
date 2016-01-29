package edu.pjatk.inn.coffeemaker;

import edu.pjatk.inn.coffeemaker.impl.CoffeeMaker;
import edu.pjatk.inn.coffeemaker.impl.Inventory;
import edu.pjatk.inn.coffeemaker.impl.Recipe;
import edu.pjatk.inn.coffeemaker.impl.ScannerImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.service.*;

import static org.junit.Assert.*;
import static sorcer.co.operator.ent;
import static sorcer.co.operator.inEnt;
import static sorcer.eo.operator.*;

@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class ScannerTest {
    private final static Logger logger = LoggerFactory.getLogger(ScannerTest.class);

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
    public void addCode() throws Exception {
        Exertion cmt = task(sig("scan", ScannerImpl.class),
                context("scanner", inEnt("code", "c2,m1,s1,ch0,p32,Cappuccino")));


        logger.info("Context: " + context(exert(cmt)) + "\n" + get(context(exert(cmt)), ScannerImpl.RESULT_STATUS));
    }

    @Test
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
    public void scanTest() throws Exception {
        String v1 = "c1,m1,s2,ch0,p123,latte";
        int v2 = 200;

        Context cxt = context("scanner", inEnt("code", v1));
        Task scanner = task("hello scanner", sig("scan", ScannerImpl.class), cxt);

        Task coffee = task("coffee", sig("makeCoffee", CoffeeMaker.class), context(
                ent("recipe/name", "latte"),
                ent("coffee/paid", 200),
                ent("coffee/change"),
                ent("recipe")));
        if ((Boolean) exert(scanner).getValue("result/status")) {
            Job drinkCoffee = job(scanner, coffee,
                    pipe(outPoint(scanner, "result/value"), inPoint(coffee, "recipe")));
        }else{
            get(exert(scanner),ScannerImpl.RESULT_PATH);
        }
    }
}