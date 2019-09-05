package com.cx.plugin;

import com.cx.plugin.dto.ScanResults;
import com.cx.restclient.CxShragaClient;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ThresholdResult;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.osa.dto.OSAResults;
import com.cx.restclient.sast.dto.SASTResults;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.MavenLoggerAdapter;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import static com.cx.plugin.utils.CxPluginUtils.*;


/**
 * The 'scan' goal creates a single comprehensive scan, including all the modules in the reactor.
 */
@Mojo(name = "scan", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST, inheritByDefault = false)
public class CxScanPlugin extends AbstractMojo {

    private static Logger log = LoggerFactory.getLogger(CxScanPlugin.class);
    public static final String PLUGIN_ORIGIN = "Maven";
    public static final String SOURCES_ZIP_NAME = "sources";

    /**
     * The username of the user running the scan.
     */
    @Parameter(required = false, property = "cx.username")
    private String username;

    /**
     * The password of the user running the scan.
     */
    @Parameter(required = false, property = "cx.password", readonly = true)
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
    @Parameter(property = "cx.fullTeamPath", defaultValue = "\\CxServer")
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


    @Parameter(property = "cx.comment")
    private String comment;
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
    @Deprecated
    @Parameter(property = "cx.osaExclusions")
    private String[] osaExclusions = new String[0];

    /**
     * List of Maven scopes to be ignored in CxOSA scan
     * test and provided scopes are ignored by default unless configured otherwise
     */
    @Parameter(property = "cx.osaIgnoreScopes")
    private String[] osaIgnoreScopes = new String[0];

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
    @Deprecated
    @Parameter(defaultValue = "true", property = "cx.osaGeneratePDFReport")
    private boolean osaGeneratePDFReport;

    /**
     * \\
     * If true, a CxOSA HTML report will be generated in the output directory.
     */
    @Deprecated
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

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    @Parameter(property = "serverId")
    private String serverId;

    @Component(role = org.sonatype.plexus.components.sec.dispatcher.SecDispatcher.class, hint = "default")
    private DefaultSecDispatcher securityDispatcher;

    @Component(role = Archiver.class, hint = "zip")
    private ZipArchiver zipArchiver;

    private String pluginVersion;

    public void execute() throws MojoExecutionException, MojoFailureException {
        MavenLoggerAdapter.setLogger(getLog());
        printLogo(log);
        CxShragaClient shraga = null;
        boolean sastCreated = false;
        boolean osaCreated = false;

        try {

            PluginDescriptor pd = (PluginDescriptor) getPluginContext().get("pluginDescriptor");
            if (pd != null) {
                pluginVersion = pd.getVersion();
            }
            //resolve configuration
            CxScanConfig config = resolveConfigurationMap();

            //print configuration
            printConfiguration(config, osaIgnoreScopes, pluginVersion, log);


            if (!config.getSastEnabled() && !config.getOsaEnabled()) {
                throw new MojoFailureException("Both SAST and OSA are disabled. exiting");
            }
            //create scans and retrieve results (in jenkins agent)
            ScanResults ret = new ScanResults(new SASTResults(), new OSAResults());


            //initialize cx client
            try {
                shraga = new CxShragaClient(config, log);
                shraga.init();
            } catch (Exception ex) {
                if (ex.getMessage().contains("Server is unavailable")) {
                    try {
                        shraga.login();
                    } catch (CxClientException e) {
                        throw new MojoFailureException(e.getMessage());
                    }
                    String errorMsg = "Connection Failed.\n" +
                            "Validate the provided login credentials and server URL are correct.\n" +
                            "In addition, make sure the installed plugin version is compatible with the CxSAST version according to CxSAST release notes.";
                    throw new MojoFailureException(ex.getMessage() + ": " + errorMsg);
                }
                throw new MojoFailureException(ex.getMessage(), ex);
            }

            //Create OSA scan
            if (config.getOsaEnabled()) {

                File dummyFileForOSA = null;
                try {
                    dummyFileForOSA = createDummyFileForOSA();
                    Properties scannerProperties = generateOSAScanConfiguration(project.getBasedir().getAbsolutePath(), osaIgnoreScopes, dummyFileForOSA.getName());
                    shraga.setOsaFSAProperties(scannerProperties);
                    shraga.createOSAScan();
                    osaCreated = true;
                } catch (com.cx.restclient.exception.CxClientException | IOException e) {
                    ret.setOsaCreateException(e);
                    log.warn(e.getMessage());
                } finally {
                    FileUtils.deleteQuietly(dummyFileForOSA);
                }
            }

            //Create SAST scan
            if (config.getSastEnabled()) {
                try {
                    //prepare sources to scan (zip them)
                    log.info("Zipping sources");
                    File zipFile = zipSources(reactorProjects, zipArchiver, outputDirectory, log);
                    config.setZipFile(zipFile);

                    shraga.createSASTScan();
                    sastCreated = true;
                } catch (IOException | CxClientException e) {
                    ret.setSastCreateException(e);
                    log.error(e.getMessage());
                }
            }

            //Asynchronous MODE
            if (!config.getSynchronous()) {
                if (ret.getSastCreateException() != null) {
                    throw new MojoExecutionException(ret.getSastCreateException().getMessage());
                }
                if (ret.getOsaCreateException() != null) {
                    throw new MojoExecutionException(ret.getOsaCreateException().getMessage());
                }

                log.info("Running in Asynchronous mode. Not waiting for scan to finish");
                return;
            }

            //Get SAST results
            if (sastCreated) {
                try {
                    SASTResults sastResults = shraga.waitForSASTResults();
                    ret.setSastResults(sastResults);
                } catch (InterruptedException e) {
                    if (config.getSynchronous()) {
                        cancelScan(shraga);
                    }
                    throw e;

                } catch (com.cx.restclient.exception.CxClientException | IOException e) {
                    ret.setSastWaitException(e);
                    log.error(e.getMessage());
                }
            }

            //Get OSA results
            if (osaCreated) {
                try {
                    OSAResults osaResults = shraga.waitForOSAResults();
                    ret.setOsaResults(osaResults);
                } catch (com.cx.restclient.exception.CxClientException | IOException e) {
                    ret.setOsaWaitException(e);
                    log.error(e.getMessage());
                }
            }

            //assert if expected exception is thrown  OR when vulnerabilities under threshold
            ThresholdResult thresholdResult = shraga.getThresholdResult();
            if (thresholdResult.isFail() || ret.getSastWaitException() != null || ret.getSastCreateException() != null ||
                    ret.getOsaCreateException() != null || ret.getOsaWaitException() != null) {
                assertBuildFailure(thresholdResult.getFailDescription(), ret);
            }

        } catch (InterruptedException e) {
            log.error("Interrupted exception: " + e.getMessage(), e);

            if (shraga != null && sastCreated) {
                log.error("Canceling scan on the Checkmarx server...");
                cancelScan(shraga);
            }
            throw new MojoExecutionException(e.getMessage());
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception: " + e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage());
        } finally {
            if (shraga != null) {
                shraga.close();
            }
        }
    }

    private File createDummyFileForOSA() throws IOException {
        String dummyFilename = "dummy" + RandomStringUtils.randomNumeric(4) + ".java";
        File file = new File(project.getBasedir().getAbsolutePath(), dummyFilename);
        file.createNewFile();
        return file;
    }

    private void cancelScan(CxShragaClient shraga) {
        try {
            shraga.cancelSASTScan();
        } catch (Exception ignored) {
        }
    }

/*    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public void setServerId(String key) {
        this.serverId = key;
    }

    public void setSecurityDispatcher(DefaultSecDispatcher securityDispatcher) {
        this.securityDispatcher = securityDispatcher;
    }*/

    private CxScanConfig resolveConfigurationMap() throws MojoExecutionException {
        CxScanConfig scanConfig = new CxScanConfig();
        scanConfig.setCxOrigin(PLUGIN_ORIGIN);
        scanConfig.setSastEnabled(true);
        scanConfig.setDisableCertificateValidation(disableCertificateVerification);
        loadUserInfoFromSettings();
        scanConfig.setUsername(username);
        scanConfig.setPassword(password);
        scanConfig.setUrl(url.toString());// todo check
        scanConfig.setReportsDir(outputDirectory);
        scanConfig.setProjectName(projectName);
        scanConfig.setTeamPath(fullTeamPath);
        scanConfig.setPresetName(preset);
        scanConfig.setSastScanTimeoutInMinutes(scanTimeoutInMinutes);
        scanConfig.setScanComment(comment);
        scanConfig.setIncremental(isIncrementalScan);
        scanConfig.setSynchronous(isSynchronous);
        boolean thresholdEnabled = (highSeveritiesThreshold > 0 || mediumSeveritiesThreshold > 0 || lowSeveritiesThreshold > 0);//todo check null
        scanConfig.setSastThresholdsEnabled(thresholdEnabled);
        scanConfig.setSastHighThreshold(highSeveritiesThreshold);
        scanConfig.setSastMediumThreshold(mediumSeveritiesThreshold);
        scanConfig.setSastLowThreshold(lowSeveritiesThreshold);
        scanConfig.setGeneratePDFReport(generatePDFReport);
        scanConfig.setOsaEnabled(osaEnabled);
        boolean osaThresholdEnabled = (osaHighSeveritiesThreshold > 0 || osaMediumSeveritiesThreshold > 0 || osaLowSeveritiesThreshold > 0);//todo checkk null
        scanConfig.setOsaGenerateJsonReport(osaGenerateJsonReport);
        scanConfig.setOsaThresholdsEnabled(osaThresholdEnabled);
        scanConfig.setOsaHighThreshold(osaHighSeveritiesThreshold);
        scanConfig.setOsaMediumThreshold(osaMediumSeveritiesThreshold);
        scanConfig.setOsaLowThreshold(osaLowSeveritiesThreshold);

        return scanConfig;
    }

    private void loadUserInfoFromSettings() throws MojoExecutionException {
        if (this.serverId == null) {
            return;
        }

        if ((username == null || password == null) && (settings != null)) {
            Server server = this.settings.getServer(this.serverId);
            if (server != null) {
                if (username == null) {
                    username = server.getUsername();
                }
                if (password == null && server.getPassword() != null) {
                    try {
                        password = securityDispatcher.decrypt(server.getPassword());
                    } catch (SecDispatcherException ex) {
                        try {
                            securityDispatcher.setConfigurationFile(System.getProperty("user.home") + "\\.m2\\settings-security.xml");
                            password = securityDispatcher.decrypt(server.getPassword());
                        } catch (Exception e) {
                            throw new MojoExecutionException(e.getMessage());
                        }
                    }
                }
            }
        }

        if (username == null) {
            username = "";
        }
        if (password == null) {
            password = "";
        }
    }

}
