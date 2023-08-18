package com.vega.service.api.sms;

import java.util.Map;

import com.vega.rabbit.message.Message;
import com.vega.rabbit.message.MessageListener;
import com.vega.service.api.RestfulStack;
import java.util.HashMap;
import org.apache.log4j.Logger;

public class RabbitListener implements MessageListener {

    Logger logger = Logger.getLogger(RabbitListener.class);
    public RestfulStack stack;

    public RestfulStack getRenewStack() {
        return stack;
    }

    public void setRestfulStack(RestfulStack stack) {
        this.stack = stack;
    }

    @Override
    public void onMessage(Message msg) {
        // TODO Auto-generated method stub
        try {
            HashMap<String, String> params = new HashMap<String, String>();
            Map<String, Object> properties = msg.getHeaders();
            logger.info("==============start show msg==========================");
            for (String k : properties.keySet()) {
                Object v = properties.get(k);
                if (k.equalsIgnoreCase("amount")) {
                    k = "charged_amount";
                }
                if (k.equalsIgnoreCase("resultcode")) {
                    k = "result";
                }
                logger.info("k= " + k + ", v=" + v);
                params.put(k, String.valueOf(v));
            }
            logger.info("msg body " + new String(msg.getBody()));
            logger.info("==============end show msg==========================");
           // stack.onSMS(params);
        } catch (Exception ex) {
            logger.error(ex);
        }
    }
}
