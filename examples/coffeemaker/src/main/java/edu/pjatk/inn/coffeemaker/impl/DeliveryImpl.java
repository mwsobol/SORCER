package edu.pjatk.inn.coffeemaker.impl;

import edu.pjatk.inn.coffeemaker.Delivery;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski on 8/29/15.
 */
public class DeliveryImpl implements Delivery {

    @Override
    public Context deliver(Context context) throws RemoteException, ContextException {
        // from/to any place the flat rate $0.60
        Integer flatRate = 60;
        context.putValue("delivery/cost", flatRate);
        if (context.getValue("coffee/change") != null) {
            context.putValue("change$", ((Integer) context.getValue("coffee/change")) - flatRate);
        }

        if (context.getContextReturn() != null) {
            context.setReturnValue(flatRate);
        }
        return context;
    }
}
