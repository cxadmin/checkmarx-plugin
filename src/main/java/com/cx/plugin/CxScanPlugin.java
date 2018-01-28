package com.cx.plugin;

import com.cx.client.*;
import com.cx.client.dto.*;
import com.cx.client.exception.CxClientException;
import com.cx.client.rest.dto.CVE;
import com.cx.client.rest.dto.CreateOSAScanResponse;
import com.cx.client.rest.dto.Library;
import com.cx.client.rest.dto.OSASummaryResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
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
import java.util.*;


/**
 * The 'scan' goal creates a single comprehensive scan, including all the modules in the reactor.
 */
@Mojo(name = "scan", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST, inheritByDefault = false)
public class CxScanPlugin extends AbstractMojo {

    private static Logger log = LoggerFactory.getLogger(CxScanPlugin.class);

    public static final String SOURCES_ZIP_NAME = "sources";
    public static final String PDF_REPORT_NAME = "CxReport";
    public static final String OSA_LIBRARIES_NAME = "OSALibraries";
    public static final String OSA_VULNERABILITIES_NAME = "OSAVulnerabilities";
    public static final String OSA_SUMMARY_NAME = "OSASummary";
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
     * If true, a CxOSA Json reports will be generated in the output directory.
     */
    @Parameter(defaultValue = "true", property = "cx.osaGenerateJsonReport")
    private boolean osaGenerateJsonReport;

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
    private ObjectMapper objectMapper = new ObjectMapper();
    private String pluginVersion;

    private String scanResultsUrl;
    private String projectStateLink;

    public void execute() throws MojoExecutionException, MojoFailureException {
        MavenLoggerAdapter.setLogger(getLog());
        printLogo();
        ScanResults scanResults = null;
        OSASummaryResults osaSummaryResults = null;

        Exception osaCreateException = null;
        Exception scanWaitException = null;

        try {

            PluginDescriptor pd = (PluginDescriptor)getPluginContext().get("pluginDescriptor");
            if(pd != null) {
                pluginVersion = pd.getVersion();
            }
            printConfiguration();
            //initialize cx client
            log.debug("Initializing Cx client");
            cxClientService = new CxClientServiceImpl(url, username, password);

            if(disableCertificateVerification) {
                cxClientService.disableSSLCertificateVerification();
            }

            cxClientService.checkServerConnectivity();

            //perform login to server
            log.info("Logging into Checkmarx service.");
            cxClientService.loginToServer();

            //prepare sources to scan (zip them)
            log.info("Zipping sources");
            zipSources(reactorProjects);

            //send sources to scan
            byte[] zippedSources = getBytesFromZippedSources();
            LocalScanConfiguration conf = generateScanConfiguration(zippedSources);
            log.info("Creating scan");
            CreateScanResponse createScanResponse = cxClientService.createLocalScanResolveFields(conf);
            projectStateLink = CxPluginHelper.composeProjectStateLink(url.toString(), createScanResponse.getProjectId());
            log.info("Scan created successfully. Link to project state: " + projectStateLink);

            CreateOSAScanResponse osaScan = null;
            if (osaEnabled) {
                try {
                    log.info("Creating OSA scan");
                    log.info("Scanning for dependencies");
                    List<OSAFile> osaSha1s = createOSASha1s();
                    log.info("Sending OSA scan request");
                    osaScan = cxClientService.createOSAScan(createScanResponse.getProjectId(), osaSha1s);
                    log.info("OSA scan created successfully");
                } catch (Exception e) {
                    log.warn(e.getMessage());
                    osaCreateException = e;
                }

            }

            if (!isSynchronous) {
                if (osaCreateException != null) {
                    throw osaCreateException;
                }
                log.info("Running in asynchronous mode. Scan continues running on the server");
                return;
            }

            //wait for sast scan to finish
            try {
                log.info("Waiting for scan to finish.");
                cxClientService.waitForScanToFinish(createScanResponse.getRunId(), scanTimeoutInMinutes, new ConsoleScanWaitHandler());

                log.info("Scan finished. Retrieving scan results");
                scanResults = cxClientService.retrieveScanResults(createScanResponse.getProjectId());

                scanResultsUrl = CxPluginHelper.composeScanLink(url.toString(), scanResults);

                printResultsToConsole(scanResults);

                //create scan report
                if (generatePDFReport) {
                    createPDFReport(scanResults.getScanID());
                }

            } catch (Exception e) {
                log.error("Failed to perform scan: " + e.getMessage());
                log.debug("", e);
                scanWaitException = e;
            }

            if (osaEnabled) {

                if (osaCreateException != null) {
                    throw osaCreateException;
                }


                log.info("Waiting for OSA scan to finish");
                cxClientService.waitForOSAScanToFinish(osaScan.getScanId(), -1, new OSAConsoleScanWaitHandler());
                log.info("OSA scan finished successfully");
                log.info("Creating OSA reports");
                osaSummaryResults = cxClientService.retrieveOSAScanSummaryResults(osaScan.getScanId());
                printOSAResultsToConsole(osaSummaryResults, createScanResponse.getProjectId());

                String now = DateFormatUtils.format(new Date(), "dd_MM_yyyy-HH_mm_ss");

                if(osaGenerateJsonReport) {
                    //create json files
                    String fileName =  OSA_SUMMARY_NAME + "_" + now + ".json";
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputDirectory, fileName), osaSummaryResults);
                    log.info("OSA summary json location: " + outputDirectory + File.separator + fileName);

                    List<Library> libraries = cxClientService.getOSALibraries(osaScan.getScanId());
                    fileName = OSA_LIBRARIES_NAME + "_" + now + ".json";
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputDirectory, fileName), libraries);
                    log.info("OSA libraries json location: " + outputDirectory + File.separator + fileName);

                    List<CVE> osaVulnerabilities = cxClientService.getOSAVulnerabilities(osaScan.getScanId());
                    fileName =  OSA_VULNERABILITIES_NAME + "_" + now + ".json";
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputDirectory, fileName), osaVulnerabilities);
                    log.info("OSA vulnerabilities json location: " + outputDirectory + File.separator + fileName);
                }

            }

            if (scanWaitException != null) {
                throw scanWaitException;
            }


        } catch (CxClientException e) {
            log.debug("Caught exception: ", e);
            throw new MojoExecutionException(e.getMessage());

        } catch (Exception e) {
            log.debug("Unexpected exception:", e);
            throw new MojoExecutionException(e.getMessage());
        }

        //assert vulnerabilities under threshold
        assertVulnerabilities(scanResults, osaSummaryResults);
    }

    private void printConfiguration() {
        log.info("----------------------------Configurations:-----------------------------");
        log.info("plugin version: " + pluginVersion);
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
            log.info("osaGenerateJsonReport: " + osaGenerateJsonReport);
        }
        log.info("------------------------------------------------------------------------");

    }

    private void printResultsToConsole(ScanResults scanResults) {
        log.info("----------------------------Checkmarx Scan Results(CxSAST):-------------------------------");
        log.info("High severity results: " + scanResults.getHighSeverityResults());
        log.info("Medium severity results: " + scanResults.getMediumSeverityResults());
        log.info("Low severity results: " + scanResults.getLowSeverityResults());
        log.info("Info severity results: " + scanResults.getInfoSeverityResults());
        log.info("Scan results location: " + scanResultsUrl);
        log.info("------------------------------------------------------------------------");
    }


    private void printOSAResultsToConsole(OSASummaryResults osaSummaryResults, long projectId) {
        log.info("----------------------------Checkmarx Scan Results(CxOSA):-------------------------------");
        log.info("");
        log.info("------------------------");
        log.info("Vulnerabilities Summary:");
        log.info("------------------------");
        log.info("OSA high severity results: " + osaSummaryResults.getTotalHighVulnerabilities());
        log.info("OSA medium severity results: " + osaSummaryResults.getTotalMediumVulnerabilities());
        log.info("OSA low severity results: " + osaSummaryResults.getTotalLowVulnerabilities());
        log.info("Vulnerability score: " + osaSummaryResults.getVulnerabilityScore());
        log.info("");
        log.info("-----------------------");
        log.info("Libraries Scan Result:");
        log.info("-----------------------");
        log.info("Open-source libraries: " + osaSummaryResults.getTotalLibraries());
        log.info("Vulnerable and outdated: " + osaSummaryResults.getVulnerableAndOutdated());
        log.info("Vulnerable and updated: " + osaSummaryResults.getVulnerableAndUpdated());
        log.info("Non-vulnerable libraries: " + osaSummaryResults.getNonVulnerableLibraries());
        log.info("");
        log.info("OSA scan results location: " + url + "/CxWebClient/SPA/#/viewer/project/" + projectId);
        log.info("------------------------------------------------------------------------");
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
            res.append("High severity results are above threshold. Results: ").append(scanResults.getHighSeverityResults()).append(". Threshold: ").append(highSeveritiesThreshold).append("\n");
            fail = true;
        }

        if (mediumSeveritiesThreshold >= 0 && scanResults.getMediumSeverityResults() > mediumSeveritiesThreshold) {
            res.append("Medium severity results are above threshold. Results: ").append(scanResults.getMediumSeverityResults()).append(". Threshold: ").append(mediumSeveritiesThreshold).append("\n");
            fail = true;

        }

        if (lowSeveritiesThreshold >= 0 && scanResults.getLowSeverityResults() > lowSeveritiesThreshold) {
            res.append("Low severity results are above threshold. Results: ").append(scanResults.getLowSeverityResults()).append(". Threshold: ").append(lowSeveritiesThreshold).append("\n");
            fail = true;
        }

        if (osaEnabled && osaSummaryResults != null) {
            if (osaHighSeveritiesThreshold >= 0 && osaSummaryResults.getTotalHighVulnerabilities() > osaHighSeveritiesThreshold) {
                res.append("OSA high severity results are above threshold. Results: ").append(osaSummaryResults.getTotalHighVulnerabilities()).append(". Threshold: ").append(osaHighSeveritiesThreshold).append("\n");
                fail = true;
            }

            if (osaMediumSeveritiesThreshold >= 0 && osaSummaryResults.getTotalMediumVulnerabilities() > osaMediumSeveritiesThreshold) {
                res.append("OSA medium severity results are above threshold. Results: ").append(osaSummaryResults.getTotalMediumVulnerabilities()).append(". Threshold: ").append(osaMediumSeveritiesThreshold).append("\n");
                fail = true;
            }

            if (osaLowSeveritiesThreshold >= 0 && osaSummaryResults.getTotalLowVulnerabilities() > osaLowSeveritiesThreshold) {
                res.append("OSA low severity results are above threshold. Results: ").append(osaSummaryResults.getTotalLowVulnerabilities()).append(". Threshold: ").append(osaLowSeveritiesThreshold).append("\n");
                fail = true;
            }
        }

        if (fail) {
            throw new MojoFailureException(res.toString());
        }
    }

    private byte[] getBytesFromZippedSources() throws MojoExecutionException {

        log.debug("Converting zipped sources to byte array");
        byte[] zipFileByte;
        try {
            InputStream fileStream = new FileInputStream(new File(outputDirectory, SOURCES_ZIP_NAME + ".zip"));
            zipFileByte = IOUtils.toByteArray(fileStream);
        } catch (Exception e) {
            throw new MojoExecutionException("Fail to set zipped file into project: " + e.getMessage(), e);
        }

        return zipFileByte;
    }


    private void createPDFReport(long scanId) {
        log.info("Generating PDF report");
        byte[] scanReport;
        try {
            scanReport = cxClientService.getScanReport(scanId, ReportType.PDF);
            String now = DateFormatUtils.format(new Date(), "dd_MM_yyyy-HH_mm_ss");
            String pdfFileName = PDF_REPORT_NAME + "_" + now + ".pdf";
            FileUtils.writeByteArrayToFile(new File(outputDirectory, pdfFileName), scanReport);
            log.info("PDF report location: " + outputDirectory + File.separator + pdfFileName);
        } catch (Exception e) {
            log.warn("Failed to generate PDF report");
            log.debug("Failed to generate PDF report", e);
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
            log.info("Sources zip location: " + outputDirectory + File.separator + SOURCES_ZIP_NAME + ".zip");
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to zip sources: ", e);
        }

    }


    private MavenProject getProject(MavenProject p) {
        if (p.getExecutionProject() != null) {
            return p.getExecutionProject();
        }

        return p;
    }


    private List<OSAFile> createOSASha1s() throws MojoExecutionException {

        List<OSAFile> ret = new ArrayList<OSAFile>();

        Set artifacts = project.getArtifacts();
        for (Object arti : artifacts) {
            Artifact a = (Artifact) arti;
            if (!isExcludedFromOSA(a)) {
                addSha1(a.getFile(), ret);
            }
        }
        return ret;
    }

    private void addSha1(File file, List<OSAFile> ret) {
        try {
            String sha1 = DigestUtils.shaHex(FileUtils.readFileToByteArray(file));
            ret.add(new OSAFile(file.getName(), sha1));
        } catch (IOException e) {
            log.warn("Failed to calculate sha1 for file: ["+file.getName()+"]: " + e.getMessage());
        }
    }

    private boolean isExcludedFromOSA(Artifact a) {
        for (String exclusion : osaExclusions) {
            if ((a.getGroupId() + '.' + a.getArtifactId()).equals(exclusion)) {
                return true;
            }
        }
        return false;
    }

    private void printLogo() {

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


}
