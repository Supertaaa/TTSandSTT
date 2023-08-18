package com.vega.service.api.config;

import com.vega.vcs.service.config.ConfigService;
import org.apache.log4j.Logger;
import org.jconfig.Configuration;

import java.util.ArrayList;

public class Config {

    static Logger logger = Logger.getLogger(Config.class);
    // Restful
    public static int port;
    public static int listenHistoryMaxLen;
    public static int contentMax;
    // Billing
    public static int billChargeLockingExpireInMinute;
    public static int billDailyPackageId;
    public static int billWeeklyPackageId;
    public static int billRegisterType;
    public static int billCancelType;
    public static int billRenewType;
    public static int billSystemCancelType;
    public static String billServiceNumber;
    public static int timeReSetPromoton;

    // Promotion
    public static boolean proEnable;

    // Redis
    public static String redis_manager_class;
    public static String[] redis_host;
    public static int redis_max_active;
    public static int redis_max_idle;
    public static int redis_min_idle;
    public static int redis_number_test;
    public static int redis_period;
    public static String redis_master_name;
    public static int redis_cache_timeout;
    // SipCM
    public static ArrayList<String> callMonitorUrls;
    public static String urlCallMonitor;
    public static Integer sipCMPoolSize;
    public static int timeOut;
    public static String ws_api_user;
    public static String ws_api_pass;
    public static int billMonthyPackageId;
    public static int billDaily2000PackageId;
    public static int billWeekly7000PackageId;
    // MT thue bao
    public static int remindMtLimit;
    public static int remindMtDelay;
    //insertfilelog
    public static int log;
    public static int logccu;
    public static int logmomt;
    public static int logivr;
    public static int loglisten;

    public static int redis_cache_timeout_log;
    public static int monthlyPackageId;
// VoiceBroadCast
    public static String voicebroadcast_url;
    public static String voicebroadcast_host;
    public static int voicebroadcast_socket_port;
    public static int voicebroadcast_thread;
    public static int voicebroadcast_time_out;

    public static void loadConfig(ConfigService configFacility) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Api PATH:" + configFacility.getConfigPath("config", "api"));
        }
        Configuration config = configFacility.getConfig("config", "api");
        port = config.getIntProperty("port", 8080, "general");
        listenHistoryMaxLen = config.getIntProperty("listenHistoryMaxLen", 3990, "general");
        contentMax = config.getIntProperty("contentMax", 500, "general");
        log = config.getIntProperty("log", 0, "general");
        logccu = config.getIntProperty("logccu", 0, "general");
        logmomt = config.getIntProperty("logmomt", 0, "general");
        logivr = config.getIntProperty("logivr", 0, "general");
        loglisten = config.getIntProperty("loglisten", 0, "general");

        // Billing
        billChargeLockingExpireInMinute = config.getIntProperty("billChargeLockingExpireInMinute", 5, "billing");
        billDailyPackageId = config.getIntProperty("billDailyPackageId", 1, "billing");
        billWeeklyPackageId = config.getIntProperty("billWeeklyPackageId", 2, "billing");
        billMonthyPackageId = config.getIntProperty("billMonthyPackageId", 3, "billing");
        billDaily2000PackageId = config.getIntProperty("billDaily2000PackageId", 4, "billing");
        billWeekly7000PackageId = config.getIntProperty("billWeekly7000PackageId", 5, "billing");
        billRegisterType = config.getIntProperty("billRegisterType", 1, "billing");
        billCancelType = config.getIntProperty("billCancelType", 2, "billing");
        billRenewType = config.getIntProperty("billRenewType", 3, "billing");
        billSystemCancelType = config.getIntProperty("billSystemCancelType", 5, "billing");
        timeReSetPromoton = config.getIntProperty("timeReSetPromoton", 30, "billing");
        billServiceNumber = config.getProperty("billServiceNumber", "", "billing");
        ws_api_user = config.getProperty("wsApiUser", "", "billing");
        ws_api_pass = config.getProperty("wsApiPass", "", "billing");
        monthlyPackageId = config.getIntProperty("monthlyPackageId", 3, "billing");
        // Promotion
        proEnable = config.getBooleanProperty("proEnable", true, "promotion");
        // Redis
        redis_manager_class = config.getProperty("redis_manager_class",
                "SentinelRedisManager", "redis");
        redis_host = config.getArray("redis_host", new String[]{}, "redis");
        redis_max_active = config.getIntProperty("redis_max_active", 20,
                "redis");
        redis_max_idle = config.getIntProperty("redis_max_idle", 5, "redis");
        redis_min_idle = config.getIntProperty("redis_min_idle", 1, "redis");
        redis_period = config.getIntProperty("redis_period", 60000, "redis");
        redis_number_test = config.getIntProperty("redis_number_test", 20,
                "redis");
        redis_master_name = config.getProperty("redis_master_name",
                "mymaster", "redis");
        redis_cache_timeout = config.getIntProperty("redis_cache_timeout", 60,
                "redis");
        redis_cache_timeout_log = config.getIntProperty("redisCacheTimeoutLog", 300, "redis");
        // SipCM
        urlCallMonitor = config.getProperty("urlCallMonitor", "", "sipcm");
        sipCMPoolSize = config.getIntProperty("sipCMPoolSize", 30, "sipcm");
        timeOut = config.getIntProperty("timeOut", 10000, "sipcm");
        //Remind MT
        remindMtLimit = config.getIntProperty("remindMtLimit", 100, "remind_mt");
        remindMtDelay = config.getIntProperty("remindMtDelay", 20, "remind_mt");
        // VoiceBroadCast
        voicebroadcast_url = config.getProperty("url", "", "voicebroadcast");
        voicebroadcast_host = config.getProperty("host", "", "voicebroadcast");
        voicebroadcast_socket_port = config.getIntProperty("socket_port", 12345, "voicebroadcast");
        voicebroadcast_thread = config.getIntProperty("thread", 5, "voicebroadcast");
        voicebroadcast_time_out = config.getIntProperty("time_out", 10000, "voicebroadcast");

        if (urlCallMonitor.indexOf(" ") > 0) {
            String[] info = urlCallMonitor.split(" ");
            callMonitorUrls = new ArrayList<String>();
            for (int i = 0; i < info.length; i++) {
                callMonitorUrls.add(info[i]);
            }
        } else {
            callMonitorUrls.add(urlCallMonitor);
        }
    }
}
