package sorcer.arithmetic.provider.impl;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.DoubleSrv;
import sorcer.core.context.ServiceContext;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.MonitorException;

import java.rmi.RemoteException;
import java.util.List;

public class DoubleAdder implements Adder, DoubleSrv {

    @Override
    public Context add(Context context) throws RemoteException, ContextException, MonitorException {
        // getValue list of input values
        List<Double> inputs = (List<Double>) context.getValue("inputs/double/list");
        Object outpath = context.getValue("outpath");

        double result = 0;
        for (Double value : inputs) {
            result += value;
        }

        if (((ServiceContext) context).getContextReturn() != null) {
            context.setReturnValue(result);
        } else if (outpath != null) {
            context.putValue(outpath.toString(), result);
        } else {
            context.putValue("result", result);
        }
        return context;
    }
}
