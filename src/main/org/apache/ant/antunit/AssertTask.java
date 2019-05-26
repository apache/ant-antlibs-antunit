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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;

/**
 * Exits the active build, giving an additional message if the single
 * nested condition fails or if there is no condition at all.
 *
 * <p>This one could as well be implemented as</p>
 *
 * <pre>
 * &lt;macrodef name="assertTrue"&gt;
 *   &lt;attribute name="message" default="Assertion failed"/&gt;
 *   &lt;element name="assertion" implicit="true"/&gt;
 *   &lt;sequential&gt;
 *     &lt;fail message="@{message}"&gt;
 *       &lt;condition&gt;
 *         &lt;assertion/&gt;
 *       &lt;/condition&gt;
 *     &lt;/fail&gt;
 *   &lt;/sequential&gt;
 * &lt;/macrodef&gt;
 * </pre>
 * 
 * <p>but wouldn't be able to throw a specialized exception that way -
 * and the macrodef would nest the exception in yet another
 * BuildException.</p>
 */
public class AssertTask extends ConditionBase {

    /**
     * Message to use when the assertion fails.
     */
    private String message = AssertionFailedException.DEFAULT_MESSAGE;

    /**
     * Message to use when the assertion fails.
     * @param value message to use when the assertion fails
     */
    public void setMessage(String value) {
        this.message = value;
    }

    public void execute() throws BuildException {
        int count = countConditions();
        if (count > 1) {
            throw new BuildException("You must not specify more than one "
                                     + "condition", getLocation());
        }
        if (count < 1 || !((Condition) getConditions().nextElement()).eval()) {
            throw new AssertionFailedException(message, getLocation());
        }
    }

}
