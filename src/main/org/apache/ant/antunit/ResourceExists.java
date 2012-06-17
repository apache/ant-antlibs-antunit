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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.Resource;

/**
 * A condition that tests whether a given resource exists.
 *
 * @since AntUnit 1.2
 */
public class ResourceExists extends ProjectComponent implements Condition {
    private Resource resource;

    /**
     * The resource to check as attribute.
     *
     * <p>Exactly one resource must be specfied either as attribute or
     * nested element.</p>
     */
    public void setResource(Resource r) {
        if (resource != null) {
            throw new BuildException("Only one resource can be tested.");
        }
        resource = r;
        System.err.println("R: " + r);
    }

    /**
     * The resource to check as nested element.
     *
     * <p>Exactly one resource must be specfied either as attribute or
     * nested element.</p>
     */
    public void add(Resource r) {
        setResource(r);
    }

    public boolean eval() {
        if (resource == null) {
            throw new BuildException("You must specify a resource.");
        }
        return resource.isExists();
    }
}
