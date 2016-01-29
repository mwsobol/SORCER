package edu.pjatk.inn.coffeemaker;

import edu.pjatk.inn.coffeemaker.impl.Inventory;
import edu.pjatk.inn.coffeemaker.impl.Recipe;

import java.rmi.RemoteException;

/**
 * @author  Mike Sobolewski
 */
public interface CoffeeMaking {

	/**
	 * Returns true if a recipe is successfully added to the 
	 * coffee maker
	 * @param r
	 * @return boolean
	 */
	public boolean addRecipe(Recipe r)  throws RemoteException;

	/**
	 * Returns true if the recipe was deleted from the 
	 * coffee maker
	 * @param r
	 * @return boolean
	 */
    public boolean deleteRecipe(Recipe r) throws RemoteException;

    /**
     * Returns true if the recipe were deleted from the
     * coffee maker
     * @return boolean
     */
    public boolean deleteRecipes();

    /**
     * Returns true if the recipe is successfully edited
     * @param oldRecipe
     * @param newRecipe
     * @return boolean
     */
    public boolean editRecipe(Recipe oldRecipe, Recipe newRecipe) throws RemoteException;
    
    /**
     * Returns true if inventory was successfully added
     * @param amtCoffee
     * @param amtMilk
     * @param amtSugar
     * @param amtChocolate
     * @return boolean
     */
    public boolean addInventory(Object amtCoffee, Object amtMilk, Object amtSugar, Object amtChocolate) throws RemoteException;
    
    /**
     * Returns the inventory of the coffee maker
     * @return Inventory
     */
    public Inventory checkInventory() throws RemoteException;

    /**
     * Returns the change of a user's beverage purchase, or
     * the user's money if the beverage cannot be made
     * @param r
     * @param amtPaid
     * @return int
     */
    public int makeCoffee(Recipe r, int amtPaid) throws RemoteException;

    /**
     * Returns an array of all the getRecipes
     * @return Recipe[]
     */
    public Recipe[] getRecipes() throws RemoteException;

    /**
     * Returns the Recipe associated with the given name
     * @param name
     * @return Recipe
     */
	public Recipe getRecipeForName(String name)  throws RemoteException;
}
