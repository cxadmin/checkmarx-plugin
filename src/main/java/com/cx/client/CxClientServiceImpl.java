package com.cx.client;

import com.checkmarx.v7.*;
import com.cx.client.dto.CreateScanResponse;
import com.cx.client.dto.LocalScanConfiguration;
import com.cx.client.dto.ReportType;
import com.cx.client.dto.ScanResults;
import com.cx.client.exception.CxClientException;
import com.cx.client.rest.CxRestClient;
import com.cx.client.rest.dto.*;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by: Dorg.
 * Date: 18/08/2016.
 */
public class CxClientServiceImpl implements CxClientService {

    private static final Logger log = LoggerFactory.getLogger(CxClientServiceImpl.class);
    private String sessionId;
    private CxSDKWebServiceSoap client;
    private CxRestClient restClient;

    private String username;
    private String password;
    private URL url;


    private static final QName SERVICE_NAME = new QName("http://Checkmarx.com/v7", "CxSDKWebService");
    private static URL WSDL_LOCATION = CxSDKWebService.class.getClassLoader().getResource("WEB-INF/CxSDKWebService.wsdl");
    private static String CHECKMARX_SERVER_WAS_NOT_FOUND_ON_THE_SPECIFIED_ADDRESS = "Fail to validate checkmarx server address";
    private static String SDK_PATH = "/cxwebinterface/sdk/CxSDKWebService.asmx";
    public static final String DEFAULT_PRESET_NAME = "Checkmarx Default";

    private static int generateReportTimeOutInSec = 500;
    private static int waitForScanToFinishRetry = 5;

    public CxClientServiceImpl(URL url, String username, String password) throws CxClientException {
        this.url = url;
        this.username = username;
        this.password = password;

        CxSDKWebService ss = new CxSDKWebService(WSDL_LOCATION, SERVICE_NAME);
        client = ss.getCxSDKWebServiceSoap();
        BindingProvider bindingProvider = (BindingProvider) client;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url + SDK_PATH);
        restClient = new CxRestClient(url.toString(), username, password);
    }


    public void checkServerConnectivity() throws CxClientException {

        try {
            HttpURLConnection urlConn;
            URL toCheck = new URL(url + SDK_PATH);
            urlConn = (HttpURLConnection) toCheck.openConnection();
            urlConn.connect();
            if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new CxClientException(CHECKMARX_SERVER_WAS_NOT_FOUND_ON_THE_SPECIFIED_ADDRESS + ": " + url + " response code: " + urlConn.getResponseCode() + ", message: " + urlConn.getResponseMessage());
            }

        } catch (IOException e) {
            log.debug(CHECKMARX_SERVER_WAS_NOT_FOUND_ON_THE_SPECIFIED_ADDRESS + ": " + url, e);
            throw new CxClientException(CHECKMARX_SERVER_WAS_NOT_FOUND_ON_THE_SPECIFIED_ADDRESS + ": " + url + ", exception message: " + e.getMessage(), e);
        }
    }

    public void disableSSLCertificateVerification() {
        CxPluginHelper.disableSSLCertificateVerification();
        restClient.disableCertificateValidation();
    }

    public void loginToServer() throws CxClientException {

        Credentials credentials = new Credentials();
        credentials.setUser(username);
        credentials.setPass(password);
        CxWSResponseLoginData res = client.login(credentials, 1099);
        sessionId = res.getSessionId();

        if (sessionId == null) {
            throw new CxClientException("fail to perform login: " + res.getErrorMessage());
        }
    }


    public CreateScanResponse createLocalScan(LocalScanConfiguration conf) throws CxClientException {

        CliScanArgs cliScanArgs = CxPluginHelper.genCliScanArgs(conf);

        //todo do this (handler)
        //SourceCodeSettings srcCodeSettings = blah(conf);
        SourceCodeSettings srcCodeSettings = new SourceCodeSettings();
        srcCodeSettings.setSourceOrigin(SourceLocationType.LOCAL);

        LocalCodeContainer packageCode = new LocalCodeContainer();
        packageCode.setFileName(conf.getFileName());
        packageCode.setZippedFile(conf.getZippedSources());
        srcCodeSettings.setPackagedCode(packageCode);
        cliScanArgs.setSrcCodeSettings(srcCodeSettings);

        //todo problem with that
        if (conf.getFolderExclusions() != null || conf.getFileExclusions() != null) {
            SourceFilterPatterns filter = new SourceFilterPatterns();
            filter.setExcludeFilesPatterns(conf.getFileExclusions() == null ? "" : conf.getFileExclusions());
            filter.setExcludeFoldersPatterns(conf.getFolderExclusions() == null ? "" : conf.getFolderExclusions());
            srcCodeSettings.setSourceFilterLists(filter);
        }

        log.info("Sending Scan Request");
        CxWSResponseRunID scanResponse = client.scan(sessionId, cliScanArgs);

        if (!scanResponse.isIsSuccesfull()) {
            throw new CxClientException("fail to perform scan: " + scanResponse.getErrorMessage());
        }

        log.debug("create scan returned with projectId: {}, runId: {}", scanResponse.getProjectID(), scanResponse.getRunId());

        return new CreateScanResponse(scanResponse.getProjectID(), scanResponse.getRunId());
    }


    //todo do this. local/shared scan handler by Class type (localConfig/tfsConfig...)
    //public abstract SourceCodeSettings blah(BaseScanConfiguration conf);

    public CreateScanResponse createLocalScanResolveFields(LocalScanConfiguration conf) throws CxClientException {

        //resolve preset
        if (conf.getPreset() != null) {

            long defaultPresetId = resolvePresetIdFromName(DEFAULT_PRESET_NAME);
            if (DEFAULT_PRESET_NAME.equalsIgnoreCase(conf.getPreset())) {
                conf.setPresetId(defaultPresetId);
            } else {
                long presetId = resolvePresetIdFromName(conf.getPreset());
                conf.setPresetId(presetId);
                if (presetId == 0) {
                    if (conf.isFailPresetNotFound()) {
                        throw new CxClientException("preset: [" + conf.getPreset() + "], not found");
                    } else {
                        conf.setPresetId(defaultPresetId);
                        log.warn("preset [" + conf.getPreset() + "] not found. preset set to default.");
                    }
                }
            }
        }


        return createLocalScan(conf);
    }


    public String resolveGroupIdFromTeamPath(String fullTeamPath) {
        fullTeamPath = StringUtils.defaultString(fullTeamPath).trim();
        CxWSResponseGroupList associatedGroupsList = client.getAssociatedGroupsList(sessionId);
        if (!associatedGroupsList.isIsSuccesfull()) {
            log.warn("fail to retrieve group list: ", associatedGroupsList.getErrorMessage());
            return null;
        }

        List<Group> group = associatedGroupsList.getGroupList().getGroup();

        for (Group g : group) {
            if (fullTeamPath.equalsIgnoreCase(g.getGroupName())) {
                return g.getID();
            }

        }

        return null;
    }

    public long resolvePresetIdFromName(String presetName) {
        presetName = StringUtils.defaultString(presetName).trim();
        CxWSResponsePresetList presetList = client.getPresetList(sessionId);
        if (!presetList.isIsSuccesfull()) {
            log.warn("fail to retrieve preset list: ", presetList.getErrorMessage());
            return 0;
        }

        List<Preset> preset = presetList.getPresetList().getPreset();

        for (Preset p : preset) {
            if (presetName.equalsIgnoreCase(p.getPresetName())) {
                return p.getID();
            }
        }
        return 0;
    }

    public void waitForScanToFinish(String runId, ScanWaitHandler<CxWSResponseScanStatus> waitHandler) throws CxClientException {
        waitForScanToFinish(runId, 0, waitHandler);
    }

    public void waitForScanToFinish(String runId, long scanTimeoutInMin, ScanWaitHandler<CxWSResponseScanStatus> waitHandler) throws CxClientException {

        long timeToStop = (System.currentTimeMillis() / 60000) + scanTimeoutInMin;

        CurrentStatusEnum currentStatus = null;
        CxWSResponseScanStatus scanStatus = null;

        long startTime = System.currentTimeMillis();

        waitHandler.onStart(startTime, scanTimeoutInMin);

        int retry = waitForScanToFinishRetry;

        while (scanTimeoutInMin <= 0 || (System.currentTimeMillis() / 60000) <= timeToStop) {

            try {
                Thread.sleep(10000); //Get status every 10 sec
            } catch (InterruptedException e) {
                log.debug("caught exception during sleep", e);
            }


            try {
                scanStatus = client.getStatusOfSingleScan(sessionId, runId);
            } catch (Exception e) {
                retry = checkRetry(retry, e.getMessage());
                continue;
            }

            if (!scanStatus.isIsSuccesfull()) {
                retry = checkRetry(retry, scanStatus.getErrorMessage());
                continue;
            }

            retry = waitForScanToFinishRetry;

            currentStatus = scanStatus.getCurrentStatus();

            if (CurrentStatusEnum.FAILED.equals(currentStatus) ||
                    CurrentStatusEnum.CANCELED.equals(currentStatus) ||
                    CurrentStatusEnum.DELETED.equals(currentStatus) ||
                    CurrentStatusEnum.UNKNOWN.equals(currentStatus)) {

                waitHandler.onFail(scanStatus);

                throw new CxClientException("scan cannot be completed. status [" + currentStatus.value() + "].");
            }

            if (CurrentStatusEnum.FINISHED.equals(currentStatus)) {
                waitHandler.onSuccess(scanStatus);
                return;
            }

            waitHandler.onIdle(scanStatus);
        }


        if (!CurrentStatusEnum.FINISHED.equals(currentStatus)) {
            waitHandler.onTimeout(scanStatus);
            throw new CxClientException("scan has reached the time limit. (" + scanTimeoutInMin + " minutes).");
        }
    }

    private int checkRetry(int retry, String errorMessage) throws CxClientException {
        log.debug("fail to get status from scan. retrying (" + (retry - 1) + " tries left). error message: " + errorMessage);
        retry--;
        if (retry <= 0) {
            throw new CxClientException("fail to get status from scan. error message: " + errorMessage);
        }

        return retry;
    }

    public ScanResults retrieveScanResults(long projectId) throws CxClientException {
        CxWSResponseProjectScannedDisplayData scanDataResponse = client.getProjectScannedDisplayData(sessionId);
        if (!scanDataResponse.isIsSuccesfull()) {
            throw new CxClientException("fail to get scan data: " + scanDataResponse.getErrorMessage());
        }

        List<ProjectScannedDisplayData> scanList = scanDataResponse.getProjectScannedList().getProjectScannedDisplayData();
        for (ProjectScannedDisplayData scan : scanList) {
            if (projectId == scan.getProjectID()) {
                return CxPluginHelper.genScanResponse(scan);
            }
        }

        throw new CxClientException("no scan data found for projectID [" + projectId + "]");
    }


    public byte[] getScanReport(long scanId, ReportType reportType) throws CxClientException {

        CxWSReportRequest cxWSReportRequest = new CxWSReportRequest();
        cxWSReportRequest.setScanID(scanId);
        CxWSReportType cxWSReportType = CxWSReportType.valueOf(reportType.name());
        cxWSReportRequest.setType(cxWSReportType);
        CxWSCreateReportResponse createScanReportResponse = client.createScanReport(sessionId, cxWSReportRequest);

        if (!createScanReportResponse.isIsSuccesfull()) {
            log.warn("fail to create scan report: " + createScanReportResponse.getErrorMessage());
            throw new CxClientException("fail to create scan report: " + createScanReportResponse.getErrorMessage());
        }

        long reportId = createScanReportResponse.getID();
        waitForReport(reportId);

        CxWSResponseScanResults scanReport = client.getScanReport(sessionId, reportId);

        if (!scanReport.isIsSuccesfull()) {
            log.debug("fail to create scan report: " + createScanReportResponse.getErrorMessage());
            throw new CxClientException("fail to retrieve scan report: " + createScanReportResponse.getErrorMessage());
        }

        return scanReport.getScanResults();

    }

    public void close() {
        //todo implement
    }

    private void waitForReport(long reportId) throws CxClientException {
        //todo: const+ research of the appropriate time
        long timeToStop = (System.currentTimeMillis() / 1000) + generateReportTimeOutInSec;
        CxWSReportStatusResponse scanReportStatus = null;

        while ((System.currentTimeMillis() / 1000) <= timeToStop) {
            log.debug("waiting for server to generate pdf report" + (timeToStop - (System.currentTimeMillis() / 1000)) + " sec left to timeout");
            try {
                Thread.sleep(2000); //Get status every 2 sec
            } catch (InterruptedException e) {
                log.debug("caught exception during sleep", e);
            }

            scanReportStatus = client.getScanReportStatus(sessionId, reportId);


            if (!scanReportStatus.isIsSuccesfull()) {
                log.warn("fail to get status from scan report: " + scanReportStatus.getErrorMessage());
            }

            if (scanReportStatus.isIsFailed()) {
                throw new CxClientException("generation of scan report [id=" + reportId + "] failed");
            }

            if (scanReportStatus.isIsReady()) {
                return;
            }
        }

        if (scanReportStatus == null || !scanReportStatus.isIsReady()) {
            throw new CxClientException("generation of scan report [id=" + reportId + "] failed. timed out");
        }
    }

    public CreateOSAScanResponse createOSAScan(long projectId, File zipFile) throws CxClientException {
        restClient.login();
        return restClient.createOSAScan(projectId, zipFile);
    }


    public OSAScanStatus waitForOSAScanToFinish(String scanId, long scanTimeoutInMin, ScanWaitHandler<OSAScanStatus> waitHandler) throws CxClientException {
        //re login in case of session timed out
        restClient.login();
        long timeToStop = (System.currentTimeMillis() / 60000) + scanTimeoutInMin;

        long startTime = System.currentTimeMillis();
        OSAScanStatus scanStatus = null;
        OSAScanState state = null;
        int stateId = 0;

        waitHandler.onStart(startTime, scanTimeoutInMin);

        int retry = waitForScanToFinishRetry;

        while (scanTimeoutInMin <= 0 || (System.currentTimeMillis() / 60000) <= timeToStop) {

            try {
                Thread.sleep(10000); //Get status every 10 sec
            } catch (InterruptedException e) {
                log.debug("caught exception during sleep", e);
            }


            try {
                scanStatus = restClient.getOSAScanStatus(scanId);
            } catch (Exception e) {
                retry = checkRetry(retry, e.getMessage());
                continue;
            }

            retry = waitForScanToFinishRetry;

            state = scanStatus.getState();

            stateId = state.getId();

            if (OSAScanStatusEnum.FAILED.getNum() == stateId) {
                waitHandler.onFail(scanStatus);
                throw new CxClientException("OSA scan cannot be completed. status: [" + state.getName() + "]. message: [" + StringUtils.defaultString(scanStatus.getState().getFailureReason()) + "]");
            }


            if (OSAScanStatusEnum.SUCCEEDED.getNum() == stateId) {
                waitHandler.onSuccess(scanStatus);
                return scanStatus;
            }
            waitHandler.onIdle(scanStatus);
        }

        if (OSAScanStatusEnum.SUCCEEDED.getNum() != stateId) {
            waitHandler.onTimeout(scanStatus);
            throw new CxClientException("OSA scan has reached the time limit. (" + scanTimeoutInMin + " minutes).");
        }

        return scanStatus;
    }

    public OSASummaryResults retrieveOSAScanSummaryResults(String scanId) throws CxClientException {
        return restClient.getOSAScanSummaryResults(scanId);
    }

    public String retrieveOSAScanHtmlResults(String scanId) throws CxClientException {
        return restClient.getOSAScanHtmlResults(scanId);
    }

    public byte[] retrieveOSAScanPDFResults(String scanId) throws CxClientException {
        return restClient.getOSAScanPDFResults(scanId);
    }

    public static int getWaitForScanToFinishRetry() {
        return waitForScanToFinishRetry;
    }

    public static void setWaitForScanToFinishRetry(int waitForScanToFinishRetry) {
        CxClientServiceImpl.waitForScanToFinishRetry = waitForScanToFinishRetry;
    }

    public static int getGenerateReportTimeOutInSec() {
        return generateReportTimeOutInSec;
    }

    public static void setGenerateReportTimeOutInSec(int generateReportTimeOutInSec) {
        CxClientServiceImpl.generateReportTimeOutInSec = generateReportTimeOutInSec;
    }

}
