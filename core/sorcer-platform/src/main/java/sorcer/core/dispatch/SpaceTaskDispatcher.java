/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.service.*;

import static sorcer.service.Exec.*;

public class SpaceTaskDispatcher extends SpaceParallelDispatcher {

    private final Logger logger = LoggerFactory.getLogger(SpaceTaskDispatcher.class);

	public SpaceTaskDispatcher(final Task task,
            final Set<Context> sharedContexts,
            final boolean isSpawned, 
            final LokiMemberUtil myMemberUtil,
            final ProvisionManager provisionManager) throws ContextException, ExertionException {
        super(task, sharedContexts, isSpawned, myMemberUtil, null, provisionManager);
	}

    @Override
    protected List<Mogram> getInputExertions() throws ContextException {
        return Arrays.asList((Mogram)xrt);
    }

    @Override
    protected void handleResult(Collection<ExertionEnvelop> results) throws ExertionException, SignatureException, RemoteException {
        logger.info("handleResult(): starting....");
        if (results.size() != 1) {
            logger.info("results.size !=1, throwing exception.");
            throw new ExertionException("Invalid number of results (" + results.size() + "), expecting 1");
        }


        Task result = (Task) results.iterator().next().exertion;

        logger.info("setting status to failed for simulation!!!!!!!!!!!!");
        result.setStatus(FAILED);

        int status = result.getStatus();

        logger.info("handleResult(): status = " + status);
        if (status == DONE) {
            state = DONE;
            result.setStatus(DONE);
            xrt = result;

        } else if (status == FAILED) {

            addPoison(xrt); //commented by SAB 2/15/2019 to prevent model hanging
            handleError(result);

            // added below by SAB 2/15/2019 to prevent model hanging
            //state = DONE;
            //xrt = result;
        }
        logger.info("handleResult(): DONE.");
    }
}
