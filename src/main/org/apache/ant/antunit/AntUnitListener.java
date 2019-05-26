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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * A test listener for &lt;antunit&gt;.
 */
public interface AntUnitListener {
    /**
     * Set a reference to the AntUnit task executing the tests, this
     * provides access to the containing project, target or Ant's
     * logging system.
     * @param t the parent task
     */
    void setParentTask(Task t);
    /**
     * Set a reference to the Project instance currently executing the
     * test target.
     *
     * <p>This provides access to the logging system or the properties
     * of the project under test.  Note that different test targets
     * will be executed in different Ant Project instances.</p>
     * @param p the test project
     */
    void setCurrentTestProject(Project p);
    /**
     * Invoked once per build file, before any targets get executed.
     * @param testProject the project
     * @param buildFile the build file
     */
    void startTestSuite(Project testProject, String buildFile);
    /**
     * Invoked once per build file, after all targets have been executed.
     * @param testProject the project
     * @param buildFile the build file
     */
    void endTestSuite(Project testProject, String buildFile);
    /**
     * Invoked before a test target gets executed.
     * @param target name of the target
     */
    void startTest(String target);
    /**
     * Invoked after a test target has been executed.
     * @param target name of the target
     */
    void endTest(String target);
    /**
     * Invoked if an assert tasked caused an error during execution.
     * @param target name of the target
     * @param ae the failure
     */
    void addFailure(String target, AssertionFailedException ae);
    /**
     * Invoked if any error other than a failed assertion occured
     * during execution.
     * @param target name of the target
     * @param ae the error
     */
    void addError(String target, Throwable ae);
}
