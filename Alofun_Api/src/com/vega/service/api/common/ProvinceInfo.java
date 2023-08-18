package com.vega.service.api.common;

public class ProvinceInfo {
	public static final String SEPERATOR = ",";
	
	private int provinceId;
	private String name;
	private String identifySign = "";
	private String fileName;
	private int region;
	
	public int getProvinceId() {
		return provinceId;
	}
	public void setProvinceId(int provinceId) {
		this.provinceId = provinceId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIdentifySign() {
		return identifySign;
	}
	public void setIdentifySign(String identifySign) {
		this.identifySign = identifySign;
	}
	
	public boolean findProvince(String sign){
		sign = SEPERATOR + sign + SEPERATOR;
                
		String fullIdentifySign = SEPERATOR + this.identifySign + SEPERATOR;
		
		return fullIdentifySign.toLowerCase().indexOf(sign.toLowerCase()) >= 0;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	/**
	 * @return the region
	 */
	public int getRegion() {
		return region;
	}
	/**
	 * @param region the region to set
	 */
	public void setRegion(int region) {
		this.region = region;
	}
}
