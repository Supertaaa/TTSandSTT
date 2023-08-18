package com.vega.service.api.redis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

public class RedisSentinelManager
        implements RedisManager {

    private String[] host;
    private int maxActive;
    private int maxIdle;
    private int minIdle;
    private int numberTest;
    private long period;
    private String masterName;
    private JedisSentinelPool pool;
    private static Logger logger = Logger.getLogger(RedisSentinelManager.class);

    public String set(String key, String value) {
        Jedis jedis = getJedis();
        String retval = null;
        try {
            retval = jedis.set(key, value);
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public long setHashFieldIfNotExisted(String key, String field, String value) {
        Jedis jedis = getJedis();
        Long retval = new Long(0L);
        try {
            retval = jedis.hsetnx(key, field, value);
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval.longValue();
    }

    public long setnx(String key, String value) {
        Jedis jedis = getJedis();
        Long retval = new Long(0L);
        try {
            retval = jedis.setnx(key, value);
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval.longValue();
    }

    public String getSet(String key, String value) {
        Jedis jedis = getJedis();
        String retval = null;
        try {
            retval = jedis.getSet(key, value);
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public long setHashField(String key, String field, String value) {
        Jedis jedis = getJedis();
        long retval = -1L;
        try {
            retval = jedis.hset(key, field, value).longValue();
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public String setHash(String key, Map map) {
        Jedis jedis = getJedis();
        String retval = null;
        try {
            retval = jedis.hmset(key, map);
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public String get(String key) {
        Jedis jedis = getJedis();
        String retval = null;
        try {
            retval = jedis.get(key);
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public String getHashField(String key, String field) {
        Jedis jedis = getJedis();
        String retval = null;
        try {
            retval = jedis.hget(key, field);
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public Map getHash(String key) {
        Jedis jedis = getJedis();
        Map retval = null;
        try {
            retval = jedis.hgetAll(key);
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public long increaseHashField(String key, String field, long value) {
        Jedis jedis = getJedis();
        long retval = -1L;
        try {
            retval = jedis.hincrBy(key, field, value).longValue();
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public long push(String key, String value) {
        Jedis jedis = getJedis();
        long retval = -1L;
        try {
            retval = jedis.lpush(key, new String[]{value}).longValue();
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public String pop(String key) {
        Jedis jedis = getJedis();
        String retval = null;
        try {
            retval = jedis.rpop(key);
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public long setExpiredTime(String key, int seconds) {
        Jedis jedis = getJedis();
        long retval = -1L;
        try {
            retval = jedis.expire(key, seconds).longValue();
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public long del(String key) {
        Jedis jedis = getJedis();
        long retval = -1L;
        try {
            retval = jedis.del(key).longValue();
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public Set<String> keys(String pattern) {
        Jedis jedis = getJedis();
        Set<String> retval = null;
        try {
            retval = jedis.keys(pattern);
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public boolean isExisted(String key) {
        Jedis jedis = getJedis();
        boolean retval = false;
        try {
            retval = jedis.exists(key).booleanValue();
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public String ping() {
        Jedis jedis = getJedis();
        String retval = null;
        try {
            retval = jedis.ping();
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public RedisSentinelManager(String masterName, String[] host, int maxActive, int maxIdle, int minIdle, int numberTest, long period) {
        this.host = host;
        this.maxActive = maxActive;
        this.maxIdle = maxIdle;
        this.minIdle = minIdle;
        this.numberTest = numberTest;
        this.period = period;
        this.masterName = masterName;
        connect();
    }

    public void connect() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        poolConfig.setMaxTotal(maxActive);


        poolConfig.setTestOnBorrow(true);



        poolConfig.setTestOnReturn(true);


        poolConfig.setMaxIdle(this.maxIdle);


        poolConfig.setMinIdle(this.minIdle);

        poolConfig.setTestWhileIdle(true);

        poolConfig.setNumTestsPerEvictionRun(this.numberTest);

        poolConfig.setTimeBetweenEvictionRunsMillis(this.period);


        Set<String> sentinels = new HashSet();
        if (this.host.length > 0) {
            for (String item : this.host) {
                try {
                    sentinels.add(item.trim());
                } catch (Exception e) {
                    logger.error("Error when connect to sentinel server: " + item);
                }
            }
        }
        this.pool = new JedisSentinelPool(this.masterName, sentinels, poolConfig);
    }

    public void release() {
        this.pool.destroy();
    }

    public Jedis getJedis() {
        try {
            return (Jedis) this.pool.getResource();
        } catch (Exception e) {
        }
        return null;
    }

    public void returnJedis(Jedis jedis) {
        this.pool.returnResource((Jedis) jedis);
    }
}
