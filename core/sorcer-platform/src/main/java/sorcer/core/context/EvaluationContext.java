package sorcer.core.context;

import sorcer.service.modeling.Variability;

/**
 * Created by Mike Sobolewski on 5/22/17.
 */
public class EvaluationContext extends ServiceContext<Object> {

    public EvaluationContext() {
        super();
        type = Variability.Type.EVALUATED;
    }

    public EvaluationContext(String name) {
        super(name);
        type = Variability.Type.EVALUATED;
    }
}
