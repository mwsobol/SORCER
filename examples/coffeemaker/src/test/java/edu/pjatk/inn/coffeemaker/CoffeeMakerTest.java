package edu.pjatk.inn.coffeemaker;

import static edu.pjatk.inn.coffeemaker.impl.Recipe.recipe;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.po.operator.invoker;

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
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.modeling.Model;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class CoffeeMakerTest {
	private final static Logger logger = LoggerFactory.getLogger(CoffeeMakerTest.class);

	private CoffeeMaker cm;
	private Inventory i;
	private Recipe r1;
	private Context rc;

	@Before
	public void setUp() throws ContextException {
		cm = new CoffeeMaker();
		i = cm.checkInventory();

		r1 = new Recipe();
		r1.setName("Coffee");
		r1.setPrice(50);
		r1.setAmtCoffee(6);
		r1.setAmtMilk(1);
		r1.setAmtSugar(1);
		r1.setAmtChocolate(0);

		rc = context(ent("name", "Coffee"), ent("price", 50),
				ent("amtCoffee", 6), ent("amtMilk", 1),
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

	// Service orientation
	@Test
	public void getRecepies() throws Exception {
//		Exertion cmt = task(sig("recipes", CoffeeService.class));
		Exertion cmt = task(sig("recipes", CoffeeMaker.class));
		cmt = exert(cmt);
		logger.info("recipes: " + context(cmt));
	}

	@Test
	public void deliverCoffee() throws Exception {

		Model mod = model(inEnt("recipe", "myCoffee)"),
				inEnt("location", "PJATK"),
				inEnt("room", "101"),

				srv(sig("makeCoffee", CoffeeService.class,
						result("coffee$", inPaths("recipe")))),
				srv(sig("deliver", Delivery.class,
						result("delivery$", inPaths("location", "room")))),
				ent("cost", invoker("coffee$ + delivery$", ents("cofee$", "delivery$"))),
				response("cost"));

		logger.info("paid: " + response(mod));
	}

}

