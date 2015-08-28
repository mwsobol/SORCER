package edu.pjatk.inn.coffeemaker;

import edu.pjatk.inn.coffeemaker.impl.Recipe;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.modeling.Model;

import static edu.pjatk.inn.coffeemaker.impl.Recipe.getRecipe;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.args;
import static sorcer.mo.operator.response;
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
						context(parameterTypes(), args()));

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
		Context espresso = context(ent("name", "espresso"), ent("price", 50),
				ent("amtCoffee", 6), ent("amtMilk", 0),
				ent("amtSugar", 1), ent("amtChocolate", 0));

		Exertion cmt = task(sig("addRecipe", CoffeeService.class), espresso);
		cmt = exert(cmt);
		logger.info("isAdded: " + context(cmt, "recipe/added"));
	}

	@Test
	public void addRecipes() throws Exception {
		Exertion cmt = job(task(sig("addRecipe", CoffeeService.class), mocha),
				task(sig("addRecipe", CoffeeService.class), macchiato),
				task(sig("addRecipe", CoffeeService.class), americano));

		cmt = exert(cmt);
		logger.info("isAdded: " + upcontext(cmt));
	}

	@Test
	public void getRecepies() throws Exception {
		Exertion cmt = task(sig("getRecipes", CoffeeService.class));
		cmt = exert(cmt);
		logger.info("getRecipes: " + context(cmt));
	}

	@Ignore
	@Test
	public void deliverCoffee() throws Exception {

		Model mod = model(inEnt("recipe", "mocha)"),
				inEnt("location", "PJATK"),
				inEnt("room", "101"),
				inEnt("tip", true),

				srv(sig("makeCoffee", CoffeeService.class,
						result("coffee$", inPaths("recipe")))),
				srv(sig("deliver", Delivery.class,
						result("delivery$", inPaths("location", "room")))),
				ent("cost", invoker("coffee$ + delivery$", ents("cofee$", "delivery$"))),
				response("cost"));

		logger.info("paid: " + response(mod));
	}

}

