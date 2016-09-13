package com.cx.plugin;

/**
 * Created by: Dorg.
 * Date: 22/08/2016.
 */
public class ScanConfiguration {

    private byte[] zippedSources;

    private String projectName;

    private String fullTeamPath;

    private String preset;

    private boolean isIncrementalScan;

    private String folderExclusions;

    private String fileExclusions;

    public byte[] getZippedSources() {
        return zippedSources;
    }

    public void setZippedSources(byte[] zippedSources) {
        this.zippedSources = zippedSources;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getFullTeamPath() {
        return fullTeamPath;
    }

    public void setFullTeamPath(String fullTeamPath) {
        this.fullTeamPath = fullTeamPath;
    }

    public String getPreset() {
        return preset;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public boolean isIncrementalScan() {
        return isIncrementalScan;
    }

    public void setIncrementalScan(boolean incrementalScan) {
        isIncrementalScan = incrementalScan;
    }

    public String getFolderExclusions() {
        return folderExclusions;
    }

    public void setFolderExclusions(String folderExclusions) {
        this.folderExclusions = folderExclusions;
    }

    public String getFileExclusions() {
        return fileExclusions;
    }

    public void setFileExclusions(String fileExclusions) {
        this.fileExclusions = fileExclusions;
    }
}
