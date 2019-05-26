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
package org.apache.ant.antunit.junit4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.ant.antunit.junit3.AntUnitSuite;
import org.apache.tools.ant.util.FileUtils;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;

public class AntUnitSuiteRunnerTest extends TestCase {

    private boolean mockExecutionOK = false;
    private String mockExecutionError = "";

    /**
     * Validates the execution sequence.
     */
    public void testRunFullSuite() throws FileNotFoundException, IOException,
            InitializationError {
        AntUnitSuiteRunner runner = new AntUnitSuiteRunner(
                JUnit4AntUnitRunnable.class);

        runner.run(new RunNotifier());
        File outFile = new File("target/test_output/junit_out.xml");

        String output = FileUtils.readFully(new FileReader(outFile));
        String EXPECT1 = "suiteSetUp-setUp-test1-tearDown-setUp-test2-tearDown-suiteTearDown";
        String EXPECT2 = "suiteSetUp-setUp-test2-tearDown-setUp-test1-tearDown-suiteTearDown";
        assertTrue("unexted output : " + output, EXPECT1.equals(output)
                || EXPECT2.equals(output));
    }

    
    /**
     * When a test is executed, the description used in the notification must be
     * equals to the description declared, otherwise the runner is confused (for
     * example in eclipse you have all the tests listed twice, but reported only
     * once as executed.
     * 
     * @throws InitializationError
     */
    public void testDescriptionsReportedInNotifier() throws InitializationError {
        final AntUnitSuiteRunner runner = new AntUnitSuiteRunner(
                JUnit4AntUnitRunnable.class);
        final ArrayList tDescs = runner.getDescription().getChildren();

        RunNotifier notifierMock = new RunNotifier() {
            Description curTest = null;

            public void fireTestStarted(Description description) {
                if (curTest != null) {
                    mockExecutionError += "Unexpected fireTestStarted("
                            + description.getDisplayName() + "\n";
                }
                if (!tDescs.contains(description)) {
                    mockExecutionError += "Unexpected fireTestStarted("
                            + description.getDisplayName() + ")\n";
                }
                curTest = description;
            }

            public void fireTestFinished(Description description) {
                if (curTest == null) {
                    mockExecutionError += "Unexpected fireTestFinished("
                            + description.getDisplayName() + "\n";
                }
                if (!curTest.equals(description)) {
                    mockExecutionError += "Unexpected fireTestFinished("
                            + description.getDisplayName() + "); expect "
                            + curTest.getDisplayName() + "\n";
                }
                curTest = null;
                mockExecutionOK = true;
            }
        };

        runner.run(notifierMock);
        assertTrue(mockExecutionError, mockExecutionError.length() == 0);
        assertTrue(mockExecutionOK);
    }

    public void testMissingSuiteMethodInitializationError() {
        try {
            new AntUnitSuiteRunner(JUnit4AntUnitRunnableWithoutSuiteMethod.class);
            fail("InitializationError expected");
        } catch (InitializationError e) {
            String msg = e.getCauses().get(0).getMessage();
            assertTrue("Unexpected error : " + msg, msg.contains("suite"));
        }
    }

    public void testNonStaticSuiteMethodInitializationError() {
        try {
            new AntUnitSuiteRunner(JUnit4AntUnitRunnableWithNonStaticSuite.class);
            fail("InitializationError expected");
        } catch (InitializationError e) {
            String msg = e.getCauses().get(0).getMessage();
            assertTrue("Unexpected error : " + msg, msg.contains("suite"));
            assertTrue("Unexpected error : " + msg, msg.contains("static"));
        }
    }

    public void testInvalidSuiteReturnTypeError() {
        try {
            new AntUnitSuiteRunner(JUnit4AntUnitRunnableWithInvalidSuiteReturnType.class);
            fail("InitializationError expected");
        } catch (InitializationError e) {
            String msg = e.getCauses().get(0).getMessage();
            assertTrue("Unexpected error : " + msg, msg.contains("suite"));
            assertTrue("Unexpected error : " + msg, msg.contains("AntUnitSuite"));
        }
    }

    public void testInvalidSuiteReturnNull() {
        try {
            new AntUnitSuiteRunner(JUnit4AntUnitRunnableWithInvalidSuiteReturningNull.class);
            fail("InitializationError expected");
        } catch (InitializationError e) {
            String msg = e.getCauses().get(0).getMessage();
            assertTrue("Unexpected error : " + msg, msg.contains("suite"));
            assertTrue("Unexpected error : " + msg, msg.contains("null"));
        }
    }


    public void testInvalidSuiteReferencingMissingFile() {
        try {
            new AntUnitSuiteRunner(JUnit4AntUnitRunnableRefferencingIncorrectFile.class);
            fail("InitializationError expected");
        } catch (InitializationError e) {
            String msg = e.getCauses().get(0).getMessage();
            assertTrue("Unexpected error : " + msg, msg.contains("build_script_not_found.xml"));
        }
    }

    public static class JUnit4AntUnitRunnable {
        public static AntUnitSuite suite() {
            File f = new File("src/etc/testcases/antunit/junit.xml");
            return new AntUnitSuite(f, JUnit4AntUnitRunnable.class);
        }
    }

    public static class JUnit4AntUnitRunnableWithNonStaticSuite {
        public AntUnitSuite suite() {
            File f = new File("src/etc/testcases/antunit/junit.xml");
            return new AntUnitSuite(f,
                    JUnit4AntUnitRunnableWithNonStaticSuite.class);
        }
    }

    public static class JUnit4AntUnitRunnableWithoutSuiteMethod {
    }

    public static class JUnit4AntUnitRunnableWithInvalidSuiteReturnType {
        public static TestSuite suite() {
            return new TestSuite("We don't support returning generic TestSuite." +
                    "  The Runner can not handle that");
        }
    }
    
    public static class JUnit4AntUnitRunnableWithInvalidSuiteReturningNull {
        public static TestSuite suite() {
            return null;
        }
    }

    
    public static class JUnit4AntUnitRunnableRefferencingIncorrectFile {
        public static AntUnitSuite suite() {
            File f = new File("build_script_not_found.xml");
            return new AntUnitSuite(f,
                    JUnit4AntUnitRunnableWithNonStaticSuite.class);
        }
    }

}
