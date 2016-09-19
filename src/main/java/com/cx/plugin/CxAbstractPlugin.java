package com.cx.plugin;

import com.cx.client.CxClientService;
import com.cx.client.CxClientServiceImpl;
import com.cx.client.CxPluginHelper;
import com.cx.client.dto.CreateScanResponse;
import com.cx.client.dto.LocalScanConfiguration;
import com.cx.client.dto.ReportType;
import com.cx.client.dto.ScanResults;
import com.cx.client.exception.CxClientException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.MavenLoggerAdapter;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by: Dorg.
 * Date: 17/08/2016.
 */
public abstract class CxAbstractPlugin extends AbstractMojo {

    @Parameter(required = true, defaultValue = "${cx.username}")
    protected String username;

    @Parameter(required = true, defaultValue="${cx.password}")
    protected String password;

    @Parameter(defaultValue = "${cx.url}")
    protected URL url;

    @Parameter(defaultValue = "${project.name}")
    protected String projectName;

    @Parameter(defaultValue = "${cx.fullTeamPath}")
    protected String fullTeamPath;

    @Parameter(defaultValue = "${cx.preset}")
    protected String preset = "Default 2014";

    @Parameter(defaultValue = "${cx.isIncrementalScan}")
    protected boolean isIncrementalScan = true;

    @Parameter(defaultValue = "${cx.folderExclusions}")
    protected String folderExclusions;

    @Parameter(defaultValue = "${cx.fileExclusions}")
    protected String fileExclusions;

    @Parameter(defaultValue = "${cx.isSynchronous}")
    protected boolean isSynchronous = true;

    @Parameter(defaultValue = "${cx.generatePDFReport}")
    protected boolean generatePDFReport = true;

    @Parameter(defaultValue = "${cx.highSeveritiesThreshHold}")
    protected int highSeveritiesThreshHold = -1;

    @Parameter(defaultValue = "${cx.mediumSeveritiesThreshHold}")
    protected int mediumSeveritiesThreshHold = -1;

    @Parameter(defaultValue = "${cx.lowSeveritiesThreshHold}")
    protected int lowSeveritiesThreshHold = -1;

    @Parameter(defaultValue = "${cx.scanTimeoutInMinuets}")
    protected int scanTimeoutInMinuets = -1;

    @Parameter(defaultValue = "${project.build.directory}\\checkmarx")
    protected File outputDirectory;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    @Parameter( defaultValue = "${reactorProjects}", readonly = true )
    protected List<MavenProject> reactorProjects;


    @Component( role = Archiver.class, hint = "zip" )
    protected ZipArchiver zipArchiver;

    public static final String SOURCES_ZIP_NAME = "sources";
    public static final String PDF_REPORT_NAME = "CxReport";

    protected CxClientService cxClientService;

    String scanResultsUrl;


    private static Logger log = LoggerFactory.getLogger(CxAbstractPlugin.class);


    public void execute() throws MojoExecutionException, MojoFailureException {
        MavenLoggerAdapter.setLogger(getLog());
        ScanResults scanResults;

        if(shouldSkip()) {
            log.info("project has no sources (reactor), skipping");
            return;
        }

        try {

            if(url == null) {
                url = new URL("http://localhost");
            }
            //initialize cx client
            log.debug("initializing cx client");
            cxClientService = new CxClientServiceImpl(url.toString());

            //perform login to server
            log.info("log in to checkmarx service at: " + url);
            cxClientService.loginToServer(username, password);

            //prepare sources to scan (zip them)
            log.info("zipping sources");
            zipSources();

            //send sources to scan
            byte[] zippedSources = getBytesFromZippedSources();
            LocalScanConfiguration conf = generateScanConfiguration(zippedSources);
            log.info("creating scan");
            CreateScanResponse createScanResponse = cxClientService.createLocalScanResolveFields(conf);
            log.info("scan created successfully");

            if(!isSynchronous) {
                log.info("not waiting for scan to finish");
                return;
            }

            //wait for scan to finish
            log.info("waiting for scan to finish");
            cxClientService.waitForScanToFinish(createScanResponse.getRunId(), scanTimeoutInMinuets);
            log.info("scan finished. retrieving scan results");
            scanResults = cxClientService.retrieveScanResults(createScanResponse.getProjectId());

            scanResultsUrl = String.format( url + "/CxWebClient/ViewerMain.aspx?scanId=%s&ProjectID=%s", scanResults.getScanID(), scanResults.getProjectId());

            printResultsToConsole(scanResults);

            log.info("generating html report");
            generateHTMLReport(scanResults.getScanID());

            //create scan report
            if(generatePDFReport) {
                createPDFReport(scanResults.getScanID());
            }

        } catch (CxClientException e) {
            log.debug("caught exception: ", e);
            throw new MojoExecutionException("something went wrong: " + e.getMessage());

        } catch (Exception e) {
            log.debug("unexpected exception:", e);
            throw new MojoExecutionException("something went wrong: " + e.getMessage());
        }

        //assert vulnerabilities under threshold
        if(scanResults != null) {
            assertVulnerabilities(scanResults);
        }

    }

    private void printResultsToConsole(ScanResults scanResults) {
        log.info("----------------------Scan Results:---------------------------");
        log.info("High Severity: " +scanResults.getHighSeverityResults());
        log.info("Medium Severity: " +scanResults.getMediumSeverityResults());
        log.info("Low Severity: " +scanResults.getLowSeverityResults());
        log.info("scan results can be found at: " + scanResultsUrl);
        log.info("--------------------------------------------------------------");
    }

    protected void generateHTMLReport(long scanId){

        try {
            byte[] scanReport = cxClientService.getScanReport(scanId, ReportType.CSV);
            String csv = new String(scanReport);

            File htmlReportFile = new File(outputDirectory + "\\report.html");

            String html = CxPluginHelper.compileHtmlReport(csv, highSeveritiesThreshHold, mediumSeveritiesThreshHold, lowSeveritiesThreshHold);

            if(html != null) {
                FileUtils.writeStringToFile(htmlReportFile, html, Charset.defaultCharset());
                log.info("html report can be found in: " + outputDirectory +  "\\report.html");
            } else {
                log.warn("fail to generate html report");
            }


        } catch (Exception e) {
            log.warn("fail to generate html report");
            log.debug("fail to generate html report: ", e);
        }

    }

    private LocalScanConfiguration generateScanConfiguration(byte[] zippedSources) {
        LocalScanConfiguration ret = new LocalScanConfiguration();
        ret.setProjectName(projectName);
        ret.setFileExclusions(fileExclusions);
        ret.setFolderExclusions(folderExclusions);
        ret.setFullTeamPath(fullTeamPath);
        ret.setIncrementalScan(isIncrementalScan);
        ret.setPreset(preset);
        ret.setZippedSources(zippedSources);
        ret.setFileName(projectName);

        return ret;

    }

    private void assertVulnerabilities(ScanResults scanResults) throws MojoFailureException {

        if(highSeveritiesThreshHold >= 0 && scanResults.getHighSeverityResults() > highSeveritiesThreshHold) {
            throw new MojoFailureException("high severity results is above threshold. results: "+scanResults.getHighSeverityResults()+". threshold: " + highSeveritiesThreshHold);
        }

        if(mediumSeveritiesThreshHold >= 0 && scanResults.getMediumSeverityResults() > mediumSeveritiesThreshHold) {
            throw new MojoFailureException("medium severity results is above threshold. results: "+scanResults.getMediumSeverityResults()+". threshold: " + mediumSeveritiesThreshHold);
        }

        if(lowSeveritiesThreshHold >= 0 && scanResults.getLowSeverityResults() > lowSeveritiesThreshHold) {
            throw new MojoFailureException("low severity results is above threshold. results: "+scanResults.getLowSeverityResults()+". threshold: " + lowSeveritiesThreshHold);
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

    protected byte[] getBytesFromZippedSources2() throws MojoExecutionException {

        log.debug("converting zipped sources to byte array");
        byte[] zipFileByte;
        try {
            InputStream fileStream = new FileInputStream(new File("C:\\temp\\commons-io-1.3.2.zip"));
            zipFileByte = IOUtils.toByteArray(fileStream);
        } catch (FileNotFoundException e) {

            throw new MojoExecutionException("Fail to set zipped file into project.", e);

        } catch (IOException e) {

            throw new MojoExecutionException("Fail to set zipped file into project.", e);
        }

        return zipFileByte;
    }


    protected void createPDFReport(long scanId) {
        log.info("generating PDF report");
        byte[] scanReport;
        try {
            scanReport = cxClientService.getScanReport(scanId, ReportType.PDF);
            FileUtils.writeByteArrayToFile(new File( outputDirectory, PDF_REPORT_NAME + ".pdf"), scanReport);
            log.info("PDF report can be found in: " + outputDirectory +  "\\" + PDF_REPORT_NAME + ".pdf");
        } catch (Exception e) {
            log.warn("fail to generate PDF report");
            log.debug("fail to generate PDF report", e);
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

            //add sources
            List compileSourceRoots = subProject.getCompileSourceRoots();
            for (Object c : compileSourceRoots) {

                File sourceDir = new File((String)c);
                if(sourceDir.exists()) {
                    zipArchiver.addDirectory(sourceDir, prefix);
                }
            }

            //add resources
            List reSourceRoots = subProject.getResources();
            for (Object c : reSourceRoots) {

                Resource resource = (Resource)c;
                File resourceDir = new File(resource.getDirectory());
                if(resourceDir.exists()) {
                    zipArchiver.addDirectory(resourceDir, prefix);
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
