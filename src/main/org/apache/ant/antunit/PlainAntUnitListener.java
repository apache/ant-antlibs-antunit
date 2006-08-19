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
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.TeeOutputStream;

/**
 * A test listener for &lt;antunit&gt; modeled aftern the Plain JUnit
 * test listener that is part of Ant.
 */
public class PlainAntUnitListener extends BaseAntUnitListener {
    private OutputStream out = null;
    /**
     * Helper to store intermediate output.
     */
    private StringWriter inner;
    /**
     * Convenience layer on top of {@link #inner inner}.
     */
    private PrintWriter wri;

    public PlainAntUnitListener() {
        super(new BaseAntUnitListener.SendLogTo(SendLogTo.ANT_LOG));
    }

    /**
     * Where to send the test report.
     */
    public void setSendLogTo(BaseAntUnitListener.SendLogTo logTo) {
        super.setSendLogTo(logTo);
    }

    public void startTestSuite(Project testProject, String buildFile) {
        super.startTestSuite(testProject, buildFile);
        inner = new StringWriter();
        wri = new PrintWriter(inner);
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
                close(out);
            }
        }
    }

    public void endTest(String target) {
        wri.print("Target: " + target);
        double seconds = (System.currentTimeMillis() - testStart) / 1000.0;
        wri.println(" took " + nf.format(seconds) + " sec");
    }

    public void addFailure(String target, AssertionFailedException ae) {
        super.addFailure(target, ae);
        formatError("\tFAILED", ae);
    }
    public void addError(String target, Throwable ae) {
        super.addError(target, ae);
        formatError("\tCaused an ERROR", ae);
    }

    private void formatError(String type, Throwable t) {
        wri.println(type);
        wri.println(t.getMessage());
    }

}