package edu.pjatk.inn.requestor;

import edu.pjatk.inn.coffeemaker.CoffeeService;
import edu.pjatk.inn.coffeemaker.Delivery;
import edu.pjatk.inn.coffeemaker.Scanner;
import edu.pjatk.inn.coffeemaker.impl.Recipe;
import edu.pjatk.inn.coffeemaker.impl.ScannerImpl;
import sorcer.core.provider.Jobber;
import sorcer.core.requestor.ExertRequestor;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import java.io.File;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.responseUp;
import static sorcer.po.operator.invoker;

public class CoffeeMakerExertRequestor extends ExertRequestor {

    public Mogram getMogram(String... args) throws MogramException {

        String option = "exertion";
        if (args != null && args.length == 2) {
            option = args[1];
        } else if (this.args != null) {
            option = this.args[0];
        } else {
            throw new MogramException("wrong arguments for: ExertRequestor type, mogram type");
        }
        try {
            if (option.equals("netlet")) {
                return (Exertion) evaluate(new File("src/main/netlets/coffeemaker-exertion-remote.ntl"));
            } else if (option.equals("model")) {
                return createModel();
            } else if (option.equals("exertion")) {
                return createExertion();
            }
        } catch (Exception e) {
            throw new MogramException(e);
        }
        return null;
    }

    private Context getEspressoContext() throws ContextException {
        return context(ent("name", "espresso"), ent("price", 50),
                ent("amtCoffee", 6), ent("amtMilk", 0),
                ent("amtSugar", 1), ent("amtChocolate", 0));
    }

    private Task getRecipeTask() throws MogramException, SignatureException {
        // make sure we have a recipe for required coffee
       return task("recipe", sig("addRecipe", CoffeeService.class), getEspressoContext());
    }

    private Exertion createExertion() throws Exception {
       /*
        Task coffee = task("coffee", sig("makeCoffee", CoffeeService.class), context(
                ent("recipe/name", "espresso"),
                ent("coffee/paid", 120),
                ent("coffee/change"),
                ent("recipe", getEspressoContext())));

        Task delivery = task("delivery", sig("deliver", Delivery.class), context(
                ent("location", "PJATK"),
                ent("delivery/paid"),
                ent("room", "101")));

        Job drinkCoffee = job(coffee, delivery,
                pipe(outPoint(coffee, "coffee/change"), inPoint(delivery, "delivery/paid")));*/


        String v1 = "c1,m1,s2,ch0,p123";
        int v2 = 200;

        Context cxt = context("scanner", inEnt("code", v1));
        Task scanner = task("hello scanner", sig("scan", Scanner.class), cxt);

        Task coffee = task("coffee", sig("makeCoffee", CoffeeService.class), context(
                ent("recipe/name", "latte"),
                ent("coffee/paid", 200),
                ent("coffee/change"),
                ent("recipe")));

        Task delivery = task("delivery", sig("deliver", Delivery.class), context(
                ent("location", "PJATK"),
                ent("delivery/paid"),
                ent("room", "room 110A")));
        if ((Boolean) exert(scanner).getValue("result/status")) {
            Job drinkCoffee = job(scanner, coffee, delivery,
                    pipe(outPoint(scanner, "result/value"), inPoint(coffee, "recipe")),
                    pipe(outPoint(coffee, "coffee/change"), inPoint(delivery, "delivery/paid"))
            );
            return drinkCoffee;
        }else{
            return exert(scanner);
        }

    }

    private Model createModel() throws Exception {
        exert(getRecipeTask());

        // order espresso with delivery
        Model mdl = model(
                ent("recipe/name", "espresso"),
                ent("paid$", 120),
                ent("location", "PJATK"),
                ent("room", "101"),

                srv(sig("makeCoffee", CoffeeService.class,
                        result("coffee$", inPaths("recipe/name")))),
                srv(sig("deliver", Delivery.class,
                        result("delivery$", inPaths("location", "room")))));
//				ent("change$", invoker("paid$ - (coffee$ + delivery$)", ents("paid$", "coffee$", "delivery$"))));

        add(mdl, ent("change$", invoker("paid$ - (coffee$ + delivery$)", ents("paid$", "coffee$", "delivery$"))));
        dependsOn(mdl, ent("change$", "makeCoffee"), ent("change$", "deliver"));
        responseUp(mdl, "makeCoffee", "deliver", "change$", "paid$");

        return mdl;
    }

}