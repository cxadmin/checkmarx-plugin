package org.slf4j.impl;


import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * Created by: Dorg.
 * Date: 14/09/2016.
 */
public class MavenLoggerAdapter extends MarkerIgnoringBase {

    private static Log log = new DefaultLog(new ConsoleLogger(0, ""));

    public static void setLogger(Log log) {
        MavenLoggerAdapter.log = log;
    }

    public MavenLoggerAdapter(String name) {
        this.name = name;
    }


    public boolean isTraceEnabled() {
        return false;
    }

    public void trace(String s) {
    }

    public void trace(String s, Object o) {
    }

    public void trace(String s, Object o, Object o1) {
    }

    public void trace(String s, Object... objects) {
    }

    public void trace(String s, Throwable throwable) {
    }


    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    public void debug(String s) {
        log.debug(s);
    }

    public void debug(String s, Object o) {
        FormattingTuple ft = MessageFormatter.format(s, o);
        log.debug(ft.getMessage(), ft.getThrowable());
    }

    public void debug(String s, Object o, Object o1) {
        FormattingTuple ft = MessageFormatter.format(s, o, o1);
        log.debug(ft.getMessage(), ft.getThrowable());
    }

    public void debug(String s, Object... objects) {
        FormattingTuple ft = MessageFormatter.format(s, objects);
        log.debug(ft.getMessage(), ft.getThrowable());
    }

    public void debug(String s, Throwable throwable) {
        log.debug(s, throwable);
    }

    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    public void info(String s) {
        log.info(s);
    }

    public void info(String s, Object o) {
        FormattingTuple ft = MessageFormatter.format(s, o);
        log.info(ft.getMessage(), ft.getThrowable());
    }

    public void info(String s, Object o, Object o1) {
        FormattingTuple ft = MessageFormatter.format(s, o, o1);
        log.info(ft.getMessage(), ft.getThrowable());
    }

    public void info(String s, Object... objects) {
        FormattingTuple ft = MessageFormatter.format(s, objects);
        log.info(ft.getMessage(), ft.getThrowable());
    }

    public void info(String s, Throwable throwable) {
        log.info(s, throwable);
    }

    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    public void warn(String s) {
        log.warn(s);
    }

    public void warn(String s, Object o) {
        FormattingTuple ft = MessageFormatter.format(s, o);
        log.warn(ft.getMessage(), ft.getThrowable());
    }

    public void warn(String s, Object... objects) {
        FormattingTuple ft = MessageFormatter.format(s, objects);
        log.warn(ft.getMessage(), ft.getThrowable());
    }

    public void warn(String s, Object o, Object o1) {
        FormattingTuple ft = MessageFormatter.format(s, o, o1);
        log.warn(ft.getMessage(), ft.getThrowable());
    }

    public void warn(String s, Throwable throwable) {
        log.warn(s, throwable);
    }

    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    public void error(String s) {
        log.error(s);
    }

    public void error(String s, Object o) {
        FormattingTuple ft = MessageFormatter.format(s, o);
        log.error(ft.getMessage(), ft.getThrowable());
    }

    public void error(String s, Object o, Object o1) {
        FormattingTuple ft = MessageFormatter.format(s, o, o1);
        log.error(ft.getMessage(), ft.getThrowable());
    }

    public void error(String s, Object... objects) {
        FormattingTuple ft = MessageFormatter.format(s, objects);
        log.error(ft.getMessage(), ft.getThrowable());
    }

    public void error(String s, Throwable throwable) {
        log.error(s, throwable);
    }
}
