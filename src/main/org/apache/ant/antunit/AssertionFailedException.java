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
import org.apache.tools.ant.Location;

/**
 * Specialized BuildException thrown by the AssertTask task.
 */
public class AssertionFailedException extends BuildException {

    private static final long serialVersionUID = -1193299712860263327L;
    public static final String DEFAULT_MESSAGE = "Test failed";

    public AssertionFailedException(String message) {
        super(message);
    }

    public AssertionFailedException(String message, Location loc) {
        super(message, loc);
    }

    public AssertionFailedException(String message, BuildException e) {
        super(message , e);
    }
}
