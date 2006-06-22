/*
 * Copyright 2005-2006 The Apache Software Foundation
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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;

/**
 * Captures log messages generated during an antunit task run and
 * makes it available to tasks via a project reference.
 *
 * <p>This class captures all messaged generated during the build and
 * adds itself as project reference to the project using the id
 * <code>ant.antunit.log</code>.</p>
 */
public class LogCapturer implements BuildListener {
    public static final String REFERENCE_ID = "ant.antunit.log";

    private StringBuffer err = new StringBuffer();
    private StringBuffer warn = new StringBuffer();
    private StringBuffer info = new StringBuffer();
    private StringBuffer verbose = new StringBuffer();
    private StringBuffer debug = new StringBuffer();
    private Project p;

    public LogCapturer(Project p) {
        this.p = p;
        p.addBuildListener(this);
        p.addReference(REFERENCE_ID, this);
    }

    /**
     * All messages with <code>logLevel == Project.MSG_ERR</code>.
     */
    public String getErrLog() {
        return err.toString();
    }
    /**
     * All messages with <code>logLevel == Project.MSG_WARN</code> or
     * more severe.
     */
    public String getWarnLog() {
        return warn.toString();
    }
    /**
     * All messages with <code>logLevel == Project.MSG_INFO</code> or
     * more severe.
     */
    public String getInfoLog() {
        return info.toString();
    }
    /**
     * All messages with <code>logLevel == Project.MSG_VERBOSE</code> or
     * more severe.
     */
    public String getVerboseLog() {
        return verbose.toString();
    }
    /**
     * All messages with <code>logLevel == Project.MSG_DEBUG</code> or
     * more severe.
     */
    public String getDebugLog() {
        return debug.toString();
    }

    /**
     * Empty.
     */
    public void buildStarted(BuildEvent event) {}
    /**
     * Empty.
     */
    public void targetStarted(BuildEvent event) {}
    /**
     * Empty.
     */
    public void targetFinished(BuildEvent event) {}
    /**
     * Empty.
     */
    public void taskStarted(BuildEvent event) {}
    /**
     * Empty.
     */
    public void taskFinished(BuildEvent event) {}

    /**
     * De-register.
     */
    public void buildFinished(BuildEvent event) {
        if (p != null && event.getProject() == p) {
            p.removeBuildListener(this);
            p.getReferences().remove(REFERENCE_ID);
            p = null;
        }
    }
    /**
     * Record the message.
     */
    public void messageLogged(BuildEvent event) {
        if (event.getPriority() <= Project.MSG_ERR) {
            err.append(event.getMessage());
        }
        if (event.getPriority() <= Project.MSG_WARN) {
            warn.append(event.getMessage());
        }
        if (event.getPriority() <= Project.MSG_INFO) {
            info.append(event.getMessage());
        }
        if (event.getPriority() <= Project.MSG_VERBOSE) {
            verbose.append(event.getMessage());
        }
        if (event.getPriority() <= Project.MSG_DEBUG) {
            debug.append(event.getMessage());
        }
    }
}