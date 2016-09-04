package com.cx.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by: Dorg.
 * Date: 17/08/2016.
 */

@Mojo(name = "scan-foreach")
public class CxForeachPlugin extends CxAbstractPlugin {

    @Override
    protected boolean shouldSkip() {
        return "pom".equals(project.getPackaging());
    }

    @Override
    protected void zipSources() throws MojoExecutionException {
        zipSourcesHelper(Collections.singletonList(project));
    }
}
