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
import org.apache.tools.ant.taskdefs.Sequential;

/**
 * Expects the nested tasks to throw a BuildException and optinally
 * asserts the message of that exception.
 *
 * <p>Throws a AssertFailedException if the nested tasks do not throw
 * the expected BuildException.</p>
 */
public class ExpectFailureTask extends Sequential {

    private String expectedMessage;
    private String message;

    /**
     * The exception message to expect.
     */
    public void setExpectedMessage(String m) {
        expectedMessage = m;
    }

    /**
     * The message to use in the AssertinFailedException if the nested
     * tasks fail to raise the "correct" exception.
     */
    public void setMessage(String m) {
        message = m;
    }

    public void execute() {
        boolean thrown = false;
        try {
            super.execute();
        } catch (BuildException e) {
            thrown = true;
            if (expectedMessage != null
                && !expectedMessage.equals(e.getMessage())) {
                if (message == null) {
                    throw new AssertionFailedException("Expected build failure "
                                                       + "with message '"
                                                       + expectedMessage
                                                       + "' but was '"
                                                       + e.getMessage() + "'");
                } else {
                    throw new AssertionFailedException(message);
                }
            }
        }

        if (!thrown) {
            if (message == null) {
                throw new AssertionFailedException("Expected build failure");
            } else {
                throw new AssertionFailedException(message);
            }
        }
    }
}        