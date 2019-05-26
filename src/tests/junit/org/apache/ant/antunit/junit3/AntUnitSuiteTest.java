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
import java.util.Enumeration;

import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;

import org.apache.tools.ant.util.FileUtils;

public class AntUnitSuiteTest extends TestCase {

    AntUnitSuite suite = new AntUnitSuite(new File(
            "src/etc/testcases/antunit/junit.xml"), AntUnitSuiteTest.class);
    File outFile = new File("target/test_output/junit_out.xml");

    public void testRunSuiteSetUp() throws FileNotFoundException, IOException {

        TestResult testResult = new TestResult();
        suite.run(testResult);
        assertTrue(testResult.wasSuccessful());

        String output = FileUtils.readFully(new FileReader(outFile));
        String EXPECT1 = "suiteSetUp-setUp-test1-tearDown-setUp-test2-tearDown-suiteTearDown";
        String EXPECT2 = "suiteSetUp-setUp-test2-tearDown-setUp-test1-tearDown-suiteTearDown";
        assertTrue("unexted output : " + output, EXPECT1.equals(output)
                || EXPECT2.equals(output));
    }

    public void testSuiteName() {
        assertTrue("Expected non empty suite name", 
                suite.getName().trim().length() > 0);
    }

    public void testChildNames() {
        assertTrue("Expected more test, received " + suite.testCount(), 
                suite.testCount() >= 1);

        Enumeration/*<Test>*/tests = suite.tests();
        StringBuffer testTargets = new StringBuffer();
        while (tests.hasMoreElements()) {
            String nextName = tests.nextElement().toString();
            testTargets.append(" ").append(nextName).append(" ,");
        }

        assertTrue("test1 not found in child : " + testTargets, testTargets
                .toString().contains(" test1 "));
    }

    public void testSingleTestRunSuiteSetUp() throws Exception {
        AntUnitTestCase test1 = (AntUnitTestCase) suite.testAt(0);
        if (test1.getTarget().equals("test2")) {
            test1 = (AntUnitTestCase) suite.testAt(1);
        }
        TestResult testResult = new TestResult();
        suite.runTest(test1, testResult);
        assertTrue(testResult.wasSuccessful());

        String output = FileUtils.readFully(new FileReader(outFile));
        assertTrue("unexted output : " + output,
                "suiteSetUp-setUp-test1-tearDown-suiteTearDown".equals(output));
    }

    public void testFileNotFound() throws Exception {
        suite = new AntUnitSuite(new File("xxxx"), AntUnitSuiteTest.class); 
        TestResult testResult = new TestResult();
        suite.run(testResult);
        
        assertNotNull(suite.getName()); 
        assertEquals(1 , testResult.errorCount());
        TestFailure error = (TestFailure) testResult.errors().nextElement();
        assertTrue("Unexpected error : " + error.exceptionMessage(),
                error.exceptionMessage().contains("xxxx"));
    }
    
    //TODO test missing target error reporting
}
