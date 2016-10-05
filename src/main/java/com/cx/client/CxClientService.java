package com.cx.client;

import com.cx.client.dto.CreateScanResponse;
import com.cx.client.dto.LocalScanConfiguration;
import com.cx.client.dto.ReportType;
import com.cx.client.dto.ScanResults;
import com.cx.client.exception.CxClientException;

/**
 * Created by: Dorg.
 * Date: 18/09/2016.
 */
public interface CxClientService {

    void loginToServer(String username, String password) throws CxClientException;

    CreateScanResponse createLocalScan(LocalScanConfiguration conf) throws CxClientException;

    CreateScanResponse createLocalScanResolveFields(LocalScanConfiguration conf) throws CxClientException;

    String resolveGroupIdFromTeamPath(String fullTeamPath);

    long resolvePresetIdFromName(String presetName);

    void waitForScanToFinish(String runId, ScanWaitHandler waitHandler) throws CxClientException;

    /**
     *
     * @param runId
     * @param scanTimeoutInMin set scanTimeoutInMin to -1 for no timeout
     * @throws CxClientException
     */
    void waitForScanToFinish(String runId, long scanTimeoutInMin, ScanWaitHandler waitHandler) throws CxClientException;

    ScanResults retrieveScanResults(long projectID) throws CxClientException;

    byte[] getScanReport(long scanId, ReportType reportType) throws CxClientException;

}
