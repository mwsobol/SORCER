package sorcer.core.context.model.srv;

import sorcer.core.SelectFidelity;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A MultiFidelityService is a service with multiple projections of the this model entries.
 * Each projection of thies service model can be treated as a service fidelity selectable
 * at runtime from multiple componet mograms available. A fidelity is associated with
 * the result of a single mogram or multiple mograms of this service model and returnig
 * a merged service context of componet service of this multi-fidelity (compound) service.
 *
 * Created by Mike Sobolewski
 */
public class MultiFidelityService extends SrvModel {

    // service fidelities for this model
    protected Map<String, Fidelity<String>> selectionFidelities;

    protected SelectFidelity selectedFidelity;

    public MultiFidelityService() {
    }

    public MultiFidelityService(String name) throws SignatureException {
        super(name);
    }

    public MultiFidelityService(Signature signature) {
        super(signature);
    }

    public MultiFidelityService(String name, Signature signature) throws SignatureException {
        super(name, signature);
    }

    /**
     * Returns the current values of atributes (fidelites) of a given projection.
     *
     * @return the projection values of this evaluation
     * @throws EvaluationException
     * @throws RemoteException
     */
    public MultiFidelityService setProjection(String... fidelities) throws EvaluationException, RemoteException {
        for (String path : fidelities)
            runtime.getResponsePaths().add(path);
        return this;
    }

    public void setSelectionFidelities(String fidelityName) {
        Fidelity fi = selectionFidelities.get(fidelityName);
        currentSelector = fi.getName();
    }

    public void setSelectionFidelities(Fidelity<String> fidelity) {
        selectionFidelities.put(fidelity.getName(), fidelity);
        currentSelector = fidelity.getName();
    }

    public Fidelity<String> getSelectionFidelity() {
        return selectionFidelities.get(currentSelector);
    }

    public void addSelectionFidelities(List<Fidelity<String>> fidelities) {
        for (Fidelity<String> fi : fidelities)
            addSelectionFidelity(fi);
    }

    public void addSelectionFidelities(Fidelity<String>... fidelities) {
        for (Fidelity<String> fi : fidelities)
            addSelectionFidelity(fi);
    }

    public void addSelectionFidelity(Fidelity<String> fidelity) {
        if (selectionFidelities == null)
            selectionFidelities = new HashMap<String, Fidelity<String>>();
        selectionFidelities.put(fidelity.getName(), fidelity);
    }

    public void selectSelectionFidelity(String fidelity) throws ExertionException {
        if (fidelity != null && selectionFidelities != null
                && selectionFidelities.containsKey(fidelity)) {
            currentSelector = selectionFidelities.get(fidelity).getName();
            runtime.getResponsePaths().clear();
            runtime.getResponsePaths().add(currentSelector);
        }
    }

    @Override
    public Mogram putValue(final String path, Object value) throws ContextException {
        if (path == null)
            throw new IllegalArgumentException("path must not be null");
        if (value instanceof Srv) {
            put(path, value);
        } else {
            throw new IllegalArgumentException("value must Service entry of Srv type");
        }
        return (Mogram)value;
    }

    @Override
    public Context getValue(Arg... entries) throws EvaluationException  {
        try {
            Mogram mogram = (Mogram) getValue(runtime.getResponsePaths().get(0));
            mogram = mogram.exert(entries);
            if (mogram instanceof Exertion)
                return ((Exertion)mogram).getContext();
            else
                return getResponse();
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }

    public Object getResponse(String fidelity, Arg... entries) throws ContextException {
        try {
            selectSelectionFidelity(fidelity);
        } catch (ExertionException e) {
            throw new ContextException(e);
        }
        return getValue(entries);
    }

    public SelectFidelity getSelectedSelectionFidelity() {
        return selectedFidelity;
    }

    public void setSelectedSelectionFidelity(SelectFidelity selectedFidelity) {
        this.selectedFidelity = selectedFidelity;
    }

}
