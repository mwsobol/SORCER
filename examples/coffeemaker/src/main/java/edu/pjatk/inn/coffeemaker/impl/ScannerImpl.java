package edu.pjatk.inn.coffeemaker.impl;

import edu.pjatk.inn.coffeemaker.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.PositionalContext;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static sorcer.co.operator.ent;
import static sorcer.eo.operator.context;

/**
 * Created by vital on 01/28/16.
 */
@SuppressWarnings("rawtypes")
public class ScannerImpl implements Scanner {

    private static final String latteCode = "c1,m1,s2,ch0,p123,latte";
    private static final String cappuccinoCode = "c2,m1,s1,ch0,p32,Cappuccino";
    private static final String chocoCode = "c0,m1,s2,ch1,p23,Choco";

    protected static List<String> recipesCode;

    public ScannerImpl() {
        recipesCode = new ArrayList<>();
        recipesCode.add(latteCode);
        recipesCode.add(cappuccinoCode);
        recipesCode.add(chocoCode);
    }

    public static final String RESULT_PATH = "result/value";
    public static final String RESULT_STATUS = "result/status";
    private ServiceProvider provider;

    private static Logger logger = LoggerFactory.getLogger(ScannerImpl.class.getName());

    public void init(Provider provider) {
        this.provider = (ServiceProvider) provider;
    }

    @Override
    public Context scan(Context context) throws RemoteException, ContextException {

        logger.info("SCANNER", "Started");
        PositionalContext cxt = (PositionalContext) context;

        List<String> inputs = cxt.getInValues();
        logger.info("inputs: " + inputs);
        List<String> outputs = cxt.getOutPaths();
        logger.info("outputs: " + outputs);


        // update the service context
        if (provider != null)
            cxt.putValue("scanner/provider", provider.getProviderName());
        else
            cxt.putValue("scanner/provider", getClass().getName());
        if (isRecipeCodeExist(inputs.get(0))) {
            Recipe recipe = decode(inputs.get(0));
            cxt.putValue(RESULT_STATUS, true);
            //cxt = (PositionalContext) getRecipeContext(recipe);
            if (cxt.getReturnPath() != null) {
                cxt.setReturnValue(getRecipeContext(recipe));
            } else if (outputs.size() == 1) {
                // put the result in the existing output path
                cxt.putValue(outputs.get(0), getRecipeContext(recipe));
            } else {
                cxt.putValue(RESULT_PATH, getRecipeContext(recipe));
            }
        } else {
            cxt.putValue(RESULT_STATUS, false);
            cxt.putValue(RESULT_PATH, "Sorry, you sent incorrect code!!!");
        }
        return cxt;
    }

    public static Context getRecipeContext(Recipe recipe) throws ContextException {
        return context( ent("name", recipe.getName()), ent("price", recipe.getPrice()),
                ent("amtCoffee", recipe.getAmtCoffee()), ent("amtMilk", recipe.getAmtMilk()),
                ent("amtSugar", recipe.getAmtSugar()), ent("amtChocolate", recipe.getAmtChocolate()));
    }

    protected boolean isRecipeCodeExist(String recipeCode) {
        final boolean[] is = new boolean[1];
        /*recipesCode.forEach(s -> {
            if (s.equals(recipeCode)) {
                is[0] = true;
            } else is[0] = false;
        });*/
        for(int i=0; i<=recipesCode.size()-1;i++){
            if(recipesCode.get(i).equals(recipeCode)){
                is[0]=true;
            }
        }
        return is[0];
    }

    protected Recipe decode(String code) {

        Recipe recipe = new Recipe();
        String codes[] = code.split("[,]");
        recipe.setAmtCoffee(Integer.parseInt(codes[0].split("c")[1]));
        recipe.setAmtMilk(Integer.parseInt(codes[1].split("m")[1]));
        recipe.setAmtSugar(Integer.parseInt(codes[2].split("s")[1]));
        recipe.setAmtChocolate(Integer.parseInt(codes[3].split("ch")[1]));
        recipe.setPrice(Integer.parseInt(codes[4].split("p")[1]));
        recipe.setName(codes[5]);
        return recipe;
    }


}
