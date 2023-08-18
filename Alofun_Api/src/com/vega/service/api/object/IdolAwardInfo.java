package com.vega.service.api.object;

import com.vega.service.api.common.Helper;

public class IdolAwardInfo {
	public static final int AWARD_TYPE_POINT = 1;
	public static final int AWARD_TYPE_CARD = 2;
	
	private int type;
	private int val;
	private int key;
	private int level = -1;
	
	private String msisdn;
	private String programName;
	private String awardName;
	
	public IdolAwardInfo(){
		
	}
	public IdolAwardInfo(String config){
		this.parse(config);
	}
	
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getVal() {
		return val;
	}
	public void setVal(int val) {
		this.val = val;
	}
	public int getKey() {
		return key;
	}
	public void setKey(int key) {
		this.key = key;
	}
	
	public boolean parse(String config){
		boolean ret = false;
		
		if(!Helper.isNull(config)){
			String[] parts = config.split(":");
			if(parts.length != 3){
				return ret;
			}
			
			int tmp = Helper.getInt(parts[0], 0);
			if(tmp <= 0){
				return ret;
			}
			
			this.level = tmp;
			
			if("diem".equalsIgnoreCase(parts[1].trim())){
				this.type = AWARD_TYPE_POINT;
				tmp = Helper.getInt(parts[2], 0);
				if(tmp <= 0 ){
					return ret;
				}
				this.val = tmp;
			}else if("the".equalsIgnoreCase(parts[1].trim())){
				this.type = AWARD_TYPE_CARD;
				tmp = Helper.getInt(parts[2], 0);
				if(tmp <= 0 ){
					return ret;
				}
				this.key = tmp;
			}else{
				return ret;
			}
			
			ret = true;
		}
		
		return ret;
	}

	/**
	 * @return the msisdn
	 */
	public String getMsisdn() {
		return msisdn;
	}

	/**
	 * @param msisdn the msisdn to set
	 */
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	/**
	 * @return the programName
	 */
	public String getProgramName() {
		return programName;
	}

	/**
	 * @param programName the programName to set
	 */
	public void setProgramName(String programName) {
		this.programName = programName;
	}

	/**
	 * @return the awardName
	 */
	public String getAwardName() {
		return awardName;
	}

	/**
	 * @param awardName the awardName to set
	 */
	public void setAwardName(String awardName) {
		this.awardName = awardName;
	}
}
