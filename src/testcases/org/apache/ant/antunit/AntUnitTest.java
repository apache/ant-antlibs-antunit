/*
 * Copyright  2005 The Apache Software Foundation
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;

public class AntUnitTest extends BuildFileTest {

    public AntUnitTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/antunit.xml");
    }

    public void testBase() {
        executeTarget("antunit-basetest");
        String log = getLog();
        int index = log.indexOf("Build File: ");
        assertTrue("start recorded", index > -1);
        index = log.indexOf("sandbox/antlibs/antunit/trunk/src/etc/testcases/"
                            + "antunit/base.xml", index);
        assertTrue("file name", index > -1);
        index = log.indexOf("Tests run: 4, Failures: 1, Errors: 1, Time "
                            + "elapsed: ", index);
        assertTrue("summary", index > -1);
        assertTrue("test1", log.indexOf("test1", index) > -1);
        assertTrue("test2", log.indexOf("test2", index) > -1);
        assertTrue("test3", log.indexOf("test3", index) == -1);
        assertTrue("test4", log.indexOf("test4", index) > -1);
        assertTrue("test5", log.indexOf("test5", index) > -1);
        int index2 = log.indexOf("Caused an ERROR", index);
        assertTrue("test5 error", index2 > -1
                   && log.indexOf("test5 exits with error", index2) > -1);
        assertTrue("Only one error", log.indexOf("ERROR", index2 + 11) == -1);
        index2 = log.indexOf("FAILED", index);
        assertTrue("test4 failure", index2 > -1
                   && log.indexOf("test4 fails", index2) > -1);
        assertTrue("Only one failure", log.indexOf("FAILED", index2 + 1) == -1);
    }
}
