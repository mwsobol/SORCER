/*
 * Copyright 2014 Sorcersoft.com S.A.
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

package sorcer.platform.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.MDC;
import sorcer.core.provider.RemoteLogger;

/**
 * Filter logging events based on existence of a {@link org.slf4j.MDC} key.
 *
 * @author Rafał Krupiński
 */
class MDCFilter extends Filter<ILoggingEvent> {
    public final static MDCFilter instance = new MDCFilter(RemoteLogger.LOGGER_CONTEXT_KEY);
    private String loggerContextKey;

    public MDCFilter(String loggerContextKey) {
        this.loggerContextKey = loggerContextKey;
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        return (MDC.get(loggerContextKey) != null) ? FilterReply.ACCEPT : FilterReply.DENY;
    }
}
