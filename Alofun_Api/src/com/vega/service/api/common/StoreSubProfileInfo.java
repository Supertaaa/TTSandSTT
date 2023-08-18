package com.vega.service.api.common;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "SubProfileInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class StoreSubProfileInfo {
	private String msisdn;
    private int userId;
    private int sex;
    private int provinceId;
    private int job = -1;
    private String birthDay;
    private String introPath;
    private int status = 0;
    private String name;
    private int birthYear;
    private String source;
    private int telco = -1;
    private String updatedDate;
    private int bonusPointYear = 0;

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public int getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }

    public int getJob() {
        return job;
    }

    public void setJob(int job) {
        this.job = job;
    }

    public String getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(String birthDay) {
        if ("null".equalsIgnoreCase(birthDay)) {
            birthDay = "";
        }
        this.birthDay = birthDay;
    }

    public String getIntroPath() {
        return introPath;
    }

    public void setIntroPath(String introPath) {
        if ("null".equalsIgnoreCase(introPath)) {
            introPath = "";
        }
        this.introPath = introPath;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if ("null".equalsIgnoreCase(name)) {
            name = "";
        }
        this.name = name;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the telco
     */
    public int getTelco() {
        return telco;
    }

    /**
     * @param telco the telco to set
     */
    public void setTelco(int telco) {
        this.telco = telco;
    }

    /**
     * @return the bonusPointYear
     */
    public int getBonusPointYear() {
        return bonusPointYear;
    }

    /**
     * @param bonusPointYear the bonusPointYear to set
     */
    public void setBonusPointYear(int bonusPointYear) {
        this.bonusPointYear = bonusPointYear;
    }
}
