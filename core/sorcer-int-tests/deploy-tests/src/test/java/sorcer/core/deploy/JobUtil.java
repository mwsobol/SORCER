/*
 * Copyright to the original author or authors.
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
package sorcer.core.deploy;

import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Multiplier;
import sorcer.arithmetic.tester.provider.Subtractor;
import sorcer.co.operator;
import sorcer.core.provider.Jobber;
import sorcer.service.*;
import sorcer.service.Strategy.Provision;

import static sorcer.co.operator.outVal;
import static sorcer.eo.operator.*;

/**
 * Utilities for creating jobs
 *
 * @author Dennis Reedy
 */
class JobUtil {

    static Job createJob() throws ContextException, SignatureException, RoutineException {
        return createJob(false);
    }

    static Job createJob(boolean fork) throws ContextException, SignatureException, RoutineException {
        Task f4 = task("f4",
                       sig("multiply",
                           Multiplier.class,
                           deploy(configuration(fork?
                                                getConfigDir()+"/multiplier-prv-fork.config":
                                                "org.sorcer:deploy-tests:config:"+System.getProperty("sorcer.version")),
                                  idle(1),
                                  Deployment.Type.SELF)),
                       context("multiply", operator.inVal("arg/x1", 10.0d),
                               operator.inVal("arg/x2", 50.0d), result("result/y1")));

        Task f5 = task("f5",
                       sig("add",
                           Adder.class,
                           deploy(configuration(getConfigDir()+"/AdderProviderConfig.groovy"))),
                       context("add", operator.inVal("arg/x3", 20.0d), operator.inVal("arg/x4", 80.0d),
                               result("result/y2")));

        Task f3 = task("f3",
                       sig("subtract", Subtractor.class,
                           deploy(maintain(2, perNode(2)),
                                  idle(1),
                                  configuration(getConfigDir()+"/subtractor-prv.config"))),
                       context("subtract", operator.inVal("arg/x5"),
                               operator.inVal("arg/x6"), result("result/y3")));

        return job("f1", sig("exert", Jobber.class, deploy(idle(1))),
                   job("f2", f4, f5), f3,
                   strategy(Provision.YES),
                   pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
                   pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));
    }

    static Job createFixedProvisioningJob() throws SignatureException, ContextException, RoutineException {
        Task f4 = task(
            "f4",
            sig("multiply",
                Multiplier.class,
                deploy(configuration(getConfigDir()+"/multiplier-prv.config"),
                       idle(1),
                       fixed())),
            context("multiply", operator.inVal("arg/x1", 10.0d),
                    operator.inVal("arg/x2", 50.0d), outVal("result/y1", null)));

        Task f5 = task(
            "f5",
            sig("add",
                Adder.class,
                deploy(configuration(getConfigDir()+"/AdderProviderConfig.groovy"))),
            context("add", operator.inVal("arg/x3", 20.0d), operator.inVal("arg/x4", 80.0d),
                    outVal("result/y2", null)));

        Task f3 = task(
            "f3",
            sig("subtract",
                Subtractor.class,
                deploy(maintain(2, fixed()),
                       idle(1),
                       configuration(getConfigDir()+"/subtractor-prv.config"))),
            context("subtract", operator.inVal("arg/x5", null),
                    operator.inVal("arg/x6"), outVal("result/y3")));

        return job("f1", sig("exert", Jobber.class, "Jobber"),
                   job(sig("exert", Jobber.class, "Jobber"), "f2", f4, f5), f3,
                   strategy(Strategy.Provision.YES),
                   pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
                     pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));
    }

    static Job createJobWithIPAndOpSys() throws SignatureException, ContextException, RoutineException {
        String[] opSys = new String[]{"OSX", "Linux"};
        String[] ips = new String[]{"10.131.5.106", "10.131.4.201", "macdna.rb.rad-e.wpafb.af.mil", "10.0.1.9"};
        return createJobWithIPAndOpSys(opSys, "x86_64", ips, false);
    }

    static Job createJobWithIPAndOpSys(String[] opSys,
                                       String arch,
                                       String[] ips,
                                       boolean excludeIPs) throws SignatureException, ContextException, RoutineException {
        Task f4;
        if(excludeIPs) {
            f4 = task("f4",
                      sig("multiply",
                          Multiplier.class,
                          deploy(configuration("org.sorcer:deploy-tests:config:"+System.getProperty("sorcer.version")),
                                 idle(1),
                                 opsys(opSys),
                                 arch(arch),
                                 ipsExclude(ips),
                                 ServiceDeployment.Type.SELF)),
                      context("multiply", operator.inVal("arg/x1", 10.0d),
                              operator.inVal("arg/x2", 50.0d), result("result/y1")));
        } else {
            f4 = task("f4",
                      sig("multiply",
                          Multiplier.class,
                          deploy(configuration("org.sorcer:deploy-tests:config:"+System.getProperty("sorcer.version")),
                                 idle(1),
                                 opsys(opSys),
                                 arch(arch),
                                 ips(ips),
                                 ServiceDeployment.Type.SELF)),
                      context("multiply", operator.inVal("arg/x1", 10.0d),
                              operator.inVal("arg/x2", 50.0d), result("result/y1")));
        }

        Task f5 = task("f5",
                       sig("add",
                           Adder.class,
                           deploy(configuration(getConfigDir()+"/AdderProviderConfig.groovy"))),
                       context("add", operator.inVal("arg/x3", 20.0d), operator.inVal("arg/x4", 80.0d),
                               result("result/y2")));

        Task f3 = task("f3",
                       sig("subtract", Subtractor.class,
                           deploy(maintain(2, perNode(2)),
                                  idle(1),
                                  configuration(getConfigDir()+"/subtractor-prv.config"))),
                       context("subtract", operator.inVal("arg/x5"),
                               operator.inVal("arg/x6"), result("result/y3")));

        return job("f1", sig("exert", Jobber.class, deploy(idle(1))),
                   job("f2", f4, f5), f3,
                   strategy(Provision.YES),
                   pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
                   pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));
    }

    static String getConfigDir() {
        //return String.format("%s/src/test/resources/deploy/configs", System.getProperty("user.dir"));
        return System.getProperty("deploy.configs");
    }
}
