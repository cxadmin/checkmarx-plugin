package com.cx.plugin;

import com.checkmarx.v7.CxWSReportRequest;
import com.checkmarx.v7.CxWSReportType;
import com.checkmarx.v7.GetStatusOfSingleScan;

/**
 * Created by: Dorg.
 * Date: 22/08/2016.
 */
public class ClientDataGen {


    public static GetStatusOfSingleScan genGetStatusOfSingleScan(String sessionId, String runId) {
        GetStatusOfSingleScan singleStat = new GetStatusOfSingleScan();
        singleStat.setSessionID(sessionId);
        singleStat.setRunId(runId);
        return singleStat;
    }

    public static CxWSReportRequest genReportRequest(long scanId, CxWSReportType type) {
        CxWSReportRequest ret = new CxWSReportRequest();
        ret.setScanID(scanId);
        ret.setType(type);
        return ret;
    }
}
