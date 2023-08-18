package com.vega.service.api.object;

import java.sql.Date;

public class IdolSubjectInfo {

	private int competitionId;
	private String subjectName;
	private Date begin_at;
	private Date end_at;
	private String pathPromt;
	private String promtRecording;

	public int getCompetitionId() {
		return competitionId;
	}

	public void setCompetitionId(int competitionId) {
		this.competitionId = competitionId;
	}

	public String getSubjectName() {
		return subjectName;
	}

	public void setSubjectName(String subjectName) {
		this.subjectName = subjectName;
	}

	public Date getBegin_at() {
		return begin_at;
	}

	public void setBegin_at(Date begin_at) {
		this.begin_at = begin_at;
	}

	public Date getEnd_at() {
		return end_at;
	}

	public void setEnd_at(Date end_at) {
		this.end_at = end_at;
	}

	public String getPathPromt() {
		return pathPromt;
	}

	public void setPathPromt(String pathPromt) {
		this.pathPromt = pathPromt;
	}

	public String getPromtRecording() {
		return promtRecording;
	}

	public void setPromtRecording(String promtRecording) {
		this.promtRecording = promtRecording;
	}

}
