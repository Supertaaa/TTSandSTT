/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.charging;

import com.vega.service.api.common.Helper;
import com.vega.service.api.common.HttpUtility;
import com.vega.service.api.config.ConfigStack;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 *
 * @author ishop
 */
public class ChargingStack {

    static transient Logger logger = Logger.getLogger(ChargingStack.class);

    public ChargingStack() {
    }

    public String chargePackage(String msisdn, String packageId, String amount, String channel, String action) {
        logger.info("chargePackage - Msisdn: " + msisdn + ", Amount: " + amount);
        String modeCharging = ConfigStack.getConfig("api_charging", "mode_charge", "1");
        String soapContent = createSoapContent(Helper.formatMobileNumberWithoutPrefix(msisdn), packageId, amount, channel, action, modeCharging);
        return processCharing(soapContent);
    }

    public String registerForwardFormUngDichVu(String msisdn, String channel, String content) {
        logger.info("registerForwardFormUngDichVu - Msisdn: " + msisdn + ", content: " + content);
        String soapContent = createSoapCallVasApi(Helper.formatMobileNumberWithoutPrefix(msisdn), channel, content);
        return processCallVasApi(soapContent);
    }

    public String chargeContent(String msisdn, String packageId, String amount, String channel, String action) {
        logger.info("chargeContent - Msisdn: " + msisdn + ", Amount: " + amount + "; action:" + action);
        String modeCharging = "0";
        String soapContent = createSoapContent(Helper.formatMobileNumberWithoutPrefix(msisdn), packageId, amount, channel, action, modeCharging);
        return processCharing(soapContent);
    }

    private String createSoapCallVasApi(String msisdn, String channel, String content) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar cal = Calendar.getInstance();
        String requestId = sdf.format(cal.getTime());
        System.out.println("L :" + sdf.format(cal.getTime()));

        String soapMsg = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"/\">"
                + "<soapenv:Header/>"
                + "<soapenv:Body>"
                + "<ns:register>"
                + "<requestid>" + requestId + "</requestid>"
                + "<msisdn>" + msisdn + "</msisdn>"
                + "<packagename>" + "NGAY" + "</packagename>"
                + "<promotion>" + "1d" + "</promotion>"
                + "<bundle>" + 0 + "</bundle>" // Id goi cuoc trong bang package
                + "<trial>" + 0 + "</trial>"
                + "<note>" + content + "</note>"
                + "<application>" + "VASGW" + "</application>"
                + "<channel>SMS</channel>"
                + "<username>" + channel + "</username>"
                + "<userip>" + "127.0.0.1" + "</userip>"
                + "</ns:register>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";
        return soapMsg;
    }

    private String createSoapContent(String msisdn, String packageId, String amount, String channel, String action, String modeCharging) {
        String username = ConfigStack.getConfig("api_charging", "username", "");
        String password = ConfigStack.getConfig("api_charging", "password", "");

        String soapMsg = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"/\">"
                + "<soapenv:Header/>"
                + "<soapenv:Body>"
                + "<ns:chargingRequest>"
                + "<username>" + username + "</username>"
                + "<password>" + password + "</password>"
                + "<msisdn>" + Helper.formatMobileNumber(msisdn) + "</msisdn>"
                + "<packageid>" + packageId + "</packageid>" // Id goi cuoc trong bang package
                + "<action>" + action + "</action>"
                + "<amount>" + amount + "</amount>"
                + "<original_amount>" + amount + "</original_amount>"
                + "<contentid>301</contentid>"
                + "<mode>" + modeCharging + "</mode>"
                + "<channel>" + channel + "</channel>"
                + "</ns:chargingRequest>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";
        return soapMsg;
    }

    private String processCharing(String sentContent) {
        String url = ConfigStack.getConfig("api_charging", "url", "");
       // String url = "http://10.211.7.220:3737/ws/chargingRequest";
        logger.info("Url api_charging :" + url + "\n");
        logger.info("XML api_charging :" + sentContent + "\n");
        String result = HttpUtility.sendXML(url, sentContent, Integer.parseInt(ConfigStack.getConfig("api_billing", "gw_timeout", "10000")));
        String code = HttpUtility.getValue(result, "return");
        return code;
    }

    private String processCallVasApi(String sentContent) {
        String url = ConfigStack.getConfig("api_vas_api", "url", "");
        String result = HttpUtility.sendXML(url, sentContent, Integer.parseInt(ConfigStack.getConfig("api_billing", "gw_timeout", "10000")));
        String code = HttpUtility.getValue(result, "return");
        return code;
    }
}
