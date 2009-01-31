package org.apache.ant.antunit;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/** 
 * Run antunit tests.  The lifecycle of this object is :
 * <ol>
 * <li> activate(file) : Indicates the runner that the given file should be used.</li>
 * <li> scanFile() : Provides you the list of targets.</li>
 * <li> startSuite() : Start the suite</li>
 * <li> runTarget(targetName) : Executed one or more time</li>
 * <li> endSuite() : End the suite</li>
 * <li> deactivate() Indicates to the runner that the test is finished and all 
 *      resources can be freed </li>
 * </ol>
 * Every step is mandatory. 
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
     * Object used to interact with the environment (for example an ant task or a junit runner)
     */
    private final AntUnitExecutionPlatform env;

    /**
     * Ant script file currently under testing.  The file is set at activation, and used
     * during all execution every time we want have to create a new project.</br>
     * It is only defined when the project isActive()
     */
    private File scriptFile;
    
    /**
     * Indicates if the active project is already scanned and if the value the fields
     * hasSuiteSetUp, hasSetUp, hasTearDown, hasSuiteTearDown are defined. 
     */
    private boolean isScanned;

    /**
     * Indicates if the startSuite method has been invoked.  Use to fail fast if the
     * the caller forget to call the startSuite method
     */
    private boolean isSuiteStarted;
    
    /**
     * Does that script have a setUp target (defined when scanning the script)
     */
    private boolean hasSetUp;

    /**
     * Does that script have a tearDown target (defined when scanning the script)
     */
    private boolean hasTearDown;

    /**
     * Does that script has a suiteSetUp target.
     */
    private boolean hasSuiteSetUp;

    /**
     * Does that script has a suite tearDown target that should be executed.
     */
    private boolean hasSuiteTearDown;

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
     * @param env The environment used to create project and where the test progress will be 
     * notified. 
     */
    public AntUnitScriptRunner(AntUnitExecutionPlatform env) {
        if (env == null) {
            throw new AssertionError();
        }
        this.env = env;
    }

    /** 
     * Set the ant script to use.
     * @post isActive() 
     */
    public void activate(File f) {
        scriptFile = f;
        project = null;
        isScanned = false;
        isSuiteStarted = false;
    }

    /** 
     * Declare that the current ant script doesn't need to be used anymore.
     * @post !isActive() 
     */
    public void deactivate() {
        scriptFile = null;
        project = null; 
    }

    /** 
     * Indicates if there is a project currently under test. 
     */
    public boolean isActive() {
        return scriptFile != null;
    }

    /**
     * Get the project currently in use.  The caller is not allowed to invoke a target or
     * do anything that would break the isolation of the test targets.
     * @pre isActif()
     */
    public Project getCurrentProject() {
        if (!isActive()) {
            throw new AssertionError();
        }
        if (project == null) {
            project = env.createProjectForFile(scriptFile);
            projectIsDirty = false;
        }
        return project;
    }

    /**
     * Get a project that has not yet been used in order to execute a target on it.
     * @pre isActive()
     */
    private Project getCleanProject() {
        if (!isActive()) {
            throw new AssertionError();
        }
        if (project == null || projectIsDirty) {
            project = env.createProjectForFile(scriptFile);
        }
        //we already set isDirty to true in order to make sure we didn't reuse
        //this project next time getCleanProject is called.  
        projectIsDirty = true;
        return project;
    }

    /**
     * Provides the list of test targets of the active antunit script.
     * @pre isActive()
     * @return List<String> List of test target names
     */
    public List scanFile() {
        if (!isActive()) {
            throw new AssertionError();
        }
        Project newProject = getCurrentProject();
        Map targets = newProject.getTargets();
        hasSetUp = targets.containsKey(SETUP);
        hasTearDown = targets.containsKey(TEARDOWN);
        hasSuiteSetUp = targets.containsKey(SUITESETUP);
        hasSuiteTearDown = targets.containsKey(SUITETEARDOWN);
        List testTargets = new LinkedList();
        Iterator it = targets.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            if (name.startsWith(TEST) && !name.equals(TEST)) {
                testTargets.add(name);
            }
        }
        isScanned = true;
        return testTargets;
    }

    /**
     * Provides the name of the active script.
     * @pre isAvtive()
     */
    public String getName() {
        if (!isActive()) {
            throw new AssertionError();
        }
        return getCurrentProject().getName();
    }

    /**
     * Executes the suiteSetUp target if presents and report any execution error.
     * Note that if the method return false, you are not allowed to run targets.
     * @return false in case of execution failure.  true in case of success. 
     */
    public boolean startSuite() {
        if (!isScanned) {
            throw new AssertionError();
        }
        getCurrentProject().fireBuildStarted();
        if (hasSuiteSetUp) {
            try {
                Project newProject = getCleanProject();
                newProject.executeTarget(SUITESETUP);
            } catch (BuildException e) {
                env.fireStartTest(SUITESETUP);
                fireFailOrError(SUITESETUP, e);
                return false;
            }
        }
        isSuiteStarted = true; //set to true only if suiteSetUp executed properly.
        return true;
    }

    /** 
     * Run the specific test target, possibly between the setUp and tearDown targets if
     * it exists.  Exception or failures are reported to the execution environment.
     * @param name name of the test target to execute.
     */
    public void runTarget(String name) {
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
            env.fireStartTest(name);
            newProject.executeTargets(v);
        } catch (BuildException e) {
            fireFailOrError(name, e);
        } finally {
            // fire endTest here instead of the endTarget
            // event, otherwise an error would be
            // registered after the endTest event -
            // endTarget is called before this method's catch block
            // is reached.
            env.fireEndTest(name);
            // clean up
            if (hasTearDown) {
                try {
                    newProject.executeTarget(TEARDOWN);
                } catch (final BuildException e) {
                    fireFailOrError(name, e);
                }
            }
        }
    }

    /**
     * Executes the suiteTearDown target if presents and report any execution error.
     * @param caught Any internal exception triggered (and catched) by the caller indicating that 
     * the this runner could not be invoked as expected.  
     */
    public void endSuite(Throwable caught) {
        if (!isScanned) {
            throw new AssertionError();
        }
        if (hasSuiteTearDown) {
            try {
                Project newProject = getCleanProject();
                newProject.executeTarget(SUITETEARDOWN);
            } catch (BuildException e) {
                env.fireStartTest(SUITETEARDOWN);
                fireFailOrError(SUITETEARDOWN, e);
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
    private void fireFailOrError(String targetName, BuildException e) {
        boolean failed = false;
        Throwable t = e;
        while (t != null && t instanceof BuildException) {
            if (t instanceof AssertionFailedException) {
                failed = true;
                env.fireFail(targetName, (AssertionFailedException) t);
                break;
            }
            t = ((BuildException) t).getCause();
        }

        if (!failed) {
            env.fireError(targetName, e);
        }
    }

}