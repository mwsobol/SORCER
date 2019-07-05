package sorcer.core.dispatch;

import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import sorcer.service.*;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SORCER class
 * User: Pawel Rubach
 * Date: 23.10.13
 *
 * Sort a list of mograms taking into account the dependencies in pipes and
 * the tasks and jobs hierarchy. The sorter also determines the Flow for jobs
 * that have the Flow setValue to AUTO.
 *
 * This code is based on the ProjectSorter class from Apache Maven 2
 */
public class ExertionSorter {

    private static Logger logger = LoggerFactory.getLogger(ExertionSorter.class.getName());
    private final DAG dag;
    private final Map projectMap;
    private final Map<String, String> contextIdsMap;
    private final Map<String, String> revContextIdsMap;
    private List<Mogram> sortedProjects = null;
    private Subroutine topLevelJob;

    /**
     * Construct the ExertionSorter
     */
    public ExertionSorter(Subroutine topLevelJob) throws ContextException, SortingException {

        dag = new DAG();
        projectMap = new HashMap();
        contextIdsMap = new HashMap<String, String>();
        revContextIdsMap = new HashMap<String, String>();
        this.topLevelJob = topLevelJob;

        addVertex(this.topLevelJob);

        try {
            getMapping(this.topLevelJob);
            checkParentCycle(this.topLevelJob);

            List sortedProjects = new ArrayList();
            for (Iterator i = TopologicalSorter.sort(dag).iterator(); i.hasNext(); ) {
                String id = (String) i.next();
                sortedProjects.add(projectMap.get(id));
            }

            this.sortedProjects = Collections.unmodifiableList(sortedProjects);
            reorderJob(this.topLevelJob, this.sortedProjects);
        } catch (CycleDetectedException ce) {
            throw new SortingException(ce.getMessage());
        }
    }

    /**
     * Return the reordered job
     *
     * @return
     */
    public Subroutine getSortedJob() {
        return topLevelJob;
    }


    /**
     * Helper method to build a tree of all exertion IDs in a tree - required by setFlow
     * @param sortedSubXrt
     * @return
     */
    private List<String> addSubExertions(List<Mogram> sortedSubXrt) {
        List<String> sortedSubsetIds = new ArrayList<String>();
        for (Mogram xrt : sortedSubXrt) {
            sortedSubsetIds.add(xrt.getId().toString());
            if (xrt instanceof Job)
                sortedSubsetIds.addAll(addSubExertions(((Job) xrt).getMograms()));
        }
        return sortedSubsetIds;
    }


    /**
     * Determine the Flow (PAR or SEQ) for mograms that have the Flow setValue to AUTO
     *
     * @param topXrt
     * @param sortedSubXrt
     * @return
     */
    private Strategy.Flow setFlow(Subroutine topXrt, List<Mogram> sortedSubXrt) {
        List<String> sortedSubsetIds = addSubExertions(sortedSubXrt);

        int edges = 0;
        for (Mogram xrt : topXrt.getMograms()) {
            for (String depId : dag.getParentLabels(xrt.getId().toString())) {
                if (sortedSubsetIds.contains(depId)) {
                    edges++;
                    logger.debug("Edge: " + xrt.getName() + " parent: " + depId);
                }
            }
            for (String depId : dag.getChildLabels(xrt.getId().toString())) {
                if (sortedSubsetIds.contains(depId)) {
                    edges++;
                    logger.debug("Edge: " + xrt.getName() + " child: " + depId);
                }
            }
        }
        if (topXrt.getMograms().size() > 0)
            logger.debug("XRT " + topXrt.getName() + " has edges: " + edges);
        if (edges == 0) return Strategy.Flow.PAR;
        else return Strategy.Flow.SEQ;
    }

    /**
     * Actually rearrange the mograms in the job according to the sorting
     *
     * @param topXrt
     * @param sortedExertions
     * @throws CycleDetectedException
     * @throws ContextException
     */
    private void reorderJob(Subroutine topXrt, List<Mogram> sortedExertions) {
        List<Mogram> sortedSubset = new ArrayList(sortedExertions);
        sortedSubset.retainAll(topXrt.getMograms());

        if (topXrt.getFlowType()!=null && topXrt.getFlowType().equals(Strategy.Flow.AUTO)) {
            ((ServiceRoutine) topXrt).setFlowType(setFlow(topXrt, sortedSubset));
            logger.info("FLOW for exertion: " + topXrt.getName() + " set to: " + topXrt.getFlowType());
        }
        List<String> exertionsBefore = new ArrayList<String>();
        for (Mogram xrt : topXrt.getMograms())
                exertionsBefore.add(xrt.getName());

        List<String> exertionsAfter = new ArrayList<String>();
        for (Mogram xrt : sortedExertions)
            exertionsAfter.add(xrt.getName());
        if (!topXrt.getMograms().equals(sortedSubset)) {
            logger.info("Order of mograms for " + topXrt.getName() + " will be changed: ");
            logger.info("From: " + exertionsBefore);
            logger.info("To: " + exertionsAfter);
            topXrt.getMograms().removeAll(sortedSubset);
            topXrt.getMograms().addAll(sortedSubset);
        }


        for (Iterator i = topXrt.getMograms().iterator(); i.hasNext(); ) {
            Subroutine xrt = (Subroutine) i.next();
            if (xrt instanceof Job) {
                reorderJob(xrt, sortedExertions);
            }
        }
    }

    /**
     * Add the job and all inner mograms as vertexes
     *
     * @param topXrt
     * @throws SortingException
     */
    private void addVertex(Subroutine topXrt) throws ContextException, SortingException {

        String id = topXrt.getId().toString();
        dag.addVertex(id);
        projectMap.put(id, topXrt);
        contextIdsMap.put(id, topXrt.getDataContext().getId().toString());
        revContextIdsMap.put(topXrt.getDataContext().getId().toString(), id);

        for (Iterator i = topXrt.getMograms().iterator(); i.hasNext(); ) {
            Subroutine project = (Subroutine) i.next();

            id = project.getId().toString();

            if (dag.getVertex(id) != null) {
                throw new SortingException("Subroutine '" + project.getName() +
                        "'(" + id + ") is duplicated in the job: '" + topXrt.getName() + "' (" + topXrt.getId() + ")");
            }

            dag.addVertex(id);
            projectMap.put(id, project);
            contextIdsMap.put(id, project.getDataContext().getId().toString());
            revContextIdsMap.put(project.getDataContext().getId().toString(), id);

            if (project instanceof Job) {
                addVertex(project);
            }
        }
    }

    /**
     * Find the dependencies that result from the pipes specified between tasks
     *
     * @param topXrt
     * @throws CycleDetectedException
     * @throws SortingException
     */
    private void getMapping(Subroutine topXrt) throws CycleDetectedException, ContextException, SortingException {
        for (Iterator i = topXrt.getMograms().iterator(); i.hasNext(); ) {
            Subroutine project = (Subroutine) i.next();
            String id = project.getId().toString();
            String topId = topXrt.getId().toString();
            dag.addEdge(id, topId);

            Map<String, Map<String, String>> metaCtx = project.getDataContext().getMetacontext();
            Map<String, String> ctxMapping = metaCtx.get("cid");
            if (ctxMapping != null) {
                for (Map.Entry<String, String> mapping : ctxMapping.entrySet()) {
                    if (mapping.getValue() != null && mapping.getValue().length() > 0) {
                        String dependencyId = revContextIdsMap.get(mapping.getValue());
                        logger.debug("Map: " + mapping.getKey() + " to " + dependencyId);
                        if (dag.getVertex(dependencyId) != null) {
                            dag.addEdge(id, dependencyId);
                        }
                    }
                }
            }
            if (project instanceof Job) {
                getMapping(project);
            }
        }
    }

    /**
     * Check if there is an edge between the exertion and its parent
     *
     * @param topXrt
     * @throws CycleDetectedException
     * @throws SortingException
     */
    private void checkParentCycle(Subroutine topXrt) throws CycleDetectedException, ContextException, SortingException {
        if (topXrt.getDataContext().getParentId() != null) {
            String parentId = topXrt.getDataContext().getParentId().toString();
            if (dag.getVertex(parentId) != null) {
                // Parent is added as an edge, but must not cause a cycle - so we remove any other edges it has in conflict
                if (dag.hasEdge(parentId, topXrt.getId().toString())) {
                    dag.removeEdge(parentId, topXrt.getId().toString());
                }
                dag.addEdge(topXrt.getId().toString(), parentId);
            }
        }
    }
}

