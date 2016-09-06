package com.cx.plugin;

import com.checkmarx.v7.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.net.URL;
import java.util.List;

/**
 * Created by: Dorg.
 * Date: 18/08/2016.
 */
public class CxClient {

    private Log log;
    private String sessionId;
    private CxSDKWebServiceSoap client;
    private Credentials credentials;
    private CxWSResponseRunID scanResponse;
    private ScanDisplayData scanDisplayData;
    private int scanTimeoutInMin =  -1;
    private int generateReportTimeLimitInSec =  60;


    private static final QName SERVICE_NAME = new QName("http://Checkmarx.com/v7", "CxSDKWebService");
    private static URL WSDL_LOCATION = CxSDKWebService.class.getClassLoader().getResource("WEB-INF/CxSDKWebService.wsdl");


    public CxClient(String username, String password, URL url, int scanTimeoutInMin, Log log) {

        CxSDKWebService ss = new CxSDKWebService(WSDL_LOCATION, SERVICE_NAME);
        client = ss.getCxSDKWebServiceSoap();
        BindingProvider bindingProvider = (BindingProvider) client;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url + "/cxwebinterface/sdk/CxSDKWebService.asmx");
        credentials = new Credentials();
        credentials.setUser(username);
        credentials.setPass(password);
        this.scanTimeoutInMin = scanTimeoutInMin;
        this.log = log;

    }

    public void loginToServer() throws MojoExecutionException {

        CxWSResponseLoginData res = client.login(credentials, 1099);
        sessionId = res.getSessionId();

        if(sessionId == null) {
            throw new MojoExecutionException("fail to perform login: " + res.getErrorMessage());
        }
    }

    public void createProject(ScanConfiguration conf) throws MojoExecutionException {

        CliScanArgs cliScanArgs = new CliScanArgs();
        cliScanArgs.setClientOrigin(CxClientType.SDK);
        cliScanArgs.setIsIncremental(conf.isIncrementalScan());

        ProjectSettings prjSettings = new ProjectSettings();
        prjSettings.setProjectName(conf.getProjectName());
        if(conf.getPreset() != null){
            long presetIdFromName = getPresetIdFromName(conf.getPreset());
            if(presetIdFromName == 0) {
                log.warn("preset ["+conf.getPreset()+"] not found. scanning with default preset");
            }
            prjSettings.setPresetID(presetIdFromName);
        }

        String associatedGroupId = null;
        if(conf.getFullTeamPath() != null) {
            associatedGroupId = getGroupIdFromTeamPath(conf.getFullTeamPath());
            if(associatedGroupId == null) {
                log.warn("Team Path ["+conf.getFullTeamPath()+"] not found");
            }
        }
        prjSettings.setAssociatedGroupID(associatedGroupId);
        cliScanArgs.setPrjSettings(prjSettings);

        SourceCodeSettings srcCodeSettings = new SourceCodeSettings();
        srcCodeSettings.setSourceOrigin(SourceLocationType.LOCAL);
        LocalCodeContainer packageCode = new LocalCodeContainer();
        packageCode.setFileName(prjSettings.getProjectName()+ "_" + System.currentTimeMillis());
        packageCode.setZippedFile(conf.getZippedSources());

        srcCodeSettings.setPackagedCode(packageCode);
        cliScanArgs.setSrcCodeSettings(srcCodeSettings);

        //todo problem with that
        if(conf.getFolderExclusions() != null || conf.getFileExclusions() != null) {
            SourceFilterPatterns filter = new SourceFilterPatterns();
            filter.setExcludeFilesPatterns(conf.getFileExclusions());
            filter.setExcludeFoldersPatterns(conf.getFolderExclusions());
            srcCodeSettings.setSourceFilterLists(filter);
        }

        log.info("sending scan request");
        scanResponse = client.scan(sessionId, cliScanArgs);
        if(!scanResponse.isIsSuccesfull()) {
            throw new MojoExecutionException("fail to perform scan: " + scanResponse.getErrorMessage());
        }

    }


    private String getGroupIdFromTeamPath(String fullTeamPath) throws MojoExecutionException {

        CxWSResponseGroupList associatedGroupsList = client.getAssociatedGroupsList(sessionId);
        if(!associatedGroupsList.isIsSuccesfull()) {
            return null;
        }

        List<Group> group = associatedGroupsList.getGroupList().getGroup();

        for (Group g: group) {
            if(fullTeamPath.equals(g.getGroupName())){
                return g.getID();
            }

        }

        return null;
    }

    private long getPresetIdFromName(String presetName) throws MojoExecutionException {

        CxWSResponsePresetList presetList = client.getPresetList(sessionId);
        if(!presetList.isIsSuccesfull()) {
            return 0;
        }

        List<Preset> preset = presetList.getPresetList().getPreset();

        for (Preset p : preset) {
            if(presetName.equals(p.getPresetName())){//presetName.equalsIgnoreCase(p.getPresetName(); todo ignore case?
                return p.getID();
            }
        }

        return 0;
    }


    public void waitForScanToFinnish() throws MojoExecutionException {

        String runId = scanResponse.getRunId();
        long timeToStop = (System.currentTimeMillis() / 60000) + scanTimeoutInMin;
        CurrentStatusEnum currentStatus = null;

        long startTime = (System.currentTimeMillis() / 60000);


        while (scanTimeoutInMin < 0 || (System.currentTimeMillis() / 60000) <= timeToStop) {

            try {
                Thread.sleep(60000); //Get status every 60 sec
            } catch (InterruptedException e) {
                throw new MojoExecutionException("caught exception during sleep", e);
            }

            CxWSResponseScanStatus scanStatus = client.getStatusOfSingleScan(sessionId, runId);

            if(!scanStatus.isIsSuccesfull()) {
                throw new MojoExecutionException("fail to get status from scan: " + scanStatus.getErrorMessage());
            }

            currentStatus = scanStatus.getCurrentStatus();

            if(CurrentStatusEnum.FAILED.equals(currentStatus) ||
                    CurrentStatusEnum.CANCELED.equals(currentStatus) ||
                    CurrentStatusEnum.DELETED.equals(currentStatus) ||
                    CurrentStatusEnum.UNKNOWN.equals(currentStatus)) {

                throw new MojoExecutionException("scan cannot be completed. status ["+currentStatus.value()+"]");
            }

            if(CurrentStatusEnum.FINISHED.equals(currentStatus)) {
                scanDisplayData = getScanDisplayData();
                return;
            }

            log.info("waiting for scan to finnish. " + ((System.currentTimeMillis() / 60000) - startTime) + " minuets elapsed" );

        }

        if(!CurrentStatusEnum.FINISHED.equals(currentStatus)) {
            String status =  currentStatus == null ? CurrentStatusEnum.UNKNOWN.value() : currentStatus.value();
            throw new MojoExecutionException("scan has reached the time limit. status: ["+ status +"]");
        }

        log.info("retrieving scan results");
        scanDisplayData = getScanDisplayData();

    }

    public byte[] getScanReport(CxWSReportType reportType) {
        CxWSCreateReportResponse createScanReportResponse = client.createScanReport(
                sessionId, ClientDataGen.genReportRequest(scanDisplayData.getScanID(), reportType));

        if(!createScanReportResponse.isIsSuccesfull()) {
            log.warn("fail to create scan report");
            log.debug("fail to create scan report: " + createScanReportResponse.getErrorMessage());
            return null;
        }

        long reportId = createScanReportResponse.getID();
        if(!waitForReport(reportId)) {
            return null;
        }

        CxWSResponseScanResults scanReport = client.getScanReport(sessionId, reportId);

        if(!scanReport.isIsSuccesfull()) {
            log.warn("fail to create scan report");
            log.debug("fail to create scan report: " + createScanReportResponse.getErrorMessage());
            return null;
        }

        return scanReport.getScanResults();

    }

    private boolean waitForReport(long reportId){
        long timeToStop = (System.currentTimeMillis() / 1000) + generateReportTimeLimitInSec;
        CxWSReportStatusResponse scanReportStatus = null;


        while ((System.currentTimeMillis() / 1000) <= timeToStop) {
            log.debug("waiting for server to generate pdf report"  + (timeToStop - (System.currentTimeMillis() / 1000)) + " sec left to timeout");
            try {
                Thread.sleep(2000); //Get status every 2 sec
            } catch (InterruptedException e) {
                log.warn("fail to generate pdf report");
                log.debug("caught exception during sleep", e);
                return false;
            }

            scanReportStatus = client.getScanReportStatus(sessionId, reportId);


            if(!scanReportStatus.isIsSuccesfull()) {
                log.warn("fail to generate pdf report");
                log.debug("fail to get status from scan report: " + scanReportStatus.getErrorMessage());
                return false;
            }

            if( scanReportStatus.isIsFailed()) {
                log.warn("fail to generate pdf report");
                log.debug("scan report status returned failed");
                return false;
            }

            if(scanReportStatus.isIsReady()) {
                return true;
            }
        }

        if(scanReportStatus == null || !scanReportStatus.isIsReady()) {
            log.warn("fail to generate pdf report");
            log.debug("wait for generate report timed out");
            return false;
        }

        return true;
    }

    private ScanDisplayData getScanDisplayData() throws MojoExecutionException {
        CxWSResponseScansDisplayData scanDataResponse = client.getScansDisplayDataForAllProjects(sessionId);
        if(!scanDataResponse.isIsSuccesfull()) {
            throw new MojoExecutionException("fail to get scan data: " + scanDataResponse.getErrorMessage());
        }

        List<ScanDisplayData> scanList = scanDataResponse.getScanList().getScanDisplayData();
        long projectID = scanResponse.getProjectID();
        for (ScanDisplayData scan : scanList) {
            if(projectID == scan.getProjectId()) {
                return scan;
            }
        }

        throw new MojoExecutionException("fail to get scan data: no scan found");
    }


    public ScanDisplayData getScanResults() {
        return scanDisplayData;
    }
}
