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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;

import junit.framework.Assert;
import junit.framework.TestCase;

public class LogCapturerTest extends TestCase {

    public LogCapturerTest(String name) {
        super(name);
    }

    public void testAddAsReference() {
        Project p = new Project();
        LogCapturer c = new LogCapturer(p);
        assertSame(c, p.getReference(LogCapturer.REFERENCE_ID));
    }

    public void testMessagePriorities() {
        Project p = new Project();
        LogCapturer c = new LogCapturer(p);

        String[] messages = new String[] {"err", "warn", "info", "verbose",
                                          "debug"};
        for (int i = 0; i < messages.length; i++) {
            BuildEvent be = new BuildEvent(p);
            be.setMessage(messages[i], i);
            c.messageLogged(be);
        }
        assertMessages(c.getErrLog(), messages, Project.MSG_ERR);
        assertMessages(c.getWarnLog(), messages, Project.MSG_WARN);
        assertMessages(c.getInfoLog(), messages, Project.MSG_INFO);
        assertMessages(c.getVerboseLog(), messages, Project.MSG_VERBOSE);
        assertMessages(c.getDebugLog(), messages, Project.MSG_DEBUG);
    }

    private static void assertMessages(String actual, String[] messages,
                                       int upTo) {
        for (int i = 0; i <= upTo && i < messages.length; i++) {
            Assert.assertTrue("checking for " + messages[i] + " in " + actual,
                              actual.indexOf(messages[i]) > -1);
        }
        for (int i = upTo + 1; i < messages.length; i++) {
            Assert.assertTrue("checking for " + messages[i] + " in " + actual,
                              actual.indexOf(messages[i]) == -1);
        }
    }
}