package sorcer.worker.provider;

import java.io.Serializable;

import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.worker.provider.impl.InvalidWork;

public interface Work extends Serializable {

    @SuppressWarnings("rawtypes")
	public Context exec(Context context) throws InvalidWork, ContextException;
}
