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
package org.apache.ant.antunit;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.ant.antunit.listener.BaseAntUnitListener;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;

public class SetUpAndTearDownTest extends BuildFileTest {
    
    public static class TestReportListener extends BaseAntUnitListener {

        private OutputStream out = null;
        /**
         * Helper to store intermediate output.
         */
        private StringWriter inner;
        /**
         * Convenience layer on top of {@link #inner inner}.
         */
        private PrintWriter wri;

        public TestReportListener() {
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
        }

        public void endTestSuite(Project testProject, String buildFile) {
            if (out != null) {
                try {
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
        }

        public void addFailure(String target, AssertionFailedException ae) {
            super.addFailure(target, ae);
            wri.println("failure:" + target + "(" + ae.getMessage() + ")");
        }
        
        public void addError(String target, Throwable ae) {
            super.addError(target, ae);
            wri.println("error:" + target + "(" + ae.getMessage() + ")");
        }
        
        public void messageLogged(int level, String message) {}
   }

    public SetUpAndTearDownTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        configureProject("src/etc/testcases/setupandteardown.xml");
    }


    public void testBoth() {
        executeTarget("testBoth");
    }

    public void testNoSetup() {
        executeTarget("testNoSetup");
    }

    public void testNoTeardown() {
        executeTarget("testNoTeardown");
    }

    public void testFailedTest() {
        executeTarget("testFailedTest");
        System.out.println(getLog());
    }

    public void testFailureSetup() {
        executeTarget("testFailureSetup");
        String log = getLog();
        int index = log.indexOf("Tests run: 3, Failures: 3, Errors: 0,");
        assertTrue("summary", index > -1);
    }
    
    public void testErrorSetup() {
        executeTarget("testErrorSetup");
        String log = getLog();
        int index = log.indexOf("Tests run: 3, Failures: 0, Errors: 3,");
        assertTrue("summary", index > -1);
    }

    public void testFailureTeardown() {
        executeTarget("testFailureTeardown");
        String log = getLog();
        int index = log.indexOf("Tests run: 3, Failures: 3, Errors: 0,");
        assertTrue("summary", index > -1);
    }
    
    public void testErrorTeardown() {
        executeTarget("testErrorTeardown");
        String log = getLog();
        int index = log.indexOf("Tests run: 3, Failures: 0, Errors: 3,");
        assertTrue("summary", index > -1);
    }

    public void testBothSuite() {
        executeTarget("testBothSuite");
    }

    public void testNoSuiteSetup() {
        executeTarget("testNoSuiteSetUp");
    }

    public void testNoSuiteTeardown() {
        executeTarget("testNoSuiteTearDown");
    }

    public void testFailedTestSuite() {
        executeTarget("testFailedTestSuite");
        System.out.println(getLog());
    }

    public void testFailureSuiteSetUp() {
        executeTarget("testFailureSuiteSetUp");
        String log = getLog();
        int index = log.indexOf("Tests run: 1, Failures: 1, Errors: 0,");
        assertTrue("summary", index > -1);
        index = log.indexOf("failure:suiteSetUp(Expected failure)");
        assertTrue("testname", index > -1);
    }

    public void testErrorSuiteSetUp() {
        executeTarget("testErrorSuiteSetUp");
        String log = getLog();
        int index = log.indexOf("Tests run: 1, Failures: 0, Errors: 1,");
        assertTrue("summary", index > -1);
        index = log.indexOf("error:suiteSetUp(Expected error)");
        assertTrue("testname", index > -1);
    }
    
    public void testFailureSuiteTearDown() {
        executeTarget("testFailureSuiteTearDown");
        String log = getLog();
        int index = log.indexOf("Tests run: 5, Failures: 1, Errors: 0,");
        assertTrue("summary", index > -1);
        index = log.indexOf("failure:suiteTearDown(Expected failure)");
        assertTrue("testname", index > -1);
    }
    
    public void testErrorSuiteTearDown() {
        executeTarget("testErrorSuiteTearDown");
        String log = getLog();
        int index = log.indexOf("Tests run: 5, Failures: 0, Errors: 1,");
        assertTrue("summary", index > -1);
        index = log.indexOf("error:suiteTearDown(Expected error)");
        assertTrue("testname", index > -1);
    }

}
