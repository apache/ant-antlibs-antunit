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

import java.io.IOException;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.LogLevel;
import org.apache.tools.ant.types.resources.StringResource;
import org.apache.tools.ant.util.ResourceUtils;
import org.apache.tools.ant.util.StringUtils;

import junit.framework.Assert;
import junit.framework.TestCase;

public class LogContentTest extends TestCase {

    public LogContentTest(String name) {
        super(name);
    }

    public void testNoLogCapturer() throws IOException {
        LogContent content = new LogContent();
        content.setProject(new Project());
        try {
            content.getInputStream();
            fail("should fail due to no LogCapturer set");
        } catch (IllegalStateException e) {
            //pass
        }
    }

    public void testMessagePriorities() throws IOException {
        Project p = new Project();
        LogCapturer c = new LogCapturer(p);

        String[] msgs = new String[] {"err", "warn", "info", "verbose",
                                          "debug"};
        for (int i = 0; i < msgs.length; i++) {
            BuildEvent be = new BuildEvent(p);
            be.setMessage(msgs[i], i);
            c.messageLogged(be);
        }
        assertMessages(new LogContent(p, LogLevel.ERR), msgs,
                       Project.MSG_ERR);
        assertMessages(new LogContent(p, LogLevel.WARN), msgs,
                       Project.MSG_WARN);
        assertMessages(new LogContent(p, LogLevel.INFO), msgs,
                       Project.MSG_INFO);
        assertMessages(new LogContent(p, LogLevel.VERBOSE), msgs,
                       Project.MSG_VERBOSE);
        assertMessages(new LogContent(p, LogLevel.DEBUG), msgs,
                       Project.MSG_DEBUG);
    }

    public void testWithoutMerge() throws IOException {
        Project p = new Project();
        LogCapturer c = new LogCapturer(p);

        for (int i = 0; i < 2; i++) {
            BuildEvent be = new BuildEvent(p);
            be.setMessage(String.valueOf(i), 0);
            c.messageLogged(be);
        }

        LogContent content = new LogContent(p, LogLevel.ERR, false);
        StringResource s = new StringResource();
        ResourceUtils.copyResource(content, s);

        Assert.assertEquals(s.getValue(),
                            "0" + StringUtils.LINE_SEP
                            + "1" + StringUtils.LINE_SEP);
    }

    public void testWithExplicitMerge() throws IOException {
        Project p = new Project();
        LogCapturer c = new LogCapturer(p);

        for (int i = 0; i < 2; i++) {
            BuildEvent be = new BuildEvent(p);
            be.setMessage(String.valueOf(i), 0);
            c.messageLogged(be);
        }

        LogContent content = new LogContent(p, LogLevel.ERR, true);
        StringResource s = new StringResource();
        ResourceUtils.copyResource(content, s);

        Assert.assertEquals(s.getValue(), "01");
    }

    public void testWithImplicitMerge() throws IOException {
        Project p = new Project();
        LogCapturer c = new LogCapturer(p);

        for (int i = 0; i < 2; i++) {
            BuildEvent be = new BuildEvent(p);
            be.setMessage(String.valueOf(i), 0);
            c.messageLogged(be);
        }

        LogContent content = new LogContent();
        content.setProject(p);
        StringResource s = new StringResource();
        ResourceUtils.copyResource(content, s);

        Assert.assertEquals(s.getValue(), "01");
    }

    private static void assertMessages(LogContent content, String[] messages,
                                       int upTo) throws IOException {
        StringResource s = new StringResource();
        ResourceUtils.copyResource(content, s);
        String actual = s.getValue();
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
