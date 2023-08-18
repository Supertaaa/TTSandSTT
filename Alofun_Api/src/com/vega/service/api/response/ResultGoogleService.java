package com.vega.service.api.response;

import lombok.NoArgsConstructor;

import java.util.List;

import com.vega.service.api.object.CustomDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;



@NoArgsConstructor
@AllArgsConstructor
@Getter 
@Setter

public class ResultGoogleService{
	
	private String province;
	private CustomDate date;
	private String music;
	private String origin;
	private String destination;
	private List<String> licencePlateByProvince;
	private List<String> provinceBylicencePlate;
	private String lat;
	private String lon;
	private List<String> route;
	
	public void setProvinceBylicencePlate(List<String> provinceBylicencePlate) {
		this.provinceBylicencePlate = provinceBylicencePlate;
	}
	
	public List<String> getProvinceBylicencePlate() {
		return provinceBylicencePlate;
	}
	
	public void setProvince(String province) {
		this.province = province;
	}
	
	public String getProvince() {
		return province;
	}
	
	
	public void setDate(CustomDate date) {
		this.date = date;
	}
	
	public CustomDate getDate() {
		return date;
	}
	
	
	public void setMusic(String music) {
		this.music = music;
	}
	
	public String getMusic() {
		return music;
	}
	
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public void setDestination(String destination) {
		this.destination = destination;
	}
	
	public String getDestination() {
		return destination;
	}
	
	public void setLicencePlateByProvince(List<String> licencePlate) {
		this.licencePlateByProvince = licencePlate;
	}
	
	public List<String> getLicencePlateByProvince() {
		return licencePlateByProvince;
	}
	
	public void setLat(String lat) {
		this.lat = lat;
	}
	
	public String getLat() {
		return lat;
	}
	
	public void setLon(String lon) {
		this.lon = lon;
	}
	
	public String getLon() {
		return lon;
	}
	
	public void setRoute(List<String> route) {
		this.route = route;
	}
	
	public List<String> getRoute() {
		return route;
	}
	
	
	

	
}