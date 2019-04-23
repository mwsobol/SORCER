package sorcer.service;

public class DisciplineFidelity extends Fidelity<Fidelity> {

    private Fidelity govFi;


    private Fidelity cxtMultiFi;

    public DisciplineFidelity(Fidelity govFi, Fidelity dsptFi) {
        this.govFi = govFi;
        this.select = dsptFi;
    }

    public DisciplineFidelity(Fidelity cxtMultiFi, Fidelity govFi, Fidelity dsptFi) {
        this.govFi = govFi;
        this.select = dsptFi;
    }

    public Fidelity getContextMultiFi() {
        return cxtMultiFi;
    }

    public void setContextMultiFi(Fidelity contextMultiFi) {
        this.cxtMultiFi = contextMultiFi;
    }

    public Fidelity getGovernanceMultiFi() {
        return govFi;
    }

    public void setGovernanceMultiFi(Fidelity govFi) {
        this.govFi = govFi;
    }

    public Fidelity getDispatcherMultiFi() {
        return select;
    }

    public void setDispatcherMultiFi(Fidelity dsptFi) {
        this.select = dsptFi;
    }
}
