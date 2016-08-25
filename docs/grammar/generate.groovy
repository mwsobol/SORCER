#!/usr/bin/env groovy
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
File template = new File("template/sml-template.html")
File target = new File("sml-grammar.html")
if(target.exists()) {
    if(target.delete())
        println "Deleted prevoiously generated ${target.name}"
}
def sections = [:]
def rules = null
String tag = null
File grammar = new File("sml.g")
File grammarText = new File("sml.g.txt")
if(grammarText.exists()) {
    if(grammarText.delete())
        println "Deleted prevoiously generated ${grammarText.name}"
}
boolean quit = false
grammar.eachLine { line ->
    grammarText.append("\n${line}")
    if (!quit) {
        quit = line.contains("<END>")
        if (line.contains("<")) {
            if (rules != null) {
                sections.put(tag, rules)
            }
            rules = new ArrayList<>()
            int ndx = line.indexOf("<")
            String t = line.substring(ndx + 1)
            ndx = t.indexOf(">")
            tag = t.substring(0, ndx)
        }
        if (rules != null && line.trim().length() > 0) {
            def parts = line.split()
            String fileName = "${parts[0]}.jpg"
            File image = new File("images/${fileName}")
            if(image.exists()) {
                rules << image
            }
        }
    }
}

template.eachLine { line ->
    if(line.contains("<%")) {
        int ndx = line.indexOf("%")
        String t = line.substring(ndx+1)
        ndx = t.indexOf("%")
        String section = t.substring(0, ndx).trim()
        println "${section}"
        sections.get(section).each { f ->

            target.append("\n<p><img src=\"images/${f.name}\"></p>")
        }
    } else {
        target.append("\n$line")
    }
}