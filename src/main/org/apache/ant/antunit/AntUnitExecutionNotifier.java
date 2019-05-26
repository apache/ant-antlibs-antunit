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


/** 
 * Provides methods that allow the AntUnitScriptRunner to notify the test progress.
 * @since 1.2
 */
public interface AntUnitExecutionNotifier {

    /**
     * invokes start on all registered test listeners.
     * @param targetName the name of the target.
     */
    public void fireStartTest(String targetName);

    /**
     * invokes addFailure on all registered test listeners.
     * @param targetName the name of the failed target.
     * @param ae the associated AssertionFailedException.
     */
    public void fireFail(String targetName, AssertionFailedException ae);

    /**
     * invokes addError on all registered test listeners.
     * @param targetName the name of the failed target.
     * @param t the associated Throwable.
     */
    public void fireError(String targetName, Throwable t);

    /**
     * invokes endTest on all registered test listeners.
     * @param targetName the name of the current target.
     */
    public void fireEndTest(String targetName);

}
