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

package org.apache.ant.antunit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * Run all targets in a given build file who's name starts with "test".
 *
 * <p>Run the "setUp" target before each of them if present, same for
 * "tearDown" after each "test" target.  If a target throws an
 * AssertionFailedException, the test has failed, any other exception
 * is counted as an error (although BuildException will be scanned
 * recursively for nested AssertionFailedExceptions).</p>
 */
public class AntUnit extends Task {

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
     * The build files to process.
     */
    private ArrayList filesets = new ArrayList();

    /**
     * project instance for the build file currently under test.
     */
    private Project newProject;

    /**
     * listeners.
     */
    private ArrayList listeners = new ArrayList();

    /**
     * has a failure occured?
     */
    private int failures=0;
    /**
     * has an error occured?
     */
    private int errors=0;
    /**
     * stop testing if an error or failure occurs?
     */
    private boolean failOnError=true;

    /**
     * Message to print if an error or failure occured.
     */
    public static final String ERROR_TESTS_FAILED = "Tests failed with ";

    /**
     * Message if no tests have been specified.
     */
    public static final String ERROR_NO_FILESET =
        "You must specify at least one nested fileset.";

    /**
     * adds build files to run as tests.
     */
    public void add(ResourceCollection rc) {
        filesets.add(rc);
    }

    /**
     * Adds a test listener.
     */
    public void add(AntUnitListener al) {
        listeners.add(al);
    }

    /**
     * stop testing if an error or failure occurs?
     */
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public void execute() {
        if (filesets.size() == 0) {
            throw new BuildException(ERROR_NO_FILESET);
        }
        Iterator iter = filesets.iterator();
        while (iter.hasNext()) {
            doFileSet((ResourceCollection) iter.next());
        }
        if (failOnError && (failures > 0 || errors > 0)) {
            throw new BuildException(ERROR_TESTS_FAILED
                    + failures + " failure" + (failures != 1 ? "s" : "")
                    + " and "
                    + errors + " error" + (errors != 1 ? "s" : ""));
        }
    }

    /**
     * Processes a fileset.
     */
    private void doFileSet(ResourceCollection rc) {
        Iterator i = rc.iterator();
        while(i.hasNext()) {
            doFile(((FileResource)i.next()).getFile());
        }
    }

    /**
     * Processes a single build file.
     */
    private void doFile(File f) {
        // setup project instance
        newProject = new Project();
        newProject.setDefaultInputStream(getProject().getDefaultInputStream());
        newProject.setJavaVersionProperty();
        newProject.setInputHandler(getProject().getInputHandler());
        getProject().initSubProject(newProject);
        newProject.setUserProperty("ant.file" , f.getAbsolutePath());
        attachListeners(f, newProject);

        // read build file
        ProjectHelper.configureProject(newProject, f);

        // find targets
        Map targets = newProject.getTargets();
        Target setUp = (Target) targets.get(SETUP);
        Target tearDown = (Target) targets.get(TEARDOWN);

        // start test
        newProject.fireBuildStarted();
        Throwable caught = null;
        try {
            Iterator iter = targets.keySet().iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                if (name.startsWith(TEST)) {
                    Vector v = new Vector();
                    if (setUp != null) {
                        v.add(SETUP);
                    }
                    v.add(name);
                    LogCapturer lc = new LogCapturer(newProject);
                    try {
                        newProject.executeTargets(v);
                    } catch (AssertionFailedException e) {
                        fireFail(name, e);
                    } catch (BuildException e) {
                        BuildException orig = e;
                        boolean failed = false;

                        // try to see whether the BuildException masks
                        // an AssertionFailedException.  if so, treat
                        // it as failure instead of error.
                        Throwable t = e.getCause();
                        while (t != null && t instanceof BuildException) {
                            if (t instanceof AssertionFailedException) {
                                failed = true;
                                fireFail(name, (AssertionFailedException) t);
                                break;
                            }
                            t = ((BuildException) t).getCause();
                        }

                        if (!failed) {
                            fireError(name, e);
                        }
                    } finally {
                        // clean up
                        if (tearDown != null) {
                            newProject.executeTarget(TEARDOWN);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            caught = e;
        } finally {
            newProject.fireBuildFinished(caught);
            newProject = null;
        }
    }
        
    /**
     * Redirect output to new project instance.
     */
    public void handleOutput(String outputToHandle) {
        if (newProject != null) {
            newProject.demuxOutput(outputToHandle, false);
        } else {
            super.handleOutput(outputToHandle);
        }
    }

    /**
     * Redirect input to new project instance.
     */
    public int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        if (newProject != null) {
            return newProject.demuxInput(buffer, offset, length);
        }
        return super.handleInput(buffer, offset, length);
    }

    /**
     * Redirect flush to new project instance.
     */
    public void handleFlush(String toFlush) {
        if (newProject != null) {
            newProject.demuxFlush(toFlush, false);
        } else {
            super.handleFlush(toFlush);
        }
    }

    /**
     * Redirect error output to new project instance.
     */
    public void handleErrorOutput(String errorOutputToHandle) {
        if (newProject != null) {
            newProject.demuxOutput(errorOutputToHandle, true);
        } else {
            super.handleErrorOutput(errorOutputToHandle);
        }
    }

    /**
     * Redirect error flush to new project instance.
     */
    public void handleErrorFlush(String errorOutputToFlush) {
        if (newProject != null) {
            newProject.demuxFlush(errorOutputToFlush, true);
        } else {
            super.handleErrorFlush(errorOutputToFlush);
        }
    }

    /**
     * Wraps all registered test listeners in BuildListeners and
     * attaches them to the new project instance.
     */
    private void attachListeners(File buildFile, Project p) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            AntUnitListener al = (AntUnitListener) it.next();
            p.addBuildListener(new BuildToAntUnitListener(buildFile
                                                          .getAbsolutePath(),
                                                          al));
        }
    }

    /**
     * invokes addFailure on all registered test listeners.
     */
    private void fireFail(String targetName, AssertionFailedException ae) {
        failures++;
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            AntUnitListener al = (AntUnitListener) it.next();
            al.addFailure(targetName, ae);
        }
    }

    /**
     * invokes addError on all registered test listeners.
     */
    private void fireError(String targetName, Throwable t) {
        errors++;
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            AntUnitListener al = (AntUnitListener) it.next();
            al.addError(targetName, t);
        }
    }

    /**
     * Adapts AntUnitListener to BuildListener.
     */
    private class BuildToAntUnitListener implements BuildListener {
        private String buildFile;
        private AntUnitListener a;

        BuildToAntUnitListener(String buildFile, AntUnitListener a) {
            this.buildFile = buildFile;
            this.a = a;
            a.setOutput(new LogOutputStream(AntUnit.this, Project.MSG_INFO));
        }

        public void buildStarted(BuildEvent event) {
            a.startTestSuite(buildFile);
        }
        public void buildFinished(BuildEvent event) {
            a.endTestSuite(buildFile);
        }
        public void targetStarted(BuildEvent event) {
            String tName = event.getTarget().getName();
            if (tName.startsWith(TEST)) {
                a.startTest(tName);
            }
        }
        public void targetFinished(BuildEvent event) {
            String tName = event.getTarget().getName();
            if (tName.startsWith(TEST)) {
                a.endTest(tName);
            }
        }
        public void taskStarted(BuildEvent event) {}
        public void taskFinished(BuildEvent event) {}
        public void messageLogged(BuildEvent event) {}
    }

}
