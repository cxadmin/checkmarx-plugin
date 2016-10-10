package com.cx.client;

import com.checkmarx.v7.CurrentStatusEnum;
import com.checkmarx.v7.CxWSResponseScanStatus;
import com.cx.client.exception.CxClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by: Dorg.
 * Date: 28/09/2016.
 */
public class ConsoleScanWaitHandler implements ScanWaitHandler<CxWSResponseScanStatus> {

    private static final Logger log = LoggerFactory.getLogger(ConsoleScanWaitHandler.class);

    private long startTime;
    private long scanTimeoutInMin;

    public void onTimeout(CxWSResponseScanStatus scanStatus) throws CxClientException {

        String status =  scanStatus.getCurrentStatus() == null ? CurrentStatusEnum.UNKNOWN.value() : scanStatus.getCurrentStatus().value();
        throw new CxClientException("scan has reached the time limit ("+scanTimeoutInMin+" minutes). status: ["+ status +"]");

    }

    public void onFail(CxWSResponseScanStatus scanStatus) throws CxClientException {
        throw new CxClientException("scan cannot be completed. status ["+scanStatus.getCurrentStatus().value()+"].\n Stage message: ["+scanStatus.getStageMessage()+"]");

    }

    public void onIdle(CxWSResponseScanStatus scanStatus) {

        long hours = (System.currentTimeMillis() - startTime) / 3600000;
        long minutes = ((System.currentTimeMillis() - startTime) % 3600000) / 60000;
        long seconds = ((System.currentTimeMillis() - startTime) % 60000) / 1000;

        String hoursStr = (hours < 10)?("0" + Long.toString(hours)):(Long.toString(hours));
        String minutesStr = (minutes < 10)?("0" + Long.toString(minutes)):(Long.toString(minutes));
        String secondsStr = (seconds < 10)?("0" + Long.toString(seconds)):(Long.toString(seconds));

        log.info("Waiting for Results. " +
                "Time Elapsed: " + hoursStr + ":" + minutesStr + ":" + secondsStr + ". " +
                scanStatus.getTotalPercent() + "% Processed. " +
                "Status: " + scanStatus.getStageName() + ".");

    }

    public void onSuccess(CxWSResponseScanStatus scanStatus) {
        log.info("Scan Finished: " + scanStatus.getStageMessage());
    }

    public void onStart(long startTime, long scanTimeoutInMin) {
        this.startTime = startTime;
        this.scanTimeoutInMin = scanTimeoutInMin;
    }
}
