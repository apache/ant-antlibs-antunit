/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.ant.antunit;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.TeeOutputStream;

/**
 * A test listener for &lt;antunit&gt; modeled aftern the Plain JUnit
 * test listener that is part of Ant.
 */
public class PlainAntUnitListener extends ProjectComponent
    implements AntUnitListener {

    /**
     * Formatter for timings.
     */
    private NumberFormat nf = NumberFormat.getInstance();

    private OutputStream out = null;
    /**
     * Helper to store intermediate output.
     */
    private StringWriter inner;
    /**
     * Convenience layer on top of {@link #inner inner}.
     */
    private PrintWriter wri;

    /**
     * Directory to write reports to.
     */
    private File toDir;

    /**
     * Where to send log.
     */
    private SendLogTo logTo = new SendLogTo(SendLogTo.ANT_LOG);

    /**
     * keeps track of the numer of executed targets, the failures an errors.
     */
    private int runCount, failureCount, errorCount;
    /**
     * time for the starts of the current test-suite and test-target.
     */
    private long start, testStart;

    /**
     * Sets the directory to write test reports to.
     */
    public void setToDir(File f) {
        toDir = f;
    }

    /**
     * Where to send the test report.
     */
    public void setSendLogTo(SendLogTo logTo) {
        this.logTo = logTo;
    }

    public void startTestSuite(Project testProject, String buildFile) {
        inner = new StringWriter();
        wri = new PrintWriter(inner);
        runCount = failureCount = errorCount;
        out = getOut(buildFile);
        String newLine = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer("Build File: ");
        sb.append(buildFile);
        sb.append(newLine);
        try {
            out.write(sb.toString().getBytes());
            out.flush();
        } catch (IOException ex) {
            throw new BuildException("Unable to write output", ex);
        }
        start = System.currentTimeMillis();
    }

    public void endTestSuite(Project testProject, String buildFile) {
        long runTime = System.currentTimeMillis() - start;
        String newLine = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer("Tests run: ");
        sb.append(runCount);
        sb.append(", Failures: ");
        sb.append(failureCount);
        sb.append(", Errors: ");
        sb.append(errorCount);
        sb.append(", Time elapsed: ");
        sb.append(nf.format(runTime/ 1000.0));
        sb.append(" sec");
        sb.append(newLine);

        if (out != null) {
            try {
                out.write(sb.toString().getBytes());
                wri.close();
                out.write(inner.toString().getBytes());
                out.flush();
            } catch (IOException ioex) {
                throw new BuildException("Unable to write output", ioex);
            } finally {
                if (out != System.out && out != System.err) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    public void startTest(String target) {
        testStart = System.currentTimeMillis();
        runCount++;
    }
    public void endTest(String target) {
        wri.print("Target: " + target);
        double seconds = (System.currentTimeMillis() - testStart) / 1000.0;
        wri.println(" took " + nf.format(seconds) + " sec");
    }

    public void addFailure(String target, AssertionFailedException ae) {
        failureCount++;
        formatError("\tFAILED", ae);
    }
    public void addError(String target, Throwable ae) {
        errorCount++;
        formatError("\tCaused an ERROR", ae);
    }

    private void formatError(String type, Throwable t) {
        wri.println(type);
        wri.println(t.getMessage());
    }

    private OutputStream getOut(String buildFile) {
        OutputStream l, f;
        l = f = null;
        if (logTo.getValue().equals(SendLogTo.ANT_LOG)
            || logTo.getValue().equals(SendLogTo.BOTH)) {
            l = new LogOutputStream(this, Project.MSG_INFO);
            if (logTo.getValue().equals(SendLogTo.ANT_LOG)) {
                return l;
            }
        }
        if (logTo.getValue().equals(SendLogTo.FILE)
            || logTo.getValue().equals(SendLogTo.BOTH)) {
            if (buildFile.length() > 0
                && buildFile.charAt(0) == File.separatorChar) {
                buildFile = buildFile.substring(1);
            }
            
            String fileName =
                buildFile.replace(File.separatorChar, '.') + ".txt";
            File file = toDir == null
                ? getProject().resolveFile(fileName)
                : new File(toDir, fileName);
            try {
                f = new FileOutputStream(file);
            } catch (IOException e) {
                throw new BuildException(e);
            }
            if (logTo.getValue().equals(SendLogTo.FILE)) {
                return f;
            }
        }
        return new TeeOutputStream(l, f);
    }

    public static class SendLogTo extends EnumeratedAttribute {
        public static final String ANT_LOG = "ant";
        public static final String FILE = "file";
        public static final String BOTH = "both";

        public SendLogTo() {}

        public SendLogTo(String s) {
            setValue(s);
        }

        public String[] getValues() {
            return new String[] {ANT_LOG, FILE, BOTH};
        }
    }
}