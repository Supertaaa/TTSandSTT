package com.vega.service.api.config;

import com.vega.vcs.service.config.ConfigService;
import org.jconfig.Configuration;

import java.text.ParseException;
import java.util.Hashtable;

public class SMSConfig {

    private String smsChargeCommand;
    private int smsChargeFee;
    private boolean SMSChargingEnable;
    private int dailyPackageId;
    private int weeklyPackageId;
    private int weekly7000PackageId;
    private int monthyPackageId;
    private int daily2000PackageId;
    private Hashtable<String, String> regDailySyntax;
    private Hashtable<String, String> regWeeklySyntax;
    private Hashtable<String, String> regPromotionDailySyntax;
    private Hashtable<String, String> regPromotionWeeklySyntax;
    private int switchPackageConfirmHourExpired;

    public void loadConfig(ConfigService configService) throws ParseException {
        Configuration config = configService.getConfig("config", "api");

        setSMSChargingEnable(config.getBooleanProperty("SMSChargingEnable", false, "smscharging"));
        setSmsChargeCommand(config.getProperty("SMSChargeCommand", "MO", "smscharging"));
        setSmsChargeFee(config.getIntProperty("SMSChargeFee", 100, "smscharging"));

        setDailyPackageId(config.getIntProperty("DailyPackageId", 1, "smspackage"));
        setWeeklyPackageId(config.getIntProperty("WeeklyPackageId", 2, "smspackage"));
        setMonthyPackageId(config.getIntProperty("MonthyPackageId", 3, "smspackage"));
        setDaily2000PackageId(config.getIntProperty("Daily2000PackageId", 4, "smspackage"));
        setWeekly7000PackageId(config.getIntProperty("Weekly7000PackageId", 5, "smspackage"));

        regDailySyntax = new Hashtable<String, String>();
        regWeeklySyntax = new Hashtable<String, String>();
        String listRegDailySyntax = config.getProperty("RegDailySyntax", "DK:DK1:DKN:DK N", "smspackage");
        String listRegWeeklySyntax = config.getProperty("RegWeeklySyntax", "DK7:DKT:DK T", "smspackage");

        String[] parts = listRegDailySyntax.split(":");
        for (int i = 0; i < parts.length; i++) {
            regDailySyntax.put(parts[i].trim().toUpperCase(), parts[i]);
        }

        parts = listRegWeeklySyntax.split(":");
        for (int i = 0; i < parts.length; i++) {
            regWeeklySyntax.put(parts[i].trim().toUpperCase(), parts[i]);
        }

        regPromotionDailySyntax = new Hashtable<String, String>();
        regPromotionWeeklySyntax = new Hashtable<String, String>();
        String listRegPromDailySyntax = config.getProperty("RegPromotionDailySyntax", "", "smspackage");
        String listRegPromWeeklySyntax = config.getProperty("RegPromotionWeeklySyntax", "", "smspackage");

        parts = listRegPromDailySyntax.split(":");
        for (int i = 0; i < parts.length; i++) {
            regPromotionDailySyntax.put(parts[i].trim().toUpperCase(), parts[i]);
        }

        parts = listRegPromWeeklySyntax.split(":");
        for (int i = 0; i < parts.length; i++) {
            regPromotionWeeklySyntax.put(parts[i].trim().toUpperCase(), parts[i]);
        }

        setSwitchPackageConfirmHourExpired(config.getIntProperty("SwitchPackageConfirmHourExpired", 24, "smspackage"));
        // SMPP
        SMPPInfo.load(config);
    }

    public String getSmsChargeCommand() {
        return smsChargeCommand;
    }

    public void setSmsChargeCommand(String smsChargeCommand) {
        this.smsChargeCommand = smsChargeCommand;
    }

    public int getSmsChargeFee() {
        return smsChargeFee;
    }

    public void setSmsChargeFee(int smsChargeFee) {
        this.smsChargeFee = smsChargeFee;
    }

    public int getDailyPackageId() {
        return dailyPackageId;
    }

    public void setDailyPackageId(int dailyPackageId) {
        this.dailyPackageId = dailyPackageId;
    }

    public int getWeeklyPackageId() {
        return weeklyPackageId;
    }

    public void setWeeklyPackageId(int weeklyPackageId) {
        this.weeklyPackageId = weeklyPackageId;
    }

    public Hashtable<String, String> getRegDailySyntax() {
        return regDailySyntax;
    }

    public void setRegDailySyntax(Hashtable<String, String> regDailySyntax) {
        this.regDailySyntax = regDailySyntax;
    }

    public Hashtable<String, String> getRegWeeklySyntax() {
        return regWeeklySyntax;
    }

    public void setRegWeeklySyntax(Hashtable<String, String> regWeeklySyntax) {
        this.regWeeklySyntax = regWeeklySyntax;
    }

    public Hashtable<String, String> getRegPromotionDailySyntax() {
        return regPromotionDailySyntax;
    }

    public void setRegPromotionDailySyntax(Hashtable<String, String> regPromotionDailySyntax) {
        this.regPromotionDailySyntax = regPromotionDailySyntax;
    }

    public Hashtable<String, String> getRegPromotionWeeklySyntax() {
        return regPromotionWeeklySyntax;
    }

    public void setRegPromotionWeeklySyntax(Hashtable<String, String> regPromotionWeeklySyntax) {
        this.regPromotionWeeklySyntax = regPromotionWeeklySyntax;
    }

    public int getSwitchPackageConfirmHourExpired() {
        return switchPackageConfirmHourExpired;
    }

    public void setSwitchPackageConfirmHourExpired(
            int switchPackageConfirmHourExpired) {
        this.switchPackageConfirmHourExpired = switchPackageConfirmHourExpired;
    }

    public boolean isSMSChargingEnable() {
        return SMSChargingEnable;
    }

    public void setSMSChargingEnable(boolean sMSChargingEnable) {
        SMSChargingEnable = sMSChargingEnable;
    }

    /**
     * @return the weekly7000PackageId
     */
    public int getWeekly7000PackageId() {
        return weekly7000PackageId;
    }

    /**
     * @param weekly7000PackageId the weekly7000PackageId to set
     */
    public void setWeekly7000PackageId(int weekly7000PackageId) {
        this.weekly7000PackageId = weekly7000PackageId;
    }

    /**
     * @return the monthyPackageId
     */
    public int getMonthyPackageId() {
        return monthyPackageId;
    }

    /**
     * @param monthyPackageId the monthyPackageId to set
     */
    public void setMonthyPackageId(int monthyPackageId) {
        this.monthyPackageId = monthyPackageId;
    }

    /**
     * @return the daily2000PackageId
     */
    public int getDaily2000PackageId() {
        return daily2000PackageId;
    }

    /**
     * @param daily2000PackageId the daily2000PackageId to set
     */
    public void setDaily2000PackageId(int daily2000PackageId) {
        this.daily2000PackageId = daily2000PackageId;
    }
}
