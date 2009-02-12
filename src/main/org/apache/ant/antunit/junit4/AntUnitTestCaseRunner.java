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

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;

import org.apache.ant.antunit.junit3.AntUnitTestCase;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;


class AntUnitTestCaseRunner extends Runner {

    private final AntUnitTestCase fTest;
    private final Class junitTestClass;

    public AntUnitTestCaseRunner(AntUnitTestCase testCase, Class junitTestClass) {
        this.fTest = testCase;
        this.junitTestClass = junitTestClass;
    }

    public void run(final RunNotifier notifier) {
        final Description description = getDescription();
        TestListener testListener = new TestListener() {
            // TODO implement directly the mapping from AntUnitExecutionNotifier
            // to junit4 RunNotifier
            public void endTest(Test test) {
                notifier.fireTestFinished(description);
            }

            public void startTest(Test test) {
                notifier.fireTestStarted(description);
            }

            public void addError(Test test, Throwable t) {
                Failure failure = new Failure(description, t);
                notifier.fireTestFailure(failure);
            }

            public void addFailure(Test test, AssertionFailedError t) {
                addError(test, t);
            }
        };
        TestResult result = new TestResult();
        result.addListener(testListener);
        fTest.run(result);
    }

    public Description getDescription() {
        return Description.createTestDescription(junitTestClass, fTest
                .getName());
    }

}
