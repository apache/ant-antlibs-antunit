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

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

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

    private List/*<BuildEvent>*/ events = Collections.synchronizedList(new LinkedList());
    private Project p;

    public LogCapturer(Project p) {
        this.p = p;
        p.addBuildListener(this);
        p.addReference(REFERENCE_ID, this);
    }

    /**
     * All messages with <code>logLevel == Project.MSG_ERR</code>
     * merging messages into a single line.
     * @return All messages with <code>logLevel == Project.MSG_ERR</code>
     */
    public String getErrLog() {
        return getErrLog(true);
    }
    /**
     * All messages with <code>logLevel == Project.MSG_WARN</code> or
     * more severe merging messages into a single line.
     * @return All messages with <code>logLevel == Project.MSG_WARN</code> or above
     */
    public String getWarnLog() {
        return getWarnLog(true);
    }
    /**
     * All messages with <code>logLevel == Project.MSG_INFO</code> or
     * more severe merging messages into a single line.
     * @return All messages with <code>logLevel == Project.MSG_INFO</code> or above
     */
    public String getInfoLog() {
        return getInfoLog(true);
    }
    /**
     * All messages with <code>logLevel == Project.MSG_VERBOSE</code> or
     * more severe merging messages into a single line.
     * @return All messages with <code>logLevel == Project.MSG_VERBOSE</code> or above
     */
    public String getVerboseLog() {
        return getVerboseLog(true);
    }
    /**
     * All messages with <code>logLevel == Project.MSG_DEBUG</code> or
     * more severe merging messages into a single line.
     * @return All messages with <code>logLevel == Project.MSG_DEBUG</code> or above
     */
    public String getDebugLog() {
        return getDebugLog(true);
    }

    /**
     * All messages with <code>logLevel == Project.MSG_ERR</code>.
     * @param mergeLines whether to merge messages into a single line
     * or split them into multiple lines
     * @return All messages with <code>logLevel == Project.MSG_ERR</code>
     */
    public String getErrLog(boolean mergeLines) {
        return getLog(Project.MSG_ERR, mergeLines);
    }
    /**
     * All messages with <code>logLevel == Project.MSG_WARN</code> or
     * more severe.
     * @param mergeLines whether to merge messages into a single line
     * or split them into multiple lines
     * @return All messages with <code>logLevel == Project.MSG_WARN</code> or above
     * @since AntUnit 1.3
     */
    public String getWarnLog(boolean mergeLines) {
        return getLog(Project.MSG_WARN, mergeLines);
    }
    /**
     * All messages with <code>logLevel == Project.MSG_INFO</code> or
     * more severe.
     * @param mergeLines whether to merge messages into a single line
     * or split them into multiple lines
     * @return All messages with <code>logLevel == Project.MSG_INFO</code> or above
     * @since AntUnit 1.3
     */
    public String getInfoLog(boolean mergeLines) {
        return getLog(Project.MSG_INFO, mergeLines);
    }
    /**
     * All messages with <code>logLevel == Project.MSG_VERBOSE</code> or
     * more severe.
     * @param mergeLines whether to merge messages into a single line
     * or split them into multiple lines
     * @return All messages with <code>logLevel == Project.MSG_VERBOSE</code> or above
     * @since AntUnit 1.3
     */
    public String getVerboseLog(boolean mergeLines) {
        return getLog(Project.MSG_VERBOSE, mergeLines);
    }
    /**
     * All messages with <code>logLevel == Project.MSG_DEBUG</code> or
     * more severe.
     * @param mergeLines whether to merge messages into a single line
     * or split them into multiple lines
     * @return All messages with <code>logLevel == Project.MSG_DEBUG</code> or above
     * @since AntUnit 1.3
     */
    public String getDebugLog(boolean mergeLines) {
        return getLog(Project.MSG_DEBUG, mergeLines);
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
        events.add(event);
    }

    private String getLog(int minPriority, boolean mergeLines) {
        StringBuffer sb = new StringBuffer();
        for (Iterator/*<BuildEvent>*/ it = new LinkedList(events).iterator();
             it.hasNext(); ) {
            append(sb, (BuildEvent) it.next(), minPriority, mergeLines);
        }
        return sb.toString();
    }

    private static void append(StringBuffer sb, BuildEvent event,
                               int minPriority, boolean mergeLines) {
        if (event.getPriority() <= minPriority) {
            sb.append(event.getMessage());
            if (!mergeLines) {
                sb.append(StringUtils.LINE_SEP);
            }
        }
    }
}
