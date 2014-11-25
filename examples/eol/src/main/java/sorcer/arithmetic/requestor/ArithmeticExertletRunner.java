package sorcer.arithmetic.requestor;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.codehaus.groovy.control.CompilationFailedException;

import sorcer.core.requestor.ExertletRunner;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.SignatureException;

public class ArithmeticExertletRunner extends ExertletRunner {

	private final static Logger logger = Logger.getLogger(ArithmeticExertletRunner.class.getName());

	/* (non-Javadoc)
	 * @see sorcer.core.requestor.ExertionRunner#getExertion(java.lang.String[])
	 */
	@Override
	public Exertion getExertion(String... args) throws ExertionException, ContextException, SignatureException {
		try {
			exertion = (Exertion)evaluate(new File(getProperty("exertion.filename")));
		} catch (CompilationFailedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return exertion;
	}

	@Override
	public void postprocess(String... args) throws ExertionException, ContextException {
		super.postprocess();
		logger.info("<<<<<<<<<< f5 context: \n" + ((Job)exertion).getExertion("f5").getContext());
	}
}