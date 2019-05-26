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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestResult;

import org.apache.ant.antunit.AntUnitExecutionNotifier;
import org.apache.ant.antunit.AssertionFailedException;

/**
 * Adapt AntUnitExecutionNotifier events into JUnit3 TestResult events
 */
class JUnitNotificationAdapter implements AntUnitExecutionNotifier {

    private final TestResult junitTestResult;
    private Map testByTarget = new HashMap(); 

    public JUnitNotificationAdapter(TestResult testResult, Enumeration tests) {
        this.junitTestResult = testResult;
        while(tests.hasMoreElements()) {
            AntUnitTestCase test = (AntUnitTestCase) tests.nextElement();
            testByTarget.put(test.getTarget(), test);
        }
    }

    public void fireStartTest(String targetName) {
        //TODO : if it is null, eclipse stop the unit test (add a unit test)
        junitTestResult.startTest((Test) testByTarget.get(targetName));
    }
    
    public void fireEndTest(String targetName) {
        junitTestResult.endTest((Test) testByTarget.get(targetName));
    }

    public void fireError(String targetName, Throwable t) {
        junitTestResult.addError((Test) testByTarget.get(targetName), t);
    }

    public void fireFail(String targetName, AssertionFailedException ae) {
        //I don't see how to transform the AntUnit assertion exception into 
        //junit assertion exception (we would loose the stack trace).
        //So failures will be reported as errors
        junitTestResult.addError((Test) testByTarget.get(targetName), ae);
    }

}
