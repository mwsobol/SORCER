package edu.pjatk.inn.requestor;

import edu.pjatk.inn.coffeemaker.CoffeeService;
import edu.pjatk.inn.coffeemaker.Delivery;
import sorcer.core.requestor.ServiceRequestor;
import sorcer.service.*;
import sorcer.service.Domain;

import java.io.File;

import static sorcer.co.operator.*;
import static sorcer.co.operator.paths;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.po.operator.*;
import static sorcer.so.operator.exert;

public class CoffeemakerRequestor extends ServiceRequestor {

    public Mogram getMogram(String... args) throws MogramException {

        String option = "exertion";
        if (args != null && args.length == 2) {
            option = args[1];
        } else if (this.args != null) {
            option = this.args[0];
        } else {
            throw new MogramException("wrong arguments for: ExertRequestor fiType, mogram fiType");
        }
        try {
            if (option.equals("netlet")) {
                return (Exertion) evaluate(new File("src/main/netlets/coffeemaker-exertion-remote.ntl"));
            } else if (option.equals("model")) {
                return createModel();
            } else if (option.equals("exertion")) {
                return createExertion();
            }
        } catch (Throwable e) {
            throw new MogramException(e);
        }
        return null;
    }

    private Context getEspressoContext() throws ContextException {
        return context(val("key", "espresso"), val("price", 50),
            val("amtCoffee", 6), val("amtMilk", 0),
            val("amtSugar", 1), val("amtChocolate", 0));
    }

    private Task getRecipeTask() throws MogramException, SignatureException {
        // make sure we have a recipe for required coffee
       return task("recipe", sig("addRecipe", CoffeeService.class), getEspressoContext());
    }

    private Exertion createExertion() throws Exception {
        Task coffee = task("coffee", sig("makeCoffee", CoffeeService.class), context(
            val("recipe/key", "espresso"),
            val("coffee/paid", 120),
            val("coffee/change"),
            val("recipe", getEspressoContext())));

        Task delivery = task("delivery", sig("deliver", Delivery.class), context(
            val("location", "PJATK"),
            val("delivery/paid"),
            val("room", "101")));

        Job drinkCoffee = job(coffee, delivery,
            pipe(outPoint(coffee, "coffee/change"), inPoint(delivery, "delivery/paid")));

        return drinkCoffee;
    }

    private Domain createModel() throws Exception {
        exert(getRecipeTask());

        // order espresso with delivery
        Domain mdl = srvModel(
            val("recipe/key", "espresso"),
            val("paid$", 120),
            val("location", "PJATK"),
            val("room", "101"),

                srv(sig("makeCoffee", CoffeeService.class,
                        result("coffee$", inPaths("recipe/key")))),
                srv(sig("deliver", Delivery.class,
                        result("delivery$", inPaths("location", "room")))));
//				proc("change$", invoker("paid$ - (coffee$ + delivery$)", ents("paid$", "coffee$", "delivery$"))));

        add(mdl, proc("change$", invoker("paid$ - (coffee$ + delivery$)", ents("paid$", "coffee$", "delivery$"))));
        dependsOn(mdl, dep("change$", paths("makeCoffee")), dep("change$", paths("deliver")));
        responseUp(mdl, "makeCoffee", "deliver", "change$", "paid$");

        return mdl;
    }

}