package sorcer.service.modeling;

import java.util.Map;

public interface Transdiscipline extends Discipline {

    public Map<String, Discipline> getDisciplines();

    public Discipline getDiscipline(String name);

}
