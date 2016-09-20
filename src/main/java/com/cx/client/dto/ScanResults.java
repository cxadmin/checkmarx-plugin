package com.cx.client.dto;

/**
 * Created by: Dorg.
 * Date: 15/09/2016.
 */
public class ScanResults {

    private long projectId;

    private long scanID;

    private int riskLevelScore;

    private int highSeverityResults;

    private int mediumSeverityResults;

    private int lowSeverityResults;

    private int infoSeverityResults;

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public long getScanID() {
        return scanID;
    }

    public void setScanID(long scanID) {
        this.scanID = scanID;
    }

    public int getRiskLevelScore() {
        return riskLevelScore;
    }

    public void setRiskLevelScore(int riskLevelScore) {
        this.riskLevelScore = riskLevelScore;
    }

    public int getHighSeverityResults() {
        return highSeverityResults;
    }

    public void setHighSeverityResults(int highSeverityResults) {
        this.highSeverityResults = highSeverityResults;
    }

    public int getMediumSeverityResults() {
        return mediumSeverityResults;
    }

    public void setMediumSeverityResults(int mediumSeverityResults) {
        this.mediumSeverityResults = mediumSeverityResults;
    }

    public int getLowSeverityResults() {
        return lowSeverityResults;
    }

    public void setLowSeverityResults(int lowSeverityResults) {
        this.lowSeverityResults = lowSeverityResults;
    }

    public int getInfoSeverityResults() {
        return infoSeverityResults;
    }

    public void setInfoSeverityResults(int infoSeverityResults) {
        this.infoSeverityResults = infoSeverityResults;
    }
}
