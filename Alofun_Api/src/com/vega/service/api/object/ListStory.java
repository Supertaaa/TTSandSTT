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
public class ListStory {

    @JsonProperty("old_stories")
    private StoryInfo[] oldStories;
    @JsonProperty("new_stories")
    private StoryInfo[] newStories;

    public StoryInfo[] getOldStories() {
        return oldStories;
    }

    public void setOldStories(StoryInfo[] oldStories) {
        this.oldStories = oldStories;
    }

    public StoryInfo[] getNewStories() {
        return newStories;
    }

    public void setNewStories(StoryInfo[] newStories) {
        this.newStories = newStories;
    }

}
