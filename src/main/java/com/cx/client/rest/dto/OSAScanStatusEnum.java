package com.cx.client.rest.dto;

/**
 * Created by: Dorg.
 * Date: 06/10/2016.
 */
public enum OSAScanStatusEnum {

    NONE(0, "None"),
    IN_PROGRESS(1, "In progress"),
    FINISHED(2, "Finished"),
    FAILED(3 ,"Failed");

    private int num;
    private String uiValue;

    public int getNum() {
        return num;
    }

    public String uiValue() {
        return uiValue;
    }

    OSAScanStatusEnum(int i, String uiValue) {
        this.num = i;
        this.uiValue = uiValue;
    }
}
