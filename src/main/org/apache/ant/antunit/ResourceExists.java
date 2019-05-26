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
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * A condition that tests whether a given resource exists.
 *
 * @since AntUnit 1.2
 */
public class ResourceExists extends ProjectComponent implements Condition {
    private Resource resource;
    private String refid;

    /**
     * The resource to check as attribute.
     *
     * <p>Exactly one resource must be specfied either as attribute or
     * nested element.</p>
     * @param r resource to check
     */
    public void setResource(Resource r) {
        onlyOne();
        resource = r;
    }

    /**
     * The resource to check as a refid.
     * @since AntUnit 1.3
     * @param refid resource to check as a refid
     */
    public void setRefid(String refid) {
        onlyOne();
        this.refid = refid;
    }

    /**
     * The resource to check as nested element.
     *
     * <p>Exactly one resource must be specfied either as attribute or
     * nested element.</p>
     * @param r resource to check
     */
    public void add(Resource r) {
        setResource(r);
    }

    public boolean eval() {
        if (resource == null && refid == null) {
            throw new BuildException("You must specify a resource.");
        }
        Resource r = resource != null ? resource : expandRefId();
        log("Checking: " + r, Project.MSG_VERBOSE);
        return r.isExists();
    }

    private void onlyOne() {
        if (resource != null || refid != null) {
            throw new BuildException("Only one resource can be tested.");
        }
    }

    // logic stolen from Ant's ResourceContains condition
    private Resource expandRefId() {
        if (getProject() == null) {
            throw new BuildException("Cannot retrieve refid; project unset");
        }
        Object o = getProject().getReference(refid);
        if (!(o instanceof Resource)) {
            if (o instanceof ResourceCollection) {
                ResourceCollection rc = (ResourceCollection) o;
                if (rc.size() == 1) {
                    o = rc.iterator().next();
                } else {
                    throw new BuildException("Referred resource collection must"
                                             + " contain exactly one resource.");
                }
            } else {
                throw new BuildException("'" + refid + "' is not a resource but "
                                         + String.valueOf(o));
            }
        }
        return (Resource) o;
    }
}
