/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vega.service.api.object;

/**
 *
 * @author thangnh
 */
public class LotteryHisDTO {
    /*
     * 1: Dang ky vung mien;
        2: Chuyen vung mien;
        3: Nghe ket qua;
        4: Nhan ket qua;
        5: Hen callout 1 ngay;
        6: Hen callout hang ngay;
        7: Nghe ket qua tra cuu;
        8: Phan tich xo so;
        9: Thong ke xo so;
     */
    public static final int ACT_REG_REGION = 1;
    public static final int ACT_SWITCH_REGION = 2;
    public static final int ACT_LISTEN_RESULT = 3;
    public static final int ACT_RECV_RESULT = 4;
    public static final int ACT_SETUP_RECV_BY_DAY = 5;
    public static final int ACT_SETUP_RECV_EVERY_DAY = 6;
    public static final int ACT_RECV_RESULT_BY_SEARCH = 7;
    public static final int ACT_ANALYZE = 8;
    public static final int ACT_STATISTICAL = 9;
    public static final int ACT_CANCEL_RECV_RESULT = 10;

    public static final int RES_SUCCESS = 0;
    public static final int RES_NOT_DATA = 1;
    public static final int RES_FAILED = 2;

    private String msisdn;
    private int action;
    private int provinceId;
    private String channel;
    private int result;
    private int region;

    /**
     * @return the msisdn
     */
    public String getMsisdn() {
        return msisdn;
    }

    /**
     * @param msisdn the msisdn to set
     */
    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    /**
     * @return the action
     */
    public int getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(int action) {
        this.action = action;
    }

    /**
     * @return the provinceId
     */
    public int getProvinceId() {
        return provinceId;
    }

    /**
     * @param provinceId the provinceId to set
     */
    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }

    /**
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @param channel the channel to set
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * @return the result
     */
    public int getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(int result) {
        this.result = result;
    }

    /**
     * @return the region
     */
    public int getRegion() {
        return region;
    }

    /**
     * @param region the region to set
     */
    public void setRegion(int region) {
        this.region = region;
    }

    
}
