/*
 * Copyright 2015 the original author or authors.
 * Copyright 2015 Sorcersoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.core.dispatch;

import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.dag.Vertex;
import sorcer.co.tuple.SignatureEntry;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.Function;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.context.model.srv.SrvModel;
import sorcer.core.dispatch.graph.DirectedGraph;
import sorcer.core.dispatch.graph.DirectedGraphRenderer;
import sorcer.core.dispatch.graph.GraphNodeRenderer;
import sorcer.service.*;

import java.util.*;

import static sorcer.co.operator.dep;
import static sorcer.co.operator.dependsOn;
import static sorcer.ent.operator.ent;
import static sorcer.co.operator.paths;

/**
 * SORCER class
 * User: Pawel Rubach
 * Date: 23.11.2015
 *
 * Sort a list of args in a model taking into account the dependencies
 */
public class SrvModelAutoDeps {

    private final DAG dag;
    private final Map entryMap;
    private final Map<String, String> entryToResultMap;
    private SrvModel srvModel;
    private List<String> sortedData;
    private List<String> topNodes = new ArrayList<String>();

    /**
     * Construct the SrvModelAutoDeps
     */
    public SrvModelAutoDeps(SrvModel srvModel) throws SortingException, ContextException {

        dag = new DAG();
        entryMap = new HashMap();
        entryToResultMap = new HashMap<String, String>();
        this.srvModel = srvModel;
        addVertex(this.srvModel);

        try {
            getMapping(this.srvModel);
            sortedData = new ArrayList<String>();
            for (Iterator i = TopologicalSorter.sort(dag).iterator(); i.hasNext(); ) {
                sortedData.add((String) i.next());
            }
            addDependsOn(this.srvModel, Collections.unmodifiableList(sortedData));
        } catch (CycleDetectedException ce) {
            throw new SortingException(ce.getMessage());
        }
    }

    /**
     * Return the processed SrvModel
     * @return srvModel
     */
    public SrvModel get() {
        return srvModel;
    }

    /**
     * Return the processed SrvModel
     * @return srvModel
     */
    public String printDeps() {
        DirectedGraphRenderer<String> graphRenderer = new DirectedGraphRenderer<String>(new GraphNodeRenderer<String>() {
            public void renderTo(String node, StringBuilder output) {
                //output.append(entryToResultMap.getValue(node));
                output.append(node);
            }
        }, new DirectedGraph<String, Object>() {
            public void getNodeValues(String node, Collection<? super Object> values, Collection<? super String> connectedNodes) {
                for (String dependency : sortedData) {
                    Vertex vertex = dag.getVertex(node);

                    if (vertex.getParentLabels().contains(dependency)) {
                        connectedNodes.add(dependency);
                    }
                }
            }
        });
        StringBuilder writer = new StringBuilder("\n");
        if (topNodes.size()>0) {
            for (String node : topNodes)
                graphRenderer.renderTo(node, writer);
        } else {
            for (String node : sortedData)
                graphRenderer.renderTo(node, writer);
        }
        return writer.toString();
    }


    /**
     * Add dependency information to the srvModel
     *
     * @param srvModel
     * @throws CycleDetectedException
     * @throws ContextException
     */
    private void addDependsOn(SrvModel srvModel, List<String> sortedEntries) throws ContextException {
        for (String entryName : sortedEntries) {
            // Only those that are args in the srvModel
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
                if (paths.size()>0) {
                    dependsOn(srvModel, dep(entryName, paths(paths.toArray())));
                    String topNode = entryName;
                    if (entryToResultMap.containsKey(entryName))
                        topNode = entryToResultMap.get(entryName);
                    if (!topNodes.contains(topNode))
                        topNodes.add(entryName);
                }
            }
        }
    }

    /**
     * Add SrvModel args as Vertexes to the Directed Acyclic Graph (DAG)
     *
     * @param srvModel
     * @throws SortingException
     */
    private void addVertex(SrvModel srvModel) throws SortingException {

        for (String entryName : srvModel.getData().keySet()) {

            if (dag.getVertex(entryName) != null
                    && entryMap.get(entryMap)!=null
                    && (!srvModel.getData().get(entryName).equals(entryMap.get(entryMap)))) {
                        throw new SortingException("Entry named: '" + entryName +
                            " is duplicated in the model: '" + srvModel.getName() + "(" + srvModel.getId() + ")" +
                            "'\n" + entryName + "=" + entryMap.get(entryMap)
                            + "\n" + entryName + "=" + srvModel.getData().get(entryName));
            }

            dag.addVertex(entryName);
            entryMap.put(entryName, srvModel.getData().get(entryName));

            Object entry = srvModel.getData().get(entryName);
            if (entry instanceof Entry) {
                Object entryVal = ((Entry)entry).getImpl();
                Context.Return rp = null;
                if (entryVal instanceof SignatureEntry) {
                    Signature signature = (Signature) ((SignatureEntry)entryVal).getImpl();
                    if (signature!=null) rp = (Context.Return)signature.getContextReturn();
                } else if (entry instanceof Srv) {
                    rp = ((Srv) entry).getReturnPath();
                }
                if (rp!=null) {
                    dag.addVertex(rp.getName());
                    entryToResultMap.put(rp.getName(), entryName);
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
    private void getMapping(SrvModel srvModel) throws CycleDetectedException, SortingException {
        for (String entryName : srvModel.getData().keySet()) {
            Object entry = srvModel.getData().get(entryName);
            if (entry instanceof Function) {
                Context.Return rp = null;
                Object entryVal = ((Entry)entry).getImpl();
                if (entryVal instanceof SignatureEntry) {
                    Signature signature = (Signature) ((SignatureEntry)entryVal).getImpl();
                    rp =  (Context.Return)signature.getContextReturn();
                } else if (entry instanceof Srv) {
                    rp = ((Srv)entry).getReturnPath();
                }
                if (rp!=null) {
                    dag.addEdge(rp.getName(), entryName);
                    if (rp.inPaths != null) {
                        for (Path inPath : rp.inPaths) {
                            dag.addEdge(inPath.path, rp.getName());
                        }
                    }
                }
            }
        }
    }
}

