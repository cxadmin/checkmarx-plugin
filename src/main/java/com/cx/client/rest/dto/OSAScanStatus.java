package com.cx.client.rest.dto;

/**
 * Created by: Dorg.
 * Date: 06/10/2016.
 */
public class OSAScanStatus {

    private OSAScanStatusEnum status;
    private String message;
    private String link;

    public OSAScanStatusEnum getStatus() {
        return status;
    }

    public void setStatus(OSAScanStatusEnum status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
