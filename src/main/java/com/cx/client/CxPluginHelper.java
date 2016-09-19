package com.cx.client;

import com.checkmarx.v7.CliScanArgs;
import com.checkmarx.v7.CxClientType;
import com.checkmarx.v7.ProjectSettings;
import com.checkmarx.v7.ScanDisplayData;
import com.cx.client.dto.BaseScanConfiguration;
import com.cx.client.dto.ScanResults;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by: Dorg.
 * Date: 15/09/2016.
 */
public abstract class CxPluginHelper {

    private static final Logger log = LoggerFactory.getLogger(CxPluginHelper.class);

    public static final String HTML_TEMPLATE_LOCATION = "com/cx/plugin/htmlReportTemplate.html";


    public static ScanResults genScanResponse(ScanDisplayData scanDisplayData) {
        ScanResults ret = new ScanResults();
        ret.setProjectId(scanDisplayData.getProjectId());
        ret.setScanID(scanDisplayData.getScanID());
        ret.setHighSeverityResults(scanDisplayData.getHighSeverityResults());
        ret.setMediumSeverityResults(scanDisplayData.getMediumSeverityResults());
        ret.setLowSeverityResults(scanDisplayData.getLowSeverityResults());
        ret.setRiskLevelScore(scanDisplayData.getRiskLevelScore());

        return ret;
    }

    public static CliScanArgs genCliScanArgs(BaseScanConfiguration conf) {
        CliScanArgs cliScanArgs = new CliScanArgs();

        CxClientType cxClientType = CxClientType.SDK;
        try {
            cxClientType = CxClientType.valueOf(conf.getClientOrigin());
        } catch (Exception e) {
            log.debug("fail to convert client origin enum from value: {}. client origin set to SDK", conf.getClientOrigin());
        }
        cliScanArgs.setClientOrigin(cxClientType);
        cliScanArgs.setIsIncremental(conf.isIncrementalScan());
        cliScanArgs.setIsPrivateScan(conf.isPrivateScan());
        cliScanArgs.setIgnoreScanWithUnchangedCode(conf.isIgnoreScanWithUnchangedCode());
        cliScanArgs.setComment(conf.getComment());

        ProjectSettings prjSettings = new ProjectSettings();
        prjSettings.setProjectName(conf.getProjectName());
        prjSettings.setPresetID(conf.getPresetId());
        prjSettings.setAssociatedGroupID(conf.getGroupId());
        prjSettings.setDescription(conf.getDescription());
        prjSettings.setIsPublic(conf.isPublic());
        prjSettings.setOwner(conf.getOwner());
        prjSettings.setProjectID(conf.getProjectId());
        cliScanArgs.setPrjSettings(prjSettings);

        return cliScanArgs;
    }

    public static String compileHtmlReport(String csv, int highThreshold, int mediumThreshold, int lowThreshold) {

        String ret = null;
        ClassLoader classLoader = CxPluginHelper.class.getClassLoader();
        try {
            String htmlTemplate = IOUtils.toString(classLoader.getResourceAsStream(HTML_TEMPLATE_LOCATION), Charset.defaultCharset());
            htmlTemplate = htmlTemplate.replace("*RESULTS_CSV*", csv.replace("\uFEFF", "").replace("\'", "").replace("\"\"\"\"", "").replace("\r\n", "~~'+\n' "));
            ret = htmlTemplate.replace("*RESULTS_THRESHOLD*", highThreshold + "," + mediumThreshold + "," + lowThreshold);

        } catch (IOException e) {
            log.debug("fail to get html template from: " +HTML_TEMPLATE_LOCATION, e);
        } catch (Exception e) {
            log.debug("fail to compile html report. exception: ", e);
        }

        return ret;

    }

}
