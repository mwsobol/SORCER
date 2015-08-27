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
	}

	@Test
	public void testAddRecipe() {
		assertTrue(cm.addRecipe(r1));
	}

	@Test
	public void testContextCofee() throws ContextException {
		assertTrue(r1.getAmtCoffee() == 6);
	}

	@Test
	public void testContextMilk() throws ContextException {
		assertTrue(r1.getAmtMilk() == 1);
	}

	@Test
	public void addRecepie() throws Exception {
		Exertion cmt = task(sig("addRecipe", CoffeeMaking.class),
						context(parameterTypes(Recipe.class), args(r1),
							result("recipe/added")));

		logger.info("isAdded: " + value(cmt));
	}

	@Test
	public void addRecipes() throws Exception {
		Recipe mocha = new Recipe();
		mocha.setName("mocha");
		mocha.setPrice(100);
		mocha.setAmtCoffee(8);
		mocha.setAmtMilk(1);
		mocha.setAmtSugar(1);
		mocha.setAmtChocolate(2);

		Recipe macchiato = new Recipe();
		macchiato.setName("macchiato");
		macchiato.setPrice(40);
		macchiato.setAmtCoffee(7);
		macchiato.setAmtMilk(1);
		macchiato.setAmtSugar(2);
		macchiato.setAmtChocolate(0);

		Recipe americano = new Recipe();
		americano.setName("americano");
		americano.setPrice(40);
		americano.setAmtCoffee(7);
		americano.setAmtMilk(1);
		americano.setAmtSugar(2);
		americano.setAmtChocolate(0);

		Exertion cmt = job(
				task(sig("addRecipe", CoffeeMaking.class),
					context(parameterTypes(Recipe.class), args(mocha))),
				task(sig("addRecipe", CoffeeMaking.class),
						context(parameterTypes(Recipe.class), args(macchiato))),
				task(sig("addRecipe", CoffeeMaking.class),
						context(parameterTypes(Recipe.class), args(americano))));

		cmt = exert(cmt);
		logger.info("isAdded: " + upcontext(cmt));
	}

	@Test
	public void getRecepies() throws Exception {
		Exertion cmt = task(sig("getRecipes", CoffeeMaking.class));
		cmt = exert(cmt);
		logger.info("getRecipes: " + context(cmt));
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

