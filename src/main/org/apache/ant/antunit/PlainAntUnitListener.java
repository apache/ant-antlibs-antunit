/*
 * Copyright 2005-2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.ant.antunit;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;

import org.apache.tools.ant.BuildException;

/**
 * A test listener for &lt;antunit&gt;.
 */
public class PlainAntUnitListener implements AntUnitListener {
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

    private int runCount, failureCount, errorCount;
    private long start, testStart;

    public void setOutput(OutputStream out) {
        this.out = out;
    }

    public void startTestSuite(String buildFile) {
        inner = new StringWriter();
        wri = new PrintWriter(inner);
        runCount = failureCount = errorCount;
        if (out == null) {
            return; // Quick return - no output do nothing.
        }
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

    public void endTestSuite(String buildFile) {
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

//        // append the err and output streams to the log
//        if (systemOutput != null && systemOutput.length() > 0) {
//            sb.append("------------- Standard Output ---------------")
//                .append(newLine)
//                .append(systemOutput)
//                .append("------------- ---------------- ---------------")
//                .append(newLine);
//        }
//
//        if (systemError != null && systemError.length() > 0) {
//            sb.append("------------- Standard Error -----------------")
//                .append(newLine)
//                .append(systemError)
//                .append("------------- ---------------- ---------------")
//                .append(newLine);
//        }

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

}