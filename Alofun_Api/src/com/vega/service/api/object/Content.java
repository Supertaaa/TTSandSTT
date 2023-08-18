/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.object;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 *
 * @author PhongTom
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class Content {

    private String channelId;
    private String channelOrd;
    private String namePath;
    private String contentId;
    private String contentPath;
    private String contentOrd;
    private String codeRbt;
    private String duration;
    private String totalPart;
    private String topicId;
    private String partNumber;
    private String summaryPath;
    private String contentNameSlug;
    private String singerSlug;
    private String code;
    // Sport Math
    private String pathTeam1;
    private String pathTeam2;
    // Gift
    private String messagePath;
    private String sender;
    // new
    private String ID;
    private String path;
    private String partID;
    private String g_question_path;
    private String g_answer_keys;
    private String g_right_key;
    private String g_right_path;
    private String g_wrong_path;
    private String link_channel;
    private String liveshow_id;
    private String time_stream;
    private String path_intro;
    private String path_calendar;
    private String calendar_id;
    private String answerID;
    private String answerPath;

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessagePath() {
        return messagePath;
    }

    public void setMessagePath(String messagePath) {
        this.messagePath = messagePath;
    }

    public String getPathTeam1() {
        return pathTeam1;
    }

    public void setPathTeam1(String pathTeam1) {
        this.pathTeam1 = pathTeam1;
    }

    public String getPathTeam2() {
        return pathTeam2;
    }

    public void setPathTeam2(String pathTeam2) {
        this.pathTeam2 = pathTeam2;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getContentNameSlug() {
        return contentNameSlug;
    }

    public void setContentNameSlug(String contentNameSlug) {
        this.contentNameSlug = contentNameSlug;
    }

    public String getSingerSlug() {
        return singerSlug;
    }

    public void setSingerSlug(String singerSlug) {
        this.singerSlug = singerSlug;
    }

    public String getSummaryPath() {
        return summaryPath;
    }

    public void setSummaryPath(String summaryPath) {
        this.summaryPath = summaryPath;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getTopicId() {
        return topicId;
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public String getTotalPart() {
        return totalPart;
    }

    public void setTotalPart(String totalPart) {
        this.totalPart = totalPart;
    }

    public String getCodeRbt() {
        return codeRbt;
    }

    public void setCodeRbt(String codeRbt) {
        this.codeRbt = codeRbt;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getContentOrd() {
        return contentOrd;
    }

    public void setContentOrd(String contentOrd) {
        this.contentOrd = contentOrd;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelOrd() {
        return channelOrd;
    }

    public void setChannelOrd(String channelOrd) {
        this.channelOrd = channelOrd;
    }

    public String getNamePath() {
        return namePath;
    }

    public void setNamePath(String namePath) {
        this.namePath = namePath;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    /**
     * @return the ID
     */
    public String getID() {
        return ID;
    }

    /**
     * @param ID the ID to set
     */
    public void setID(String ID) {
        this.ID = ID;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the partID
     */
    public String getPartID() {
        return partID;
    }

    /**
     * @param partID the partID to set
     */
    public void setPartID(String partID) {
        this.partID = partID;
    }

    /**
     * @return the g_question_path
     */
    public String getG_question_path() {
        return g_question_path;
    }

    /**
     * @param g_question_path the g_question_path to set
     */
    public void setG_question_path(String g_question_path) {
        this.g_question_path = g_question_path;
    }

    /**
     * @return the g_answer_keys
     */
    public String getG_answer_keys() {
        return g_answer_keys;
    }

    /**
     * @param g_answer_keys the g_answer_keys to set
     */
    public void setG_answer_keys(String g_answer_keys) {
        this.g_answer_keys = g_answer_keys;
    }

    /**
     * @return the g_right_key
     */
    public String getG_right_key() {
        return g_right_key;
    }

    /**
     * @param g_right_key the g_right_key to set
     */
    public void setG_right_key(String g_right_key) {
        this.g_right_key = g_right_key;
    }

    /**
     * @return the g_right_path
     */
    public String getG_right_path() {
        return g_right_path;
    }

    /**
     * @param g_right_path the g_right_path to set
     */
    public void setG_right_path(String g_right_path) {
        this.g_right_path = g_right_path;
    }

    /**
     * @return the g_wrong_path
     */
    public String getG_wrong_path() {
        return g_wrong_path;
    }

    /**
     * @param g_wrong_path the g_wrong_path to set
     */
    public void setG_wrong_path(String g_wrong_path) {
        this.g_wrong_path = g_wrong_path;
    }

    public void setLink_channel(String link_channel) {
        this.link_channel = link_channel;
    }

    public String getLink_channel() {
        return link_channel;
    }

    public void setLiveshow_id(String liveshow_id) {
        this.liveshow_id = liveshow_id;
    }

    public String getLiveshow_id() {
        return liveshow_id;
    }

    public void setTime_stream(String time_stream) {
        this.time_stream = time_stream;
    }

    public String getTime_stream() {
        return time_stream;
    }

    public void setPath_intro(String path_intro) {
        this.path_intro = path_intro;
    }

    public String getPath_intro() {
        return path_intro;
    }

    public void setPath_calendar(String path_calendar) {
        this.path_calendar = path_calendar;
    }

    public String getPath_calendar() {
        return path_calendar;
    }

    public void setCalendar_id(String calendar_id) {
        this.calendar_id = calendar_id;
    }

    public String getCalendar_id() {
        return calendar_id;
    }

    public void setAnswerID(String answerID) {
        this.answerID = answerID;
    }

    public String getAnswerID() {
        return answerID;
    }

    public void setAnswerPath(String answerPath) {
        this.answerPath = answerPath;
    }

    public String getAnswerPath() {
        return answerPath;
    }
}
