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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/** 
 * Run antunit tests suites.  This AntUnitScriptRunner is responsible for the 
 * management of the ant project and the correct invocation the target (taking 
 * into account properly the [case]setUp and [case]tearDown targets).
 * The user can however provide the order of the test targets and or can filter
 * the list of test targets to execute.
 * The user must also provide its ProjectFactory and an AntUnitExecutionNotifier.
 * @since 1.2 
 */
public class AntUnitScriptRunner {

    /**
     * name of the magic setUp target.
     */
    private static final String SETUP = "setUp";

    /**
     * prefix that identifies test targets.
     */
    private static final String TEST = "test";

    /**
     * name of the magic tearDown target.
     */
    private static final String TEARDOWN = "tearDown";

    /**
     * name of the magic suiteSetUp target.
     */
    private static final String SUITESETUP = "suiteSetUp";

    /**
     * name of the magic suiteTearDown target.
     */
    private static final String SUITETEARDOWN = "suiteTearDown";

    /**
     * Object used to create projects in order to support test isolation.
     */
    private final ProjectFactory prjFactory;
    
    /**
     * Indicates if the startSuite method has been invoked.  Use to fail fast if the
     * the caller forget to call the startSuite method
     */
    private boolean isSuiteStarted;
    
    /**
     * Does that script have a setUp target (defined when scanning the script)
     */
    private final boolean hasSetUp;

    /**
     * Does that script have a tearDown target (defined when scanning the script)
     */
    private final boolean hasTearDown;

    /**
     * Does that script has a suiteSetUp target.
     */
    private final boolean hasSuiteSetUp;

    /**
     * Does that script has a suite tearDown target that should be executed.
     */
    private final boolean hasSuiteTearDown;

    /**
     * List of target names
     */
    private final List testTargets;

    /** 
     * The project currently used.
     */
    private Project project = null;

    /** 
     * Indicates if a target has already be executed using this project. 
     * Value is undefined when project is null.
     */
    private boolean projectIsDirty;

    
    /**
     * Create a new AntScriptRunner on the given environment.
     * @param prjFactory A factory for the ant project that will contains the antunit test to execute.
     * The factory might be invoked multiple time in order to provide test isolation.
     * @throws BuildException The project can not be parsed
     */
    public AntUnitScriptRunner(ProjectFactory prjFactory) throws BuildException {
        this.prjFactory = prjFactory;
        Project newProject = getCurrentProject();
        Map targets = newProject.getTargets();
        hasSetUp = targets.containsKey(SETUP);
        hasTearDown = targets.containsKey(TEARDOWN);
        hasSuiteSetUp = targets.containsKey(SUITESETUP);
        hasSuiteTearDown = targets.containsKey(SUITETEARDOWN);
        testTargets = new LinkedList();
        Iterator it = targets.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            if (name.startsWith(TEST) && !name.equals(TEST)) {
                testTargets.add(name);
            }
        }
    }

    /**
     * Get the project currently in use.  The caller is not allowed to invoke a target or
     * do anything that would break the isolation of the test targets.
     * @throws BuildException The project can not be parsed
     * @return the current project
     */
    public final Project getCurrentProject() throws BuildException {
    	//Method is final because it is called from the constructor
        if (project == null) {
            project = prjFactory.createProject();
            projectIsDirty = false;
        }
        return project;
    }

    /**
     * Get a project that has not yet been used in order to execute a target on it.
     */
    private Project getCleanProject() {
        if (project == null || projectIsDirty) {
            project = prjFactory.createProject();
        }
        //we already set isDirty to true in order to make sure we didn't reuse
        //this project next time getCleanProject is called.  
        projectIsDirty = true;
        return project;
    }

    /**
     * @return List&lt;String&gt; List of test targets of the script file
     */
    public List getTestTartgets() {
        return testTargets;
    }

    /**
     * Provides the name of the active script.
     * @return name of the project
     */
    public String getName() {
        return getCurrentProject().getName();
    }

    /**
     * Executes the suiteSetUp target if presents and report any execution error.
     * <p>A failure is reported to the notifier and by returning false.
     * Note that if the method return false, you are not allowed to run targets.</p>
     * @return false in case of execution failure.  true in case of success. 
     */
    private boolean startSuite(AntUnitExecutionNotifier notifier) {
        getCurrentProject().fireBuildStarted();
        if (hasSuiteSetUp) {
            try {
                Project newProject = getCleanProject();
                newProject.executeTarget(SUITESETUP);
            } catch (BuildException e) {
                notifier.fireStartTest(SUITESETUP);
                fireFailOrError(SUITESETUP, e, notifier);
                return false;
            }
        }
        isSuiteStarted = true; //set to true only if suiteSetUp executed properly.
        return true;
    }

    /** 
     * Run the specific test target, possibly between the setUp and tearDown targets if
     * it exists.  Exception or failures are reported to the notifier.
     * @param name name of the test target to execute.
     * @param notifier will receive execution notifications.
     * @pre startSuite has been invoked successfully
     */
    private void runTarget(String name, AntUnitExecutionNotifier notifier) {
        if (!isSuiteStarted) {
            throw new AssertionError();
        }
        Project newProject = getCleanProject();
        Vector v = new Vector();
        if (hasSetUp) {
            v.add(SETUP);
        }
        v.add(name);
        // create and register a logcapturer on the newProject
        LogCapturer lc = new LogCapturer(newProject);
        try {
            notifier.fireStartTest(name);
            newProject.executeTargets(v);
        } catch (BuildException e) {
            fireFailOrError(name, e, notifier);
        } finally {
            // fire endTest here instead of the endTarget
            // event, otherwise an error would be
            // registered after the endTest event -
            // endTarget is called before this method's catch block
            // is reached.
            notifier.fireEndTest(name);
            // clean up
            if (hasTearDown) {
                try {
                    newProject.executeTarget(TEARDOWN);
                } catch (final BuildException e) {
                    fireFailOrError(name, e, notifier);
                }
            }
        }
    }

    /**
     * Executes the suiteTearDown target if presents and report any execution error.
     * @param caught Any internal exception triggered (and caught) by the caller indicating that 
     * the execution could not be invoked as expected.  
     * @param notifier will receive execution notifications.
     */
    private void endSuite(Throwable caught, AntUnitExecutionNotifier notifier) {
        if (hasSuiteTearDown) {
            try {
                Project newProject = getCleanProject();
                newProject.executeTarget(SUITETEARDOWN);
            } catch (BuildException e) {
                notifier.fireStartTest(SUITETEARDOWN);
                fireFailOrError(SUITETEARDOWN, e, notifier);
            }
        }
        getCurrentProject().fireBuildFinished(caught);
        isSuiteStarted = false;
    }

    /**
     * Try to see whether the BuildException e is an AssertionFailedException
     * or is caused by an AssertionFailedException. If so, fire a failure for 
     * given targetName.  Otherwise fire an error.
     */
    private void fireFailOrError(String targetName, BuildException e, 
                                 AntUnitExecutionNotifier notifier) {
        boolean failed = false;
        Throwable t = e;
        while (t != null && t instanceof BuildException) {
            if (t instanceof AssertionFailedException) {
                failed = true;
                notifier.fireFail(targetName, (AssertionFailedException) t);
                break;
            }
            t = ((BuildException) t).getCause();
        }

        if (!failed) {
            notifier.fireError(targetName, e);
        }
    }


    /**
     * Executes the suite.
     * @param suiteTargets An ordered list of test targets.  It must be a sublist of getTestTargets
     * @param notifier is notified on test progress
     */
    public void runSuite(List suiteTargets, AntUnitExecutionNotifier notifier) {
        Throwable caught = null;
        try {
            if (!startSuite(notifier)) {
                return;
            }
            Iterator iter = suiteTargets.iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                runTarget(name, notifier);
            }
        } catch (Throwable e) {
            caught = e;
        } finally {
            endSuite(caught, notifier);
        }
    }

}
