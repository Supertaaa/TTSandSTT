package com.vega.service.api;

import com.vega.service.api.bundletype.ApiInterface;
import com.vega.service.api.bundletype.BirthDayPointEvent;
import com.vega.service.api.bundletype.CCUEvent;
import com.vega.service.api.bundletype.CheckingPointEvent;
import com.vega.service.api.bundletype.SendSubMessageEvent;
import com.vega.service.api.common.Helper;
import org.apache.log4j.Logger;

import com.vega.vcs.engine.AbstractBundle;
import com.vega.vcs.peer.event.MasterEvent;
import com.vega.vcs.peer.event.SlaveEvent;
import java.util.HashMap;

public class RestfulRA extends AbstractBundle implements ApiInterface {

    static Logger logger = Logger.getLogger(RestfulRA.class);
    RestfulStack stack;
    boolean isMaster = false;

    public RestfulRA() {
    }

    @Override
    public String onCommand(String command, HashMap<String, String> args) {
        logger.info("Command:" + command);
        if (!Helper.isNull(command)) {
            if (command.equalsIgnoreCase("reload_config")) {
                try {
                    stack.refreshConfig();
                    return "Success";
                } catch (Exception ex) {
                    logger.error(ex);
                }
            } else if (command.equalsIgnoreCase("reload_package")) {
                stack.refreshListPackage();
                return "Success";
            }
        }
        return "Unsuccess";
    }

    @Override
    public void register() {
        try {
            this.registerEvent(MasterEvent.class);
            this.registerEvent(SlaveEvent.class);
            this.registerEvent(CCUEvent.class);
            this.registerEvent(SendSubMessageEvent.class);
            this.registerEvent(CheckingPointEvent.class);
            this.registerEvent(BirthDayPointEvent.class);
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    @Override
    public boolean reload() {
        return stack.reload();
    }

    @Override
    public boolean start() {
        try {
            if (stack == null) {
                stack = new RestfulStack(this);
            }         
            return stack.start();
        } catch (Exception ex) {
            logger.error(ex);
            return false;
        }
    }

    @Override
    public boolean stop() {
        try {
            return stack.stop();
        } catch (Exception ex) {
            logger.error(ex.getStackTrace());
            return false;
        }
    }

    public void onMasterEvent(MasterEvent event) {
        logger.info("MasterEvent is called");
        isMaster = true;
        stack.setIsMaster(isMaster);
    }

    public void onSlaveEvent(SlaveEvent event) {
        logger.info("SlaveEvent is called");
        isMaster = false;
        stack.setIsMaster(isMaster);
    }

    public void onCCUEvent(CCUEvent event) {
        logger.info("Billing CCUEvent is called");
        stack.onCCUEvent();
    }

    public void onSendSubMessageEvent(SendSubMessageEvent event) {
        logger.info("restful onSendSubMessage is called");
        stack.sendSubMessage();
    }

    public void onCheckingPointEvent(CheckingPointEvent event) {
        logger.info("restful checkingPointEvent is called");
        stack.checkingPointEvent();
    }

     public void onBirthDayPointEvent(BirthDayPointEvent event) {
        logger.info("restful birthDayPointEvent is called");
        stack.birthDayPointEvent();
    }

    @Override
    public String SendSMS(String msisdn, String mt, String Action, String Source, String subPackageId) {
//        logger.info("SendSMS called");
//        return "OKE";
        logger.info("Send SMS via Interface msisdn:" + msisdn);
        stack.sendSMS1(msisdn, mt, Action, Source, subPackageId);
        return "send OKE";
    }

    @Override
    public String onSMS(String keyWord, String msisdn, String serviceId) {
      String rs =  stack.onSMS(keyWord, msisdn, serviceId);
       return rs;
    }
}
