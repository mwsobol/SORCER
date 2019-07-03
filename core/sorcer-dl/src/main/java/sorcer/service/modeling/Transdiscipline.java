package sorcer.service.modeling;

import sorcer.service.Governance;

import java.util.Map;

public interface Transdiscipline extends Discipline, Governance {

    public Map<String, Discipline> getDisciplines();

    public Discipline getDiscipline(String name);

}
