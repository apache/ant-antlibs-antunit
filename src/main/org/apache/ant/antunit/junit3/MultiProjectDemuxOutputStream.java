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

package org.apache.ant.antunit.junit3;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.ant.antunit.AntUnitScriptRunner;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Project;


/**
 * Forward stdout or stderr operation to the current antunit project.
 */
class MultiProjectDemuxOutputStream extends OutputStream {

    private final AntUnitScriptRunner scriptRunner;
    
    private Project lastProject;
    private DemuxOutputStream lastDemuxOutputStream = null;

    private final boolean isErrorStream; 

    public MultiProjectDemuxOutputStream(AntUnitScriptRunner scriptRunner, boolean isErrorStream) {
        this.scriptRunner = scriptRunner;
        this.isErrorStream = isErrorStream;        
    }
    

    private DemuxOutputStream getDemuxOutputStream() {
        if (lastProject != scriptRunner.getCurrentProject()) {
            lastProject = scriptRunner.getCurrentProject();
            lastDemuxOutputStream = new DemuxOutputStream(lastProject,isErrorStream);
        }
        return lastDemuxOutputStream;
    }
    
    public void write(int b) throws IOException {
        getDemuxOutputStream().write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        getDemuxOutputStream().write(b, off, len);
    }
    
    public void close() throws IOException {
        getDemuxOutputStream().close();
    }
    
    public void flush() throws IOException {
        getDemuxOutputStream().flush();
    }
}
