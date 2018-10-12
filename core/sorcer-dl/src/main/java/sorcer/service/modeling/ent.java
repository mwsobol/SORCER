package sorcer.service.modeling;

import sorcer.service.Arg;
import sorcer.service.Evaluation;
import sorcer.service.Identifiable;
import sorcer.service.Service;


public interface ent<V> extends Service, Evaluation<V>, Identifiable, Arg {
}
