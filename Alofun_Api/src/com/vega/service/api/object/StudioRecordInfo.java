package com.vega.service.api.object;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "StudioRecordInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class StudioRecordInfo {
	public static final int STATUS_APPROVED = 1;
	public static final int STATUS_NOT_APPROVE = 0;
	
	private int recordId;
	private int userId;
	private String msisdn;
	private String recordPath;
	private int approveStatus;
	private int listenCount;
	private int listenDuration;
	private int voteCount;
	private Date createdDate;
	private int exchangePoint;
	
	private int listened = 0;
	private int totalPoint = 0;
	
	private int rollbackPoint = 0;
	private int refundPoint = 0;
	private int isOwner = 0;
        private int competitionId = 0;
	
	public int getRecordId() {
		return recordId;
	}
	public void setRecordId(int recordId) {
		this.recordId = recordId;
	}
	
	public int getVoteCount() {
		return voteCount;
	}
	public void setVoteCount(int voteCount) {
		this.voteCount = voteCount;
	}
	public int getExchangePoint() {
		return exchangePoint;
	}
	public void setExchangePoint(int exchangePoint) {
		this.exchangePoint = exchangePoint;
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
	public String getRecordPath() {
		return recordPath;
	}
	public void setRecordPath(String recordPath) {
		this.recordPath = recordPath;
	}
	public int getApproveStatus() {
		return approveStatus;
	}
	public void setApproveStatus(int approveStatus) {
		this.approveStatus = approveStatus;
	}
	
	public int getListenCount() {
		return listenCount;
	}
	public void setListenCount(int listenCount) {
		this.listenCount = listenCount;
	}
	public int getListenDuration() {
		return listenDuration;
	}
	public void setListenDuration(int listenDuration) {
		this.listenDuration = listenDuration;
	}
	
	/**
	 * @return the listened
	 */
	public int getListened() {
		return listened;
	}
	/**
	 * @param listened the listened to set
	 */
	public void setListened(int listened) {
		this.listened = listened;
	}
	/**
	 * @return the createdDate
	 */
	public Date getCreatedDate() {
		return createdDate;
	}
	/**
	 * @param createdDate the createdDate to set
	 */
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
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
	 * @return the rollbackPoint
	 */
	public int getRollbackPoint() {
		return rollbackPoint;
	}
	/**
	 * @param rollbackPoint the rollbackPoint to set
	 */
	public void setRollbackPoint(int rollbackPoint) {
		this.rollbackPoint = rollbackPoint;
	}
	/**
	 * @return the refundPoint
	 */
	public int getRefundPoint() {
		return refundPoint;
	}
	/**
	 * @param refundPoint the refundPoint to set
	 */
	public void setRefundPoint(int refundPoint) {
		this.refundPoint = refundPoint;
	}
	/**
	 * @return the isOwner
	 */
	public int getIsOwner() {
		return isOwner;
	}
	/**
	 * @param isOwner the isOwner to set
	 */
	public void setIsOwner(int isOwner) {
		this.isOwner = isOwner;
	}

    /**
     * @return the competitionId
     */
    public int getCompetitionId() {
        return competitionId;
    }

    /**
     * @param competitionId the competitionId to set
     */
    public void setCompetitionId(int competitionId) {
        this.competitionId = competitionId;
    }
	
	
}
