/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.object;

/**
 *
 * @author PhongTom
 */
public class User {

    private int code = BillingErrorCode.SystemError.getValue();
    private String msisdn;
    private boolean sendPass;
    private String password;
    private boolean sendGiftFree;
    private int giftFree;

    public int getGiftFree() {
        return giftFree;
    }

    public void setGiftFree(int giftFree) {
        this.giftFree = giftFree;
    }

    public boolean isSendGiftFree() {
        return sendGiftFree;
    }

    public void setSendGiftFree(boolean sendGiftFree) {
        this.sendGiftFree = sendGiftFree;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isSendPass() {
        return sendPass;
    }

    public void setSendPass(boolean sendPass) {
        this.sendPass = sendPass;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }
}
