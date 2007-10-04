/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.tools.ant.BuildFileTest;

public class SetUpAndTearDownTest extends BuildFileTest {

    public SetUpAndTearDownTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        configureProject("src/etc/testcases/setupandteardown.xml");
    }


    public void testBoth() {
        executeTarget("testBoth");
    }

    public void testNoSetup() {
        executeTarget("testNoSetup");
    }

    public void testNoTeardown() {
        executeTarget("testNoTeardown");
    }

    public void testFailedTest() {
        executeTarget("testFailedTest");
    }

    public void testFailedSetup() {
        executeTarget("testFailedSetup");
    }

    public void testFailedTeardown() {
        executeTarget("testFailedTeardown");
    }
}