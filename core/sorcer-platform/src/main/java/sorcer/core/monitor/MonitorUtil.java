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

package sorcer.core.monitor;

import sorcer.core.context.StrategyContext;
import sorcer.service.*;
import sorcer.util.StringUtils;

import java.rmi.RemoteException;

/**
 * @author Rafał Krupiński
 */
public class MonitorUtil {
    private static final String KEY_MONITORING_SESSION = MonitorUtil.class.getName() + ".MONITORING_SESSION";

    /**
     * Record this context as updated if the related exertion is monitored.
     */
    public static void checkpoint(Context context) throws ContextException {
        Subroutine mxrt = context.getMogram();
        if (mxrt == null)
            return;
        StrategyContext controlContext = mxrt.getControlContext();
        MonitoringSession monSession = getMonitoringSession(controlContext);
        if (controlContext.isMonitorable() && monSession != null) {
            try {
                context.putValue("context/checkpoint/time", StringUtils.getDateTime());
                monSession.changed(context, controlContext, Exec.State.UPDATED.ordinal());
            } catch (Exception e) {
                throw new ContextException(e);
            }
        }
    }

    /**
     * Record this context according to the corresponding aspect if the related
     * exertion is monitored.
     *
     * @throws java.rmi.RemoteException
     * @throws sorcer.service.MonitorException
     */
    public void changed(Context context, Exec.State aspect) throws RemoteException,
            MonitorException {
        Subroutine mxrt = context.getMogram();
        if (mxrt == null)
            return;
        StrategyContext controlContext = mxrt.getControlContext();
        MonitoringSession monSession = getMonitoringSession(controlContext);
        if (mxrt.isMonitorable() && monSession != null) {
            monSession.changed(context, controlContext, aspect.ordinal());
        }
    }

    public static void setMonitorSession(Subroutine exertion, MonitoringSession monitorSession) {
        setMonitoringSession(exertion.getControlContext(), monitorSession);
    }

    public static void setMonitoringSession(StrategyContext controlContext, MonitoringSession monitorSession) {
        try {
            controlContext.putValue(KEY_MONITORING_SESSION, monitorSession);
        } catch (ContextException e) {
            throw new IllegalStateException("Context broken", e);
        }
    }

    public static MonitoringSession getMonitoringSession(Mogram mogram) {
        return getMonitoringSession(((Subroutine)mogram).getControlContext());
    }

    public static MonitoringSession getMonitoringSession(StrategyContext controlContext) {
        try {
            return (MonitoringSession) controlContext.getValue(KEY_MONITORING_SESSION);
        } catch (ContextException | RemoteException e) {
            throw new IllegalStateException("Context broken", e);
        }
    }
}
