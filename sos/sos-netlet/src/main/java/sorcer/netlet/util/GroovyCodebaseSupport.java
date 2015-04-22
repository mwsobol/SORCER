package sorcer.netlet.util;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Rafał Krupiński
 */
@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public class GroovyCodebaseSupport extends AbstractASTTransformation {

    private NetletClassLoader classLoader;

    public GroovyCodebaseSupport(NetletClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        CodebaseVisitor visitor = new CodebaseVisitor(source);
        ModuleNode ast = source.getAST();

        for (ClassNode node : ast.getClasses()) {
            visitor.visitClass(node);
        }


        URL[] addCP = new URL[visitor.classpath.size()];
        List<String> classpath = visitor.classpath;
        try {
            for (int i = 0, classpathSize = classpath.size(); i < classpathSize; i++) {
                String cp = classpath.get(i);
                addCP[i] = new URL(cp);
            }

            URL[] codebase = new URL[visitor.codebase.size()];
            for (int i = 0; i < visitor.codebase.size(); i++) {
                String cb = visitor.codebase.get(i);
                codebase[i] = new URL(cb);
            }

            classLoader.addURLs(addCP);
            classLoader.setCodebase(codebase);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    static class CodebaseVisitor extends ClassCodeVisitorSupport {
        private SourceUnit sourceUnit;
        List<String> codebase = new LinkedList<String>();
        List<String> classpath = new LinkedList<String>();

        public CodebaseVisitor(SourceUnit sourceUnit) {
            this.sourceUnit = sourceUnit;
        }

        @Override
        public void visitAnnotations(AnnotatedNode node) {
            super.visitAnnotations(node);

            for (AnnotationNode anno : node.getAnnotations()) {
                String annotationClassName = anno.getClassNode().getName();
                if ("Codebase".equals(annotationClassName)) {
                    String value = anno.getMember("value").getText();
                    codebase.add(value);

                } else if ("Load".equals(annotationClassName)) {
                    String value = anno.getMember("value").getText();
                    classpath.add(value);
                }
            }
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return sourceUnit;
        }
    }
}
