package com.vega.service.api.common;

public class AwardInfo {
	public static final int STATUS_PUBLIC = 1;
	
	private int awardId;
	private String awardKey;
	private String awardName;
	private String awardNameSlug;
	private int pointExchange;
	private int freeMinutes;
	private int dayUsing;
	private int topupPrice;
	
	public int getAwardId() {
		return awardId;
	}
	public void setAwardId(int awardId) {
		this.awardId = awardId;
	}
	public String getAwardName() {
		return awardName;
	}
	public void setAwardName(String awardName) {
		this.awardName = awardName;
	}
	public String getAwardNameSlug() {
		return awardNameSlug;
	}
	public void setAwardNameSlug(String awardNameSlug) {
		this.awardNameSlug = awardNameSlug;
	}
	public String getAwardKey() {
		return awardKey;
	}
	public void setAwardKey(String awardKey) {
		this.awardKey = awardKey;
	}
	public int getPointExchange() {
		return pointExchange;
	}
	public void setPointExchange(int pointExchange) {
		this.pointExchange = pointExchange;
	}
	public int getFreeMinutes() {
		return freeMinutes;
	}
	public void setFreeMinutes(int freeMinutes) {
		this.freeMinutes = freeMinutes;
	}
	public int getDayUsing() {
		return dayUsing;
	}
	public void setDayUsing(int dayUsing) {
		this.dayUsing = dayUsing;
	}
	/**
	 * @return the topupPrice
	 */
	public int getTopupPrice() {
		return topupPrice;
	}
	/**
	 * @param topupPrice the topupPrice to set
	 */
	public void setTopupPrice(int topupPrice) {
		this.topupPrice = topupPrice;
	}
	
}
