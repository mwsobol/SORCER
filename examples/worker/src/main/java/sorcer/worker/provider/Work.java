package sorcer.worker.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.io.Serializable;

public interface Work extends Serializable {

    @SuppressWarnings("rawtypes")
	public Context exec(Context context) throws InvalidWork, ContextException;
}
