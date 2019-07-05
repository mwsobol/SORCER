package sorcer.service.modeling;

import sorcer.service.Collaboration;

import java.util.Map;

public interface Transdiscipline extends Discipline, Collaboration {

    public Map<String, Discipline> getDisciplines();

    public Discipline getDiscipline(String name);

}
