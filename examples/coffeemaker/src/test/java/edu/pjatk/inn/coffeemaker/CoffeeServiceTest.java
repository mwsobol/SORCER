package edu.pjatk.inn.coffeemaker;

import edu.pjatk.inn.coffeemaker.impl.CoffeeMaker;
import edu.pjatk.inn.coffeemaker.impl.Inventory;
import edu.pjatk.inn.coffeemaker.impl.Recipe;
import org.junit.Before;
import org.junit.BeforeClass;
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

import static edu.pjatk.inn.coffeemaker.impl.Recipe.recipe;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.response;
import static sorcer.po.operator.invoker;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class CoffeeServiceTest {
	private final static Logger logger = LoggerFactory.getLogger(CoffeeServiceTest.class);

	private CoffeeMaker cm;
	private Inventory i;
	private Recipe r1;
	private Context rc;

	@Before
	public void setUp() throws ContextException {
		cm = new CoffeeMaker();
		i = cm.checkInventory();

		r1 = new Recipe();
		r1.setName("espresso");
		r1.setPrice(50);
		r1.setAmtCoffee(6);
		r1.setAmtMilk(1);
		r1.setAmtSugar(1);
		r1.setAmtChocolate(0);

		rc = context(ent("name", "espresso"), ent("price", 50),
				ent("amtCoffee", 6), ent("amtMilk", 0),
				ent("amtSugar", 1), ent("amtChocolate", 0));
	}

	@Test
	public void testAddRecipe() {
		assertTrue(cm.addRecipe(r1));
	}

	@Test
	public void testContextCofee() throws ContextException {
		assertTrue(recipe(rc).getAmtCoffee() == 6);
	}

	@Test
	public void testContextMilk() throws ContextException {
		assertTrue(recipe(rc).getAmtMilk() == 1);
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
		Context mocha  = context(ent("name", "mocha"), ent("price", 100),
				ent("amtCoffee", 8), ent("amtMilk", 1),
				ent("amtSugar", 1), ent("amtChocolate", 2));

		Context macchiato  = context(ent("name", "macchiato"), ent("price", 40),
				ent("amtCoffee", 7), ent("amtMilk", 1),
				ent("amtSugar", 2), ent("amtChocolate", 0));

		Context americano  = context(ent("name", "americano"), ent("price", 40),
				ent("amtCoffee", 4), ent("amtMilk", 0),
				ent("amtSugar", 1), ent("amtChocolate", 0));

		Exertion cmt = job(task(sig("addRecipe", CoffeeService.class), mocha),
				task(sig("addRecipe", CoffeeService.class), macchiato),
				task(sig("addRecipe", CoffeeService.class), americano));

		cmt = exert(cmt);
		logger.info("isAdded: " + upcontext(cmt));
	}

	@Test
	public void getRecepies() throws Exception {
		Exertion cmt = task(sig("recipes", CoffeeService.class));
		cmt = exert(cmt);
		logger.info("recipes: " + context(cmt));
	}

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

