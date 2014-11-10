package sorcer.worker.provider;

import java.io.Serializable;

import sorcer.service.Context;
import sorcer.service.ContextException;

public interface Work extends Serializable {

    @SuppressWarnings("rawtypes")
	public Context exec(Context context) throws InvalidWork, ContextException;
}
