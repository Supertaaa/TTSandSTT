package com.vega.service.api.redis;
	
import java.util.Map;
import java.util.Set;

public abstract interface RedisManager
{
  public abstract String set(String paramString1, String paramString2);
  
  public abstract long setHashFieldIfNotExisted(String paramString1, String paramString2, String paramString3);
  
  public abstract long setnx(String paramString1, String paramString2);
  
  public abstract String getSet(String paramString1, String paramString2);
  
  public abstract long setHashField(String paramString1, String paramString2, String paramString3);
  
  public abstract String setHash(String paramString, Map paramMap);
  
  public abstract String get(String paramString);
  
  public abstract String getHashField(String paramString1, String paramString2);
  
  public abstract Map getHash(String paramString);
  
  public abstract long increaseHashField(String paramString1, String paramString2, long paramLong);
  
  public abstract long push(String paramString1, String paramString2);
  
  public abstract String pop(String paramString);
  
  public abstract long setExpiredTime(String paramString, int paramInt);
  
  public abstract long del(String paramString);
  
  public abstract Set<String> keys(String paramString);
  
  public abstract boolean isExisted(String paramString);
  
  public abstract String ping();
  
  public abstract void release();
  
  public abstract void connect();
}
