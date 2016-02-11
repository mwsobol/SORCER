/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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
package sorcer.service;

import java.util.List;

/**
 * Created by Mike Sobolewski on 8/2/15.
 */
public interface MogramStrategy {

    public boolean isMonitorable();

    public void setMonitorable(boolean state);

    public void setAccessType(Strategy.Access access);

    public void setFlowType(Strategy.Flow flow);

    public boolean isProvisionable();

    public void setProvisionable(boolean state);

    public boolean isTracable();

    public void setTracable(boolean state);

    public void appendTrace(String info);

    public List<String> getTrace();

    public void addException(Throwable t);

    public void addException(String message, Throwable t);

    public void setOpti(Strategy.Opti optiType);

    public Strategy.Access getAccessType();

    public Strategy.Flow getFlowType();

    public Strategy.Opti getOpti();
}
