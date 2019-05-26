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

import org.apache.tools.ant.BuildFileTest;

/**
 * Tests the plain listener.
 */
public class XMLListenerTest extends BuildFileTest {
    protected void setUp() throws Exception {
        configureProject("src/etc/testcases/listener/xmllistener.xml");
    }

    public void testStdoutPlacement() {
        executeTarget("stdoutplacement");
    }
    
    public void testShowDefault() {
        executeTarget("showdefault");
    }
    
    public void testShowError() {
        executeTarget("showerror");
    }
    
    public void testShowWarning() {
        executeTarget("showwarning");
    }
    
    public void testShowInfo() {
        executeTarget("showinfo");
    }
    
    public void testShowVerbose() {
        executeTarget("showverbose");
    }
    
    public void testShowDebug() {
        executeTarget("showdebug");
    }

    public void testShowNone() {
        executeTarget("shownone");
    }

    public void testSetUpTearDown() {
        executeTarget("setupteardown");
    }
    
    public void testSuiteSetUpTearDown() {
        executeTarget("suitesetupteardown");
    }
    
    public void testBadCharacters() {
        executeTarget("badcharacters");
    }
    
    public void testProperties() {
        executeTarget("properties");
    }
}