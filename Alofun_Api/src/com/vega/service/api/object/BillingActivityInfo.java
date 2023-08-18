package com.vega.service.api.object;

public class BillingActivityInfo {

    public static final Integer PROMOTION_FREE = 1;
    public static final Integer PROMOTION_NO = 0;

    public Integer getBillingActivityId() {
        return billingActivityId;
    }

    public void setBillingActivityId(Integer billingActivityId) {
        this.billingActivityId = billingActivityId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(Integer packageId) {
        this.packageId = packageId;
    }

    public Integer getSubPackageId() {
        return subPackageId;
    }

    public void setSubPackageId(Integer subPackageId) {
        this.subPackageId = subPackageId;
    }

    public Integer getBillingType() {
        return billingType;
    }

    public void setBillingType(Integer billingType) {
        this.billingType = billingType;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBillingAt() {
        return billingAt;
    }

    public void setBillingAt(String billingAt) {
        this.billingAt = billingAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPromotion() {
        return promotion;
    }

    public void setPromotion(Integer promotion) {
        this.promotion = promotion;
    }

    public String getBillingRequest() {
        return billingRequest;
    }

    public void setBillingRequest(String billingRequest) {
        this.billingRequest = billingRequest;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
    private Integer billingActivityId;
    private String requestId;
    private String msisdn;
    private Integer packageId;
    private Integer subPackageId;
    private Integer billingType;
    private Integer amount;
    private String result;
    private String source;
    private String billingAt;
    private String billingRequest;
    private String description;
    private Integer promotion = PROMOTION_NO;
    private String action;
}
