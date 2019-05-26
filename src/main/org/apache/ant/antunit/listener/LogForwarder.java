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

import org.apache.ant.antunit.AntUnitListener;
import org.apache.ant.antunit.AssertionFailedException;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;

/**
 * A test listener for &lt;antunit&gt; who's whole purpose is to
 * forward log output from the project under test to the project
 * executing the &lt;antunit&gt; task.
 */
public class LogForwarder implements AntUnitListener {

    private Task parentTask;

    public void setParentTask(Task t) {
        parentTask = t;
    }

    public void setCurrentTestProject(Project p) {
        p.addBuildListener(new BuildListener() {
                public void buildStarted(BuildEvent event) {}
                public void buildFinished(BuildEvent event) {}
                public void targetStarted(BuildEvent event) {}
                public void targetFinished(BuildEvent event) {}
                public void taskStarted(BuildEvent event) {}
                public void taskFinished(BuildEvent event) {}
                public void messageLogged(BuildEvent event) {
                    parentTask.log(event.getMessage(), event.getPriority());
                }
            });
    }

    public void startTestSuite(Project testProject, String buildFile) {}
    public void endTestSuite(Project testProject, String buildFile) {}
    public void startTest(String target) {}
    public void endTest(String target) {}
    public void addFailure(String target, AssertionFailedException ae) {}
    public void addError(String target, Throwable ae) {}
}
