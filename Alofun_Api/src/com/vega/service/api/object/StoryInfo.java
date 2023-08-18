/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.object;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 *
 * @author ishop
 */
public class StoryInfo {

    @JsonProperty("story_id")
    private long storyId;
    @JsonProperty("part_id")
    private long partId;
    @JsonProperty("user_id")
    private long userId;
    @JsonProperty("last_use_time")
    private long lastUseTime;
    @JsonProperty("finish_part")
    private String finishPart = "true";
    @JsonProperty("dur_sec")
    private long durSec = 0;
    @JsonProperty("score")
    private double score;

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public long getStoryId() {
        return storyId;
    }

    public void setStoryId(long storyId) {
        this.storyId = storyId;
    }

    public long getPartId() {
        return partId;
    }

    public void setPartId(long partId) {
        this.partId = partId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(long lastUseTime) {
        this.lastUseTime = lastUseTime;
    }

    public String getFinishPart() {
        return finishPart;
    }

    public void setFinishPart(String finishPart) {
        this.finishPart = finishPart;
    }

    public long getDurSec() {
        return durSec;
    }

    public void setDurSec(long durSec) {
        this.durSec = durSec;
    }

}
