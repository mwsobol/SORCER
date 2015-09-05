/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.ui.util;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Mike Sobolewski on 8/22/15.
 */
public class TextAreaPrintStream extends OutputStream {

    private JTextArea textArea;
    private final StringBuilder sb = new StringBuilder();

    public TextAreaPrintStream(final JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\r')
            return;

        if (b == '\n') {
            final String text = sb.toString() + "\n";
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    textArea.append(text);
                }
            });
            sb.setLength(0);
            return;
        }

        sb.append((char) b);
    }
}
