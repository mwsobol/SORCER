/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.service.Exerter;
import sorcer.service.*;

import java.util.Set;

public class SpaceSequentialDispatcher extends SpaceParallelDispatcher {
    private final Logger logger = LoggerFactory.getLogger(SpaceSequentialDispatcher.class);

	public SpaceSequentialDispatcher(Routine job,
                                     Set<Context> sharedContexts,
                                     boolean isSpawned,
                                     LokiMemberUtil myMemberUtil,
                                     Exerter provider,
                                     ProvisionManager provisionManager) throws RoutineException, ContextException  {
		super(job, sharedContexts, isSpawned, myMemberUtil, provider, provisionManager);
	}

    protected void dispatchExertion(Routine exertion) throws RoutineException, SignatureException {
        super.dispatchExertion(exertion);
		waitForExertion(exertion);
	}

    protected synchronized void waitForExertion(Routine exertion) {
        while (exertion.getIndex() - getDoneExertionIndex() > -1) {
            try {
                logger.debug("Waiting for exertion: " + exertion.getName() + " to finish index/done: " + exertion.getIndex() + "/" + getDoneExertionIndex());
                wait();
                logger.debug("Finished waiting for exertion: " + exertion.getName() + " to finish index/done: " + exertion.getIndex() + "/" + getDoneExertionIndex());
            } catch (InterruptedException e) {
                // continue
            }
        }
    }
}
