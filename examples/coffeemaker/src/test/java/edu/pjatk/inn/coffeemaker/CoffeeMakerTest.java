package edu.pjatk.inn.coffeemaker;

import edu.pjatk.inn.coffeemaker.impl.CoffeeMaker;
import edu.pjatk.inn.coffeemaker.impl.Inventory;
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
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Exertion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class CoffeeMakerTest {
	private final static Logger logger = LoggerFactory.getLogger(CoffeeMakerTest.class);

	private CoffeeMaker cm;
	private Inventory i;
	private Recipe espresso, mocha, macchiato, americano;

	@Before
	public void setUp() throws ContextException {
		cm = new CoffeeMaker();
		i = cm.checkInventory();

		espresso = new Recipe();
		espresso.setName("espresso");
		espresso.setPrice(50);
		espresso.setAmtCoffee(6);
		espresso.setAmtMilk(1);
		espresso.setAmtSugar(1);
		espresso.setAmtChocolate(0);

		mocha = new Recipe();
		mocha.setName("mocha");
		mocha.setPrice(100);
		mocha.setAmtCoffee(8);
		mocha.setAmtMilk(1);
		mocha.setAmtSugar(1);
		mocha.setAmtChocolate(2);

		macchiato = new Recipe();
		macchiato.setName("macchiato");
		macchiato.setPrice(40);
		macchiato.setAmtCoffee(7);
		macchiato.setAmtMilk(1);
		macchiato.setAmtSugar(2);
		macchiato.setAmtChocolate(0);

		americano = new Recipe();
		americano.setName("americano");
		americano.setPrice(40);
		americano.setAmtCoffee(7);
		americano.setAmtMilk(1);
		americano.setAmtSugar(2);
		americano.setAmtChocolate(0);
	}

	@After
	public void cleanUp() throws Exception {
		cm.deleteRecipes();
	}

	@Test
	public void testAddRecipe() {
		assertTrue(cm.addRecipe(espresso));
	}

	@Test
	public void testContextCofee() throws ContextException {
		assertTrue(espresso.getAmtCoffee() == 6);
	}

	@Test
	public void testContextMilk() throws ContextException {
		assertTrue(espresso.getAmtMilk() == 1);
	}

	@Test
	public void addRecepie() throws Exception {
		cm.addRecipe(mocha);
		assertEquals(cm.getRecipeForName("mocha").getName(), "mocha");
	}

	@Test
	public void addContextRecepie() throws Exception {
		cm.addRecipe(Recipe.getContext(mocha));
		assertEquals(cm.getRecipeForName("mocha").getName(), "mocha");
	}

	@Test
	public void addServiceRecepie() throws Exception {
		Exertion cmt = task(sig("addRecipe", cm),
						context(parameterTypes(Recipe.class), args(espresso),
							result("recipe/added")));

		logger.info("isAdded: " + value(cmt));
		assertEquals(cm.getRecipeForName("espresso").getName(), "espresso");
	}

	@Test
	public void addRecipes() throws Exception {
		Exertion cmj = job("recipes", sig("execute", ServiceJobber.class),
				task("mocha", sig("addRecipe", cm),
					context(parameterTypes(Recipe.class), args(mocha))),
				task("macchiato", sig("addRecipe", cm),
						context(parameterTypes(Recipe.class), args(macchiato))),
				task("americano", sig("addRecipe", cm),
						context(parameterTypes(Recipe.class), args(americano))));

		cmj = exert(cmj);
		Context out = upcontext(cmj);
		logger.info("job context: " + out);
		assertEquals(value(out, "recipes/mocha/context/result"), true);
		assertEquals(value(out, "recipes/macchiato/context/result"), true);
		assertEquals(value(out, "recipes/americano/context/result"), true);

		assertEquals(cm.getRecipeForName("mocha").getName(), "mocha");
		assertEquals(cm.getRecipeForName("macchiato").getName(), "macchiato");
		assertEquals(cm.getRecipeForName("americano").getName(), "americano");
	}

	@Test
	public void makeCoffee() throws Exception {
		Exertion cmj = job("coffee", sig("execute", ServiceJobber.class),
				task("recipe", sig("addRecipe", cm),
						context(parameterTypes(Recipe.class), args(espresso))),
				task("pay", sig("makeCoffee",cm),
						context(parameterTypes(Recipe.class, int.class), args(espresso, 200))));
		cmj = exert(cmj);
		logger.info("job context: " + upcontext(cmj));
		logger.info("change: " + value(upcontext(cmj), "coffee/pay/context/result"));
		assertEquals(value(upcontext(cmj), "coffee/pay/context/result"), 150);
	}

}

