package com.cx.client.rest.dto;

/**
 * Created by: Dorg.
 * Date: 09/10/2016.
 */
public class OSASummaryResults {

    private int totalLibraries;
    private int highVulnerabilityLibraries;
    private int mediumVulnerabilityLibraries;
    private int lowVulnerabilityLibraries;
    private int nonVulnerableLibraries;
    private int vulnerableAndUpdated;
    private int vulnerableAndOutdated;
    private String vulnerabilityScore;
    private int highVulnerabilities;
    private int mediumVulnerabilities;
    private int lowVulnerabilities;
    private String analyzeTime;

    public int getTotalLibraries() {
        return totalLibraries;
    }

    public void setTotalLibraries(int totalLibraries) {
        this.totalLibraries = totalLibraries;
    }

    public int getHighVulnerabilityLibraries() {
        return highVulnerabilityLibraries;
    }

    public void setHighVulnerabilityLibraries(int highVulnerabilityLibraries) {
        this.highVulnerabilityLibraries = highVulnerabilityLibraries;
    }

    public int getMediumVulnerabilityLibraries() {
        return mediumVulnerabilityLibraries;
    }

    public void setMediumVulnerabilityLibraries(int mediumVulnerabilityLibraries) {
        this.mediumVulnerabilityLibraries = mediumVulnerabilityLibraries;
    }

    public int getLowVulnerabilityLibraries() {
        return lowVulnerabilityLibraries;
    }

    public void setLowVulnerabilityLibraries(int lowVulnerabilityLibraries) {
        this.lowVulnerabilityLibraries = lowVulnerabilityLibraries;
    }

    public int getNonVulnerableLibraries() {
        return nonVulnerableLibraries;
    }

    public void setNonVulnerableLibraries(int nonVulnerableLibraries) {
        this.nonVulnerableLibraries = nonVulnerableLibraries;
    }

    public int getVulnerableAndUpdated() {
        return vulnerableAndUpdated;
    }

    public void setVulnerableAndUpdated(int vulnerableAndUpdated) {
        this.vulnerableAndUpdated = vulnerableAndUpdated;
    }

    public int getVulnerableAndOutdated() {
        return vulnerableAndOutdated;
    }

    public void setVulnerableAndOutdated(int vulnerableAndOutdated) {
        this.vulnerableAndOutdated = vulnerableAndOutdated;
    }

    public String getVulnerabilityScore() {
        return vulnerabilityScore;
    }

    public void setVulnerabilityScore(String vulnerabilityScore) {
        this.vulnerabilityScore = vulnerabilityScore;
    }

    public int getHighVulnerabilities() {
        return highVulnerabilities;
    }

    public void setHighVulnerabilities(int highVulnerabilities) {
        this.highVulnerabilities = highVulnerabilities;
    }

    public int getMediumVulnerabilities() {
        return mediumVulnerabilities;
    }

    public void setMediumVulnerabilities(int mediumVulnerabilities) {
        this.mediumVulnerabilities = mediumVulnerabilities;
    }

    public int getLowVulnerabilities() {
        return lowVulnerabilities;
    }

    public void setLowVulnerabilities(int lowVulnerabilities) {
        this.lowVulnerabilities = lowVulnerabilities;
    }

    public String getAnalyzeTime() {
        return analyzeTime;
    }

    public void setAnalyzeTime(String analyzeTime) {
        this.analyzeTime = analyzeTime;
    }
}
