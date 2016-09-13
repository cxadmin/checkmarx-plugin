package com.cx.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Created by: Dorg.
 * Date: 17/08/2016.
 */

@Mojo(name = "scan", aggregator = true)
public class CxPlugin extends CxAbstractPlugin {


    @Override
    protected boolean shouldSkip() {
        return false;
    }

    @Override
    protected void zipSources() throws MojoExecutionException {
        zipSourcesHelper(reactorProjects);
    }

}
