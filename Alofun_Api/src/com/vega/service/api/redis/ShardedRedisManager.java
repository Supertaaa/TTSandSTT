package com.vega.service.api.redis;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.util.Hashing;

public class ShardedRedisManager implements RedisManager {

    private String[] host;
    private int maxActive;
    private int maxIdle;
    private int minIdle;
    private int numberTest;
    private long period;
    private ShardedJedisPool pool;
    ArrayList<JedisShardInfo> shards;

    public String set(String key, String value) {
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
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
        ShardedJedis jedis = getJedis();
        long retval = -1L;
        try {
            retval = jedis.del(key).longValue();
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public boolean isExisted(String key) {
        ShardedJedis jedis = getJedis();

        boolean retval = false;
        try {
            retval = jedis.exists(key).booleanValue();
        } catch (Exception e) {
        } finally {
            returnJedis(jedis);
        }
        return retval;
    }

    public ShardedRedisManager(String[] host, int maxActive, int maxIdle,
            int minIdle, int numberTest, long period) {
        this.host = host;
        this.maxActive = maxActive;
        this.maxIdle = maxIdle;
        this.minIdle = minIdle;
        this.numberTest = numberTest;
        this.period = period;
        connect();
    }

    public void connect() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        poolConfig.setMaxTotal(maxActive);

        poolConfig.setTestOnBorrow(false);

        poolConfig.setTestWhileIdle(true);

        poolConfig.setTestOnReturn(true);

        poolConfig.setMaxIdle(this.maxIdle);

        poolConfig.setMinIdle(this.minIdle);

        poolConfig.setTestWhileIdle(true);

        poolConfig.setNumTestsPerEvictionRun(this.numberTest);

        poolConfig.setTimeBetweenEvictionRunsMillis(this.period);

        this.shards = new ArrayList();
        if (this.host.length > 0) {
            for (String item : this.host) {
                String[] hostport = item.split(":");
                if (hostport.length < 2) {
                    hostport[1] = "80";
                }
                if (hostport.length >= 1) {
                    this.shards.add(new JedisShardInfo(hostport[0], Integer.parseInt(hostport[1])));
                }
            }
        }
        this.pool = new ShardedJedisPool(poolConfig, this.shards, Hashing.MD5,
                Pattern.compile("redis_shard_block"));
    }

    public void release() {
        this.pool.destroy();
    }

    public ShardedJedis getJedis() {
        try {
            return (ShardedJedis) this.pool.getResource();
        } catch (Exception e) {
        }
        return null;
    }

    public void returnJedis(ShardedJedis j) {
        this.pool.returnResource(j);
    }

    @Override
    public Set<String> keys(String pattern) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String ping() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
}
