package sorcer.netlet.util;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.antlr.AntlrParserPlugin;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.ParserPlugin;
import org.codehaus.groovy.control.ParserPluginFactory;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Reduction;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author Rafał Krupiński
 */

public class ShebangPreprocessorFactory extends ParserPluginFactory {
    public ParserPlugin createParserPlugin() {
        return new Plugin();
    }

    static class Plugin extends AntlrParserPlugin {
        public Reduction parseCST(SourceUnit sourceUnit, Reader reader) throws CompilationFailedException {
            try {
                String text = modifyTextSource(IOUtils.toString(reader));
                StringReader stringReader = new StringReader(text);
                return super.parseCST(sourceUnit, stringReader);
            } catch (IOException e) {
                throw new CompilationFailedException(sourceUnit.getPhase(), sourceUnit, e);
            }
        }

        // skip shebang, leave empty line
        String modifyTextSource(String text) {
            if (text.startsWith("#!")) {
                int eol = text.indexOf('\n');
                return text.substring(eol);
            }
            return text;
        }
    }
}