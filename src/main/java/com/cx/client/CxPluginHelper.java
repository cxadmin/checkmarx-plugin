package com.cx.client;

import com.checkmarx.v7.CliScanArgs;
import com.checkmarx.v7.CxClientType;
import com.checkmarx.v7.ProjectScannedDisplayData;
import com.checkmarx.v7.ProjectSettings;
import com.cx.client.dto.BaseScanConfiguration;
import com.cx.client.dto.ScanResults;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created by: Dorg.
 * Date: 15/09/2016.
 */
public abstract class CxPluginHelper {

    public static final String HTML_TEMPLATE_LOCATION = "com/cx/plugin/htmlReportTemplate.html";
    private static final Logger log = LoggerFactory.getLogger(CxPluginHelper.class);

    public static ScanResults genScanResponse(ProjectScannedDisplayData scanDisplayData) {
        ScanResults ret = new ScanResults();
        ret.setProjectId(scanDisplayData.getProjectID());
        ret.setScanID(scanDisplayData.getLastScanID());
        ret.setHighSeverityResults(scanDisplayData.getHighVulnerabilities());
        ret.setMediumSeverityResults(scanDisplayData.getMediumVulnerabilities());
        ret.setLowSeverityResults(scanDisplayData.getLowVulnerabilities());
        ret.setInfoSeverityResults(scanDisplayData.getInfoVulnerabilities());
        ret.setRiskLevelScore(scanDisplayData.getRiskLevelScore());

        return ret;
    }

    public static CliScanArgs genCliScanArgs(BaseScanConfiguration conf) {
        CliScanArgs cliScanArgs = new CliScanArgs();

        CxClientType cxClientType = CxClientType.SDK;
        try {
            cxClientType = CxClientType.valueOf(conf.getClientOrigin().name());
        } catch (Exception e) {
            log.debug("Failed to convert client origin enum from value: {}. Client origin set to SDK", conf.getClientOrigin().name());
        }
        cliScanArgs.setClientOrigin(cxClientType);
        cliScanArgs.setIsIncremental(conf.isIncrementalScan());
        cliScanArgs.setIsPrivateScan(conf.isPrivateScan());
        cliScanArgs.setIgnoreScanWithUnchangedCode(conf.isIgnoreScanWithUnchangedCode());
        cliScanArgs.setComment(conf.getComment());

        ProjectSettings prjSettings = new ProjectSettings();
        String projectName = StringUtils.isEmpty(conf.getFullTeamPath()) ? conf.getProjectName() : conf.getFullTeamPath() + "\\" +conf.getProjectName();
        prjSettings.setProjectName(projectName);
        prjSettings.setPresetID(conf.getPresetId());
        prjSettings.setAssociatedGroupID(conf.getGroupId());
        prjSettings.setDescription(conf.getDescription());
        prjSettings.setIsPublic(conf.isPublic());
        prjSettings.setOwner(conf.getOwner());
        prjSettings.setProjectID(conf.getProjectId());
        cliScanArgs.setPrjSettings(prjSettings);

        return cliScanArgs;
    }

    public static String compileHtmlReport(String csv, int highThreshold, int mediumThreshold, int lowThreshold) {

        String ret = null;
        ClassLoader classLoader = CxPluginHelper.class.getClassLoader();
        try {
            String htmlTemplate = IOUtils.toString(classLoader.getResourceAsStream(HTML_TEMPLATE_LOCATION), Charset.defaultCharset());
            htmlTemplate = htmlTemplate.replace("*RESULTS_CSV*", csv.replace("\uFEFF", "").replace("\'", "").replace("\"\"\"\"", "").replace("\r\n", "~~'+\n' "));
            ret = htmlTemplate.replace("*RESULTS_THRESHOLD*", highThreshold + "," + mediumThreshold + "," + lowThreshold);

        } catch (IOException e) {
            log.debug("Failed to get HTML template from: " +HTML_TEMPLATE_LOCATION, e);
        } catch (Exception e) {
            log.debug("Failed to compile HTML report. exception: ", e);
        }

        return ret;

    }

    public static String composeScanLink(String url, ScanResults scanResults) {
        return String.format( url + "/CxWebClient/ViewerMain.aspx?scanId=%s&ProjectID=%s", scanResults.getScanID(), scanResults.getProjectId());
    }

    public static String composeProjectStateLink(String url, long projectId) {
        return String.format( url + "/CxWebClient/portal#/projectState/%s/Summary", projectId);
    }

    public static String convertArrayToString(String[] array){
        return StringUtils.join(array, ',');
    }

    public static void createEmptyZip(File zipFile) throws IOException {

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(zipFile);
            byte[] ioe = new byte[22];
            ioe[0] = 80;
            ioe[1] = 75;
            ioe[2] = 5;
            ioe[3] = 6;
            os.write(ioe);
            os.close();
            os = null;
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    public static void disableSSLCertificateVerification() {

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustManagers = createFakeTrustManager();

        // Install the fake trust manager
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (KeyManagementException e) {
            // In case of exception, do not install fake trust manager
            log.warn("Failed to disable SSL/TLS certificate validation",e);
        } catch (NoSuchAlgorithmException e) {
            // In case of exception, do not install fake trust manager
            log.warn("Failed to disable SSL/TLS certificate validation",e);
        }
    }

    public static TrustManager[] createFakeTrustManager() {
        return new TrustManager[]{new X509TrustManager() {

            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};


    }

}
