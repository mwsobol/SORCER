package sorcer.core.monitoring;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dennis Reedy
 */
public class MonitorAgentTest {
    @Test
    public void testMonitorAgent() throws InterruptedException {
        System.setSecurityManager(new SecurityManager());
        System.setProperty("monitor.discovery.timeout", "0");
        MonitorAgent monitorAgent = new MonitorAgent();
        monitorAgent.register("foo", "bar");
        monitorAgent.started();
        monitorAgent.inprocess(null);
        monitorAgent.completed();
        int numTries = 0;
        while(monitorAgent.getMonitorRegistration()==null && numTries<3) {
            Thread.sleep(1000);
            numTries++;
        }
        assertNotNull(monitorAgent.getMonitorRegistration());
    }

}