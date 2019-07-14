package edu.pjatk.inn.requestor;

import edu.pjatk.inn.coffeemaker.CoffeeService;
import edu.pjatk.inn.coffeemaker.Delivery;
import edu.pjatk.inn.coffeemaker.impl.CoffeeMaker;
import edu.pjatk.inn.coffeemaker.impl.DeliveryImpl;
import sorcer.core.requestor.ServiceConsumer;
import sorcer.service.*;
import sorcer.service.ContextDomain;

import java.io.File;

import static sorcer.co.operator.*;
import static sorcer.co.operator.paths;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.*;
import static sorcer.so.operator.exert;

public class CoffeemakerConsumer extends ServiceConsumer {

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
            if (option.equals("block")) {
                return createExertionBlock();
            } else if (option.equals("remoteBlock")) {
                return createExertionRemoteBlock();
            } else if (option.equals("model")) {
                return createModel();
            } else if (option.equals("job")) {
                return createExertionJob();
            } else if (option.equals("netlet")) {
                return (Mogram) evaluate(new File("src/main/netlets/coffeemaker-exertion-remote.ntl"));
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

    private Mogram createExertionBlock() throws Exception {
        Task coffee = task("coffee", sig("makeCoffee", CoffeeMaker.class), context(
            inVal("recipe/key", "espresso"),
            inVal("coffee/paid", 120),
            outPaths("coffee/change"),
            val("recipe", getEspressoContext())));

        Task delivery = task("delivery", sig("deliver", DeliveryImpl.class), context(
            inVal("location", "PJATK"),
            inVal("room", "101"),
            outPaths("coffee/change", "delivery/cost", "change$")));

        Block drinkCoffee = block(context(inVal("coffee/paid", 120), val("coffee/change")), coffee, delivery);

        return drinkCoffee;
    }

    private Mogram createExertionRemoteBlock() throws Exception {
        Task coffee = task("coffee", sig("makeCoffee", CoffeeService.class), context(
            inVal("recipe/key", "espresso"),
            inVal("coffee/paid", 120),
            outPaths("coffee/change"),
            val("recipe", getEspressoContext())));

        Task delivery = task("delivery", sig("deliver", Delivery.class), context(
            inVal("location", "PJATK"),
            inVal("room", "101"),
            outPaths("coffee/change", "delivery/cost", "change$")));

        Block drinkCoffee = block(context(inVal("coffee/paid", 120), val("coffee/change")), coffee, delivery);

        return drinkCoffee;
    }

    private Mogram createExertionJob() throws Exception {
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

    private ContextDomain createModel() throws Exception {
        exert(getRecipeTask());

        // order espresso with delivery
        ContextDomain mdl = srvModel(
            val("recipe/key", "espresso"),
            val("paid$", 120),
            val("location", "PJATK"),
            val("room", "101"),

                srv(sig("makeCoffee", CoffeeService.class,
                        result("coffee$", inPaths("recipe/key")))),
                srv(sig("deliver", Delivery.class,
                        result("delivery$", inPaths("location", "room")))));
//				prc("change$", invoker("paid$ - (coffee$ + delivery$)", ents("paid$", "coffee$", "delivery$"))));

        add(mdl, prc("change$", invoker("paid$ - (coffee$ + delivery$)", ents("paid$", "coffee$", "delivery$"))));
        dependsOn(mdl, dep("change$", paths("makeCoffee")), dep("change$", paths("deliver")));
        responseUp(mdl, "makeCoffee", "deliver", "change$", "paid$");

        return mdl;
    }

}