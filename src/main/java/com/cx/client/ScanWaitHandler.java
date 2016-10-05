package com.cx.client;

import com.checkmarx.v7.CxWSResponseScanStatus;
import com.cx.client.exception.CxClientException;

/**
 * Created by: Dorg.
 * Date: 28/09/2016.
 */
public interface ScanWaitHandler {

    void onStart(long startTime, long scanTimeoutInMin);

    void onIdle(CxWSResponseScanStatus scanStatus) throws CxClientException;

    void onSuccess(CxWSResponseScanStatus scanStatus);

    void onFail(CxWSResponseScanStatus scanStatus) throws CxClientException;

    void onTimeout(CxWSResponseScanStatus scanStatus) throws CxClientException;

}
