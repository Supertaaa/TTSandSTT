/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.logfile;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 *
 * @author PhongTom
 */
public class LogFileStack {

    public static Logger logger_ccu = Logger.getLogger("log_ccu");
    public static Logger logger_dtmf = Logger.getLogger("log_dtmf");
    public static Logger logger_sub_listen = Logger.getLogger("log_sub_listen");
    public static Logger logger_billing = Logger.getLogger("log_billing");
    public static Logger logger_sms = Logger.getLogger("log_sms");


    public LogFileStack() {
    }

    public static void logCCU(String time, int total_ccu, int ccu_type, int package_id, int key) {
        String value = time + "|" + total_ccu + "|" + ccu_type + "|" + package_id + "|" + key + "|";
        logger_ccu.info(value);
    }

    public static void logDTMF(String begin_at, String end_at, String msisdn, String dtmf) {
        String value = begin_at + "|" + end_at + "|" + msisdn + "|" + dtmf + "|";
        logger_dtmf.info(value);
    }

    public static void logSubListen(String created_at, int daily_call_id, int sub_package_id, int package_id, String msisdn, String topic_name, String channel_name, int content_id, String content_name, int duration, int topic_ord, int channel_ord, int channel_id, int topic_type) {
        String a = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        try {
            Date b = sdf.parse(created_at);
            a = format.format(b);
        } catch (Exception ex) {
            a = format.format(new Date());
        }
        String value = a + "|" + daily_call_id + "|" + sub_package_id + "|" + package_id + "|" + msisdn + "|" + topic_name + "|" + channel_name + "|" + content_id + "|" + content_name + "|" + duration + "|" + topic_ord + "|" + channel_ord + "|" + channel_id + "|" + topic_type + "|";
        logger_sub_listen.info(value);
    }

    public static void logSubListen(String created_at, int daily_call_id, int sub_package_id, int package_id, String msisdn, String topic_name, String channel_name, int content_id, String content_name, int duration, int topic_ord, int channel_ord, int channel_id) {
        String value = created_at + "|" + daily_call_id + "|" + sub_package_id + "|" + package_id + "|" + msisdn + "|" + topic_name + "|" + channel_name + "|" + content_id + "|" + content_name + "|" + duration + "|" + topic_ord + "|" + channel_ord + "|" + channel_id;
        logger_sub_listen.info(value);
    }

    public static void logBilling(String channel, String user_id, String package_id, String package_name, String expired_time, String action, String retry_count_inday, String retry_count, String price, String request_time, String response_time, String error_code, String error_message) {
        String a1 = "";
        String a2 = "";
        String a3 = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        try {
            Date b1 = sdf.parse(expired_time);
            a1 = format.format(b1);
            Date b2 = sdf.parse(request_time);
            a2 = format.format(b2);
            Date b3 = sdf.parse(response_time);
            a3 = format.format(b3);
        } catch (Exception ex) {
            a1 = format.format(new Date());
            a2 = format.format(new Date());
            a3 = format.format(new Date());
        }
        logger_billing.info(channel + "|" + user_id + "|" + package_id + "|" + package_name + "|" + a1 + "|" + action + "|" + retry_count_inday + "|" + retry_count + "|" + price + "|" + a2 + "|" + a3 + "|" + error_code + "|" + error_message + "|");
    }

    public static void logSMS(String mo_receive_time, String mt_sent_time, String msisdn, String mo_content, String mt_content, String service_number) {
        String value = mo_receive_time + "|" + mt_sent_time + "|" + msisdn + "|" + mo_content + "|" + mt_content + "|" + service_number;
        logger_sms.info(value);
    }
}
