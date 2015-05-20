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

import java.util.Set;

import sorcer.core.provider.Provider;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.service.*;

public class SpaceSequentialDispatcher extends SpaceParallelDispatcher {

	public SpaceSequentialDispatcher(Exertion job,
            Set<Context> sharedContexts,
            boolean isSpawned,
            LokiMemberUtil myMemberUtil,
            Provider provider,
            ProvisionManager provisionManager) throws ExertionException, ContextException  {
		super(job, sharedContexts, isSpawned, myMemberUtil, provider, provisionManager);
	}

    protected void dispatchExertion(Exertion exertion) throws ExertionException, SignatureException {
        super.dispatchExertion(exertion);
		waitForExertion(exertion);
	}

    protected synchronized void waitForExertion(Exertion exertion) {
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
