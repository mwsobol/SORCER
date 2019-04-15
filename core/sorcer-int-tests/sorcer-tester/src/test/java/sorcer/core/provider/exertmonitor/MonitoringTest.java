package sorcer.core.provider.exertmonitor;

import net.jini.id.Uuid;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Multiplier;
import sorcer.arithmetic.tester.provider.Subtractor;
import sorcer.core.exertion.TaskTest;
import sorcer.core.provider.Concatenator;
import sorcer.ent.operator;
import sorcer.service.*;
import sorcer.util.GenericUtil;
import sorcer.util.Sorcer;
import sorcer.util.exec.CommonsExecUtil;
import sorcer.util.exec.ExecUtils;

import java.io.IOException;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.value;
import static sorcer.so.operator.*;

/**
 * @author Pawel Rubach
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class MonitoringTest {
	private final static Logger logger = LoggerFactory.getLogger(TaskTest.class);
	private static String nshCmd;
	private static String[] cmds;
	private static final String EXCEPTION = "Exception";

	@BeforeClass
	public static void init() throws IOException {
		nshCmd = new StringBuilder(new java.io.File(Sorcer.getHomeDir(),
				"bin"+ java.io.File.separator + "nsh"
						+ (GenericUtil.isWindows() ? ".bat" : ""))
				.getCanonicalPath()).toString();
		cmds = new String[] { "-c", "emx", "-a"};
	}

	@Test
	public void exertMonitoredSpaceTaskTest() throws Exception {

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0), outVal("result/y")),
				strategy(Strategy.Access.PULL, Strategy.Wait.YES, Strategy.Monitor.YES));

		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		assertNotNull(context(t5).asis("context/checkpoint/time"));
		logger.info("t5 eval: " + get(t5, "result/y"));
		assertEquals("Wrong eval for 100.0", 100d, get(t5, "result/y"));

		verifyExertionMonitorStatus(t5, "DONE");
	}

	@Test
	public void exertMonitoredJobPushParTest() throws Exception {

		Job job = createJob(Strategy.Flow.PAR, Strategy.Access.PUSH);
		job = exert(job);
		logger.info("job j1 job context: " + upcontext(job));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
		verifyExertionMonitorStatus(job, "DONE");
	}

	@Test
	public void exertMonitoredJobPushSeqTest() throws Exception {

		Job job = createJob(Strategy.Flow.SEQ, Strategy.Access.PUSH);
		logger.info("job j1: " + job);
		job = exert(job);
		logger.info("job j1: " + job);
		logger.info("job j1 job context: " + upcontext(job));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
		verifyExertionMonitorStatus(job, "DONE");
	}

	@Test
	public void exertMonitoredJobPullParTest() throws Exception {
		Job job = createJob(Strategy.Flow.PAR, Strategy.Access.PULL);
		job = exert(job);
		logger.info("job j1 job context: " + upcontext(job));
		assertEquals(400d, get(job, "j1/t3/result/y"));
		verifyExertionMonitorStatus(job, "DONE");
	}

	@Test
	public void exertMonitoredJobPullSeqTest() throws Exception {

		Job job = createJob(Strategy.Flow.SEQ, Strategy.Access.PULL);
		job = exert(job);
		logger.info("job j1 job context: " + upcontext(job));
		assertEquals(400d, get(job, "j1/t3/result/y"));
		verifyExertionMonitorStatus(job, "DONE");
	}

	// two level job composition with PULL and PAR execution
	static Job createJob(Strategy.Flow flow, Strategy.Access access) throws Exception {

		Task t3 = task("t3", sig("subtract", Subtractor.class),
				context("subtract", inVal("arg/x1"), inVal("arg/x2"),
						outVal("result/y")), strategy(Strategy.Monitor.YES));

		Task t4 = task("t4", sig("multiply", Multiplier.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y")), strategy(Strategy.Monitor.YES));

		Task t5 = task("t5", sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						outVal("result/y")), strategy(Strategy.Monitor.YES));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job("j1",
				job("j2", t4, t5, strategy(flow, access, Strategy.Monitor.YES)),
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
				strategy(Strategy.Monitor.YES));

		return job;
	}

	@Test
	public void optBlockTest() throws Exception {
		Task t4 = task("t4", sig("multiply", Multiplier.class), strategy(Strategy.Monitor.YES),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("out")));

		Task t5 = task("t5", sig("add", Adder.class), strategy(Strategy.Monitor.YES),
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("out")));

		Block block = block("block", sig(Concatenator.class), strategy(Strategy.Monitor.YES),
				t4,
				opt(condition("{ out -> out > 600 }", "out"), t5));

		block = exert(block);
		logger.info("block context 1: " + context(block));
//		logger.info("result: " + eval(context(block), "out"));
		assertEquals(value(context(block), "out"), 500.0);

		block = exert(block, operator.ent("block/t4/arg/x1", 200.0), operator.ent("block/t4/arg/x2", 800.0));
		logger.info("block context 2: " + context(block));
//		logger.info("result: " + eval(context(block), "out"));
		assertEquals(value(context(block), "out"), 100.0);

	}

	private static void verifyExertionMonitorStatus(Routine exertion, String state) throws Exception {
		ExecUtils.CmdResult result = CommonsExecUtil.execCommand(nshCmd, cmds);

		for (Mogram xrt : exertion.getAllMograms())
			verifyMonitorStatus(result.getOut(), ((Routine)xrt).getId(), "DONE");
	}

	private static void verifyMonitorStatus(String output, Uuid exertionId, String state) {
		int posExertionId = output.indexOf(exertionId.toString());
		if (posExertionId<0) {
			logger.error("Cannot find exertion ID: " + exertionId.toString() + " in:\n" + output);
		}
		assertTrue(posExertionId > 0);
		String cutString = output.substring(posExertionId);
		int lineEnds = cutString.indexOf('\n');
		cutString = cutString.substring(0, lineEnds);
		assertTrue(cutString.contains(state));
	}

}
