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
import java.util.List;
import java.util.Properties;

/**
 * Created by vital on 01/28/16.
 */
@SuppressWarnings("rawtypes")
public class ScannerImpl implements Scanner {

    private static final String latteCode = Scanner.latteCode;
    private static final String cappuccinoCode = Scanner.cappuccinoCode;
    private static final String chocoCode = Scanner.chocoCode;

    public static final String RESULT_PATH = "result/value";

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
        List<String> outpaths = cxt.getOutPaths();
        logger.info("outpaths: " + outpaths);
        Recipe recipe = decode(inputs.get(0));
        // update the service context
        if (provider != null)
            cxt.putValue("scanner/provider", provider.getProviderName());
        else
            cxt.putValue("scanner/provider", getClass().getName());
        if (cxt.getReturnPath() != null) {
            cxt.setReturnValue(recipe);
        } else if (outpaths.size() == 1) {
            // put the result in the existing output path
            cxt.putValue(outpaths.get(0), recipe);
        } else {
            cxt.putValue(RESULT_PATH, recipe);
        }

        return cxt;
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
