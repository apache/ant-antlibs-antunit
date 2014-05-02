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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.StringUtils;

/**
 * Captures log messages generated during an antunit task run and
 * makes them available to tasks via a project reference.
 *
 * <p>This class captures all messages generated during the build and
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
            append(err, event);
        }
        if (event.getPriority() <= Project.MSG_WARN) {
            append(warn, event);
        }
        if (event.getPriority() <= Project.MSG_INFO) {
            append(info, event);
        }
        if (event.getPriority() <= Project.MSG_VERBOSE) {
            append(verbose, event);
        }
        if (event.getPriority() <= Project.MSG_DEBUG) {
            append(debug, event);
        }
    }

    private static void append(StringBuffer sb, BuildEvent event) {
        sb.append(event.getMessage()).append(StringUtils.LINE_SEP);
    }
}
