package sorcer.service;

import sorcer.co.tuple.ExecDependency;
import sorcer.core.service.Governance;
import sorcer.service.modeling.Discipline;
import sorcer.service.modeling.Exploration;
import sorcer.service.modeling.ExploreException;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import static sorcer.co.operator.path;

public class GovernanceExplorer implements Service, Exploration {

    protected Governance governance;
    // exec discipline dependencies
    protected Map<String, List<ExecDependency>> dependentDisciplines;

    public GovernanceExplorer() {
        // do nothing
    }

    public GovernanceExplorer(Governance governance) {
        this.governance = governance;
    }

    public Map<String, List<ExecDependency>> getDependentDisciplines() {
        return dependentDisciplines;
    }

    public void setDependentDisciplines(Map<String, List<ExecDependency>> dependentDisciplines) {
        this.dependentDisciplines = dependentDisciplines;
    }

    @Override
    public Object execute(Arg... args) throws ServiceException {
//        try {
//            List<Fidelity> fis = Arg.selectFidelities(args);
//            if (fis != null && fis.size() > 0) {
//                ((ServiceFidelity)governance.getMultiFi()).selectFi(fis.get(0));
//            }
//            Subroutine xrt = (Subroutine) governance.getDispatcher();
//            if (governance.input != null) {
//                if (governance.inConnector != null) {
//                    xrt.setContext(((ServiceContext) governance.input).updateContextWith(governance.inConnector));
//                } else {
//                    xrt.setContext(governance.input);
//                }
//            }
//            xrt.dispatch(governance.getOut());
//            governance.setOutput(); = xrt.evaluate(input);
            execDependencies(governance.getName(), args);
            return governance.getOutput();
//        } catch (ConfigurationException | RemoteException e) {
//            throw new ServiceException(e);
//        }
    }

    public void execDependencies(String path, Arg... args) throws ContextException {
        Map<String, List<ExecDependency>> dpm = dependentDisciplines;
        if (dpm != null && dpm.get(path) != null) {
            List<ExecDependency> del = dpm.get(path);
            Discipline dis = governance.getDiscipline(path);
            if (del != null && del.size() > 0) {
                for (ExecDependency de : del) {
                    List<Path> dpl = (List<Path>) de.getImpl();
                    if (dpl != null && dpl.size() > 0) {
                        for (Path p : dpl) {
                            try {
                                governance.getDiscipline(p.path).execute(args);
                            } catch (ServiceException e) {
                                e.printStackTrace();
                            } catch (RemoteException e) {
                                throw new ContextException(e);
                            }
                        }
                    }

                }
            }
        }
    }

    @Override
    public Context explore(Context searchContext, Arg... args) throws ExploreException, RemoteException {
        try {
            governance.setInput(searchContext);
            return (Context) execute(args);
        } catch (ServiceException e) {
            throw new ExploreException(e);
        }
    }
}
