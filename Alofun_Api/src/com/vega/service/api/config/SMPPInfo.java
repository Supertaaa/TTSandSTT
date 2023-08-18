/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.config;

import org.apache.log4j.Logger;
import org.jconfig.Configuration;

import java.util.ArrayList;

/**
 *
 * @author User
 */
public class SMPPInfo {

    static Logger logger = Logger.getLogger(SMPPInfo.class);
    public static String serviceNumber;
    public static String separator;
    public static long timeRetry;
    public static int numberRetry;
    public static long smsDefaultDelay;
    public static long smsRegisterDelay;
    /*
     * For test
     */
    public static ArrayList<String> testSub = new ArrayList<String>();
    public static String mode;

    public static void load(Configuration config) {
        serviceNumber = config.getProperty("serviceNumber", "1608", "smpp");
        separator = config.getProperty("separator", "_", "smpp");
        // retry
        timeRetry = config.getLongProperty("timeRetry", 3000, "smpp");
        numberRetry = config.getIntProperty("numberRetry", 2, "smpp");

        smsDefaultDelay = config.getLongProperty("smsDefaultDelay", 0, "smpp");
        smsRegisterDelay = config.getIntProperty("smsRegisterDelay", 0, "smpp");

        //test
        mode = config.getProperty("Mode", "production", "service");
        String[] parts = config.getProperty("TestSub", "", "service").split(" ");
        for (int i = 0; i < parts.length; i++) {
            testSub.add(parts[i]);
        }
    }
}
