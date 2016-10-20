package com.cx.client;

import com.cx.client.exception.CxClientException;
import com.cx.client.rest.dto.OSAScanStatus;
import com.cx.client.rest.dto.OSAScanStatusEnum;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by: Dorg.
 * Date: 28/09/2016.
 */
public class OSAConsoleScanWaitHandler implements ScanWaitHandler<OSAScanStatus> {

    private static final Logger log = LoggerFactory.getLogger(OSAConsoleScanWaitHandler.class);

    private long startTime;
    private long scanTimeoutInMin;

    public void onTimeout(OSAScanStatus scanStatus) throws CxClientException {

        String status =  scanStatus.getStatus() == null ? OSAScanStatusEnum.NONE.uiValue() : scanStatus.getStatus().uiValue();
        throw new CxClientException("OSA scan has reached the time limit ("+scanTimeoutInMin+" minutes). status: ["+ status +"]");

    }

    public void onFail(OSAScanStatus scanStatus) throws CxClientException {
        throw new CxClientException("OSA scan cannot be completed. status ["+scanStatus.getStatus().uiValue()+"]. message: ["+StringUtils.defaultString(scanStatus.getMessage())+"]" );

    }

    public void onIdle(OSAScanStatus scanStatus) {

        long hours = (System.currentTimeMillis() - startTime) / 3600000;
        long minutes = ((System.currentTimeMillis() - startTime) % 3600000) / 60000;
        long seconds = ((System.currentTimeMillis() - startTime) % 60000) / 1000;

        String hoursStr = (hours < 10)?("0" + Long.toString(hours)):(Long.toString(hours));
        String minutesStr = (minutes < 10)?("0" + Long.toString(minutes)):(Long.toString(minutes));
        String secondsStr = (seconds < 10)?("0" + Long.toString(seconds)):(Long.toString(seconds));

        log.info("Waiting for OSA Scan Results. " +
                "Time Elapsed: " + hoursStr + ":" + minutesStr + ":" + secondsStr + ". " +
                "Status: " + scanStatus.getStatus().uiValue());

    }

    public void onSuccess(OSAScanStatus scanStatus) {
        log.info("OSA Scan Finished.");
    }

    public void onStart(long startTime, long scanTimeoutInMin) {
        this.startTime = startTime;
        this.scanTimeoutInMin = scanTimeoutInMin;
    }
}
