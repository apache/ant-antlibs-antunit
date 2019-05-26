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

import org.apache.tools.ant.BuildException;

import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * JUnit TestCase that will executes a single AntUnit target.
 * <p>This class is not
 * supposed to be used directly.</p>
 * <p>It is public only because junit must access it as a public.</p>
 */
public class AntUnitTestCase extends TestCase {
    // We have to extends TestCase, and not implements Test because otherwise 
    // JUnit4 will derive the Description composing the suite description from 
    // this className only (AntUnitTestCase), and not from the name.
    // However, during execution it use the right Description (base on the 
    // toString)

    /**
     * AntUnitSuite that contains this AntUnitTestCase. Execution is done via
     * this suite
     */
    private final AntUnitSuite suite;

    /**
     * The test target
     */
    private final String target;

    /**
     * Store the exception when the project can not be parsed, but only if this
     * class has been created directly by the IDE from its name.
     * In case of initialisation problem when the test is build from the suite,
     * the problem is handled at the level of the suite (and this object is never
     * created) 
     */
    private final BuildException initialisationException;
    
    /**
     * Prepare an AntUnitTestCase that will be executed alone.
     * <p>This constructor 
     * is typically used by a junit 3 runner that will reexecute a specific 
     * test.</p>
     * <p>The execution of this test will be embed in a suiteSetUp and 
     * suiteTearDown.</p>
     * @param name The name of the AntUnitTestCase, normally obtained from a 
     * previous execution. 
     */
    public AntUnitTestCase(String name) {
        super(name);
        BuildException catchedEx = null;
        AntUnitSuite createdSuite = null;
        TestCaseName nameParser = new TestCaseName(name);        
        try {
            createdSuite = new AntUnitSuite(this, nameParser.getScript());
        } catch (BuildException e) {
            catchedEx = e;
        }
        this.initialisationException = catchedEx;
        this.suite = createdSuite;
        this.target = nameParser.getTarget();
        //There is no need to check here if the target still exists.  This check
        //will be done during execution, and we will get a very nice error
    }

    /**
     * Prepare an AntUnitTestCase that will be executed in a suite. It is the
     * suite that prepare the antScriptRunner and the JUnitExcutionPlatform. It
     * is the responsibility of the suite to execute the suiteSetUp and the
     * suiteTearDown.
     * 
     * @param target test target 
     * @param suite test suite
     * @param scriptFile test file
     */
    public AntUnitTestCase(AntUnitSuite suite, File scriptFile, String target) {
        // The name can be reused by eclipse when running a single test
        super(new TestCaseName(scriptFile, target).getName());
        this.target = target;
        this.suite = suite;
        this.initialisationException = null;
    }

    /**
     * Get the AntUnit test target name.
     * @return target name
     */
    public String getTarget() {
        return target;
    }

    /** 
     * Called by a Junit Runner that want to executes specifically
     * this test target.
     * <p>This implementation delegates the call to the suite.</p>
     */
    public void run(TestResult result) {
        if (initialisationException==null && suite!=null) {
            //normal case, the test is executed from the suite
            suite.runTest(this, result);
        } else {
            //special case, the suite failed to be created
            //the execution will be handled by this object
            //directly
            super.run(result);
        }
    }

    /**
     * Normally never used because this object delegates all execution
     * to an AntUnitSuite.  However, when the suite can not be created
     * (because the ant project is invalid), this object is executed
     * and just throws the exception.
     */
    protected void runTest() throws BuildException {
        throw initialisationException;
    }
    
    /**
     * Handle the serialization and the parsing of the name of a TestCase. The
     * name of the TestCase contains the filename of the script and the target,
     * so that the name uniquely identify the TestCase, and a TestCase can be
     * executed from its name.
     */
    static class TestCaseName {
        private final String name;
        private final File script;
        private final String target;

        public TestCaseName(String name) {
            this.name = name;
            this.target = name.substring(0, name.indexOf(' '));
            String filename = name.substring(name.indexOf(' ') + 2, name
                    .length() - 1);
            this.script = new File(filename);
        }

        public TestCaseName(File script, String target) {
            this.script = script;
            this.target = target;
            this.name = target + " [" + script + "]";
        }

        public String getName() {
            return name;
        }

        public File getScript() {
            return script;
        }

        public String getTarget() {
            return target;
        }
    }

}
