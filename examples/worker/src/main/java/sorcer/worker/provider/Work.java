package sorcer.worker.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.io.Serializable;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public interface Work extends Serializable {

	public Context exec(Context context) throws InvalidWork,
			ContextException;
}
