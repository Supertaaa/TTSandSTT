package com.vega.service.api.object;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "TopTenHistoryInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
public class TopTenHistoryInfo {
	private int id;
	private int toptenRecordId;
	private String msisdn;
	private int subPackageId;
	private int packageId;
	private int duration;
	private Date created_time;
	private int orderInUserList;
	private Date start_date;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getToptenRecordId() {
		return toptenRecordId;
	}

	public void setToptenRecordId(int toptenRecordId) {
		this.toptenRecordId = toptenRecordId;
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

	public int getPackageId() {
		return packageId;
	}

	public void setPackageId(int packageId) {
		this.packageId = packageId;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public Date getCreated_time() {
		return created_time;
	}

	public void setCreated_time(Date created_time) {
		this.created_time = created_time;
	}

	public int getOrderInUserList() {
		return orderInUserList;
	}

	public void setOrderInUserList(int orderInUserList) {
		this.orderInUserList = orderInUserList;
	}

	public Date getStart_date() {
		return start_date;
	}

	public void setStart_date(Date start_date) {
		this.start_date = start_date;
	}

}
