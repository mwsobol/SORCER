package edu.pjatk.inn.coffeemaker.impl;

import edu.pjatk.inn.coffeemaker.QR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.PositionalContext;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vital on 01/28/16.
 */
@SuppressWarnings("rawtypes")
public class QRImpl implements QR {

    public static final String code1="c1,m1,s2,ch0,p123";
    public static final String code2="c2,m1,s1,ch0,p32";
    public static final String code3="c0,m1,s2,ch1,p23";

    public static final String RESULT_PATH = "result/value";

    private ServiceProvider provider;

    private static Logger logger = LoggerFactory.getLogger(QRImpl.class.getName());

    public void init(Provider provider) {
        this.provider = (ServiceProvider)provider;
    }

    @Override
    public Context scan(Context context) throws RemoteException, ContextException {

        logger.info("SCANNER","Started");
        PositionalContext cxt = (PositionalContext) context;

        List<String> inputs = cxt.getInValues();
        logger.info("inputs: " + inputs);
        List<String> outpaths = cxt.getOutPaths();
        logger.info("outpaths: " + outpaths);
/*
        // update the service context
        if (provider != null)
            cxt.putValue("calculated/provider", provider.getProviderName());
        else
            cxt.putValue("calculated/provider", getClass().getName());
        if (context.getReturnPath() != null) {
            context.setReturnValue(decode(code1));
        } else if (outpaths.size() == 1) {
            // put the result in the existing output path
            cxt.putValue( outpaths.get(0), decode(code1));
        } else {
            cxt.putValue(RESULT_PATH, decode(code1));
        }*/

      /*  context.putValue("recipe", decode(code1));
        if (context.getReturnPath() != null) {
            context.setReturnValue(decode(code1));
        }*/
        return cxt;
    }

    protected Recipe decode(String code){

        Recipe recipe = new Recipe();
        String codes[] = code.split("[,]");
        for (String s:codes) {

            recipe.setAmtCoffee(Integer.parseInt(s.split("c")[s.length()-1]));
            recipe.setAmtMilk(Integer.parseInt(s.split("m")[s.length()-1]));
            recipe.setAmtSugar(Integer.parseInt(s.split("s")[s.length()-1]));
            recipe.setAmtChocolate(Integer.parseInt(s.split("c")[s.length()-1]));
            recipe.setPrice(Integer.parseInt(s.split("p")[s.length()-1]));
        }
        return recipe;
    }

}
