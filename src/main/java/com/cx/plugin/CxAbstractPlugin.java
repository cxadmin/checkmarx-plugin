package com.cx.plugin;

import com.cx.client.*;
import com.cx.client.dto.*;
import com.cx.client.exception.CxClientException;
import com.cx.client.rest.dto.CreateOSAScanResponse;
import com.cx.client.rest.dto.OSASummaryResults;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by: Dorg.
 * Date: 17/08/2016.
 */
public abstract class CxAbstractPlugin extends AbstractMojo {

    /**
     * The username of the user running the scan.
     */
    @Parameter(required = true, property = "cx.username")
    protected String username;

    /**
     * The password of the user running the scan.
     */
    @Parameter(required = true, property="cx.password")
    protected String password;

    /**
     * Host name of the Checkmarx application.
     */
    @Parameter(defaultValue = "http://localhost", property = "cx.url")
    protected URL url;

    /**
     * The name of the project being scanned.
     */
    @Parameter(defaultValue = "${project.name}", property = "cx.projectName")
    protected String projectName;

    /**
     * The full path describing the team the scan belongs to.
     */
    @Parameter(property = "cx.fullTeamPath", defaultValue = "CxServer")
    protected String fullTeamPath;

    /**
     * Configure this field to scan the project with one of the predefined scan presets, or one of your custom presets.
     */
    @Parameter(defaultValue = "Checkmarx Default", property = "cx.preset")
    protected String preset;

    /**
     * If true, an incremental scan will be performed, meaning - only modified files will be scanned.
     */
    @Parameter(defaultValue = "true", property = "cx.isIncrementalScan")
    protected boolean isIncrementalScan;

    /**
     * Configure this field if you want the scan to skip certain folders.
     */
    @Parameter(property = "cx.folderExclusions")
    protected String folderExclusions;

    /**
     * Configure this field if you want the scan to skip certain files.
     */
    @Parameter(property = "cx.fileExclusions")
    protected String fileExclusions;

    /**
     * If true, a synchronous scan will be performed - the scan will run until finished, and produce a results page.
     * If false, the scan will be asynchronous - the scan will run in the background,
     * and the results will appear, when finished, on the Checkmarx application.
     */
    @Parameter(defaultValue = "true", property = "cx.isSynchronous")
    protected boolean isSynchronous;

    /**
     * If true, a PDF results page will be generated.
     */
    @Parameter(defaultValue = "true", property = "cx.generatePDFReport")
    protected boolean generatePDFReport;

    /**
     * Configure a threshold for the High Severity Vulnerabilities.
     * The scan will not fail if lower sum of High Severity Vulnerabilities is found.
     * Set to -1 to ignore threshold.
     */
    @Parameter(defaultValue = "-1", property = "cx.highSeveritiesThreshold")
    protected int highSeveritiesThreshold;

    /**
     * Configure a threshold for the Medium Severity Vulnerabilities.
     * The scan will not fail if lower sum of Medium Severity Vulnerabilities is found.
     * Set to -1 to ignore threshold.
     */
    @Parameter(defaultValue = "-1", property = "cx.mediumSeveritiesThreshold")
    protected int mediumSeveritiesThreshold;

    /**
     * Configure a threshold for the Low Severity Vulnerabilities.
     * The scan will not fail if lower sum of Low Severity Vulnerabilities is found.
     * Set to -1 to ignore threshold.
     */
    @Parameter(defaultValue = "-1", property = "cx.lowSeveritiesThreshold")
    protected int lowSeveritiesThreshold;

    /**
     * Define a timeout (in minutes) for the scan. If the specified time has passed, the build is failed.
     * Set to 0 to run the scan with no time limit.
     */
    @Parameter(defaultValue = "0", property ="cx.scanTimeoutInMinuets")
    protected int scanTimeoutInMinuets;

    @Parameter(defaultValue = "false", property ="cx.osaEnabled")
    protected boolean osaEnabled;

    @Parameter(property = "cx.osaExclusions")
    protected String[] osaExclusions = new String[0];

    @Parameter(defaultValue = "-1", property = "cx.osaHighSeveritiesThreshold")
    protected int osaHighSeveritiesThreshold;

    @Parameter(defaultValue = "-1", property = "cx.osaMediumSeveritiesThreshold")
    protected int osaMediumSeveritiesThreshold;

    @Parameter(defaultValue = "-1", property = "cx.osaLowSeveritiesThreshold")
    protected int osaLowSeveritiesThreshold;

    @Parameter(defaultValue = "true", property = "cx.osaGeneratePDFReport")
    protected boolean osaGeneratePDFReport;

    @Parameter(defaultValue = "true", property = "cx.osaGenerateHTMLReport")
    protected boolean osaGenerateHTMLReport;


    /**
     * Define an output directory for the scan results.
     */
    @Parameter(defaultValue = "${project.build.directory}\\checkmarx", property = "cx.outputDirectory")
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
    public static final String OSA_REPORT_NAME = "OSA_Report";

    protected CxClientService cxClientService;

    protected String scanResultsUrl;
    protected String projectStateLink;



    private static Logger log = LoggerFactory.getLogger(CxAbstractPlugin.class);


    public void execute() throws MojoExecutionException, MojoFailureException {
        MavenLoggerAdapter.setLogger(getLog());
        ScanResults scanResults;
        OSASummaryResults osaSummaryResults = null;

        if(shouldSkip()) {
            log.info("Project Has No Sources (Reactor), Skipping");
            return;
        }

        try {

            printConfiguration();
            //initialize cx client
            log.debug("Initializing Cx Client");
            cxClientService = new CxClientServiceImpl(url, username, password);

            //perform login to server
            log.info("Logging In to Checkmarx Service.");
            cxClientService.loginToServer();

            //prepare sources to scan (zip them)
            log.info("Zipping Sources");
            zipSources();

            //send sources to scan
            byte[] zippedSources = getBytesFromZippedSources();
            LocalScanConfiguration conf = generateScanConfiguration(zippedSources);
            log.info("Creating Scan");
            CreateScanResponse createScanResponse = cxClientService.createLocalScanResolveFields(conf);
            projectStateLink = CxPluginHelper.composeProjectStateLink(url.toString(), createScanResponse.getProjectId());
            log.info("Scan Created Successfully. Link to Project State: " + projectStateLink);

            CreateOSAScanResponse osaScan = null;
            if(osaEnabled) {
                log.info("creating OSA scan");
                log.info("zipping dependencies");
                File zipForOSA = createZipForOSA();
                log.info("sending OSA scan request");
                osaScan = cxClientService.createOSAScan(createScanResponse.getProjectId(), zipForOSA);
                log.info("OSA scan created successfully");
            }

            if(!isSynchronous) {
                log.info("Running in Asynchronous Mode. Not Waiting for Scan to Finish");
                return;
            }

            //wait for scan to finish
            log.info("Waiting For Scan To Finish.");
            cxClientService.waitForScanToFinish(createScanResponse.getRunId(), scanTimeoutInMinuets, new ConsoleScanWaitHandler());

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

            if(osaEnabled) {
                log.info("Waiting for OSA Scan to Finish");
                cxClientService.waitForOSAScanToFinish(osaScan.getScanId(), -1, new OSAConsoleScanWaitHandler());
                log.info("OSA Scan Finished Successfully");
                log.info("Creating OSA Reports");
                osaSummaryResults = cxClientService.retrieveOSAScanSummaryResults(createScanResponse.getProjectId());
                printOSAResultsToConsole(osaSummaryResults);

                String now = DateFormatUtils.format(new Date(), "dd_MM_yyyy-HH_mm_ss");
                if(osaGeneratePDFReport) {
                    byte[] osaPDF  = cxClientService.retrieveOSAScanPDFResults(createScanResponse.getProjectId());
                    String pdfFileName = OSA_REPORT_NAME + "_" + now + ".pdf";
                    FileUtils.writeByteArrayToFile(new File( outputDirectory, pdfFileName), osaPDF);
                    log.info("OSA PDF Report Can Be Found in: " + outputDirectory +  "\\" + pdfFileName);

                }

                if(osaGenerateHTMLReport) {
                    String osaHtml = cxClientService.retrieveOSAScanHtmlResults(createScanResponse.getProjectId());
                    String htmlFileName = OSA_REPORT_NAME + "_" + now + ".html";
                    FileUtils.writeStringToFile(new File( outputDirectory, htmlFileName), osaHtml, Charset.defaultCharset());
                    log.info("OSA HTML Report Can Be Found in: " + outputDirectory +  "\\" + htmlFileName);
                }
            }



        } catch (CxClientException e) {
            log.debug("Caught Exception: ", e);
            throw new MojoExecutionException(e.getMessage());

        } catch (Exception e) {
            log.debug("Unexpected Exception:", e);
            throw new MojoExecutionException(e.getMessage());
        }

        //assert vulnerabilities under threshold
        assertVulnerabilities(scanResults, osaSummaryResults);
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
        log.info("highSeveritiesThreshold: " + (highSeveritiesThreshold < 0 ? "[No Threshold]" : highSeveritiesThreshold));
        log.info("mediumSeveritiesThreshold: " + (mediumSeveritiesThreshold < 0 ? "[No Threshold]" : mediumSeveritiesThreshold));
        log.info("lowSeveritiesThreshold: " + (lowSeveritiesThreshold < 0 ? "[No Threshold]" : lowSeveritiesThreshold));
        log.info("scanTimeoutInMinuets: " + scanTimeoutInMinuets);
        log.info("outputDirectory: " + outputDirectory);
        log.info("osaEnabled: " + osaEnabled);
        if(osaEnabled) {
            log.info("osaExclusions: " + Arrays.toString(osaExclusions));
            log.info("osaHighSeveritiesThreshold: " + (osaHighSeveritiesThreshold < 0 ? "[No Threshold]" : osaHighSeveritiesThreshold));
            log.info("osaMediumSeveritiesThreshold: " + (osaMediumSeveritiesThreshold < 0 ? "[No Threshold]" : osaMediumSeveritiesThreshold));
            log.info("osaLowSeveritiesThreshold: " + (osaLowSeveritiesThreshold < 0 ? "[No Threshold]" : osaLowSeveritiesThreshold));
            log.info("osaGeneratePDFReport: " + osaGeneratePDFReport);
            log.info("osaGenerateHTMLReport: " + osaGenerateHTMLReport);
        }
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


    private void printOSAResultsToConsole(OSASummaryResults osaSummaryResults) {
        log.info("----------------------------OSA Results:-------------------------------");
        log.info("High Severity Results: " + osaSummaryResults.getHighVulnerabilities());
        log.info("Medium Severity Results: " + osaSummaryResults.getMediumVulnerabilities());
        log.info("Low Severity Results: " + osaSummaryResults.getLowVulnerabilities());
        log.info("Vulnerability Score: " +osaSummaryResults.getVulnerabilityScore());
        log.info("Open Source Libraries: " + osaSummaryResults.getTotalLibraries());
        log.info("Vulnerable And Outdated: " + osaSummaryResults.getVulnerableAndOutdated());
        log.info("Vulnerable And Updated: " + osaSummaryResults.getVulnerableAndUpdated());
        log.info("Non Vulnerable Libraries: " + osaSummaryResults.getNonVulnerableLibraries());
        log.info("OSA Scan Results Can Be Found at: " + projectStateLink.replace("Summary", "OSA"));
        log.info("------------------------------------------------------------------------");
    }



    protected void generateHTMLReport(long scanId){

        try {
            byte[] scanReport = cxClientService.getScanReport(scanId, ReportType.CSV);
            String csv = new String(scanReport);

            String now = DateFormatUtils.format(new Date(), "dd_MM_yyyy-HH_mm_ss");
            String htmlFileName = "report" + "_" + now + ".html";
            File htmlReportFile = new File(outputDirectory + "\\" + htmlFileName);

            String html = CxPluginHelper.compileHtmlReport(csv, highSeveritiesThreshold, mediumSeveritiesThreshold, lowSeveritiesThreshold);

            if(html != null) {
                FileUtils.writeStringToFile(htmlReportFile, html, Charset.defaultCharset());
                log.info("HTML Report Can Be Found in: " + outputDirectory +  "\\" + htmlFileName);
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

    private void assertVulnerabilities(ScanResults scanResults, OSASummaryResults osaSummaryResults) throws MojoFailureException {

        StringBuilder res = new StringBuilder("\n");
        boolean fail = false;
        if(highSeveritiesThreshold >= 0 && scanResults.getHighSeverityResults() > highSeveritiesThreshold) {
            res.append("High Severity Results are Above Threshold. Results: ").append(scanResults.getHighSeverityResults()).append(". Threshold: ").append(highSeveritiesThreshold).append("\n");
            fail = true;
        }

        if(mediumSeveritiesThreshold >= 0 && scanResults.getMediumSeverityResults() > mediumSeveritiesThreshold) {
            res.append("Medium Severity Results are Above Threshold. Results: ").append(scanResults.getMediumSeverityResults()).append(". Threshold: ").append(mediumSeveritiesThreshold).append("\n");
            fail = true;

        }

        if(lowSeveritiesThreshold >= 0 && scanResults.getLowSeverityResults() > lowSeveritiesThreshold) {
            res.append("Low Severity Results are Above Threshold. Results: ").append(scanResults.getLowSeverityResults()).append(". Threshold: ").append(lowSeveritiesThreshold).append("\n");
            fail = true;
        }

        if(osaEnabled && osaSummaryResults != null) {
            if(osaHighSeveritiesThreshold >= 0 && osaSummaryResults.getHighVulnerabilities() > osaHighSeveritiesThreshold) {
                res.append("OSA High Severity Results are Above Threshold. Results: ").append(osaSummaryResults.getHighVulnerabilities()).append(". Threshold: ").append(osaHighSeveritiesThreshold).append("\n");
                fail = true;
            }

            if(osaMediumSeveritiesThreshold >= 0 && osaSummaryResults.getMediumVulnerabilities() > osaMediumSeveritiesThreshold) {
                res.append("OSA Medium Severity Results are Above Threshold. Results: ").append(osaSummaryResults.getMediumVulnerabilities()).append(". Threshold: ").append(osaMediumSeveritiesThreshold).append("\n");
                fail = true;
            }

            if(osaLowSeveritiesThreshold >= 0 && osaSummaryResults.getLowVulnerabilities() > osaLowSeveritiesThreshold) {
                res.append("OSA Low Severity Results are Above Threshold. Results: ").append(osaSummaryResults.getLowVulnerabilities()).append(". Threshold: ").append(osaLowSeveritiesThreshold).append("\n");
                fail = true;
            }
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
            String pdfFileName = PDF_REPORT_NAME + "_" + now + ".pdf";
            FileUtils.writeByteArrayToFile(new File( outputDirectory, pdfFileName), scanReport);
            log.info("PDF Report Can Be Found in: " + outputDirectory +  "\\" + pdfFileName);
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


    private File createZipForOSA() throws MojoExecutionException {

        Set artifacts = project.getArtifacts();
        for (Object arti :artifacts) {
            Artifact a = (Artifact)arti;
            if(!isExcludedFromOSA(a)) {
                zipArchiver.addFile(a.getFile(), a.getFile().getName());
            }
        }

        File ret = new File(outputDirectory, OSA_ZIP_NAME + ".zip");
        zipArchiver.setDestFile(ret);
        try {
            zipArchiver.createArchive();
            log.info("Files for OSA scan zipped at: " + outputDirectory + "\\" +OSA_ZIP_NAME + ".zip");
        } catch (IOException e) {
            throw new MojoExecutionException("Fail to zip files for OSA scan:", e);
        }

        return ret;

    }

    private boolean isExcludedFromOSA(Artifact a) {
        for (String s : osaExclusions) {
            if(s.equals(a.getArtifactId())) {
                return true;
            }
        }
        return false;
    }


}
