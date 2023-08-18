package com.vega.service.api.object;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "TopTenInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class TopTenInfo {
	private int id;
	private String contentPath;
	private String contentName;
	private int status;
	private int deleted;
	private Date publishedDate;
	private int createdId;
	private Date createdDate;
	private int updatedId;
	private Date updatedDate;
	/**
	 * Bản topten user chưa nghe lần nào trong vòng {@code x} ngày
	 */
	private boolean isNewRecord;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getContentPath() {
		return contentPath;
	}

	public void setContentPath(String contentPath) {
		this.contentPath = contentPath;
	}

	public String getContentName() {
		return contentName;
	}

	public void setContentName(String contentName) {
		this.contentName = contentName;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getDeleted() {
		return deleted;
	}

	public void setDeleted(int deleted) {
		this.deleted = deleted;
	}

	public Date getPublishedDate() {
		return publishedDate;
	}

	public void setPublishedDate(Date publishedDate) {
		this.publishedDate = publishedDate;
	}

	public int getCreatedId() {
		return createdId;
	}

	public void setCreatedId(int createdId) {
		this.createdId = createdId;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public int getUpdatedId() {
		return updatedId;
	}

	public void setUpdatedId(int updatedId) {
		this.updatedId = updatedId;
	}

	public Date getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Date updatedDate) {
		this.updatedDate = updatedDate;
	}

	public boolean isNewRecord() {
		return isNewRecord;
	}

	public void setNewRecord(boolean isNewRecord) {
		this.isNewRecord = isNewRecord;
	}

}
