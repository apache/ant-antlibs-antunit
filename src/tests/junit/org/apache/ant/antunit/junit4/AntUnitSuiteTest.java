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
package org.apache.ant.antunit.junit4;

import java.io.File;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.ant.antunit.junit3.AntUnitSuite;
import org.junit.Ignore;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;

public class AntUnitSuiteTest extends TestCase {

    private boolean mockExecutionOK = false;
    private String mockExcutionError = "";

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

        final int TEST_STARTED = 1, TEST_FINISHED = 2;
        RunNotifier notifierMock = new RunNotifier() {
            Description curTest = null;

            public void fireTestStarted(Description description) {
                if (curTest != null) {
                    mockExcutionError += "Unexpected fireTestStarted("
                            + description.getDisplayName() + "\n";
                }
                if (!tDescs.contains(description)) {
                    mockExcutionError += "Unexpected fireTestStarted("
                            + description.getDisplayName() + ")\n";
                }
                curTest = description;
            }

            @Override
            public void fireTestFinished(Description description) {
                if (curTest == null) {
                    mockExcutionError += "Unexpected fireTestFinished("
                            + description.getDisplayName() + "\n";
                }
                if (!curTest.equals(description)) {
                    mockExcutionError += "Unexpected fireTestFinished("
                            + description.getDisplayName() + "); expect "
                            + curTest.getDisplayName() + "\n";
                }
                curTest = null;
                mockExecutionOK = true;
            }
        };

        runner.run(notifierMock);
        assertTrue(mockExcutionError, mockExcutionError.isEmpty());
        assertTrue(mockExecutionOK);
    }

    public void testMissingSuiteMethodInitializationError() {
        try {
            AntUnitSuiteRunner runner = new AntUnitSuiteRunner(
                    JUnit4AntUnitRunnableWithoutSuiteMethod.class);
            fail("InitializationError expected");
        } catch (InitializationError e) {
            String msg = e.getCauses().get(0).getMessage();
            assertTrue("Unexpected error : " + msg, msg.contains("suite"));
        }
    }

    public void testNonStaticSuiteMethodInitializationError() {
        try {
            AntUnitSuiteRunner runner = new AntUnitSuiteRunner(
                    JUnit4AntUnitRunnableWithNonStaticSuite.class);
            fail("InitializationError expected");
        } catch (InitializationError e) {
            String msg = e.getCauses().get(0).getMessage();
            assertTrue("Unexpected error : " + msg, msg.contains("suite"));
            assertTrue("Unexpected error : " + msg, msg.contains("static"));
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

}
