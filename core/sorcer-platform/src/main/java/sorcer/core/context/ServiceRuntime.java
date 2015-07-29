package sorcer.core.context;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.service.*;
import sorcer.util.FileURLHandler;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Mike Sobolewski
 */
public class ServiceRuntime implements Projection<String>, Serializable {

    protected List<ThrowableTrace> exceptions;

    private List<String> traceList;

    private boolean isMonitorable = false;

    private Mogram target;

    protected transient FileURLHandler dataService;

    // dependency management for this Context
    protected List<Evaluation> dependers = new ArrayList<Evaluation>();

    protected String currentSelector;

    // mapping from paths of this inConnector to input paths of this context
    protected Context inConnector;

    // mapping from paths of this context to input paths of requestors
    protected Context outConnector;

    protected Map<String, List<String>> dependentPaths;

    protected Fidelity<String> selectedFidelity;

    // select fidelities for this service context
    protected Map<String, Fidelity<String>> selectFidelities;

    // evaluated model response entries
    protected Context outcome;

    protected Exec.State execState = Exec.State.INITIAL;

    // reponse paths of the runtime model
    protected List<String> responsePaths = new ArrayList<String>();

    public ServiceRuntime(Mogram service) {
        target = service;
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

    public String getCurrentSelector() {
        return currentSelector;
    }

    public void setCurrentSelector(String currentSelector) {
        this.currentSelector = currentSelector;
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

    public void setSelectFidelities(Map<String, Fidelity<String>> selectFidelities) {
        this.selectFidelities = selectFidelities;
    }

    public Context getOutcome() {
        return outcome;
    }

    public void setResult(String path, Object value) throws ContextException {
        if (!responsePaths.contains(path))
            throw new ContextException("no such response path: " + path);
        outcome.putValue(path, value);
    }

    public List<String> getResponsePaths() {
        return responsePaths;
    }

    public void setResponsePaths(List<String> responsePaths) {
        this.responsePaths = responsePaths;
    }

    public <T extends Mogram> Mogram exert(Transaction txn, Arg... entries) throws TransactionException, MogramException, RemoteException {
        return target.exert(txn, entries);
    }

    public <T extends Mogram> Mogram exert(Arg... entries) throws TransactionException, MogramException, RemoteException {
        return target.exert(entries);
    }

    public Mogram getTarget() {
        return target;
    }

    public void setTarget(Mogram target) {
        this.target = target;
    }

    @Override
    public Fidelity<String> getFidelity() {
        return selectedFidelity;
    }

    @Override
    public void setFidelity(Fidelity<String> fidelity) {
        selectedFidelity = fidelity;
    }

    public void appendTrace(String info) {
        traceList.add(info);
    }

    public List<String> getTrace() {
        return traceList;
    }

    public FileURLHandler getDataService() {
        return dataService;
    }

    public void setDataService(FileURLHandler dataService) {
        this.dataService = dataService;
    }

    public Exec.State getExecState() {
        return execState;
    }

}
