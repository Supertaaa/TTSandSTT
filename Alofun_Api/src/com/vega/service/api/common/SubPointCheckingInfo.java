package com.vega.service.api.common;

import java.sql.Timestamp;
import java.util.Calendar;

public class SubPointCheckingInfo {

    public static final int BEFORE_FLAG_NOT_REMIND = 1;
    public static final int BEFORE_FLAG_REMIND = 2;

    private int subPointOrd;
    private String msisdn;
    private int packageId;
    private int subPackageid;
    private int oldSubPackageId;
    private Timestamp regAt;
    private Timestamp lastCallDate;
    private Timestamp checkedCallDate;
    private Timestamp checkedUsingDate;
    private int remainCallMinutes;
    private int remainUsingDays;
    private int totalPoint;
    private int beforeTotalPoint;

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public int getSubPackageid() {
        return subPackageid;
    }

    public void setSubPackageid(int subPackageid) {
        this.subPackageid = subPackageid;
    }

    public int getOldSubPackageId() {
        return oldSubPackageId;
    }

    public void setOldSubPackageId(int oldSubPackageId) {
        this.oldSubPackageId = oldSubPackageId;
    }
   
    public int getRemainCallMinutes() {
        return remainCallMinutes;
    }

    public void setRemainCallMinutes(int remainCallMinutes) {
        this.remainCallMinutes = remainCallMinutes;
    }

    public int getTotalPoint() {
        return totalPoint;
    }

    public void setTotalPoint(int totalPoint) {
        this.totalPoint = totalPoint;
    }

    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
    }

    public int getRemainUsingDays() {
        return remainUsingDays;
    }

    public void setRemainUsingDays(int remainUsingDays) {
        this.remainUsingDays = remainUsingDays;
    }

    public int getBeforeTotalPoint() {
        return beforeTotalPoint;
    }

    public void setBeforeTotalPoint(int beforeTotalPoint) {
        this.beforeTotalPoint = beforeTotalPoint;
    }

    public int getSubPointOrd() {
        return subPointOrd;
    }

    public void setSubPointOrd(int subPointOrd) {
        this.subPointOrd = subPointOrd;
    }

    /**
     * @return the regAt
     */
    public Timestamp getRegAt() {
        return regAt;
    }

    /**
     * @param regAt the regAt to set
     */
    public void setRegAt(Timestamp regAt) {
        this.regAt = regAt;
    }

    /**
     * @return the lastCallDate
     */
    public Timestamp getLastCallDate() {
        return lastCallDate;
    }

    /**
     * @param lastCallDate the lastCallDate to set
     */
    public void setLastCallDate(Timestamp lastCallDate) {
        this.lastCallDate = lastCallDate;
    }

    /**
     * @return the checkedCallDate
     */
    public Timestamp getCheckedCallDate() {
        return checkedCallDate;
    }

    /**
     * @param checkedCallDate the checkedCallDate to set
     */
    public void setCheckedCallDate(Timestamp checkedCallDate) {
        this.checkedCallDate = checkedCallDate;
    }

    /**
     * @return the checkedUsingDate
     */
    public Timestamp getCheckedUsingDate() {
        return checkedUsingDate;
    }

    /**
     * @param checkedUsingDate the checkedUsingDate to set
     */
    public void setCheckedUsingDate(Timestamp checkedUsingDate) {
        this.checkedUsingDate = checkedUsingDate;
    }
}
