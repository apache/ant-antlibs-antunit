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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.ant.antunit.AntUnitExecutionNotifier;
import org.apache.ant.antunit.AssertionFailedException;
import org.apache.ant.antunit.junit3.AntUnitSuite;
import org.apache.ant.antunit.junit3.AntUnitTestCase;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

/**
 * JUnit4 Runner to put in a RunWith annotation of the AntUnitSuite when using a
 * JUnit4 runner. Using this runner is not mandatory because junit4 is able to
 * run junit3 test. However, the test may be faster with this Runner (with the
 * default junit4 adapter, the suiteSetUp and suiteTearDown will be executed
 * around every test target). Also, more features are available when this runner
 * is used (filtering &amp; sorting)
 */
public class AntUnitSuiteRunner extends Runner implements Filterable, Sortable {

    private final AntUnitSuite junit3Suite;
    private final Map/*<String, Description>*/ targetDescriptions = new HashMap();
    private final List/*<String>*/ targetsOrder = new LinkedList();
    
    private AntUnitSuiteRunner(AntUnitSuite suite, Class junitTestClass) throws InitializationError {
        junit3Suite = suite;
        if (suite.hasAntInitError()) {
            throw new InitializationError(
                    new Throwable[] { suite.getAntInitialisationException() } 
                  );
        } else { 
            Enumeration tests = suite.tests();
            while (tests.hasMoreElements()) {
                TestCase nextTc = (TestCase) tests.nextElement();
                //TODO Handle the possibility for the user to define suite of AntUnit scripts            	
            	AntUnitTestCase tc = (AntUnitTestCase) nextTc;
            	Description tc_desc = Description.createTestDescription(junitTestClass, tc.getName());
            	targetDescriptions.put(tc.getTarget(), tc_desc);
            	targetsOrder.add(tc.getTarget());
            }
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
            Object suite = suiteMethod.invoke(null, new Object[0]);
            if (suite == null) {
                throw new InitializationError("suite method can not return null");
            }
            if (!(suite instanceof AntUnitSuite)) {
                throw new InitializationError("suite method must return an AntUnitSuite");
            }
            return (AntUnitSuite) suite;
        } catch (NoSuchMethodException e) {
            throw new InitializationError(new Throwable[] { e });
        } catch (IllegalAccessException e) {
            throw new InitializationError(new Throwable[] { e });
        } catch (InvocationTargetException e) {
            throw new InitializationError(new Throwable[] { e });
        }
    }

    /**
     * Filterable implementation
     */
    public void filter(Filter filter) throws NoTestsRemainException {
        for (Iterator iter= targetDescriptions.entrySet().iterator(); iter.hasNext();) {
            Map.Entry mapEntry = (Entry) iter.next(); 
            if (!filter.shouldRun((Description) mapEntry.getValue()))
                iter.remove();
                targetsOrder.remove(mapEntry.getKey());
        }
    }

    /**
     * Sortable implementation
     */
    public void sort(final Sorter sorter) {
        Collections.sort(targetsOrder, new Comparator/*<String>*/() {
            public int compare(Object target1, Object target2) {
                Description d2 = (Description)targetDescriptions.get(target2);
                Description d1 = (Description)targetDescriptions.get(target1);
                return sorter.compare(d1, d2);
            }
        });
        /*for (Runner each : fRunners)
            sorter.apply(each);
        */
    }

    /**
     * Runner implementation
     */
    public Description getDescription() {
        Description r = Description.createSuiteDescription(
                junit3Suite.getName(), new Annotation[0]);
        
        Collection childDesc = targetDescriptions.values();
        for (Iterator iterator = childDesc.iterator(); iterator.hasNext();) {
            Description desc = (Description) iterator.next();
            r.addChild(desc);
        }
        return r;
    }

    /**
     * Runner implementation
     */
    public void run(final RunNotifier junitNotifier) {
        LinkedList targetList = new LinkedList(targetDescriptions.keySet());
        
        AntUnitExecutionNotifier antUnitNotifier = new AntUnitExecutionNotifier() {            
            public void fireStartTest(String targetName) {
                junitNotifier.fireTestStarted(getDescription(targetName));
            }
            public void fireEndTest(String targetName) {
                junitNotifier.fireTestFinished(getDescription(targetName));                
            }
            public void fireError(String targetName, Throwable t) {
                Failure failure = new Failure(getDescription(targetName), t);
                junitNotifier.fireTestFailure(failure);
            }
            public void fireFail(String targetName, AssertionFailedException ae) {
                Failure failure = new Failure(getDescription(targetName), ae);
                junitNotifier.fireTestFailure(failure);
            }            
            private Description getDescription(String targetName) {
                return (Description) targetDescriptions.get(targetName);
            }
        };
        
        junit3Suite.runInContainer(targetList, antUnitNotifier);
    }

}
