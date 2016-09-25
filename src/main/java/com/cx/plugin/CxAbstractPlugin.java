package com.cx.plugin;

import com.cx.client.CxClientService;
import com.cx.client.CxClientServiceImpl;
import com.cx.client.CxPluginHelper;
import com.cx.client.dto.*;
import com.cx.client.exception.CxClientException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.maven.artifact.Artifact;
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
import java.util.Date;
import java.util.List;
import java.util.Set;

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

    @Parameter(defaultValue = "${cx.highSeveritiesThreshold}")
    protected int highSeveritiesThreshold = -1;

    @Parameter(defaultValue = "${cx.mediumSeveritiesThreshold}")
    protected int mediumSeveritiesThreshold = -1;

    @Parameter(defaultValue = "${cx.lowSeveritiesThreshold}")
    protected int lowSeveritiesThreshold = -1;

    @Parameter(defaultValue = "${cx.scanTimeoutInMinuets}")
    protected int scanTimeoutInMinuets = 0;

    @Parameter(defaultValue = "${project.build.directory}\\checkmarx")
    protected File outputDirectory;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    @Parameter( defaultValue = "${reactorProjects}", readonly = true )
    protected List<MavenProject> reactorProjects;


    @Component( role = Archiver.class, hint = "zip" )
    protected ZipArchiver zipArchiver;

    public static final String SOURCES_ZIP_NAME = "sources";
    public static final String OSA_ZIP_NAME = "OSAScan";
    public static final String PDF_REPORT_NAME = "CxReport";

    protected CxClientService cxClientService;

    String scanResultsUrl;


    private static Logger log = LoggerFactory.getLogger(CxAbstractPlugin.class);


    public void execute() throws MojoExecutionException, MojoFailureException {
        MavenLoggerAdapter.setLogger(getLog());
        ScanResults scanResults;

        if(shouldSkip()) {
            log.info("Project Has No Sources (Reactor), Skipping");
            return;
        }

        try {

            if(url == null) {
                url = new URL("http://localhost");
            }

            printConfiguration();
            //initialize cx client
            log.debug("Initializing Cx Client");
            cxClientService = new CxClientServiceImpl(url.toString());

            //perform login to server
            log.info("Logging In to Checkmarx Service.");
            cxClientService.loginToServer(username, password);

            //prepare sources to scan (zip them)
            log.info("Zipping Sources");
            zipSources();

            //send sources to scan
            byte[] zippedSources = getBytesFromZippedSources();
            LocalScanConfiguration conf = generateScanConfiguration(zippedSources);
            log.info("Creating Scan");
            CreateScanResponse createScanResponse = cxClientService.createLocalScanResolveFields(conf);
            log.info("Scan Created Successfully. Link to Project State: " + CxPluginHelper.composeProjectStateLink(url.toString(), createScanResponse.getProjectId()));

            if(!isSynchronous) {
                log.info("Running in Asynchronous Mode. Not Waiting for Scan to Finish");
                return;
            }

            //wait for scan to finish
            log.info("Starting Scan.");
            cxClientService.waitForScanToFinish(createScanResponse.getRunId(), scanTimeoutInMinuets);
            log.info("Scan Finished. Retrieving Scan Results");
            scanResults = cxClientService.retrieveScanResults(createScanResponse.getProjectId());

            scanResultsUrl = CxPluginHelper.composeScanLink(url.toString(), scanResults);

            printResultsToConsole(scanResults);

            log.info("Generating HTML Report");
            generateHTMLReport(scanResults.getScanID());

            //create scan report
            if(generatePDFReport) {
                createPDFReport(scanResults.getScanID());
            }

        } catch (CxClientException e) {
            log.debug("Caught Exception: ", e);
            throw new MojoExecutionException("Something Went Wrong: " + e.getMessage());

        } catch (Exception e) {
            log.debug("Unexpected Exception:", e);
            throw new MojoExecutionException("Something Went Wrong: " + e.getMessage());
        }

        //assert vulnerabilities under threshold
        assertVulnerabilities(scanResults);

    }

    private void printConfiguration() {
        log.info("----------------------------Configurations:-----------------------------");
        log.info("username: " + username);
        log.info("url: " + url);
        log.info("projectName: " + projectName);
        log.info("fullTeamPath: " + fullTeamPath);
        log.info("preset: " + preset);
        log.info("isIncrementalScan: " + isIncrementalScan);
        log.info("folderExclusions: " + folderExclusions);
        log.info("fileExclusions: " + fileExclusions);
        log.info("isSynchronous: " + isSynchronous);
        log.info("generatePDFReport: " + generatePDFReport);
        log.info("highSeveritiesThreshold: " + highSeveritiesThreshold);
        log.info("mediumSeveritiesThreshold: " + mediumSeveritiesThreshold);
        log.info("lowSeveritiesThreshold: " + lowSeveritiesThreshold);
        log.info("scanTimeoutInMinuets: " + scanTimeoutInMinuets);
        log.info("outputDirectory: " + outputDirectory);
        log.info("------------------------------------------------------------------------");

    }

    private void printResultsToConsole(ScanResults scanResults) {
        log.info("----------------------------Scan Results:-------------------------------");
        log.info("High Severity Results: " +scanResults.getHighSeverityResults());
        log.info("Medium Severity Results: " +scanResults.getMediumSeverityResults());
        log.info("Low Severity Results: " +scanResults.getLowSeverityResults());
        log.info("Info Severity Results: " +scanResults.getInfoSeverityResults());
        log.info("Scan Results Can Be Found at: " + scanResultsUrl);
        log.info("------------------------------------------------------------------------");
    }

    protected void generateHTMLReport(long scanId){

        try {
            byte[] scanReport = cxClientService.getScanReport(scanId, ReportType.CSV);
            String csv = new String(scanReport);

            File htmlReportFile = new File(outputDirectory + "\\report.html");

            String html = CxPluginHelper.compileHtmlReport(csv, highSeveritiesThreshold, mediumSeveritiesThreshold, lowSeveritiesThreshold);

            if(html != null) {
                FileUtils.writeStringToFile(htmlReportFile, html, Charset.defaultCharset());
                log.info("HTML Report Can Be Found in: " + outputDirectory +  "\\report.html");
            } else {
                log.warn("Fail To Generate HTML Report");
            }


        } catch (Exception e) {
            log.warn("Fail To Generate HTML Report");
            log.debug("Fail To Generate HTML Report: ", e);
        }

    }

    private LocalScanConfiguration generateScanConfiguration(byte[] zippedSources) {
        LocalScanConfiguration ret = new LocalScanConfiguration();
        ret.setProjectName(projectName);
        ret.setClientOrigin(ClientOrigin.MAVEN);
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

        StringBuilder res = new StringBuilder("\n");
        boolean fail = false;
        if(highSeveritiesThreshold >= 0 && scanResults.getHighSeverityResults() > highSeveritiesThreshold) {
            res.append("High Severity Results are Above Threshold. Results: "+scanResults.getHighSeverityResults()+". Threshold: " + highSeveritiesThreshold).append("\n");
            fail = true;
        }

        if(mediumSeveritiesThreshold >= 0 && scanResults.getMediumSeverityResults() > mediumSeveritiesThreshold) {
            res.append("Medium Severity Results are Above Threshold. Results: "+scanResults.getMediumSeverityResults()+". Threshold: " + mediumSeveritiesThreshold).append("\n");
            fail = true;

        }

        if(lowSeveritiesThreshold >= 0 && scanResults.getLowSeverityResults() > lowSeveritiesThreshold) {
            res.append("Low Severity Results are Above Threshold. Results: "+scanResults.getLowSeverityResults()+". Threshold: " + lowSeveritiesThreshold).append("\n");
            fail = true;
        }

        if(fail) {
            throw new MojoFailureException(res.toString());
        }
    }

    protected byte[] getBytesFromZippedSources() throws MojoExecutionException {

        log.debug("Converting Zipped Sources to Byte Array");
        byte[] zipFileByte;
        try {
            InputStream fileStream = new FileInputStream(new File(outputDirectory, SOURCES_ZIP_NAME + ".zip"));
            zipFileByte = IOUtils.toByteArray(fileStream);
        } catch (FileNotFoundException e) {

            throw new MojoExecutionException("Fail to Set Zipped File Into Project.", e);

        } catch (IOException e) {

            throw new MojoExecutionException("Fail to Set Zipped File Into Project.", e);
        }

        return zipFileByte;
    }


    protected void createPDFReport(long scanId) {
        log.info("Generating PDF Report");
        byte[] scanReport;
        try {
            scanReport = cxClientService.getScanReport(scanId, ReportType.PDF);
            String now = DateFormatUtils.format(new Date(), "dd_MM_yyyy-HH_mm_ss");
            FileUtils.writeByteArrayToFile(new File( outputDirectory, PDF_REPORT_NAME + "_" + now + ".pdf"), scanReport);
            log.info("PDF Report Can Be Found in: " + outputDirectory +  "\\" + PDF_REPORT_NAME + ".pdf");
        } catch (Exception e) {
            log.warn("Fail to Generate PDF Report");
            log.debug("Fail to Generate PDF Report", e);
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
            log.info("Sources Zipped at: " + outputDirectory + "\\" +SOURCES_ZIP_NAME + ".zip");
        } catch (IOException e) {
            throw new MojoExecutionException("Fail to Create Zip Sources: ", e);
        }

    }


    protected MavenProject getProject(MavenProject p) {
        if (p.getExecutionProject() != null) {
            return p.getExecutionProject();
        }

        return p;
    }


    private void createZipForOSA() throws MojoExecutionException {

        Set artifacts = project.getArtifacts();
        for (Object arti :artifacts) {
            Artifact a = (Artifact)arti;
            zipArchiver.addFile(a.getFile(), a.getFile().getName());
        }

        zipArchiver.setDestFile(new File(outputDirectory, OSA_ZIP_NAME + ".zip"));
        try {
            zipArchiver.createArchive();
            log.info("Files for OSA scan zipped at: " + outputDirectory + "\\" +OSA_ZIP_NAME + ".zip");
        } catch (IOException e) {
            throw new MojoExecutionException("Fail to zip files for OSA scan:", e);
        }

    }


}
