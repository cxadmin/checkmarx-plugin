package com.cx.plugin.utils;


import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ScanResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.slf4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static com.cx.plugin.CxScanPlugin.SOURCES_ZIP_NAME;

/**
 * Created by Galn on 24/10/2017.
 */

public abstract class CxPluginUtils {

    public static void printLogo(Logger log) {

        //design by Gal Nussbaum <gal.nussbaum@checkmarx.com>
        log.info(
                "                                            \n" +
                        "         CxCxCxCxCxCxCxCxCxCxCxC            \n" +
                        "        CxCxCxCxCxCxCxCxCxCxCxCxCx          \n" +
                        "       CxCxCxCxCxCxCxCxCxCxCxCxCxCx         \n" +
                        "      CxCxCx                CxCxCxCx        \n" +
                        "      CxCxCx                CxCxCxCx        \n" +
                        "      CxCxCx  CxCxCx      CxCxCxCxC         \n" +
                        "      CxCxCx  xCxCxCx  .CxCxCxCxCx          \n" +
                        "      CxCxCx   xCxCxCxCxCxCxCxCx            \n" +
                        "      CxCxCx    xCxCxCxCxCxCx               \n" +
                        "      CxCxCx     CxCxCxCxCx   CxCxCx        \n" +
                        "      CxCxCx       xCxCxC     CxCxCx        \n" +
                        "      CxCxCx                 CxCxCx         \n" +
                        "       CxCxCxCxCxCxCxCxCxCxCxCxCxCx         \n" +
                        "        CxCxCxCxCxCxCxCxCxCxCxCxCx          \n" +
                        "          CxCxCxCxCxCxCxCxCxCxCx            \n" +
                        "                                            \n" +
                        "            C H E C K M A R X               \n"
        );

    }

    public static void printConfiguration(CxScanConfig config, String[] osaIgnoreScopes, String pluginVersion, Logger log) {
        log.info("---------------------------------------Configurations:------------------------------------");
        log.info("Maven plugin version: " + pluginVersion);
        log.info("Username: " + config.getUsername());
        log.info("URL: " + config.getUrl());
        log.info("Project name: " + config.getProjectName());
        log.info("outputDirectory: " + config.getReportsDir());
        log.info("Deny project creation: " + config.getDenyProject()); //todo check
        log.info("Scan timeout in minutes: " + (config.getSastScanTimeoutInMinutes() <= 0 ? "" : config.getSastScanTimeoutInMinutes()));
        log.info("Full team path: " + config.getTeamPath());
        log.info("Preset: " + config.getPresetName());
        log.info("Is incremental scan: " + config.getIncremental());
        log.info("Folder exclusions: " + (config.getSastFolderExclusions()));
        log.info("Is synchronous scan: " + config.getSynchronous());
        log.info("Generate PDF report: " + config.getGeneratePDFReport());
        log.info("Policy violations enabled: " + config.getEnablePolicyViolations());
        log.info("CxSAST thresholds enabled: " + config.getSastThresholdsEnabled());
        if (config.getSastThresholdsEnabled()) {
            log.info("CxSAST high threshold: " + (config.getSastHighThreshold() == null ? "[No Threshold]" : config.getSastHighThreshold()));
            log.info("CxSAST medium threshold: " + (config.getSastMediumThreshold() == null ? "[No Threshold]" : config.getSastMediumThreshold()));
            log.info("CxSAST low threshold: " + (config.getSastLowThreshold() == null ? "[No Threshold]" : config.getSastLowThreshold()));
        }
        log.info("CxOSA enabled: " + config.getOsaEnabled());
        if (config.getOsaEnabled()) {
            log.info("osaIgnoreScopes: " + Arrays.toString(osaIgnoreScopes));
            log.info("CxOSA thresholds enabled: " + config.getOsaThresholdsEnabled());
            if (config.getOsaThresholdsEnabled()) {
                log.info("CxOSA high threshold: " + (config.getOsaHighThreshold() == null ? "[No Threshold]" : config.getOsaHighThreshold()));
                log.info("CxOSA medium threshold: " + (config.getOsaMediumThreshold() == null ? "[No Threshold]" : config.getOsaMediumThreshold()));
                log.info("CxOSA low threshold: " + (config.getOsaLowThreshold() == null ? "[No Threshold]" : config.getOsaLowThreshold()));
            }
        }
        log.info("------------------------------------------------------------------------------------------");
        //todo check log.info("fileExclusions: " + Arrays.toString(fileExclusions));
    }

    public static void assertBuildFailure(String thDescription, ScanResults ret) throws MojoFailureException {
        StringBuilder builder = new StringBuilder();
        builder.append("*****The Build Failed for the Following Reasons: *****");

        appendError(ret.getGeneralException(), builder);
        appendError(ret.getSastCreateException(), builder);
        appendError(ret.getSastWaitException(), builder);
        appendError(ret.getOsaCreateException(), builder);
        appendError(ret.getOsaWaitException(), builder);

        String[] lines = thDescription.split("\\n");
        for (String s : lines) {
            builder.append(s);
        }
        builder.append("-----------------------------------------------------------------------------------------\n");

        throw new MojoFailureException(builder.toString());
    }

    private static StringBuilder appendError(Exception ex, StringBuilder builder) {
        if (ex != null) {
            builder.append(ex.getMessage()).append("\\n");
        }
        return builder;
    }

    public static Integer resolveInt(String value, Logger log) {
        Integer inti = null;
        if (!StringUtils.isEmpty(value)) {
            try {
                inti = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                log.warn("failed to parse integer value: " + value);
            }
        }
        return inti;
    }

    public static File zipSources(List<MavenProject> projects, ZipArchiver zipArchiver, File outputDirectory, Logger log) throws MojoExecutionException {
        for (MavenProject p : projects) {

            MavenProject subProject = getProject(p);
            if ("pom".equals(subProject.getPackaging())) {
                continue;
            }

            String prefix = subProject.getName() + "\\";

            //add sources
            List compileSourceRoots = subProject.getCompileSourceRoots();
            File sourceDir = subProject.getBasedir();//todo check if java not exist (source not exist)

            for (Object c : compileSourceRoots) {
                sourceDir = new File((String) c);
                if (sourceDir.exists()) {
                    zipArchiver.addDirectory(sourceDir, prefix);
                }
            }

            //add webapp sources
            File[] webappDir = sourceDir.getParentFile().listFiles(new FilenameFilter() {
                public boolean accept(File directory, String fileName) {
                    return fileName.endsWith("webapp");
                }
            });
            if (webappDir.length > 0 && webappDir[0].exists()) {
                zipArchiver.addDirectory(webappDir[0], prefix);
            }

            //add resources
            List reSourceRoots = subProject.getResources();
            for (Object c : reSourceRoots) {

                Resource resource = (Resource) c;
                File resourceDir = new File(resource.getDirectory());
                if (resourceDir.exists()) {
                    zipArchiver.addDirectory(resourceDir, prefix);
                }
            }

            //add scripts
            List scriptSourceRoots = subProject.getScriptSourceRoots();
            for (Object c : scriptSourceRoots) {
                File scriptDir = new File((String) c);
                if (scriptDir.exists()) {
                    zipArchiver.addDirectory(scriptDir, prefix);
                }
            }
        }

        zipArchiver.setDestFile(new File(outputDirectory, SOURCES_ZIP_NAME + ".zip"));
        try {
            zipArchiver.createArchive();
            log.info("Sources zip location: " + outputDirectory + File.separator + SOURCES_ZIP_NAME + ".zip");
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to zip sources: ", e);
        }

        return new File(outputDirectory, SOURCES_ZIP_NAME + ".zip");
    }

    private static MavenProject getProject(MavenProject p) {
        if (p.getExecutionProject() != null) {
            return p.getExecutionProject();
        }

        return p;
    }

    public static Properties generateOSAScanConfiguration(String scanFolder, String[] osaIgnoreScopes, String dummyFilename) {

        Properties ret = new Properties();

        ret.put("includes", dummyFilename);

        if (osaIgnoreScopes != null && osaIgnoreScopes.length > 0) {
            ret.put("maven.ignoredScopes", StringUtils.join(",", osaIgnoreScopes));
        }
        ret.put("d", scanFolder);

        return ret;
    }
}


