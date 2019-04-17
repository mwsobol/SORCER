package sorcer.service;

public class DisciplineFidelity extends Fidelity<Fidelity> {

    private Fidelity govFi;

    public DisciplineFidelity(Fidelity dsptFi, Fidelity govFi) {
        this.govFi = govFi;
        this.select = dsptFi;
    }

    public Fidelity getGovernanceFidelity() {
        return govFi;
    }

    public void setGovernanceFidelity(Fidelity govFi) {
        this.govFi = govFi;
    }

    public Fidelity getDispatcherFidelity() {
        return select;
    }

    public void setDispatcherFidelity(Fidelity dsptFi) {
        this.select = dsptFi;
    }
}
