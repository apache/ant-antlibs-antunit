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

package org.apache.ant.antunit.listener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.ant.antunit.AssertionFailedException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

/**
 * This AntUnitListener creates a new buildfile with a target for each
 * failed test target in the AntUnit run. The generated target calls
 * the failed target (with setUp and tearDown if present in the called
 * project). 
 * This is intended for rerunning just failed tests.
 */
public class FailureAntUnitListener extends BaseAntUnitListener {
 
    /** LineSeparator just for beautifying the output. */
    private static final String BR = System.getProperty("line.separator"); 

    /** A sorted list (without duplicates) of failed tests. */
    private static SortedSet failedTests = new TreeSet();
    
    /** Where to write the generated buildfile. */
    private static File failureBuildfile;
    
    /** The current running test project. Needed for addError()/addFailure(). */
    private Project currentTestProject;

    /** The current running build file. Needed for addError()/addFailure(). */
    private String currentBuildFile;
    
    
    /** No-arg constructor. */
    public FailureAntUnitListener() {
        super(new BaseAntUnitListener.SendLogTo(SendLogTo.ANT_LOG), "txt");
    }

    public void setFile(File file) {
        failureBuildfile = file;
    }

    public void startTestSuite(Project testProject, String buildFile) {
        super.startTestSuite(testProject, buildFile);
        currentTestProject = testProject;
        currentBuildFile = buildFile;
    }
    
    public void addError(String target, Throwable ae) {
        super.addError(target, ae);
        failedTests.add(new TestInfos(currentTestProject, currentBuildFile, target, ae.getMessage()));
    }
    
    public void addFailure(String target, AssertionFailedException ae) {
        super.addFailure(target, ae);
        failedTests.add(new TestInfos(currentTestProject, currentBuildFile, target, ae.getMessage()));
    } 
    
    /** not in use */
    public void endTest(String target) { 
    }

    public void endTestSuite(Project testProject, String buildFile) {
        StringBuffer sb = new StringBuffer();
        // <project> and antunit-target for direct run
        sb.append("<project default=\"antunit\" xmlns:au=\"antlib:org.apache.ant.antunit\">");
        sb.append(BR);
        sb.append(BR);
        sb.append("  <target name=\"antunit\">").append(BR);
        sb.append("    <au:antunit>").append(BR);
        sb.append("      <au:plainlistener/>").append(BR);
        sb.append("      <file file=\"${ant.file}\"/>").append(BR);
        sb.append("    </au:antunit>").append(BR);
        sb.append("  </target>").append(BR);
        sb.append(BR);
        sb.append(BR);
        
        // one target for each failed test
        int testNumber = 0;
        NumberFormat f = NumberFormat.getIntegerInstance();
        for (Iterator it = failedTests.iterator(); it.hasNext();) {
            sb.append("  <target name=\"test");
            sb.append(f.format(testNumber++));
            sb.append("\">").append(BR);
            TestInfos testInfos = (TestInfos) it.next();
            sb.append(testInfos);
            sb.append("  </target>").append(BR);
            sb.append(BR);
        }
        
        // close the <project>
        sb.append("</project>").append(BR);
        
        // write the whole file
        try {
            FileOutputStream fos = new FileOutputStream(failureBuildfile);
            fos.write(sb.toString().getBytes());
            FileUtils.close(fos);
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }
    
    
    /**
     * Class for collecting needed information about failed tests.
     */
    public class TestInfos implements Comparable {
        /** Does the project has a setUp target? */
        boolean projectHasSetup = false;
        
        /** Does the project has a tearDown target? */
        boolean projectHasTearDown = false;
        
        /** The called target. */
        String target;
        
        /** The buildfile of the project. */
        String buildfile;
        
        /** The error message which was shown. */
        String errorMessage;
        
        public TestInfos(Project project, String buildfile, String target, String errorMessage) {
            projectHasSetup = project.getTargets().containsKey("setUp");
            projectHasTearDown = project.getTargets().containsKey("tearDown");
            this.buildfile = buildfile;
            this.target = target; 
            this.errorMessage = errorMessage;
        }
        
        /** 
         * Creates an &lt;ant&gt; call according to the stored information. 
         * @see java.lang.Object#toString()
         */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            // make the reader of the buildfile happy
            sb.append("    <!-- ");
            sb.append(errorMessage);
            sb.append(" -->").append(BR);
            // <ant antfile="" inheritAll="false">
            sb.append("    <ant ");
            sb.append("antfile=\"");
            sb.append(buildfile.replace('\\', '/'));
            sb.append("\" ");
            sb.append("inheritAll=\"false\">");
            sb.append(BR);
            // <target name=""/>
            if (projectHasSetup) {
                sb.append("      <target name=\"setUp\"/>").append(BR);
            }
            sb.append("      <target name=\"");
            sb.append(target);
            sb.append("\"/>");
            sb.append(BR);
            if (projectHasTearDown) {
                sb.append("      <target name=\"tearDown\"/>").append(BR);
            }
            // </ant>
            sb.append("    </ant>").append(BR);
            return sb.toString();
        }
        
        // Needed, so that a SortedSet could sort this class into the list.
        public int compareTo(Object other) {
            if (!(other instanceof TestInfos)) {
                return -1;
            } else {
                TestInfos that = (TestInfos)other;
                return this.toString().compareTo(that.toString());   
            }
        }
    }

}
