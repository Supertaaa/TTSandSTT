package com.vega.service.api.object;

import java.sql.Timestamp;

public class SMS {

    private String msisdn;
    private String mtContent;
    private String serviceNumber;
    private int status;
    private int type;
    private String source;
    private String moContent;
    private String moKeyword;
    private Timestamp moReceivedTime;
    private Timestamp mtSentTime;
    private int numberRetry = 0;
    private boolean haveMO;
    private int packageId = 0;
    private String smsId = "";
    private String action;
    private boolean brandName = false;
    private int priority = 10;
    private long delay;
    private int moId;


    public boolean isBrandName() {
        return brandName;
    }

    public void setBrandName(boolean brandName) {
        this.brandName = brandName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSmsId() {
        return smsId;
    }

    public void setSmsId(String smsId) {
        this.smsId = smsId;
    }

    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
    }

    public boolean isHaveMO() {
        return haveMO;
    }

    public void setHaveMO(boolean haveMO) {
        this.haveMO = haveMO;
    }

    public String getMtContent() {
        return mtContent;
    }

    public void setMtContent(String mtContent) {
        this.mtContent = mtContent;
    }

    public String getMoContent() {
        return moContent;
    }

    public void setMoContent(String moContent) {
        this.moContent = moContent;
    }

    public String getMoKeyword() {
        return moKeyword;
    }

    public void setMoKeyword(String moKeyword) {
        this.moKeyword = moKeyword;
    }

    public Timestamp getMoReceivedTime() {
        return moReceivedTime;
    }

    public void setMoReceivedTime(Timestamp moReceivedTime) {
        this.moReceivedTime = moReceivedTime;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public int getNumberRetry() {
        return numberRetry;
    }

    public void setNumberRetry(int numberRetry) {
        this.numberRetry = numberRetry;
    }

    public String getServiceNumber() {
        return serviceNumber;
    }

    public void setServiceNumber(String serviceNumber) {
        this.serviceNumber = serviceNumber;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Timestamp getMtSentTime() {
        return mtSentTime;
    }

    public void setMtSentTime(Timestamp mtSentTime) {
        this.mtSentTime = mtSentTime;
    }

        public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public long getDelay() {
        return delay;
    }

    public int getMoId() {
        return moId;
    }

    public void setMoId(int moId) {
        this.moId = moId;
    }
}
