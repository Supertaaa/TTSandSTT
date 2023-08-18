/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.config;

import com.vega.service.api.db.DBStack;
import com.vega.vcs.service.config.ConfigService;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.jconfig.Configuration;

/**
 *
 * @author ishop
 */
public class ConfigStack {

    static transient Logger logger = Logger.getLogger(ConfigStack.class);
    public static DBStack db;
    private static Configuration config;
    public static String appServerIp;

    public static void loadConfig(ConfigService configFacility) throws Exception {        
        config = configFacility.getConfig("config", "api");
        logger.info("ConfigStack : "+config+"\n");
        logger.info("ConfigStack : "+config.getProperty("appServerIp", "", "appIp")+"\n");
        appServerIp = config.getProperty("appServerIp", "", "appIp");

    }

    public static String getConfig(String categoryName, String name, String defaultValue) {
        String key = categoryName + "." + name;
        key = key.toLowerCase();
        String value = db.getConfig().get(key);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    public static ArrayList<String[]> getConfig(String categoryName) {
        Hashtable<String, String> list = db.getConfig();
        Iterator iterator = list.keySet().iterator();
        ArrayList<String[]> resp = new ArrayList<String[]>();
        while (iterator.hasNext()) {
            String key = String.valueOf(iterator.next());
            String a = categoryName.toLowerCase() + ".";
            if (key.startsWith(a)) {
                String value = list.get(key);
                String[] info = new String[2];
                info[0] = key.replaceFirst(a, "");
                info[1] = value;
                resp.add(info);
            }
        }
        return resp;
    }
}
