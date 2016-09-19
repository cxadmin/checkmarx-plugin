package com.cx.client.exception;

/**
 * Created by: Dorg.
 * Date: 15/09/2016.
 */
public class CxRestClientException extends CxClientException {

    private int statusCode;
    private String status;

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public CxRestClientException(int statusCode, String status) {
        this.statusCode = statusCode;
        this.status = status;
    }

    public CxRestClientException(String message, int statusCode, String status) {
        super(message);
        this.statusCode = statusCode;
        this.status = status;
    }

    public CxRestClientException(String message, Throwable cause, int statusCode, String status) {
        super(message, cause);
        this.statusCode = statusCode;
        this.status = status;
    }

    public CxRestClientException(Throwable cause) {
        super(cause);
    }
}
