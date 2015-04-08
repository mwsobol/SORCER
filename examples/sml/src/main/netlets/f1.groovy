
codebase artifact:org.sorcer/arithmetic/jar/dl/5.2.0
//@Grab(group='org.sorcer', module='arithmetic', version='5.2.0', classifier='prv')

import sorcer.arithmetic.provider.Adder
import sorcer.arithmetic.provider.Multiplier
import sorcer.arithmetic.provider.Subtractor
import sorcer.core.provider.Jobber


Task f4 = task("f4",
        sig("multiply", Multiplier.class),
        context("multiply", inEnt("arg/x1", 10.0d), inEnt("arg/x2", 50.0d), result("result/y1")));

Task f5 = task("f5",
        sig("add", Adder.class),
        context("add", inEnt("arg/x3", 20.0d), inEnt("arg/x4", 80.0d), result("result/y2")));

Task f3 = task("f3",
        sig("subtract", Subtractor.class),
        context("subtract", inEnt("arg/x5"), inEnt("arg/x6"), result("result/y3")));

job("f1", sig("service", Jobber.class),
        job("f2", f4, f5), f3,
        pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
        pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));
