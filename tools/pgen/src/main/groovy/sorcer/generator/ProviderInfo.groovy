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
package sorcer.generator

/**
 * Provides information about a provider in the current MSTC Engineering project
 *
 * @author Dennis Reedy
 */
class ProviderInfo {
    def classPath = []
    File apiJar
    String baseName
    String version
    boolean loaded = false
    ClassLoader loader
    String interfaceClass

    ProviderInfo setBaseName(name) {
        if (name.endsWith("-api"))
            baseName = name.substring(0, name.length() - "-api".length());
        else
            baseName = name;
        this
    }

    ProviderInfo setClassPath(classPath) {
        this.classPath.clear()
        this.classPath.addAll(classPath)
        if(System.properties['debug']!=null) {
            StringBuilder cp = new StringBuilder()
            classPath.each { url ->
                cp.append(url.toExternalForm()).append("\n")
            }
            File f = new File("${System.properties['user.dir']}/../gradle-ext/src/test/resources/${baseName}.txt")
            if(f.exists())
                f.delete()
            f.text = cp.toString()
            println "Created ${f.path}"

        }
        this
    }

    String getVersion() {
        return version
    }

    ProviderInfo setVersion(String version) {
        this.version = version
        this
    }

    ProviderInfo setApiJar(apiJar) {
        this.apiJar = apiJar
        this
    }

    ClassLoader getClassLoader() {
        if(loader==null)
            loader = new URLClassLoader(classPath as URL[])
        loader
    }

    @Override
    public String toString() {
        return baseName;
    }
}
