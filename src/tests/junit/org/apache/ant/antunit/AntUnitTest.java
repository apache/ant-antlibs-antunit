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

import java.io.PrintStream;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.DemuxOutputStream;

public class AntUnitTest extends BuildFileTest {

    public AntUnitTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        configureProject("src/etc/testcases/antunit.xml");
    }

    public void testBase() {
        expectBuildExceptionContaining("antunit-basetest",
            "expected basetest to fail",
            AntUnit.ERROR_TESTS_FAILED);
        String log = getLog();
        int index = log.indexOf("Build File: ");
        assertTrue("start recorded", index > -1);
        index = log.indexOf("/src/etc/testcases/antunit/base.xml"
                            .replace('/', java.io.File.separatorChar),
                            index);
        assertTrue("file name", index > -1);
        index = log.indexOf("Tests run: 5, Failures: 1, Errors: 1, Time "
                            + "elapsed: ", index);
        assertTrue("summary", index > -1);
        assertTrue("test1", log.indexOf("test1", index) > -1);
        assertTrue("test2", log.indexOf("test2", index) > -1);
        assertTrue("test3", log.indexOf("test3", index) == -1);
        assertTrue("test4", log.indexOf("test4", index) > -1);
        assertTrue("test5", log.indexOf("test5", index) > -1);
        assertTrue("testLogCaptureActive",
                   log.indexOf("testLogCaptureActive", index) > -1);
        int index2 = log.indexOf("caused an ERROR", index);
        assertTrue("test5 error", index2 > -1
                   && log.indexOf("test5 exits with error", index2) > -1);
        assertTrue("Only one error", log.indexOf("ERROR", index2 + 11) == -1);
        index2 = log.indexOf("FAILED", index);
        assertTrue("test4 failure", index2 > -1
                   && log.indexOf("test4 fails", index2) > -1);
        assertTrue("Only one failure", log.indexOf("FAILED", index2 + 1) == -1);
    }

    public void testNoTests() {
        expectSpecificBuildException("noTests", "No tests have been specified",
                                     AntUnit.ERROR_NO_TESTS);
    }

    public void testEmptyTests() {
        executeTarget("emptyTests");
    }

    public void testNonFile() {
        expectSpecificBuildException("nonFile",
                                     "URL has been specified",
                                     AntUnit.ERROR_NON_FILES);
    }

    public void testNonExistingFile() {
        executeTarget("nonExistingFile");
        assertDebuglogContaining(java.io.File.separator
                                 + "I don't exist.xml since it doesn't exist");
    }

    public void testNewProject() {
        executeTarget("testNewProject");
    }

    public void testSystemIoHandling() {
        PrintStream savedErr = System.err;
        PrintStream savedOut = System.out;
        try {
            savedErr.flush();
            savedOut.flush();
            System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
            System.setErr(new PrintStream(new DemuxOutputStream(project, true)));
            
            project.executeTarget("testSystemIoHandling");
            
        } finally {
            System.setOut(savedOut);
            System.setErr(savedErr);
        }
    }

    public void testReference() {
        executeTarget("testReference");
    }
    
    public void testReferenceSet() {
        executeTarget("testReferenceSet");
    }

    public void testReferenceRegex() {
        executeTarget("testReferenceRegex");
    }

    public void testReferenceMapper() {
        executeTarget("testReferenceMapper");
    }

    public static class HelloWorld {
        public static void main(String[] args) {
            System.out.println("HelloWorld");
        }
    }
}
