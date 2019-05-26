/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.ant.antunit.listener;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.ant.antunit.AssertionFailedException;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;

/**
 * A test listener for &lt;antunit&gt; modelled after the Plain JUnit
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
    /**
     * Collects log messages.
     */
    private StringBuffer log = new StringBuffer();

    private static final String NEW_LINE = System.getProperty("line.separator");

    public PlainAntUnitListener() {
        super(new BaseAntUnitListener.SendLogTo(SendLogTo.ANT_LOG), "txt");
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
        StringBuffer sb = new StringBuffer("Build File: ");
        sb.append(buildFile);
        sb.append(NEW_LINE);
        try {
            out.write(sb.toString().getBytes());
            out.flush();
        } catch (IOException ex) {
            throw new BuildException("Unable to write output", ex);
        }
    }

    public void endTestSuite(Project testProject, String buildFile) {
        long runTime = System.currentTimeMillis() - start;
        StringBuffer sb = new StringBuffer("Tests run: ");
        sb.append(runCount);
        sb.append(", Failures: ");
        sb.append(failureCount);
        sb.append(", Errors: ");
        sb.append(errorCount);
        sb.append(", Time elapsed: ");
        sb.append(nf.format(runTime/ 1000.0));
        sb.append(" sec");
        sb.append(NEW_LINE);

        if (log.length() > 0) {
            sb.append("------------- Log Output       ---------------");
            sb.append(NEW_LINE);
            sb.append(log.toString());
            log.setLength(0);
            sb.append("------------- ---------------- ---------------");
            sb.append(NEW_LINE);
        }

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

    public void startTest(String target) {
        super.startTest(target);
        wri.print("Target: " + target + " ");
    }

    public void endTest(String target) {
        double seconds = (System.currentTimeMillis() - testStart) / 1000.0;
        wri.println("took " + nf.format(seconds) + " sec");
    }

    public void addFailure(String target, AssertionFailedException ae) {
        super.addFailure(target, ae);
        formatError(" FAILED", ae);
    }
    public void addError(String target, Throwable ae) {
        super.addError(target, ae);
        formatError(" caused an ERROR", ae);
    }

    private void formatError(String type, Throwable t) {
        wri.println(type);
        Location l = getLocation(t);
        if (l.getLineNumber() != 0) {
            wri.print("\tat line " + l.getLineNumber());
            if (l.getColumnNumber() != 0) {
                wri.print(", column " + l.getColumnNumber());
            }
            wri.println();
        }
        wri.println("\tMessage: " + t.getMessage());
        wri.print("\t");
    }

    protected void messageLogged(BuildEvent event) {
        log.append(event.getMessage());
        log.append(NEW_LINE);
    }

}