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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to create deployment ids using a MessageDigest
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
     * @throws NoSuchAlgorithmException
     */
    public static String create(List<Signature> list) throws NoSuchAlgorithmException, SignatureException {
        StringBuilder ssb = new StringBuilder();
        List<String> items = new ArrayList<String>();
        for (Signature s : list) {
            String item = String.format("%s%s", s.getProviderName(), s.getServiceType().getName());
            if(!items.contains(item))
                items.add(item);
        }
        java.util.Collections.sort(items);
        for (String item : items) {
            ssb.append(item);
        }

        String deploymentID = createDeploymentID(ssb.toString());
        if(logger.isDebugEnabled()) {
            logger.debug("Create deployment key from Signature list: {}\nID: {}",
                         ssb.toString(),
                         deploymentID);
        }
        return deploymentID;

    }

    /**
     * Create a deployment ID for a ServiceElement
     *
     * @param service A ServiceElement
     * @return A deployment ID
     *
     * @throws NoSuchAlgorithmException
     */
    public static String create(ServiceElement service) throws NoSuchAlgorithmException {
        StringBuilder nameBuilder = new StringBuilder();
        List<String> items = new ArrayList<>();
        items.add(service.getName());
        for (ClassBundle export : service.getExportBundles()) {
            items.add(export.getClassName());
        }
        java.util.Collections.sort(items);
        for (String item : items) {
            nameBuilder.append(item);
        }
        String deploymentID = createDeploymentID(nameBuilder.toString());
        if(logger.isDebugEnabled()) {
            logger.debug("Create deployment key from service element: {}\nID: {}",
                         nameBuilder.toString(),
                         deploymentID);
        }
        return deploymentID;
    }

    static String createDeploymentID(final String ssb) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(ssb.getBytes());
        byte byteData[] = md.digest();
        // convert the byte to hex
        StringBuilder hexString = new StringBuilder();
        for (byte data : byteData) {
            String hex = Integer.toHexString(0xff & data);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
