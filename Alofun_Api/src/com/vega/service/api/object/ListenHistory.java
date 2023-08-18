package com.vega.service.api.object;

public class ListenHistory {

    public static final int CONTENT_TYPE_NEWS = 1;
    public static final String SEPERATOR = "-";
    private String msisdn;
    private int channelId;
    private String contentListened;
    // SMS Reminder
    private String musicListened;
    private String storyListened;
    private int status;
    private String lastestSentDate;
    private String musicSentDate;
    private String storySentDate;
    private int musicMtInDay;
    private int storyMtInDay;
    private int contentId ;
    private int duration;
    private int countReject;
    private int channelType;


    public int getMusicMtInDay() {
        return musicMtInDay;
    }

    public void setMusicMtInDay(int musicMtInDay) {
        this.musicMtInDay = musicMtInDay;
    }

    public int getStoryMtInDay() {
        return storyMtInDay;
    }

    public void setStoryMtInDay(int storyMtInDay) {
        this.storyMtInDay = storyMtInDay;
    }

    public String getLastestSentDate() {
        return lastestSentDate;
    }

    public void setLastestSentDate(String lastestSentDate) {
        this.lastestSentDate = lastestSentDate;
    }

    public String getMusicSentDate() {
        return musicSentDate;
    }

    public void setMusicSentDate(String musicSentDate) {
        this.musicSentDate = musicSentDate;
    }

    public String getStorySentDate() {
        return storySentDate;
    }

    public void setStorySentDate(String storySentDate) {
        this.storySentDate = storySentDate;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMusicListened() {
        return musicListened;
    }

    public void setMusicListened(String musicListened) {
        this.musicListened = musicListened;
    }

    public String getStoryListened() {
        return storyListened;
    }

    public void setStoryListened(String storyListened) {
        this.storyListened = storyListened;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getContentListened() {
        return contentListened;
    }

    public void setContentListened(String contentListened) {
        this.contentListened = contentListened;
    }

    /**
     * @return the contentId
     */
    public int getContentId() {
        return contentId;
    }

    /**
     * @param contentId the contentId to set
     */
    public void setContentId(int contentId) {
        this.contentId = contentId;
    }

    /**
     * @return the duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * @return the countReject
     */
    public int getCountReject() {
        return countReject;
    }

    /**
     * @param countReject the countReject to set
     */
    public void setCountReject(int countReject) {
        this.countReject = countReject;
    }

    public void setChannelType(int channelType) {
        this.channelType = channelType;
    }

    public int getChannelType() {
        return channelType;
    }
}
