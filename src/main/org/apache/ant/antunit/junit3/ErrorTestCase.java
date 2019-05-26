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

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;

/**
 * A TestCase that will just report an error when running. This is pretty useful
 * when an error is detected during initialization.
 */
public class ErrorTestCase extends TestCase {

    /** The name we use for the error test case ('warning') */
    public static final String NAME = "warning";

    private final BuildException ex;

    /**
     * Creates a TestCase that will report the Ant BuildException when running.
     * @param antScriptError The Ant BuildException that triggered the initialization 
     * failure
     */
    public ErrorTestCase(BuildException antScriptError) {
        super(NAME);
        this.ex = antScriptError;
    }

    protected void runTest() throws BuildException {
        throw ex;
    }

    public BuildException getAntScriptError() {
        return ex;
    }

}
