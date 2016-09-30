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
package sorcer.generator.ui;

import sorcer.generator.Options;
import sorcer.generator.tasks.GeneratorTask;
import sorcer.generator.tasks.GitSettingsUpdater;
import sorcer.generator.tasks.ProviderGeneratorTasks;
import sorcer.generator.tasks.RequestorGeneratorTasks;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;

/**
 * Displays status of project generation.
 */
public class GenerationPanel extends JPanel implements PropertyChangeListener {
    private final JProgressBar progressBar = new JProgressBar();
    private JTextArea taskOutput;
    private Task task;
    private final LinkedList<GeneratorTask> steps = new LinkedList<GeneratorTask>();
    private GenerationListener generationListener;

    public GenerationPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        taskOutput = new JTextArea(5, 20);
        taskOutput.setMargin(new Insets(5,5,5,5));
        taskOutput.setEditable(false);

        add(progressBar, BorderLayout.NORTH);
        add(new JScrollPane(taskOutput), BorderLayout.CENTER);
    }

    void generate(final Options options, final GenerationListener listener) {
        generationListener = listener;

        if(options.getProvider()) {
            steps.add(new ProviderGeneratorTasks.CreateApiDirectory(options));
            steps.add(new ProviderGeneratorTasks.CreateApiGradleBuild(options));
            steps.add(new ProviderGeneratorTasks.CreateApiInterface(options));
            steps.add(new ProviderGeneratorTasks.CreateApiContext(options));
            steps.add(new ProviderGeneratorTasks.CreateProviderDirectory(options));
            steps.add(new ProviderGeneratorTasks.CreateProviderGradleBuild(options));
            steps.add(new ProviderGeneratorTasks.CreateProviderStarterConfig(options));
            steps.add(new ProviderGeneratorTasks.CreateProviderDataDirectory(options));
            steps.add(new ProviderGeneratorTasks.CreateProviderDataInputDirectoryAndContent(options));
            steps.add(new ProviderGeneratorTasks.CreateProviderImpl(options));
            steps.add(new ProviderGeneratorTasks.CreateProviderProperties(options));
            steps.add(new ProviderGeneratorTasks.CreateProviderTest(options));
            steps.add(new GitSettingsUpdater(options));
        } else {
            steps.add(new RequestorGeneratorTasks.CreateRequestorGradleBuild(options));
            steps.add(new RequestorGeneratorTasks.CreateRequestorData(options));
            steps.add(new RequestorGeneratorTasks.CreateRequestorProperties(options));
            steps.add(new RequestorGeneratorTasks.CreateRequestor(options));
            steps.add(new GitSettingsUpdater(options));
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setMaximum(steps.size());
        task = new Task(steps.size());
        task.addPropertyChangeListener(this);
        task.execute();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int step = (Integer) evt.getOldValue();
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
            taskOutput.append(String.format("%s\n", steps.get(step).describe()));
        }
    }

    class Task extends SwingWorker<Void, Void> {
        final int numSteps;

        Task(int numSteps) {
            this.numSteps = numSteps;
        }

        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            int progress = 0;
            setProgress(0);
            while (progress < numSteps) {
                GeneratorTask gTask = steps.get(progress);
                try {
                    gTask.exec();
                    Thread.sleep(200);
                } catch(Throwable t) {
                    taskOutput.append(String.format("%s: %s\n", t.getClass().getName(), t.getMessage()));
                    for(StackTraceElement s : t.getStackTrace()) {
                        taskOutput.append(String.format("    %s\n", s.toString()));
                    }
                }
                progress++;
                setProgress(progress);
            }
            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            if(generationListener!=null)
                generationListener.notifyComplete();
            Toolkit.getDefaultToolkit().beep();
            setCursor(null);
        }
    }
}
