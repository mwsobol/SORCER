/*
 * Distribution Statement
 *
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 *
 * Disclaimer
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
 */
package sorcer.core.context.model.ent;

import sorcer.service.Context;
import sorcer.service.Fidelity;
import sorcer.eo.operator.Args;
import sorcer.service.modeling.Functionality;

/**
 * @author Mike Sobolewski
 */
public class NeoFidelity extends Fidelity<Context<Float>> {

    private Args args;

    private Context<Float> weights;

    private Double threshold = null;

    private Double bias = null;

    private boolean isRectified = false;

    public NeoFidelity(String name, Context<Float> weights) {
        fiName = name;
        this.weights = weights;
    }

    public NeoFidelity(String name, Args args, Function... entries) {
        this(name, args, null, entries);
    }

    public NeoFidelity(String name, Context<Float> weights, Function... entries) {
        this(name, null, weights, entries);
    }

    public NeoFidelity(String name, Args args, Context<Float> weights, Function... entries) {
        fiName = name;
        this.args = args;
        this.weights = weights;
        for (Function e : entries) {
            if (e.getType() == Functionality.Type.THRESHOLD) {
                threshold = (double) e.get();
            } else if (e.getType() == Functionality.Type.BIAS) {
                bias = (double) e.get();
            }
        }
    }

    public Args getArgs() {
        return args;
    }

    public void setArgs(Args args) {
        this.args = args;
    }

    public Context<Float> getWeights() {
        return weights;
    }

    public void setWeights(Context<Float> weights) {
        this.weights = weights;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public Double getBias() {
        return bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public boolean isRectified() {
        return isRectified;
    }

    public void setRectified(boolean rectified) {
        isRectified = rectified;
    }
}

