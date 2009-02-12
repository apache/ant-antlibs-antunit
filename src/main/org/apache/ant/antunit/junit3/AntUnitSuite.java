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

package org.apache.ant.antunit.junit3;

import java.io.File;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.ant.antunit.AntUnitScriptRunner;
import org.apache.ant.antunit.ProjectFactory;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

/**
 * A JUnit 3 TestSuite that group a suite of AntUnit targets coming from an ant
 * script.
 */
public class AntUnitSuite extends TestSuite {

    private final AntUnitScriptRunner antScriptRunner;
    private final MultiProjectDemuxOutputStream stderr;
    private final MultiProjectDemuxOutputStream stdout;

    /**
     * Create a JUnit TestSuite that when executed will run the given ant
     * script.<br/> 
     * Note that it is the responsibility of the caller to give the correct
     * File reference. Namely, if the File is a relative file, it will
     * be resolve relatively to the execution directory (which might be
     * different different from the project root directory).
     * 
     * @param scriptFile
     *            AntUnit script file
     * @param rootClass
     *            The test class that creates this suite. This is used to give
     *            a name to the suite so that an IDE can reexecute this suite.
     */
    public AntUnitSuite(final File scriptFile, Class rootClass) {
        this(scriptFile);
        setName(rootClass.getName());// Allows eclipse to reexecute the test
    }

    /**
     * Constructor used by AntUnitTestCase when a single test case is created.
     * The difference with the public constructor is this version doesn't set
     * the name.
     */
    AntUnitSuite(final File scriptFile) {
        MyProjectFactory prjFactory = new MyProjectFactory(scriptFile);
        antScriptRunner = new AntUnitScriptRunner(prjFactory);
        stdout = new MultiProjectDemuxOutputStream(antScriptRunner, false);
        stderr = new MultiProjectDemuxOutputStream(antScriptRunner, true);
        setName(antScriptRunner.getName() + "[" + scriptFile + "]"); 
        List testTargets = antScriptRunner.getTestTartgets();
        for (Iterator it = testTargets.iterator(); it.hasNext();) {
            String target = (String) it.next();
            AntUnitTestCase tc = new AntUnitTestCase(this, scriptFile, target);
            addTest(tc);
        }
    }

    /**
     * @Override Run the full AntUnit suite
     */
    public void run(TestResult testResult) {
        List testTartgets = antScriptRunner.getTestTartgets();
        runInContainer(testTartgets, testResult, tests());
    }

    /**
     * @Override Run a single test target of the AntUnit suite. suiteSetUp,
     *           setUp, tearDown and suiteTearDown are executed around it.
     */
    public void runTest(Test test, TestResult result) {
        String targetName = ((AntUnitTestCase) test).getTarget();
        List singleTargetList = Collections.singletonList(targetName);
        Enumeration singleTestList = Collections.enumeration(Collections
                .singletonList(test));
        runInContainer(singleTargetList, result, singleTestList);
    }

    /**
     * Execute the test suite in a 'container' similar to the ant 'container'.
     * When ant executes a project it redirect the input and the output. In this
     * context we will only redirect output (unit test are not supposed to be
     * interactive)
     * 
     * @param targetList
     *            The list of test target to execute
     * @param result
     *            The JUnit3 TestResult receiving result notification
     * @param tests
     *            The JUnit3 Test classes instances to use in the notification.
     */
    private void runInContainer(List targetList, TestResult result,
            Enumeration/*<Test>*/tests) {
        JUnitNotificationAdapter notifier = new JUnitNotificationAdapter(
                result, tests);
        PrintStream savedErr = System.err;
        PrintStream savedOut = System.out;
        try {
            System.setOut(new PrintStream(stdout));
            System.setErr(new PrintStream(stderr));
            antScriptRunner.runSuite(targetList, notifier);
        } finally {
            System.setOut(savedOut);
            System.setErr(savedErr);
        }
    }

    /**
     * The antscript project factory that creates projects in a junit context.
     */
    private static class MyProjectFactory implements ProjectFactory {

        private final File scriptFile;
        private final PrintStream realStdErr = System.err;
        private final PrintStream realStdOut = System.out;

        public MyProjectFactory(File scriptFile) {
            this.scriptFile = scriptFile;
        }

        public Project createProject() { 
            ProjectHelper prjHelper = ProjectHelper.getProjectHelper();
            Project prj = new Project();
            DefaultLogger logger = new DefaultLogger();
            logger.setMessageOutputLevel(Project.MSG_INFO);
            logger.setErrorPrintStream(realStdErr);
            logger.setOutputPrintStream(realStdOut);
            prj.addBuildListener(logger);
            String absolutePath = scriptFile.getAbsolutePath();
            prj.setUserProperty(MagicNames.ANT_FILE, absolutePath);
            prj.addReference(ProjectHelper.PROJECTHELPER_REFERENCE, prjHelper);
            prj.init();
            prjHelper.parse(prj, scriptFile);
            return prj;
        }
    }

}
