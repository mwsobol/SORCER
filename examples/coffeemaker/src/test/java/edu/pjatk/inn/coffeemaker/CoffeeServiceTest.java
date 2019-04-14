package edu.pjatk.inn.coffeemaker;

import edu.pjatk.inn.coffeemaker.impl.CoffeeMaker;
import edu.pjatk.inn.coffeemaker.impl.DeliveryImpl;
import edu.pjatk.inn.coffeemaker.impl.Recipe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.Domain;

import static edu.pjatk.inn.coffeemaker.impl.Recipe.getRecipe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.inVal;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.result;
import static sorcer.mo.operator.*;
import static sorcer.mo.operator.result;
import static sorcer.ent.operator.ent;
import static sorcer.ent.operator.ents;
import static sorcer.ent.operator.invoker;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class CoffeeServiceTest {
	private final static Logger logger = LoggerFactory.getLogger(CoffeeServiceTest.class);

	private Context espresso, mocha, macchiato, americano;
	private Recipe recipe;

	@Before
	public void setUp() throws ContextException {
		recipe = new Recipe();
		recipe.setName("espresso");
		recipe.setPrice(50);
		recipe.setAmtCoffee(6);
		recipe.setAmtMilk(1);
		recipe.setAmtSugar(1);
		recipe.setAmtChocolate(0);

		espresso = context(ent("key", "espresso"), ent("price", 50),
				ent("amtCoffee", 6), ent("amtMilk", 0),
				ent("amtSugar", 1), ent("amtChocolate", 0));

		mocha  = context(ent("key", "mocha"), ent("price", 100),
				ent("amtCoffee", 8), ent("amtMilk", 1),
				ent("amtSugar", 1), ent("amtChocolate", 2));

		macchiato  = context(ent("key", "macchiato"), ent("price", 40),
				ent("amtCoffee", 7), ent("amtMilk", 1),
				ent("amtSugar", 2), ent("amtChocolate", 0));

		americano  = context(ent("key", "americano"), ent("price", 40),
				ent("amtCoffee", 4), ent("amtMilk", 0),
				ent("amtSugar", 1), ent("amtChocolate", 0));
	}

	@After
	public void cleanUp() throws Exception {
		Program cmt =
				task(sig("deleteRecipes", CoffeeMaking.class),
						context(types(), args()));

		cmt = exert(cmt);
		logger.info("deleted recipes context: " + context(cmt));
	}

	@Test
	public void testContextCofee() throws ContextException {
		assertTrue(getRecipe(espresso).getAmtCoffee() == 6);
	}

	@Test
	public void testContextMilk() throws ContextException {
		assertTrue(getRecipe(espresso).getAmtMilk() == 0);
	}

	@Test
	public void addRecipes() throws Exception {

		Program cmt = task(sig("addRecipe", CoffeeService.class), espresso);
		Context out = context(exert(cmt));
		logger.info("job context: " + out);
		assertEquals(value(out, "recipe/added"), true);

		Program cmj = job("recipes",
				task("mocha", sig("addRecipe", CoffeeService.class), mocha),
				task("macchiato", sig("addRecipe", CoffeeService.class), macchiato),
				task("americano", sig("addRecipe", CoffeeService.class), americano));

		out = upcontext(exert(cmj));
		logger.info("job context: " + out);
		assertEquals(value(out, "recipes/americano/recipe/added"), true);
		assertEquals(value(out, "recipes/americano/recipe/added"), true);
		assertEquals(value(out, "recipes/americano/recipe/added"), true);
	}

	@Test
	public void getRecepies() throws Exception {
		Program cmt = task(sig("getRecipes", CoffeeService.class));
		cmt = exert(cmt);
		logger.info("getRecipes: " + context(cmt));
	}

	@Test
	public void getDelivery() throws Exception {
		Program cmt = task(sig("deliver", Delivery.class));
		cmt = exert(cmt);
		logger.info("getRecipes: " + context(cmt));
		assertEquals(value(context(cmt), "delivery/cost"), 60);
	}

	@Test
	public void deliverCoffee() throws Exception {
		// make sure that the CoffeMaker knows the recipe
		Program cmt = task(sig("addRecipe", CoffeeService.class), espresso);
		exert(cmt);

		// order espresso with delivery
		Domain mod = model(
				val("recipe/key", "espresso"),
				val("paid$", 120),
				val("location", "PJATK"),
				val("room", "101"),

				ent(sig("makeCoffee", CoffeeService.class,
						result("coffee$", inPaths("recipe/key")))),
				ent(sig("deliver", Delivery.class,
						result("delivery$", inPaths("location", "room")))));
//				ent("change$", invoker("paid$ - (coffee$ + delivery$)", args("paid$", "coffee$", "delivery$"))));

		add(mod, ent("change$", invoker("paid$ - (coffee$ + delivery$)", args("paid$", "coffee$", "delivery$"))));
		dependsOn(mod, dep("change$", paths("makeCoffee")), dep("change$", paths("deliver")));

		responseUp(mod, "makeCoffee", "deliver", "change$", "paid$");
		Context out = response(mod);
		logger.info("out: " + out);
		logger.info("result: " + result(mod));
		assertEquals(value(result(mod), "paid$"), 120);
		assertEquals(value(result(mod), "change$"), 10);
		assertEquals(value(result(mod), "makeCoffee"), 50);
		assertEquals(value(result(mod), "deliver"), 60);
	}

	@Test
	public void getJobLocalCoffee() throws Exception {

		Task coffee = task("tc", sig("makeCoffee", CoffeeMaker.class), context(
				inVal("recipe/key", "espresso"),
				inVal("coffee/paid", 120),
				val("x", 100),
				inVal("recipe", espresso)));

		Task delivery = task("td", sig("deliver", DeliveryImpl.class), context(
				inVal("location", "PJATK"),
				inVal("room", "101")));

		Job jcd = job("jcd", sig("exert", ServiceJobber.class), coffee, delivery,
                pipe(outPoint(coffee, "coffee/change"), inPoint(delivery, "coffee/change")),
				pipe(outPoint(coffee, "x"), inPoint(delivery, "y")));

        jcd = exert(jcd);

        //TODO where selfContext is created?
        // should be only when outPaths in thr job are declared
//        Context selfOut = selfContext(jcd);
//        Context out = upcontext(jcd);

		Context out = upcontext(exert(jcd));

		logger.info("out: " + out);
        assertEquals(value(out, "jcd/tc/coffee/paid"), 120);
        assertEquals(value(out, "jcd/tc/coffee/price"), 50);
        assertEquals(value(out, "jcd/td/delivery/cost"), 60);
        assertEquals(value(out, "jcd/td/change$"), 10);

        //shortcut with the job context (no context links)
        assertEquals(value(out, "coffee/paid"), 120);
        assertEquals(value(out, "coffee/price"), 50);
        assertEquals(value(out, "delivery/cost"), 60);
        assertEquals(value(out, "change$"), 10);
	}

    @Test
    public void getJobRemoteCoffee() throws Exception {

        Task coffee = task("tc", sig("makeCoffee", CoffeeService.class), context(
                inVal("recipe/key", "espresso"),
                inVal("coffee/paid", 120),
                inVal("recipe", espresso)));

        Task delivery = task("td", sig("deliver", Delivery.class), context(
                inVal("location", "PJATK"),
                inVal("room", "101")));

        Job jcd = job("jcd", coffee, delivery,
                pipe(outPoint(coffee, "coffee/change"), inPoint(delivery, "coffee/change")));

        Context out = upcontext(exert(jcd));

        logger.info("out: " + out);
        assertEquals(value(out, "jcd/tc/coffee/paid"), 120);
        assertEquals(value(out, "jcd/tc/coffee/price"), 50);
        assertEquals(value(out, "jcd/td/delivery/cost"), 60);
        assertEquals(value(out, "jcd/td/change$"), 10);
    }

    @Test
    public void getBlockLocalCoffee() throws Exception {

        Task coffee = task("coffee", sig("makeCoffee", CoffeeMaker.class), context(
                inVal("recipe/key", "espresso"),
                inVal("coffee/paid", 120),
                inVal("recipe", espresso),
                outPaths("coffee/change")));

        Task delivery = task("delivery", sig("deliver", DeliveryImpl.class), context(
                inVal("location", "PJATK"),
                inVal("room", "101"),
                outPaths("coffee/change", "delivery/cost", "change$")));

        Block drinkCoffee = block(context(inVal("coffee/paid", 120), val("coffee/change")), coffee, delivery);

        Context out = context(exert(drinkCoffee));

        logger.info("out: " + out);
        assertEquals(value(out, "coffee/paid"), 120);
        assertEquals(value(out, "coffee/change"), 70);
        assertEquals(value(out, "delivery/cost"), 60);
        assertEquals(value(out, "change$"), 10);
    }

    @Test
    public void getBlockRemoteCoffee() throws Exception {

        Task coffee = task("coffee", sig("makeCoffee", CoffeeService.class), context(
                inVal("recipe/key", "espresso"),
                inVal("coffee/paid", 120),
                inVal("recipe", espresso),
                outPaths("coffee/change")));

        Task delivery = task("delivery", sig("deliver", Delivery.class), context(
                inVal("location", "PJATK"),
                inVal("room", "101"),
                outPaths("coffee/change", "delivery/cost", "change$")));

        Block drinkCoffee = block(context(inVal("coffee/paid", 120), val("coffee/change")), coffee, delivery);

        Context out = context(exert(drinkCoffee));

        logger.info("out: " + out);
        assertEquals(value(out, "coffee/paid"), 120);
        assertEquals(value(out, "coffee/change"), 70);
        assertEquals(value(out, "delivery/cost"), 60);
        assertEquals(value(out, "change$"), 10);
    }
}

