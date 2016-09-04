package com.cx.plugin;

import com.checkmarx.v7.CliScanArgs;
import com.checkmarx.v7.CxClientType;
import com.checkmarx.v7.ProjectConfiguration;
import com.checkmarx.v7.Scan;

/**
 * Created by: iland.
 * Date: 7/9/2015.
 */
public class SDKDataGen {

//    private static final Logger log = LoggerFactory.getLogger(SDKDataGen.class);

    private static final long PROJECT_ID = 0;
    private static final String PROJECT_NAME = "SDK_Scan_OriginName_Test";
    private static final long PRESET_ID = 17; //Presets: 1=All, 17=Default 2014
    private static final long SCAN_CONFIGURATION_ID = 1;
    private static final String TFS_SERVERNAME = "tfs2013:8080/tfs/DefaultCollection";
    private static final String TFS_USERNAME = "dm\\auto";
    private static final String TFS_PASS = "cx123456";

    private static final String ZIP_FILE_PATH = "";

    public static Scan genScan(String sessionId, ProjectConfiguration prjConf, boolean isPrivateScan) {
        Scan scan = new Scan();
        scan.setSessionId(sessionId);
        scan.setArgs(genCliScanArgs(prjConf, isPrivateScan));
        return scan;
    }

    public static CliScanArgs genCliScanArgs(ProjectConfiguration prjConf, boolean isPrivateScan) {
        CliScanArgs cliScanArgs = new CliScanArgs();
        cliScanArgs.setIsIncremental(false);
        cliScanArgs.setIsPrivateScan(isPrivateScan);
        cliScanArgs.setIgnoreScanWithUnchangedCode(false);
        cliScanArgs.setClientOrigin(CxClientType.SDK);
        cliScanArgs.setComment("maven plugin scan");

        //Set Source Code settings
        cliScanArgs.setSrcCodeSettings(prjConf.getSourceCodeSettings());
        //Set Project settings
        cliScanArgs.setPrjSettings(prjConf.getProjectSettings());

        return cliScanArgs;
    }

//    public static ProjectConfiguration genProjectConfigurationToScan(ProjectParam prParam) {
//        ProjectConfiguration projConfig = new ProjectConfiguration();
//        projConfig.setProjectSettings(genProjectSettings(prParam.getProjectName(), prParam.getAssociatedGroupsListResponse()));
//
//        switch (prParam.getSrcType()) {
//            case LOCAL:
//                projConfig.setSourceCodeSettings(genSourceCodeSettingsLocal(prParam.getFilePath()));
//                break;
//            case SHARED:
//                //todo: fixme
//                projConfig.setSourceCodeSettings(genSourceCodeSettingsLocal(prParam.getFilePath()));
//                break;
//            case SOURCE_CONTROL:
//                projConfig.setSourceCodeSettings(genSourceCodeSettings(prParam.getSrcControlSettings(), prParam.getSrcType(), prParam.getArrayOfScanPath()));
//                break;
//            default:
//                projConfig.setSourceCodeSettings(genSourceCodeSettingsLocal(ZIP_FILE_PATH));
//        }
//
//        projConfig.setScheduleSettings(genScheduleSettings(ScheduleType.NOW));
//
//        return projConfig;
//    }
//
//    public static GetStatusOfSingleScan genGetStatusOfSingleScan(String sessionId, String runId) {
//        GetStatusOfSingleScan singleStat = new GetStatusOfSingleScan();
//        singleStat.setSessionID(sessionId);
//        singleStat.setRunId(runId);
//        return singleStat;
//    }
//
//    public static ScheduleSettings genScheduleSettings(ScheduleType scheduleType) {
//        ScheduleSettings scheduleSettings = new ScheduleSettings();
//        scheduleSettings.setSchedule(scheduleType);
//        return scheduleSettings;
//    }
//
//    //Generate project settings
//    public static ProjectSettings genProjectSettings(String projName, GetAssociatedGroupsListResponse ascGrpListRes) {
//        ProjectSettings projSettings = new ProjectSettings();
//        projSettings.setPresetID(PRESET_ID);
//        projSettings.setProjectName(projName);
//
//        if (ascGrpListRes == null || ascGrpListRes.getGetAssociatedGroupsListResult().toString().isEmpty()) {
//            String cxServerGrpId = "00000000-1111-1111-b111-989c9070eb11";
//            projSettings.setAssociatedGroupID(cxServerGrpId);
//        } else {
//            String validId = ascGrpListRes.getGetAssociatedGroupsListResult().getGroupList().getGroup().get(0).getID();
//            projSettings.setAssociatedGroupID(validId);
//        }
//        projSettings.setScanConfigurationID(SCAN_CONFIGURATION_ID);
//        return projSettings;
//    }
//
//
//    public static SourceCodeSettings genSourceCodeSettings(SourceControlSettings srcControlSettings, SourceLocationType srcType, ArrayOfScanPath arrOfScanPath) {
//        SourceCodeSettings srcSettings = new SourceCodeSettings();
//        srcSettings.setSourceControlSetting(srcControlSettings);
//        srcSettings.setSourceOrigin(srcType);
//        srcSettings.setPathList(arrOfScanPath);
//        return srcSettings;
//    }
//
//    public static SourceCodeSettings genSourceCodeSettingsLocal(String zipFilePath) {
//        SourceCodeSettings sourceCodeSettings = new SourceCodeSettings();
//        sourceCodeSettings.setSourceOrigin(SourceLocationType.LOCAL);
//
//        LocalCodeContainer localCodeContainer = new LocalCodeContainer();
//        byte[] zipFileByte;
//        try {
//            InputStream fileStream = new FileInputStream(new File(zipFilePath));
//            zipFileByte = IOUtils.toByteArray(fileStream);
//        } catch (Exception e) {
//            log.error("Fail to set zipped file into project.", e);
//            throw new RuntimeException("Fail to set zipped file into project.", e);
//        }
//        localCodeContainer.setZippedFile(zipFileByte);
//        localCodeContainer.setFileName(extractFileNameFromPath(zipFilePath));
//        sourceCodeSettings.setPackagedCode(localCodeContainer);
//
//        SourceFilterPatterns filters = new SourceFilterPatterns();
//        filters.setExcludeFilesPatterns("");
//        filters.setExcludeFoldersPatterns("");
//        sourceCodeSettings.setSourceFilterLists(filters);
//
//        return sourceCodeSettings;
//    }
//
//
//    public static GetScanSummary genGetScanSummary(String sessionId, long lastScanId) {
//        GetScanSummary scanSummary = new GetScanSummary();
//        scanSummary.setSessionID(sessionId);
//        scanSummary.setScanID(lastScanId);
//        return scanSummary;
//    }
//
//    public static GetProjectScannedDisplayData genGetProjectScannedDisplayData(String sessionId) {
//        GetProjectScannedDisplayData scannedDisplayData = new GetProjectScannedDisplayData();
//        scannedDisplayData.setSessionID(sessionId);
//        return scannedDisplayData;
//    }
//
//
//    public static GetAllUsers genGetAllUsers(String sessionId) {
//        GetAllUsers getAllUsers = new GetAllUsers();
//        getAllUsers.setSessionID(sessionId);
//
//        return getAllUsers;
//    }
//
//    public static DeleteUser genDeleteUser(String sessionId, int userId) {
//        DeleteUser deleteUser = new DeleteUser();
//        deleteUser.setSessionID(sessionId);
//        deleteUser.setUserID(userId);
//
//        return deleteUser;
//    }
//
//    public static UpdateProjectConfiguration genUpdateProjectConfiguration(ProjectConfiguration prConfig, String sessionId, long projectId) {
//        UpdateProjectConfiguration updPrjConfig = new UpdateProjectConfiguration();
//        updPrjConfig.setProjectConfiguration(prConfig);
//        updPrjConfig.setSessionID(sessionId);
//        updPrjConfig.setProjectID(projectId);
//        return updPrjConfig;
//    }
//
//    public static Login genLogin(String userName, String password) {
//        Login loginReq = new Login();
//        Credentials credentials = new Credentials();
//        credentials.setUser(userName);
//        credentials.setPass(password);
//        loginReq.setApplicationCredentials(credentials);
//        loginReq.setLcid(1033);
//
//        return loginReq;
//    }
//
//    public static GetAssociatedGroupsList genGetAssociatedGroupsList(String sessionId) {
//        GetAssociatedGroupsList groupsList = new GetAssociatedGroupsList();
//
//        groupsList.setSessionID(sessionId);
//
//        return groupsList;
//    }
//
//    public static ScanWithOriginName genScanWithOriginName(String sessionId, String origName, String groupId, String zipFilePath) {
//        ScanWithOriginName scanWithOriginName = new ScanWithOriginName();
//
//        scanWithOriginName.setSessionId(sessionId);
//        scanWithOriginName.setOrigName(origName);
//        scanWithOriginName.setArgs(genCliScanArgs(groupId, zipFilePath));
//
//        return scanWithOriginName;
//    }
//
//    public static CliScanArgs genCliScanArgs(String groupId, String zipFilePath) {
//        CliScanArgs args = new CliScanArgs();
//
//        args.setPrjSettings(genProjectSettingsByGroupId(groupId));
//        args.setSrcCodeSettings(genSourceCodeSettingsLocal(zipFilePath));
//        args.setIsPrivateScan(false);
//        args.setIsIncremental(false);
//        args.setIgnoreScanWithUnchangedCode(false);
//        args.setClientOrigin(CxClientType.NONE);
//        args.setComment("Automated test client: NONE");
//
//        return args;
//    }
//
//    public static ProjectSettings genProjectSettingsByGroupId(String groupId) {
//        ProjectSettings prjSettings = new ProjectSettings();
//
//        prjSettings.setProjectID(PROJECT_ID);
//        prjSettings.setProjectName(PROJECT_NAME + "_" + IdGenerator.generateNumID(4) + "_" + CommonPluginUtil.getDateNow());
//        prjSettings.setPresetID(PRESET_ID);
//        prjSettings.setAssociatedGroupID(groupId);
//        prjSettings.setScanConfigurationID(SCAN_CONFIGURATION_ID);
//
//        return prjSettings;
//    }
//
//
//    private static String extractFileNameFromPath(String filePath) {
//        String fileName = null;
//        if (filePath.contains("\\")) {
//            fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
//        } else if (filePath.contains("/")) {
//            fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
//        }
//
//        return fileName;
//    }
//
//
//    public static ProjectConfiguration genProjectConfigurationGIT(String prjName, GetAssociatedGroupsListResponse ascGrpListRes) {
//        // Project configuration
//        ProjectConfiguration prjConf = new ProjectConfiguration();
//
//        ProjectSettings prjSettings = new ProjectSettings();
//        prjSettings.setProjectName(prjName);
//        String validId = ascGrpListRes.getGetAssociatedGroupsListResult().getGroupList().getGroup().get(0).getID();
//        prjSettings.setAssociatedGroupID(validId);
//        prjSettings.setPresetID(PRESET_ID); //Presets: 1=All, 17=Default 2014
//        prjSettings.setScanConfigurationID(SCAN_CONFIGURATION_ID);
//
//        prjConf.setProjectSettings(prjSettings);
//
//        SourceCodeSettings scSetting = new SourceCodeSettings();
//        scSetting.setSourceOrigin(SourceLocationType.SOURCE_CONTROL);
//
//        //Create scan path in GitHub definition
//        ScanPath scanPath = new ScanPath();
//        scanPath.setPath("/refs/heads/master");
//        scanPath.setIncludeSubTree(false);
//
//        ArrayOfScanPath arrayOfScanPaths = new ArrayOfScanPath();
//        arrayOfScanPaths.getScanPath().add(scanPath);
//
//        scSetting.setPathList(arrayOfScanPaths);
//
//        //Source control settings
//        SourceControlSettings srcControlSettings = new SourceControlSettings();
//        srcControlSettings.setRepository(RepositoryType.GIT);
//        srcControlSettings.setServerName("https://github.com/BearSpace/Ghost-automation.git");
//        srcControlSettings.setProtocol(SourceControlProtocolType.WINDOWS_AUTHENTICATION);
//        srcControlSettings.setUseSSL(false);
//        srcControlSettings.setUseSSH(false);
//        srcControlSettings.setGitLsViewType(GitLsRemoteViewType.TAGS_AND_HEADS);
//
//        Credentials credentials = new Credentials();
//        String username = "BearSpace";
//        String password = "buga696969";
//        credentials.setUser(username);
//        credentials.setPass(password);
//
//        GitHubIntegrationSettings gitHubSet = new GitHubIntegrationSettings();
//        gitHubSet.setEventsThreshold(4);
//        gitHubSet.setOwnerCredentials(credentials);
//        gitHubSet.setContributorCredentials(credentials);
//        srcControlSettings.setGitHubSettings(gitHubSet);
//        srcControlSettings.setGITBranch("master");
//
//        ScheduleSettings scheduleSettings = new ScheduleSettings();
//        scheduleSettings.setSchedule(ScheduleType.NOW);
//
//        scSetting.setSourceControlSetting(srcControlSettings);
//
//        prjConf.setScheduleSettings(scheduleSettings);
//        prjConf.setSourceCodeSettings(scSetting);
//
//        return prjConf;
//    }
//
//    //Change project configuration defined by GitHub source control
//    public static ProjectConfiguration genChangeProjectConfigurationGitHub(ProjectConfiguration projectConfiguration) {
//
//        String servernameOld = projectConfiguration.getSourceCodeSettings().getSourceControlSetting().getServerName();
//        projectConfiguration.getSourceCodeSettings().getSourceControlSetting().setServerName("https://github.com/BearSpace/sheltr-master-automation.git");
//        String servernameNew = projectConfiguration.getSourceCodeSettings().getSourceControlSetting().getServerName();
//
//        int eventsThresholdOld = projectConfiguration.getSourceCodeSettings().getSourceControlSetting().getGitHubSettings().getEventsThreshold();
//        projectConfiguration.getSourceCodeSettings().getSourceControlSetting().getGitHubSettings().setEventsThreshold(8);
//        int eventsThresholdNew = projectConfiguration.getSourceCodeSettings().getSourceControlSetting().getGitHubSettings().getEventsThreshold();
//        //Avoid scan to run immediately
//        projectConfiguration.getScheduleSettings().setSchedule(ScheduleType.NONE);
//        log.info(String.format("Server name changed from %s to %s \neventsThreshold changed from %d to %d", servernameOld, servernameNew, eventsThresholdOld, eventsThresholdNew));
//
//        return projectConfiguration;
//
//    }
//
//    //generate project configuration with TFS source control
//    public static ProjectConfiguration genProjectConfigurationTFS(String prjName, GetAssociatedGroupsListResponse ascGrpListRes) {
//
//        //Project configuration
//        ProjectConfiguration prConfig = new ProjectConfiguration();
//
//        //Project settings
//        ProjectSettings prSettings = new ProjectSettings();
//        String validId = ascGrpListRes.getGetAssociatedGroupsListResult().getGroupList().getGroup().get(0).getID();
//        prSettings.setAssociatedGroupID(validId);
//        prSettings.setProjectName(prjName);
//        prSettings.setPresetID(PRESET_ID);
//        prSettings.setScanConfigurationID(SCAN_CONFIGURATION_ID);
//
//        prConfig.setProjectSettings(prSettings);
//        //Source code settings
//        SourceCodeSettings srcSettings = new SourceCodeSettings();
//        srcSettings.setSourceOrigin(SourceLocationType.SOURCE_CONTROL);
//
//        prConfig.setSourceCodeSettings(srcSettings);
//
//        //Source control settings
//        SourceControlSettings srcControl = new SourceControlSettings();
//        srcControl.setRepository(RepositoryType.TFS);
//        srcControl.setPort(8080);
//        srcControl.setUseSSL(false);
//        srcControl.setUseSSH(false);
//        srcControl.setServerName(TFS_SERVERNAME);
//        srcControl.setGitLsViewType(GitLsRemoteViewType.TAGS);
//        srcControl.setProtocol(SourceControlProtocolType.WINDOWS_AUTHENTICATION);
//
//        Credentials credentials = new Credentials();
//        credentials.setUser(TFS_USERNAME);
//        credentials.setPass(TFS_PASS);
//        srcControl.setUserCredentials(credentials);
//
//        //Scan path
//        ScanPath scanPath = new ScanPath();
//        scanPath.setPath("/Test/BookStore.NET-branch1");
//        scanPath.setIncludeSubTree(false);
//        //Array of scan path
//        ArrayOfScanPath arrayOfScanPath = new ArrayOfScanPath();
//        arrayOfScanPath.getScanPath().add(scanPath);
//
//        srcSettings.setPathList(arrayOfScanPath);
//        srcSettings.setSourceControlSetting(srcControl);
//
//        ScheduleSettings scheduleSettings = new ScheduleSettings();
//        scheduleSettings.setSchedule(ScheduleType.NOW);
//        prConfig.setScheduleSettings(scheduleSettings);
//
//        return prConfig;
//
//    }
//
//    public static GetStatusOfSingleScan genGetStatusOfSingleScanReq(String sessionId, String runId) {
//
//        GetStatusOfSingleScan singleScan = new GetStatusOfSingleScan();
//        singleScan.setSessionID(sessionId);
//        singleScan.setRunId(runId);
//
//        return singleScan;
//    }
//
//    public static GetProjectsDisplayData genGetProjectsDisplayData(String sessionId) {
//        GetProjectsDisplayData req = new GetProjectsDisplayData();
//        req.setSessionID(sessionId);
//        return req;
//    }
//
//    public static DeleteProjects genDeleteSingleProject(String sessionId, long prjId) {
//        DeleteProjects req = new DeleteProjects();
//        req.setSessionID(sessionId);
//
//        ArrayOfLong arrayOfLong = new ArrayOfLong();
//        arrayOfLong.getLong().add(prjId);
//        req.setProjectIDs(arrayOfLong);
//
//        return req;
//    }
//
//    public static DeleteProjects genDeleteProjects(String sessionId, ArrayOfLong prjIds) {
//        DeleteProjects req = new DeleteProjects();
//        req.setSessionID(sessionId);
//        req.setProjectIDs(prjIds);
//        return req;
//    }
//
//    public static CancelScan genCancelScan(String sessionId, String runId) {
//        CancelScan cancelScanReq = new CancelScan();
//        cancelScanReq.setSessionID(sessionId);
//        cancelScanReq.setRunId(runId);
//        return cancelScanReq;
//    }
//
//    public static GetProjectConfiguration genGetProjectConfiguration(String sessionId, long projectId) {
//        GetProjectConfiguration prjConfig = new GetProjectConfiguration();
//        prjConfig.setSessionID(sessionId);
//        prjConfig.setProjectID(projectId);
//        return prjConfig;
//    }
//
//
//    public static DeleteScans genDeleteScans(String sessionId, List<Long> scanIds) {
//        ArrayOfLong arrOfLongs = new ArrayOfLong();
//        DeleteScans deleteScans = new DeleteScans();
//        deleteScans.setSessionID(sessionId);
//        for (Long scanId : scanIds) {
//            arrOfLongs.getLong().add(scanId);
//        }
//        deleteScans.setScanIDs(arrOfLongs);
//        return deleteScans;
//    }
//
//    public static UpdateProjectIncrementalConfiguration genUpdateProjectIncrementalConfiguration(String sessionId, long projectId, ProjectConfiguration prConfig) {
//        UpdateProjectIncrementalConfiguration updIncrConfig = new UpdateProjectIncrementalConfiguration();
//        updIncrConfig.setSessionID(sessionId);
//        updIncrConfig.setProjectID(projectId);
//        updIncrConfig.setProjectConfiguration(prConfig);
//        return updIncrConfig;
//    }
//
//    public static GetPresetList getPresetList(String sessionId) {
//        GetPresetList presetList = new GetPresetList();
//        presetList.setSessionID(sessionId);
//        return presetList;
//    }
//
//    public static GetConfigurationSetList genGetConfigurationSetList(String sessionId) {
//        GetConfigurationSetList confSetList = new GetConfigurationSetList();
//        confSetList.setSessionID(sessionId);
//        return confSetList;
//    }
//
//    public static UpdateScanComment genUpdateScanComment(String sessionId, long scanId, String comment) {
//        UpdateScanComment updComment = new UpdateScanComment();
//        updComment.setSessionID(sessionId);
//        updComment.setScanID(scanId);
//        updComment.setComment(comment);
//        return updComment;
//    }
}
