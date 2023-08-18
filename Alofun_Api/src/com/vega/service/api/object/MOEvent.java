package com.vega.service.api.object;

public class MOEvent {

    String msisdn;
    String shortcode;
    String msg;
    private int moId;

    public MOEvent() {
    }

    public MOEvent(String msg, String msisdn, String shortcode, int moId) {
        this.msg = msg;
        this.msisdn = msisdn;
        this.shortcode = shortcode;
        this.moId = moId;
    }

    public String getMsisdn() {
        return this.msisdn;
    }

    public String getShortCode() {
        return this.shortcode;
    }

    public String getMessage() {
        return this.msg;
    }

    public int getMoId() {
        return moId;
    }

    public void setMoId(int moId) {
        this.moId = moId;
    }
}
