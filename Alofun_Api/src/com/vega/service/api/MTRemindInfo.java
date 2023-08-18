/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vega.service.api;

/**
 *
 * @author Administrator
 */
public class MTRemindInfo {
	public static final int STATUS_SENT = 1;
	public static final int STATUS_NOT_SEND = 0;
	
	private int mtRemindKey;
	private String msisdn;
	private int subPackageId;
	private String mtContent;
	private int mtType;
	private int status = STATUS_NOT_SEND;
	
	public MTRemindInfo(){
		
	}
	
	public MTRemindInfo(String msisdn, int subPackageId, String mt, int type){
		this.msisdn = msisdn;
		this.subPackageId = subPackageId;
		this.mtContent = mt;
		this.mtType = type;
	}
	
	public String getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	public int getSubPackageId() {
		return subPackageId;
	}
	public void setSubPackageId(int subPackageId) {
		this.subPackageId = subPackageId;
	}
	public String getMtContent() {
		return mtContent;
	}
	public void setMtContent(String mtContent) {
		this.mtContent = mtContent;
	}
	public int getMtType() {
		return mtType;
	}
	public void setMtType(int mtType) {
		this.mtType = mtType;
	}

	public int getMtRemindKey() {
		return mtRemindKey;
	}

	public void setMtRemindKey(int mtRemindKey) {
		this.mtRemindKey = mtRemindKey;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}