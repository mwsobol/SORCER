package sorcer.service;

public class DisciplineFidelity extends Fidelity<Fidelity> {

    private Fidelity govFi;


    private Fidelity cxtFi;

    public DisciplineFidelity(Fidelity govFi, Fidelity dsptFi) {
        this.govFi = govFi;
        this.select = dsptFi;
    }

    public DisciplineFidelity(Fidelity cxtMultiFi, Fidelity govFi, Fidelity dsptFi) {
        this.govFi = govFi;
        this.select = dsptFi;
    }

    public Fidelity getContextFi() {
        return cxtFi;
    }

    public void setContextFi(Fidelity contextMultiFi) {
        this.cxtFi = contextMultiFi;
    }

    public Fidelity getGovernanceFi() {
        return govFi;
    }

    public void setGovernanceFi(Fidelity govFi) {
        this.govFi = govFi;
    }

    public Fidelity getDispatcherFi() {
        return select;
    }

    public void setDispatcherFi(Fidelity dsptFi) {
        this.select = dsptFi;
    }
}
