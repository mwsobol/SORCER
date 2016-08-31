You must first get the ANTLRWorks application. Go to http://www.antlr3.org/works/ and download
ANTLRWorks 1.5.2.

Load the sml.g file with ANTLRWorks

java -jar <location where you installed it>antlrworks-1.5.2-complete.jar sml.g

Go to File-> Export All Rules -> As Bitmap Image ...

Choose the images directory

You might get a few NullPointerException dialogs, clear them out.

run ./generate.groovy

You will have a sml-grammar.html document generated.

If you want to edit the content, you need to edit the sml-template.html document. Note that this is raw HTML,
with place holders for the sections of the document. The approach taken was to provide meta-data in the sml.g
grammar file that the generator uses to create a table of sections to bitmap files. As the template is being
processed the table is is used to fill in rule images accordingly.