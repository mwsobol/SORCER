package sorcer.core.invoker;

import sorcer.service.Context;
import sorcer.service.Evaluator;

import java.util.ArrayList;
import java.util.List;

public class Pipeline extends ServiceInvoker<Context> {
    private List<Evaluator> evaluators = new ArrayList<>();

    public Pipeline(Evaluator... evaluators) {
        this(null, evaluators);
    }

    public Pipeline(String name, Evaluator... evaluators) {
        super(name);
        for (Evaluator eval : evaluators) {
            this.evaluators.add(eval);
        }
    }

    public Pipeline(List<Evaluator> evaluators) {
        this(null, evaluators);
    }

    public Pipeline(String name, List<Evaluator> evaluators) {
        super(name);
        this.evaluators = evaluators;
    }

    public List<Evaluator> getEvaluators() {
        return evaluators;
    }

    public void setEvaluators(List<Evaluator> evaluators) {
        this.evaluators = evaluators;
    }


}
