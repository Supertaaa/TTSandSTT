package com.vega.service.api.object;

public class PackageInfo {

    public static final int RENEW_ALLOW = 1;
    public static final int RENEW_DENY = 0;
    private int packageId;
    private String packageName;
    private int subFee;
    private int timeLife;
    private int FreeMinutes;
    private int overFee;
    private int pendingLife;
    private int timeLifePromotion;
    private int promotionMode;
    private int freeMinutesPromotion;
    private int debitLife;
    private int overFeePending;
    private int allowRenew;

    public int getFreeMinutesPromotion() {
        return freeMinutesPromotion;
    }

    public void setFreeMinutesPromotion(int freeMinutesPromotion) {
        this.freeMinutesPromotion = freeMinutesPromotion;
    }

    public int getPromotionMode() {
        return promotionMode;
    }

    public void setPromotionMode(int promotionMode) {
        this.promotionMode = promotionMode;
    }

    public int getTimeLifePromotion() {
        return timeLifePromotion;
    }

    public void setTimeLifePromotion(int timeLifePromotion) {
        this.timeLifePromotion = timeLifePromotion;
    }

    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getSubFee() {
        return subFee;
    }

    public void setSubFee(int subFee) {
        this.subFee = subFee;
    }

    public int getTimeLife() {
        return timeLife;
    }

    public void setTimeLife(int timeLife) {
        this.timeLife = timeLife;
    }

    public int getFreeMinutes() {
        return FreeMinutes;
    }

    public void setFreeMinutes(int freeMinutes) {
        FreeMinutes = freeMinutes;
    }

    public int getOverFee() {
        return overFee;
    }

    public void setOverFee(int overFee) {
        this.overFee = overFee;
    }

    public int getPendingLife() {
        return pendingLife;
    }

    public void setPendingLife(int pendingLife) {
        this.pendingLife = pendingLife;
    }

    public int getDebitLife() {
        return debitLife;
    }

    public void setDebitLife(int debitLife) {
        this.debitLife = debitLife;
    }

    public int getOverFeePending() {
        return overFeePending;
    }

    public void setOverFeePending(int overFeePending) {
        this.overFeePending = overFeePending;
    }

    public void setAllowRenew(int allowRenew) {
        this.allowRenew = allowRenew;
    }

    public int getAllowRenew() {
        return allowRenew;
    }
}
