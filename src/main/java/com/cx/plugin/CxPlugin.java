package com.cx.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
