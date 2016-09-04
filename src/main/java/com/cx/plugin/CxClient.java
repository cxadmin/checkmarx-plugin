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
    private int scanTimeLimitInSec =  60 * 5;
    private int generateReportTimeLimitInSec =  60;


    private static final QName SERVICE_NAME = new QName("http://Checkmarx.com/v7", "CxSDKWebService");
    private static URL WSDL_LOCATION = CxSDKWebService.class.getClassLoader().getResource("WEB-INF/CxSDKWebService.wsdl");


    public CxClient(String username, String password, URL url, Log log) {

        CxSDKWebService ss = new CxSDKWebService(WSDL_LOCATION, SERVICE_NAME);
        client = ss.getCxSDKWebServiceSoap();
        BindingProvider bindingProvider = (BindingProvider) client;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url + "/cxwebinterface/sdk/CxSDKWebService.asmx");
        credentials = new Credentials();
        credentials.setUser(username);
        credentials.setPass(password);
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
        cliScanArgs.setComment("maven plugin scan");//todo what to write?
        cliScanArgs.setIgnoreScanWithUnchangedCode(true);//todo true or false?
        cliScanArgs.setIsIncremental(conf.isIncrementalScan());
        cliScanArgs.setIsPrivateScan(true);//todo true or false?


        ProjectSettings prjSettings = new ProjectSettings();
        prjSettings.setProjectName(conf.getProjectName());
        if(conf.getPreset() != null){
            prjSettings.setPresetID(getPresetIdFromName(conf.getPreset()));
        }

        String associatedGroupId = null;
        if(conf.getFullTeamPath() != null) {//todo what to do if teamName is missing?
            associatedGroupId = getGroupIdFromTeamPath(conf.getFullTeamPath());
        }
        prjSettings.setAssociatedGroupID(associatedGroupId);
        cliScanArgs.setPrjSettings(prjSettings);

        SourceCodeSettings srcCodeSettings = new SourceCodeSettings();
        srcCodeSettings.setSourceOrigin(SourceLocationType.LOCAL);
        LocalCodeContainer packageCode = new LocalCodeContainer();
        packageCode.setFileName("maven_project");//todo what file name?
        packageCode.setZippedFile(conf.getZippedSources());

        srcCodeSettings.setPackagedCode(packageCode);
        cliScanArgs.setSrcCodeSettings(srcCodeSettings);

        //todo handle 500 in server if "SourceFilterPatterns" is set but its fields are null
        /*2016-08-23 15:16:29,812 [6812] ERROR - Unexpected error occurred at CxServiceFacadeExecuter Execute: System.NullReferenceException: Object reference not set to an instance of an object.
        at Checkmarx.ExtDataTypes.InvalideExcludePatternsCleaner.RemoveInvalidExcludePatterns(String excludePatterns)
        at Checkmarx.ExtDataTypes.SourceFilterPatterns.ConvertSourceFilterPatterns()
        at Checkmarx.UseCase.Scans.Projects.ProjectUseCase.CreateNewProject(ProjectConfiguration projectConfiguration, NewProjectCreationSettings newProjectCreationSettings, Boolean isBranchProject, Project originalProject, Boolean isScanPublic)
        at Checkmarx.UseCase.Scans.Projects.ProjectUseCase.ScanNewProject(CliScanArgs args, CronTriggerData cronTriggerData, Guid teamId, Boolean runScanNow, Boolean forceScan, String origin, String rawProjectName)
        at Checkmarx.UseCase.Scans.Projects.ProjectUseCase.Scan(CliScanArgs args, CronTriggerData triggerData, Boolean runScanNow, String origin)
        at lambda_method(Closure , IProjectUseCase )
        at Checkmarx.Facade.CxFacadeExecuter.InvokeUseCase[TResponse,TUseCase](TUseCase useCase, Func`2 compiledAction, CultureInfo contextLocalization)
        at Checkmarx.Facade.CxFacadeExecuter.Execute[TResponse,TUseCase](UseCaseExecutionArguments`2 executionArgs)*/

//        SourceFilterPatterns filter = new SourceFilterPatterns();//todo check if this even works
//        filter.setExcludeFilesPatterns(conf.getFileExclusions());
//        filter.setExcludeFoldersPatterns(conf.getFolderExclusions());
//        srcCodeSettings.setSourceFilterLists(filter);

        log.info("sending scan request");
        scanResponse = client.scan(sessionId, cliScanArgs);
        if(!scanResponse.isIsSuccesfull()) {
            throw new MojoExecutionException("fail to perform scan: " + scanResponse.getErrorMessage());
        }

    }

    //todo maybe get all things necessary from server before doing anything?
    private String getGroupIdFromTeamPath(String fullTeamPath) throws MojoExecutionException {//todo what to do if group not found?

        CxWSResponseGroupList associatedGroupsList = client.getAssociatedGroupsList(sessionId);
        if(!associatedGroupsList.isIsSuccesfull()) {
            return null;
        }

        List<Group> group = associatedGroupsList.getGroupList().getGroup();

        for (Group g: group) {
            if(fullTeamPath.equals(g.getGroupName())){//teamFullPath.equalsIgnoreCase(g.getFullPath()); todo ignore case?
                return g.getID();
            }

        }

        return null;
    }

    private long getPresetIdFromName(String presetName) throws MojoExecutionException {//todo what to do if preset not found?

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
        log.info("waiting for scan to finnish");
        String runId = scanResponse.getRunId();
        long timeToStop = (System.currentTimeMillis() / 1000) + scanTimeLimitInSec;
        CurrentStatusEnum currentStatus = null;


        while ((System.currentTimeMillis() / 1000) <= timeToStop) {
            log.info("waiting for scan to finnish " + (timeToStop - (System.currentTimeMillis() / 1000)) + " seconds left before timeout" );

            try {
                Thread.sleep(5000); //Get status every 10 sec
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

        }

        if(!CurrentStatusEnum.FINISHED.equals(currentStatus)) {
            String status =  currentStatus == null ? CurrentStatusEnum.UNKNOWN.value() : currentStatus.value();
            throw new MojoExecutionException("has reached time limit. status: ["+ status +"]");
        }

        log.info("requesting scan data");
        scanDisplayData = getScanDisplayData();

    }

    public byte[] getScanReport(CxWSReportType reportType) throws MojoExecutionException {

        log.info("getScanReport");
        CxWSCreateReportResponse createScanReportResponse = client.createScanReport(
                sessionId, ClientDataGen.genReportRequest(scanDisplayData.getScanID(), reportType));

        if(!createScanReportResponse.isIsSuccesfull()) {
            throw new MojoExecutionException("fail to create scan report: " + createScanReportResponse.getErrorMessage());
        }

        long reportId = createScanReportResponse.getID();
        waitForReport(reportId);

        CxWSResponseScanResults scanReport = client.getScanReport(sessionId, reportId);

        if(!scanReport.isIsSuccesfull()) {
            throw new MojoExecutionException("fail to get scan report: " + scanReport.getErrorMessage());
        }

        return scanReport.getScanResults();

    }

    private void waitForReport(long reportId) throws MojoExecutionException {
        log.info("waitForReport");
        long timeToStop = (System.currentTimeMillis() / 1000) + generateReportTimeLimitInSec;
        CxWSReportStatusResponse scanReportStatus = null;


        while ((System.currentTimeMillis() / 1000) <= timeToStop) {
            log.info("waitForReport "  + (timeToStop - (System.currentTimeMillis() / 1000)) + " sec left");
            try {
                Thread.sleep(2000); //Get status every 2 sec
            } catch (InterruptedException e) {
                throw new MojoExecutionException("caught exception during sleep", e);
            }

            scanReportStatus = client.getScanReportStatus(sessionId, reportId);


            if(!scanReportStatus.isIsSuccesfull()) {
                throw new MojoExecutionException("fail to get status from scan report: " + scanReportStatus.getErrorMessage());
            }

            if( scanReportStatus.isIsFailed()) {
                throw new MojoExecutionException("fail to generate report");
            }

            if(scanReportStatus.isIsReady()) {
                return;
            }
        }

        if(scanReportStatus == null || !scanReportStatus.isIsReady()) {
            throw new MojoExecutionException("wait for generate report timed out");
        }

    }

    private ScanDisplayData getScanDisplayData() throws MojoExecutionException {
        log.info("getScanDisplayData");
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
