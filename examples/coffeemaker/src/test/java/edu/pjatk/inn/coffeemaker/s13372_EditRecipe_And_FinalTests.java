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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Created by Svitlana Bilan on 25-Jan-16.
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class s13372_EditRecipe_And_FinalTests {

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
    public void addRecipeOnly3() throws Exception {
        assertEquals(true, coffeeMaker.addRecipe(mocha));
        assertEquals(true, coffeeMaker.addRecipe(macchiato));
        assertEquals(true, coffeeMaker.addRecipe(americano));
        assertEquals(false, coffeeMaker.addRecipe(espresso));
    }

    @Test
    public void addSameRecipe() throws Exception {
        assertEquals(true, coffeeMaker.addRecipe(mocha));
        assertEquals(false, coffeeMaker.addRecipe(mocha));
    }

    @Test
    public void deleteRecipe() throws Exception{
        coffeeMaker.addRecipe(mocha);
        assertEquals(true, coffeeMaker.deleteRecipe(mocha));
        assertEquals(false, coffeeMaker.deleteRecipe(espresso));

    }

    @Test
    public void editRecipeWhenAlready3() throws Exception{
        Recipe oldR = new Recipe();
        oldR.setName("americano");
        oldR.setPrice(40);
        oldR.setAmtCoffee(7);
        oldR.setAmtMilk(1);
        oldR.setAmtSugar(2);
        oldR.setAmtChocolate(0);

        Recipe newR = new Recipe();
        newR.setName("latte");
        newR.setPrice(50);
        newR.setAmtCoffee(6);
        newR.setAmtMilk(2);
        newR.setAmtSugar(1);
        newR.setAmtChocolate(0);

        coffeeMaker.addRecipe(oldR);
        coffeeMaker.addRecipe(espresso);
        coffeeMaker.addRecipe(mocha);
        assertEquals(true, coffeeMaker.editRecipe(oldR, newR));
        assertEquals(coffeeMaker.getRecipeForName("espresso").getName(), "espresso");
        assertEquals(coffeeMaker.getRecipeForName("mocha").getName(), "mocha");
        assertEquals(coffeeMaker.getRecipeForName("latte").getName(), "latte");

    }

    @Test
    public void editRecipeWithTheSameName() throws Exception{

        coffeeMaker.addRecipe(espresso);

        Recipe oldR = new Recipe();
        oldR.setName("americano");
        oldR.setPrice(40);
        oldR.setAmtCoffee(7);
        oldR.setAmtMilk(1);
        oldR.setAmtSugar(2);
        oldR.setAmtChocolate(0);

        Recipe newR = new Recipe();
        newR.setName("espresso");
        newR.setPrice(40);
        newR.setAmtCoffee(7);
        newR.setAmtMilk(1);
        newR.setAmtSugar(2);
        newR.setAmtChocolate(0);


        coffeeMaker.addRecipe(oldR);
        assertEquals(false, coffeeMaker.editRecipe(oldR, newR));
    }

    @Test
    public void addInventoryNonPositive() throws Exception{

        assertEquals(false, coffeeMaker.addInventory(7,2,-2,0));
        assertEquals(coffeeMaker.checkInventory().getCoffee(), inventory.getCoffee());
        assertEquals(coffeeMaker.checkInventory().getChocolate(), inventory.getChocolate());
        assertEquals(coffeeMaker.checkInventory().getMilk(), inventory.getMilk());
        assertEquals(coffeeMaker.checkInventory().getSugar(), inventory.getSugar());
    }

    @Test
    public void addInventory() throws Exception{

        assertEquals(true, coffeeMaker.addInventory(7,2,2,0));
        assertEquals(22, coffeeMaker.checkInventory().getCoffee());
        assertEquals(15, coffeeMaker.checkInventory().getChocolate());
        assertEquals(17, coffeeMaker.checkInventory().getMilk());
        assertEquals(17, coffeeMaker.checkInventory().getSugar());
    }

    @Test
    public void checkChangeAfterPurchase() throws Exception{
        coffeeMaker.addRecipe(espresso);
        coffeeMaker.addRecipe(americano);
        coffeeMaker.addRecipe(mocha);

        assertEquals(50,coffeeMaker.makeCoffee(mocha, 150));
    }

    @Test
    public void checkInventoryAfterPurchase() throws Exception{
        coffeeMaker.addRecipe(espresso);
        coffeeMaker.addRecipe(americano);
        coffeeMaker.addRecipe(mocha);

        assertEquals(0, coffeeMaker.makeCoffee(mocha,100));
        assertEquals(7, coffeeMaker.checkInventory().getCoffee());
        assertEquals(14, coffeeMaker.checkInventory().getSugar());
        assertEquals(14, coffeeMaker.checkInventory().getMilk());
        assertEquals(13, coffeeMaker.checkInventory().getChocolate());
    }

    @Test
    public void checkEnoughIngredients() throws Exception{
        Recipe temp = new Recipe();

        temp.setName("latte");
        temp.setPrice(50);
        temp.setAmtCoffee(10);
        temp.setAmtMilk(16);
        temp.setAmtSugar(1);
        temp.setAmtChocolate(0);

        assertEquals(false, inventory.enoughIngredients(temp));
    }

    @Test
    public void returnChangeIfNotEnoughIngredients() throws Exception{
        Recipe temp = new Recipe();

        temp.setName("latte");
        temp.setPrice(50);
        temp.setAmtCoffee(10);
        temp.setAmtMilk(16);
        temp.setAmtSugar(1);
        temp.setAmtChocolate(0);

        assertEquals(150,coffeeMaker.makeCoffee(temp, 150));

    }

    @Test
    public void returnMoneyIfNotEnoughEntered() throws Exception{
        Recipe temp = new Recipe();

        temp.setName("latte");
        temp.setPrice(50);
        temp.setAmtCoffee(10);
        temp.setAmtMilk(16);
        temp.setAmtSugar(1);
        temp.setAmtChocolate(0);

        assertEquals(40,coffeeMaker.makeCoffee(temp, 40));

    }


}
