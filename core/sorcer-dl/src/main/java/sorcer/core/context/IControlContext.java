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

package sorcer.core.context;

import sorcer.service.Context;
import sorcer.service.Strategy;

/**
 * @author Rafał Krupiński
 */
public interface IControlContext extends Context<Object>, Strategy {
    boolean isMonitorable();

    void isMonitorable(Strategy.Monitor value);

    void setMonitorable(boolean state);

    boolean isProvisionable();

    void setProvisionable(boolean state);

    void setOpti(Strategy.Opti optiType);

    Strategy.Opti getOpti();

    void addException(Throwable t);

    void addException(String message, Throwable t);
}
