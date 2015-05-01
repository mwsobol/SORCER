package sorcer.core.context.model.srv;

import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A ProductMogram is a mogram with multiple projections of the product of services
 * (mograms) that can be treated as service fidelities selectable at runtime from
 * multiple mograms available. A fidelity is associated with results of a single mogram
 * or multiple mograms in its service model returnig a service contexts of a requested
 * fidelity.
 *
 * Created by Mike Sobolewski
 */
public class SrvProduct extends SrvModel {

    // service fidelities for this model
    private Map<String, SelectionFidelity> selectionFidelities;

    private SelectionFidelity selectedFidelity;

    public SrvProduct() {
    }

    public SrvProduct(String name) throws SignatureException {
        super(name);
    }

    public SrvProduct(Signature signature) {
        super(signature);
    }

    public SrvProduct(String name, Signature signature) throws SignatureException {
        super(name, signature);
    }

    /**
     * Returns the current values of atributes (fidelites) of a given projection.
     *
     * @return the projection values of this evaluation
     * @throws EvaluationException
     * @throws RemoteException
     */
    public SrvProduct setProjection(String... fidelities) throws EvaluationException, RemoteException {
        for (String path : fidelities)
            addResponsePath(path);
        return this;
    }

    public void setSelectionFidelities(String fidelityName) {
        SelectionFidelity fi = selectionFidelities.get(fidelityName);
        currentSelector = fi.getName();
    }

    public void setSelectionFidelities(SelectionFidelity fidelity) {
        selectionFidelities.put(fidelity.getName(), fidelity);
        currentSelector = fidelity.getName();
    }

    public SelectionFidelity getSelectionFidelity() {
        return selectionFidelities.get(currentSelector);
    }

    public void addSelectionFidelities(List<SelectionFidelity> fidelities) {
        for (SelectionFidelity fi : fidelities)
            addSelectionFidelity(fi);
    }

    public void addSelectionFidelities(SelectionFidelity... fidelities) {
        for (SelectionFidelity fi : fidelities)
            addSelectionFidelity(fi);
    }

    public void addSelectionFidelity(SelectionFidelity fidelity) {
        if (selectionFidelities == null)
            selectionFidelities = new HashMap<String, SelectionFidelity>();
        selectionFidelities.put(fidelity.getName(), fidelity);
    }

    public void selectSelectionFidelity(String fidelity) throws ExertionException {
        if (fidelity != null && selectionFidelities != null
                && selectionFidelities.containsKey(fidelity)) {
            currentSelector = selectionFidelities.get(fidelity).getName();
            responsePaths.clear();
            responsePaths.add(currentSelector);
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
            Mogram mogram = (Mogram) getValue(responsePaths.get(0));
            mogram = mogram.exert(entries);
            if (mogram instanceof Exertion)
                return ((Exertion)mogram).getContext();
            else
                return getResponses();
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }

    @Override
    public Object getResponse(String fidelity, Arg... entries) throws ContextException {
        try {
            selectSelectionFidelity(fidelity);
        } catch (ExertionException e) {
            throw new ContextException(e);
        }
        return getValue(entries);
    }

    public SelectionFidelity getSelectedSelectionFidelity() {
        return selectedFidelity;
    }

    public void setSelectedSelectionFidelity(SelectionFidelity selectedFidelity) {
        this.selectedFidelity = selectedFidelity;
    }

}
