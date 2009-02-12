package org.apache.ant.antunit.junit3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.ant.antunit.junit3.AntUnitTestCase;
import org.apache.tools.ant.util.FileUtils;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;

public class AntUnitTestCaseTest extends TestCase {

    File f = new File("src/etc/testcases/antunit/junit.xml");
    String test1Name = new AntUnitTestCase.TestCaseName(f, "test1").getName();

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

        Assert.assertSame(antUnitTestCase, startedTest);
        Assert.assertSame(antUnitTestCase, endedTest);
    }
}
