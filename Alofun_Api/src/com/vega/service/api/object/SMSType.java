package com.vega.service.api.object;

public enum SMSType {

    Genral(0),
    Register(1),
    Cancel(2),
    Renew(3),
    FindContent(4),
    Nofity(5),
    Download(6),
    WEB(7),
    WAP(8),
    Promotion(9) ;
    int value;

    public int getValue() {
        return value;
    }

    private SMSType(int val) {
        this.value = val;
    }
}
