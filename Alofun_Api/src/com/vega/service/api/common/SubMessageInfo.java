package com.vega.service.api.common;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "SubMessageInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class SubMessageInfo {
	private int messageId;
	private int idSender;
	private int idReceiver;
	private String voicePath;
	private String smsContent;
	private int messageType;
	private String sender;
	private String receiver;
	private int readed;
	private int pushStatus;
        private int fromTelco;
        private int toTelco;
	
	public int getMessageId() {
		return messageId;
	}
	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}
	public int getIdSender() {
		return idSender;
	}
	public void setIdSender(int idSender) {
		this.idSender = idSender;
	}
	public int getIdReceiver() {
		return idReceiver;
	}
	public void setIdReceiver(int idReceiver) {
		this.idReceiver = idReceiver;
	}
	public String getVoicePath() {
		return voicePath;
	}
	public void setVoicePath(String voicePath) {
		this.voicePath = voicePath;
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
	public int getReaded() {
		return readed;
	}
	public void setReaded(int readed) {
		this.readed = readed;
	}
	public int getPushStatus() {
		return pushStatus;
	}
	public void setPushStatus(int pushStatus) {
		this.pushStatus = pushStatus;
	}
	public int getMessageType() {
		return messageType;
	}
	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}
	public String getSmsContent() {
		return smsContent;
	}
	public void setSmsContent(String smsContent) {
		this.smsContent = smsContent;
	}

    /**
     * @return the fromTelco
     */
    public int getFromTelco() {
        return fromTelco;
    }

    /**
     * @param fromTelco the fromTelco to set
     */
    public void setFromTelco(int fromTelco) {
        this.fromTelco = fromTelco;
    }

    /**
     * @return the toTelco
     */
    public int getToTelco() {
        return toTelco;
    }

    /**
     * @param toTelco the toTelco to set
     */
    public void setToTelco(int toTelco) {
        this.toTelco = toTelco;
    }
	
	
}
