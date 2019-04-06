package sorcer.pml.modeling;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.EntModel;
import sorcer.core.context.model.ent.SysCall;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.SysCaller;
import sorcer.core.provider.caller.SysCallerProvider;
import sorcer.pml.provider.impl.Volume;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.util.Sorcer;
import sorcer.util.exec.ExecUtils.CmdResult;

import java.io.File;
import java.io.StringReader;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/pml")
public class SysCalls {
	private final static Logger logger = LoggerFactory.getLogger(SysCalls.class);

	@Test
	public void systemCmdInvoker() throws Exception {
		String riverVersion = System.getProperty("river.version");
		String sorcerVersion = System.getProperty("sorcer.version");
		String slf4jVersion = System.getProperty("slf4j.version");
		String logbackVersion = System.getProperty("logback.version");
		String buildDir = System.getProperty("project.build.dir");

        String cp = buildDir + "/libs/pml-" + sorcerVersion + "-bean.jar" + File.pathSeparator
        		+ Sorcer.getHome() + "/lib/sorcer/lib/sorcer-platform-" + sorcerVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/slf4j-api-" + slf4jVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-core-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-classic-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-platform-" + riverVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-lib-" + riverVersion + ".jar ";

		ServiceInvoker cmd = cmdInvoker("volume",
				"java -cp  " + cp + Volume.class.getName() + " cylinder");

		EntModel pm = entModel(call(cmd),
				val("x", 10.0), val("y"),
				pro("multiply", invoker("x * y", args("x", "y"))),
				pro("add", invoker("x + y", args("x", "y"))));

		CmdResult result = (CmdResult) invoke(pm, "volume");
		// get from the result the volume of cylinder and assign to y parameter
		assertTrue("EXPECTED '0' return eval, GOT: "+result.getExitValue(), result.getExitValue() == 0);
		Properties props = new Properties();
		props.load(new StringReader(result.getOut()));
        setValue(pm, "y", new Double(props.getProperty("cylinder/volume")));

        logger.info("x exec:" + exec(pm, "x"));
        logger.info("y exec:" + exec(pm, "y"));
        logger.info("multiply exec:" + exec(pm, "add"));
        assertTrue(value(pm, "add").equals(47.69911184307752));
		logger.info("x exec:" + exec(pm, "x"));
		logger.info("y exec:" + exec(pm, "y"));
		logger.info("multiply exec:" + exec(pm, "add"));
		assertTrue(exec(pm, "add").equals(47.69911184307752));
	}

    @Test
    public void systemCall() throws Exception {
        String riverVersion = System.getProperty("river.version");
        String sorcerVersion = System.getProperty("sorcer.version");
        String slf4jVersion = System.getProperty("slf4j.version");
        String logbackVersion = System.getProperty("logback.version");
        String buildDir = System.getProperty("project.build.dir");

        String cp = buildDir + "/libs/pml-" + sorcerVersion + "-bean.jar" + File.pathSeparator
                + Sorcer.getHome() + "/lib/sorcer/lib/sorcer-platform-" + sorcerVersion + ".jar"  + File.pathSeparator
                + Sorcer.getHome() + "/lib/logging/slf4j-api-" + slf4jVersion + ".jar"  + File.pathSeparator
                + Sorcer.getHome() + "/lib/logging/logback-core-" + logbackVersion + ".jar"  + File.pathSeparator
                + Sorcer.getHome() + "/lib/logging/logback-classic-" + logbackVersion + ".jar"  + File.pathSeparator
                + Sorcer.getHome() + "/lib/river/jsk-platform-" + riverVersion + ".jar"  + File.pathSeparator
                + Sorcer.getHome() + "/lib/river/jsk-lib-" + riverVersion + ".jar ";

        Model pm = entModel(val("x", 10.0), args("y"),
				pro("multiply", invoker("x * y", args("x", "y"))),
				pro("add", invoker("x + y", args("x", "y"))));

        SysCall caller = sysCall("volume", cxt(val("cmd", "java -cp  " + cp + Volume.class.getName()),
                inVal("cylinder"), outVal("cylinder/volume"), outVal("cylinder/radius"),
				outVal("cylinder/height")));
        add(pm, caller);

		Context result = (Context) eval(pm, "volume");
//		Context result = (Context) invoke(pm, "volume");
        // get from the result the volume of cylinder and assign to y parameter
        assertTrue("EXPECTED '0' return eval, GOT: "+value(result, "exit/eval"),
                value(result, "exit/eval").equals(0));

		setValue(pm, "y", new Double((String)value(result, "cylinder/volume")));

        logger.info("cylinder/radius:" + eval(result, "cylinder/radius"));
		logger.info("cylinder/height:" + eval(result, "cylinder/height"));
		logger.info("x exec:" + exec(pm, "x"));
		logger.info("y exec:" + exec(pm, "y"));
        logger.info("multiply exec:" + exec(pm, "add"));
        assertTrue(exec(pm, "add").equals(47.69911184307752));
    }

	@Test
	public void systemCallerTask() throws Exception {
		String riverVersion = property("river.version");
		String sorcerVersion = property("sorcer.version");
		String slf4jVersion = property("slf4j.version");
		String logbackVersion = property("logback.version");
		String buildDir = property("project.build.dir");

		String cp = buildDir + "/libs/pml-" + sorcerVersion + "-bean.jar" + File.pathSeparator
				+ Sorcer.getHome() + "/lib/sorcer/lib/sorcer-platform-" + sorcerVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/slf4j-api-" + slf4jVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-core-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-classic-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-platform-" + riverVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-lib-" + riverVersion + ".jar ";

		Task callerTask = task("volume", sig("exec", SysCaller.class),
						cxt(val("cmd", "java -cp  " + cp + Volume.class.getName()),
								inVal("cylinder"), outVal("cylinder/volume"), outVal("cylinder/radius"),
								outVal("cylinder/height")));

		Context out = context(exert(callerTask));
		logger.info("out:" + out);
		assertTrue(value(out, "cylinder/height").equals("3.0"));
		assertTrue(value(out, "cylinder/radius").equals("2.0"));
		assertTrue(value(out, "cylinder/volume").equals("37.69911184307752"));
	}

	@Test
	public void systemCaller() throws Exception {
		String riverVersion = System.getProperty("river.version");
		String sorcerVersion = System.getProperty("sorcer.version");
		String slf4jVersion = System.getProperty("slf4j.version");
		String logbackVersion = System.getProperty("logback.version");
		String buildDir = System.getProperty("project.build.dir");

		String cp = buildDir + "/libs/pml-" + sorcerVersion + "-bean.jar" + File.pathSeparator
				+ Sorcer.getHome() + "/lib/sorcer/lib/sorcer-platform-" + sorcerVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/slf4j-api-" + slf4jVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-core-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-classic-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-platform-" + riverVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-lib-" + riverVersion + ".jar ";

		Model sm = srvModel(val("x", 10.0), val("y"),
				pro("multiply", invoker("x * y", args("x", "y"))),
				pro("add", invoker("x + y", args("x", "y"))),
				result("cylinder/volume"),
				srv("volume", sig("exec", SysCaller.class,
//				srv("volume", sig("exec", SysCallerProvider.class,
						cxt(val("cmd", "java -cp  " + cp + Volume.class.getName()),
								inVal("cylinder"),
								outVal("cylinder/volume"), outVal("cylinder/radius"), outVal("cylinder/height")))));

		String volume = (String) exec(sm, "volume");
		logger.info("volume: " + volume);
		assertTrue(volume.equals("37.69911184307752"));
		assertTrue(exec(sm, "cylinder/height").equals("3.0"));
		assertTrue(exec(sm, "cylinder/radius").equals("2.0"));
		assertTrue(exec(sm, "cylinder/volume").equals("37.69911184307752"));

		// multitype conversion for numbers
		double v = Double.valueOf(volume).doubleValue();
		assertTrue(v == 37.69911184307752);
		volume = Double.toString(v);
		assertTrue(exec(sm, "cylinder/volume").equals(volume));
	}

	@Test
	public void systemCallerWithTypes() throws Exception {
		String riverVersion = System.getProperty("river.version");
		String sorcerVersion = System.getProperty("sorcer.version");
		String slf4jVersion = System.getProperty("slf4j.version");
		String logbackVersion = System.getProperty("logback.version");
		String buildDir = System.getProperty("project.build.dir");

		String cp = buildDir + "/libs/pml-" + sorcerVersion + "-bean.jar" + File.pathSeparator
				+ Sorcer.getHome() + "/lib/sorcer/lib/sorcer-platform-" + sorcerVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/slf4j-api-" + slf4jVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-core-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-classic-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-platform-" + riverVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-lib-" + riverVersion + ".jar ";

		Model sm = srvModel(val("x", 10.0), val("y"),
				ent("multiply", invoker("x * y", args("x", "y"))),
				ent("add", invoker("x + y", args("x", "y"))),
				result("cylinder/volume"),
				srv("volume", sig("exec", SysCallerProvider.class,
//				srv("volume", sig("exec", SysCaller.class,
						cxt(val("cmd", "java -cp  " + cp + Volume.class.getName()),
								inVal("cylinder", Arg.class),
								outVal("cylinder/volume", double.class),
								outVal("cylinder/radius", double.class),
								outVal("cylinder/height", double.class)))));

		Double volume = (Double) exec(sm, "volume");
		logger.info("volume: " + volume);
		assertTrue(volume.equals(37.69911184307752));
		assertTrue(exec(sm, "cylinder/height").equals(3.0));
		assertTrue(exec(sm, "cylinder/radius").equals(2.0));
		assertTrue(exec(sm, "cylinder/volume").equals(37.69911184307752));

		// use values fro system call in the model sm
		setValue(sm, "y", volume);
		logger.info("multiply eval:" + eval(sm, "add"));
		assertTrue(exec(sm, "add").equals(47.69911184307752));
	}

	@Test
	public void classTypes() throws Exception {
		ServiceContext context = (ServiceContext) cxt(inVal("cylinder", String.class),
				inVal("cylinder/height", 3.0, double.class),
				outVal("cylinder/volume", double.class),
				outVal("cylinder/radius", double.class),
				outVal("cylinder/height", double.class));
		String ct = context.getValClass("cylinder/volume");
		logger.info("ct: " + ct);
		logger.info("double?: " + context.isDouble("cylinder/volume"));
        assertTrue(context.isDouble("cylinder/volume"));
	}
}
