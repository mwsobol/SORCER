package edu.pjatk.inn.coffeemaker;

import edu.pjatk.inn.coffeemaker.impl.Recipe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/coffeemaker")
public class RecipeTest {

    @Test
    public void editRecipeCoffe1() {
        //given
        Recipe coffe = new Recipe();
        coffe.setAmtCoffee(1);

        //when
        coffe.setAmtCoffee(10);

        //then
        assertEquals(10,coffe.getAmtCoffee());
    }

    @Test
    public void editRecipeCoffe2() {
        //given
        Recipe coffe = new Recipe();
        coffe.setAmtCoffee(1);

        //when
        coffe.setAmtCoffee(-10);

        //then
        assertEquals(1,coffe.getAmtCoffee());
    }

    @Test
    public void editRecipeMilk1() {
        //given
        Recipe coffe = new Recipe();
        coffe.setAmtMilk(1);

        //when
        coffe.setAmtMilk(-10);

        //then
        assertEquals(1,coffe.getAmtMilk());
    }

    @Test
    public void editRecipeMilk2() {
        //given
        Recipe coffe = new Recipe();
        coffe.setAmtMilk(1);

        //when
        coffe.setAmtMilk(10);

        //then
        assertEquals(10, coffe.getAmtMilk());
    }
    @Test
    public void editRecipeSugar1() {
        //given
        Recipe coffe = new Recipe();
        coffe.setAmtSugar(1);

        //when
        coffe.setAmtSugar(-10);

        //then
        assertEquals(1,coffe.getAmtSugar());
    }

    @Test
    public void editRecipeSugar2() {
        //given
        Recipe coffe = new Recipe();
        coffe.setAmtSugar(1);

        //when
        coffe.setAmtSugar(10);

        //then
        assertEquals(10, coffe.getAmtSugar());
    }

    @Test
    public void editRecipeChocolate1() {
        //given
        Recipe coffe = new Recipe();
        coffe.setAmtChocolate(1);

        //when
        coffe.setAmtChocolate(-10);

        //then
        assertEquals(1,coffe.getAmtChocolate());
    }

    @Test
    public void editRecipeChocolate2() {
        //given
        Recipe coffe = new Recipe();
        coffe.setAmtChocolate(1);

        //when
        coffe.setAmtChocolate(10);

        //then
        assertEquals(10, coffe.getAmtChocolate());
    }

    @Test
    public void editRecipePrice1() {
        //given
        Recipe coffe = new Recipe();
        coffe.setPrice(1);

        //when
        coffe.setPrice(-10);

        //then
        assertEquals(1,coffe.getPrice());
    }

    @Test
    public void editRecipePrice2() {
        //given
        Recipe coffe = new Recipe();
        coffe.setPrice(1);

        //when
        coffe.setPrice(10);

        //then
        assertEquals(10, coffe.getPrice());
    }
}
