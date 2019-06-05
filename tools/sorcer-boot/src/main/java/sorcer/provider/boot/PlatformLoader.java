/*
 * Copyright 2008 the original author or authors.
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
package sorcer.provider.boot;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sorcer.core.provider.util.PropertyHelper;

/**
 * Parses platform configuration documents
 *
 * @author Dennis Reedy
 */
public class PlatformLoader {
    static final String COMPONENT = "sorcer.provider.boot";
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Parse the platform
     *
     * @param directory The directory to search for XML configuration documents
     *
     * @return An array of Capability objects
     *
     * @throws Exception if there are errors parsing the configuration files
     */
    public Capability[] parsePlatform(String directory) throws Exception {
        if(directory == null)
            throw new IllegalArgumentException("directory is null");
        List<Capability> platformList = new ArrayList<Capability>();
        File dir = new File(directory);
        if(dir.exists()) {
            if(dir.isDirectory()) {
                if(dir.canRead()) {
                    File[] files = dir.listFiles();
                    for (File file : files) {
                        if (file.getName().endsWith("xml") ||
                            file.getName().endsWith("XML")) {
                            try {
                                platformList.addAll(
                                    parsePlatform(file.toURI().toURL()));
                            } catch (Exception e) {
                                logger.warn(
                                           "Could not compute ["+file.getAbsolutePath()+"], " +
                                           "continue building platform",
                                           e);
                            }
                        }
                    }
                } else {
                    logger.warn("No read permissions for platform " +
                                   "directory ["+directory+"]");
                }
            } else {
                logger.warn("Platform directory ["+dir+"] " +
                               "is not a directory");
            }
        } else {
            logger.warn("Platform directory ["+directory+"] not found");
        }
        Capability[] caps = platformList.toArray(new Capability[platformList.size()]);
        logger.debug("Platform capabilities: " + Arrays.toString(caps));
        return caps;
    }

    /*
     * Parse the platform
     */
    Collection<Capability> parsePlatform(URL configURL) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream is = configURL.openStream();
        Collection<Capability> caps = new ArrayList<Capability>();
        try {
            Document document = builder.parse(is);
            Element element = document.getDocumentElement();
            if ((element != null) && element.getTagName().equals("platform")) {
                caps.addAll(visitElement_platform(element,
                                                  configURL.toExternalForm()));
            }
        } finally {
            is.close();
        }

        return(caps);
    }
    
    /*
     * Scan through Element named platform.
     */
    Collection<Capability> visitElement_platform(Element element,
                                                 String configFile) {
        List<Capability> capabilities = new ArrayList<Capability>();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node.getNodeType()==Node.ELEMENT_NODE) {
                Element nodeElement = (Element)node;
                if (nodeElement.getTagName().equals("capability")) {
                    Capability cap = visitElement_capability(nodeElement);
                    if(cap.getPath()!=null) {
                        File file = new File(cap.getPath());
                        if(file.exists())
                            capabilities.add(cap);
                        else
                            logger.warn("Platform configuration " +
                                           "for ["+cap+"] not loaded, " +
                                           "the path ["+cap.getPath()+"] " +
                                           "does not exist. Make sure the " +
                                           "configuration file " +
                                           "["+configFile+"] " +
                                           "is correct, or delete the file " +
                                           "if it no longer references a " +
                                           "valid capability");
                    } else if(cap.getClasspath()!=null) {
                        String[] classpath = cap.getClasspath();
                        boolean okay = true;
                        String failedClassPathEntry = null;
                        for(String s : classpath) {
                            File file = new File(s);
                            if(!file.exists()) {
                                failedClassPathEntry = file.getName();
                                okay = false;
                                break;
                            }
                        }
                        if(okay)
                            capabilities.add(cap);
                        else {
                            StringBuffer sb = new StringBuffer();
                            for(String s : cap.getClasspath()) {
                                if(sb.length()>0)
                                    sb.append(" ");
                                sb.append(s);
                            }
                            logger.warn("Platform configuration " +
                                           "for ["+cap+"] not loaded, " +
                                           "could not locate classpath " +
                                           "entry ["+failedClassPathEntry+"]. The "+
                                           "classpath ["+sb.toString()+"] " +
                                           "is invalid. Make sure the " +
                                           "configuration file " +
                                           "["+configFile+"] is " +
                                           "correct, or delete the file " +
                                           "if it no longer references a " +
                                           "valid capability");
                        }
                    } else {
                        capabilities.add(cap);
                    }
                }
            }
        }
        return(capabilities);
    }
    
    /*
     * Scan through Element named capability.
     */
    Capability visitElement_capability(Element element) { // <capability>
        // element.execute();
        Capability cap = new Capability();
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr)attrs.item(i);
            if (attr.getName().equals("common")) { // <capability common="???">
                cap.setCommon(attr.getValue());
            }
            if (attr.getName().equals("key")) { // <capability key="???">
                cap.setName(attr.getValue());
            }
            if (attr.getName().equals("class")) { // <capability class="???">
                cap.setPlatformClass(attr.getValue());
            }
        }
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node.getNodeType()==Node.ELEMENT_NODE) {
                Element nodeElement = (Element)node;
                if(nodeElement.getTagName().equals("description")) {
                    cap.setDescription(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("version")) {
                    cap.setVersion(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("manufacturer")) {
                    cap.setManufacturer(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("classpath")) {
                    cap.setClasspath(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("path")) {
                    cap.setPath(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("native")) {
                    cap.setNativeLib(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("costmodel")) {
                    cap.setCostModelClass(getTextValue(nodeElement));
                }
            }
        }
        return(cap);
    }


    /**
     * Get the text eval for a node
     *
     * @param node The Node to getValue the text eval for
     * @return The text eval for the Node, or a zero-length String if
     * the Node is not recognized
     */
    String getTextValue(Node node) {
        NodeList eList = node.getChildNodes();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < eList.getLength(); i++) {
            Node n = eList.item(i);
            if (n.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
                sb.append(getTextValue(n));
            } else if (n.getNodeType() == Node.TEXT_NODE) {
                sb.append(n.getNodeValue());
            }
        }
        return(replaceProperties(sb.toString().trim()));
    }

    String replaceProperties(String arg) {
        return(PropertyHelper.expandProperties(arg, PropertyHelper.PARSETIME));
    }


    URL getURL(String location) throws MalformedURLException {
        URL url;

        if(location.startsWith("http") || location.startsWith("file:")) {
            url = new URL(location);
        } else {
            url = new File(location).toURI().toURL();
        }
        return(url);
    }

    /**
     * Get the default platform configuration
     *
     * @return An array of Capability objects returned from parsing the
     * default configuration META-INF/platform.xml
     *
     * @throws Exception if there are errors parsing and/or processing the
     * default configuration
     */
    public Capability[] getDefaultPlatform() throws Exception {
        URL platformConfig =
            PlatformLoader.class.getClassLoader().getResource("META-INF/platform.xml");
        if (platformConfig==null) {
            throw new RuntimeException("META-INF/platform.xml not found");
        }
        Collection<Capability> c = parsePlatform(platformConfig);
        return(c.toArray(new Capability[c.size()]));
    }

    /**
     * Contains attributes for a platform capability.
     */
    public static class Capability {
        String name;
        String description;
        String manufacturer;
        String version;
        String classpath;
        String path;
        String nativeLib;
        String common="false";
        static String DEFAULT_PLATFORM_CLASS =
            "sorcer.provider.system.capability.software.SoftwareSupport";
        String platformClass =DEFAULT_PLATFORM_CLASS;
        String costModelClass;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String[] getClasspath() {
            if(classpath==null)
                return(new String[0]);
            StringTokenizer st = new StringTokenizer(classpath);
            String[] paths = new String[st.countTokens()];
            int n=0;
            while (st.hasMoreTokens ()) {
                paths[n++] = st.nextToken();
            }
            return paths;
        }

        public URL[] getClasspathURLs() throws MalformedURLException {
            String[] classpath = getClasspath();
            return(Booter.toURLs(classpath));                    
        }

        public void setClasspath(String classpath) {
            this.classpath = classpath;
        }

        public String getNativeLib() {
            return nativeLib;
        }

        public void setNativeLib(String nativeLib) {
            this.nativeLib = nativeLib;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean getCommon() {
            return Boolean.parseBoolean(common.equals("yes")?"true":"false");
        }

        public void setCommon(String common) {
            this.common = common;
        }

        public String getPlatformClass() {
            return platformClass;
        }

        public void setPlatformClass(String platformClass) {
            this.platformClass = platformClass;
        }

        public String geCostModelClass() {
            return costModelClass;
        }

        public void setCostModelClass(String costModelClass) {
            this.costModelClass = costModelClass;
        }

        public String toString() {
            return "Capability{" +
                   "key='" + name + '\'' +
                   ", description='" + description + '\'' +
                   ", manufacturer='" + manufacturer + '\'' +
                   ", version='" + version + '\'' +
                   ", classpath='" + classpath + '\'' +
                   ", path='" + path + '\'' +
                   ", native='" + nativeLib + '\'' +
                   ", common='" + common + '\'' +
                   ", platformClass='" + platformClass + '\'' +
                   ", costModelClass='" + costModelClass + '\'' +
                   '}';
        }
    }
}
