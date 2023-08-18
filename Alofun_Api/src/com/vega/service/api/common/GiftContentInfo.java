package com.vega.service.api.common;

import java.util.Calendar;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "GiftContentInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class GiftContentInfo {
	public static final int TOPIC_MUSIC = 1;
	public static final int TOPIC_IDOL = 2;
	public static final int TOPIC_STUDIO = 3;
	
	public static final int STATUS_WAITING = 0;
	public static final int STATUS_OK = 1;
	public static final int STATUS_RETRY = 2;
	public static final int STATUS_EXPIRED = 3;
	public static final int STATUS_REJECTED = 4;
	public static final int STATUS_DELETED = 5;
        
        //Nhac theo phan
        public static final int TOPIC_MUSIC_PART = 6;
        
	private int giftContentId;
	private String giftContentName;
	private String sender;
	private String receiver;
	private int contentId;
	private String contentCode;
	private String contentPath;
	private int topicType;
	private String messagePath;
	private String audioPath;
	private String source;
	private Calendar sendMTDate;
	private Calendar callDate;
	private int status;
	private int fee;
	private int telco = -1;
	private int toTelco = -1;
	private String msgContent;
	
	public int getTelco() {
		return telco;
	}
	public void setTelco(int telco) {
		this.telco = telco;
	}
	public int getToTelco() {
		return toTelco;
	}
	public void setToTelco(int toTelco) {
		this.toTelco = toTelco;
	}
	public String getMsgContent() {
		return msgContent;
	}
	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;
	}
	public int getGiftContentId() {
		return giftContentId;
	}
	public void setGiftContentId(int giftContentId) {
		this.giftContentId = giftContentId;
	}
	public String getGiftContentName() {
		return giftContentName;
	}
	public void setGiftContentName(String giftContentName) {
		this.giftContentName = giftContentName;
	}
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String getReceiver() {
		return receiver;
	}
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	public int getContentId() {
		return contentId;
	}
	public void setContentId(int contentId) {
		this.contentId = contentId;
	}
	public String getContentCode() {
		return contentCode;
	}
	public void setContentCode(String contentCode) {
		this.contentCode = contentCode;
	}
	public int getTopicType() {
		return topicType;
	}
	public void setTopicType(int topicType) {
		this.topicType = topicType;
	}
	public String getMessagePath() {
		return messagePath;
	}
	public void setMessagePath(String messagePath) {
		this.messagePath = messagePath;
	}
	public String getAudioPath() {
		return audioPath;
	}
	public void setAudioPath(String audioPath) {
		this.audioPath = audioPath;
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
	 * @return the sendMTDate
	 */
	public Calendar getSendMTDate() {
		return sendMTDate;
	}
	/**
	 * @param sendMTDate the sendMTDate to set
	 */
	public void setSendMTDate(Calendar sendMTDate) {
		this.sendMTDate = sendMTDate;
	}
	/**
	 * @return the callDate
	 */
	public Calendar getCallDate() {
		return callDate;
	}
	/**
	 * @param callDate the callDate to set
	 */
	public void setCallDate(Calendar callDate) {
		this.callDate = callDate;
	}
	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(int status) {
		this.status = status;
	}
	/**
	 * @return the contentPath
	 */
	public String getContentPath() {
		return contentPath;
	}
	/**
	 * @param contentPath the contentPath to set
	 */
	public void setContentPath(String contentPath) {
		this.contentPath = contentPath;
	}
	/**
	 * @return the fee
	 */
	public int getFee() {
		return fee;
	}
	/**
	 * @param fee the fee to set
	 */
	public void setFee(int fee) {
		this.fee = fee;
	}
}
