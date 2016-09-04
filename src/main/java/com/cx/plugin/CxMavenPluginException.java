package com.cx.plugin;

/**
 * Created by: Dorg.
 * Date: 18/08/2016.
 */
public class CxMavenPluginException extends Exception {

    public CxMavenPluginException() {
        super();
    }

    public CxMavenPluginException(String message) {
        super(message);
    }

    public CxMavenPluginException(String message, Throwable cause) {
        super(message, cause);
    }

    public CxMavenPluginException(Throwable cause) {
        super(cause);
    }

    protected CxMavenPluginException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
