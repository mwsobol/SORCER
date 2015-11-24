package sorcer.core.dispatch;

import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.dag.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.co.operator;
import sorcer.co.tuple.SignatureEntry;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.srv.SrvModel;
import sorcer.service.*;

import java.util.*;

import static sorcer.co.operator.ent;
import static sorcer.co.operator.paths;

/**
 * SORCER class
 * User: Pawel Rubach
 * Date: 23.11.2015
 *
 * Sort a list of entries in a model taking into account the dependencies
 */
public class SrvModelAutoDeps {

    private static Logger logger = LoggerFactory.getLogger(SrvModelAutoDeps.class.getName());
    private final DAG dag;
    private final Map entryMap;
    private final Map<String, String> entryToResultMap;
    private SrvModel srvModel;

    /**
     * Construct the ExertionSorter
     */
    public SrvModelAutoDeps(SrvModel srvModel) throws ContextException, SortingException {

        dag = new DAG();
        entryMap = new HashMap();
        entryToResultMap = new HashMap<String, String>();
        this.srvModel = srvModel;

        addVertex(this.srvModel);

        try {
            getMapping(this.srvModel);

            List<String> sortedData = new ArrayList<String>();
            for (Iterator i = TopologicalSorter.sort(dag).iterator(); i.hasNext(); ) {
                sortedData.add((String) i.next());
            }
            addDependsOn(this.srvModel, Collections.unmodifiableList(sortedData));
        } catch (CycleDetectedException ce) {
            throw new SortingException(ce.getMessage());
        }
    }

    /**
     * Return the reordered job
     *
     * @return
     */
    public SrvModel get() {
        return srvModel;
    }

    /**
     * Actually rearrange the exertions in the job according to the sorting
     *
     * @param srvModel
     * @throws CycleDetectedException
     * @throws ContextException
     */
    private void addDependsOn(SrvModel srvModel, List<String> sortedEntries) {
        for (String entryName : sortedEntries) {
            // Only those that are entries in the srvModel
            if (!srvModel.getData().keySet().contains(entryName)) continue;
            Vertex vertex = dag.getVertex(entryName);
            if (vertex.getParentLabels() != null && vertex.getParentLabels().size() > 0) {
                List<String> paths = new ArrayList<String>();
                for (String dependent : vertex.getParentLabels()) {
                    if (entryToResultMap.containsKey(dependent)) {
                        Vertex depVertex = dag.getVertex(dependent);
                        if (depVertex.getParentLabels()!=null) {
                            for (String depInternal : depVertex.getParentLabels()) {
                                if (entryToResultMap.containsKey(depInternal)) {
                                    paths.add(entryToResultMap.get(depInternal));
                                }
                            }
                        }
                    }
                }
                if (paths.size()>0) operator.dependsOn(srvModel, ent(entryName, paths(paths.toArray(new String[0]))));
            }
        }
    }

    /**
     * Add the job and all inner exertions as vertexes
     *
     * @param srvModel
     * @throws SortingException
     */
    private void addVertex(SrvModel srvModel) throws ContextException, SortingException {

        for (String entryName : srvModel.getData().keySet()) {
            if (dag.getVertex(entryName) != null) {
                throw new SortingException("Entry named: '" + entryName +
                        " is duplicated in the model: '" + srvModel.getName() + "' (" + srvModel.getId() + ")");
            }

            dag.addVertex(entryName);
            entryMap.put(entryName, srvModel.getData().get(entryName));

            Object entry = srvModel.getData().get(entryName);
            if (entry instanceof Entry) {
                Object entryVal = ((Entry)entry)._2;
                if (entryVal instanceof SignatureEntry) {
                    Signature signature = ((SignatureEntry)entryVal)._2;
                    if (signature.getReturnPath()!=null) {
                        dag.addVertex(signature.getReturnPath().getName());
                        entryToResultMap.put(signature.getReturnPath().getName(), entryName);
                    }

                }
            }
            if (srvModel.getData().get(entryName) instanceof SrvModel) {
                addVertex((SrvModel)srvModel.getData().get(entryName));
            }
        }
    }

    /**
     * Find the dependencies that result from the paths in SignatureEntries for each entry that contains a task
     *
     * @param srvModel
     * @throws CycleDetectedException
     * @throws SortingException
     */
    private void getMapping(SrvModel srvModel) throws CycleDetectedException, ContextException, SortingException {
        for (String entryName : srvModel.getData().keySet()) {
            Object entry = srvModel.getData().get(entryName);
            if (entry instanceof Entry) {
                Object entryVal = ((Entry)entry)._2;
                if (entryVal instanceof SignatureEntry) {
                    Signature signature = ((SignatureEntry)entryVal)._2;
                    if (signature.getReturnPath()!=null) {
                        dag.addEdge(signature.getReturnPath().getName(), entryName);
                        if (signature.getReturnPath().inPaths != null) {
                            for (String inPath : signature.getReturnPath().inPaths) {
                                dag.addEdge(inPath, signature.getReturnPath().getName());
                            }
                        }
                    }
                }
            }
        }
    }
}

