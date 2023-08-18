package com.vega.service.api.object;

public class StudioListenInfo {
	private int recordId;
	private int userId;
	private String msisdn;
	private int duration;
	private int channel;
	private int packageId;
	private int subPackageId;
	private int updateCounter;
	
	public int getRecordId() {
		return recordId;
	}
	public void setRecordId(int recordId) {
		this.recordId = recordId;
	}
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public String getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	public int getDuration() {
		return duration;
	}
	public void setDuration(int duration) {
		this.duration = duration;
	}
	public int getChannel() {
		return channel;
	}
	public void setChannel(int channel) {
		this.channel = channel;
	}
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
	/**
	 * @return the updateCounter
	 */
	public int getUpdateCounter() {
		return updateCounter;
	}
	/**
	 * @param updateCounter the updateCounter to set
	 */
	public void setUpdateCounter(int updateCounter) {
		this.updateCounter = updateCounter;
	}
	
	
}
