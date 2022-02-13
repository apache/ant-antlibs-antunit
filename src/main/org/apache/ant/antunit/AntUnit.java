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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;

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
     * The build files to process.
     */
    private Union buildFiles;


    private AntUnitExecutionNotifier notifier = new AntUnitExecutionNotifier() {

        public void fireEndTest(String targetName) {
            AntUnit.this.fireEndTest(targetName);
        }

        public void fireError(String targetName, Throwable t) {
            AntUnit.this.fireError(targetName, t);
        }

        public void fireFail(String targetName, AssertionFailedException ae) {
            AntUnit.this.fireFail(targetName, ae);
        }

        public void fireStartTest(String targetName) {
            AntUnit.this.fireStartTest(targetName);
        }
    };

    /**
     * The object responsible for the execution of the unit test.
     * scriptRunner is invoked to executes the targets and keep the
     * reference to the project.  scriptRunner is defined only when the
     * antunit script is running.
     */
    private AntUnitScriptRunner scriptRunner;

    /**
     * listeners.
     */
    private ArrayList<AntUnitListener> listeners = new ArrayList<AntUnitListener>();

    /**
     * propertysets.
     */
    private ArrayList<PropertySet> propertySets = new ArrayList<PropertySet>();

    /**
     * Holds references to be inherited by the test project
     */
    private ArrayList<ReferenceSet> referenceSets = new ArrayList<ReferenceSet>();

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

    private Path classpath;

    private AntClassLoader subprojectCoreLoader;

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
     * Add a set of inherited references.
     * @return set of inherited references
     */
    public ReferenceSet createReferenceSet() {
        ReferenceSet set = new ReferenceSet();
        set.setProject(getProject());
        referenceSets.add(set);
        return set;
    }

    /**
     * Add an inherited reference.
     * @param reference inherited reference
     */
    public void addReference(Reference reference) {
        //wrap in a singleton reference set.
        createReferenceSet().addReference(reference);
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
     * Create the nested classpath element.
     * @return {@link Path}
     */
    public Path createClasspath() {
        if (classpath != null) {
            throw new BuildException("Can only set classpath once");
        }
        return classpath = new Path(getProject());
    }

    /**
     * Set the test classpath as a reference to a {@link Path} defined elsewhere.
     * @param classpathRefid
     */
    public void setClasspathRefid(String classpathRefid) {
        if (classpathRefid == null || classpathRefid.trim().length() == 0) {
            throw new BuildException("Null/empty/blank @classpathrefid specified");
        }
        createClasspath().setRefid(new org.apache.tools.ant.types.Reference(getProject(), classpathRefid));
    }

    /**
     * Execute the tests.
     */
    public void execute() {
        if (buildFiles == null) {
            throw new BuildException(ERROR_NO_TESTS);
        }
        if (classpath != null && classpath.size() > 0) {
            subprojectCoreLoader = getProject().createClassLoader(classpath);
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
        @SuppressWarnings("unchecked")
        Iterable<Resource> iterable = (Iterable<Resource>) rc;

        for (Resource resource : iterable) {
            FileResource r = (FileResource) resource;
            if (r.isExists()) {
                doFile(r.getFile());
            } else {
                log("Skipping " + r + " since it doesn't exist", Project.MSG_VERBOSE);
            }
        }
    }

    /**
     * Processes a single build file.
     */
    private void doFile(final File f) {
        log("Running tests in build file " + f, Project.MSG_DEBUG);
        ProjectFactory prjFactory = new ProjectFactory() {
            public Project createProject() {
                return createProjectForFile(f);
            }
        };
        try {
            scriptRunner = new AntUnitScriptRunner(prjFactory);
            List<String> testTargets = scriptRunner.getTestTargets();
            scriptRunner.runSuite(testTargets, notifier);
        } finally {
            scriptRunner=null;
        }
    }

    /**
     * Redirect output to new project instance.
     * @param outputToHandle the output to handle.
     */
    public void handleOutput(String outputToHandle) {
        if (scriptRunner != null) {
            scriptRunner.getCurrentProject().demuxOutput(outputToHandle, false);
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
        if (scriptRunner != null) {
            return scriptRunner.getCurrentProject().demuxInput(buffer, offset, length);
        }
        return super.handleInput(buffer, offset, length);
    }

    /**
     * Redirect flush to new project instance.
     * @param toFlush the output String to flush.
     */
    public void handleFlush(String toFlush) {
        if (scriptRunner != null) {
            scriptRunner.getCurrentProject().demuxFlush(toFlush, false);
        } else {
            super.handleFlush(toFlush);
        }
    }

    /**
     * Redirect error output to new project instance.
     * @param errorOutputToHandle the error output to handle.
     */
    public void handleErrorOutput(String errorOutputToHandle) {
        if (scriptRunner != null) {
            scriptRunner.getCurrentProject().demuxOutput(errorOutputToHandle, true);
        } else {
            super.handleErrorOutput(errorOutputToHandle);
        }
    }

    /**
     * Redirect error flush to new project instance.
     * @param errorOutputToFlush the error output to flush.
     */
    public void handleErrorFlush(String errorOutputToFlush) {
        if (scriptRunner != null) {
            scriptRunner.getCurrentProject().demuxFlush(errorOutputToFlush, true);
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
        if (subprojectCoreLoader != null) {
            p.setCoreLoader(subprojectCoreLoader);
        }
        p.setDefaultInputStream(getProject().getDefaultInputStream());
        p.initProperties();
        p.setInputHandler(getProject().getInputHandler());
        getProject().initSubProject(p);
        //pass through inherited properties
        for (PropertySet set : propertySets) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Map<String,Object> props = (Map) set.getProperties();
            for (String key : props.keySet()) {
                if (MagicNames.PROJECT_BASEDIR.equals(key) || MagicNames.ANT_FILE.equals(key)) {
                    continue;
                }
                Object value = props.get(key);
                if (value instanceof String && p.getProperty(key) == null) {
                    p.setNewProperty(key, (String) value);
                }
            }
        }

        //pass through inherited references.  this code is borrowed
        //with significant modification from taskdefs.Ant in Ant core.
        //unfortunately the only way we can share the code directly
        //would be to extend Ant (which might not be a bad idea?)
        for (ReferenceSet set : referenceSets) {
            set.copyReferencesInto(p);
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
        for (AntUnitListener al : listeners) {
            p.addBuildListener(new BuildToAntUnitListener(buildFile.getAbsolutePath(), al));
            al.setCurrentTestProject(p);
        }
    }

    /**
     * invokes start on all registered test listeners.
     * @param targetName the name of the target.
     */
    private void fireStartTest(String targetName) {
        for (AntUnitListener al : listeners) {
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
        for (AntUnitListener al : listeners) {
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
        for (AntUnitListener al : listeners) {
            al.addError(targetName, t);
        }
    }

    /**
     * invokes endTest on all registered test listeners.
     * @param targetName the name of the current target.
     */
    private void fireEndTest(String targetName) {
        for (AntUnitListener al : listeners) {
            al.endTest(targetName);
        }
    }

    /**
     * Defines a collection of inherited {@link Reference references},
     * with an optional nested {@link Mapper} that maps them to new
     * reference IDs in the target project.
     */
    public static class ReferenceSet extends ProjectComponent {
        /**
         * references inherited from parent project by antunit scripts
         */
        private List<Reference> references = new ArrayList<Reference>();
        /**
         * maps source reference ID to target reference ID
         */
        private Mapper mapper;

        public void addReference(Reference reference) {
            references.add(reference);
        }

        /**
         * Create a nested mapper element.
         * @return {@link Mapper}
         */
        public Mapper createMapper() {
            if (mapper == null) {
                return mapper = new Mapper(getProject());
            }
            throw new BuildException("Only one mapper element is allowed per referenceSet",
                getLocation());
        }

        /**
         * Configure a single mapper to translate reference IDs.
         * @param typeName the mapper type
         * @param from the from attribute
         * @param to the to attribute
         */
        public void setMapper(String typeName, String from, String to) {
            Mapper mapper = createMapper();
            Mapper.MapperType type = new Mapper.MapperType();
            type.setValue(typeName);

            mapper.setType(type);
            mapper.setFrom(from);
            mapper.setTo(to);
        }

        /**
         * Copy all identified references into the target project,
         * applying any name mapping required by a nested mapper
         * element.
         * @param newProject the target project to copy references into
         */
        public void copyReferencesInto(Project newProject) {
            FileNameMapper mapper = this.mapper == null ? null : this.mapper.getImplementation();
            Set<String> matches = new HashSet<String>();
            @SuppressWarnings("unchecked")
            Hashtable<String,Object> src = getProject().getReferences();

            for (Reference ref : references) {
                matches.clear();
                ref.addMatchingReferences(src, matches);

                for (String refid : matches) {
                    String toRefid = ref.getToRefid();

                    //transform the refid with the mapper if necessary
                    if (mapper != null && toRefid == null) {
                        String[] mapped = mapper.mapFileName(refid);
                        if (mapped != null) {
                            toRefid = mapped[0];
                        }
                    }
                    if (toRefid == null) {
                        toRefid = refid;
                    }

                    //clone the reference into the new project
                    copyReference(refid, newProject, toRefid);
                }
            }
        }

        /**
         * Try to clone and reconfigure the object referenced by
         * oldkey in the parent project and add it to the new project
         * with the key newkey.  This protects the parent project from
         * modification by the child project.
         *
         * <p>If we cannot clone it, copy the referenced object itself and
         * keep our fingers crossed.</p>
         * @param oldKey the reference id in the current project.
         * @param newKey the reference id in the new project.
         */
        private void copyReference(String oldKey, Project newProject,
                                   String newKey) {
            Object orig = getProject().getReference(oldKey);
            if (orig == null) {
                log("No object referenced by " + oldKey + ". Can't copy to "
                    + newKey,
                    Project.MSG_WARN);
                return;
            }

            Class<?> c = orig.getClass();
            Object copy = orig;
            try {
                Method cloneM = c.getMethod("clone");
                if (cloneM != null) {
                    copy = cloneM.invoke(orig);
                    log("Adding clone of reference " + oldKey,
                        Project.MSG_DEBUG);
                }
            } catch (Exception e) {
                // not Clonable
            }

            if (copy instanceof ProjectComponent) {
                ((ProjectComponent) copy).setProject(newProject);
            } else {
                try {
                    Method setProjectM =
                        c.getMethod("setProject", Project.class);
                    if (setProjectM != null) {
                        setProjectM.invoke(copy, newProject);
                    }
                } catch (NoSuchMethodException e) {
                    // ignore this if the class being referenced does not have
                    // a set project method.
                } catch (Exception e2) {
                    String msg = "Error setting new project instance for "
                        + "reference with id " + oldKey;
                    throw new BuildException(msg, e2, getLocation());
                }
            }
            newProject.addReference(newKey, copy);
        }
    }

    public static class Reference extends Ant.Reference {

        private String regex;
        private RegexpMatcher matcher;

        public String getRegex() {
            return regex;
        }

        /**
         * Set a regular expression to match references.
         * @param regex the regular expression
         */
        public void setRegex(String regex) {
            this.regex = regex;
            RegexpMatcherFactory matchMaker = new RegexpMatcherFactory();
            matcher = matchMaker.newRegexpMatcher();
            matcher.setPattern(regex);
        }

        /**
         * Add to <code>dest</code> any reference IDs in
         * <code>src</code> matching this reference descriptor
         * @param src table of references to check
         * @param dest set of reference IDs matching this reference pattern
         */
        public void addMatchingReferences(Hashtable<String, Object> src, Collection<String> dest) {
            String id = getRefId();
            if (id != null) {
                if (src.containsKey(id)) {
                    dest.add(id);
                }
            } else if (matcher != null) {
                for (String refid : src.keySet()) {
                    if (matcher.matches(refid)) {
                        dest.add(refid);
                    }
                }
            } else {
                throw new BuildException(
                    "either the refid or regex attribute is required for reference elements");
            }
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
        public void targetStarted(BuildEvent event) {}
        public void targetFinished(BuildEvent event) {}
        public void taskStarted(BuildEvent event) {}
        public void taskFinished(BuildEvent event) {}
        public void messageLogged(BuildEvent event) {}
    }

}
