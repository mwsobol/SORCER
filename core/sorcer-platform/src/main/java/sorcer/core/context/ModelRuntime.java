package sorcer.core.context;

import sorcer.core.SelectFidelity;
import sorcer.service.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Mike Sobolewski
 */
public class ModelRuntime implements SelectProjection, Serializable {

    protected List<ThrowableTrace> exceptions;

    private List<String> traceList;

    private boolean isMonitorable = false;

     private ServiceContext target;

    // dependency management for this Context
    protected List<Evaluation> dependers = new ArrayList<Evaluation>();

    // mapping from paths of this inConnector to input paths of this context
    protected Context inConnector;

    // mapping from paths of this context to input paths of requestors
    protected Context outConnector;

    protected Map<String, List<String>> dependentPaths;

    protected SelectFidelity selectFidelity;

    // select fidelities for this service context
    protected Map<String, SelectFidelity> selectFidelities;

    // evaluated model response entries
    protected Context outcome;

    // reponse paths of the runtime model
    protected List<String> responsePaths = new ArrayList<String>();

    ModelRuntime(ServiceContext context) {
        target = context;
    }

    public void setExceptions(List<ThrowableTrace> exceptions) {
        this.exceptions = exceptions;
    }

    public boolean isMonitorable() {
        return isMonitorable;
    }

    public void setIsMonitorable(boolean isMonitorable) {
        this.isMonitorable = isMonitorable;
    }

    public List<ThrowableTrace> getExceptions() {
        exceptions = new ArrayList<ThrowableTrace>();
        if (exceptions != null)
            return exceptions;
        else
            return new ArrayList<ThrowableTrace>();
    }

    public List<String> getTraceList() {
        return traceList;
    }

    public void setTraceList(List<String> traceList) {
        this.traceList = traceList;
    }

    public List<ThrowableTrace> getAllExceptions() {
        return getExceptions();
    }

    public Map<String, List<String>> getDependentPaths() {
        if (dependentPaths == null) {
            dependentPaths = new HashMap<String, List<String>>();
        }
        return dependentPaths;
    }

    public Context getInConnector(Arg... arg) {
        return inConnector;
    }

    public void setInConnector(Context inConnector) {
        this.inConnector = inConnector;
    }


    public Context getOutConnector(Arg... args) {
        return outConnector;
    }

    public void setOutConnector(Context outConnector) {
        this.outConnector = outConnector;
    }


    public void addDependers(Evaluation... dependers) {
        if (this.dependers == null)
            this.dependers = new ArrayList<Evaluation>();
        for (Evaluation depender : dependers)
            this.dependers.add(depender);
    }

    public List<Evaluation> getDependers() {
        return dependers;
    }

    @Override
    public SelectFidelity getSelectFidelity() {
        return selectFidelity;
    }

    @Override
    public void setSelectFidelity(SelectFidelity fidelity) {
        selectFidelity= fidelity;
    }

    public Map<String, SelectFidelity> getSelectFidelities() {
        return selectFidelities;
    }

    public void setSelectFidelities(Map<String, SelectFidelity> selectFidelities) {
        this.selectFidelities = selectFidelities;
    }

    public Context getOutcome() {
        return outcome;
    }

    public void setResult(String path, Object value) throws ContextException {
        if (!responsePaths.contains(path))
            throw new ContextException("no such response path: " + path);
        target.putValue(path, value);
    }

    public List<String> getResponsePaths() {
        return responsePaths;
    }

    public void setResponsePaths(List<String> responsePaths) {
        this.responsePaths = responsePaths;
    }
}
