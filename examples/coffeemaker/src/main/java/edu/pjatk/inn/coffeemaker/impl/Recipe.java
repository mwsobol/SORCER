package edu.pjatk.inn.coffeemaker.impl;

import sorcer.core.context.ServiceContext;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.io.Serializable;

/**
 * Recipe class used to model information about recipe in
 * coffee maker.
 *
 * @author   Sarah & Mike
 */
public class Recipe implements Serializable {
    /**
     * The unique name of recipe
     */
    private String name;
    /**
     * The price of beverage prepared according to this recipe
     */
    private int price;
    /**
     * The units of coffe in this recipe
     */
    private int amtCoffee;
    /**
     * The units of milk in this recipe
     */
    private int amtMilk;
    /**
     * The units of sugar in this recipe
     */
    private int amtSugar;
    /**
     * The units of chocolate in this recipe
     */
    private int amtChocolate;

    /**
     * Default Constructor.
     * The default behaviour of recipe object
     * <ul>
     * <li>name is empty string</li>
     * <li>price is 0</li>
     * <li>amtCoffe is 0</li>
     * <li>amtMilk is 0</li>
     * <li>amtSugar is 0</li>
     * <li>amtChocolate is 0</li>
     * </ul>
     */
    public Recipe() {
    	this.name = "";
    	this.price = 0;
    	this.amtCoffee = 0;
    	this.amtMilk = 0;
    	this.amtSugar = 0;
    	this.amtChocolate = 0;
    }

    /**
	 * @return   Returns the amount of chocolate.
	 */
    public int getAmtChocolate() {
		return amtChocolate;
	}
    /**
	 * @param amtChocolate   The amount of chocolate to set.
	 */
    public void setAmtChocolate(int amtChocolate) {
		if (amtChocolate >= 0) {
			this.amtChocolate = amtChocolate;
		}
	}
	/**
	 * @return Returns the amount of coffee.
	 */
    public int getAmtCoffee() {
		return amtCoffee;
	}
	/**
	 * @param amtCoffee The amount of coffee to set.
	 */
    public void setAmtCoffee(int amtCoffee) {
		if (amtCoffee >= 0) {
			this.amtCoffee = amtCoffee;
		}
	}
	/**
	 * @return Returns the amount of milk.
	 */
    public int getAmtMilk() {
		return amtMilk;
	}
	/**
	 * @param amtMilk The amount of milk to set.
	 */
    public void setAmtMilk(int amtMilk) {
		if (amtMilk >= 0) {
			this.amtMilk = amtMilk;
		}
	}
	/**
	 * @return Returns the amount of sugar.
	 */
    public int getAmtSugar() {
		return amtSugar;
	}
	/**
	 * @param amtSugar The amount of sugar to set.
	 */
    public void setAmtSugar(int amtSugar) {
		if (amtSugar >= 0) {
			this.amtSugar = amtSugar;
		}
	}
	/**
	 * @return Returns the name of recipe.
	 */
    public String getName() {
		return name;
	}
	/**
	 * @param name The name of recipe to set.
	 */
    public void setName(String name) {
    	if(name != null) {
    		this.name = name;
    	}
	}
	/**
	 * @return Returns the price of beverage.
	 */
    public int getPrice() {
		return price;
	}
	/**
	 * @param price The price of beverage to set.
	 */
    public void setPrice(int price) {
		if (price >= 0) {
			this.price = price;
		}
	}
	/**
	 * @param r Recipe to compare with.
	 * @return Return true value if the recipe in the parameter is equal to this recipe, otherwise return false.
	 */
	public boolean equals(Recipe r) {
		if ((this.name).equals(r.getName())) {
			return true;
		}
		return false;
	}

	/**
	 * Returns a string representation of this Recipe
	 *
	 * @return a string representation
	 */
	public String toString() {
		return name;
	}

	/**
	 * @param context Context needed to set recipe fields.
	 * @return Returns the recipe.
	 */
	static public Recipe getRecipe(Context context) throws ContextException {
		Recipe r = new Recipe();
		r.name = (String) context.getValue("name");
		r.price = (int) context.getValue("price");
		r.amtCoffee = (int) context.getValue("amtCoffee");
		r.amtMilk = (int) context.getValue("amtMilk");
		r.amtSugar = (int) context.getValue("amtSugar");
		r.amtChocolate = (int) context.getValue("amtChocolate");
		return r;
	}

	/**
	 * @param recipe Recipe to set context values.
	 * @return Returns the context based on <code>recipe</code> fields.
	 * @throws ContextException if any of <code>recipe</code> fields is null.
	 */
	static public Context getContext(Recipe recipe) throws ContextException {
		Context cxt = new ServiceContext();
		cxt.putValue("name", recipe.getName());
		cxt.putValue("price", recipe.getPrice());
		cxt.putValue("amtCoffee", recipe.getAmtCoffee());
		cxt.putValue("amtMilk", recipe.getAmtMilk());
		cxt.putValue("amtSugar", recipe.getAmtSugar());
		cxt.putValue("amtChocolate", recipe.getAmtChocolate());
		return cxt;
	}


}
