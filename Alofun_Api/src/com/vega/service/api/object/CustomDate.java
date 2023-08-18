package com.vega.service.api.object;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class CustomDate{
	
	private String day;
	private String month;
	
	public void setDay(String day) {
		this.day = day;
	}
	public void setMonth(String month) {
		this.month = month;
	}
	
	public String getDay() {
		return day;
	}
	
	public String getMonth() {
		return month;
	}
	
	
}