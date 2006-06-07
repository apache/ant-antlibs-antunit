/*
 * Copyright 2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.FileSet;

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

    private static final String SETUP = "setUp";
    private static final String TEST = "test";
    private static final String TEARDOWN = "tearDown";

    private ArrayList filesets = new ArrayList();
    private Project newProject;

    private ArrayList listeners = new ArrayList();

    private int failures=0;
    private int errors=0;
    private boolean failOnError=true;
    public static final String ERROR_TESTS_FAILED = "Tests failed with ";
    public static final String ERROR_NO_FILESET = "You must specify at least one nested"
                            + " fileset.";

    public void add(FileSet fs) {
        filesets.add(fs);
    }

    public void add(AntUnitListener al) {
        listeners.add(al);
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public void execute() {
        if (filesets.size() == 0) {
            throw new BuildException(ERROR_NO_FILESET);
        }
        Iterator iter = filesets.iterator();
        while (iter.hasNext()) {
            doFileSet((FileSet) iter.next());
        }
        if (failOnError && (failures > 0 || errors > 0)) {
            throw new BuildException(ERROR_TESTS_FAILED
                    + failures + " failure" + (failures != 1 ? "s" : "")
                    + " and "
                    + errors + " error" + (errors != 1 ? "s" : ""));
        }
    }

    private void doFileSet(FileSet fs) {
        DirectoryScanner ds = fs.getDirectoryScanner(getProject());
        File fromDir = fs.getDir(getProject());
        String[] files = ds.getIncludedFiles();
        for (int i = 0; i < files.length; i++) {
            doFile(new File(fromDir, files[i]));
        }
    }

    private void doFile(File f) {
        newProject = new Project();
        newProject.setDefaultInputStream(getProject().getDefaultInputStream());
        newProject.setJavaVersionProperty();
        newProject.setInputHandler(getProject().getInputHandler());
        getProject().initSubProject(newProject);
        newProject.setUserProperty("ant.file" , f.getAbsolutePath());
        attachListeners(f, newProject);
        ProjectHelper.configureProject(newProject, f);
        Map targets = newProject.getTargets();
        Target setUp = (Target) targets.get(SETUP);
        Target tearDown = (Target) targets.get(TEARDOWN);
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
        
    public void handleOutput(String outputToHandle) {
        if (newProject != null) {
            newProject.demuxOutput(outputToHandle, false);
        } else {
            super.handleOutput(outputToHandle);
        }
    }

    public int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        if (newProject != null) {
            return newProject.demuxInput(buffer, offset, length);
        }
        return super.handleInput(buffer, offset, length);
    }

    public void handleFlush(String toFlush) {
        if (newProject != null) {
            newProject.demuxFlush(toFlush, false);
        } else {
            super.handleFlush(toFlush);
        }
    }

    public void handleErrorOutput(String errorOutputToHandle) {
        if (newProject != null) {
            newProject.demuxOutput(errorOutputToHandle, true);
        } else {
            super.handleErrorOutput(errorOutputToHandle);
        }
    }

    public void handleErrorFlush(String errorOutputToFlush) {
        if (newProject != null) {
            newProject.demuxFlush(errorOutputToFlush, true);
        } else {
            super.handleErrorFlush(errorOutputToFlush);
        }
    }

    private void attachListeners(File buildFile, Project p) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            AntUnitListener al = (AntUnitListener) it.next();
            p.addBuildListener(new BuildToAntUnitListener(buildFile
                                                          .getAbsolutePath(),
                                                          al));
        }
    }

    private void fireFail(String targetName, AssertionFailedException ae) {
        failures++;
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            AntUnitListener al = (AntUnitListener) it.next();
            al.addFailure(targetName, ae);
        }
    }

    private void fireError(String targetName, Throwable t) {
        errors++;
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            AntUnitListener al = (AntUnitListener) it.next();
            al.addError(targetName, t);
        }
    }

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
