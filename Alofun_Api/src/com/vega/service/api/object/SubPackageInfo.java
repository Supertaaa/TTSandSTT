package com.vega.service.api.object;

import java.util.Calendar;

public class SubPackageInfo {

    private BillingErrorCode errorCode;
    private String msisdn;
    private int subPackageId;
    private int packageId;
    private int freeMinutes;
    private SubPackageStatus status;
    private String updatedAt;
    private String regAt;
    private String expireAt;
    private PackagePromotionStatus promotion = PackagePromotionStatus.NO_PROMOTION;
    private PackageFunPromotionStatus funPromotion = PackageFunPromotionStatus.NO_PROMOTION;
    private int regFee;
    private boolean isExpired = false;
    private String sourceReg;
    private int renewSuccess;
    private boolean allowDebit = false;
    private int expireDay;
    private int updateDay;
    private int registerType;
    private String command;
    private String packageName;
    private int overFee;
    private int subFee;
    private int subFeeLevel2;
    private int nextChargeFee;
    private int debitFee;
    private boolean updateFeeOnly = false;
    // mini game
    private boolean isFirstCallInDay = false;
    private int promtCustomerCareOrd;
    private Calendar lastCallDate;
    private boolean  isBonusFirstCallInDay;
    private int totalPoint ;
    private int beforeTotalPoint;
    private boolean isHasPointBalance = false;
    private int renewed;



    public int getSubFee() {
        return subFee;
    }

    public void setSubFee(int subFee) {
        this.subFee = subFee;
    }

    public int getSubFeeLevel2() {
        return subFeeLevel2;
    }

    public void setSubFeeLevel2(int subFeeLevel2) {
        this.subFeeLevel2 = subFeeLevel2;
    }

    public int getNextChargeFee() {
        return nextChargeFee;
    }

    public void setNextChargeFee(int nextChargeFee) {
        this.nextChargeFee = nextChargeFee;
    }

    public int getDebitFee() {
        return debitFee;
    }

    public void setDebitFee(int debitFee) {
        this.debitFee = debitFee;
    }

    public boolean isUpdateFeeOnly() {
        return updateFeeOnly;
    }

    public void setUpdateFeeOnly(boolean updateFeeOnly) {
        this.updateFeeOnly = updateFeeOnly;
    }

    public int getOverFee() {
        return overFee;
    }

    public void setOverFee(int overFee) {
        this.overFee = overFee;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getRegisterType() {
        return registerType;
    }

    public void setRegisterType(int registerType) {
        this.registerType = registerType;
    }

    public int getUpdateDay() {
        return updateDay;
    }

    public void setUpdateDay(int updateDay) {
        this.updateDay = updateDay;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public int getSubPackageId() {
        return subPackageId;
    }

    public void setSubPackageId(int subPackageId) {
        this.subPackageId = subPackageId;
    }

    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
    }

    public int getFreeMinutes() {
        return freeMinutes;
    }

    public void setFreeMinutes(int freeMinutes) {
        this.freeMinutes = freeMinutes;
    }

    public String getRegAt() {
        return regAt;
    }

    public void setRegAt(String regAt) {
        this.regAt = regAt;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(String expireAt) {
        this.expireAt = expireAt;
    }

    public SubPackageStatus getStatus() {
        return status;
    }

    public void setStatus(SubPackageStatus packageStatus) {
        this.status = packageStatus;
    }

    public BillingErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(BillingErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public PackagePromotionStatus getPromotion() {
        return promotion;
    }

    public void setPromotion(PackagePromotionStatus promotion) {
        this.promotion = promotion;
    }

    public PackageFunPromotionStatus getFunPromotion() {
        return funPromotion;
    }

    public void setFunPromotion(PackageFunPromotionStatus funPromotion) {
        this.funPromotion = funPromotion;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getRegFee() {
        return regFee;
    }

    public void setRegFee(int regFee) {
        this.regFee = regFee;
    }

    public Boolean isExpired() {
        return isExpired;
    }

    public void setExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    public String getSourceReg() {
        return sourceReg;
    }

    public void setSourceReg(String sourceReg) {
        this.sourceReg = sourceReg;
    }

    public int getRenewSuccess() {
        return renewSuccess;
    }

    public void setRenewSuccess(int renewSuccess) {
        this.renewSuccess = renewSuccess;
    }

    public boolean isAllowDebit() {
        return allowDebit;
    }

    public void setAllowDebit(boolean allowDebit) {
        this.allowDebit = allowDebit;
    }

    public int getExpireDay() {
        return expireDay;
    }

    public void setExpireDay(int expireDay) {
        this.expireDay = expireDay;
    }  

    /**
     * @return the promtCustomerCareOrd
     */
    public int getPromtCustomerCareOrd() {
        return promtCustomerCareOrd;
    }

    /**
     * @param promtCustomerCareOrd the promtCustomerCareOrd to set
     */
    public void setPromtCustomerCareOrd(int promtCustomerCareOrd) {
        this.promtCustomerCareOrd = promtCustomerCareOrd;
    }

    /**
     * @return the lastCallDate
     */
    public Calendar getLastCallDate() {
        return lastCallDate;
    }

    /**
     * @param lastCallDate the lastCallDate to set
     */
    public void setLastCallDate(Calendar lastCallDate) {
        this.lastCallDate = lastCallDate;
    }

    /**
     * @return the isBonusFirstCallInDay
     */
    public boolean isIsBonusFirstCallInDay() {
        return isBonusFirstCallInDay;
    }

    /**
     * @param isBonusFirstCallInDay the isBonusFirstCallInDay to set
     */
    public void setIsBonusFirstCallInDay(boolean isBonusFirstCallInDay) {
        this.isBonusFirstCallInDay = isBonusFirstCallInDay;
    }

    /**
     * @return the totalPoint
     */
    public int getTotalPoint() {
        return totalPoint;
    }

    /**
     * @param totalPoint the totalPoint to set
     */
    public void setTotalPoint(int totalPoint) {
        this.totalPoint = totalPoint;
    }

    /**
     * @return the beforeTotalPoint
     */
    public int getBeforeTotalPoint() {
        return beforeTotalPoint;
    }

    /**
     * @param beforeTotalPoint the beforeTotalPoint to set
     */
    public void setBeforeTotalPoint(int beforeTotalPoint) {
        this.beforeTotalPoint = beforeTotalPoint;
    }

    /**
     * @return the isFirstCallInDay
     */
    public boolean isIsFirstCallInDay() {
        return isFirstCallInDay;
    }

    /**
     * @param isFirstCallInDay the isFirstCallInDay to set
     */
    public void setIsFirstCallInDay(boolean isFirstCallInDay) {
        this.isFirstCallInDay = isFirstCallInDay;
    }

    /**
     * @return the isHasPointBalance
     */
    public boolean isIsHasPointBalance() {
        return isHasPointBalance;
    }

    /**
     * @param isHasPointBalance the isHasPointBalance to set
     */
    public void setIsHasPointBalance(boolean isHasPointBalance) {
        this.isHasPointBalance = isHasPointBalance;
    }

    public int getRenewed() {
        return renewed;
    }

    public void setRenewed(int renewed) {
        this.renewed = renewed;
    }


    /**
     * Trang thai chu ki goi cuoc
     */
    public static enum SubPackageStatus {

        newsub(4), Active(1), Cancel(0);
        int value;

        public int getValue() {
            return value;
        }

        private SubPackageStatus(int val) {
            value = val;
        }
    }

    /**
     * Trang thai huong khuyen mai goi cuoc
     */
    public static enum PackagePromotionStatus {

        NO_PROMOTION(0), PROMOTION_ALL(1), PROMOTION_DAILY(2), PROMOTION_WEEKLY(3), PROMOTION_MONTHLY(4);
        int value;

        public int getValue() {
            return value;
        }

        private PackagePromotionStatus(int val) {
            value = val;
        }
    }

    /**
     * Trang thai huong khuyen mai goi cuoc
     */
    public static enum PackageFunPromotionStatus {

        NO_PROMOTION(0), PROMOTION_DAILY(1), PROMOTION_WEEKLY(2), PROMOTION_ALL(
                3), PROMOTION_MONTHY(4), PROMOTION_DAILY2000(5), PROMOTION_WEEKLY7000(6);
        int value;

        public int getValue() {
            return value;
        }

        private PackageFunPromotionStatus(int val) {
            value = val;
        }
    }
}
