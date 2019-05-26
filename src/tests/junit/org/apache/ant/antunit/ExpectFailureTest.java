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

public class ExpectFailureTest extends BuildFileTest {

    public ExpectFailureTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        configureProject("src/etc/testcases/expectfailure.xml");
    }

    public void testPassNoMessage() {
        testPass("passNoMessage");
    }
    public void testPassMessage() {
        testPass("passMessage");
    }

    public void testFailNoMessage() {
        testFail("failNoMessage", "Expected build failure");
    }
    public void testFailMessage() {
        testFail("failWrongMessage", "Expected build failure with message 'bar'"
                 + " but was 'foo'");
    }
    public void testFailNoMessageMessageSet() {
        testFail("failNoMessageMessageSet", "oops");
    }
    public void testFailMessageMessageSet() {
        testFail("failWrongMessageMessageSet", "oops");
    }

    private void testPass(String target) {
        executeTarget(target);
    }
    private void testFail(String target, String message) {
        try {
            executeTarget(target);
            fail("Expected an exception");
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
         }
    }
}
