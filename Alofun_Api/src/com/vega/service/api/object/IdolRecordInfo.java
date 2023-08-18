package com.vega.service.api.object;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "IdolRecordInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class IdolRecordInfo {
	public static final int STATUS_NORMAL = 1;
	public static final int STATUS_GOOD = 2;
	public static final int STATUS_BAD = 0;
	
	public static final int SECOND_ROUND_ON = 1;
	public static final int SECOND_ROUND_OFF = 0;
	
	public static final int TOP_STATUS_ON = 1;
	
	private int recordId;
	private int competitionId;
	private int userId;
	private String msisdn;
	private String recordPath;
	private int approveStatus;
	private int firstTopStatus;
	private int secondTopStatus;
	private int secondRoundStatus = SECOND_ROUND_OFF;
	private int quantityTop;
	private int listenCount;
	private int listenDuration;
	private int firstVoteCount;
	private int secondVoteCount;
	private String recordCode;
	private int quantityTop1;
	private int quantityTop2;
	private Date createdDate;
	
	private int listened = 0;
	
	private int totalVote = 0;
	private int totalPoint = 0;
	
	public int getRecordId() {
		return recordId;
	}
	public void setRecordId(int recordId) {
		this.recordId = recordId;
	}
	public int getCompetitionId() {
		return competitionId;
	}
	public void setCompetitionId(int competitionId) {
		this.competitionId = competitionId;
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
	public int getFirstTopStatus() {
		return firstTopStatus;
	}
	public void setFirstTopStatus(int firstTopStatus) {
		this.firstTopStatus = firstTopStatus;
	}
	public int getSecondTopStatus() {
		return secondTopStatus;
	}
	public void setSecondTopStatus(int secondTopStatus) {
		this.secondTopStatus = secondTopStatus;
	}
	public int getSecondRoundStatus() {
		return secondRoundStatus;
	}
	public void setSecondRoundStatus(int secondRoundStatus) {
		this.secondRoundStatus = secondRoundStatus;
	}
	public int getQuantityTop() {
		return quantityTop;
	}
	public void setQuantityTop(int quantityTop) {
		this.quantityTop = quantityTop;
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
	public int getFirstVoteCount() {
		return firstVoteCount;
	}
	public void setFirstVoteCount(int firstVoteCount) {
		this.firstVoteCount = firstVoteCount;
	}
	public int getSecondVoteCount() {
		return secondVoteCount;
	}
	public void setSecondVoteCount(int secondVoteCount) {
		this.secondVoteCount = secondVoteCount;
	}
	public String getRecordCode() {
		return recordCode;
	}
	public void setRecordCode(String recordCode) {
		this.recordCode = recordCode;
	}
	public int getQuantityTop1() {
		return quantityTop1;
	}
	public void setQuantityTop1(int quantityTop1) {
		this.quantityTop1 = quantityTop1;
	}
	public int getQuantityTop2() {
		return quantityTop2;
	}
	public void setQuantityTop2(int quantityTop2) {
		this.quantityTop2 = quantityTop2;
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
	 * @return the totalVote
	 */
	public int getTotalVote() {
		return totalVote;
	}
	/**
	 * @param totalVote the totalVote to set
	 */
	public void setTotalVote(int totalVote) {
		this.totalVote = totalVote;
	}
	
	
}
