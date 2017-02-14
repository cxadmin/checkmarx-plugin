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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.MavenLoggerAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * The 'scan' goal creates a single comprehensive scan, including all the modules in the reactor.
 */
@Mojo(name = "scan", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST, inheritByDefault = false)
public class CxScanPlugin extends AbstractMojo {

    private static Logger log = LoggerFactory.getLogger(CxScanPlugin.class);

    public static final String SOURCES_ZIP_NAME = "sources";
    public static final String OSA_ZIP_NAME = "OSAScan";
    public static final String PDF_REPORT_NAME = "CxReport";
    public static final String OSA_REPORT_NAME = "OSA_Report";
    /**
     * The username of the user running the scan.
     */
    @Parameter(required = true, property = "cx.username")
    private String username;

    /**
     * The password of the user running the scan.
     */
    @Parameter(required = true, property = "cx.password")
    private String password;

    /**
     * Host name of the Checkmarx application.
     */
    @Parameter(defaultValue = "http://localhost", property = "cx.url")
    private URL url;

    /**
     * The name of the project being scanned.
     */
    @Parameter(defaultValue = "${project.name}", property = "cx.projectName")
    private String projectName;

    /**
     * The full path describing the team the scan belongs to.
     */
    @Parameter(property = "cx.fullTeamPath", defaultValue = "CxServer")
    private String fullTeamPath;

    /**
     * Configure this field to scan the project with one of the predefined scan presets, or one of your custom presets.
     */
    @Parameter(defaultValue = "Checkmarx Default", property = "cx.preset")
    private String preset;

    /**
     * If true, an incremental scan will be performed, meaning - only modified files will be scanned.
     */
    @Parameter(defaultValue = "true", property = "cx.isIncrementalScan")
    private boolean isIncrementalScan;

    /**
     * List of folders and\or folder patterns which the scan will ignore
     */
    @Parameter(property = "cx.folderExclusions")
    private String[] folderExclusions = new String[0];

    /**
     * List of files and\or file patterns which the scan will ignore.
     */
    @Parameter(property = "cx.fileExclusions")
    private String[] fileExclusions = new String[0];

    /**
     * If true, the build will wait for the scan to end and display the results.
     * If false, the build will trigger the scan without waiting for the scan to end and the results will not be displayed
     */
    @Parameter(defaultValue = "true", property = "cx.isSynchronous")
    private boolean isSynchronous;

    /**
     * If true, a PDF report will be generated in the output directory.
     */
    @Parameter(defaultValue = "true", property = "cx.generatePDFReport")
    private boolean generatePDFReport;

    /**
     * Configure a threshold for the High Severity Vulnerabilities.
     * The build will fail if the sum of High Severity Vulnerabilities is larger than the threshold.
     * Leave empty to ignore threshold.
     */
    @Parameter(defaultValue = "-1", property = "cx.highSeveritiesThreshold")
    private int highSeveritiesThreshold;

    /**
     * Configure a threshold for the Medium Severity Vulnerabilities.
     * The build will fail if the sum of Medium Severity Vulnerabilities is larger than the threshold.
     * Leave empty to ignore threshold.
     */
    @Parameter(defaultValue = "-1", property = "cx.mediumSeveritiesThreshold")
    private int mediumSeveritiesThreshold;

    /**
     * Configure a threshold for the Low Severity Vulnerabilities.
     * The build will fail if the sum of Low Severity Vulnerabilities is larger than the threshold.
     * Leave empty to ignore threshold.
     */
    @Parameter(defaultValue = "-1", property = "cx.lowSeveritiesThreshold")
    private int lowSeveritiesThreshold;

    /**
     * Define a timeout (in minutes) for the scan. If the specified time has passed, the build fails.
     * Set to 0 to run the scan with no time limit.
     */
    @Parameter(defaultValue = "0", property = "cx.scanTimeoutInMinutes")
    private int scanTimeoutInMinutes;

    /**
     * If true, CxOSA will be enabled
     */
    @Parameter(defaultValue = "false", property = "cx.osaEnabled")
    private boolean osaEnabled;

    /**
     * List of Maven dependencies that will not be included in CxOSA.
     * An exclusion should be of the form: groupId.artifactId
     */
    @Parameter(property = "cx.osaExclusions")
    private String[] osaExclusions = new String[0];

    /**
     * Configure a threshold for the CxOSA High Severity Vulnerabilities.
     * The build will fail if the sum of High Severity Vulnerabilities is larger than the threshold.
     * Leave empty to ignore threshold.
     */
    @Parameter(defaultValue = "-1", property = "cx.osaHighSeveritiesThreshold")
    private int osaHighSeveritiesThreshold;

    /**
     * Configure a threshold for the CxOSA Medium Severity Vulnerabilities.
     * The build will fail if the sum of Medium Severity Vulnerabilities is larger than the threshold.
     * Leave empty to ignore threshold.
     */
    @Parameter(defaultValue = "-1", property = "cx.osaMediumSeveritiesThreshold")
    private int osaMediumSeveritiesThreshold;

    /**
     * Configure a threshold for the CxOSA Low Severity Vulnerabilities.
     * The build will fail if the sum of Low Severity Vulnerabilities is larger than the threshold.
     * Leave empty to ignore threshold.
     */
    @Parameter(defaultValue = "-1", property = "cx.osaLowSeveritiesThreshold")
    private int osaLowSeveritiesThreshold;

    /**
     * If true, a CxOSA PDF report will be generated in the output directory.
     */
    @Parameter(defaultValue = "true", property = "cx.osaGeneratePDFReport")
    private boolean osaGeneratePDFReport;

    /**
     * If true, a CxOSA HTML report will be generated in the output directory.
     */
    @Parameter(defaultValue = "true", property = "cx.osaGenerateHTMLReport")
    private boolean osaGenerateHTMLReport;

    /**
     * Define an output directory for the scan reports.
     */
    @Parameter(defaultValue = "${project.build.directory}/checkmarx", property = "cx.outputDirectory")
    private File outputDirectory;

    /**
     * Disables certificate verification.
     */
    @Parameter(defaultValue = "false", property = "cx.disableCertificateVerification")
    private boolean disableCertificateVerification;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;
    @Component(role = Archiver.class, hint = "zip")
    private ZipArchiver zipArchiver;
    private CxClientService cxClientService;
    private String scanResultsUrl;
    private String projectStateLink;

    public void execute() throws MojoExecutionException, MojoFailureException {
        MavenLoggerAdapter.setLogger(getLog());
        ScanResults scanResults = null;
        OSASummaryResults osaSummaryResults = null;

        Exception osaCreateException = null;
        Exception scanWaitException = null;

        try {

            printConfiguration();
            //initialize cx client
            log.debug("Initializing Cx Client");
            cxClientService = new CxClientServiceImpl(url, username, password);

            if(disableCertificateVerification) {
                cxClientService.disableSSLCertificateVerification();
            }

            cxClientService.checkServerConnectivity();

            //perform login to server
            log.info("Logging In to Checkmarx Service.");
            cxClientService.loginToServer();

            //prepare sources to scan (zip them)
            log.info("Zipping Sources");
            zipSources(reactorProjects);

            //send sources to scan
            byte[] zippedSources = getBytesFromZippedSources();
            LocalScanConfiguration conf = generateScanConfiguration(zippedSources);
            log.info("Creating Scan");
            CreateScanResponse createScanResponse = cxClientService.createLocalScanResolveFields(conf);
            projectStateLink = CxPluginHelper.composeProjectStateLink(url.toString(), createScanResponse.getProjectId());
            log.info("Scan Created Successfully. Link to Project State: " + projectStateLink);

            CreateOSAScanResponse osaScan = null;
            if (osaEnabled) {
                try {
                    log.info("creating OSA scan");
                    log.info("zipping dependencies");
                    File zipForOSA = createZipForOSA();
                    log.info("sending OSA scan request");
                    osaScan = cxClientService.createOSAScan(createScanResponse.getProjectId(), zipForOSA);
                    log.info("OSA scan created successfully");
                } catch (Exception e) {
                    log.warn("fail to create OSA Scan: " + e.getMessage());
                    osaCreateException = e;
                }

            }

            if (!isSynchronous) {
                if (osaCreateException != null) {
                    throw osaCreateException;
                }
                log.info("Running in Asynchronous Mode. Not Waiting for Scan to Finish");
                return;
            }

            //wait for sast scan to finish
            try {
                log.info("Waiting For Scan To Finish.");
                cxClientService.waitForScanToFinish(createScanResponse.getRunId(), scanTimeoutInMinutes, new ConsoleScanWaitHandler());

                log.info("Scan Finished. Retrieving Scan Results");
                scanResults = cxClientService.retrieveScanResults(createScanResponse.getProjectId());

                scanResultsUrl = CxPluginHelper.composeScanLink(url.toString(), scanResults);

                printResultsToConsole(scanResults);

    /*            log.info("Generating HTML Report");
                generateHTMLReport(scanResults.getScanID());*/

                //create scan report
                if (generatePDFReport) {
                    createPDFReport(scanResults.getScanID());
                }

            } catch (Exception e) {
                log.error("fail to perform scan: " + e.getMessage());
                log.debug("", e);
                scanWaitException = e;
            }

            if (osaEnabled) {

                if (osaCreateException != null) {
                    throw osaCreateException;
                }


                log.info("Waiting for OSA Scan to Finish");
                cxClientService.waitForOSAScanToFinish(osaScan.getScanId(), -1, new OSAConsoleScanWaitHandler());
                log.info("OSA Scan Finished Successfully");
                log.info("Creating OSA Reports");
                osaSummaryResults = cxClientService.retrieveOSAScanSummaryResults(createScanResponse.getProjectId());
                printOSAResultsToConsole(osaSummaryResults);

                String now = DateFormatUtils.format(new Date(), "dd_MM_yyyy-HH_mm_ss");
                if (osaGeneratePDFReport) {
                    byte[] osaPDF = cxClientService.retrieveOSAScanPDFResults(createScanResponse.getProjectId());
                    String pdfFileName = OSA_REPORT_NAME + "_" + now + ".pdf";
                    FileUtils.writeByteArrayToFile(new File(outputDirectory, pdfFileName), osaPDF);
                    log.info("OSA PDF Report Can Be Found in: " + outputDirectory + File.separator + pdfFileName);
                }

                if (osaGenerateHTMLReport) {
                    String osaHtml = cxClientService.retrieveOSAScanHtmlResults(createScanResponse.getProjectId());
                    String htmlFileName = OSA_REPORT_NAME + "_" + now + ".html";
                    FileUtils.writeStringToFile(new File(outputDirectory, htmlFileName), osaHtml, Charset.defaultCharset());
                    log.info("OSA HTML Report Can Be Found in: " + outputDirectory + File.separator + htmlFileName);
                }

            }

            if (scanWaitException != null) {
                throw scanWaitException;
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
        log.info("folderExclusions: " + Arrays.toString(folderExclusions));
        log.info("fileExclusions: " + Arrays.toString(fileExclusions));
        log.info("isSynchronous: " + isSynchronous);
        log.info("generatePDFReport: " + generatePDFReport);
        log.info("highSeveritiesThreshold: " + (highSeveritiesThreshold < 0 ? "[No Threshold]" : highSeveritiesThreshold));
        log.info("mediumSeveritiesThreshold: " + (mediumSeveritiesThreshold < 0 ? "[No Threshold]" : mediumSeveritiesThreshold));
        log.info("lowSeveritiesThreshold: " + (lowSeveritiesThreshold < 0 ? "[No Threshold]" : lowSeveritiesThreshold));
        log.info("scanTimeoutInMinutes: " + scanTimeoutInMinutes);
        log.info("outputDirectory: " + outputDirectory);
        log.info("osaEnabled: " + osaEnabled);
        if (osaEnabled) {
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
        log.info("----------------------------Checkmarx Scan Results(CxSAST):-------------------------------");
        log.info("High Severity Results: " + scanResults.getHighSeverityResults());
        log.info("Medium Severity Results: " + scanResults.getMediumSeverityResults());
        log.info("Low Severity Results: " + scanResults.getLowSeverityResults());
        log.info("Info Severity Results: " + scanResults.getInfoSeverityResults());
        log.info("Scan Results Can Be Found at: " + scanResultsUrl);
        log.info("------------------------------------------------------------------------");
    }


    private void printOSAResultsToConsole(OSASummaryResults osaSummaryResults) {
        log.info("----------------------------Checkmarx Scan Results(CxOSA):-------------------------------");
        log.info("");
        log.info("------------------------");
        log.info("Vulnerabilities Summary:");
        log.info("------------------------");
        log.info("OSA High Severity Results: " + osaSummaryResults.getHighVulnerabilities());
        log.info("OSA Medium Severity Results: " + osaSummaryResults.getMediumVulnerabilities());
        log.info("OSA Low Severity Results: " + osaSummaryResults.getLowVulnerabilities());
        log.info("Vulnerability Score: " + osaSummaryResults.getVulnerabilityScore());
        log.info("");
        log.info("-----------------------");
        log.info("Libraries Scan Results:");
        log.info("-----------------------");
        log.info("Open Source Libraries: " + osaSummaryResults.getTotalLibraries());
        log.info("Vulnerable And Outdated: " + osaSummaryResults.getVulnerableAndOutdated());
        log.info("Vulnerable And Updated: " + osaSummaryResults.getVulnerableAndUpdated());
        log.info("Non Vulnerable Libraries: " + osaSummaryResults.getNonVulnerableLibraries());
        log.info("");
        log.info("OSA Scan Results Can Be Found at: " + projectStateLink.replace("Summary", "OSA"));
        log.info("------------------------------------------------------------------------");
    }


    private void generateHTMLReport(long scanId) {

        try {
            byte[] scanReport = cxClientService.getScanReport(scanId, ReportType.CSV);
            String csv = new String(scanReport);

            String now = DateFormatUtils.format(new Date(), "dd_MM_yyyy-HH_mm_ss");
            String htmlFileName = "report" + "_" + now + ".html";
            File htmlReportFile = new File(outputDirectory + File.separator + htmlFileName);

            String html = CxPluginHelper.compileHtmlReport(csv, highSeveritiesThreshold, mediumSeveritiesThreshold, lowSeveritiesThreshold);

            if (html != null) {
                FileUtils.writeStringToFile(htmlReportFile, html, Charset.defaultCharset());
                log.info("HTML Report Can Be Found in: " + outputDirectory + File.separator+ htmlFileName);
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
        ret.setFileExclusions(CxPluginHelper.convertArrayToString(fileExclusions));
        ret.setFolderExclusions(CxPluginHelper.convertArrayToString(folderExclusions));
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
        if (highSeveritiesThreshold >= 0 && scanResults.getHighSeverityResults() > highSeveritiesThreshold) {
            res.append("High Severity Results are Above Threshold. Results: ").append(scanResults.getHighSeverityResults()).append(". Threshold: ").append(highSeveritiesThreshold).append("\n");
            fail = true;
        }

        if (mediumSeveritiesThreshold >= 0 && scanResults.getMediumSeverityResults() > mediumSeveritiesThreshold) {
            res.append("Medium Severity Results are Above Threshold. Results: ").append(scanResults.getMediumSeverityResults()).append(". Threshold: ").append(mediumSeveritiesThreshold).append("\n");
            fail = true;

        }

        if (lowSeveritiesThreshold >= 0 && scanResults.getLowSeverityResults() > lowSeveritiesThreshold) {
            res.append("Low Severity Results are Above Threshold. Results: ").append(scanResults.getLowSeverityResults()).append(". Threshold: ").append(lowSeveritiesThreshold).append("\n");
            fail = true;
        }

        if (osaEnabled && osaSummaryResults != null) {
            if (osaHighSeveritiesThreshold >= 0 && osaSummaryResults.getHighVulnerabilities() > osaHighSeveritiesThreshold) {
                res.append("OSA High Severity Results are Above Threshold. Results: ").append(osaSummaryResults.getHighVulnerabilities()).append(". Threshold: ").append(osaHighSeveritiesThreshold).append("\n");
                fail = true;
            }

            if (osaMediumSeveritiesThreshold >= 0 && osaSummaryResults.getMediumVulnerabilities() > osaMediumSeveritiesThreshold) {
                res.append("OSA Medium Severity Results are Above Threshold. Results: ").append(osaSummaryResults.getMediumVulnerabilities()).append(". Threshold: ").append(osaMediumSeveritiesThreshold).append("\n");
                fail = true;
            }

            if (osaLowSeveritiesThreshold >= 0 && osaSummaryResults.getLowVulnerabilities() > osaLowSeveritiesThreshold) {
                res.append("OSA Low Severity Results are Above Threshold. Results: ").append(osaSummaryResults.getLowVulnerabilities()).append(". Threshold: ").append(osaLowSeveritiesThreshold).append("\n");
                fail = true;
            }
        }

        if (fail) {
            throw new MojoFailureException(res.toString());
        }
    }

    private byte[] getBytesFromZippedSources() throws MojoExecutionException {

        log.debug("Converting Zipped Sources to Byte Array");
        byte[] zipFileByte;
        try {
            InputStream fileStream = new FileInputStream(new File(outputDirectory, SOURCES_ZIP_NAME + ".zip"));
            zipFileByte = IOUtils.toByteArray(fileStream);
        } catch (Exception e) {
            throw new MojoExecutionException("Fail to Set Zipped File Into Project: " + e.getMessage(), e);
        }

        return zipFileByte;
    }


    private void createPDFReport(long scanId) {
        log.info("Generating PDF Report");
        byte[] scanReport;
        try {
            scanReport = cxClientService.getScanReport(scanId, ReportType.PDF);
            String now = DateFormatUtils.format(new Date(), "dd_MM_yyyy-HH_mm_ss");
            String pdfFileName = PDF_REPORT_NAME + "_" + now + ".pdf";
            FileUtils.writeByteArrayToFile(new File(outputDirectory, pdfFileName), scanReport);
            log.info("PDF Report Can Be Found in: " + outputDirectory + File.separator + pdfFileName);
        } catch (Exception e) {
            log.warn("Fail to Generate PDF Report");
            log.debug("Fail to Generate PDF Report", e);
        }


    }

    private void zipSources(List<MavenProject> projects) throws MojoExecutionException {

        for (MavenProject p : projects) {

            MavenProject subProject = getProject(p);
            if ("pom".equals(subProject.getPackaging())) {
                continue;
            }

            String prefix = subProject.getName() + "\\";

            //add sources
            List compileSourceRoots = subProject.getCompileSourceRoots();
            for (Object c : compileSourceRoots) {

                File sourceDir = new File((String) c);
                if (sourceDir.exists()) {
                    zipArchiver.addDirectory(sourceDir, prefix);
                }
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
        }


        zipArchiver.setDestFile(new File(outputDirectory, SOURCES_ZIP_NAME + ".zip"));
        try {
            zipArchiver.createArchive();
            log.info("Sources Zipped at: " + outputDirectory + File.separator + SOURCES_ZIP_NAME + ".zip");
        } catch (IOException e) {
            throw new MojoExecutionException("Fail to Create Zip Sources: ", e);
        }

    }


    private MavenProject getProject(MavenProject p) {
        if (p.getExecutionProject() != null) {
            return p.getExecutionProject();
        }

        return p;
    }


    private File createZipForOSA() throws MojoExecutionException {

        Set artifacts = project.getArtifacts();
        for (Object arti : artifacts) {
            Artifact a = (Artifact) arti;
            if (!isExcludedFromOSA(a)) {
                zipArchiver.addFile(a.getFile(), a.getGroupId() + "." + a.getFile().getName());
            }
        }

        File ret = new File(outputDirectory, OSA_ZIP_NAME + ".zip");
        zipArchiver.setDestFile(ret);
        try {
            if (zipArchiver.getFiles().isEmpty()) {
                log.info("no dependencies found to zip.");
                CxPluginHelper.createEmptyZip(ret);
            } else {
                zipArchiver.createArchive();
                log.info("Files for OSA scan zipped at: " + outputDirectory + File.separator + OSA_ZIP_NAME + ".zip");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Fail to zip files for OSA scan: " + e.getMessage(), e);
        }

        return ret;

    }

    private boolean isExcludedFromOSA(Artifact a) {
        for (String exclusion : osaExclusions) {
            if ((a.getGroupId() + '.' + a.getArtifactId()).equals(exclusion)) {
                return true;
            }
        }
        return false;
    }


}
