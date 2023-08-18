package com.vega.service.api.ws;

import java.util.List;
import java.util.logging.Handler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;

import org.apache.log4j.Logger;

public class WSServiceExecutor extends Thread {

    @Override
    public void run() {
        logger.info("WSService IS STARTING.....");
        logger.info("WSService URL: " + service.getServiceUrl());
        Endpoint e = Endpoint.create(this.service);        
        e.publish(this.service.getServiceUrl(), service);
        logger.info("WSService IS STARTED SUCCESSFULLY!");
    }
    static Logger logger = Logger.getLogger(WSServiceExecutor.class);
    private WSService service;

    public WSServiceExecutor(WSService ser) {
        this.service = ser;
    }

    public WSService getService() {
        return service;
    }

    public void setService(WSService service) {
        this.service = service;
    }
}
