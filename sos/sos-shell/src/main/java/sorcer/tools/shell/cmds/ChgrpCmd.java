/*
 * Copyright 2011 the original author or authors.
 * Copyright 2011 SorcerSoft.org.
 * Copyright 2015 SorcerSoft.com
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

package sorcer.tools.shell.cmds;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryGroupManagement;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.util.StringUtils;

public class ChgrpCmd extends ShellCmd {

    {
        COMMAND_NAME = "chgrp";

        NOT_LOADED_MSG = "***command not loaded due to conflict";

        COMMAND_USAGE = "chgrp (<groups> | all) | chgrp -r <registrar index> <groups>";

        COMMAND_HELP = "Change groups (<groups> space separated) for discovery/lookup, or a selected lookup service.";
    }

    public Options getOptions() {
        Options options = new Options();
        options.addOption(new Option("r", true, "registrar index"));
        return options;
    }

    public void execute(String command, CommandLine cmd) {
        List<ServiceRegistrar> registrars = NetworkShell.getRegistrars();

        String[] groups = null; // matches all

        ServiceRegistrar registrar = null;
        int myIdx = 0;

        if (cmd.hasOption('r')) {
            myIdx = Integer.parseInt(cmd.getOptionValue('r'));
            if ((myIdx < registrars.size()) && (myIdx >= 0)) {
                registrar = registrars.get(myIdx);
            }
        }

        groups = cmd.getArgs();

        Set<String> groupSet = new HashSet<String>();

        for (String group : groups) {
            if (group.indexOf('-') == 0) {
                printUsage();
                return;
            }

            if ("all".equals(group)) {
                // empty set makes null array which means all groups
                groupSet.clear();
                break;
            }

            if ("public".equals(group)) {
                groupSet.clear();
                groupSet.add("");
                break;
            }

            Collections.addAll(groupSet, StringUtils.toArray(group, ","));
        }

        groups = groupSet.isEmpty() ? DiscoveryGroupManagement.ALL_GROUPS : groupSet.toArray(new String[groupSet.size()]);

        if (registrar != null) {
            setGroups(registrar, myIdx, groups);
        } else {
            NetworkShell.setGroups(groups);
            NetworkShell.getDisco().terminate();
            NetworkShell.getRegistrars().clear();
            DiscoCmd.selectedRegistrar = 0;
            NetworkShell.setLookupDiscovery(groups);
        }
    }

    private void setGroups(ServiceRegistrar registrar, int Idx, String[] gps) {
        try {
            if (registrar instanceof Administrable) {
                Administrable admin = (Administrable) registrar;
                JoinAdmin jAdmin = (JoinAdmin) admin.getAdmin();
                out.println("\tChanging LUS groups # " + Idx + " now!");
                jAdmin.setLookupGroups(gps);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
