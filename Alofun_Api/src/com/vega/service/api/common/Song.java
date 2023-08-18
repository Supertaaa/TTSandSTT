package com.vega.service.api.common;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "Song")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class Song {
	private String songId;
	private String beat;
	private String path;
	private String crbtCode;
	private String channelId;
	private String name;
	private String singer;
	private String namePath;
	private String partNumber;
	private String totalPart;
	
	public String getPath() {
		return path;
	}
	public void setPath(String songPath) {
		if(songPath != null && !songPath.startsWith("http://"))
		this.path = songPath;
	}
	public String getBeat() {
		return beat;
	}
	public void setBeat(String beat) {
		this.beat = beat;
	}
	public String getCrbtCode() {
		return crbtCode;
	}
	public void setCrbtCode(String crbtCode) {
		this.crbtCode = crbtCode;
	}
	public String getChannelId() {
		return channelId;
	}
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSinger() {
		return singer;
	}
	public void setSinger(String singer) {
		this.singer = singer;
	}
	public String getNamePath() {
		return namePath;
	}
	public void setNamePath(String namePath) {
		this.namePath = namePath;
	}
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public String getTotalPart() {
		return totalPart;
	}
	public void setTotalPart(String totalPart) {
		this.totalPart = totalPart;
	}
	public String getSongId() {
		return songId;
	}
	public void setSongId(String songId) {
		this.songId = songId;
	}
	
	
}
