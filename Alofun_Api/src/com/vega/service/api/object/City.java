package com.vega.service.api.object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@AllArgsConstructor
@NoArgsConstructor
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class City{
	
	private Integer code;
	private String name;
	private String lat;
	private String lon;
	
	public void setCode(Integer code) {
		this.code = code;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setLat(String lat) {
		this.lat = lat;
	}
	
	public void setLon(String lon) {
		this.lon = lon;
	}
	
	public Integer getCode() {
		return code;
	}
	
	public String getLat() {
		return lat;
	}
	
	public String getLon() {
		return lon;
	}
	
	public String getName() {
		return name;
	}

	
	
}