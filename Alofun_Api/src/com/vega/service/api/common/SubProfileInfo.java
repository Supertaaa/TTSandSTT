package com.vega.service.api.common;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "SubProfileInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class SubProfileInfo {

    public static final String SEPERATOR = "-";

    private String msisdn;
    private int userId;
    private int sex;
    private int provinceId;
    private int job = -1;
    private String birthDay;
    private String introPath;
    private int lastStep;
    private int status;
    private String name;
    private int birthYear;
    private String hobby;
    private String source;
    private String provinceName;
    private int telco;
    private String updatedDate;
    private String createdDate;
    private long updatedTimestamp;
    private long createdTimestamp;
    private int errorCode;
    private int packageId;

    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
    }

    public int getSubPackageId() {
        return subPackageId;
    }

    public void setSubPackageId(int subPackageId) {
        this.subPackageId = subPackageId;
    }
    private int subPackageId;

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
        this.birthDay = birthDay;
    }

    public String getIntroPath() {
        return introPath;
    }

    public void setIntroPath(String introPath) {
        this.introPath = introPath;
    }

    public int getLastStep() {
        return lastStep;
    }

    public void setLastStep(int lastStep) {
        this.lastStep = lastStep;
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
        this.name = name;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    public String getHobby() {
        return hobby;
    }

    public void setHobby(String hobby) {
        this.hobby = hobby;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
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
     * @return the updatedDate
     */
    public String getUpdatedDate() {
        return updatedDate;
    }

    /**
     * @param updatedDate the updatedDate to set
     */
    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    /**
     * @return the createdDate
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate the createdDate to set
     */
    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @return the updatedTimestamp
     */
    public long getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    /**
     * @param updatedTimestamp the updatedTimestamp to set
     */
    public void setUpdatedTimestamp(long updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    /**
     * @return the createdTimestamp
     */
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    /**
     * @param createdTimestamp the createdTimestamp to set
     */
    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    /**
     * @return the errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }


}
