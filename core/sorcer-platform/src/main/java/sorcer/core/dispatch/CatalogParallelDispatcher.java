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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.exertion.Mograms;
import sorcer.service.Exerter;
import sorcer.service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.*;

import static sorcer.service.Exec.*;

public class CatalogParallelDispatcher extends CatalogExertDispatcher {
    private final Logger logger = LoggerFactory.getLogger(CatalogParallelDispatcher.class);
    protected ExecutorService executor = Executors.newCachedThreadPool();

    public CatalogParallelDispatcher(Job job,
            Set<Context> sharedContexts,
            boolean isSpawned, 
            Exerter provider,
            ProvisionManager provisionManager) {
		super(job, sharedContexts, isSpawned, provider, provisionManager);
	}

    @Override
    public void exec(Arg... args) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                CatalogParallelDispatcher.super.exec(args);
            }
        });
    }

    public void doExec(Arg... args) throws RoutineException,
			SignatureException {
        List<Future<Routine>> results = new ArrayList<Future<Routine>>(inputXrts.size());
        for (Mogram mogram : inputXrts) {
            if (mogram instanceof Routine)
                results.add(executor.submit(new ExecExertion((Routine)mogram)));
		}

        boolean isFailed = false;
        boolean isSuspended = false;
        for (Future<Routine> result : results) {
            try {
                ServiceRoutine se = (ServiceRoutine) result.get();
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
			RoutineException fe = new RoutineException(this.getClass().getName()
					+ " failed job", xrt);
			xrt.reportException(fe);
			dispatchers.remove(xrt.getId());
			throw fe;
		}
		else if (isSuspended) {
			xrt.setStatus(SUSPENDED);
			state = SUSPENDED;
			RoutineException fe = new RoutineException(this.getClass().getName()
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
			// finally exert Master Routine
			masterXrt = (ServiceRoutine) execExertion(masterXrt);
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

    protected class ExecExertion implements Callable<Routine> {
        private final Routine exertion;

        public ExecExertion(Routine exertion) {
            this.exertion = exertion;
        }

        @Override
        public Routine call() throws Exception {
            return execExertion(exertion);
        }
	}
}
