package sorcer.worker.provider;

import java.io.Serializable;

import sorcer.service.Context;
import sorcer.service.ContextException;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public interface Work extends Serializable {

	public Context exec(Context context) throws InvalidWork,
			ContextException;
}
