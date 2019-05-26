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
package org.apache.ant.antunit.junit3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;

import org.apache.tools.ant.util.FileUtils;

public class AntUnitTestCaseTest extends TestCase {

    File f = new File("src/etc/testcases/antunit/junit.xml");
    File invalidF = new File("invalidFile");
    String test1Name = new AntUnitTestCase.TestCaseName(f, "test1").getName();
    String unknownName = new AntUnitTestCase.TestCaseName(f, "unknown").getName();
    String nameForInvalidF = new AntUnitTestCase.TestCaseName(invalidF, "x").getName();

    File outFile = new File("target/test_output/junit_out.xml");

    public void testNameParsing() {
        AntUnitTestCase.TestCaseName nameObj = new AntUnitTestCase.TestCaseName(
                test1Name);
        assertEquals(f, nameObj.getScript());
        assertEquals("test1", nameObj.getTarget());
    }

    public void testRunSuiteSetUp() throws FileNotFoundException, IOException {
        // When eclipse has to run a specific testCase (user click Run on it),
        // an AntUnitTestCase(name) is created, and the run should execute the
        // suiteSetup/SuiteTearDown
        AntUnitTestCase antUnitTestCase = new AntUnitTestCase(test1Name);

        TestResult testResult = new TestResult();
        antUnitTestCase.run(testResult);
        assertTrue(testResult.wasSuccessful());

        String output = FileUtils.readFully(new FileReader(outFile));
        assertEquals("suiteSetUp-setUp-test1-tearDown-suiteTearDown", output);
    }

    private Test startedTest = null;
    private Test endedTest = null;

    public void testTestIdentityInNotification() {
        // When eclipse has to run a specific testCase (user click Run on it),
        // an AntUnitTestCase(name) is created, and this instance must be used
        // in the notification (otherwise the test appears twice, once normal 
        // but never executed, and once with "Unrooted Tests" parent.
        TestResult testResultMock = new TestResult() {
            public void startTest(Test test) {
                // Note that putting an assertion here to fail fatser doesn't
                // work because
                // exceptions are catched by the runner
                startedTest = test;
            }

            public void endTest(Test test) {
                endedTest = test;
            }
        };

        AntUnitTestCase antUnitTestCase = new AntUnitTestCase(test1Name);
        antUnitTestCase.run(testResultMock);

        assertSame(antUnitTestCase, startedTest);
        assertSame(antUnitTestCase, endedTest);
    }
    
    public void testUnknownTarget() {
        //when the antscript has changed (the target has been removed) and the user try 
        //to rerun this target.
        TestResult testResult = new TestResult();

        AntUnitTestCase antUnitTestCase = new AntUnitTestCase(unknownName);
        antUnitTestCase.run(testResult);

        assertEquals(1 , testResult.errorCount());
        TestFailure error = (TestFailure) testResult.errors().nextElement();
        assertSame(antUnitTestCase, error.failedTest());
        assertTrue("Unexpected error : " + error.exceptionMessage(),
                error.exceptionMessage().contains("unknown"));
    }

    public void testInvalidFile() {
        //when the ant script has changed (or just disappeared) and the user try 
        //to rerun this target.
        TestResult testResult = new TestResult();

        AntUnitTestCase antUnitTestCase = new AntUnitTestCase(nameForInvalidF);
        antUnitTestCase.run(testResult);

        assertEquals(1 , testResult.errorCount());
        TestFailure error = (TestFailure) testResult.errors().nextElement();
        assertSame(antUnitTestCase, error.failedTest());
        assertTrue("Unexpected error : " + error.exceptionMessage(),
                error.exceptionMessage().contains("invalidFile"));
    }

}
