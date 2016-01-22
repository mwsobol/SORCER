package edu.pjatk.inn.coffeemaker.impl;

import sorcer.core.context.ServiceContext;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.io.Serializable;

/**
 * COPYRIGHT (C) 2016 Sarah & Mike. All Rights Reserved.
 * Recipe class for the coffee maker.
 *
 * @author   Sarah & Mike
 */

public class Recipe implements Serializable {
    private String name;
    private int price;
    private int amtCoffee;
    private int amtMilk;
    private int amtSugar;
    private int amtChocolate;

	/**
	 * Constructor for the Recipe class.
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
	 * Returns the amount of chocolate.
	 *
	 * @return int The amount of chocolate.
	 */
    public int getAmtChocolate() {
		return amtChocolate;
	}

    /**
	 * Sets the amount of chocolate to the recipe if
	 * amtChocolate is greater than or equal to 0
	 *
	 * @param amtChocolate The amount of chocolate to set.
	 */
    public void setAmtChocolate(int amtChocolate) {
		if (amtChocolate >= 0) {
			this.amtChocolate = amtChocolate;
		} 
	}

    /**
	 * Returns the amount of coffee.
	 *
	 * @return int The amount of coffee.
	 */
    public int getAmtCoffee() {
		return amtCoffee;
	}

    /**
	 * Sets the amount of coffee to the recipe if amtCoffee
	 * is greater than or equal to 0.
	 *
	 * @param amtCoffee The amount of coffee to set.
	 */
    public void setAmtCoffee(int amtCoffee) {
		if (amtCoffee >= 0) {
			this.amtCoffee = amtCoffee;
		} 
	}

    /**
	 * Return the amount of milk.
	 * @return   Returns the amtMilk, if amtMilk
	 * is greater than or equal to 0.
	 *
	 */
    public int getAmtMilk() {
		return amtMilk;
	}

    /**
	 * Set the amount of milk to the recipe.
	 * @param amtMilk   The amtMilk to set.
	 */
    public void setAmtMilk(int amtMilk) {
		if (amtMilk >= 0) {
			this.amtMilk = amtMilk;
		} 
	}

    /**
	 * Returns the amount of sugar.
	 *
	 * @return  int  Returns the amtSugar.
	 */
    public int getAmtSugar() {
		return amtSugar;
	}

    /**
	 * Sets the amount of sugar to the recipe if amtSugar
	 * is greater than or equal to 0.
	 *
	 * @param amtSugar   The amtSugar to set.
	 */
    public void setAmtSugar(int amtSugar) {
		if (amtSugar >= 0) {
			this.amtSugar = amtSugar;
		} 
	}

    /**
	 * Returns the name.
	 *
	 * @return   Returns String the name.
	 *
	 */
    public String getName() {
		return name;
	}

    /**
	 * Set the name if name not equal to NULL(empty field).
	 *
	 * @param name   The name to set.
	 */
    public void setName(String name) {
    	if(name != null) {
    		this.name = name;
    	}
	}

    /**
	 * Return the price.
	 *
	 * @return   Returns the price.
	 */
    public int getPrice() {
		return price;
	}

    /**
	 * Set the price if price is greater than or equal to 0.
	 *
	 * @param price   The price to set.
	 */
    public void setPrice(int price) {
		if (price >= 0) {
			this.price = price;
		} 
	}

	/**
	 * Return boolean(true/false) of equals of object Recipe type
	 * @param r  Recipe object
	 *           r.getName name of recipe
	 * @return boolean
	 */

    public boolean equals(Recipe r) {
        if((this.name).equals(r.getName())) {
            return true;
        }
        return false;
    }

	/**
	 * Return string value of name
	 * @return String
	 * */
    public String toString() {
    	return name;
    }

	/**
	 * Returns recipe for coffee of the given Context.
	 *
	 * @param context The context to create recipe from.
	 * @return Recipe The Recipe created by the given context.
	 * @throws ContextException
     */
	static public Recipe getRecipe(Context context) throws ContextException {
		Recipe r = new Recipe();
		r.name = (String)context.getValue("name");
		r.price = (int)context.getValue("price");
		r.amtCoffee = (int)context.getValue("amtCoffee");
		r.amtMilk = (int)context.getValue("amtMilk");
		r.amtSugar = (int)context.getValue("amtSugar");
		r.amtChocolate = (int)context.getValue("amtChocolate");
		return r;
	}

	/**
	 * Returns the context of the given recipe.
	 *
	 * @param recipe The recipe to create context from.
	 * @return Context The context created by the given recipe.
	 * @throws ContextException
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
