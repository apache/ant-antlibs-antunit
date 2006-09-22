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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.LogLevel;
import org.apache.tools.ant.types.Resource;

/**
 * Exposes AntUnit log content as a (read-only) Resource.
 */
public class LogContent extends Resource {

    private LogLevel level;

    /**
     * Create a new LogContent resource.
     */
    public LogContent() {
        setLevel(LogLevel.INFO);
    }

    /**
     * Create a new LogContent resource, specifying Project and log level.
     * This constructor is provided primarily for convenience during
     * programmatic usage.
     * @param level the LogLevel.
     */
    public LogContent(Project p, LogLevel level) {
        setProject(p);
        setLevel(level);
    }

    /**
     * Set the desired log level.
     * @param level a LogLevel enumerated attribute.
     */
    public void setLevel(LogLevel level) {
        this.level = level;
        setName(level.getValue());
    }

    //inherit doc
    public InputStream getInputStream() throws IOException {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getInputStream();
        }
        LogCapturer lc = getLogCapturer();
        if (lc != null) {
            return getLogStream(lc);
        }
        throw new IllegalStateException("antunit log unavailable");
    }

    //inherit doc
    public boolean isExists() {
        return getLogCapturer() != null;
    }

    //inherit doc
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LogContent)) {
            return false;
        }
        LogContent olc = (LogContent) o;
        return olc.getProject() == getProject()
                && olc.level.getLevel() == level.getLevel();
    }

    private LogCapturer getLogCapturer() {
        Object o = getProject().getReference(LogCapturer.REFERENCE_ID);
        return o instanceof LogCapturer ? (LogCapturer) o : null;
    }

    private InputStream getLogStream(LogCapturer lc) {
        String log = null;
        switch (level.getLevel()) {
        case Project.MSG_ERR:
            log = lc.getErrLog();
            break;
        case Project.MSG_WARN:
            log = lc.getWarnLog();
            break;
        case Project.MSG_INFO:
            log = lc.getInfoLog();
            break;
        case Project.MSG_VERBOSE:
            log = lc.getVerboseLog();
            break;
        case Project.MSG_DEBUG:
            log = lc.getDebugLog();
            break;
        default:
            throw new IllegalStateException("how possible?");
        }
        return new ByteArrayInputStream(log.getBytes());
    }

}
