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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;

import org.apache.ant.antunit.junit3.AntUnitSuite;
import org.apache.ant.antunit.junit3.AntUnitTestCase;
import org.junit.internal.runners.CompositeRunner;
import org.junit.internal.runners.InitializationError;

/**
 * JUnit4 Runner to put in a RunWith annotation of the AntUnitSuite when using a
 * JUnit4 runner. Using this runner is not mandatory because junit4 is able to
 * run junit3 test. However, the test will be faster (TODO make that true :-) )
 * with this Runner. Also, more features are available when this runner is used
 * (filtering & sorting) 
 * TODO Support filtering and sorting
 */
public class AntUnitSuiteRunner extends CompositeRunner {

    private AntUnitSuiteRunner(AntUnitSuite suite, Class junitTestClass) {
        super(suite.getName());
        Enumeration tests = suite.tests();
        while (tests.hasMoreElements()) {
            AntUnitTestCase tc = (AntUnitTestCase) tests.nextElement();
            add(new AntUnitTestCaseRunner(tc, junitTestClass));
        }
    }

    public AntUnitSuiteRunner(Class testCaseClass) throws InitializationError {
        this(getJUnit3AntSuite(testCaseClass), testCaseClass);
    }

    private static AntUnitSuite getJUnit3AntSuite(Class testCaseClass)
            throws InitializationError {
        try {
            Method suiteMethod = testCaseClass.getMethod("suite", new Class[0]);
            if (!Modifier.isStatic(suiteMethod.getModifiers())) {
                throw new InitializationError("suite method must be static");
            }
            return (AntUnitSuite) suiteMethod.invoke(null, new Object[0]);
        } catch (NoSuchMethodException e) {
            throw new InitializationError(new Throwable[] { e });
        } catch (IllegalAccessException e) {
            throw new InitializationError(new Throwable[] { e });
        } catch (InvocationTargetException e) {
            throw new InitializationError(new Throwable[] { e });
        } catch (ClassCastException e) {
            throw new InitializationError(new Throwable[] { e });
        }
    }

}
