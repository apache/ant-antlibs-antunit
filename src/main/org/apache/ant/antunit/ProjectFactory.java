package org.apache.ant.antunit;

import org.apache.tools.ant.Project;

/** 
 * Provides project instances for AntUnit execution.<br/>  
 * The approach to creates a project depends on the context.  When invoked from an 
 * ant project, some elements might be intialized from the parent project.  When
 * executed in a junit runner, a brand new project must be initialized.<br/>
 * The AntScriptRunner will usually creates multiple project in order to provide test isolation. 
 * @since 1.2
 */
public interface ProjectFactory {
    
    /**
     * Creates a new project instance and configures it according to the execution context.
     */
    public Project createProject();

}
