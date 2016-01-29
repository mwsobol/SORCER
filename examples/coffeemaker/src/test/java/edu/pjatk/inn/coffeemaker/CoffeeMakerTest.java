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
import sorcer.service.Exertion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class CoffeeMakerTest {
	private final static Logger logger = LoggerFactory.getLogger(CoffeeMakerTest.class);

	private CoffeeMaker coffeeMaker;
	private Inventory inventory;
	private Recipe espresso, mocha, macchiato, americano,coffee;

	@Before
	public void setUp() throws ContextException {
		coffeeMaker = new CoffeeMaker();
		inventory = coffeeMaker.checkInventory();

		coffee = new Recipe();
		coffee.setName("Coffee");
	    coffee.setPrice(50);
		coffee.setAmtCoffee(3);
		coffee.setAmtMilk(1);
		coffee.setAmtSugar(1);
		coffee.setAmtChocolate(0);

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

	@Test
	public void testAddRecipe() {
		assertTrue(coffeeMaker.addRecipe(espresso));
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
		coffeeMaker.addRecipe(mocha);
		assertEquals(coffeeMaker.getRecipeForName("mocha").getName(), "mocha");
	}

	@Test
	public void addContextRecepie() throws Exception {
		coffeeMaker.addRecipe(Recipe.getContext(mocha));
		assertEquals(coffeeMaker.getRecipeForName("mocha").getName(), "mocha");
	}

	@Test
	public void addServiceRecepie() throws Exception {
		Exertion cmt = task(sig("addRecipe", coffeeMaker),
						context(parameterTypes(Recipe.class), args(espresso),
							result("recipe/added")));

		logger.info("isAdded: " + value(cmt));
		assertEquals(coffeeMaker.getRecipeForName("espresso").getName(), "espresso");
	}

	@Test
	public void addRecipes() throws Exception {
		coffeeMaker.addRecipe(mocha);
		coffeeMaker.addRecipe(macchiato);
		coffeeMaker.addRecipe(americano);

		assertEquals(coffeeMaker.getRecipeForName("mocha").getName(), "mocha");
		assertEquals(coffeeMaker.getRecipeForName("macchiato").getName(), "macchiato");
		assertEquals(coffeeMaker.getRecipeForName("americano").getName(), "americano");
	}

	@Test
	public void makeCoffee() throws Exception {
		coffeeMaker.addRecipe(espresso);
		assertEquals(coffeeMaker.makeCoffee(espresso, 200), 150);
	}

	///Coffee: 15
	// Milk: 15
	// Sugar: 15
	// Chocolate: 15
	@Test
	public void checkInventory() throws Exception {
		assertTrue((inventory.getChocolate()==15) && (inventory.getCoffee()==15) &&
				(inventory.getMilk()==15) && (inventory.getSugar()==15));
	}
	//Your change is 10.
	// Return to main menu
	@Test
	public void purchaseBeverage1() throws Exception {
		coffeeMaker.addRecipe(coffee);
	assertEquals(coffeeMaker.makeCoffee(coffee,60),10);
}
	//Your change is 40
	// Return to main menu
	// Coffee: 15
	// Milk: 15
	// Sugar: 15
	// Chocolate: 15
	@Test
	public void purchaseBeverage2() throws Exception {
		coffeeMaker.addRecipe(coffee);
		assertTrue((coffeeMaker.makeCoffee(coffee,40)==40) && (inventory.getChocolate()==15) &&
				(inventory.getCoffee()==15) && (inventory.getMilk()==15) && (inventory.getSugar()==15));
	}
	//Your change is 50
	// Return to main menu
	@Test
	public void purchaseBeverage3() throws Exception {
		Recipe newDrink = new Recipe();
		newDrink.setName("Coffee");
		newDrink.setPrice(50);
		newDrink.setAmtCoffee(16);
		newDrink.setAmtMilk(2);
		newDrink.setAmtSugar(3);
		newDrink.setAmtChocolate(5);
		coffeeMaker.addRecipe(newDrink);
		assertEquals(coffeeMaker.makeCoffee(newDrink,50),50);

	}

}

