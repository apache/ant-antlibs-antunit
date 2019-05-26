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

import java.io.File;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.ant.antunit.junit4.AntUnitSuiteRunner;
import org.junit.runner.RunWith;

/**
 * A unit test using the junit3 and junit4 adapter.
 */
@RunWith(AntUnitSuiteRunner.class)
public class EatYourOwnDogFoodTest extends TestCase {

    public static TestSuite suite() {
        File script = new File("src/etc/testcases/antunit/java-io.xml");
        return new AntUnitSuite(script, EatYourOwnDogFoodTest.class);
    }

}
