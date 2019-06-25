package sorcer.provider.exchange.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.ServiceExerter;
import sorcer.service.Exerter;
import sorcer.provider.exchange.Exchange;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Created by sobolemw on 8/5/15.
 */
public class ExchangeBean implements Exchange, Serializable {

    private static Logger logger = LoggerFactory.getLogger(ExchangeBean.class.getName());

    private ServiceExerter provider;

    public void init(Exerter provider) {
        this.provider = (ServiceExerter)provider;
    }

    @Override
    public Context exchange(Context context) throws RemoteException, ContextException {
        int[] input = (int[])context.getValue("values");
        context.putValue("values", build(input));
        return context;
    }

    @Override
    public int[] exchange(int[] input) throws RemoteException {
        return build(input);
    }

    private  int[] build(int[] input) {
        for (int n = 0; n < input.length; n++) {
            input[n] += 1;
        }
        return input;
    }
}
