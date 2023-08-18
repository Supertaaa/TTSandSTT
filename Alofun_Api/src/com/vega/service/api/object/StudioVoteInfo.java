package com.vega.service.api.object;

public class StudioVoteInfo {
	private int recordId;
	private int receivedUserId;
	private String receiver;
	private int votedUserId;
	private String msisdn;
	private int point;
	private int voteCount;
	
	public int getRecordId() {
		return recordId;
	}
	public void setRecordId(int recordId) {
		this.recordId = recordId;
	}
	public int getReceivedUserId() {
		return receivedUserId;
	}
	public void setReceivedUserId(int receivedUserId) {
		this.receivedUserId = receivedUserId;
	}
	public String getReceiver() {
		return receiver;
	}
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	public int getVotedUserId() {
		return votedUserId;
	}
	public void setVotedUserId(int votedUserId) {
		this.votedUserId = votedUserId;
	}
	public String getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	public int getPoint() {
		return point;
	}
	public void setPoint(int point) {
		this.point = point;
	}
	/**
	 * @return the voteCount
	 */
	public int getVoteCount() {
		return voteCount;
	}
	/**
	 * @param voteCount the voteCount to set
	 */
	public void setVoteCount(int voteCount) {
		this.voteCount = voteCount;
	}
	
	
}
