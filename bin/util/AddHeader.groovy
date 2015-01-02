#!/usr/bin/env groovy
/*
 * Distribution Statement
 *
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 *
 * Disclaimer
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
 */

import groovy.io.FileVisitResult

import static groovy.io.FileType.*

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
def text = []
text << " Copyright to the original author or authors."
text << ""
text << "Licensed under the Apache License, Version 2.0 (the \"License\");"
text << "you may not use this file except in compliance with the License."
text << "You may obtain a copy of the License at"
text << ""
text << "     http://www.apache.org/licenses/LICENSE-2.0"
text << ""
text << "Unless required by applicable law or agreed to in writing, software"
text << "distributed under the License is distributed on an \"AS IS\" BASIS"
text << "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied."
text << "See the License for the specific language governing permissions and"
text << "limitations under the License."

def commentLineStarters = [
        "java":   ["/*", " * ", " */"],
        "xml":    ["<!--", " ~ ", "-->"],
        "matlab": ["%{", "  ", "%}"],
        "sh":     ["#", "# ", "#"]
]

boolean checkFirstLine(String line, boolean isXml) {
    boolean startsWith
    if(isXml)
        startsWith = line.startsWith("<?xml version")
    else
        startsWith = line.startsWith("#!/usr/bin/env groovy")
    startsWith

}
dir = new File( args.size()==0? System.getProperty("user.dir") : args[0] )
println "Using ${dir.path} as source root"
final excludedDirs = ['.svn', '.git', '.idea', '.classpath', 'sbin', '.ivy2', 'iGrid', 'native', 'data', 'ivy']
def couldNotProcess = []
dir.traverse(
        type: FILES,
        nameFilter        : ~/.*(java|groovy|config|xml|m|c|h|properties)$/,
        preDir            : {
            if (it.name in excludedDirs)
                return FileVisitResult.SKIP_SUBTREE
        },
        excludeNameFilter : { it in excludedDirs || it == "AddHeader.groovy"}
) { file ->
    def lineStarters = []
    if (file.name.endsWith(".java") ||
            file.name.endsWith(".c") ||
            file.name.endsWith(".h") ||
            file.name.endsWith(".groovy") ||
            file.name.endsWith(".config")) {
        lineStarters = commentLineStarters.get("java")
    } else if (file.name.endsWith(".xml")) {
        lineStarters = commentLineStarters.get("xml")
    } else if (file.name.endsWith(".m")) {
        lineStarters = commentLineStarters.get("matlab")
    } else if (file.name.endsWith(".sh") || file.name.endsWith(".properties")) {
        lineStarters = commentLineStarters.get("sh")
    } else {
        couldNotProcess << file.path
    }
    if (lineStarters.size() > 0 && file.size()>0) {
        StringBuilder headerBuilder = new StringBuilder()
        headerBuilder.append(lineStarters[0]).append("\n")
        for (String line : text) {
            headerBuilder.append(lineStarters[1]).append(line).append("\n")
        }
        headerBuilder.append(lineStarters[2]).append("\n")
        String header = headerBuilder.toString()

        if (!file.text.contains(header)) {
            println "Adding header to ${file.path}"
            if(file.name.endsWith(".xml") || file.name.endsWith(".groovy")) {
                boolean xml = file.name.endsWith(".xml")
                StringBuilder contents = new StringBuilder()
                file.withReader { r ->
                    line = r.readLine()
                    if(checkFirstLine(line, xml)) {
                        contents << line
                        contents << "\n"
                        contents << header
                    } else {
                        contents << header
                        contents << line
                        contents << "\n"
                    }
                    r.eachLine() { l ->
                        contents << l
                        contents << "\n"
                    }
                }
                file.text = contents.toString()
            } else if(file.name.endsWith(".java")) {
                StringBuilder contents = new StringBuilder()
                file.withReader { r ->
                    boolean havePackageDeclaration = false
                    r.eachLine() { line ->
                        if (line.startsWith("package")) {
                            contents << header
                            havePackageDeclaration = true
                        }
                        if (havePackageDeclaration) {
                            contents << line
                            contents << "\n"
                        }
                    }
                }
                file.text = contents.toString()
            } else {
                def source = file.text
                file.text = "$header$source"
            }
        } else {
            println "Header found in  ${file.path}"
        }
        assert file.text.contains(header)
    }
}
if(couldNotProcess.size()>0) {
    println "\nDon't know how to process the following files (unrecognized extension)\n"
    for(String doh : couldNotProcess)
        println "$doh"
}
