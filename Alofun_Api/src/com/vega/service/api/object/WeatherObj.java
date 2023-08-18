/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.object;

/**
 *
 * @author thangnh
 */
public class WeatherObj {
    private String errorCode;
    private String msisdn;
    private int duration;
    private int weather_content_id;
    private String content_name;
    private String content_sms;
    private String content_path;
    private int action;
    private String source;
    private int weather_region;
    private int province_id;

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
     * @return the duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * @return the weather_content_id
     */
    public int getWeather_content_id() {
        return weather_content_id;
    }

    /**
     * @param weather_content_id the weather_content_id to set
     */
    public void setWeather_content_id(int weather_content_id) {
        this.weather_content_id = weather_content_id;
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
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the weather_region
     */
    public int getWeather_region() {
        return weather_region;
    }

    /**
     * @param weather_region the weather_region to set
     */
    public void setWeather_region(int weather_region) {
        this.weather_region = weather_region;
    }

    /**
     * @return the province_id
     */
    public int getProvince_id() {
        return province_id;
    }

    /**
     * @param province_id the province_id to set
     */
    public void setProvince_id(int province_id) {
        this.province_id = province_id;
    }

    /**
     * @return the errorCode
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @return the content_name
     */
    public String getContent_name() {
        return content_name;
    }

    /**
     * @return the content_sms
     */
    public String getContent_sms() {
        return content_sms;
    }

    /**
     * @return the content_path
     */
    public String getContent_path() {
        return content_path;
    }

    /**
     * @param content_name the content_name to set
     */
    public void setContent_name(String content_name) {
        this.content_name = content_name;
    }

    /**
     * @param content_sms the content_sms to set
     */
    public void setContent_sms(String content_sms) {
        this.content_sms = content_sms;
    }

    /**
     * @param content_path the content_path to set
     */
    public void setContent_path(String content_path) {
        this.content_path = content_path;
    }
}
