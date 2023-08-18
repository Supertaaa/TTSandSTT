package com.vega.service.api.common;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "GiftAccountInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class GiftAccountInfo {
	public static final String SEPERATOR = "-";
	
	public static final int STATUS_RECEIVED = 1;
	public static final int STATUS_REJECT = 0;
	public static final int STATUS_NOT_FOUND = -1;
	
	private String msisdn;
        private int subPackageId;
	private int status = STATUS_NOT_FOUND;
	private int freeCount;
        private String subExpireAt;
	private String whiteList;
	private String blackList;
	
	public String getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getFreeCount() {
		return freeCount;
	}
	public void setFreeCount(int freeCount) {
		this.freeCount = freeCount;
	}
	public String getWhiteList() {
		return whiteList;
	}
	public void setWhiteList(String whiteList) {
		this.whiteList = whiteList;
	}
	public String getBlackList() {
		return blackList;
	}
	public void setBlackList(String blackList) {
		this.blackList = blackList;
	}

    /**
     * @return the subPackageId
     */
    public int getSubPackageId() {
        return subPackageId;
    }

    /**
     * @param subPackageId the subPackageId to set
     */
    public void setSubPackageId(int subPackageId) {
        this.subPackageId = subPackageId;
    }

    /**
     * @return the subExpireAt
     */
    public String getSubExpireAt() {
        return subExpireAt;
    }

    /**
     * @param subExpireAt the subExpireAt to set
     */
    public void setSubExpireAt(String subExpireAt) {
        this.subExpireAt = subExpireAt;
    }
}
