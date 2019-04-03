package sorcer.netlet.util;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

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

        List<URL> codebase = expand(visitor.codebase);
        List<URL> addCP = expand(visitor.classpath);
        addCP.addAll(codebase);

        classLoader.addURLs(addCP.toArray(new URL[addCP.size()]));
        classLoader.setCodebase(codebase.toArray(new URL[codebase.size()]));
    }

    private List<URL> expand(List<String> entries) {
        List<URL> result = new LinkedList<URL>();
        for (String entry : entries) {
            result.addAll(LoaderConfigurationHelper.load(entry));
        }
        return result;
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
                    codebase.add(parseParameters(anno));
                } else if ("Load".equals(annotationClassName)) {
                    classpath.add(parseParameters(anno));
                }
            }
        }

        private String parseParameters(AnnotationNode anno) {
            if (anno.getMember("execute")!=null) {
                return anno.getMember("execute").getText();
            } else {
                String group = (anno.getMember("group")!=null) ? anno.getMember("group").getText() : null;
                String module = (anno.getMember("module")!=null) ? anno.getMember("module").getText() : null;
                String version = (anno.getMember("version")!=null) ? anno.getMember("version").getText() : null;
                String classifier = (anno.getMember("classifier")!=null) ? anno.getMember("classifier").getText() : null;
                return group + ":" + module + ":jar:" + (classifier!=null ? classifier + ":" : "") + version;
            }
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return sourceUnit;
        }
    }
}
