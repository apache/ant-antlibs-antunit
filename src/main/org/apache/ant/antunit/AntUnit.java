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
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;

/**
 * Run every target whose name starts with "test" in a set of build files.
 *
 * <p>Run the "setUp" target before each of them if present, same for
 * "tearDown" after each "test*" target (targets named just "test" are
 * ignored).  If a target throws an AssertionFailedException, the test
 * has failed; any other exception is considered an error (although
 * BuildException will be scanned recursively for nested
 * AssertionFailedExceptions).</p>
 */
public class AntUnit extends Task {

    /**
     * Message to print if an error or failure occured.
     */
    public static final String ERROR_TESTS_FAILED = "Tests failed with ";

    /**
     * Message if no tests have been specified.
     */
    public static final String ERROR_NO_TESTS =
        "You must specify build files to test.";

    /**
     * Message if non-File resources have been specified.
     */
    public static final String ERROR_NON_FILES =
        "Only file system resources are supported.";

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
    private Union buildFiles;

    /**
     * project instance for the build file currently under test.
     */
    private Project newProject;

    /**
     * listeners.
     */
    private ArrayList listeners = new ArrayList();

    /**
     * propertysets.
     */
    private ArrayList propertySets = new ArrayList();

    /**
     * has a failure occured?
     */
    private int failures = 0;

    /**
     * has an error occured?
     */
    private int errors = 0;

    /**
     * stop testing if an error or failure occurs?
     */
    private boolean failOnError = true;

    /**
     * Name of a property to set in case of an error.
     */
    private String errorProperty = null;

    /**
     * Add build files to run as tests.
     * @param rc the ResourceCollection to add.
     */
    public void add(ResourceCollection rc) {
        if (buildFiles == null) {
            buildFiles = new Union();
        }
        buildFiles.add(rc);
    }

    /**
     * Add a test listener.
     * @param al the AntUnitListener to add.
     */
    public void add(AntUnitListener al) {
        listeners.add(al);
        al.setParentTask(this);
    }

    /**
     * Add a PropertySet.
     * @param ps the PropertySet to add.
     */
    public void addPropertySet(PropertySet ps) {
        propertySets.add(ps);
    }

    /**
     * Set the name of a property to set if an error or failure occurs.
     * @param s the name of the error property.
     */
    public void setErrorProperty(String s) {
        errorProperty = s;
    }

    /**
     * Set whether to stop testing if an error or failure occurs?
     * @param failOnError default <code>true</code>
     */
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Execute the tests.
     */
    public void execute() {
        if (buildFiles == null) {
            throw new BuildException(ERROR_NO_TESTS);
        }
        doResourceCollection(buildFiles);
        if (failures > 0 || errors > 0) {
            if (errorProperty != null) {
                getProject().setNewProperty(errorProperty, "true");
            }
            if (failOnError) {
                throw new BuildException(ERROR_TESTS_FAILED
                                         + failures + " failure"
                                         + (failures != 1 ? "s" : "")
                                         + " and "
                                         + errors + " error"
                                         + (errors != 1 ? "s" : ""));
            }
        }
    }

    /**
     * Processes a ResourceCollection.
     */
    private void doResourceCollection(ResourceCollection rc) {
        //should relax this restriction if/when Ant core allows non-files
        if (!rc.isFilesystemOnly()) {
            throw new BuildException(ERROR_NON_FILES);
        }

        Iterator i = rc.iterator();
        while (i.hasNext()) {
            FileResource r = (FileResource) i.next();
            if (r.isExists()) {
                doFile(r.getFile());
            } else {
                log("Skipping " + r + " since it doesn't exist",
                    Project.MSG_VERBOSE);
            }
        }
    }

    /**
     * Processes a single build file.
     */
    private void doFile(File f) {
        log("Running tests in build file " + f, Project.MSG_DEBUG);

        // setup project instance
        newProject = createProjectForFile(f);

        // find targets
        Map targets = newProject.getTargets();
        boolean setUp = targets.containsKey(SETUP);
        boolean tearDown = targets.containsKey(TEARDOWN);

        // start test
        newProject.fireBuildStarted();

        Throwable caught = null;
        try {
            Iterator iter = targets.keySet().iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                if (name.startsWith(TEST) && !name.equals(TEST)) {
                    Vector v = new Vector();
                    if (setUp) {
                        v.add(SETUP);
                    }
                    v.add(name);
                    // create an register a logcapturer on the newProject
                    LogCapturer lc = new LogCapturer(newProject);
                    try {
                        fireStartTest(name);
                        newProject.executeTargets(v);
                    } catch (AssertionFailedException e) {
                        fireFail(name, e);
                    } catch (BuildException e) {
                        boolean failed = false;

                        // try to see whether the BuildException masks
                        // an AssertionFailedException. If so, treat
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
                        // fire endTest here instead of the endTarget
                        // event, otherwise an error would be
                        // registered after the endTest event -
                        // endTarget is called before this method's catch block
                        // is reached.
                        fireEndTest(name);
                        // clean up
                        if (tearDown) {
                            newProject.executeTarget(TEARDOWN);
                        }
                        if (iter.hasNext()) {
                            newProject = createProjectForFile(f);
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
     * @param outputToHandle the output to handle.
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
     * @param buffer the buffer containing the input.
     * @param offset the offset into <code>buffer</code>.
     * @param length the length of the data.
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
     * @param toFlush the output String to flush.
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
     * @param errorOutputToHandle the error output to handle.
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
     * @param errorOutputToFlush the error output to flush.
     */
    public void handleErrorFlush(String errorOutputToFlush) {
        if (newProject != null) {
            newProject.demuxFlush(errorOutputToFlush, true);
        } else {
            super.handleErrorFlush(errorOutputToFlush);
        }
    }

    /**
     * Creates a new project instance and configures it.
     * @param f the File for which to create a Project.
     */
    private Project createProjectForFile(File f) {
        Project p = new Project();
        p.setDefaultInputStream(getProject().getDefaultInputStream());
        p.initProperties();
        p.setInputHandler(getProject().getInputHandler());
        getProject().initSubProject(p);
        for (Iterator outer = propertySets.iterator(); outer.hasNext(); ) {
            PropertySet set = (PropertySet) outer.next();
            Map props = set.getProperties();
            for (Iterator keys = props.keySet().iterator();
                 keys.hasNext(); ) {
                String key = keys.next().toString();
                if (MagicNames.PROJECT_BASEDIR.equals(key)
                    || MagicNames.ANT_FILE.equals(key)) {
                    continue;
                }
                Object value = props.get(key);
                if (value != null && value instanceof String
                    && p.getProperty(key) == null) {
                    p.setNewProperty(key, (String) value);
                }
            }
        }
        p.setUserProperty(MagicNames.ANT_FILE, f.getAbsolutePath());
        attachListeners(f, p);

        // read build file
        ProjectHelper.configureProject(p, f);

        return p;
    }

    /**
     * Wraps all registered test listeners in BuildListeners and
     * attaches them to the new project instance.
     * @param buildFile a build file.
     * @param p the Project to attach to.
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
     * invokes start on all registered test listeners.
     * @param targetName the name of the target.
     */
    private void fireStartTest(String targetName) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            AntUnitListener al = (AntUnitListener) it.next();
            al.startTest(targetName);
        }
    }

    /**
     * invokes addFailure on all registered test listeners.
     * @param targetName the name of the failed target.
     * @param ae the associated AssertionFailedException.
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
     * @param targetName the name of the failed target.
     * @param t the associated Throwable.
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
     * invokes endTest on all registered test listeners.
     * @param targetName the name of the current target.
     */
    private void fireEndTest(String targetName) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            AntUnitListener al = (AntUnitListener) it.next();
            al.endTest(targetName);
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
        }

        public void buildStarted(BuildEvent event) {
            a.startTestSuite(event.getProject(), buildFile);
        }
        public void buildFinished(BuildEvent event) {
            a.endTestSuite(event.getProject(), buildFile);
        }
        public void targetStarted(BuildEvent event) {
        }
        public void targetFinished(BuildEvent event) {
        }
        public void taskStarted(BuildEvent event) {}
        public void taskFinished(BuildEvent event) {}
        public void messageLogged(BuildEvent event) {}
    }

}
