package com.cx.client;

import com.checkmarx.v7.CxWSResponseScanStatus;
import com.cx.client.dto.CreateScanResponse;
import com.cx.client.dto.LocalScanConfiguration;
import com.cx.client.dto.ReportType;
import com.cx.client.dto.ScanResults;
import com.cx.client.exception.CxClientException;
import com.cx.client.rest.dto.CreateOSAScanResponse;
import com.cx.client.rest.dto.OSAScanStatus;
import com.cx.client.rest.dto.OSASummaryResults;

import java.io.File;

/**
 * Created by: Dorg.
 * Date: 18/09/2016.
 */
public interface CxClientService {

    void checkServerConnectivity() throws CxClientException;

    void loginToServer() throws CxClientException;

    CreateScanResponse createLocalScan(LocalScanConfiguration conf) throws CxClientException;

    CreateScanResponse createLocalScanResolveFields(LocalScanConfiguration conf) throws CxClientException;

    String resolveGroupIdFromTeamPath(String fullTeamPath);

    long resolvePresetIdFromName(String presetName);

    void waitForScanToFinish(String runId, ScanWaitHandler<CxWSResponseScanStatus> waitHandler) throws CxClientException;

    /**
     * @param runId
     * @param scanTimeoutInMin set scanTimeoutInMin to -1 for no timeout
     * @throws CxClientException
     */
    void waitForScanToFinish(String runId, long scanTimeoutInMin, ScanWaitHandler<CxWSResponseScanStatus> waitHandler) throws CxClientException;

    ScanResults retrieveScanResults(long projectId) throws CxClientException;

    CreateOSAScanResponse createOSAScan(long projectId, File zipFile) throws CxClientException;

    OSAScanStatus waitForOSAScanToFinish(String scanId, long scanTimeoutInMin, ScanWaitHandler<OSAScanStatus> waitHandler) throws CxClientException;

    OSASummaryResults retrieveOSAScanSummaryResults(long projectId) throws CxClientException;

    String retrieveOSAScanHtmlResults(long projectId) throws CxClientException;

    byte[] retrieveOSAScanPDFResults(long projectId) throws CxClientException;

    byte[] getScanReport(long scanId, ReportType reportType) throws CxClientException;

    void disableSSLCertificateVerification();

    void close();//todo implement

}
