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
import org.apache.tools.ant.Project;

/** 
 * Provides project instances for AntUnit execution.
 * <p>The approach to creates a project depends on the context.  When invoked from an 
 * ant project, some elements might be intialized from the parent project.  When
 * executed in a junit runner, a brand new project must be initialized.</p>
 * <p>The AntScriptRunner will usually create multiple project in order to provide test isolation.</p>
 * @since 1.2
 */
public interface ProjectFactory {
    
    /**
     * Creates a new project instance and configures it according to the execution context.
     * @throws BuildException The project can not be created (probably parsed)
     * @return a new project
     */
    Project createProject() throws BuildException;

}
