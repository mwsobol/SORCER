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

import static org.junit.Assert.*;
import static sorcer.eo.operator.context;
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
	private Recipe espresso, mocha, macchiato, americano, coffee, latte, hotChocolate;

	@Before
	public void setUp() throws ContextException {
		coffeeMaker = new CoffeeMaker();
		inventory = coffeeMaker.checkInventory();

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

		coffee = new Recipe();
		coffee.setName("Coffee");
		coffee.setPrice(50);
		coffee.setAmtCoffee(3);
		coffee.setAmtMilk(1);
		coffee.setAmtSugar(1);
		coffee.setAmtChocolate(0);

		latte = new Recipe();
		latte.setName("Latte");
		latte.setPrice(60);
		latte.setAmtCoffee(3);
		latte.setAmtMilk(3);
		latte.setAmtSugar(2);
		latte.setAmtChocolate(0);

		hotChocolate = new Recipe();
		hotChocolate.setName("Hot Chocolate");
		hotChocolate.setPrice(60);
		hotChocolate.setAmtCoffee(0);
		hotChocolate.setAmtMilk(2);
		hotChocolate.setAmtSugar(2);
		hotChocolate.setAmtChocolate(3);

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

	@Test
	public void addRecipe1() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();


		//when
		boolean result = coffeeMaker.addRecipe(coffee);

		//then
		assertEquals(true, result);
	}

	@Test
	public void addRecipe2() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();

		//when
		coffeeMaker.addRecipe(coffee);
		boolean result = coffeeMaker.addRecipe(coffee);

		//then
		assertEquals(false, result);
	}

	@Test
	public void addRecipe3() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();

		//when
		mocha.setPrice(-50);
		boolean result = coffeeMaker.addRecipe(mocha);

		//then
		assertNotEquals(-50, coffeeMaker.getRecipeForName("mocha").getPrice());
		assertTrue(result);
	}

	@Test
	public void addRecipe15() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();

		//when
		coffeeMaker.addRecipe(coffee);
		coffeeMaker.addRecipe(mocha);
		coffeeMaker.addRecipe(latte);
		boolean result = coffeeMaker.addRecipe(hotChocolate);

		//then
		assertEquals(false, result);
	}

	@Test
	public void editRecipe1() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();
		coffeeMaker.addRecipe(latte);
		coffeeMaker.addRecipe(coffee);

		//when
		Recipe coffe1 = new Recipe();
		coffe1.setName("Coffee");

		boolean result = coffeeMaker.editRecipe(latte, coffe1);

		//then
		assertEquals(false, result);
	}

	@Test
	public void editRecipe2() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();
		coffeeMaker.addRecipe(latte);

		//when
		Recipe coffe1 = new Recipe();
		coffe1.setName("Coffee");

		boolean result = coffeeMaker.editRecipe(latte, coffe1);

		//then
		assertEquals(true, result);
	}

	@Test
	public void deleteRecipe1() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();
		coffeeMaker.addRecipe(coffee);

		//when
		boolean result = coffeeMaker.deleteRecipe(coffee);
		Recipe coffeeResult = coffeeMaker.getRecipeForName(coffee.getName());
		//then
		assertEquals(true, result);
		assertNull(coffeeResult);
	}

	@Test
	public void deleteRecipe2() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();

		//when
		boolean result = coffeeMaker.deleteRecipe(coffee);

		//then
		assertFalse(result);
	}

	@Test
	public void makeCoffee1() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();
		Inventory inv = coffeeMaker.checkInventory();
		int chocolateBefore = inv.getChocolate();
		int sugarBefore = inv.getSugar();
		int milkBefore = inv.getMilk();
		int coffeeBefore = inv.getCoffee();

		//when
		int result = coffeeMaker.makeCoffee(mocha, 150);

		//then
		assertEquals(150 - mocha.getPrice(), result);
		assertEquals(chocolateBefore - mocha.getAmtChocolate(), coffeeMaker.checkInventory().getChocolate());
		assertEquals(sugarBefore - mocha.getAmtSugar(), coffeeMaker.checkInventory().getSugar());
		assertEquals(milkBefore - mocha.getAmtMilk(), coffeeMaker.checkInventory().getMilk());
		assertEquals(coffeeBefore - mocha.getAmtCoffee(), coffeeMaker.checkInventory().getCoffee());
	}

	@Test
	public void addInventory1() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();

		//when
		boolean result = coffeeMaker.addInventory(11,11,11,11);

		//then
		assertTrue(result);
	}

	@Test
	public void addInventory2() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();

		//when
		boolean result = coffeeMaker.addInventory(-11,11,11,11);

		//then
		assertFalse(result);
	}
	@Test
	public void addInventory3() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();

		//when
		boolean result = coffeeMaker.addInventory(11,-1,11,11);

		//then
		assertFalse(result);
	}
	@Test
	public void addInventory4() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();

		//when
		boolean result = coffeeMaker.addInventory(11,11,-11,11);

		//then
		assertFalse(result);
	}
	@Test
	public void addInventory5() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();

		//when
		boolean result = coffeeMaker.addInventory(11,11,11,-11);

		//then
		assertFalse(result);
	}
	@Test
	public void addInventory6() throws Exception {
		//given
		coffeeMaker = new CoffeeMaker();

		//when
		boolean result = coffeeMaker.addInventory(0,0,1,0);

		//then
		assertTrue(result);
	}

}

