/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
 * Copyright 2013 Sorcersoft.com S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.core.dispatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import sorcer.core.exertion.Mograms;
import sorcer.core.provider.Provider;
import sorcer.service.*;

import static sorcer.service.Exec.*;

public class CatalogParallelDispatcher extends CatalogExertDispatcher {
    protected ExecutorService executor = Executors.newCachedThreadPool();

    public CatalogParallelDispatcher(Job job,
            Set<Context> sharedContexts,
            boolean isSpawned, 
            Provider provider,
            ProvisionManager provisionManager) {
		super(job, sharedContexts, isSpawned, provider, provisionManager);
	}

    @Override
    public void exec() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                CatalogParallelDispatcher.super.exec();
            }
        });
    }

    public void doExec() throws ExertionException,
			SignatureException {
        List<Future<Exertion>> results = new ArrayList<Future<Exertion>>(inputXrts.size());
        for (Mogram mogram : inputXrts) {
            if (mogram instanceof Exertion)
                results.add(executor.submit(new ExecExertion((Exertion)mogram)));
		}

        boolean isFailed = false;
        boolean isSuspended = false;
        for (Future<Exertion> result : results) {
            try {
                ServiceExertion se = (ServiceExertion) result.get();
                se.stopExecTime();
                if (se.getStatus() == FAILED)
                    isFailed = true;
                else if (se.getStatus() == SUSPENDED)
                    isSuspended = true;
            } catch (InterruptedException e) {
                logger.warn("Interrupted {}", result, e);
                isFailed = true;
            } catch (ExecutionException e) {
                logger.warn("Error while executing {}", result, e.getCause());
                isFailed = true;
            }
        }
		if (isFailed) {
			xrt.setStatus(FAILED);
			state = FAILED;
			ExertionException fe = new ExertionException(this.getClass().getName()
					+ " failed job", xrt);
			xrt.reportException(fe);
			dispatchers.remove(xrt.getId());
			throw fe;
		}
		else if (isSuspended) {
			xrt.setStatus(SUSPENDED);
			state = SUSPENDED;
			ExertionException fe = new ExertionException(this.getClass().getName()
					+ " suspended job", xrt);
			xrt.reportException(fe);
			dispatchers.remove(xrt.getId());
			throw fe;
		}

		if (masterXrt != null) {
		/*	if (isInterupted(masterXrt)) {
				masterXrt.stopExecTime();
				dispatchers.remove(xrt.getId());
				return;
			}*/
			// finally exert Master Exertion
			masterXrt = (ServiceExertion) execExertion(masterXrt);
			masterXrt.stopExecTime();
			if (masterXrt.getStatus() <= FAILED)
				xrt.setStatus(FAILED);
			else
				xrt.setStatus(DONE);

		}
		xrt.setStatus(DONE);
		dispatchers.remove(xrt.getId());
		state = DONE;
	}

    @Override
    protected List<Mogram> getInputExertions() throws ContextException {
        return Mograms.getInputExertions(((Job) xrt));
    }

    protected class ExecExertion implements Callable<Exertion> {
        private final Exertion exertion;

        public ExecExertion(Exertion exertion) {
            this.exertion = exertion;
        }

        @Override
        public Exertion call() throws Exception {
            return execExertion(exertion);
        }
	}
}
