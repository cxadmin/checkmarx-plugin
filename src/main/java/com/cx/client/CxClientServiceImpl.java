package com.cx.client;

import com.checkmarx.v7.*;
import com.cx.client.dto.CreateScanResponse;
import com.cx.client.dto.LocalScanConfiguration;
import com.cx.client.dto.ReportType;
import com.cx.client.dto.ScanResults;
import com.cx.client.exception.CxClientException;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
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


    private static final QName SERVICE_NAME = new QName("http://Checkmarx.com/v7", "CxSDKWebService");
    private static URL WSDL_LOCATION = CxSDKWebService.class.getClassLoader().getResource("WEB-INF/CxSDKWebService.wsdl");

    int GENERATE_REPORT_TIME_OUT_IN_SEC = 500;




    public CxClientServiceImpl(String url) {
        CxSDKWebService ss = new CxSDKWebService(WSDL_LOCATION, SERVICE_NAME);
        client = ss.getCxSDKWebServiceSoap();
        BindingProvider bindingProvider = (BindingProvider) client;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url + "/cxwebinterface/sdk/CxSDKWebService.asmx");
    }

    public void loginToServer(String username, String password) throws CxClientException {

        Credentials credentials = new Credentials();
        credentials.setUser(username);
        credentials.setPass(password);
        CxWSResponseLoginData res = client.login(credentials, 1099);
        sessionId = res.getSessionId();

        if(sessionId == null) {
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
        if(conf.getFolderExclusions() != null || conf.getFileExclusions() != null) {
            SourceFilterPatterns filter = new SourceFilterPatterns();
            filter.setExcludeFilesPatterns(conf.getFileExclusions());
            filter.setExcludeFoldersPatterns(conf.getFolderExclusions());
            srcCodeSettings.setSourceFilterLists(filter);
        }

        log.info("Sending Scan Request");
        CxWSResponseRunID scanResponse = client.scan(sessionId, cliScanArgs);

        if(!scanResponse.isIsSuccesfull()) {
            throw new CxClientException("fail to perform scan: " + scanResponse.getErrorMessage());
        }

        log.debug("create scan returned with projectId: {}, runId: {}", scanResponse.getProjectID(), scanResponse.getRunId());

        return  new CreateScanResponse(scanResponse.getProjectID(), scanResponse.getRunId());
    }



    //todo do this. local/shared scan handler by Class type (localConfig/tfsConfig...)
    //public abstract SourceCodeSettings blah(BaseScanConfiguration conf);

    public CreateScanResponse createLocalScanResolveFields(LocalScanConfiguration conf) throws CxClientException {

        //resolve preset
        if(conf.getPreset() != null) {
            long presetId = resolvePresetIdFromName(conf.getPreset());
            conf.setPresetId(presetId);
            if(presetId == 0 && conf.isFailPresetNotFound()) {
                throw new CxClientException("preset: ["+conf.getPreset()+"], not found");
            }
        }

        //resolve preset
        if(conf.getFullTeamPath() != null) {
            String groupId = resolveGroupIdFromTeamPath(conf.getFullTeamPath());
            conf.setGroupId(groupId);
            if(groupId == null && conf.isFailTeamNotFound()) {
                throw new CxClientException("team: ["+conf.getFullTeamPath()+"], not found");
            }
        }

        return createLocalScan(conf);
    }



    public String resolveGroupIdFromTeamPath(String fullTeamPath) {
        fullTeamPath = StringUtils.defaultString(fullTeamPath).trim();
        CxWSResponseGroupList associatedGroupsList = client.getAssociatedGroupsList(sessionId);
        if(!associatedGroupsList.isIsSuccesfull()) {
            log.warn("fail to retrieve group list: ", associatedGroupsList.getErrorMessage());
            return null;
        }

        List<Group> group = associatedGroupsList.getGroupList().getGroup();

        for (Group g: group) {
            if(fullTeamPath.equalsIgnoreCase(g.getGroupName())){
                return g.getID();
            }

        }

        log.warn("team ["+fullTeamPath+"] not found");
        return null;
    }

    public long resolvePresetIdFromName(String presetName) {
        presetName = StringUtils.defaultString(presetName).trim();
        CxWSResponsePresetList presetList = client.getPresetList(sessionId);
        if(!presetList.isIsSuccesfull()) {
            log.warn("fail to retrieve preset list: ", presetList.getErrorMessage());
            return 0;
        }

        List<Preset> preset = presetList.getPresetList().getPreset();

        for (Preset p : preset) {
            if(presetName.equalsIgnoreCase(p.getPresetName())){
                return p.getID();
            }
        }
        log.warn("preset ["+presetName+"] not found", presetList.getErrorMessage());
        return 0;
    }

    public void waitForScanToFinish(String runId) throws CxClientException {
        waitForScanToFinish(runId, 0);
    }

    public void waitForScanToFinish(String runId, long scanTimeoutInMin) throws CxClientException {

        long timeToStop = (System.currentTimeMillis() / 60000) + scanTimeoutInMin;
        CurrentStatusEnum currentStatus = null;

        long startTime = System.currentTimeMillis();


        while (scanTimeoutInMin <= 0 || (System.currentTimeMillis() / 60000) <= timeToStop) {

            long hours = (System.currentTimeMillis() - startTime) / 3600000;
            long minutes = ((System.currentTimeMillis() - startTime) % 3600000) / 60000;
            long seconds = ((System.currentTimeMillis() - startTime) % 60000) / 1000;

            String hoursStr = (hours < 10)?("0" + Long.toString(hours)):(Long.toString(hours));
            String minutesStr = (minutes < 10)?("0" + Long.toString(minutes)):(Long.toString(minutes));
            String secondsStr = (seconds < 10)?("0" + Long.toString(seconds)):(Long.toString(seconds));

            try {
                Thread.sleep(10000); //Get status every 10 sec
            } catch (InterruptedException e) {
                log.debug("caught exception during sleep", e);
            }

            CxWSResponseScanStatus scanStatus = client.getStatusOfSingleScan(sessionId, runId);

            if(!scanStatus.isIsSuccesfull()) {
                log.warn("fail to get status from scan: " + scanStatus.getErrorMessage());
                continue;
            }

            currentStatus = scanStatus.getCurrentStatus();

            if(CurrentStatusEnum.FAILED.equals(currentStatus) ||
                    CurrentStatusEnum.CANCELED.equals(currentStatus) ||
                    CurrentStatusEnum.DELETED.equals(currentStatus) ||
                    CurrentStatusEnum.UNKNOWN.equals(currentStatus)) {

                throw new CxClientException("scan cannot be completed. status ["+currentStatus.value()+"]. Stage message: ["+scanStatus.getStageMessage()+"]");
            }

            if(CurrentStatusEnum.FINISHED.equals(currentStatus)) {
                return;
            }
            log.info("Waiting for Results. " +
                    "Time Elapsed: " + hoursStr + ":" + minutesStr + ":" + secondsStr + ". " +
                    scanStatus.getTotalPercent() + "% Processed. " +
                    "Status: " + scanStatus.getStageName() + ".");

        }

        if(!CurrentStatusEnum.FINISHED.equals(currentStatus)) {
            String status =  currentStatus == null ? CurrentStatusEnum.UNKNOWN.value() : currentStatus.value();
            throw new CxClientException("scan has reached the time limit ("+scanTimeoutInMin+" minutes). status: ["+ status +"]");
        }
    }

    public ScanResults retrieveScanResults(long projectID) throws CxClientException {
        CxWSResponseProjectScannedDisplayData scanDataResponse = client.getProjectScannedDisplayData(sessionId);
        if(!scanDataResponse.isIsSuccesfull()) {
            throw new CxClientException("fail to get scan data: " + scanDataResponse.getErrorMessage());
        }

        List<ProjectScannedDisplayData> scanList = scanDataResponse.getProjectScannedList().getProjectScannedDisplayData();
        for (ProjectScannedDisplayData scan : scanList) {
            if(projectID == scan.getProjectID()) {
                return CxPluginHelper.genScanResponse(scan);
            }
        }

        throw new CxClientException("no scan data found for projectID ["+projectID+"]");
    }




    public byte[] getScanReport(long scanId, ReportType reportType) throws CxClientException {

        CxWSReportRequest cxWSReportRequest = new CxWSReportRequest();
        cxWSReportRequest.setScanID(scanId);
        CxWSReportType cxWSReportType = CxWSReportType.valueOf(reportType.name());
        cxWSReportRequest.setType(cxWSReportType);
        CxWSCreateReportResponse createScanReportResponse = client.createScanReport(sessionId, cxWSReportRequest);

        if(!createScanReportResponse.isIsSuccesfull()) {
            log.warn("fail to create scan report: " + createScanReportResponse.getErrorMessage());
            throw new CxClientException("fail to create scan report: " + createScanReportResponse.getErrorMessage());
        }

        long reportId = createScanReportResponse.getID();
        waitForReport(reportId);

        CxWSResponseScanResults scanReport = client.getScanReport(sessionId, reportId);

        if(!scanReport.isIsSuccesfull()) {
            log.debug("fail to create scan report: " + createScanReportResponse.getErrorMessage());
            throw new CxClientException("fail to retrieve scan report: " + createScanReportResponse.getErrorMessage());
        }

        return scanReport.getScanResults();

    }

    private void waitForReport(long reportId) throws CxClientException {
        //todo: const+ research of the appropriate time
        long timeToStop = (System.currentTimeMillis() / 1000) + GENERATE_REPORT_TIME_OUT_IN_SEC;
        CxWSReportStatusResponse scanReportStatus = null;

        while ((System.currentTimeMillis() / 1000) <= timeToStop) {
            log.debug("waiting for server to generate pdf report"  + (timeToStop - (System.currentTimeMillis() / 1000)) + " sec left to timeout");
            try {
                Thread.sleep(2000); //Get status every 2 sec
            } catch (InterruptedException e) {
                log.debug("caught exception during sleep", e);
            }

            scanReportStatus = client.getScanReportStatus(sessionId, reportId);


            if(!scanReportStatus.isIsSuccesfull()) {
                log.warn("fail to get status from scan report: " + scanReportStatus.getErrorMessage());
            }

            if( scanReportStatus.isIsFailed()) {
                throw new CxClientException("generation of scan report [id="+reportId+"] failed");
            }

            if(scanReportStatus.isIsReady()) {
                return;
            }
        }

        if(scanReportStatus == null || !scanReportStatus.isIsReady()) {
            throw new CxClientException("generation of scan report [id="+reportId+"] failed. timed out");
        }
    }

}
