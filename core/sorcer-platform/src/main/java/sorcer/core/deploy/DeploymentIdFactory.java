/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.Signature;
import sorcer.service.SignatureException;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to create deployment ids
 *
 * @author Dennis Reedy
 */
public class DeploymentIdFactory {
    private static Logger logger = LoggerFactory.getLogger(DeploymentIdFactory.class);

    /**
     * Create a deployment ID for a list of Signatures
     *
     * @param list A list of signatures
     * @return A deployment ID
     *
     * @throws SignatureException
     */
    public static String create(List<Signature> list) throws SignatureException {
        StringBuilder ssb = new StringBuilder();
        List<String> items = new ArrayList<>();
        for (Signature s : list) {
            String item = String.format("%s_%s", s.getProviderName(), s.getServiceType().getName());
            if(!items.contains(item))
                items.add(item);
        }
        java.util.Collections.sort(items);
        for (String item : items) {
            if(ssb.length()>0)
                ssb.append(";");
            ssb.append(item);
        }

        String deploymentID = ssb.toString();
        if(logger.isDebugEnabled()) {
            logger.debug("Create deployment name from Signature list: {}\nID: {}",
                ssb.toString(),
                deploymentID);
        }
        return replaceIllegalWindowsCharacters(deploymentID);

    }

    /**
     * Create a deployment ID for a ServiceElement
     *
     * @param service A ServiceElement
     * @return A deployment ID
     */
    public static String create(ServiceElement service) {
        StringBuilder nameBuilder = new StringBuilder();
        List<String> items = new ArrayList<>();
        for (ClassBundle export : service.getExportBundles()) {
            if(!items.contains(export.getClassName()))
                items.add(export.getClassName());
        }
        java.util.Collections.sort(items);
        for (String item : items) {
            if(nameBuilder.length()>0)
                nameBuilder.append(";");
            nameBuilder.append(item);
        }
        String deploymentID = String.format("%s_%s", service.getName(), nameBuilder.toString());
        if(logger.isDebugEnabled()) {
            logger.debug("Create deployment name from service element: {}\nID: {}",
                nameBuilder.toString(),
                deploymentID);
        }
        return replaceIllegalWindowsCharacters(deploymentID);
    }

    private static String replaceIllegalWindowsCharacters(String s) {
        String newName = s.replace("*", "#");
        /*if(System.getProperty("os.name").startsWith("Windows"))
            newName = String.format("\"%s\"", newName);*/
        return newName;
    }
}
