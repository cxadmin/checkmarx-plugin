package com.cx.plugin;

import com.checkmarx.v7.CxWSReportType;
import com.checkmarx.v7.ScanDisplayData;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import java.io.*;
import java.net.URL;
import java.util.List;

/**
 * Created by: Dorg.
 * Date: 17/08/2016.
 */
public abstract class CxAbstractPlugin extends AbstractMojo {

    @Parameter(required = true)
    protected String username;

    @Parameter(required = true)
    protected String password;

    @Parameter(required = true)
    protected URL url;

    @Parameter(defaultValue = "${projectName}")//todo default value?
    protected String projectName;

    @Parameter
    protected String fullTeamPath;

    @Parameter
    protected String preset;

    @Parameter
    protected boolean isIncrementalScan;

    @Parameter
    protected String folderExclusions;

    @Parameter
    protected String fileExclusions;

    @Parameter//todo implement
    protected boolean isSynchronous;

    @Parameter//todo choose name
    protected boolean generatePDFReport;

    @Parameter(defaultValue = "-1")//todo what default values should threshold have? maybe it is required field?
    protected int highSeveritiesThreshHold;

    @Parameter(defaultValue = "-1")
    protected int mediumSeveritiesThreshHold;

    @Parameter(defaultValue = "-1")
    protected int lowSeveritiesThreshHold;

    @Parameter(defaultValue = "${project.build.directory}\\checkmarx")
    protected File outputDirectory;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    @Parameter( defaultValue = "${project.build.finalName}", readonly = true )
    protected String finalName;

    @Parameter( defaultValue = "${reactorProjects}", readonly = true )
    protected List<MavenProject> reactorProjects;


    @Component( role = Archiver.class, hint = "zip" )
    protected ZipArchiver zipArchiver;

    public static final String SOURCES_ZIP_NAME = "sources";
    public static final String PDF_REPORT_NAME = "report";

    protected CxClient cxClient;

    private Log log = getLog();


    //todo scan timeout, what about "generate report" timeout? what about logging?
    public void execute() throws MojoExecutionException, MojoFailureException {

        if(shouldSkip()) {
            log.info("project has no sources (reactor), skipping");
            return;
        }

        //initialize cx client
        log.debug("initializing cx client");
        cxClient = new CxClient(username, password, url, getLog());

        //

        //perform login to server
        getLog().info("logging in to checkmarx service at: " +url);
        cxClient.loginToServer();

        //prepare sources to scan (zip them)
        getLog().info("zipping sources");
        zipSources();

        //send sources to scan
        byte[] zippedSources = getBytesFromZippedSources();
        ScanConfiguration conf = generateScanConfiguration(zippedSources);
        log.info("preparing project");
        cxClient.createProject(conf);
        log.info("project created successfully");

        if(isSynchronous) {
            log.info("not waiting for scan to finnish");
            return;
        }
        //wait for scan to finnish
        cxClient.waitForScanToFinnish();

        //create scan report
        if(generatePDFReport) {
            log.info("generating PDF report");
            createScanReport();
        }

        //get scan results
        ScanDisplayData scanResults = cxClient.getScanResults();

        //assert vulnerabilities under threshold
        assertVulnerabilities(scanResults);


    }

    private ScanConfiguration generateScanConfiguration(byte[] zippedSources) {
        ScanConfiguration ret = new ScanConfiguration();
        ret.setProjectName(projectName);
        ret.setFileExclusions(fileExclusions);
        ret.setFolderExclusions(folderExclusions);
        ret.setFullTeamPath(fullTeamPath);
        ret.setIncrementalScan(isIncrementalScan);
        ret.setPreset(preset);
        ret.setZippedSources(zippedSources);

        return ret;
    }

    private void assertVulnerabilities(ScanDisplayData scanResults) throws MojoFailureException {

        if(highSeveritiesThreshHold > 0 && scanResults.getHighSeverityResults() > highSeveritiesThreshHold) {
            throw new MojoFailureException("high severity results is above threshold: " +scanResults.getHighSeverityResults());
        }

        if(mediumSeveritiesThreshHold > 0 && scanResults.getMediumSeverityResults() > mediumSeveritiesThreshHold) {
            throw new MojoFailureException("medium severity results is above threshold: " +scanResults.getMediumSeverityResults());
        }

        if(lowSeveritiesThreshHold > 0 && scanResults.getLowSeverityResults() > lowSeveritiesThreshHold) {
            throw new MojoFailureException("low severity results is above threshold: " +scanResults.getLowSeverityResults());
        }

    }

    protected byte[] getBytesFromZippedSources() throws MojoExecutionException {

        log.debug("converting zipped sources to byte array");
        byte[] zipFileByte;
        try {
            InputStream fileStream = new FileInputStream(new File(outputDirectory, SOURCES_ZIP_NAME + ".zip"));
            zipFileByte = IOUtils.toByteArray(fileStream);
        } catch (FileNotFoundException e) {

            throw new MojoExecutionException("Fail to set zipped file into project.", e);

        } catch (IOException e) {

            throw new MojoExecutionException("Fail to set zipped file into project.", e);
        }

        return zipFileByte;
    }


    protected void createScanReport() throws MojoExecutionException {
        byte[] scanReport = cxClient.getScanReport(CxWSReportType.PDF);

        try {
            FileUtils.writeByteArrayToFile(new File( outputDirectory, PDF_REPORT_NAME + ".pdf"), scanReport);
        } catch (IOException e) {
            throw new MojoExecutionException("fail to create report", e);
        }

    }

    protected abstract boolean shouldSkip();



    protected abstract void zipSources() throws MojoExecutionException;

    protected void zipSourcesHelper(List<MavenProject> projects) throws MojoExecutionException {

        for (MavenProject p : projects) {

            MavenProject subProject = getProject(p);
            if ( "pom".equals(subProject.getPackaging())) {
                continue;
            }

            String prefix = subProject.getName() + "\\";

            List compileSourceRoots = subProject.getCompileSourceRoots();
            for (Object c : compileSourceRoots) {

                File sourceDir = new File((String)c);
                if(sourceDir.exists()) {
                    zipArchiver.addDirectory(sourceDir, prefix);
                }
            }
        }



        zipArchiver.setDestFile(new File(outputDirectory, SOURCES_ZIP_NAME + ".zip"));
        try {
            zipArchiver.createArchive();
            log.info("sources zipped at: " + outputDirectory + "\\" +SOURCES_ZIP_NAME + ".zip");
        } catch (IOException e) {
            throw new MojoExecutionException("fail to create zip sources: ", e);
        }

    }


    protected MavenProject getProject(MavenProject p) {
        if (p.getExecutionProject() != null) {
            return p.getExecutionProject();
        }

        return p;
    }


}
