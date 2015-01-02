// Adder netlet for testing the requestor of the examples/server project


import sorcer.service.Strategy

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import sorcer.provider.adder.*;

Double v1 = 100.0;
Double v2 = 200.0;

task("hello adder", sig("add",
                        Adder.class,
                        deploy(configuration("org.sorcer:adder:config:5.0"))),
     strategy(Strategy.Provision.YES),
     context("adder", inEnt("arg/x1", v1), inEnt("arg/x2", v2), result("out/y")));


