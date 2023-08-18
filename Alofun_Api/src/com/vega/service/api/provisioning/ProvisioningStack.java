/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.provisioning;

import com.vega.service.api.common.Helper;
import com.vega.service.api.common.HttpUtility;
import com.vega.service.api.config.ConfigStack;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 *
 * @author ishop
 */
public class ProvisioningStack {

    static transient Logger logger = Logger.getLogger(ProvisioningStack.class);

    public ProvisioningStack() {
    }

    public String regisPackage(String msisdn, String packageId, String promotion, String trial, String note,
            String channel, String username, String userip) {
         logger.debug(">>>>>>>>>>> RegisPackage Provisioning Stack : "+msisdn);
        logger.info(" - Msisdn: " + msisdn + ", packageId: " + packageId);
        if (packageId == null || msisdn == null) {
            logger.info(" Tham so bi null");
            return "-1";
        } else {
            String application = ConfigStack.getConfig("api_provisioning", "application", "");
            String command = "subscribe";
            String soapContent = createSoapContent(command,msisdn, packageId, promotion, trial, note,
                     application, channel, username, userip);
            return regisProvisioning(soapContent);
        }
    }

    private String createSoapContent(String command, String msisdn, String packageId, String promotion, String trial, String note,
            String application, String channel, String username, String userip) {
        String subscribe = command;
        logger.debug(">>>>>>>>>>> createSoapContent Provisioning Stack : "+msisdn);
        String service = ConfigStack.getConfig("api_provisioning", "service", "");
        int requestid = (int) new Date().getTime();
        logger.info("requestid: "+ requestid);
        String modeCharging = ConfigStack.getConfig("api_provisioning", "mode_charge", "1");
        String packageName = null;
        if (Integer.parseInt(packageId) == 1) {
            packageName = ConfigStack.getConfig("api_provisioning", "packageDaily", "");
        }
        if (Integer.parseInt(packageId) == 2) {
            packageName = ConfigStack.getConfig("api_provisioning", "packageWeekly", "");
        }
        if (Integer.parseInt(packageId) == 3) {
            packageName = ConfigStack.getConfig("api_provisioning", "packageMonthy", "");
        }

        String soapMsg = "<RQST>"
                + "<name>" + subscribe + "</name>"
                + "<requestid>" + requestid + "</requestid>"
                + "<msisdn>" + msisdn + "</msisdn>"
                + "<service>" + service + "</service>"
                + "<package>" + packageName + "</package>"
                + "<promotion>" + promotion + "</promotion>"
                + "<trial>" + trial + "</trial>"
                + "<bundle>0</bundle>"
                + "<note>" + note + "</note>"
                + "<application>" + application + "</application>"
                + "<channel>" + channel + "</channel>"
                + "<username>" + username + "</username>"
                + "<userip>" + userip + "</userip>"
                + "</RQST>";
        logger.info("XML: "+soapMsg);

        return soapMsg;
    }

     private String createSoapCancel(String command, String msisdn, String packageId, String policy, String trial, String note,
            String application, String channel, String username, String userip) {
        String unsubscribe = command;
        logger.debug(">>>>>>>>>>> createSoapContent Provisioning Stack : "+msisdn);
        String service = ConfigStack.getConfig("api_provisioning", "service", "");
        int requestid = (int) new Date().getTime();
        logger.info("requestid: "+ requestid);
        String modeCharging = ConfigStack.getConfig("api_provisioning", "mode_charge", "1");
        String packageName = null;
        if (Integer.parseInt(packageId) == 1) {
            packageName = ConfigStack.getConfig("api_provisioning", "packageDaily", "");
        }
        if (Integer.parseInt(packageId) == 2) {
            packageName = ConfigStack.getConfig("api_provisioning", "packageWeekly", "");
        }
        if (Integer.parseInt(packageId) == 3) {
            packageName = ConfigStack.getConfig("api_provisioning", "packageMonthy", "");
        }

        String soapMsg = "<RQST>"
                + "<name>" + unsubscribe + "</name>"
                + "<requestid>" + requestid + "</requestid>"
                + "<msisdn>" + msisdn + "</msisdn>"
                + "<service>" + service + "</service>"
                + "<package>" + packageName + "</package>"
                + "<policy>" + policy + "</policy>"
                + "<note>" + note + "</note>"
                + "<application>" + application + "</application>"
                + "<channel>" + channel + "</channel>"
                + "<username>" + username + "</username>"
                + "<userip>" + userip + "</userip>"
                + "</RQST>";
        logger.info("XML: "+soapMsg);

        return soapMsg;
    }

    private String regisProvisioning(String sentContent) {
        String url = ConfigStack.getConfig("api_provisioning", "url", "");
        String result = HttpUtility.sendXML(url, sentContent, Integer.parseInt(ConfigStack.getConfig("api_billing", "gw_timeout", "10000")));
        logger.info("Vas Provisioning Respones :"+result);
        String code = HttpUtility.getValue(result, "error");
         logger.debug(">>>>>>>>>>> Register regisProvisioning errorCode : "+code);
        return code;
    }
    public String cancelSubPackage(String msisdn, String packageId, String policy, String trial, String note,
            String channel, String username, String userip) {
        
        logger.info("cancelSubPackage - Msisdn: " + msisdn + ", packageId: " + packageId);
        if (packageId == null || msisdn == null) {
            logger.info(" Tham so bi null");
            return "-1";
        } else {
            String application = ConfigStack.getConfig("api_provisioning", "application", "");
            String command = "unsubscribe";
            String soapContent = createSoapCancel(command,Helper.formatMobileNumber(msisdn), packageId, policy, trial, note,
                     application, channel, username, userip);
            return regisProvisioning(soapContent);
        }
    }
}
