package com.cx.client;

import com.cx.client.exception.CxClientException;

/**
 * Created by: Dorg.
 * Date: 28/09/2016.
 */
public interface ScanWaitHandler<T> {

    void onStart(long startTime, long scanTimeoutInMin);

    void onIdle(T scanStatus) throws CxClientException;

    void onSuccess(T scanStatus);

    void onFail(T scanStatus) throws CxClientException;

    void onTimeout(T scanStatus) throws CxClientException;

}
