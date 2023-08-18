package com.vega.service.api.redis;

import com.vega.service.api.config.ConfigStack;
import org.apache.log4j.Logger;

public class RedisManagerFactory {

    private static final int DEFAULT_REDIS_SERVER_PORT = 6379;
    private static Logger logger = Logger.getLogger(RedisManagerFactory.class);

    public static RedisManager createRedisManager() {
        String redisManagerClass = ConfigStack.getConfig("api_redis", "class", "");
        System.out.println("==================" + redisManagerClass);
        if (redisManagerClass != null) {
            if (redisManagerClass.equalsIgnoreCase("SentinelRedisManager")) {
                logger.info("try create SentinelRedisManager class");
                return createSentinelRedisManager();
            } else if (redisManagerClass.equalsIgnoreCase("SharededRedisManager")) {
                logger.info("try create SharededRedisManager class");
                return createSharededRedisManager();
            }
        }
        return createDefaultRedisManager();
    }

    public static RedisManager createDefaultRedisManager() {
        String a = ConfigStack.getConfig("api_redis", "host", "");
        String[] urls = a.split(",");
        if (urls != null && urls.length > 0) {
            String url = urls[0];
            String host = url;
            int port = DEFAULT_REDIS_SERVER_PORT;
            String[] split = url.split(":");
            if (split != null && split.length > 1) {
                host = split[0];
                try {
                    port = Integer.parseInt(split[1]);
                } catch (Exception ex) {
                    logger.warn("error when try get port of refis server ", ex);
                }
            } else {
                logger.warn("can't get port of refis server,use default  "
                        + port);
            }
            RedisManager redisManager = new DefaultRedisManager(host, port,
                    Integer.parseInt(ConfigStack.getConfig("api_redis", "max_active", "1")), Integer.parseInt(ConfigStack.getConfig("api_redis", "max_idle", "1")),
                    Integer.parseInt(ConfigStack.getConfig("api_redis", "min_idle", "1")), Integer.parseInt(ConfigStack.getConfig("api_redis", "number_test", "1")),
                    Long.parseLong(ConfigStack.getConfig("api_redis", "period", "1")));
            return redisManager;

        } else {
            logger.warn("can't start redis client with url " + urls);
            return null;
        }

    }

    public static RedisManager createSentinelRedisManager() {
        String a = ConfigStack.getConfig("api_redis", "host", "");
        String[] urls = a.split(",");
        if (urls != null && urls.length > 0) {
            RedisManager redisManager = new RedisSentinelManager(
                    ConfigStack.getConfig("api_redis", "master_name", ""), urls,
                    Integer.parseInt(ConfigStack.getConfig("api_redis", "max_active", "1")), Integer.parseInt(ConfigStack.getConfig("api_redis", "max_idle", "1")),
                    Integer.parseInt(ConfigStack.getConfig("api_redis", "min_idle", "1")), Integer.parseInt(ConfigStack.getConfig("api_redis", "number_test", "1")),
                    Long.parseLong(ConfigStack.getConfig("api_redis", "period", "1")));
            return redisManager;

        } else {
            logger.warn("can't start redis client with url " + urls);
            return null;
        }

    }

    public static RedisManager createSharededRedisManager() {
        String a = ConfigStack.getConfig("api_redis", "host", "");
        String[] urls = a.split(",");
        if (urls != null && urls.length > 0) {

            RedisManager redisManager = new ShardedRedisManager(urls,
                    Integer.parseInt(ConfigStack.getConfig("api_redis", "max_active", "1")), Integer.parseInt(ConfigStack.getConfig("api_redis", "max_idle", "1")),
                    Integer.parseInt(ConfigStack.getConfig("api_redis", "min_idle", "1")), Integer.parseInt(ConfigStack.getConfig("api_redis", "number_test", "1")),
                    Long.parseLong(ConfigStack.getConfig("api_redis", "period", "1")));
            return redisManager;

        } else {
            logger.warn("can't start redis client with url " + urls);
            return null;
        }

    }
}
