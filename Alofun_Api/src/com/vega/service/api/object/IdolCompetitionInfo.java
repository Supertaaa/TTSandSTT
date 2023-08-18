package com.vega.service.api.object;

import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "IdolCompetitionInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class IdolCompetitionInfo implements Cloneable {
	public static final int STATUS_ON = 1;
	public static final int STATUS_OFF = 0;

	public static final int RULE_SORT_1 = 1;
	public static final int RULE_SORT_2 = 2;

	private int competitionId;
	private Date firstRoundStart;
	private Date firstRoundEnd;
	private Date secondRoundStart;
	private Date secondRoundEnd;
	private int maxRecordFirstRound;
	private int maxRecordSecondRound;
	private int pointVoteFirstRound;
	private int pointVoteSecondRound;
	private String promtPause;
	private int ruleKey1FirstRound;
	private int ruleKey2FirstRound;
	private int ruleKey3FirstRound;
	private int ruleKey1SecondRound;
	private int ruleKey2SecondRound;

	private int maxRecord;
	private int roundNo;
	private String promtSubject = "";
	/**
	 * Promt trong bang idol_subject, File duoc phat khi vao nhanh thu am
	 */
	private String promptRecording;

	public int getCompetitionId() {
		return competitionId;
	}

	public void setCompetitionId(int competitionId) {
		this.competitionId = competitionId;
	}

	public Date getFirstRoundStart() {
		return firstRoundStart;
	}

	public void setFirstRoundStart(Date firstRoundStart) {
		this.firstRoundStart = firstRoundStart;
	}

	public Date getFirstRoundEnd() {
		return firstRoundEnd;
	}

	public void setFirstRoundEnd(Date firstRoundEnd) {
		this.firstRoundEnd = firstRoundEnd;
	}

	public Date getSecondRoundStart() {
		return secondRoundStart;
	}

	public void setSecondRoundStart(Date secondRoundStart) {
		this.secondRoundStart = secondRoundStart;
	}

	public Date getSecondRoundEnd() {
		return secondRoundEnd;
	}

	public void setSecondRoundEnd(Date secondRoundEnd) {
		this.secondRoundEnd = secondRoundEnd;
	}

	public int getMaxRecordFirstRound() {
		return maxRecordFirstRound;
	}

	public void setMaxRecordFirstRound(int maxRecordFirstRound) {
		this.maxRecordFirstRound = maxRecordFirstRound;
	}

	public int getMaxRecordSecondRound() {
		return maxRecordSecondRound;
	}

	public void setMaxRecordSecondRound(int maxRecordSecondRound) {
		this.maxRecordSecondRound = maxRecordSecondRound;
	}

	public int getPointVoteFirstRound() {
		return pointVoteFirstRound;
	}

	public void setPointVoteFirstRound(int pointVoteFirstRound) {
		this.pointVoteFirstRound = pointVoteFirstRound;
	}

	public int getPointVoteSecondRound() {
		return pointVoteSecondRound;
	}

	public void setPointVoteSecondRound(int pointVoteSecondRound) {
		this.pointVoteSecondRound = pointVoteSecondRound;
	}

	public String getPromtPause() {
		return promtPause;
	}

	public void setPromtPause(String promtPause) {
		this.promtPause = promtPause;
	}

	public int getRuleKey1FirstRound() {
		return ruleKey1FirstRound;
	}

	public void setRuleKey1FirstRound(int ruleKey1FirstRound) {
		this.ruleKey1FirstRound = ruleKey1FirstRound;
	}

	public int getRuleKey2FirstRound() {
		return ruleKey2FirstRound;
	}

	public void setRuleKey2FirstRound(int ruleKey2FirstRound) {
		this.ruleKey2FirstRound = ruleKey2FirstRound;
	}

	public int getRuleKey3FirstRound() {
		return ruleKey3FirstRound;
	}

	public void setRuleKey3FirstRound(int ruleKey3FirstRound) {
		this.ruleKey3FirstRound = ruleKey3FirstRound;
	}

	public int getRuleKey1SecondRound() {
		return ruleKey1SecondRound;
	}

	public void setRuleKey1SecondRound(int ruleKey1SecondRound) {
		this.ruleKey1SecondRound = ruleKey1SecondRound;
	}

	public int getRuleKey2SecondRound() {
		return ruleKey2SecondRound;
	}

	public void setRuleKey2SecondRound(int ruleKey2SecondRound) {
		this.ruleKey2SecondRound = ruleKey2SecondRound;
	}

	public int getMaxRecord() {
		return maxRecord;
	}

	public void setMaxRecord(int maxRecord) {
		this.maxRecord = maxRecord;
	}

	public int getRoundNo() {
		return roundNo;
	}

	public void setRoundNo(int roundNo) {
		this.roundNo = roundNo;
	}

	public String getPromtSubject() {
		return promtSubject;
	}

	public void setPromtSubject(String promtSubject) {
		this.promtSubject = promtSubject;
	}

	public String getPromptRecording() {
		return promptRecording;
	}

	public void setPromptRecording(String promptRecording) {
		this.promptRecording = promptRecording;
	}

	public boolean isExpired() {
		if (secondRoundEnd == null)
			return false;

		boolean ret = false;

		Calendar currentDate = Calendar.getInstance();
		Calendar secondEnd = Calendar.getInstance();
		secondEnd.setTime(secondRoundEnd);
		if (secondEnd.before(currentDate)) {
			ret = true;
		}

		return ret;
	}

	public boolean isPause() {
		if (firstRoundEnd == null)
			return false;

		boolean ret = false;

		Calendar currentDate = Calendar.getInstance();
		Calendar firstEnd = Calendar.getInstance();
		Calendar secondStart = Calendar.getInstance();
		firstEnd.setTime(firstRoundEnd);
		secondStart.setTime(secondRoundStart);
		if (currentDate.after(firstEnd) && currentDate.before(secondStart)) {
			ret = true;
		}

		return ret;
	}

	public int getCurrentRoundNo() {
		if (secondRoundStart == null || secondRoundEnd == null)
			return 0;

		int roundNo = 1;
		long currentTimestamp = System.currentTimeMillis();
		if (currentTimestamp >= secondRoundStart.getTime() && currentTimestamp <= secondRoundEnd.getTime()) {
			roundNo = 2;
		}

		return roundNo;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}

		return null;
	}
}
