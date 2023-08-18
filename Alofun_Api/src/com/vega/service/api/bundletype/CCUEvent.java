package com.vega.service.api.bundletype;

import com.vega.vcs.event.Event;
import com.vega.vcs.event.Message;

public class CCUEvent extends Event {

    private String name;

    public CCUEvent() {
    }

    public CCUEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Class<? extends Message> getType() {
        return getClass();
    }
}
