package org.slf4j.impl;


import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

/**
 * Created by: Dorg.
 * Date: 14/09/2016.
 */
public class MavenLoggerAdapter implements Logger {

    private static Log log = new DefaultLog(new ConsoleLogger(0, ""));

    protected String name;

    public static void setLogger(Log log) {
        MavenLoggerAdapter.log = log;
    }

    public MavenLoggerAdapter(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
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



    //--- ignoring marker (inherit from MarkerIgnoringBase causes error on some maven versions (3.2.5 - 3.3.9)) ---
    public boolean isTraceEnabled(Marker marker) {
        return this.isTraceEnabled();
    }

    public void trace(Marker marker, String msg) {
        this.trace(msg);
    }

    public void trace(Marker marker, String format, Object arg) {
        this.trace(format, arg);
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        this.trace((String)format, (Object)arg1, (Object)arg2);
    }

    public void trace(Marker marker, String format, Object... arguments) {
        this.trace(format, arguments);
    }

    public void trace(Marker marker, String msg, Throwable t) {
        this.trace(msg, t);
    }

    public boolean isDebugEnabled(Marker marker) {
        return this.isDebugEnabled();
    }

    public void debug(Marker marker, String msg) {
        this.debug(msg);
    }

    public void debug(Marker marker, String format, Object arg) {
        this.debug(format, arg);
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        this.debug((String)format, (Object)arg1, (Object)arg2);
    }

    public void debug(Marker marker, String format, Object... arguments) {
        this.debug(format, arguments);
    }

    public void debug(Marker marker, String msg, Throwable t) {
        this.debug(msg, t);
    }

    public boolean isInfoEnabled(Marker marker) {
        return this.isInfoEnabled();
    }

    public void info(Marker marker, String msg) {
        this.info(msg);
    }

    public void info(Marker marker, String format, Object arg) {
        this.info(format, arg);
    }

    public void info(Marker marker, String format, Object arg1, Object arg2) {
        this.info((String)format, (Object)arg1, (Object)arg2);
    }

    public void info(Marker marker, String format, Object... arguments) {
        this.info(format, arguments);
    }

    public void info(Marker marker, String msg, Throwable t) {
        this.info(msg, t);
    }

    public boolean isWarnEnabled(Marker marker) {
        return this.isWarnEnabled();
    }

    public void warn(Marker marker, String msg) {
        this.warn(msg);
    }

    public void warn(Marker marker, String format, Object arg) {
        this.warn(format, arg);
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        this.warn((String)format, (Object)arg1, (Object)arg2);
    }

    public void warn(Marker marker, String format, Object... arguments) {
        this.warn(format, arguments);
    }

    public void warn(Marker marker, String msg, Throwable t) {
        this.warn(msg, t);
    }

    public boolean isErrorEnabled(Marker marker) {
        return this.isErrorEnabled();
    }

    public void error(Marker marker, String msg) {
        this.error(msg);
    }

    public void error(Marker marker, String format, Object arg) {
        this.error(format, arg);
    }

    public void error(Marker marker, String format, Object arg1, Object arg2) {
        this.error((String)format, (Object)arg1, (Object)arg2);
    }

    public void error(Marker marker, String format, Object... arguments) {
        this.error(format, arguments);
    }

    public void error(Marker marker, String msg, Throwable t) {
        this.error(msg, t);
    }

    public String toString() {
        return this.getClass().getName() + "(" + this.getName() + ")";
    }
}
