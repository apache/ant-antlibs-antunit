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

import org.apache.tools.ant.BuildFileTest;

public class AssertTest extends BuildFileTest {

    public AssertTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/assert.xml");
    }

    public void testFail() {
        testFail("fail", "Test failed");
    }
    public void testFailWithMessage() {
        testFail("failWithMessage", "This test is expecting to fail");
    }
    public void testTruePass() {
        testPass("assertTruePass");
    }
    public void testFalsePass() {
        testPass("assertFalsePass");
    }
    public void testEqualsPass() {
        testPass("assertEqualsPass");
    }
    public void testEqualsCasePass() {
        testPass("assertEqualsCasePass");
    }
    public void testPropertySetPass() {
        testPass("assertPropertySetPass");
    }
    public void testPropertyEqualsPass() {
        testPass("assertPropertyEqualsPass");
    }
    public void testPropertyEqualsCasePass() {
        testPass("assertPropertyEqualsCasePass");
    }
    public void testFileExistsPass() {
        testPass("assertFileExistsPass");
    }
    public void testFileDoesntExistPass() {
        testPass("assertFileDoesntExistPass");
    }
    public void testResourceExistsPass() {
        testPass("assertResourceExistsPass");
    }
    public void testResourceDoesntExistPass() {
        testPass("assertResourceDoesntExistPass");
    }
    public void testDestIsUptodatePass() {
        testPass("assertDestIsUptodatePass");
    }
    public void testDestIsOutofdatePass() {
        testPass("assertDestIsOutofdatePass");
    }
    public void testFilesMatchPass() {
        testPass("assertFilesMatchPass");
    }
    public void testFilesDifferPass() {
        testPass("assertFilesDifferPass");
    }
    public void testReferenceSetPass() {
        testPass("assertReferenceSetPass");
    }
    public void testReferenceIsTypePass() {
        testPass("assertReferenceIsTypePass");
    }

    public void testTrueFail() {
        testFail("assertTrueFail");
    }
    public void testFalseFail() {
        testFail("assertFalseFail");
    }
    public void testEqualsFail1() {
        testFail("assertEqualsFail1", "Expected 'bar' but was 'baz'");
    }
    public void testEqualsFail2() {
        testFail("assertEqualsFail2", "Expected 'bar' but was 'BAR'");
    }
    public void testPropertySetFail() {
        testFail("assertPropertySetFail", "Expected property 'foo'");
    }
    public void testPropertyEqualsFail1() {
        testFail("assertPropertyEqualsFail1", "Expected property 'foo' to have value 'bar' but was '${foo}'");
    }
    public void testPropertyEqualsFail2() {
        testFail("assertPropertyEqualsFail2", "Expected property 'foo' to have value 'baz' but was 'bar'");
    }
    public void testPropertyEqualsFail3() {
        testFail("assertPropertyEqualsFail3", "Expected property 'foo' to have value 'BAR' but was 'bar'");
    }
    public void testPropertyContains() {
        testPass("assertPropertyContains");
    }
    public void testPropertyContainsFail() {
        testFail("assertPropertyContainsFail", "Expected property 'foo' to contain value 'foo' but was 'bar'");
    }
    public void testFileExistsFail() {
        testFail("assertFileExistsFail",
                 "Expected file 'assert.txt' to exist");
    }
    public void testFileDoesntExistFail() {
        testFail("assertFileDoesntExistFail",
                 "Didn't expect file 'assert.xml' to exist");
    }
    public void testResourceExistsFail() {
        testFail("assertResourceExistsFail",
                 "Expected resource 'assert.txt' to exist");
    }
    public void testResourceDoesntExistFail() {
        testFail("assertResourceDoesntExistFail",
                 "Didn't expect resource 'assert.xml' to exist");
    }
    public void testDestIsUptodateFail() {
        testFail("assertDestIsUptodateFail",
                 "Expected '../../main/org/apache/ant/antunit/AssertTask.java' to be more recent than '../../../build/classes/org/apache/ant/antunit/AssertTask.class'");
    }
    public void testDestIsOutofdateFail() {
        testFail("assertDestIsOutofdateFail",
                 "Expected '../../main/org/apache/ant/antunit/AssertTask.java' to be more recent than '../../../build/classes/org/apache/ant/antunit/AssertTask.class'");
    }
    public void testFilesMatchFail() {
        testFail("assertFilesMatchFail",
                 "Expected files 'assert.xml' and 'antunit.xml' to match");
    }
    public void testFilesDifferFail() {
        testFail("assertFilesDifferFail",
                 "Expected files 'assert.xml' and 'assert.xml' to differ");
    }
    public void testReferenceSetFail() {
        testFail("assertReferenceSetFail", "Expected reference 'foo2'");
    }
    public void testReferenceIsTypeFailNotSet() {
        testFail("assertReferenceIsTypeFailNotSet",
                 "Expected reference 'foo4'");
    }
    public void testReferenceIsTypeFailWrongType() {
        testFail("assertReferenceIsTypeFailWrongType",
                 "Expected reference 'foo5' to be a 'fileset'");
    }

    public void testMatches() {
        executeTarget("assertMatches");
    }

    public void testDoesntMatch() {
        executeTarget("assertDoesntMatch");
    }

    public void testMatchesDefaultCaseSensitivity() {
        executeTarget("assertMatchesDefaultCaseSensitivity");
    }

    private void testPass(String target) {
        executeTarget(target);
    }

    private void testFail(String target) {
        testFail(target, "Assertion failed");
    }

    private void testFail(String target, String message) {
        try {
            executeTarget(target);
            fail("Expected failed assetion");
        } catch (AssertionFailedException e) {
            assertEquals(message, e.getMessage());
        } catch (Throwable t) {
            if (t.getClass().getName().equals(
                    AssertionFailedException.class.getName())) {
                // Some classloader issue!
                assertEquals(message, t.getMessage());
            } else {
                fail("Unexpected exception of type " + t.getClass()
                     + ", message '" + t.getMessage() + "'"
                     + "\nexpected exception of type "
                     + AssertionFailedException.class);
            }
        } // end of try-catch
    }
}
