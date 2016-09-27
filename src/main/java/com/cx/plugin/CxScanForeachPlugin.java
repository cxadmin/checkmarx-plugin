package com.cx.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.Collections;

/**
 * The 'scan-foreach' goal creates a separate scan for each of the modules found in the reactor.
 */
@Mojo(name = "scan-foreach", requiresDependencyResolution = ResolutionScope.TEST)
public class CxScanForeachPlugin extends CxAbstractPlugin {

    @Override
    protected boolean shouldSkip() {
        return "pom".equals(project.getPackaging());
    }

    @Override
    protected void zipSources() throws MojoExecutionException {
        zipSourcesHelper(Collections.singletonList(project));
    }
}
