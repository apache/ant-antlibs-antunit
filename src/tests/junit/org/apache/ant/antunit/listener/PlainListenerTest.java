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
public class PlainListenerTest extends BuildFileTest {
    protected void setUp() throws Exception {
        configureProject("src/etc/testcases/listener/plainlistener.xml");
    }

    public void testStdoutPlacement() {
        executeTarget("showinfo");
        String log = getLog();
        int indexElapsed = log.indexOf("Time elapsed");
        int indexTarget = log.indexOf("Target");
        int index = log.indexOf("------------- Log Output       ---------------");
        assertTrue("Standard output message not present", index > -1);
        assertTrue("Standard output message not located after summary.", index > indexElapsed);
        assertTrue("Standard output message not located before test details.", index < indexTarget);
        int indexTest1 = log.indexOf("infomessage", index);
        int indexTest2 = log.indexOf("test2", index);
        assertTrue("infomessage", indexTest1 > -1);
        assertTrue("test2", indexTest2 > -1);
        index = log.indexOf("------------- ---------------- ---------------", Math.max(indexTest1, indexTest2));
        assertTrue("End of standard output message not present.", index > -1);
        assertTrue("End of standard output message not located before test details.", index < indexTarget);
    }
    
    public void testShowDefault() {
        executeTarget("showdefault");
        String log = getLog();
        assertTrue("Should not have shown error message", -1 == log.indexOf("errormessage"));
        assertTrue("Should not have shown warning message", -1 == log.indexOf("warningmessage"));
        assertTrue("Should not have shown info message", -1 == log.indexOf("infomessage"));
        assertTrue("Should not have shown verbose message", -1 == log.indexOf("verbosemessage"));
        assertTrue("Should not have shown debug message", -1 == log.indexOf("debugmessage"));
    }
    
    public void testShowError() {
        executeTarget("showerror");
        String log = getLog();
        assertTrue("Should have shown error message", -1 != log.indexOf("errormessage"));
        assertTrue("Should not have shown warning message", -1 == log.indexOf("warningmessage"));
        assertTrue("Should not have shown info message", -1 == log.indexOf("infomessage"));
        assertTrue("Should not have shown verbose message", -1 == log.indexOf("verbosemessage"));
        assertTrue("Should not have shown debug message", -1 == log.indexOf("debugmessage"));
    }
    
    public void testShowWarning() {
        executeTarget("showwarning");
        String log = getLog();
        assertTrue("Should have shown error message", -1 != log.indexOf("errormessage"));
        assertTrue("Should have shown warning message", -1 != log.indexOf("warningmessage"));
        assertTrue("Should not have shown info message", -1 == log.indexOf("infomessage"));
        assertTrue("Should not have shown verbose message", -1 == log.indexOf("verbosemessage"));
        assertTrue("Should not have shown debug message", -1 == log.indexOf("debugmessage"));
    }
    
    public void testShowInfo() {
        executeTarget("showinfo");
        String log = getLog();
        assertTrue("Should have shown error message", -1 != log.indexOf("errormessage"));
        assertTrue("Should have shown warning message", -1 != log.indexOf("warningmessage"));
        assertTrue("Should have shown info message", -1 != log.indexOf("infomessage"));
        assertTrue("Should not have shown verbose message", -1 == log.indexOf("verbosemessage"));
        assertTrue("Should not have shown debug message", -1 == log.indexOf("debugmessage"));
    }
    
    public void testShowVerbose() {
        executeTarget("showverbose");
        String log = getLog();
        assertTrue("Should have shown error message", -1 != log.indexOf("errormessage"));
        assertTrue("Should have shown warning message", -1 != log.indexOf("warningmessage"));
        assertTrue("Should have shown info message", -1 != log.indexOf("infomessage"));
        assertTrue("Should have shown verbose message", -1 != log.indexOf("verbosemessage"));
        assertTrue("Should not have shown debug message", -1 == log.indexOf("debugmessage"));
    }
    
    public void testShowDebug() {
        executeTarget("showdebug");
        String log = getLog();
        assertTrue("Should have shown error message", -1 != log.indexOf("errormessage"));
        assertTrue("Should have shown warning message", -1 != log.indexOf("warningmessage"));
        assertTrue("Should have shown info message", -1 != log.indexOf("infomessage"));
        assertTrue("Should have shown verbose message", -1 != log.indexOf("verbosemessage"));
        assertTrue("Should have shown debug message", -1 != log.indexOf("debugmessage"));
    }

    public void testShowNone() {
        executeTarget("shownone");
        String log = getLog();
        assertTrue("Should not have shown error message", -1 == log.indexOf("errormessage"));
        assertTrue("Should not have shown warning message", -1 == log.indexOf("warningmessage"));
        assertTrue("Should not have shown info message", -1 == log.indexOf("infomessage"));
        assertTrue("Should not have shown verbose message", -1 == log.indexOf("verbosemessage"));
        assertTrue("Should not have shown debug message", -1 == log.indexOf("debugmessage"));
    }

    public void testSetUpTearDown() {
        executeTarget("showinfo");
        String log = getLog();
        int index = log.indexOf("setUp");
        assertTrue("First setUp not present", index > -1);
        index = log.indexOf("setUp", index);
        assertTrue("Second setUp not present", index > -1);
        index = log.indexOf("tearDown");
        assertTrue("First tearDown not present", index > -1);
        index = log.indexOf("tearDown", index);
        assertTrue("Second tearDown not present", index > -1);
    }
    
    public void testSuiteSetUpTearDown() {
        executeTarget("showinfo");
        String log = getLog();
        int index = log.indexOf("suiteSetUp");
        assertTrue("suiteSetUp not present", index > -1);
        index = log.indexOf("suiteSetUp", index + 1);
        assertTrue("suiteSetUp present more than once", index == -1);
        index = log.indexOf("suiteTearDown");
        assertTrue("suiteTearDown not present", index > -1);
        index = log.indexOf("suiteTearDown", index + 1);
        assertTrue("suiteTearDown present more than once", index == -1);
    }
}