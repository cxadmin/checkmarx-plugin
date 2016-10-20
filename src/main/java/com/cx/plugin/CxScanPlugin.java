package com.cx.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * The 'scan' goal creates a single comprehensive scan, including all the modules in the reactor.
 */
@Mojo(name = "scan", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST, inheritByDefault = false)
public class CxScanPlugin extends CxAbstractPlugin {


    @Override
    protected boolean shouldSkip() {
        return false;
    }

    @Override
    protected void zipSources() throws MojoExecutionException {
        zipSourcesHelper(reactorProjects);
    }

}
