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
import sorcer.eo.operator;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import static edu.pjatk.inn.coffeemaker.impl.Recipe.getRecipe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.*;
import static sorcer.mo.operator.result;
import static sorcer.po.operator.invoker;

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

		espresso = context(ent("name", "espresso"), ent("price", 50),
				ent("amtCoffee", 6), ent("amtMilk", 0),
				ent("amtSugar", 1), ent("amtChocolate", 0));

		mocha  = context(ent("name", "mocha"), ent("price", 100),
				ent("amtCoffee", 8), ent("amtMilk", 1),
				ent("amtSugar", 1), ent("amtChocolate", 2));

		macchiato  = context(ent("name", "macchiato"), ent("price", 40),
				ent("amtCoffee", 7), ent("amtMilk", 1),
				ent("amtSugar", 2), ent("amtChocolate", 0));

		americano  = context(ent("name", "americano"), ent("price", 40),
				ent("amtCoffee", 4), ent("amtMilk", 0),
				ent("amtSugar", 1), ent("amtChocolate", 0));

	}

	@After
	public void cleanUp() throws Exception {
		Exertion cmt =
				task(sig("deleteRecipes", CoffeeMaking.class),
						context(parameterTypes(), operator.args()));

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
	public void addRecepie() throws Exception {
		Exertion cmt = task(sig("addRecipe", CoffeeService.class), espresso);
		Context out = context(exert(cmt));
		logger.info("job context: " + out);
		assertEquals(value(out, "recipe/added"), true);
	}

	@Test
	public void addRecipes() throws Exception {
		Exertion cmj = job("recipes",
				task("mocha", sig("addRecipe", CoffeeService.class), mocha),
				task("macchiato", sig("addRecipe", CoffeeService.class), macchiato),
				task("americano", sig("addRecipe", CoffeeService.class), americano));

		Context out = upcontext(exert(cmj));
		logger.info("job context: " + out);
		assertEquals(value(out, "recipes/americano/recipe/added"), true);
		assertEquals(value(out, "recipes/americano/recipe/added"), true);
		assertEquals(value(out, "recipes/americano/recipe/added"), true);
	}

	@Test
	public void getRecepies() throws Exception {
		Exertion cmt = task(sig("getRecipes", CoffeeService.class));
		cmt = exert(cmt);
		logger.info("getRecipes: " + context(cmt));
	}

	@Test
	public void getDelivery() throws Exception {
		Exertion cmt = task(sig("deliver", Delivery.class));
		cmt = exert(cmt);
		logger.info("getRecipes: " + context(cmt));
		assertEquals(value(context(cmt), "delivery/cost"), 60);
	}

	@Test
	public void deliverCoffee() throws Exception {
		// make sure that the CoffeMaker knows the recipe
		Exertion cmt = task(sig("addRecipe", CoffeeService.class), espresso);
		exert(cmt);

		// order espresso with delivery
		Model mod = model(
				inEnt("recipe/name", "espresso"),
				inEnt("paid$", 120),
				inEnt("location", "PJATK"),
				inEnt("room", "101"),

				srv(sig("makeCoffee", CoffeeService.class,
						result("coffee$", inPaths("recipe/name")))),
				srv(sig("deliver", Delivery.class,
						result("delivery$", inPaths("location", "room")))));
//				ent("change$", invoker("paid$ - (coffee$ + delivery$)", ents("paid$", "coffee$", "delivery$"))));

		add(mod, ent("change$", invoker("paid$ - (coffee$ + delivery$)", ents("paid$", "coffee$", "delivery$"))));
		dependsOn(mod, ent("change$", "makeCoffee"), ent("change$", "deliver"));
		responseUp(mod, "makeCoffee", "deliver", "change$", "paid$");
		Context out = response(mod);
		logger.info("out: " + out);
		logger.info("result: " + result(mod));
		assertEquals(value(result(mod), "paid$"), 120);
		assertEquals(value(result(mod), "makeCoffee"), 50);
		assertEquals(value(result(mod), "deliver"), 60);
		assertEquals(value(result(mod), "change$"), 10);
	}

	@Test
	public void getCoffee() throws Exception {

		Task coffee = task("coffee", sig("makeCoffee", CoffeeMaker.class), context(
				inEnt("recipe/name", "espresso"),
				inEnt("coffee/paid", 120),
				inEnt("recipe", espresso)));

		Task delivery = task("delivery", sig("deliver", DeliveryImpl.class), context(
				inEnt("location", "PJATK"),
				inEnt("room", "101")));

		Job drinkCoffee = job(sig("exert", ServiceJobber.class), coffee, delivery);

		Context out = upcontext(exert(drinkCoffee));

		logger.info("out: " + out);
	}
}

