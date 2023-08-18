package com.vega.service.api.db;

import com.google.gson.Gson;
import com.vega.service.api.RestfulStack;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.DateUtil;
import com.vega.service.api.common.Helper;
import com.vega.service.api.common.ProvinceInfo;
import com.vega.service.api.common.SubMessageInfo;
import com.vega.service.api.common.SubProfileInfo;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.logfile.LogFileStack;
import com.vega.service.api.object.BillingActivityInfo;
import com.vega.service.api.object.BillingErrorCode;
import com.vega.service.api.object.CCUInfo;
import com.vega.service.api.object.Content;
import com.vega.service.api.object.HoroscopeInDateRange;
import com.vega.service.api.object.ListenHistory;
import com.vega.service.api.object.PackageInfo;
import com.vega.service.api.object.PromtInTimeRange;
import com.vega.service.api.object.SMS;
import com.vega.service.api.object.SubPackageInfo;
import com.vega.service.api.object.SubPackageInfo.PackagePromotionStatus;
import com.vega.service.api.object.SubPackageInfo.SubPackageStatus;
import com.vega.service.api.object.User;
import com.vega.vcs.service.cache.CacheService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.vega.vcs.service.database.DBPool;
import com.vega.vcs.service.database.pool.DbConnection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public class DBStack {

    static transient Logger logger = Logger.getLogger(DBStack.class);
    private DBPool dbPool;
    private ArrayList<PackageInfo> packages = new ArrayList<PackageInfo>();
    private Hashtable<Integer, String> listTopics = new Hashtable<Integer, String>();
    private Hashtable<Integer, String> listChannels = new Hashtable<Integer, String>();
    private Hashtable<Integer, String> listContentsMusic = new Hashtable<Integer, String>();
    private Hashtable<Integer, String> listContentsStory = new Hashtable<Integer, String>();
    private Hashtable<Integer, Integer> listStoryCodes = new Hashtable<Integer, Integer>();
    private Hashtable<Integer, String> listContentsNews = new Hashtable<Integer, String>();
    private Hashtable<Integer, String> listContentsFun = new Hashtable<Integer, String>();
    private Hashtable<Integer, String> listContentsHoro = new Hashtable<Integer, String>();
    private Object lock = new Object();
    Gson gson = new Gson();
    Ehcache cache;
    // Value cache
    String package_cache_values = "0123456789";
    String config_cache_values = "0123456789";

    public boolean start() throws NamingException {
        logger.info("DBStack start  db: ");
        Context ctx = new InitialContext();
        dbPool = (DBPool) ctx.lookup("service/dbpool");
        logger.info("DBStack start  dbPool: " + dbPool.getConnectionPool().getSize());
        CacheService cacheService = (CacheService) ctx.lookup("service/cache");
        logger.info("DBStack start  cacheService: " + cacheService.getStatus());
        cache = cacheService.getCache("api");
        this.getListPackage();
        return true;
    }

    public boolean stop() {
        return true;
    }

    public boolean reload() {
        return true;
    }

    /**
     * Check-out 1 ket noi toi DB
     *
     * @return
     * @throws SQLException
     */
    private DbConnection getConnection() throws Exception {
        DbConnection conn = null;
        try {
            conn = dbPool.getConnectionPool().getConnection(30);
           // dbPool.getConnectionPool().getConnection().clearCachedStatements();
        } catch (Exception ex) {
            logger.error(ex);
        }
        if (conn == null) {
            logger.error("connection is not established");
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("get dbconnection ok");
            }
        }
        return conn;
    }

    /**
     * Giai phong connection toi DB
     *
     * @param cnn
     */
    private void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                logger.error("error in closeResultSet", e);
            }
        }
    }

    private void closeStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                logger.error("error in closeStatement", e);
            }
        }
    }

    private void freeConnection(DbConnection cnn) {
        if (cnn != null) {
            try {
                dbPool.getConnectionPool().freeConnection(cnn);
            } catch (Exception e) {
                logger.error("error in freeConnection", e);
            }
        }
    }

    /**
     * Rollback lai giao dich
     *
     * @param cnn
     */
    private void rollbackTransaction(DbConnection cnn) {
        if (cnn != null) {
            try {
                cnn.getConnection().rollback();
            } catch (Exception ex) {
                logger.error("Error in rollbackTransaction", ex);
            }
        }
    }

    public void refreshListPackage() {
        synchronized (lock) {
            String key = "package_api";
            cache.remove(key);
            packages.clear();
            getListPackage();
        }
    }

    public ArrayList<PackageInfo> getListPackage() {
        logger.info("getListPackage function ");
        String key = "package_api";
        Element data = cache.get(key);
        if (data != null) {
            logger.debug("get list package from cache");
            return (ArrayList<PackageInfo>) data.getObjectValue();
        }
        logger.debug("get list package from Database");
        synchronized (lock) {
            packages.clear();
            if (packages.isEmpty()) {
                DbConnection cnn = null;
                PreparedStatement stmt = null;
                ResultSet rs = null;
                try {
                    cnn = getConnection();
                    String sql = "select package_id, package_name, sub_fee, time_life, free_minutes, pending_life, over_fee, time_life_promotion,promotion_mode,free_minutes_promotion from package";
                    stmt = cnn.getConnection().prepareStatement(sql);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    rs = stmt.executeQuery();
                    while (rs.next()) {
                        PackageInfo p = new PackageInfo();
                        p.setPackageId(rs.getInt("package_id"));
                        p.setPackageName(rs.getString("package_name"));
                        p.setFreeMinutes(rs.getInt("free_minutes"));
                        p.setFreeMinutesPromotion(rs.getInt("free_minutes_promotion"));
                        p.setSubFee(rs.getInt("sub_fee"));
                        p.setPendingLife(rs.getInt("pending_life"));
                        p.setTimeLife(rs.getInt("time_life"));
                        p.setOverFee(rs.getInt("over_fee"));
                        p.setTimeLifePromotion(rs.getInt("time_life_promotion"));
                        p.setPromotionMode(rs.getInt("promotion_mode"));
                        packages.add(p);
                    }
//                    if (!packages.isEmpty()) {
//                        cache.put(new Element(key, packages));
//                    }
                } catch (Exception e) {
                    logger.error("Error in getListPackage", e);
                } finally {
                    closeResultSet(rs);
                    closeStatement(stmt);
                    freeConnection(cnn);
                }
            }
        }

        return packages;
    }

    // Get list topics
    public Hashtable<Integer, String> getListTopics() {
        String key = this.getClass().getCanonicalName() + ".getListTopics";
        String data = RestfulStack.getFromCache(key);
        boolean isCache = false;
        if (data != null) {
            isCache = true;
            if (!listTopics.isEmpty()) {
                logger.debug("getListTopics from cache");
                return listTopics;
            }
        }
        logger.debug("getListTopics from Database");
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select topic_ord, topic_name "
                    + "from content_topic where ivr_publish = 1 order by topic_ord";
            stmt = cnn.getConnection().prepareStatement(sql);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("topic_ord");
                String name = rs.getString("topic_name");
                listTopics.put(ord, name);
            }
            if (!listTopics.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "listTopics", Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
            }
        } catch (Exception e) {
            logger.error("Error in getListTopics", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listTopics;
    }

    // Get list channels
    public Hashtable<Integer, String> getListChannels() {
        String key = this.getClass().getCanonicalName() + ".getListChannels";
        String data = RestfulStack.getFromCache(key);
        boolean isCache = false;
        if (data != null) {
            isCache = true;
            if (!listChannels.isEmpty()) {
                logger.debug("getListChannels from cache");
                return listChannels;
            }
        }
        logger.debug("getListChannels from Database");
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select channel_id, channel_name "
                    + "from content_channel where ivr_publish = 1 order by channel_id";
            stmt = cnn.getConnection().prepareStatement(sql);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("channel_id");
                String name = rs.getString("channel_name");
                listChannels.put(ord, name);
            }
            if (!listChannels.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "listChannels", Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
            }
        } catch (Exception e) {
            logger.error("Error in getListChannels", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listChannels;
    }

    // Get list music content
    public Hashtable<Integer, String> getListMusicContents() {
        String key = this.getClass().getCanonicalName() + ".getListMusicContents";
        String data = RestfulStack.getFromCache(key);
        boolean isCache = false;
        if (data != null) {
            isCache = true;
            if (!listContentsMusic.isEmpty()) {
                logger.debug("getListMusicContents from cache");
                return listContentsMusic;
            }
        }
        logger.debug("getListMusicContents from Database");
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select music_content_id content_id,content_name from music_content "
                    + "where status = 1 and ivr_publish = 1 order by music_content_id";
            stmt = cnn.getConnection().prepareStatement(sql);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("content_id");
                String name = rs.getString("content_name");
                listContentsMusic.put(ord, name);
            }
            if (!listContentsMusic.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "getListMusicContents", Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
            }
        } catch (Exception e) {
            logger.error("Error in getListMusicContents", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listContentsMusic;
    }

    // Get list story content
    public Hashtable<Integer, String> getListStoryContents() {
        String key = this.getClass().getCanonicalName() + ".getListStoryContents";
        String data = RestfulStack.getFromCache(key);
        boolean isCache = false;
        if (data != null) {
            isCache = true;
            if (!listContentsStory.isEmpty()) {
                logger.debug("getListStoryContents from cache");
                return listContentsStory;
            }
        }
        logger.debug("getListStoryContents from Database");
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select story_content_id content_id,content_name from story_content "
                    + "where status = 1 and ivr_publish = 1 order by story_content_id";
            stmt = cnn.getConnection().prepareStatement(sql);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("content_id");
                String name = rs.getString("content_name");
                listContentsStory.put(ord, name);
            }
            if (!listContentsStory.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "getListStoryContents", Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
            }
        } catch (Exception e) {
            logger.error("Error in getListStoryContents", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listContentsStory;
    }

    // Get list story content
    public Hashtable<Integer, Integer> getListStoryCodes() {
        String key = this.getClass().getCanonicalName() + ".getListStoryCodes";
        String data = RestfulStack.getFromCache(key);
        boolean isCache = false;
        if (data != null) {
            isCache = true;
            if (!listStoryCodes.isEmpty()) {
                logger.debug("getListStoryCodes from cache");
                return listStoryCodes;
            }
        }
        logger.debug("getListStoryCodes from Database");
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select story_content_id content_id,code from story_content "
                    + "where status = 1 and ivr_publish = 1 order by story_content_id";
            stmt = cnn.getConnection().prepareStatement(sql);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("content_id");
                int code = rs.getInt("code");
                listStoryCodes.put(ord, code);
            }
            if (!listStoryCodes.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "getListStoryCodes", Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
            }
        } catch (Exception e) {
            logger.error("Error in getListStoryCodes", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listStoryCodes;
    }

    // Get list news content
    public Hashtable<Integer, String> getListNewsContents() {
        String key = this.getClass().getCanonicalName() + ".getListNewsContents";
        String data = RestfulStack.getFromCache(key);
        boolean isCache = false;
        if (data != null) {
            isCache = true;
            if (!listContentsNews.isEmpty()) {
                logger.debug("getListNewsContents from cache");
                return listContentsNews;
            }
        }
        logger.debug("getListNewsContents from Database");
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select news_content_id content_id,content_name from news_content "
                    + "where status = 1 and ivr_publish = 1 order by news_content_id";
            stmt = cnn.getConnection().prepareStatement(sql);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("content_id");
                String name = rs.getString("content_name");
                listContentsNews.put(ord, name);
            }
            if (!listContentsNews.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "getListNewsContents", Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
            }
        } catch (Exception e) {
            logger.error("Error in getListNewsContents", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listContentsNews;
    }

    // Get list fun content
    public Hashtable<Integer, String> getListFunContents() {
        String key = this.getClass().getCanonicalName() + ".getListFunContents";
        String data = RestfulStack.getFromCache(key);
        boolean isCache = false;
        if (data != null) {
            isCache = true;
            if (!listContentsFun.isEmpty()) {
                logger.debug("getListFunContents from cache");
                return listContentsFun;
            }
        }
        logger.debug("getListFunContents from Database");
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select fun_content_id content_id,content_name from fun_content "
                    + "where status = 1 and ivr_publish = 1 order by fun_content_id";
            stmt = cnn.getConnection().prepareStatement(sql);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("content_id");
                String name = rs.getString("content_name");
                listContentsFun.put(ord, name);
            }
            if (!listContentsFun.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "getListFunContents", Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
            }
        } catch (Exception e) {
            logger.error("Error in getListFunContents", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listContentsFun;
    }

    // Get list horo content
    public Hashtable<Integer, String> getListHoroContents() {
        String key = this.getClass().getCanonicalName() + ".getListHoroContents";
        String data = RestfulStack.getFromCache(key);
        boolean isCache = false;
        if (data != null) {
            isCache = true;
            if (!listContentsHoro.isEmpty()) {
                logger.debug("getListHoroContents from cache");
                return listContentsHoro;
            }
        }
        logger.debug("getListHoroContents from Database");
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select horo_content_id content_id,SUBSTRING_INDEX(content_path,'/',-1) content_name from horo_content "
                    + "where status = 1 and ivr_publish = 1 order by horo_content_id";
            stmt = cnn.getConnection().prepareStatement(sql);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("content_id");
                String name = rs.getString("content_name");
                listContentsHoro.put(ord, name);
            }
            if (!listContentsHoro.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "getListHoroContents", Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
            }
        } catch (Exception e) {
            logger.error("Error in getListHoroContents", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listContentsHoro;
    }

    public SubPackageInfo getLastSubPackage(String msisdn, boolean checkChargingSubLocking, int packageId) {
        SubPackageInfo subInfo = new SubPackageInfo();
        subInfo.setErrorCode(BillingErrorCode.SystemError);
        subInfo.setPromotion(PackagePromotionStatus.NO_PROMOTION);
        subInfo.setMsisdn(msisdn);

        DbConnection cnn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt_cnt = null;
        PreparedStatement stmt_blacklist = null;
        PreparedStatement stmt_subpoint = null;
        ResultSet rs_cnt = null;
        ResultSet rs = null;
        ResultSet rs_blacklist = null;
        ResultSet rsCheckLastCall = null;

        String sql_black_list = "select msisdn from sub_blacklist where msisdn = ? and status = 1";

        String sql = "SELECT a.sub_package_id, a.msisdn, a.package_id, a.free_minutes, a.status, a.reg_at,"
                + " a.updated_at, a.expire_at, a.source, a.charge_success,"
                + " DATEDIFF(NOW(), a.expire_at) AS expire_day,a.reg_type,"
                + " b.package_name,b.sub_fee,b.over_fee,upper(a.command) command,"
                + " b.sub_fee_level2,a.next_charge_fee,a.debit_fee, "
                + " DATEDIFF(NOW(), a.updated_at) AS update_day "
                + " FROM sub_package a,package b"
                + " WHERE a.package_id = b.package_id"
                + " AND msisdn = ?"
                + " AND (? = 0 OR a.package_id = ?)"
                + " ORDER BY updated_at DESC, sub_package_id DESC LIMIT 1";

        String sql_count = "select count(*) as cnt from charge_locking where msisdn = ? and charge_type = ? and TIMESTAMPDIFF(MINUTE, created_at, NOW()) < ?";
        boolean chargeSubLocked = false;

        try {
            cnn = getConnection();
            stmt_blacklist = cnn.getConnection().prepareStatement(sql_black_list);
            stmt_blacklist.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_blacklist.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs_blacklist = stmt_blacklist.executeQuery();
            //if (!rs_blacklist.next()) {
            if (checkChargingSubLocking) {
                stmt_cnt = cnn.getConnection().prepareStatement(sql_count);
                stmt_cnt.setString(1, msisdn);
                stmt_cnt.setInt(2, Constants.CHARGE_TYPE_SUB);
                stmt_cnt.setInt(3,
                        Integer.parseInt(ConfigStack.getConfig("api_billing", "lock_charging_time", "0")));
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt_cnt.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                rs_cnt = stmt_cnt.executeQuery();
                if (rs_cnt.next() && rs_cnt.getInt("cnt") > 0) {
                    subInfo.setErrorCode(BillingErrorCode.ChargingSubProcessing);
                    chargeSubLocked = true;
                }
            }

            if (!chargeSubLocked) {
                stmt = cnn.getConnection().prepareStatement(sql);
                stmt.setString(1, msisdn);
                stmt.setInt(2, packageId);
                stmt.setInt(3, packageId);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                rs = stmt.executeQuery();
                if (rs.next()) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    subInfo.setSubPackageId(rs.getInt("sub_package_id"));
                    subInfo.setMsisdn(rs.getString("msisdn"));
                    subInfo.setPackageId(rs.getInt("package_id"));
                    subInfo.setUpdatedAt(dateFormat.format(rs.getTimestamp("updated_at")));
                    subInfo.setRegAt(dateFormat.format(rs.getTimestamp("reg_at")));
                    subInfo.setExpireAt(dateFormat.format(rs.getTimestamp("expire_at")));
                    SubPackageStatus stt = rs.getInt("status") == SubPackageStatus.Active.getValue() ? SubPackageStatus.Active
                            : SubPackageStatus.Cancel;
                    subInfo.setStatus(stt);
                    subInfo.setFreeMinutes(rs.getInt("free_minutes"));
                    subInfo.setExpireDay(rs.getInt("expire_day"));
                    subInfo.setExpired(subInfo.getExpireDay() <= 0 ? false : true);
                    subInfo.setSourceReg(rs.getString("source"));
                    subInfo.setRenewSuccess(rs.getInt("charge_success"));
                    subInfo.setRegisterType(rs.getInt("reg_type"));
                    subInfo.setPackageName(rs.getString("package_name"));
                    subInfo.setRegFee(rs.getInt("sub_fee"));
                    subInfo.setOverFee(rs.getInt("over_fee"));
                    subInfo.setCommand(rs.getString("command"));
                    subInfo.setSubFee(rs.getInt("sub_fee"));
                    subInfo.setSubFeeLevel2(rs.getInt("sub_fee_level2"));
                    subInfo.setNextChargeFee(rs.getInt("next_charge_fee"));
                    if (subInfo.getNextChargeFee() == 0) {
                        subInfo.setNextChargeFee(subInfo.getSubFee());
                    } else {
                        if (subInfo.isExpired() && rs.getInt("update_day") > 0) {
                            subInfo.setNextChargeFee(subInfo.getSubFee());
                        }
                    }
                    subInfo.setDebitFee(rs.getInt("debit_fee"));

                    subInfo.setErrorCode(BillingErrorCode.Success);
                    /*
                     * Kiem tra lich su ngay phat sinh cuoc goi gan nhat
                     */
                    String sqlCheckSubpoint = "select total_point, DATE_FORMAT(last_call_date, '%Y-%m-%d %H:%i:%s') last_call_date, DATEDIFF(DATE_FORMAT(now(), '%Y-%m-%d'),DATE_FORMAT(last_call_date, '%Y-%m-%d')) as call_date_diff, last_promt_ord from sub_point where msisdn = ? ";
                    stmt_subpoint = cnn.getConnection().prepareStatement(sqlCheckSubpoint);
                    stmt_subpoint.setString(1, subInfo.getMsisdn());
                    rsCheckLastCall = stmt_subpoint.executeQuery();
                    if (rsCheckLastCall.next()) {
                        /*
                         * Da ton tai ban ghi tai khoan diem
                         */
                        subInfo.setIsHasPointBalance(true);
                        subInfo.setLastCallDate(DateUtil.string2Calendar(rsCheckLastCall.getString("last_call_date")));
                        subInfo.setTotalPoint(rsCheckLastCall.getInt("total_point"));
                        /*
                         * Xac dinh cuoc goi lan dau trong ngay
                         */
                        Date lastCallDate = rsCheckLastCall.getDate("last_call_date");
                        int callDateDiff = rsCheckLastCall.getInt("call_date_diff");
                        if (lastCallDate == null || callDateDiff != 0) {
                            subInfo.setIsFirstCallInDay(true);
                        }
                        subInfo.setPromtCustomerCareOrd(rsCheckLastCall.getInt("last_promt_ord"));
                    } else {
                        subInfo.setIsFirstCallInDay(true);
                    }
                } else {
                    subInfo.setErrorCode(BillingErrorCode.NotFoundData);
                    subInfo.setRenewSuccess(0);
                    /*
                     * Chua dang ky dich vu => du dieu kien KM tat ca goi cuoc
                     */
                    if (Boolean.parseBoolean(ConfigStack.getConfig("api_promotion", "enable", "false"))) {
                        subInfo.setPromotion(PackagePromotionStatus.PROMOTION_ALL);
                    }
                }
            }
//            } else {
//                subInfo.setErrorCode(BillingErrorCode.BlackList);
//            }
        } catch (Exception e) {
            logger.error("Exception in getLastSubPackage for " + msisdn, e);

            subInfo.setErrorCode(BillingErrorCode.SystemError);
        } finally {
            closeResultSet(rs_cnt);
            closeResultSet(rs);
            closeResultSet(rs_blacklist);
            closeResultSet(rsCheckLastCall);

            closeStatement(stmt_cnt);
            closeStatement(stmt);
            closeStatement(stmt_blacklist);
            closeStatement(stmt_subpoint);
            freeConnection(cnn);

        }

        return subInfo;
    }
    // Check user package

    public SubPackageInfo checkSubPackage(String msisdn) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        SubPackageInfo subInfo = new SubPackageInfo();
        subInfo.setErrorCode(BillingErrorCode.SystemError);
        subInfo.setMsisdn(msisdn);

        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT distinct(a.package_id) as package_id, a.free_minutes, a.expire_at, b.package_name, datediff(now(),expire_at) datediff, b.sub_fee"
                + " FROM sub_package a, package b"
                + " WHERE a.package_id = b.package_id"
                + " and a.msisdn = ? and a.status = 1"
                + " ORDER BY a.updated_at DESC, a.sub_package_id DESC LIMIT 1";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                subInfo.setPackageId(rs.getInt("package_id"));
                subInfo.setFreeMinutes(rs.getInt("free_minutes"));
                subInfo.setPackageName(rs.getString("package_name"));
                subInfo.setExpireAt(formatter.format(rs.getTimestamp("expire_at")));
                subInfo.setSubFee(rs.getInt("sub_fee"));
                int datediff = rs.getInt("datediff");
                if (datediff > 0) {
                    subInfo.setExpired(true);
                } else {
                    subInfo.setExpired(false);
                }
                subInfo.setErrorCode(BillingErrorCode.Success);
            } else {
                subInfo.setErrorCode(BillingErrorCode.NotFoundData);
                /*
                 * Chua dang ky dich vu => du dieu kien KM tat ca goi cuoc
                 */
                if (Boolean.parseBoolean(ConfigStack.getConfig("api_promotion", "enable", "false"))) {
                    subInfo.setPromotion(PackagePromotionStatus.PROMOTION_ALL);
                }
            }
        } catch (Exception e) {
            logger.error("Exception in checkSubPackage for " + msisdn, e);
            subInfo.setErrorCode(BillingErrorCode.SystemError);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return subInfo;
    }

    public BillingErrorCode insertSubPackage(SubPackageInfo sub) {
        BillingErrorCode result = BillingErrorCode.SystemError;
        Integer subPackageId = 0;
        DbConnection cnn = null;
        PreparedStatement stmt_ins = null;
        PreparedStatement stmt_up = null;
        try {
            cnn = getConnection();
            String sql_up = "update sub_package set status = ? where msisdn = ? and status = ? ";

            String sql = "";
            if (sub.getRegAt() == null || sub.getRegAt().trim().equalsIgnoreCase("")) {
                sql = "insert into sub_package(msisdn, package_id, free_minutes, source, expire_at, "
                        + " status, charge_success, updated_at, reg_at,command,reg_type)  "
                        + " values(?, ?, ?, ?, ?, ?, ?, NOW(), NOW(),?,?)";
            } else {
                sql = "insert into sub_package(msisdn, package_id, free_minutes, source, reg_at, expire_at, "
                        + " status, charge_success, updated_at,command,reg_type)  "
                        + " values(?, ?, ?, ?, ?, ?, ?, ?, NOW(),?,?)";
            }

            stmt_up = cnn.getConnection().prepareStatement(sql_up);
            stmt_up.setInt(1, SubPackageStatus.Cancel.getValue());
            stmt_up.setString(2, sub.getMsisdn());
            stmt_up.setInt(3, SubPackageStatus.Active.getValue());
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_up.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt_up.executeUpdate();
            stmt_ins = cnn.getConnection().prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS);
            if (sub.getRegAt() == null || sub.getRegAt().trim().equalsIgnoreCase("")) {
                stmt_ins.setString(1, sub.getMsisdn());
                stmt_ins.setInt(2, sub.getPackageId());
                stmt_ins.setInt(3, sub.getFreeMinutes());
                stmt_ins.setString(4, sub.getSourceReg());
                stmt_ins.setString(5, sub.getExpireAt());
                stmt_ins.setInt(6, SubPackageStatus.Active.getValue());
                stmt_ins.setInt(7, sub.getRenewSuccess());
                stmt_ins.setString(8, sub.getCommand());
                stmt_ins.setInt(9, sub.getRegisterType());
            } else {
                stmt_ins.setString(1, sub.getMsisdn());
                stmt_ins.setInt(2, sub.getPackageId());
                stmt_ins.setInt(3, sub.getFreeMinutes());
                stmt_ins.setString(4, sub.getSourceReg());
                stmt_ins.setString(5, sub.getRegAt());
                stmt_ins.setString(6, sub.getExpireAt());
                stmt_ins.setInt(7, SubPackageStatus.Active.getValue());
                stmt_ins.setInt(8, sub.getRenewSuccess());
                stmt_ins.setString(9, sub.getCommand());
                stmt_ins.setInt(10, sub.getRegisterType());
            }
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_ins.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt_ins.executeUpdate();

            // Lay sub_package_id (auto_increment)
            ResultSet rs = stmt_ins.getGeneratedKeys();
            if (rs.next()) {
                subPackageId = rs.getInt(1);
            }

            if (subPackageId > 0) {
                cnn.getConnection().commit();
                sub.setSubPackageId(subPackageId);
                result = BillingErrorCode.Success;
            } else {
                rollbackTransaction(cnn);
            }
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("SQLException in insertSubPackage: " + sub.getMsisdn(), e);
        } finally {
            closeStatement(stmt_up);
            closeStatement(stmt_ins);
            freeConnection(cnn);
        }

        return result;
    }

    /**
     * Huy goi cuoc dang su dung cua thue bao
     *
     * @param msisdn
     * @return
     */
    public SubPackageInfo cancelSubPackage(String msisdn) {
        SubPackageInfo subInfo = new SubPackageInfo();
        subInfo.setErrorCode(BillingErrorCode.SystemError);

        DbConnection cnn = null;
        PreparedStatement stmt_sub = null;
        PreparedStatement stmt_get = null;
        ResultSet rs = null;
        String sql_up_sub = "update sub_package set status = ?, updated_at = date_add(NOW(), interval -1 second) where sub_package_id = ?";
        String sql_get_acc = "SELECT a.package_name,b.sub_package_id, b.msisdn, b.package_id, b.free_minutes, b.status, b.reg_at, "
                + " b.updated_at, b.expire_at, b.source, b.charge_success, b.expire_at < NOW() AS expired "
                + " FROM package a, sub_package b"
                + " WHERE a.package_id = b.package_id"
                + " and b.msisdn = ? "
                + " ORDER BY b.updated_at DESC, b.sub_package_id DESC LIMIT 1";
        try {
            cnn = getConnection();
            stmt_get = cnn.getConnection().prepareStatement(sql_get_acc);
            stmt_get.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_get.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt_get.executeQuery();
            if (rs.next()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                subInfo.setSubPackageId(rs.getInt("sub_package_id"));
                subInfo.setMsisdn(rs.getString("msisdn"));
                subInfo.setPackageId(rs.getInt("package_id"));
                subInfo.setUpdatedAt(dateFormat.format(rs.getTimestamp("updated_at")));
                subInfo.setRegAt(dateFormat.format(rs.getTimestamp("reg_at")));
                subInfo.setExpireAt(dateFormat.format(rs.getTimestamp("expire_at")));
                SubPackageStatus stt = rs.getInt("status") == SubPackageStatus.Active.getValue() ? SubPackageStatus.Active
                        : SubPackageStatus.Cancel;
                subInfo.setStatus(stt);
                subInfo.setFreeMinutes(rs.getInt("free_minutes"));
                subInfo.setExpired(rs.getInt("expired") == 0 ? false : true);
                subInfo.setSourceReg(rs.getString("source"));
                subInfo.setRenewSuccess(rs.getInt("charge_success"));
                subInfo.setPackageName(rs.getString("package_name"));
                if (subInfo.getStatus() == SubPackageStatus.Active) {
                    /*
                     * Huy goi cuoc dang active
                     */
                    stmt_sub = cnn.getConnection().prepareStatement(sql_up_sub);
                    stmt_sub.setInt(1, SubPackageStatus.Cancel.getValue());
                    stmt_sub.setInt(2, subInfo.getSubPackageId());
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt_sub.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    stmt_sub.executeUpdate();

                    cnn.getConnection().commit();
                    subInfo.setErrorCode(BillingErrorCode.Success);
                } else {
                    /*
                     * Goi cuoc da huy truoc do
                     */
                    subInfo.setErrorCode(BillingErrorCode.NotFoundData);
                }
            } else {
                /*
                 * Chua dang ky dich vu
                 */
                subInfo.setErrorCode(BillingErrorCode.NotFoundData);
            }
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("SQLException in cancelSubPackage", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt_get);
            closeStatement(stmt_sub);
            freeConnection(cnn);
        }

        return subInfo;
    }

    public SubPackageInfo cancelSubPackage(String msisdn, String package_id) {
        SubPackageInfo subInfo = new SubPackageInfo();
        subInfo.setErrorCode(BillingErrorCode.SystemError);

        DbConnection cnn = null;
        PreparedStatement stmt_sub = null;
        PreparedStatement stmt_get = null;
        ResultSet rs = null;
        String sql_up_sub = "update sub_package set status = ?, updated_at = date_add(NOW(), interval -1 second) where sub_package_id = ?";
        String sql_get_acc = "SELECT a.package_name,b.sub_package_id, b.msisdn, b.package_id, b.free_minutes, b.status, b.reg_at, "
                + " b.updated_at, b.expire_at, b.source, b.charge_success, b.expire_at < NOW() AS expired "
                + " FROM package a, sub_package b"
                + " WHERE a.package_id = b.package_id"
                + " and b.msisdn = ?"
                + " and b.package_id = ?"
                + " ORDER BY b.updated_at DESC, b.sub_package_id DESC LIMIT 1";
        try {
            cnn = getConnection();
            stmt_get = cnn.getConnection().prepareStatement(sql_get_acc);
            stmt_get.setString(1, msisdn);
            stmt_get.setString(2, package_id);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_get.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt_get.executeQuery();
            if (rs.next()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                subInfo.setSubPackageId(rs.getInt("sub_package_id"));
                subInfo.setMsisdn(rs.getString("msisdn"));
                subInfo.setPackageId(rs.getInt("package_id"));
                subInfo.setUpdatedAt(dateFormat.format(rs.getTimestamp("updated_at")));
                subInfo.setRegAt(dateFormat.format(rs.getTimestamp("reg_at")));
                subInfo.setExpireAt(dateFormat.format(rs.getTimestamp("expire_at")));
                SubPackageStatus stt = rs.getInt("status") == SubPackageStatus.Active.getValue() ? SubPackageStatus.Active
                        : SubPackageStatus.Cancel;
                subInfo.setStatus(stt);
                subInfo.setFreeMinutes(rs.getInt("free_minutes"));
                subInfo.setExpired(rs.getInt("expired") == 0 ? false : true);
                subInfo.setSourceReg(rs.getString("source"));
                subInfo.setRenewSuccess(rs.getInt("charge_success"));
                subInfo.setPackageName(rs.getString("package_name"));
                if (subInfo.getStatus() == SubPackageStatus.Active) {
                    /*
                     * Huy goi cuoc dang active
                     */
                    stmt_sub = cnn.getConnection().prepareStatement(sql_up_sub);
                    stmt_sub.setInt(1, SubPackageStatus.Cancel.getValue());
                    stmt_sub.setInt(2, subInfo.getSubPackageId());
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt_sub.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    stmt_sub.executeUpdate();

                    cnn.getConnection().commit();
                    subInfo.setErrorCode(BillingErrorCode.Success);
                } else {
                    /*
                     * Goi cuoc da huy truoc do
                     */
                    subInfo.setErrorCode(BillingErrorCode.NotFoundData);
                }
            } else {
                /*
                 * Chua dang ky dich vu
                 */
                subInfo.setErrorCode(BillingErrorCode.NotFoundData);
            }
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("SQLException in cancelSubPackage", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt_get);
            closeStatement(stmt_sub);
            freeConnection(cnn);
        }

        return subInfo;
    }

    public BillingErrorCode renewSubPackage(SubPackageInfo subInfo) {
        BillingErrorCode result = BillingErrorCode.SystemError;

        DbConnection cnn = null;
        PreparedStatement stmt_pre_up = null;
        PreparedStatement stmt_up = null;
        String sql_pre_up_sub = "update sub_package set last_expire_day = DATEDIFF(NOW(),expire_at) WHERE sub_package_id = ? AND status = ?";
        String sql_up_sub = "update sub_package set expire_at = ?, charge_success = ?, free_minutes = ? , updated_at = NOW(),next_charge_fee = ?, debit_fee = ? "
                + "where sub_package_id = ? and status = ?";
        String sql_up_sub_money = "update sub_package set next_charge_fee = ?, debit_fee = ? "
                + "where sub_package_id = ? and status = ?";
        try {
            cnn = getConnection();
            // Pre
            stmt_pre_up = cnn.getConnection().prepareStatement(sql_pre_up_sub);
            stmt_pre_up.setInt(1, subInfo.getSubPackageId());
            stmt_pre_up.setInt(2, SubPackageStatus.Active.getValue());
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_pre_up.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt_pre_up.executeUpdate();
            if (subInfo.isUpdateFeeOnly()) {
                // Money
                stmt_up = cnn.getConnection().prepareStatement(sql_up_sub_money);
                stmt_up.setInt(1, subInfo.getNextChargeFee());
                stmt_up.setInt(2, subInfo.getDebitFee());
                stmt_up.setInt(3, subInfo.getSubPackageId());
                stmt_up.setInt(4, SubPackageStatus.Active.getValue());
            } else {
                // Sub
                stmt_up = cnn.getConnection().prepareStatement(sql_up_sub);
                stmt_up.setString(1, subInfo.getExpireAt());
                stmt_up.setInt(2, subInfo.getRenewSuccess());
                stmt_up.setInt(3, subInfo.getFreeMinutes());
                stmt_up.setInt(4, subInfo.getNextChargeFee());
                stmt_up.setInt(5, subInfo.getDebitFee());
                stmt_up.setInt(6, subInfo.getSubPackageId());
                stmt_up.setInt(7, SubPackageStatus.Active.getValue());
            }
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_up.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt_up.executeUpdate();
            cnn.getConnection().commit();
            result = BillingErrorCode.Success;
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error(
                    "SQLException in renewSubPackage for "
                    + subInfo.getMsisdn(), e);
        } finally {
            closeStatement(stmt_pre_up);
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return result;
    }

    public BillingErrorCode reActiveSubPackage(SubPackageInfo subInfo) {
        BillingErrorCode result = BillingErrorCode.SystemError;

        DbConnection cnn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt_up = null;
        String sql = "update sub_package set status = ? where sub_package_id <> ? and msisdn = ? and status = ?";
        String sql_up_sub = "update sub_package set status = ?, updated_at = NOW() where sub_package_id = ? and status = ?";

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, SubPackageStatus.Cancel.getValue());
            stmt.setInt(2, subInfo.getSubPackageId());
            stmt.setString(3, subInfo.getMsisdn());
            stmt.setInt(4, SubPackageStatus.Active.getValue());
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.executeUpdate();

            stmt_up = cnn.getConnection().prepareStatement(sql_up_sub);
            stmt_up.setInt(1, SubPackageStatus.Active.getValue());
            stmt_up.setInt(2, subInfo.getSubPackageId());
            stmt_up.setInt(3, SubPackageStatus.Cancel.getValue());
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_up.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }                String sql_log = stmt_up.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
            stmt_up.executeUpdate();
            cnn.getConnection().commit();
            result = BillingErrorCode.Success;
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error(
                    "SQLException in reActiveSubPackage for "
                    + subInfo.getMsisdn(), e);
        } finally {
            closeStatement(stmt);
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return result;
    }

    public BillingErrorCode insertBillingActivity(BillingActivityInfo actInfo, SubPackageInfo sub, PackageInfo p) {
        BillingErrorCode result = BillingErrorCode.SystemError;
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        String sql = "insert into billing_activity(request_id, msisdn, package_id, sub_package_id, "
                + " billing_type, amount, result, source, `desc`, promotion, billing_at) "
                + " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try {
            boolean logFile = false;
            if (Integer.parseInt(ConfigStack.getConfig("api_log", "billing", "0")) < 2) {
                cnn = getConnection();
                stmt = cnn.getConnection().prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, actInfo.getRequestId());
                stmt.setString(2, actInfo.getMsisdn());
                stmt.setInt(3, actInfo.getPackageId());
                stmt.setInt(4, actInfo.getSubPackageId());
                stmt.setInt(5, actInfo.getBillingType());
                stmt.setInt(6, actInfo.getAmount());
                stmt.setString(7, actInfo.getResult());
                stmt.setString(8, actInfo.getSource());
                stmt.setString(9, actInfo.getDescription());
                stmt.setInt(10, actInfo.getPromotion());
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    actInfo.setBillingActivityId(rs.getInt(1));
                    cnn.getConnection().commit();
                    result = BillingErrorCode.Success;
                } else {
                    rollbackTransaction(cnn);
                }
                if (Integer.parseInt(ConfigStack.getConfig("api_log", "billing", "0")) == 0) {
                    logFile = true;
                }
            }
            if (Integer.parseInt(ConfigStack.getConfig("api_log", "billing", "0")) == 2 || logFile) {
                if (sub == null && p == null) {
                    LogFileStack.logBilling("system", actInfo.getMsisdn(), actInfo.getPackageId() + "", "", "", actInfo.getAction(), "0", "0", actInfo.getAmount() + "", actInfo.getBillingRequest(), actInfo.getBillingAt(), actInfo.getResult(), actInfo.getDescription());
                } else {
                    LogFileStack.logBilling("system", actInfo.getMsisdn(), actInfo.getPackageId() + "", p.getPackageName(), sub.getExpireAt(), actInfo.getAction(), "0", "0", p.getSubFee() + "", actInfo.getBillingRequest(), actInfo.getBillingAt(), actInfo.getResult(), actInfo.getDescription());
                }
            }

        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("SQLException in insertBillingActivity", e);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return result;
    }

    public BillingErrorCode insertCCU(ArrayList<CCUInfo> ccuList) {
        BillingErrorCode result = BillingErrorCode.SystemError;
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        try {
            if (Integer.parseInt(ConfigStack.getConfig("api_log", "ccu", "0")) == 1) {
                for (int i = 0; i < ccuList.size(); i++) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                    Date now = new Date();
                    LogFileStack.logCCU(format.format(now), ccuList.get(i).getCcu_total(), ccuList.get(i).getCcu_type(), ccuList.get(i).getPackage_id(), ccuList.get(i).getKey());
                }
            }
            result = BillingErrorCode.Success;
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("Exception in insertCCU", e);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return result;
    }

    public BillingErrorCode insertChargeLocking(String msisdn,
            Integer chargeType) {
        BillingErrorCode result = BillingErrorCode.SystemError;
        DbConnection cnn = null;
        PreparedStatement stmt_del = null;
        PreparedStatement stmt_ins = null;
        String sql_del = "delete from charge_locking where msisdn = ? and charge_type = ? and TIMESTAMPDIFF(MINUTE, created_at, NOW()) >= ?";
        String sql_ins = "insert ignore into charge_locking(msisdn, charge_type, created_at) values(?, ?, NOW())";

        try {
            cnn = getConnection();
            stmt_del = cnn.getConnection().prepareStatement(sql_del);
            stmt_del.setString(1, msisdn);
            stmt_del.setInt(2, chargeType);
            stmt_del.setInt(3, Integer.parseInt(ConfigStack.getConfig("api_billing", "lock_charging_time", "0")));
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_del.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt_del.executeUpdate();

            stmt_ins = cnn.getConnection().prepareStatement(sql_ins);
            stmt_ins.setString(1, msisdn);
            stmt_ins.setInt(2, chargeType);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_ins.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            int count = stmt_ins.executeUpdate();
            cnn.getConnection().commit();

            if (count > 0) {
                result = BillingErrorCode.Success;
            } else {
                result = BillingErrorCode.ChargingSubProcessing;
            }
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("SQLException in insertChargeLocking for " + msisdn, e);
        } finally {
            closeStatement(stmt_del);
            closeStatement(stmt_ins);
            freeConnection(cnn);
        }

        return result;
    }

    public BillingErrorCode deleteChargeLocking(String msisdn,
            Integer chargeType) {
        BillingErrorCode result = BillingErrorCode.SystemError;
        DbConnection cnn = null;
        PreparedStatement stmt_del = null;
        String sql_del = "delete from charge_locking where msisdn = ? and charge_type = ?";

        try {
            cnn = getConnection();
            stmt_del = cnn.getConnection().prepareStatement(sql_del);
            stmt_del.setString(1, msisdn);
            stmt_del.setInt(2, chargeType);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_del.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt_del.executeUpdate();
            cnn.getConnection().commit();
            result = BillingErrorCode.Success;
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("SQLException in deleteChargeLocking for " + msisdn, e);
        } finally {
            closeStatement(stmt_del);
            freeConnection(cnn);
        }

        return result;
    }

    public HashMap<String, Object> insertLogIVR(String msisdn, Date begin_at,
            Date end_at, String dtmf, int package_id, int sub_package_id) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        int i = 1;
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        try {
            boolean logFile = false;
            if (Integer.parseInt(ConfigStack.getConfig("api_log", "dtmf", "0")) < 2) {
                cnn = getConnection();
                Timestamp beginTime = DateUtil.getTimeStamp(begin_at);
                Timestamp endTime = DateUtil.getTimeStamp(end_at);
                String sql = "INSERT INTO `dtmf_activity` (`msisdn`, `begin_at`, `end_at`, `dtmf`, `package_id`, `sub_package_id`)"
                        + " VALUES (?, ?, ?, ?, ?, ?)";
                stmt = cnn.getConnection().prepareStatement(sql);
                stmt.setString(i++, msisdn);
                stmt.setTimestamp(i++, beginTime);
                stmt.setTimestamp(i++, endTime);
                stmt.setString(i++, dtmf);
                stmt.setInt(i++, package_id);
                stmt.setInt(i++, sub_package_id);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                stmt.executeUpdate();
                if (Integer.parseInt(ConfigStack.getConfig("api_log", "dtmf", "0")) == 0) {
                    logFile = true;
                }
            }
            if (Integer.parseInt(ConfigStack.getConfig("api_log", "dtmf", "0")) == 2 || logFile) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                LogFileStack.logDTMF(format.format(begin_at), format.format(end_at), msisdn, dtmf);
            }
            resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("Error in insert Log Dtmf", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> insertAllSubListen(int sub_package_id, int package_id, String msisdn, String contentListen, int daily_call_id, String beginAt) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        try {
            cnn = getConnection();
            String[] content = contentListen.split("#");
            if (content.length > 0) {
                boolean logFile = false;
                if (Integer.parseInt(ConfigStack.getConfig("api_log", "sub_listen", "0")) < 2) {
                    String sql = "INSERT INTO sub_listen (sub_package_id,package_id,msisdn,channel_id,content_id,duration,channel_ord,topic_ord,daily_call_id,listen_date,topic_type)"
                            + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";
                    stmt = cnn.getConnection().prepareStatement(sql);
                    for (int j = 0; j < content.length; j++) {
                        String[] info = content[j].split("-");
                        if (info.length >= 6) {
                            int topic_type = Integer.parseInt(info[0]);
                            int topic_ord = Integer.parseInt(info[1]);
                            int channel_ord = Integer.parseInt(info[2]);
                            int channel_id = Integer.parseInt(info[3]);
                            int content_id = Integer.parseInt(info[4]);
                            int duration = Integer.parseInt(info[5]);
                            int i = 1;
                            stmt.setInt(i++, sub_package_id);
                            stmt.setInt(i++, package_id);
                            stmt.setString(i++, msisdn);
                            stmt.setInt(i++, channel_id);
                            stmt.setInt(i++, content_id);
                            stmt.setInt(i++, duration);
                            stmt.setInt(i++, channel_ord);
                            stmt.setInt(i++, topic_ord);
                            stmt.setInt(i++, daily_call_id);
                            stmt.setString(i++, beginAt);
                            stmt.setInt(i++, topic_type);
                            if (logger.isDebugEnabled()) {
                                String sql_log = stmt.toString();
                                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                                logger.debug(sql_log);
                            }
                            stmt.addBatch();
                        } else {
                            logger.debug("Tham so contentListen k du du lieu de insert");
                        }
                    }
                    if (stmt != null) {
                        stmt.executeBatch();
                    }
                    cnn.getConnection().commit();
                    if (Integer.parseInt(ConfigStack.getConfig("api_log", "sub_listen", "0")) == 0) {
                        logFile = true;
                    }
                }
                if (Integer.parseInt(ConfigStack.getConfig("api_log", "sub_listen", "0")) == 2 || logFile) {
                    for (int j = 0; j < content.length; j++) {
                        String[] info = content[j].split("-");
                        if (info.length >= 6) {
                            int topic_type = Integer.parseInt(info[0]);
                            int topic_ord = Integer.parseInt(info[1]);
                            int channel_ord = Integer.parseInt(info[2]);
                            int channel_id = Integer.parseInt(info[3]);
                            int content_id = Integer.parseInt(info[4]);
                            int duration = Integer.parseInt(info[5]);
                            String topic_name = getListTopics().get(topic_ord);
                            String channel_name = getListChannels().get(channel_id);
                            String content_name = "";
                            if (topic_type == 1) {
                                content_name = getListMusicContents().get(content_id);
                            }
                            if (topic_type == 2) {
                                content_name = getListStoryContents().get(content_id);
                            }
                            if (topic_type == 3) {
                                content_name = getListNewsContents().get(content_id);
                            }
                            if (topic_type == 4) {
                                content_name = getListFunContents().get(content_id);
                            }
                            if (topic_type == 5) {
                                content_name = getListHoroContents().get(content_id);
                            }
                            LogFileStack.logSubListen(beginAt, daily_call_id, sub_package_id, package_id, msisdn, topic_name, channel_name, content_id, content_name, duration, topic_ord, channel_ord, channel_id, topic_type);
                        } else {
                            logger.debug("Tham so contentListen k du du lieu de log");
                        }
                    }
                }
            }
            resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("Error in insertSubListen", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListMusicByChannel(
            int topic_ord, int channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getListMusicByChannel." + topic_ord + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getListMusicByChannel from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListMusicByChannel from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt3 = null;
        ResultSet re3 = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id,b.play_type,b.play_limit,b.limit_type "
                    + "from content_topic a, content_channel b "
                    + "where a.topic_id = b.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 "
                    + "and a.topic_ord = ? and a.topic_type = ? and b.channel_ord = ? "
                    + "and a.status = 1 and b.status = 1 ";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, topic_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setInt(3, channel_ord);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int channel_id = re1.getInt("channel_id");
                int play_type = re1.getInt("play_type");
                int play_limit = re1.getInt("play_limit");
                if (play_limit == 0) {
                    play_limit = Integer.MAX_VALUE;
                }
                int limit_type = re1.getInt("limit_type");
                resp.put("channel_id", channel_id);
                resp.put("play_type", play_type);
                sql = "Select a.music_content_id,b.content_path,a.content_ord,b.name_path,b.code_crbt,b.content_duration,b.code "
                        + "from music_content_channel a, music_content b "
                        + "where a.channel_id = ? "
                        + "and a.music_content_id = b.music_content_id "
                        + "and b.ivr_publish = 1 "
                        + "and a.status = 1 and b.status = 1 "
                        + "and b.publish_date <= now() "
                        + "order by a.content_ord desc, b.publish_date desc limit 0," + play_limit;
                if (limit_type == 1) {
                    sql = "Select a.music_content_id,b.content_path,a.content_ord,b.name_path,b.code_crbt,b.content_duration,b.code "
                            + "from music_content_channel a, music_content b "
                            + "where a.channel_id = ? "
                            + "and a.music_content_id = b.music_content_id "
                            + "and b.ivr_publish = 1 "
                            + "and a.status = 1 and b.status = 1 "
                            + "and DATEDIFF(NOW(), b.publish_date) < " + play_limit + " "
                            + "and b.publish_date <= now() "
                            + "order by a.content_ord desc, b.publish_date desc";
                }

                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setInt(1, channel_id);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re3 = stmt3.executeQuery();
                ArrayList<Content> data = new ArrayList<Content>();
                while (re3.next()) {
                    String contentId = re3.getString("music_content_id");
                    String contentPath = re3.getString("content_path");
                    String namePath = re3.getString("name_path");
                    String contentOrd = re3.getString("content_ord");
                    String codeRbt = re3.getString("code_crbt");
                    String duration = re3.getString("content_duration");
                    String code = re3.getString("code");
                    Content a = new Content();
                    a.setContentId(contentId);
                    a.setContentPath(contentPath);
                    a.setContentOrd(contentOrd);
                    a.setNamePath(namePath);
                    a.setCodeRbt(codeRbt);
                    a.setDuration(duration);
                    a.setCode(code);
                    data.add(a);
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListMusicByChannel", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re3);
            closeStatement(stmt1);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListHoroByChannel(
            int topic_ord, int channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getListHoroByChannel." + topic_ord + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getListHoroByChannel from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListHoroByChannel from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt3 = null;
        ResultSet re3 = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id,b.play_type,b.play_limit,b.limit_type "
                    + "from content_topic a, content_channel b "
                    + "where a.topic_id = b.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 "
                    + "and a.topic_ord = ? and a.topic_type = ? and b.channel_ord = ? "
                    + "and a.status = 1 and b.status = 1 ";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, topic_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setInt(3, channel_ord);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int channel_id = re1.getInt("channel_id");
                int play_type = re1.getInt("play_type");
                int play_limit = re1.getInt("play_limit");
                if (play_limit == 0) {
                    play_limit = Integer.MAX_VALUE;
                }
                int limit_type = re1.getInt("limit_type");
                resp.put("channel_id", channel_id);
                resp.put("play_type", play_type);
                sql = "Select horo_content_id,content_path,content_ord,content_duration from horo_content "
                        + "where channel_id = ? "
                        + "and status = 1 "
                        + "and ivr_publish = 1 "
                        + "and publish_date <= now() "
                        + "order by content_ord desc, publish_date desc limit 0," + play_limit;
                if (limit_type == 1) {
                    sql = "Select horo_content_id,content_path,content_ord,content_duration from horo_content "
                            + "where channel_id = ? "
                            + "and status = 1 "
                            + "and ivr_publish = 1 "
                            + "and DATEDIFF(now(),publish_date) < " + play_limit + " "
                            + "and publish_date <= now() "
                            + "order by content_ord desc, publish_date desc";
                }

                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setInt(1, channel_id);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re3 = stmt3.executeQuery();
                ArrayList<Content> data = new ArrayList<Content>();
                while (re3.next()) {
                    String contentId = re3.getString("horo_content_id");
                    String contentPath = re3.getString("content_path");
                    String contentOrd = re3.getString("content_ord");
                    String duration = re3.getString("content_duration");
                    Content a = new Content();
                    a.setContentId(contentId);
                    a.setContentPath(contentPath);
                    a.setContentOrd(contentOrd);
                    a.setDuration(duration);
                    data.add(a);
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListHoroByChannel", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re3);
            closeStatement(stmt1);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListNewsByChannel(
            int topic_ord, int channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getListNewsByChannel." + topic_ord + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getListNewsByChannel from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListNewsByChannel from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt3 = null;
        ResultSet re3 = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id,b.play_type,b.play_limit,b.limit_type "
                    + "from content_topic a, content_channel b "
                    + "where a.topic_id = b.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 "
                    + "and a.topic_ord = ? and a.topic_type = ? and b.channel_ord = ? "
                    + "and a.status = 1 and b.status = 1 ";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, topic_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setInt(3, channel_ord);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int channel_id = re1.getInt("channel_id");
                int play_type = re1.getInt("play_type");
                int play_limit = re1.getInt("play_limit");
                if (play_limit == 0) {
                    play_limit = Integer.MAX_VALUE;
                }
                int limit_type = re1.getInt("limit_type");
                resp.put("channel_id", channel_id);
                resp.put("play_type", play_type);
                sql = "Select a.news_content_id,b.content_path,a.content_ord,b.content_duration,b.top_status,"
                        + "b.g_question_path,b.g_answer_keys,b.g_right_key,b.g_right_path,b.g_wrong_path "
                        + "from news_content_channel a, news_content b "
                        + "where a.channel_id = ? "
                        + "and a.news_content_id = b.news_content_id "
                        + "and b.ivr_publish = 1 "
                        + "and a.status = 1 and b.status = 1 "
                        + "and b.publish_date <= now() "
                        + "order by b.top_status desc,a.content_ord desc, b.publish_date desc limit 0," + play_limit;
                if (limit_type == 1) {
                    sql = "Select a.news_content_id,b.content_path,a.content_ord,b.content_duration,b.top_status,"
                            + "b.g_question_path,b.g_answer_keys,b.g_right_key,b.g_right_path,b.g_wrong_path "
                            + "from news_content_channel a, news_content b "
                            + "where a.channel_id = ? "
                            + "and a.news_content_id = b.news_content_id "
                            + "and b.ivr_publish = 1 "
                            + "and a.status = 1 and b.status = 1 "
                            + "and DATEDIFF(now(),b.publish_date) < " + play_limit + " "
                            + "and b.publish_date <= now() "
                            + "order by b.top_status desc,a.content_ord desc, b.publish_date desc";
                }
                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setInt(1, channel_id);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re3 = stmt3.executeQuery();
                ArrayList<Content> data = new ArrayList<Content>();
                while (re3.next()) {
                    String contentId = re3.getString("news_content_id");
                    String contentPath = re3.getString("content_path");
                    String contentOrd = re3.getString("content_ord");
                    String duration = re3.getString("content_duration");
                    String gQuestionPath = re3.getString("g_question_path");
                    String gAnswer = re3.getString("g_answer_keys");
                    String gRightKey = re3.getString("g_right_key");
                    String gRightPath = re3.getString("g_right_path");
                    String gWrongPath = re3.getString("g_wrong_path");

                    Content a = new Content();
                    a.setContentId(contentId);
                    a.setContentPath(contentPath);
                    a.setContentOrd(contentOrd);
                    a.setDuration(duration);
                    a.setG_question_path(gQuestionPath);
                    a.setG_answer_keys(gAnswer);
                    a.setG_right_key(gRightKey);
                    a.setG_right_path(gRightPath);
                    a.setG_wrong_path(gWrongPath);
                    data.add(a);
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListNewsByChannel", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re3);
            closeStatement(stmt1);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListFunByChannel(int topic_ord, int channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getListFunByChannel." + topic_ord + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getListFunByChannel from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListFunByChannel from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt3 = null;
        ResultSet re3 = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id,b.play_type,b.play_limit,b.limit_type "
                    + "from content_topic a, content_channel b "
                    + "where a.topic_id = b.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 "
                    + "and a.topic_ord = ? and a.topic_type = ? and b.channel_ord = ? "
                    + "and a.status = 1 and b.status = 1 ";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, topic_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setInt(3, channel_ord);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int channel_id = re1.getInt("channel_id");
                int play_type = re1.getInt("play_type");
                int play_limit = re1.getInt("play_limit");
                if (play_limit == 0) {
                    play_limit = Integer.MAX_VALUE;
                }
                int limit_type = re1.getInt("limit_type");
                resp.put("channel_id", channel_id);
                resp.put("play_type", play_type);
                sql = "Select a.fun_content_id,b.content_path,a.content_ord,b.name_path,b.content_duration "
                        + "from fun_content_channel a, fun_content b "
                        + "where a.channel_id = ? "
                        + "and a.fun_content_id = b.fun_content_id "
                        + "and b.ivr_publish = 1 "
                        + "and a.status = 1 and b.status = 1 "
                        + "and b.publish_date <= now() "
                        + "order by a.content_ord desc, b.publish_date desc limit 0," + play_limit;
                if (limit_type == 1) {
                    sql = "Select a.fun_content_id,b.content_path,a.content_ord,b.name_path,b.content_duration "
                            + "from fun_content_channel a, fun_content b "
                            + "where a.channel_id = ? "
                            + "and a.fun_content_id = b.fun_content_id "
                            + "and b.ivr_publish = 1 "
                            + "and a.status = 1 and b.status = 1 "
                            + "and DATEDIFF(now(),b.publish_date) < " + play_limit + " "
                            + "and b.publish_date <= now() "
                            + "order by a.content_ord desc, b.publish_date desc";
                }
                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setInt(1, channel_id);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re3 = stmt3.executeQuery();
                ArrayList<Content> data = new ArrayList<Content>();
                while (re3.next()) {
                    String contentId = re3.getString("fun_content_id");
                    String contentPath = re3.getString("content_path");
                    String namePath = re3.getString("name_path");
                    String contentOrd = re3.getString("content_ord");
                    String duration = re3.getString("content_duration");
                    Content a = new Content();
                    a.setContentId(contentId);
                    a.setContentPath(contentPath);
                    a.setContentOrd(contentOrd);
                    a.setNamePath(namePath);
                    a.setDuration(duration);
                    data.add(a);
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListFunByChannel", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re3);
            closeStatement(stmt1);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListStoryByChannel(
            int topic_ord, int channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getListStoryByChannel." + topic_ord + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getListStoryByChannel from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListStoryByChannel from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt3 = null;
        ResultSet re3 = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id,b.play_type,b.play_limit,b.limit_type "
                    + "from content_topic a, content_channel b "
                    + "where a.topic_id = b.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 "
                    + "and a.topic_ord = ? and a.topic_type = ? and b.channel_ord = ? "
                    + "and a.status = 1 and b.status = 1 ";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, topic_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setInt(3, channel_ord);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int channel_id = re1.getInt("channel_id");
                int play_type = re1.getInt("play_type");
                int play_limit = re1.getInt("play_limit");
                if (play_limit == 0) {
                    play_limit = Integer.MAX_VALUE;
                }
                int limit_type = re1.getInt("limit_type");
                resp.put("channel_id", channel_id);
                resp.put("play_type", play_type);
                sql = "Select a.story_content_id,b.content_path,a.content_ord,b.name_path,b.summary_path,b.content_duration "
                        + "from story_content_channel a, story_content b "
                        + "where a.channel_id = ? "
                        + "and a.story_content_id = b.story_content_id "
                        + "and b.ivr_publish = 1 "
                        + "and a.status = 1 and b.status = 1 "
                        + "and b.publish_date <= now() "
                        + "order by a.content_ord desc, b.publish_date desc limit 0," + play_limit;
                if (limit_type == 1) {
                    sql = "Select a.story_content_id,b.content_path,a.content_ord,b.name_path,b.summary_path,b.content_duration "
                            + "from story_content_channel a, story_content b "
                            + "where a.channel_id = ? "
                            + "and a.story_content_id = b.story_content_id "
                            + "and b.ivr_publish = 1 "
                            + "and a.status = 1 and b.status = 1 "
                            + "and DATEDIFF(now(),publish_date) < " + play_limit + " "
                            + "and b.publish_date <= now() "
                            + "order by a.content_ord desc, b.publish_date desc";
                }
                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setInt(1, channel_id);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re3 = stmt3.executeQuery();
                ArrayList<Content> data = new ArrayList<Content>();
                while (re3.next()) {
                    String contentId = re3.getString("story_content_id");
                    String contentPath = re3.getString("content_path");
                    String namePath = re3.getString("name_path");
                    String contentOrd = re3.getString("content_ord");
                    String summaryPath = re3.getString("summary_path");
                    String duration = re3.getString("content_duration");
                    Content a = new Content();
                    a.setContentId(contentId);
                    a.setContentPath(contentPath);
                    a.setContentOrd(contentOrd);
                    a.setNamePath(namePath);
                    a.setSummaryPath(summaryPath);
                    a.setDuration(duration);
                    data.add(a);
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListStoryByChannel", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re3);
            closeStatement(stmt1);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListMusicMultiPartByChannel(
            int topic_ord, int channel_ord, int topic_type, int music_topic) {
        String key = this.getClass().getCanonicalName() + ".getListMusicMultiPartByChannel." + topic_ord + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getListMusicMultiPartByChannel from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListMusicMultiPartByChannel from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt3 = null;
        ResultSet re3 = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id from content_topic a, content_channel b "
                    + "where a.topic_id = b.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 "
                    + "and a.topic_ord = ? and a.topic_type = ? and b.channel_ord = ? "
                    + "and a.status = 1 and b.status = 1 ";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, topic_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setInt(3, channel_ord);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int channel_id = re1.getInt("channel_id");
                resp.put("channel_id", channel_id);
                int limit = 0;
                if (music_topic > 0) {
                    limit = 1;
                }
                sql = "Select a.music_content_id,a.content_ord,b.name_path,b.total_part "
                        + "from music_content_channel a, music_content b "
                        + "where a.channel_id = ? "
                        + "and a.music_content_id = b.music_content_id "
                        + "and b.ivr_publish = 1 "
                        + "and a.status = 1 and b.status = 1 "
                        + "and b.publish_date <= now() "
                        + "order by a.content_ord desc, b.publish_date desc limit " + limit + "," + Integer.parseInt(ConfigStack.getConfig("api_general", "content_max", "100"));

                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setInt(1, channel_id);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re3 = stmt3.executeQuery();
                ArrayList<Content> data = new ArrayList<Content>();
                while (re3.next()) {
                    String contentId = re3.getString("music_content_id");
                    String namePath = re3.getString("name_path");
                    String contentOrd = re3.getString("content_ord");
                    String totalPart = re3.getString("total_part");
                    Content a = new Content();
                    a.setContentId(contentId);
                    a.setContentOrd(contentOrd);
                    a.setNamePath(namePath);
                    a.setTotalPart(totalPart);
                    data.add(a);
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListMusicMultiPartByChannel", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re3);
            closeStatement(stmt1);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListStoryMultiPartByChannel(
            int topic_ord, int channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getListStoryMultiPartByChannel." + topic_ord + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getListStoryMultiPartByChannel from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListStoryMultiPartByChannel from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt3 = null;
        ResultSet re3 = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id,b.play_type,b.play_limit,b.limit_type "
                    + "from content_topic a, content_channel b "
                    + "where a.topic_id = b.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 "
                    + "and a.topic_ord = ? and a.topic_type = ? and b.channel_ord = ? "
                    + "and a.status = 1 and b.status = 1 ";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, topic_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setInt(3, channel_ord);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int channel_id = re1.getInt("channel_id");
                int play_limit = re1.getInt("play_limit");
                if (play_limit == 0) {
                    play_limit = Integer.MAX_VALUE;
                }
                int limit_type = re1.getInt("limit_type");
                resp.put("channel_id", channel_id);
                resp.put("play_limit", play_limit);
                sql = "Select a.story_content_id,a.content_ord,b.name_path,b.total_part "
                        + "from story_content_channel a, story_content b "
                        + "where a.channel_id = ? "
                        + "and a.story_content_id = b.story_content_id "
                        + "and b.ivr_publish = 1 "
                        + "and a.status = 1 and b.status = 1 "
                        + "and b.publish_date <= now() "
                        + "order by a.content_ord desc, b.publish_date desc limit 0," + play_limit;
                if (limit_type == 1) {
                    sql = "Select a.story_content_id,a.content_ord,b.name_path,b.total_part "
                            + "from story_content_channel a, story_content b "
                            + "where a.channel_id = ? "
                            + "and a.story_content_id = b.story_content_id "
                            + "and b.ivr_publish = 1 "
                            + "and a.status = 1 and b.status = 1 "
                            + "and DATEDIFF(now(),b.publish_date) < " + play_limit + " "
                            + "and b.publish_date <= now() "
                            + "order by a.content_ord desc, b.publish_date desc";
                }
                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setInt(1, channel_id);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re3 = stmt3.executeQuery();
                ArrayList<Content> data = new ArrayList<Content>();
                while (re3.next()) {
                    String contentId = re3.getString("story_content_id");
                    String namePath = re3.getString("name_path");
                    String contentOrd = re3.getString("content_ord");
                    String totalPart = re3.getString("total_part");
                    Content a = new Content();
                    if (namePath != null && !namePath.equals("")) {
                        a.setContentId(contentId);
                        a.setContentOrd(contentOrd);
                        a.setNamePath(namePath);
                        a.setTotalPart(totalPart);
                        data.add(a);
                    }
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }

            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListStoryMultiPartByChannel", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re3);
            closeStatement(stmt1);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListFunMultiPartByChannel(
            int topic_ord, int channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getListFunMultiPartByChannel." + topic_ord + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getListFunMultiPartByChannel from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListFunMultiPartByChannel from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt3 = null;
        ResultSet re3 = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id,b.play_type,b.play_limit,b.limit_type "
                    + "from content_topic a, content_channel b "
                    + "where a.topic_id = b.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 "
                    + "and a.topic_ord = ? and a.topic_type = ? and b.channel_ord = ? "
                    + "and a.status = 1 and b.status = 1 ";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, topic_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setInt(3, channel_ord);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int channel_id = re1.getInt("channel_id");
                int play_limit = re1.getInt("play_limit");
                if (play_limit == 0) {
                    play_limit = Integer.MAX_VALUE;
                }
                int limit_type = re1.getInt("limit_type");
                resp.put("channel_id", channel_id);
                sql = "Select a.fun_content_id,a.content_ord,b.name_path,b.total_part "
                        + "from fun_content_channel a, fun_content b "
                        + "where a.channel_id = ? "
                        + "and a.fun_content_id = b.fun_content_id "
                        + "and b.ivr_publish = 1 "
                        + "and a.status = 1 and b.status = 1 "
                        + "and b.publish_date <= now() "
                        + "order by a.content_ord desc, b.publish_date desc limit 0," + play_limit;
                if (limit_type == 1) {
                    sql = "Select a.fun_content_id,a.content_ord,b.name_path,b.total_part "
                            + "from fun_content_channel a, fun_content b "
                            + "where a.channel_id = ? "
                            + "and a.fun_content_id = b.fun_content_id "
                            + "and b.ivr_publish = 1 "
                            + "and a.status = 1 and b.status = 1 "
                            + "and DATEDIFF(now(),b.publish_date) < " + play_limit + " "
                            + "and b.publish_date <= now() "
                            + "order by a.content_ord desc, b.publish_date desc";
                }
                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setInt(1, channel_id);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re3 = stmt3.executeQuery();
                ArrayList<Content> data = new ArrayList<Content>();
                while (re3.next()) {
                    String contentId = re3.getString("fun_content_id");
                    String namePath = re3.getString("name_path");
                    String contentOrd = re3.getString("content_ord");
                    String totalPart = re3.getString("total_part");
                    Content a = new Content();
                    a.setContentId(contentId);
                    a.setContentOrd(contentOrd);
                    a.setNamePath(namePath);
                    a.setTotalPart(totalPart);
                    data.add(a);
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListFunMultiPartByChannel", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re3);
            closeStatement(stmt1);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getMusicMultiPart(int content_id) {
        String key = this.getClass().getCanonicalName() + ".getMusicMultiPart." + content_id;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getMusicMultiPart from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getMusicMultiPart from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "Select a.total_part,a.name_path,b.part_number,b.path,b.duration "
                    + "from music_content a, music_content_part b "
                    + "where a.music_content_id = b.music_content_id "
                    + "and a.ivr_publish = 1 "
                    + "and a.music_content_id = ? and a.status = 1 and a.publish_date <= now() "
                    + "and b.status = 1 order by b.part_number";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, content_id);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            while (re1.next()) {
                String totalPart = re1.getString("total_part");
                String namePath = re1.getString("name_path");
                String part_number = re1.getString("part_number");
                String path = re1.getString("path");
                String duration = re1.getString("duration");
                if (data.isEmpty()) {
                    resp.put("total_part", totalPart);
                    resp.put("name_path", namePath);
                }
                Content a = new Content();
                a.setPartNumber(part_number);
                a.setContentPath(path);
                a.setDuration(duration);
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                cache.put(new Element(key, resp));
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getMusicMultiPart", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getFunMultiPart(int content_id) {
        String key = this.getClass().getCanonicalName() + ".getFunMultiPart." + content_id;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getFunMultiPart from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getFunMultiPart from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "Select a.total_part,a.name_path,b.part_number,b.path,b.duration "
                    + "from fun_content a, fun_content_part b "
                    + "where a.fun_content_id = b.fun_content_id "
                    + "and a.ivr_publish = 1 "
                    + "and a.fun_content_id = ? and a.status = 1 and a.publish_date <= now() "
                    + "and b.status = 1 order by b.part_number";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, content_id);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            while (re1.next()) {
                String totalPart = re1.getString("total_part");
                String namePath = re1.getString("name_path");
                String part_number = re1.getString("part_number");
                String path = re1.getString("path");
                String duration = re1.getString("duration");
                if (data.isEmpty()) {
                    resp.put("total_part", totalPart);
                    resp.put("name_path", namePath);
                }
                Content a = new Content();
                a.setPartNumber(part_number);
                a.setContentPath(path);
                a.setDuration(duration);
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                cache.put(new Element(key, resp));
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getFunMultiPart", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getStoryMultiPart(int content_id) {
        String key = this.getClass().getCanonicalName() + ".getStoryMultiPart." + content_id;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getStoryByCode from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getStoryMultiPart from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "Select a.name_path,a.total_part,b.part_number,b.path,b.duration,b.summary_path,a.summary_path summary_story_path "
                    + "from story_content a, story_content_part b "
                    + "where a.story_content_id = b.story_content_id "
                    + "and a.ivr_publish = 1 "
                    + "and a.story_content_id = ? and a.status = 1 and a.publish_date <= now() and b.publish_date <= now() "
                    + "and b.status = 1 order by b.part_number";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, content_id);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            int i = 0;
            while (re1.next()) {
                if (i == 0) {
                    resp.put("summary_story_path", re1.getString("summary_story_path"));
                }
                i++;
                String totalPart = re1.getString("total_part");
                String part_number = re1.getString("part_number");
                String path = re1.getString("path");
                String duration = re1.getString("duration");
                String summaryPath = re1.getString("summary_path");
                String namePath = re1.getString("name_path");
                if (data.isEmpty()) {
                    resp.put("total_part", totalPart);
                    resp.put("name_path", namePath);
                }
                Content a = new Content();
                a.setPartNumber(part_number);
                a.setContentPath(path);
                a.setDuration(duration);
                a.setSummaryPath(summaryPath);
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                cache.put(new Element(key, resp));
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getStoryMultiPart", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getMusicByCode(int code) {
        String key = this.getClass().getCanonicalName() + ".getMusicByCode." + code;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getMusicByCode from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getMusicByCode from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re2 = null;
        try {
            cnn = getConnection();
            String sql = "Select total_part,music_content_id,content_path,name_path,code_crbt,content_duration,content_name "
                    + "from music_content "
                    + "where code = ? and status = 1 "
                    + "and ivr_publish = 1 "
                    + "and publish_date <= now()";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, code);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                String totalPart = re1.getString("total_part");
                String contentId = re1.getString("music_content_id");
                String namePath = re1.getString("name_path");
                String contentName = re1.getString("content_name");
                resp.put("total_part", totalPart);
                resp.put("content_id", contentId);
                resp.put("name_path", namePath);
                resp.put("content_name", Helper.convertToFriendly(contentName));
                ArrayList<Content> data = new ArrayList<Content>();
                if (Integer.parseInt(totalPart) > 1) {
                    sql = "select part_number,path,duration from music_content_part "
                            + "where music_content_id = ? and status = 1 order by part_number";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, contentId);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    re2 = stmt2.executeQuery();
                    while (re2.next()) {
                        String part_number = re2.getString("part_number");
                        String path = re2.getString("path");
                        String duration = re2.getString("duration");
                        Content a = new Content();
                        a.setPartNumber(part_number);
                        a.setContentPath(path);
                        a.setDuration(duration);
                        data.add(a);
                    }
                } else {
                    String contentPath = re1.getString("content_path");
                    String contentDuration = re1.getString("content_duration");
                    String codeRbt = re1.getString("code_crbt");
                    Content a = new Content();
                    a.setContentPath(contentPath);
                    a.setDuration(contentDuration);
                    a.setCodeRbt(codeRbt);
                    data.add(a);
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getMusicByCode", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re2);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getMusicByCodeForAll(int code) {
        String key = this.getClass().getCanonicalName() + ".getMusicByCodeForAll." + code;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getMusicByCode from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getMusicByCode from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re2 = null;
        try {
            cnn = getConnection();
            String sql = "Select total_part,music_content_id,content_path,name_path,code_crbt,content_duration,content_name "
                    + "from music_content "
                    + "where code = ? and status = 1 "
                    + "and publish_date <= now()";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, code);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                String totalPart = re1.getString("total_part");
                String contentId = re1.getString("music_content_id");
                String namePath = re1.getString("name_path");
                String contentName = re1.getString("content_name");
                resp.put("total_part", totalPart);
                resp.put("content_id", contentId);
                resp.put("name_path", namePath);
                resp.put("content_name", Helper.convertToFriendly(contentName));
                ArrayList<Content> data = new ArrayList<Content>();
                if (Integer.parseInt(totalPart) > 1) {
                    sql = "select part_number,path,duration from music_content_part "
                            + "where music_content_id = ? and status = 1 order by part_number";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, contentId);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    re2 = stmt2.executeQuery();
                    while (re2.next()) {
                        String part_number = re2.getString("part_number");
                        String path = re2.getString("path");
                        String duration = re2.getString("duration");
                        Content a = new Content();
                        a.setPartNumber(part_number);
                        a.setContentPath(path);
                        a.setDuration(duration);
                        data.add(a);
                    }
                } else {
                    String contentPath = re1.getString("content_path");
                    String contentDuration = re1.getString("content_duration");
                    String codeRbt = re1.getString("code_crbt");
                    Content a = new Content();
                    a.setContentPath(contentPath);
                    a.setDuration(contentDuration);
                    a.setCodeRbt(codeRbt);
                    data.add(a);
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getMusicByCode", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re2);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getMusicByCodeOrName(String input) {
        String key = this.getClass().getCanonicalName() + ".getMusicByCodeOrName." + input;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getMusicByCodeOrName from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getMusicByCodeOrName from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re2 = null;
        try {
            cnn = getConnection();
            String sql = "Select music_content_id,content_name,code "
                    + "from music_content "
                    + "where (code = ? or REPLACE(content_name_slug,'-','') = ? or REPLACE(content_name_slug,' ','') = ?) "
                    + "and status = 1 "
                    + "and ivr_publish = 1 "
                    + "and publish_date <= now() order by music_content_id desc limit 0,1";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, input);
            stmt1.setString(2, input);
            stmt1.setString(3, input);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                String contentId = re1.getString("music_content_id");
                String contentName = re1.getString("content_name");
                String code = re1.getString("code");
                resp.put("content_id", contentId);
                resp.put("content_name", Helper.convertToFriendly(contentName));
                resp.put("code", code);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                cache.put(new Element(key, resp));
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getMusicByCodeOrName", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re2);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }
        return resp;
    }

    public ArrayList<Content> getMusicByName(String content_name) {
        ArrayList<Content> resp = new ArrayList<Content>();
        logger.debug("getMusicByName from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "SELECT DISTINCT(CODE), content_name_slug, singer_slug "
                    + "FROM "
                    + "(SELECT CODE,content_name_slug,singer_slug "
                    + "FROM music_content "
                    + "WHERE REPLACE(content_name_slug,' ','') LIKE REPLACE(?,' ','') "
                    + "AND STATUS = 1 AND ivr_publish = 1 AND publish_date <= NOW() "
                    + "ORDER BY content_name_slug LIMIT 0," + ConfigStack.getConfig("api_general", "limit_number_find_music", "5") + ") t1 "
                    + "UNION "
                    + "(SELECT CODE,content_name_slug,singer_slug "
                    + "FROM music_content "
                    + "WHERE REPLACE(content_name_slug,' ','') LIKE REPLACE(?,' ','') "
                    + "AND STATUS = 1 AND ivr_publish = 1 AND publish_date <= NOW() "
                    + "ORDER BY content_name_slug LIMIT 0," + ConfigStack.getConfig("api_general", "limit_number_find_music", "5") + ") "
                    + "LIMIT 0," + ConfigStack.getConfig("api_general", "limit_number_find_music", "5");
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, content_name + "%");
            stmt1.setString(2, "%" + content_name + "%");
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            while (re1.next()) {
                String contentName = re1.getString("content_name_slug");
                String code = re1.getString("code");
                String singer = re1.getString("singer_slug");
                Content a = new Content();
                a.setContentNameSlug(contentName);
                a.setSingerSlug(singer);
                a.setCode(code);
                resp.add(a);
            }
        } catch (Exception e) {
            logger.error("Error in getMusicByName", e);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getStoryByCode(int code) {
        String key = this.getClass().getCanonicalName() + ".getStoryByCode." + code;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getStoryByCode from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getStoryByCode from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re2 = null;
        try {
            cnn = getConnection();
            String sql = "Select story_content_id,total_part,content_path,name_path,summary_path,content_duration "
                    + "from story_content "
                    + "where code = ? and status = 1 "
                    + "and ivr_publish = 1 "
                    + "and publish_date <= now()";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, code);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                String totalPart = re1.getString("total_part");
                String contentPath = re1.getString("content_path");
                String namePath = re1.getString("name_path");
                String contentDuration = re1.getString("content_duration");
                String summaryPath = re1.getString("summary_path");
                String contentId = re1.getString("story_content_id");
                resp.put("total_part", totalPart);
                resp.put("content_path", contentPath);
                resp.put("name_path", namePath);
                resp.put("duration", contentDuration);
                resp.put("summary_path", summaryPath);
                resp.put("content_id", contentId);
                if (Integer.parseInt(totalPart) > 1) {
                    sql = "select part_number,path,duration,summary_path from story_content_part "
                            + "where story_content_id = ? and status = 1 and publish_date <= now() "
                            + "order by part_number";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, contentId);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    re2 = stmt2.executeQuery();
                    ArrayList<Content> data = new ArrayList<Content>();
                    while (re2.next()) {
                        String part_number = re2.getString("part_number");
                        String path = re2.getString("path");
                        String duration = re2.getString("duration");
                        summaryPath = re2.getString("summary_path");
                        Content a = new Content();
                        a.setPartNumber(part_number);
                        a.setContentPath(path);
                        a.setDuration(duration);
                        a.setContentId(contentId);
                        a.setSummaryPath(summaryPath);
                        data.add(a);
                    }
                    if (data.size() > 0) {
                        resp.put("data", data);
                    }
                }
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                cache.put(new Element(key, resp));
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getStoryByCode", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re2);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }
        return resp;
    }

    public ArrayList<Content> getStoryByName(String content_name) {
        ArrayList<Content> resp = new ArrayList<Content>();
        logger.debug("getStoryByName from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "SELECT DISTINCT(CODE), content_name_slug "
                    + "FROM "
                    + "(SELECT CODE,content_name_slug "
                    + "FROM story_content "
                    + "WHERE REPLACE(content_name_slug,' ','') LIKE REPLACE(?,' ','') "
                    + "AND STATUS = 1 AND ivr_publish = 1 AND publish_date <= NOW() "
                    + "ORDER BY content_name_slug LIMIT 0," + ConfigStack.getConfig("api_general", "limit_number_find_story", "5") + ") t1 "
                    + "UNION "
                    + "(SELECT CODE,content_name_slug "
                    + "FROM story_content "
                    + "WHERE REPLACE(content_name_slug,' ','') LIKE REPLACE(?,' ','') "
                    + "AND STATUS = 1 AND ivr_publish = 1 AND publish_date <= NOW() "
                    + "ORDER BY content_name_slug LIMIT 0," + ConfigStack.getConfig("api_general", "limit_number_find_story", "5") + ") "
                    + "LIMIT 0," + ConfigStack.getConfig("api_general", "limit_number_find_story", "5");
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, content_name + "%");
            stmt1.setString(2, "%" + content_name + "%");
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            while (re1.next()) {
                String contentName = re1.getString("content_name_slug");
                String code = re1.getString("code");
                Content a = new Content();
                a.setContentNameSlug(contentName);
                a.setCode(code);
                resp.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getStoryByName", e.getCause());
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> addMusicIntoAlbum(String msisdn, String contentId, String partNumber) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re2 = null;
        PreparedStatement stmt3 = null;
        try {
            cnn = getConnection();
            String sql = "select status from music_album where msisdn = ? and music_content_id = ? "
                    + "and part_number = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, contentId);
            stmt1.setString(3, partNumber);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int status = re1.getInt("status");
                if (status == 1) {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                } else {
                    sql = "update music_album set status = 1 where msisdn = ? and music_content_id = ? "
                            + "and part_number = ?";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, msisdn);
                    stmt2.setString(2, contentId);
                    stmt2.setString(3, partNumber);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    stmt2.execute();
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                }
            } else {
                sql = "insert into music_album(msisdn,music_content_id,part_number,status) values(?,?,?,1)";
                stmt2 = cnn.getConnection().prepareStatement(sql);
                stmt2.setString(1, msisdn);
                stmt2.setString(2, contentId);
                stmt2.setString(3, partNumber);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt2.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                stmt2.execute();
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            }
            cnn.getConnection().commit();
            String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
            if (errorCode.equals(Constants.SUCCESS)) {
                sql = "select content_name_slug,code from music_content "
                        + "where music_content_id = ? and ivr_publish = 1";
                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setString(1, contentId);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re2 = stmt3.executeQuery();
                if (re2.next()) {
                    resp.put("content_name", re2.getString("content_name_slug"));
                    resp.put("code", re2.getString("code"));
                }
            }
        } catch (Exception e) {
            logger.error("Error in addMusicIntoAlbum", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re2);
            closeStatement(stmt1);
            closeStatement(stmt2);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> addFunIntoAlbum(String msisdn, String contentId, String partNumber) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re2 = null;
        PreparedStatement stmt3 = null;
        try {
            cnn = getConnection();
            String sql = "select status from fun_album where msisdn = ? and fun_content_id = ? "
                    + "and part_number = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, contentId);
            stmt1.setString(3, partNumber);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int status = re1.getInt("status");
                if (status == 1) {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                } else {
                    sql = "update fun_album set status = 1 where msisdn = ? and fun_content_id = ? "
                            + "and part_number = ?";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, msisdn);
                    stmt2.setString(2, contentId);
                    stmt2.setString(3, partNumber);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    stmt2.execute();
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                }
            } else {
                sql = "insert into fun_album(msisdn,fun_content_id,part_number,status) values(?,?,?,1)";
                stmt2 = cnn.getConnection().prepareStatement(sql);
                stmt2.setString(1, msisdn);
                stmt2.setString(2, contentId);
                stmt2.setString(3, partNumber);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt2.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                stmt2.execute();
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            }
            cnn.getConnection().commit();
            String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
            if (errorCode.equals(Constants.SUCCESS)) {
                sql = "select content_name_slug from fun_content "
                        + "where fun_content_id = ? and ivr_publish = 1";
                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setString(1, contentId);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re2 = stmt3.executeQuery();
                if (re2.next()) {
                    resp.put("content_name", re2.getString("content_name_slug"));
                }
            }
        } catch (Exception e) {
            logger.error("Error in addFunIntoAlbum", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re2);
            closeStatement(stmt1);
            closeStatement(stmt2);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> addStoryIntoAlbum(String msisdn, String contentId, String contentType) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re2 = null;
        PreparedStatement stmt3 = null;
        try {
            cnn = getConnection();
            String sql = "select status from story_album where msisdn = ? and story_content_id = ? "
                    + "and content_type = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, contentId);
            stmt1.setString(3, contentType);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int status = re1.getInt("status");
                if (status == 1) {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                } else {
                    sql = "update story_album set status = 1 where msisdn = ? and story_content_id = ? "
                            + "and content_type = ?";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, msisdn);
                    stmt2.setString(2, contentId);
                    stmt2.setString(3, contentType);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    stmt2.execute();
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                }
            } else {
                sql = "insert into story_album(msisdn,story_content_id,content_type,status) values(?,?,?,1)";
                stmt2 = cnn.getConnection().prepareStatement(sql);
                stmt2.setString(1, msisdn);
                stmt2.setString(2, contentId);
                stmt2.setString(3, contentType);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt2.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                stmt2.execute();
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            }
            cnn.getConnection().commit();
            String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
            if (errorCode.equals(Constants.SUCCESS)) {
                sql = "select content_name_slug,code from story_content "
                        + "where story_content_id = ? and ivr_publish = 1";
                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setString(1, contentId);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re2 = stmt3.executeQuery();
                if (re2.next()) {
                    resp.put("content_name", re2.getString("content_name_slug"));
                    resp.put("code", re2.getString("code"));
                }
            }
        } catch (Exception e) {
            logger.error("Error in addStoryIntoAlbum", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(re1);
            closeResultSet(re2);
            closeStatement(stmt1);
            closeStatement(stmt2);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> removeMusicFromAlbum(String msisdn, String contentId, String partNumber) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        try {
            cnn = getConnection();
            String sql = "update music_album set status = -1 where msisdn = ? and music_content_id = ? "
                    + "and part_number = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, contentId);
            stmt1.setString(3, partNumber);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt1.execute();
            resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in removeMusicFromAlbum", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> removeFunFromAlbum(String msisdn, String contentId, String partNumber) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        try {
            cnn = getConnection();
            String sql = "update fun_album set status = -1 where msisdn = ? and fun_content_id = ? "
                    + "and part_number = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, contentId);
            stmt1.setString(3, partNumber);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt1.execute();
            resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in removeFunFromAlbum", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> removeStoryFromAlbum(String msisdn, String contentId) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        try {
            cnn = getConnection();
            String sql = "update story_album set status = -1 where msisdn = ? and story_content_id = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, contentId);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt1.execute();
            resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in removeStoryFromAlbum", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> checkStoryContentTypeFromAlbum(String msisdn) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "select distinct(content_type) "
                    + "from story_album where msisdn = ? and status = 1 order by content_type";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            int p1 = 0;
            int p2 = 0;
            while (re1.next()) {
                int a = re1.getInt("content_type");
                if (a == 0) {
                    p1++;
                }
                if (a == 1) {
                    p2++;
                }
            }
            if (p1 > 0 && p2 == 0) {
                resp.put("content_type", 0);
            } else if (p1 == 0 && p2 > 0) {
                resp.put("content_type", 1);
            } else if (p1 > 0 && p2 > 0) {
                resp.put("content_type", 2);
            } else {
                resp.put("content_type", -1);
            }
            resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
        } catch (Exception e) {
            logger.error("Error in checkStoryContentTypeFromAlbum", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListChannel(int topic_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getListChannel." + topic_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getListChannel from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        logger.debug("getListChannel from database");
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet re = null;
        try {
            cnn = getConnection();
            String sql = "Select a.topic_id,b.channel_id, b.channel_ord, b.name_path "
                    + "from content_topic a, content_channel b "
                    + "where a.topic_id = b.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 "
                    + "and b.status = 1 "
                    + "and a.status = 1 "
                    + "and a.topic_ord = ? "
                    + "and a.topic_type = ? "
                    + "order by b.channel_ord";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, topic_ord);
            stmt.setInt(2, topic_type);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re = stmt.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            while (re.next()) {
                Content a = new Content();
                String topicId = re.getString("topic_id");
                String channelId = re.getString("channel_id");
                String channelOrd = re.getString("channel_ord");
                String namePath = re.getString("name_path");
                a.setTopicId(topicId);
                a.setChannelId(channelId);
                a.setChannelOrd(channelOrd);
                a.setNamePath(namePath);
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                cache.put(new Element(key, resp));
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListChannel", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListMusicFromAlbum(String msisdn) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "SELECT * FROM ("
                    + "SELECT * FROM ("
                    + "SELECT a.music_content_id,a.part_number,b.content_path,b.name_path,b.content_duration, a.updated_date,b.code "
                    + "FROM music_album a,music_content b "
                    + "WHERE a.music_content_id = b.music_content_id "
                    + "and b.ivr_publish = 1 "
                    + "AND a.msisdn = ? AND a.status = 1 "
                    + "AND b.status = 1 AND a.part_number = 0) t1 "
                    + "UNION ("
                    + "SELECT b.music_content_id,b.part_number,b.path content_path,c.name_path,b.duration content_duration, a.updated_date,c.code "
                    + "FROM music_album a, music_content_part b ,music_content c "
                    + "WHERE a.music_content_id = b.music_content_id "
                    + "AND b.music_content_id = c.music_content_id "
                    + "and c.ivr_publish = 1 "
                    + "AND a.part_number = b.part_number "
                    + "AND a.msisdn = ? "
                    + "AND a.status = 1 "
                    + "AND b.status = 1 AND a.part_number > 0 )"
                    + ") t3 ORDER BY t3.updated_date DESC";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            while (re1.next()) {
                Content a = new Content();
                String music_content_id = re1.getString("music_content_id");
                String code = re1.getString("code");
                int part_number = re1.getInt("part_number");
                String contentPath = re1.getString("content_path");
                String namePath = re1.getString("name_path");
                String contentDuration = re1.getString("content_duration");
                a.setContentPath(contentPath);
                a.setNamePath(namePath);
                a.setDuration(contentDuration);
                a.setContentId(music_content_id);
                a.setPartNumber(String.valueOf(part_number));
                a.setCode(code);
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListMusicFromAlbum", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListFunFromAlbum(String msisdn) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "SELECT * FROM ("
                    + "SELECT * FROM ("
                    + "SELECT a.fun_content_id,a.part_number,b.content_path,b.name_path,b.content_duration, a.updated_date "
                    + "FROM fun_album a,fun_content b "
                    + "WHERE a.fun_content_id = b.fun_content_id "
                    + "and b.ivr_publish = 1 "
                    + "AND a.msisdn = ? AND a.status = 1 "
                    + "AND b.status = 1 AND a.part_number = 0) t1 "
                    + "UNION ("
                    + "SELECT b.fun_content_id,b.part_number,b.path content_path,c.name_path,b.duration content_duration, a.updated_date "
                    + "FROM fun_album a, fun_content_part b ,fun_content c "
                    + "WHERE a.fun_content_id = b.fun_content_id "
                    + "AND b.fun_content_id = c.fun_content_id "
                    + "and c.ivr_publish = 1 "
                    + "AND a.part_number = b.part_number "
                    + "AND a.msisdn = ? "
                    + "AND a.status = 1 "
                    + "AND b.status = 1 AND a.part_number > 0 )"
                    + ") t3 ORDER BY t3.updated_date DESC";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            while (re1.next()) {
                Content a = new Content();
                int part_number = re1.getInt("part_number");
                String music_content_id = re1.getString("fun_content_id");
                String contentPath = re1.getString("content_path");
                String namePath = re1.getString("name_path");
                String contentDuration = re1.getString("content_duration");
                a.setContentPath(contentPath);
                a.setNamePath(namePath);
                a.setDuration(contentDuration);
                a.setContentId(music_content_id);
                a.setPartNumber(String.valueOf(part_number));
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListFunFromAlbum", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListStoryFromAlbum(String msisdn, String contentType) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            ArrayList<Content> data = new ArrayList<Content>();
            if (contentType.equals("0")) {
                String sql = "Select a.story_content_id,b.content_path,b.name_path,b.content_duration,b.summary_path "
                        + "from story_album a,story_content b "
                        + "where a.story_content_id = b.story_content_id "
                        + "and b.ivr_publish = 1 "
                        + "and a.msisdn = ? and a.content_type =  ? "
                        + "and a.status = 1 and b.status = 1 and b.publish_date <= now()";
                stmt1 = cnn.getConnection().prepareStatement(sql);
                stmt1.setString(1, msisdn);
                stmt1.setString(2, contentType);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt1.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re1 = stmt1.executeQuery();
                while (re1.next()) {
                    Content a = new Content();
                    String contentPath = re1.getString("content_path");
                    String namePath = re1.getString("name_path");
                    String contentDuration = re1.getString("content_duration");
                    String summaryPath = re1.getString("summary_path");
                    String contentId = re1.getString("story_content_id");
                    a.setContentPath(contentPath);
                    a.setNamePath(namePath);
                    a.setDuration(contentDuration);
                    a.setContentId(contentId);
                    a.setSummaryPath(summaryPath);
                    data.add(a);
                }
            } else {
                String sql = "Select a.story_content_id,b.name_path,b.total_part "
                        + "from story_album a,story_content b "
                        + "where a.story_content_id = b.story_content_id "
                        + "and b.ivr_publish = 1 "
                        + "and a.msisdn = ? and a.content_type =  ? "
                        + "and a.status = 1 and b.status = 1 and b.publish_date <= now()";
                stmt1 = cnn.getConnection().prepareStatement(sql);
                stmt1.setString(1, msisdn);
                stmt1.setString(2, contentType);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt1.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re1 = stmt1.executeQuery();
                while (re1.next()) {
                    Content a = new Content();
                    String totalPart = re1.getString("total_part");
                    String namePath = re1.getString("name_path");
                    String contentId = re1.getString("story_content_id");
                    a.setNamePath(namePath);
                    a.setContentId(contentId);
                    a.setTotalPart(totalPart);
                    data.add(a);
                }
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListStoryFromAlbum", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public ListenHistory getListenHistory(String msisdn, int channelId, int channelType) {
        String key = this.getClass().getCanonicalName() + ".getListenHistory." + msisdn + "." + channelId + "." + channelType;
        String data = RestfulStack.getFromCache(key);
        if (data != null) {
            logger.debug("getListenHistory from cache");
            return gson.fromJson(data, ListenHistory.class);
        }
        logger.debug("getListenHistory from Database");

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select content_list from access_history where msisdn = ? "
                + "and channel_id = ? and topic_type = ?";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            stmt.setInt(2, channelId);
            stmt.setInt(3, channelType);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                ListenHistory listenHistory = new ListenHistory();
                listenHistory.setMsisdn(msisdn);
                listenHistory.setChannelId(channelId);
                listenHistory.setContentListened(rs.getString("content_list"));
                logger.info("content listened :"+ rs.getString("content_list") + " and msisdn :" + msisdn);
                RestfulStack.pushToCacheWithExpiredTime(key,
                        gson.toJson(listenHistory), Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
                return listenHistory;
            }
        } catch (Exception e) {
            logger.error("error in getListenHistory in database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return null;
    }

    public ListenHistory getSMSRemiderHistory(String msisdn) {
        String key = this.getClass().getCanonicalName() + ".getSMSRemiderHistory." + msisdn;
        String data = RestfulStack.getFromCache(key);
        if (data != null) {
            logger.debug("getSMSRemiderHistory from cache");
            return gson.fromJson(data, ListenHistory.class);
        }
        logger.debug("getSMSRemiderHistory from Database");

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select music_mt_in_day,story_mt_in_day,music_contents,story_contents,status,music_sent_date,story_sent_date,lastest_sent_date "
                + "from sms_reminder where msisdn = ?";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                ListenHistory listenHistory = new ListenHistory();
                listenHistory.setMsisdn(msisdn);
                listenHistory.setStatus(rs.getInt("status"));
                listenHistory.setMusicListened(rs.getString("music_contents"));
                listenHistory.setStoryListened(rs.getString("story_contents"));
                listenHistory.setLastestSentDate(rs.getString("lastest_sent_date"));
                listenHistory.setMusicSentDate(rs.getString("music_sent_date"));
                listenHistory.setStorySentDate(rs.getString("story_sent_date"));
                listenHistory.setMusicMtInDay(rs.getInt("music_mt_in_day"));
                listenHistory.setStoryMtInDay(rs.getInt("story_mt_in_day"));
                RestfulStack.pushToCacheWithExpiredTime(key,
                        gson.toJson(listenHistory), Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
                return listenHistory;
            }
        } catch (Exception e) {
            logger.error("error in getSMSRemiderHistory in database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return null;
    }

    public int insertListenHistory(ListenHistory listenHis, int channel_type) {
        logger.info("LISTENT HISTORY CONTENT LISTENNED :" + listenHis.getContentListened());
        int result = -1;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "insert into access_history(msisdn,channel_id,topic_type,content_list) values(?,?,?,?)";
        /*
         * Limit length
         */
        while (listenHis.getContentListened() != null
                && listenHis.getContentListened().length() >= Integer.parseInt(ConfigStack.getConfig("api_general", "listen_content_max_length", "1000"))) {
            int lastIndexOfSep = listenHis.getContentListened().lastIndexOf(
                    ListenHistory.SEPERATOR,
                    listenHis.getContentListened().length() - 2);
            listenHis.setContentListened(listenHis.getContentListened().substring(0, lastIndexOfSep + 1));
        }
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, listenHis.getMsisdn());
            stmt.setInt(2, listenHis.getChannelId());
            stmt.setInt(3, channel_type);
            stmt.setString(4, listenHis.getContentListened());
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.executeUpdate();
            result = 0;
            cnn.getConnection().commit();
            updateCacheListenHistory(listenHis, channel_type);
        } catch (Exception e) {
            logger.error("error in insertListenHistory in database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return result;
    }

    public int insertSMSRemiderHistory(ListenHistory listenHis) {
        int result = -1;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "insert into sms_reminder(msisdn,music_mt_in_day,story_mt_in_day,music_contents,story_contents,status,music_sent_date,story_sent_date,lastest_sent_date) "
                + "values(?,?,?,?,?,1,?,?,?)";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, listenHis.getMsisdn());
            stmt.setInt(2, listenHis.getMusicMtInDay());
            stmt.setInt(3, listenHis.getStoryMtInDay());
            stmt.setString(4, listenHis.getMusicListened());
            stmt.setString(5, listenHis.getStoryListened());
            stmt.setString(6, listenHis.getMusicSentDate());
            stmt.setString(7, listenHis.getStorySentDate());
            stmt.setString(8, listenHis.getLastestSentDate());
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.executeUpdate();
            result = 0;
            cnn.getConnection().commit();
            updateCacheSMSReminderHistory(listenHis);
        } catch (Exception e) {
            logger.error("error in insertSMSRemiderHistory in database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return result;
    }

    private void updateCacheListenHistory(ListenHistory listenHis,
            int channelType) {
        String key = this.getClass().getCanonicalName() + ".getListenHistory." + listenHis.getMsisdn() + "."
                + listenHis.getChannelId() + "." + channelType;
        RestfulStack.pushToCacheWithExpiredTime(key, gson.toJson(listenHis),
                Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
    }

    private void updateCacheSMSReminderHistory(ListenHistory listenHis) {
        String key = this.getClass().getCanonicalName() + ".getSMSRemiderHistory." + listenHis.getMsisdn();
        RestfulStack.pushToCacheWithExpiredTime(key, gson.toJson(listenHis),
                Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "0")));
    }

    public int updateListenHistory(ListenHistory listenHis, int channel_type) {
        int result = -1;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "update access_history set content_list = ? "
                + "where msisdn = ? and channel_id = ? "
                + "and topic_type = ?";
        /*
         * Limit length
         */
        while (listenHis.getContentListened() != null
                && listenHis.getContentListened().length() >= Integer.parseInt(ConfigStack.getConfig("api_general", "listen_content_max_length", "1000"))) {
            int lastIndexOfSep = listenHis.getContentListened().lastIndexOf(
                    ListenHistory.SEPERATOR,
                    listenHis.getContentListened().length() - 2);
            listenHis.setContentListened(listenHis.getContentListened().substring(0, lastIndexOfSep + 1));
        }

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, listenHis.getContentListened());
            stmt.setString(2, listenHis.getMsisdn());
            stmt.setInt(3, listenHis.getChannelId());
            stmt.setInt(4, channel_type);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.executeUpdate();
            result = 0;
            cnn.getConnection().commit();
            updateCacheListenHistory(listenHis, channel_type);
        } catch (Exception e) {
            logger.error("error in updateListenHistory in database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return result;
    }

    public int updateSMSReminderHistory(ListenHistory listenHis) {
        int result = -1;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "update sms_reminder set music_contents = ?, story_contents = ?, music_mt_in_day = ?,"
                + "story_mt_in_day = ?, lastest_sent_date = ?, music_sent_date = ?, story_sent_date = ? "
                + "where msisdn = ?";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, listenHis.getMusicListened());
            stmt.setString(2, listenHis.getStoryListened());
            stmt.setInt(3, listenHis.getMusicMtInDay());
            stmt.setInt(4, listenHis.getStoryMtInDay());
            stmt.setString(5, listenHis.getLastestSentDate());
            stmt.setString(6, listenHis.getMusicSentDate());
            stmt.setString(7, listenHis.getStorySentDate());
            stmt.setString(8, listenHis.getMsisdn());
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.executeUpdate();
            result = 0;
            cnn.getConnection().commit();
            updateCacheSMSReminderHistory(listenHis);
        } catch (Exception e) {
            logger.error("error in updateSMSReminderHistory in database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return result;
    }

    public ArrayList<PromtInTimeRange> getListPromtInTimeRange() {
        ArrayList<PromtInTimeRange> promts = new ArrayList<PromtInTimeRange>();
        String key = this.getClass().getCanonicalName() + ".getListPromtInTimeRange";
        Element data = cache.get(key);
        if (data != null) {
            logger.debug("getListPromtInTimeRange from cache");
            return (ArrayList<PromtInTimeRange>) data.getObjectValue();
        }

        synchronized (this) {
            data = cache.get(key);
            if (data != null) {
                logger.debug("getListPromtInTimeRange from cache");
                return (ArrayList<PromtInTimeRange>) data.getObjectValue();
            }

            logger.debug("getListPromtInTimeRange from DB");
            PreparedStatement stmt = null;
            DbConnection cnn = null;
            ResultSet re = null;
            String sql = "select config_name, val from configuration where upper(group_name) = 'PROMT' and status = 1";

            try {
                cnn = getConnection();
                stmt = cnn.getConnection().prepareStatement(sql);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re = stmt.executeQuery();

                while (re.next()) {
                    String timeRange = re.getString("config_name");
                    String val = re.getString("val");
                    int hourFrom = -1;
                    int minuteFrom = -1;
                    int hourTo = -1;
                    int minuteTo = -1;

                    try {
                        String[] part = timeRange.split("-");
                        String[] part2 = part[0].split(":");
                        hourFrom = Integer.parseInt(part2[0]);
                        minuteFrom = Integer.parseInt(part2[1]);

                        part2 = part[1].split(":");
                        hourTo = Integer.parseInt(part2[0]);
                        minuteTo = Integer.parseInt(part2[1]);
                    } catch (Exception e) {
                        logger.error(e);
                    }

                    if (hourFrom >= 0 && minuteFrom >= 0 && hourTo >= 0
                            && minuteTo >= 0) {
                        PromtInTimeRange p = new PromtInTimeRange();
                        p.setHourFrom(hourFrom);
                        p.setHourTo(hourTo);
                        p.setMinuteFrom(minuteFrom);
                        p.setMinuteTo(minuteTo);

                        String[] part = val.split(",");
                        ArrayList<Integer> listPromtNo = new ArrayList<Integer>();
                        for (int i = 0; i < part.length; i++) {
                            listPromtNo.add(Integer.valueOf(part[i]));
                        }
                        p.setListPromtNo(listPromtNo);

                        logger.debug("hourFrom: " + p.getHourFrom());
                        logger.debug("minuteFrom: " + p.getMinuteFrom());
                        logger.debug("hourTo: " + p.getHourTo());
                        logger.debug("minuteTo: " + p.getMinuteTo());
                        logger.debug("listPromtNo: " + listPromtNo);

                        promts.add(p);
                    }
                }
                if (promts.size() > 0) {
                    cache.put(new Element(key, promts));
                }
            } catch (Exception e) {
                logger.error("error in getListPromtInTimeRange in database", e);
            } finally {
                closeResultSet(re);
                closeStatement(stmt);
                freeConnection(cnn);
            }
        }
        logger.debug("get from Database");
        return promts;
    }
    // Config
    private Hashtable<String, String> listConfigs = new Hashtable<String, String>();

    public void refreshConfig() {
        synchronized (lock) {
            String key = "config_api";
            cache.remove(key);
            listConfigs.clear();
            getConfig();
        }
    }

    public Hashtable<String, String> getConfig() {
        String key = "config_api";
        Element data = cache.get(key);
        if (data != null) {
            logger.debug("get config from cache");
            return (Hashtable<String, String>) data.getObjectValue();
        }
        logger.debug("get config from Database");
        synchronized (lock) {
            listConfigs.clear();
            if (listConfigs.isEmpty()) {
                DbConnection cnn = null;
                String sql = "select lower(group_name) group_name, lower(config_name) config_name, val from configuration where status = 1";
                PreparedStatement stmt = null;
                ResultSet rs = null;
                try {
                    cnn = getConnection();
                    stmt = cnn.getConnection().prepareStatement(sql);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    rs = stmt.executeQuery();
                    while (rs.next()) {
                        String group_name = rs.getString("group_name");
                        String name = rs.getString("config_name");
                        String val = rs.getString("val");
                        listConfigs.put(group_name + "." + name, val);
                    }
                    if (!listConfigs.isEmpty()) {
                        cache.put(new Element(key, listConfigs));
                    }
                } catch (Exception e) {
                    logger.error("Error in getConfig", e);
                } finally {
                    closeResultSet(rs);
                    closeStatement(stmt);
                    freeConnection(cnn);
                }
            }
        }
        return listConfigs;
    }

    public int insertSMS(SMS sms) {
        DbConnection cnn = null;
        int resp = Integer.parseInt(Constants.SUCCESS);
        sms.setServiceNumber(ConfigStack.getConfig("api_general", "service_number", ""));
        String sql = "insert into sms(msisdn,mt_content,sent_at,sms_type,source,package_id,mo_content,received_at) "
                + "values (?,?,?,?,?,?,?,?)";
        if (!sms.isHaveMO()) {
            sql = "insert into sms(msisdn,mt_content,sent_at,sms_type,source,package_id) "
                    + "values (?,?,?,?,?,?)";
        }
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (Integer.parseInt(ConfigStack.getConfig("api_log", "sms", "1")) == 1) {
                cnn = getConnection();
                stmt = cnn.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, sms.getMsisdn());
                stmt.setString(2, sms.getMtContent());
                Calendar cal = Calendar.getInstance();
                stmt.setTimestamp(3, new java.sql.Timestamp(cal.getTimeInMillis()));
                stmt.setInt(4, sms.getType());
                stmt.setString(5, sms.getSource());
                stmt.setInt(6, sms.getPackageId());
                if (sms.isHaveMO()) {
                    stmt.setString(7, sms.getMoContent());
                    stmt.setTimestamp(8, sms.getMoReceivedTime());
                }
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                stmt.execute();
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    resp = rs.getInt(1);
                }
                if (resp > 0) {
                    cnn.getConnection().commit();
                } else {
                    rollbackTransaction(cnn);
                }
            }
        } catch (Exception ex) {
            logger.error("error in insertSMS in database", ex);
            rollbackTransaction(cnn);
            resp = Integer.parseInt(Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return resp;
    }

    public int insertSubBlackList(String msisdn) {
        DbConnection cnn = null;
        int resp = Integer.parseInt(Constants.SUCCESS);
        String sql;
        sql = "insert into sub_blacklist(msisdn,status,created_by,updated_by) values (?,1,1,1) ON DUPLICATE KEY UPDATE status=1";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.execute();
            cnn.getConnection().commit();
        } catch (Exception ex) {
            logger.error("error in insertSubBlackList in database", ex);
            rollbackTransaction(cnn);
            resp = Integer.parseInt(Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return resp;
    }

    public SubPackageInfo checkChargingLock(String msisdn) {
        SubPackageInfo subInfo = new SubPackageInfo();
        subInfo.setErrorCode(BillingErrorCode.Success);
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql_count = "select count(*) as cnt from charge_locking where msisdn = ? and charge_type = ? and TIMESTAMPDIFF(MINUTE, created_at, NOW()) < ?";
        try {
            cnn = getConnection();

            stmt = cnn.getConnection().prepareStatement(sql_count);
            stmt.setString(1, msisdn);
            stmt.setInt(2, Constants.CHARGE_TYPE_SUB);
            stmt.setInt(3, Integer.parseInt(ConfigStack.getConfig("api_billing", "lock_charging_time", "0")));
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("cnt") > 0) {
                subInfo.setErrorCode(BillingErrorCode.ChargingSubProcessing);
            }
        } catch (Exception e) {
            logger.error("Exception in checkChargingLock for " + msisdn, e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return subInfo;
    }

    public ArrayList<HoroscopeInDateRange> getListHoroScopeInDateRange() {
        ArrayList<HoroscopeInDateRange> list = new ArrayList<HoroscopeInDateRange>();
        String key = this.getClass().getCanonicalName() + ".getListHoroScopeInDateRange";
        Element data = cache.get(key);
        if (data != null) {
            logger.debug("getListHoroScopeInDateRange from cache");
            list = (ArrayList<HoroscopeInDateRange>) data.getObjectValue();
            if (list != null && list.size() > 0) {
                return list;
            }
        }
        synchronized (this) {
            ArrayList<String[]> a = ConfigStack.getConfig("HOROSCOPE");
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM");
            for (int i = 0; i < a.size(); i++) {
                String dateRange = a.get(i)[0];
                String val = a.get(i)[1];
                logger.info(dateRange + " --------- " + val);
                Calendar fromDate = null;
                Calendar toDate = null;
                try {
                    String[] part = dateRange.split(":");

                    fromDate = Calendar.getInstance();
                    fromDate.setTime(sdf.parse(part[0]));

                    toDate = Calendar.getInstance();
                    toDate.setTime(sdf.parse(part[1]));
                } catch (Exception e) {
                    logger.error(e);
                }
                if (fromDate != null && toDate != null) {
                    HoroscopeInDateRange h = new HoroscopeInDateRange();
                    h.setFromDate(fromDate);
                    h.setToDate(toDate);
                    h.setType(Helper.getInt(val, 0));
                    list.add(h);
                }
            }
            if (list.size() > 0) {
                cache.put(new Element(key, list));
            }
        }
        return list;
    }

    public HashMap<String, Object> getHoroByDate(int channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getHoroByDate." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getHoroByDate from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getHoroByDate from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re2 = null;
        try {
            cnn = getConnection();
            String sql = "select a.channel_id,a.channel_ord,b.name_path,b.content_path,b.horo_content_id "
                    + "from content_channel a, horo_content b , content_topic c "
                    + "where a.channel_id = b.channel_id "
                    + "and a.topic_id = c.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 and c.ivr_publish = 1 "
                    + "and a.channel_ord = ? "
                    + "and c.topic_type = ? "
                    + "and b.status = 1 "
                    + "and b.publish_date <= now() "
                    + "order by content_ord desc, publish_date desc";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, channel_ord);
            stmt1.setInt(2, topic_type);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                String channelId = re1.getString("channel_id");
                String channelOrd = re1.getString("channel_ord");
                String namePath = re1.getString("name_path");
                String contentPath = re1.getString("content_path");
                String conetentId = re1.getString("horo_content_id");
                resp.put("channel_id", channelId);
                resp.put("channel_ord", channelOrd);
                resp.put("content_path", contentPath);
                resp.put("content_id", conetentId);
                if (namePath != null && namePath.length() > 0 && !namePath.equals("")) {
                    resp.put("name_path", namePath);
                } else {
                    sql = "select name_path from horo_content "
                            + "where channel_id = ? "
                            + "and status = 1 "
                            + "and name_path IS NOT NULL "
                            + "order by publish_date desc limit 0,1";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, channelId);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    re2 = stmt2.executeQuery();
                    if (re2.next()) {
                        namePath = re2.getString("name_path");
                        resp.put("name_path", namePath);
                    }
                }
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                cache.put(new Element(key, resp));
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getHoroByDate", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re2);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getCalendarByPhongThuy(String name, String channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getCalendarByDate." + name + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getCalendarByDate from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getCalendarByDate from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "select a.channel_id,a.channel_ord,b.name_path,b.content_path,b.horo_content_id "
                    + "from content_channel a, horo_content b, content_topic c "
                    + "where a.channel_id = b.channel_id "
                    + "and a.topic_id = c.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 and c.ivr_publish = 1 "
                    + "and a.channel_ord = ? "
                    + "and c.topic_type = ? "
                    + "and SUBSTRING_INDEX(b.content_path,'/',-1)  like ? "
                    + "and b.status = 1 "
                    + "and b.publish_date <= now() "
                    + "order by content_ord desc, publish_date desc";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, channel_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setString(3, "%" + name + "%");
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            while (re1.next()) {
                Content a = new Content();
                a.setChannelId(re1.getString("channel_id"));
                a.setChannelOrd(re1.getString("channel_ord"));
                a.setNamePath(re1.getString("name_path"));
                a.setContentPath(re1.getString("content_path"));
                a.setContentId(re1.getString("horo_content_id"));
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getCalendarByDate", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getCalendarByDate(String name, String channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getCalendarByDate." + name + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getCalendarByDate from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getCalendarByDate from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "select a.channel_id,a.channel_ord,b.name_path,b.content_path,b.horo_content_id "
                    + "from content_channel a, horo_content b, content_topic c "
                    + "where a.channel_id = b.channel_id "
                    + "and a.topic_id = c.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 and c.ivr_publish = 1 "
                    + "and a.channel_ord = ? "
                    + "and c.topic_type = ? "
                    + "and SUBSTRING_INDEX(b.content_path,'/',-1)  like ? "
                    + "and b.status = 1 "
                    + "and b.publish_date <= now() "
                    + "order by content_ord desc, publish_date desc";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, channel_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setString(3, "%" + name + "%");
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                String channelId = re1.getString("channel_id");
                String channelOrd = re1.getString("channel_ord");
                String namePath = re1.getString("name_path");
                String contentPath = re1.getString("content_path");
                String conetentId = re1.getString("horo_content_id");
                resp.put("channel_id", channelId);
                resp.put("channel_ord", channelOrd);
                resp.put("content_path", contentPath);
                resp.put("content_id", conetentId);
                resp.put("name_path", namePath);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                cache.put(new Element(key, resp));
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getCalendarByDate", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public void insertLastInteractive(String msisdn) {
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        try {
            cnn = getConnection();
            String sql = "INSERT INTO last_interactive (msisdn,last_interact_time) VALUES (?,now()) ON DUPLICATE KEY UPDATE last_interact_time=now()";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt1.execute();
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in insertLastInteractive", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }
    }

    public void insertRegisterSMS(String msisdn) {
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        try {
            cnn = getConnection();
            String sql = "INSERT INTO sms_reminder(msisdn,status) VALUES (?,1) ON DUPLICATE KEY UPDATE status = 1";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt1.execute();
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in insertRegisterSMS", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }
    }

    public void removeRegisterSMS(String msisdn) {
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        try {
            cnn = getConnection();
            String sql = "update sms_reminder set status = 0 where msisdn = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt1.execute();
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in removeRegisterSMS", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }
    }

    public void insertRegisterGift(String msisdn, String sender) {
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re = null;
        try {
            cnn = getConnection();
            String sql = "select * from gift_account where msisdn = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re = stmt1.executeQuery();
            if (re.next()) {
                int status = re.getInt("status");
                String a = re.getString("white_list");
                String b = re.getString("black_list");
                if (status == 1) {
                    if (sender.length() > 0) {
                        b = b.replaceAll("-" + sender, "");
                        sql = "update gift_account set black_list = ?, updated_date=now() where msisdn = ?";
                        stmt2 = cnn.getConnection().prepareStatement(sql);
                        stmt2.setString(1, b);
                        stmt2.setString(2, msisdn);
                        if (logger.isDebugEnabled()) {
                            String sql_log = stmt2.toString();
                            sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                            logger.debug(sql_log);
                        }
                        stmt2.execute();
                        cnn.getConnection().commit();
                    } else {
                        sql = "update gift_account set black_list = ?, white_list = ?, updated_date=now() where msisdn = ?";
                        stmt2 = cnn.getConnection().prepareStatement(sql);
                        stmt2.setString(1, "");
                        stmt2.setString(2, "");
                        stmt2.setString(3, msisdn);
                        if (logger.isDebugEnabled()) {
                            String sql_log = stmt2.toString();
                            sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                            logger.debug(sql_log);
                        }
                        stmt2.execute();
                        cnn.getConnection().commit();
                    }
                } else {
                    if (sender.length() > 0) {
                        a = a.replaceAll("-" + sender, "");
                        b = b.replaceAll("-" + sender, "");
                        a = a + "-" + sender;
                        sql = "update gift_account set white_list = ?, black_list = ?, updated_date=now() where msisdn = ?";
                        stmt2 = cnn.getConnection().prepareStatement(sql);
                        stmt2.setString(1, a);
                        stmt2.setString(2, b);
                        stmt2.setString(3, msisdn);
                        if (logger.isDebugEnabled()) {
                            String sql_log = stmt2.toString();
                            sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                            logger.debug(sql_log);
                        }
                        stmt2.execute();
                        cnn.getConnection().commit();
                    } else {
                        sql = "update gift_account set status = 1, updated_date=now(),black_list = ?,white_list = ? where msisdn = ?";
                        stmt2 = cnn.getConnection().prepareStatement(sql);
                        stmt2.setString(1, "");
                        stmt2.setString(2, "");
                        stmt2.setString(3, msisdn);
                        if (logger.isDebugEnabled()) {
                            String sql_log = stmt2.toString();
                            sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                            logger.debug(sql_log);
                        }
                        stmt2.execute();
                        cnn.getConnection().commit();
                    }
                }
            } else {
                if (sender.length() > 0) {
                    sql = "insert into gift_account(msisdn,status,white_list,black_list,free_count) values(?,0,?,'',?)";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, msisdn);
                    stmt2.setString(2, "-" + sender);
                    stmt2.setString(3, ConfigStack.getConfig("gift", "free_count", "6"));
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    stmt2.execute();
                    cnn.getConnection().commit();
                } else {
                    sql = "insert into gift_account(msisdn,status,white_list,black_list,free_count) values(?,1,'','',?)";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, msisdn);
                    stmt2.setString(2, ConfigStack.getConfig("gift", "free_count", "6"));
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    stmt2.execute();
                    cnn.getConnection().commit();
                }
            }
        } catch (Exception e) {
            logger.error("Error in insertRegisterGift", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(re);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }

    }

    public int removeRegisterGift(String msisdn, String sender) {
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        PreparedStatement stmt3 = null;
        ResultSet re = null;
        try {
            cnn = getConnection();
            String sql = "select * from gift_account where msisdn = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re = stmt1.executeQuery();
            if (re.next()) {
                int status = re.getInt("status");
                String a = re.getString("white_list");
                String b = re.getString("black_list");
                if (a == null) {
                    a = "";
                }
                if (b == null) {
                    b = "";
                }

                if (status == 1) {
                    if (sender.length() > 0) {
                        a = a.replaceAll("-" + sender, "");
                        b = b.replaceAll("-" + sender, "");
                        b = b + "-" + sender;
                        sql = "update gift_account set white_list = ?, black_list = ? where msisdn = ?";
                        stmt2 = cnn.getConnection().prepareStatement(sql);
                        stmt2.setString(1, a);
                        stmt2.setString(2, b);
                        stmt2.setString(3, msisdn);
                        if (logger.isDebugEnabled()) {
                            String sql_log = stmt2.toString();
                            sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                            logger.debug(sql_log);
                        }
                        stmt2.execute();
                        cnn.getConnection().commit();
                    } else {
                        sql = "update gift_account set status = 0, white_list = ?, black_list = ? where msisdn = ?";
                        stmt2 = cnn.getConnection().prepareStatement(sql);
                        stmt2.setString(1, "");
                        stmt2.setString(2, "");
                        stmt2.setString(3, msisdn);
                        if (logger.isDebugEnabled()) {
                            String sql_log = stmt2.toString();
                            sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                            logger.debug(sql_log);
                        }
                        stmt2.execute();
                        cnn.getConnection().commit();
                    }
                } else {
                    if (sender.length() > 0) {
                        a = a.replaceAll("-" + sender, "");
                        b = b.replaceAll("-" + sender, "");
                        b = b + "-" + sender;
                        sql = "update gift_account set white_list = ?, black_list = ? where msisdn = ?";
                        stmt2 = cnn.getConnection().prepareStatement(sql);
                        stmt2.setString(1, a);
                        stmt2.setString(2, b);
                        stmt2.setString(3, msisdn);
                        if (logger.isDebugEnabled()) {
                            String sql_log = stmt2.toString();
                            sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                            logger.debug(sql_log);
                        }
                        stmt2.execute();
                        cnn.getConnection().commit();
                    } else {
                        sql = "update gift_account set status = 0, white_list = ?, black_list = ? where msisdn = ?";
                        stmt2 = cnn.getConnection().prepareStatement(sql);
                        stmt2.setString(1, "");
                        stmt2.setString(2, "");
                        stmt2.setString(3, msisdn);
                        if (logger.isDebugEnabled()) {
                            String sql_log = stmt2.toString();
                            sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                            logger.debug(sql_log);
                        }
                        stmt2.execute();
                        cnn.getConnection().commit();
                    }
                }

            } else {
                if (sender.length() > 0) {
                    sql = "insert into gift_account(msisdn,status,white_list,black_list,free_count) values(?,1,'',?,?)";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, msisdn);
                    stmt2.setString(2, "-" + sender);
                    stmt2.setString(3, ConfigStack.getConfig("gift", "free_count", "6"));
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    stmt2.execute();
                    cnn.getConnection().commit();
                } else {
                    sql = "insert into gift_account(msisdn,status,white_list,black_list,free_count) values(?,0,'','',?)";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, msisdn);
                    stmt2.setString(2, ConfigStack.getConfig("gift", "free_count", "6"));
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    stmt2.execute();
                    cnn.getConnection().commit();
                }
            }
            sql = "update gift_content set status = 4, updated_date = now() where receiver = ? ";
            if (sender.length() > 0) {
                sql = sql + "and sender = ? ";
            }
            sql = sql + "and (status = 0 or status = 2 or status = 3)";
            stmt3 = cnn.getConnection().prepareStatement(sql);
            stmt3.setString(1, msisdn);
            if (sender.length() > 0) {
                stmt3.setString(2, sender);
            }
            if (logger.isDebugEnabled()) {
                String sql_log = stmt3.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt3.execute();
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in removeRegisterGift", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(re);
            closeStatement(stmt1);
            closeStatement(stmt2);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return 0;
    }

    // Check msisdn receive gift
    public User checkMsisdnReceiveGift(String sender, String receiver, boolean isCharge) {
        User user = new User();
        user.setCode(BillingErrorCode.Success.getValue());
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        PreparedStatement stmt3 = null;
        ResultSet re = null;
        ResultSet re2 = null;
        try {
            cnn = getConnection();
            String sql = "select status, white_list, black_list from gift_account "
                    + "where msisdn = ?";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, receiver);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re = stmt.executeQuery();
            if (re.next()) {
                int status = re.getInt("status");
                if (status == 0) {
                    String a = re.getString("white_list");
                    if (!a.contains(sender)) {
                        user.setCode(BillingErrorCode.BlackList.getValue());
                    } else {
                        user.setCode(BillingErrorCode.Success.getValue());
                    }
                } else {
                    String a = re.getString("black_list");
                    if (a.contains(sender)) {
                        user.setCode(BillingErrorCode.BlackList.getValue());
                    } else {
                        user.setCode(BillingErrorCode.Success.getValue());
                    }
                }
            }
            if (isCharge && user.getCode() == BillingErrorCode.Success.getValue()) {
                sql = "select free_count from gift_account "
                        + "where msisdn = ?";
                stmt2 = cnn.getConnection().prepareStatement(sql);
                stmt2.setString(1, sender);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt2.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re2 = stmt2.executeQuery();
                if (re2.next()) {
                    int free_count = re2.getInt("free_count");
                    if (free_count > 0) {
                        sql = "update gift_account set free_count = free_count - 1 where msisdn = ?";
                        stmt3 = cnn.getConnection().prepareStatement(sql);
                        stmt3.setString(1, sender);
                        if (logger.isDebugEnabled()) {
                            String sql_log = stmt3.toString();
                            sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                            logger.debug(sql_log);
                        }
                        stmt3.execute();
                        cnn.getConnection().commit();
                        user.setSendGiftFree(true);
                        free_count = free_count - 1;
                    } else {
                        user.setSendGiftFree(false);
                    }
                    user.setGiftFree(free_count);
                } else {
                    int free_count = Integer.parseInt(ConfigStack.getConfig("gift", "free_count", "6")) - 1;
                    user.setSendGiftFree(true);
                    sql = "insert into gift_account(msisdn,status,white_list,black_list,free_count) values(?,1,'','',?)";
                    stmt3 = cnn.getConnection().prepareStatement(sql);
                    stmt3.setString(1, sender);
                    stmt3.setInt(2, free_count);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt3.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    stmt3.execute();
                    cnn.getConnection().commit();
                    user.setGiftFree(free_count);
                }
            }
        } catch (Exception e) {
            logger.error("Exception in checkMsisdnReceiveGift for ", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(re);
            closeResultSet(re2);
            closeStatement(stmt);
            closeStatement(stmt2);
            closeStatement(stmt3);
            freeConnection(cnn);
        }

        return user;
    }

    // Wap, Web
    public User insertUser(String msisdn, String password, String salt) {
        User user = new User();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select a.msisdn, a.status from sub_profile a "
                    + "where a.msisdn = ?";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                int status = rs.getInt("status");
                if (status != 1) {
                    sql = "update sub_profile set status = 1 where msisdn = ?";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, msisdn);
                    if (logger.isDebugEnabled()) {
                        String sql_log = stmt2.toString();
                        sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                        logger.debug(sql_log);
                    }
                    user.setSendPass(false);
                    stmt2.execute();
                    cnn.getConnection().commit();
                }
            } else {
                sql = "insert into sub_profile(password, salt, msisdn, status, created_date) "
                        + "values(?,?,?,1,now())";
                stmt2 = cnn.getConnection().prepareStatement(sql);
                stmt2.setString(1, password);
                stmt2.setString(2, salt);
                stmt2.setString(3, msisdn);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt2.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                user.setSendPass(true);
                stmt2.execute();
                cnn.getConnection().commit();
            }
            user.setCode(BillingErrorCode.Success.getValue());
            user.setPassword(password);
            user.setMsisdn(msisdn);
        } catch (Exception e) {
            logger.error("Exception in insertUser for ", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeStatement(stmt2);
            freeConnection(cnn);
        }

        return user;
    }

    // Reset Password
    public User resetPassword(String msisdn, String password, String salt) {
        User user = new User();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        try {
            cnn = getConnection();
            String sql = "insert into sub_profile(password, salt, msisdn, status) "
                    + "values(?,?,?,1) ON DUPLICATE KEY UPDATE password = ?, salt = ?, status = 1";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, password);
            stmt.setString(2, salt);
            stmt.setString(3, msisdn);
            stmt.setString(4, password);
            stmt.setString(5, salt);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.execute();
            cnn.getConnection().commit();
            user.setCode(BillingErrorCode.Success.getValue());
            user.setPassword(password);
            user.setMsisdn(msisdn);
        } catch (Exception e) {
            logger.error("Exception in resetPassword for ", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return user;
    }

    public User checkExistUser(String msisdn) {
        User user = new User();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select * from sub_profile where msisdn = ? and status = 1";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);

            rs = stmt.executeQuery();
            if (rs.next()) {
                user.setCode(BillingErrorCode.Success.getValue());
            } else {
                user.setCode(BillingErrorCode.NotFoundData.getValue());
            }
        } catch (Exception e) {
            logger.error("Exception in checkExistUser for ", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return user;
    }

    public HashMap<String, Object> getListMatchByChannel(
            int topic_ord, int channel_ord, int topic_type) {
        String key = this.getClass().getCanonicalName() + ".getListMatchByChannel." + topic_ord + "." + channel_ord + "." + topic_type;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getListMatchByChannel from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListMatchByChannel from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt3 = null;
        ResultSet re3 = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id,b.play_type,b.play_limit,b.limit_type "
                    + "from content_topic a, content_channel b "
                    + "where a.topic_id = b.topic_id "
                    + "and a.ivr_publish = 1 and b.ivr_publish = 1 "
                    + "and a.topic_ord = ? and a.topic_type = ? and b.channel_ord = ? "
                    + "and a.status = 1 and b.status = 1 ";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, topic_ord);
            stmt1.setInt(2, topic_type);
            stmt1.setInt(3, channel_ord);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int channel_id = re1.getInt("channel_id");
                int play_type = re1.getInt("play_type");
                int play_limit = re1.getInt("play_limit");
                if (play_limit == 0) {
                    play_limit = Integer.MAX_VALUE;
                }
                int limit_type = re1.getInt("limit_type");
                resp.put("channel_id", channel_id);
                resp.put("play_type", play_type);
                sql = "Select a.sport_content_id,b.content_path,a.content_ord, b.path_team_1, b.path_team_2 "
                        + "from sport_content_channel a, sport_content b "
                        + "where a.channel_id = ? "
                        + "and a.sport_content_id = b.sport_content_id "
                        + "and b.ivr_publish = 1 "
                        + "and a.status = 1 and b.status = 1 "
                        + "and (b.publish_date <= now() or b.publish_date_2 <= now()) "
                        + "order by a.content_ord desc, b.publish_date desc limit 0," + play_limit;
                if (limit_type == 1) {
                    sql = "Select a.sport_content_id,b.content_path,a.content_ord, b.path_team_1, b.path_team_2 "
                            + "from sport_content_channel a, sport_content b "
                            + "where a.channel_id = ? "
                            + "and a.sport_content_id = b.sport_content_id "
                            + "and b.ivr_publish = 1 "
                            + "and a.status = 1 and b.status = 1 "
                            + "and DATEDIFF(now(),b.publish_date) < " + play_limit + " "
                            + "and (b.publish_date <= now() or b.publish_date_2 <= now()) "
                            + "order by a.content_ord desc, b.publish_date desc";
                }
                stmt3 = cnn.getConnection().prepareStatement(sql);
                stmt3.setInt(1, channel_id);
                if (logger.isDebugEnabled()) {
                    String sql_log = stmt3.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                re3 = stmt3.executeQuery();
                ArrayList<Content> data = new ArrayList<Content>();
                while (re3.next()) {
                    String contentId = re3.getString("sport_content_id");
                    String contentPath = re3.getString("content_path");
                    String contentOrd = re3.getString("content_ord");
                    String pathTeam1 = re3.getString("path_team_1");
                    String pathTeam2 = re3.getString("path_team_2");
                    Content a = new Content();
                    a.setContentId(contentId);
                    a.setContentPath(contentPath);
                    a.setContentOrd(contentOrd);
                    a.setPathTeam1(pathTeam1);
                    a.setPathTeam2(pathTeam2);
                    data.add(a);
                }
                if (data.size() > 0) {
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    cache.put(new Element(key, resp));
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListMatchByChannel", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re3);
            closeStatement(stmt1);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getContentMatch(int content_id) {
        String key = this.getClass().getCanonicalName() + ".getContentMatch." + content_id;
        Element value = cache.get(key);
        if (value != null) {
            logger.debug("getContentMatch from cache");
            return (HashMap<String, Object>) value.getObjectValue();
        }
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getContentMatch from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "Select a.sport_content_id,a.content_path,a.content_path_2, "
                    + "TIMESTAMPDIFF(MINUTE,now(),a.publish_date) datediff1,"
                    + "TIMESTAMPDIFF(MINUTE,now(),a.publish_date_2) datediff2 "
                    + "from sport_content a "
                    + "where a.sport_content_id = ? "
                    + "and a.status = 1";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, content_id);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            if (re1.next()) {
                String contentId = re1.getString("sport_content_id");
                long datediff1 = re1.getLong("datediff1");
                long datediff2 = re1.getLong("datediff2");
                if (datediff1 <= 0
                        && re1.getString("content_path") != null
                        && re1.getString("content_path").length() > 0) {
                    Content a = new Content();
                    a.setContentId(contentId);
                    a.setContentPath(re1.getString("content_path"));
                    data.add(a);
                }
                if (datediff2 <= 0
                        && re1.getString("content_path_2") != null
                        && re1.getString("content_path_2").length() > 0) {
                    Content a = new Content();
                    a.setContentId(contentId);
                    a.setContentPath(re1.getString("content_path_2"));
                    data.add(a);
                }
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                cache.put(new Element(key, resp));
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getContentMatch", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> checkGift(String msisdn) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "select gift_content_id from gift_content where receiver = ? order by gift_content_id desc limit 0,1";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in checkGift", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListNewGiftMusic(String msisdn) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListNewGiftMusic from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "Select a.gift_content_id,a.message_path,b.content_path,a.sender,b.genre_id,a.audio_path,a.source "
                    + "from gift_content a, music_content b "
                    + "where a.content_id = b.music_content_id "
                    + "and a.receiver = ? "
                    + "and ((a.status = 0 and a.call_date<= now()) or a.status = 2 or a.status = 3) "
                    + "and a.user_deleted = 0 "
                    + "and (b.status = 1 and b.publish_date <= now()) "
                    + "order by a.call_date desc, a.gift_content_id desc";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            while (re1.next()) {
                Content a = new Content();
                a.setContentId(re1.getString("gift_content_id"));
                if (re1.getInt("source") == 0) {
                    if (re1.getInt("genre_id") == 0) {
                        a.setContentPath(re1.getString("content_path") + ".wav");
                    } else {
                        a.setContentPath(re1.getString("content_path") + ".mp3");
                    }
                } else {
                    a.setContentPath(re1.getString("audio_path") + ".wav");
                }
                a.setMessagePath(re1.getString("message_path"));
                a.setSender("0" + Helper.formatMobileNumberWithoutPrefix(re1.getString("sender")));
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListNewGiftMusic", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> updateGiftSuccess(String callType, String giftId, String listenFull, String duration) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("updateGiftSuccess from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "update gift_content set status = 1, updated_date = now(), "
                    + "channel_received = ?, listen_gift_status = ?, listen_gift_duration = ? "
                    + "where gift_content_id = ? and status != 1";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, Integer.parseInt(callType));
            stmt1.setInt(2, Integer.parseInt(listenFull));
            stmt1.setInt(3, Integer.parseInt(duration));
            stmt1.setInt(4, Integer.parseInt(giftId));
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt1.execute();
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in updateGiftSuccess", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListGiftMusic(String msisdn) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListGiftMusic from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        try {
            cnn = getConnection();
            String sql = "Select a.gift_content_id,a.message_path,b.content_path,a.sender,b.genre_id,a.audio_path,a.source "
                    + "from gift_content a, music_content b "
                    + "where a.content_id = b.music_content_id "
                    + "and a.receiver = ? "
                    + "and ((a.status = 0 and a.call_date<= now()) or a.status = 1 or a.status = 2 or a.status = 3) "
                    + "and a.user_deleted = 0 "
                    + "and (b.status = 1 and b.publish_date <= now()) "
                    + "order by a.call_date desc, a.gift_content_id desc";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re1 = stmt1.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            while (re1.next()) {
                Content a = new Content();
                a.setContentId(re1.getString("gift_content_id"));
                if (re1.getInt("source") == 0) {
                    if (re1.getInt("genre_id") == 0) {
                        a.setContentPath(re1.getString("content_path") + ".wav");
                    } else {
                        a.setContentPath(re1.getString("content_path") + ".mp3");
                    }
                } else {
                    a.setContentPath(re1.getString("audio_path") + ".wav");
                }
                a.setMessagePath(re1.getString("message_path"));
                a.setSender("0" + Helper.formatMobileNumberWithoutPrefix(re1.getString("sender")));
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in getListGiftMusic", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> createGift(String msisdn, String receiver, String contentId, String contentName, String messagePath, String timeSendMT, String timeSendGift, int topic_type, int fee, String code) {
        logger.debug("createGift");
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        try {
            cnn = getConnection();
            String sql = "insert into gift_content(sender,receiver,gift_content_name,content_id,message_path,send_mt_date,call_date,topic_type,fee,content_code,created_date,updated_date,status,retry) "
                    + "values(?,?,?,?,?,?,?,?,?,?,now(),now(),0,0)";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, receiver);
            stmt1.setString(3, contentName);
            stmt1.setString(4, contentId);
            stmt1.setString(5, messagePath);
            stmt1.setString(6, timeSendMT);
            stmt1.setString(7, timeSendGift);
            stmt1.setInt(8, topic_type);
            stmt1.setInt(9, fee);
            stmt1.setString(10, code);
            stmt1.execute();
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            cnn.getConnection().commit();
            resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
        } catch (Exception e) {
            logger.error("Error in createGift", e);
            rollbackTransaction(cnn);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> removeGift(String msisdn, String code) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        try {
            cnn = getConnection();
            String sql = "update gift_content set user_deleted = 1,updated_date = now() where receiver = ? and gift_content_id = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, code);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt1.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt1.execute();
            resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in removeGift", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public SubProfileInfo getSubProfile(String msisdn, int userId) {
        return getSubProfile(msisdn, userId, -1);
    }

    /**
     * *******************
     * Kich ban Ket ban
     */
    public SubProfileInfo getSubProfile(String msisdn, int userId, int telco) {
        logger.info(">>>>>>>>>>>>>>>>>>>>DB STACK  profile: " + msisdn);
        if (Helper.isNull(msisdn)) {
            msisdn = "";
        } else {
            msisdn = Helper.processMobile(msisdn);
        }
        SubProfileInfo profile = new SubProfileInfo();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;

        String sql = "";
        if (telco >= 0) {
            sql = "select * from sub_profile_friend where (msisdn = ? or user_id = ?) and telco = ?";
        } else {
            sql = "select * from sub_profile_friend where msisdn = ? or user_id = ?";
        }
        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setInt(2, userId);
            if (telco >= 0) {
                stmt1.setInt(3, telco);
            }

            logger.info(">> SQL getSubProfile:" + stmt1.toString());
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                profile.setMsisdn(re1.getString("msisdn"));
                profile.setUserId(re1.getInt("user_id"));
                profile.setSex(re1.getInt("sex"));
                profile.setBirthDay(re1.getString("birth_day"));
                profile.setBirthYear(re1.getInt("birth_year"));
                profile.setJob(re1.getInt("job"));
                profile.setName(re1.getString("name"));
                profile.setProvinceId(re1.getInt("province_id"));
                profile.setStatus(re1.getInt("status"));
                profile.setLastStep(re1.getInt("last_step"));
                profile.setSource(re1.getString("source"));
                profile.setIntroPath(re1.getString("intro_path"));
                profile.setTelco(re1.getInt("telco"));
            }
            logger.info(">>>>>>>>>>>>>>>>>>>>DB STACK  profile: " + profile.getUserId());
        } catch (Exception e) {
            logger.error("Error in getSubProfile", e);
            profile = null;
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return profile;
    }

    public SubProfileInfo generateNewUserId(String msisdn, int userID, String updatedDate, String createdDate) {
        SubProfileInfo profile = new SubProfileInfo();

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re1 = null;
        String sql_user_id = "select user_id from sub_profile_friend where msisdn = ? order by user_id desc limit 1";
        String sql_insert = "";
        if (userID == -1) {
            sql_insert = "insert into sub_profile_friend( msisdn,updated_date, created_date) values(?,now(),now())";
        } else {
            sql_insert = "insert into sub_profile_friend(user_id, msisdn,updated_date, created_date) values(?,?,STR_TO_DATE(?,'%Y%m%d%H%i%s'),now())";
        }
        int userId = 0;

        try {

            cnn = getConnection();
            stmt2 = cnn.getConnection().prepareStatement(sql_insert);
            if (userID == -1) {
                stmt2.setString(1, msisdn);
            } else {
                stmt2.setInt(1, userID);
                stmt2.setString(2, msisdn);
                stmt2.setString(3, updatedDate);
            }
            logger.info(stmt2.toString());
            stmt2.execute();
            cnn.getConnection().commit();

            stmt1 = cnn.getConnection().prepareStatement(sql_user_id);
            stmt1.setString(1, msisdn);
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                userId = re1.getInt("user_id");
            } else {
                throw new Exception(sql_user_id);
            }
            profile.setMsisdn(msisdn);
            profile.setUserId(userId);
        } catch (Exception e) {
            logger.error("Error in generateNewUserProfile", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            profile = null;
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }

        return profile;
    }

    public int refreshUserId(SubProfileInfo profile) {
        int newUserId = 0;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re1 = null;
        String sql_user_id = "select user_id from sub_profile_friend where msisdn = ? order by user_id desc limit 1";
        String sql_insert = "insert into sub_profile_friend(msisdn) values(?)";

        try {
            cnn = getConnection();
            stmt2 = cnn.getConnection().prepareStatement(sql_insert);
            stmt2.setString(1, profile.getMsisdn());
            stmt2.execute();
            cnn.getConnection().commit();
            stmt1 = cnn.getConnection().prepareStatement(sql_user_id);
            stmt1.setString(1, profile.getMsisdn());
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                newUserId = re1.getInt("user_id");
            } else {
                throw new Exception("No SEQ_SUB_PROFILE");
            }

        } catch (Exception e) {
            logger.error("Error in generateNewUserProfile", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            newUserId = 0;
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }

        return newUserId;
    }

    public boolean updateSubProfile(SubProfileInfo profile, int pointBonus, boolean autoApprove, boolean updateIntroPath) {
        boolean updateSuccess = false;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt_point = null;
        PreparedStatement stmt_log = null;
        PreparedStatement stmt_approve = null;
        int i = 0;

        String sql = "update sub_profile_friend set sex = ?, province_id = ?, job = ?, birth_day = ?, birth_year = ?, intro_path = ?, last_step = ?, source = ?, updated_date = now() ";
        if (updateIntroPath && !autoApprove) {
            sql += ", status = ? ";
        } else {
            if (profile.getStatus() > 0) {
                sql += ", status = ? ";
            }
        }
        sql += " where msisdn = ?";

        try {
            cnn = getConnection();

            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(++i, profile.getSex());
            stmt1.setInt(++i, profile.getProvinceId());
            stmt1.setInt(++i, profile.getJob());
            stmt1.setString(++i, profile.getBirthDay());
            stmt1.setInt(++i, profile.getBirthYear());
            stmt1.setString(++i, profile.getIntroPath());
            stmt1.setInt(++i, profile.getLastStep());
            stmt1.setString(++i, profile.getSource());
            if (updateIntroPath && !autoApprove) {
                /*
                 * Neu cap nhat file thu am thi chuyen sang che do cho Duyet lai (neu khong tu dong duyet)
                 */
                stmt1.setInt(++i, Constants.PROFILE_STATUS_WAITING);
            } else {
                if (profile.getStatus() > 0) {
                    stmt1.setInt(++i, profile.getStatus());
                }
            }
            stmt1.setString(++i, profile.getMsisdn());
            updateSuccess = stmt1.executeUpdate() > 0;

//            if (updateSuccess) {
//                /*
//                 * Tu dong duyet
//                 */
            if (autoApprove) {
                sql = "update sub_profile_friend set status = ? where msisdn = ?";
                stmt_approve = cnn.getConnection().prepareStatement(sql);
                stmt_approve.setInt(1, Constants.PROFILE_STATUS_ACTIVE);
                stmt_approve.setString(2, profile.getMsisdn());
                stmt_approve.executeUpdate();
            }
//
//                if (pointBonus > 0) {
//                    /*
//                     * Cong diem vao tai khoan
//                     */
//                    sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, updated_date = updated_date where msisdn = ?";
//                    stmt_point = cnn.getConnection().prepareStatement(sql);
//                    stmt_point.setInt(1, pointBonus);
//                    stmt_point.setInt(2, pointBonus);
//                    stmt_point.setString(3, profile.getMsisdn());
//                    stmt_point.executeUpdate();
//
//                    sql = "insert into sub_point_history( msisdn, package_id, sub_package_id, point, action) "
//                            + " values( ?, ?, ?, ?, ?)";
//                    stmt_log = cnn.getConnection().prepareStatement(sql);
//                    stmt_log.setString(1, profile.getMsisdn());
//                    stmt_log.setInt(2, profile.getPackageId());
//                    stmt_log.setInt(3, profile.getSubPackageId());
//                    stmt_log.setInt(4, pointBonus);
//                    stmt_log.setInt(5, Constants.POINT_ACTION_DECLARE_PROFILE);
//                    stmt_log.execute();
//                }
//            }
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in updateSubProfile", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            updateSuccess = false;
        } finally {
            closeStatement(stmt1);
            closeStatement(stmt_point);
            closeStatement(stmt_approve);
            closeStatement(stmt_log);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public Boolean hasUnReadVoiceMessage(String msisdn) {
        Boolean hasUnReadMsg = null;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select count(1) total from sub_message where receiver = ? and approve_status = ? and read_status = ? and message_type = ? and delete_status = 0";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setInt(2, Constants.MSG_APPROVED_STATUS);
            stmt1.setInt(3, Constants.MSG_UNREAD_STATUS);
            stmt1.setInt(4, Constants.MSG_VOICE_TYPE);

            rs = stmt1.executeQuery();
            if (rs.next()) {
                hasUnReadMsg = rs.getInt("total") > 0;
            }
        } catch (Exception e) {
            logger.error("Error in hasUnReadVoiceMessage", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return hasUnReadMsg;
    }

    public String getLastInteractDate1(String msisdn, int action) {
        String interactDate = null;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select to_char(last_interact_date, 'yyyy-mm-dd') last_interact_date "
                + " from sub_interact "
                + " where msisdn = ? "
                + " and action = ? ";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setInt(2, action);

            rs = stmt1.executeQuery();
            if (rs.next()) {
                interactDate = rs.getString("last_interact_date");
                if (interactDate == null) {
                    interactDate = "";
                }
            } else {
                interactDate = "";
            }
        } catch (Exception e) {
            logger.error("Error in getLastInteractDate", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return interactDate;
    }

    public boolean updateLastInteractDate(String msisdn, int action) {
        boolean updateSuccess = false;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt_up = null;
        ResultSet rs = null;
        String sql = "select count(1) total "
                + " from sub_interact "
                + " where msisdn = ? "
                + " and action = ? ";

        int total = 0;

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setInt(2, action);

            rs = stmt1.executeQuery();
            if (rs.next()) {
                total = rs.getInt("total");
            }

            if (total > 0) {
                sql = "update sub_interact set last_interact_date = now() where msisdn = ? and action = ? ";
            } else {
                sql = "insert into sub_interact(msisdn, action, last_interact_date) values(?, ?, now()) ";
            }
            stmt_up = cnn.getConnection().prepareStatement(sql);
            stmt_up.setString(1, msisdn);
            stmt_up.setInt(2, action);
            stmt_up.executeUpdate();

            cnn.getConnection().commit();
            updateSuccess = true;
        } catch (Exception e) {
            logger.error("Error in updateLastInteractDate", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public ArrayList<SubProfileInfo> getFriendList(int userId) {
        ArrayList<SubProfileInfo> list = new ArrayList<SubProfileInfo>();

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select t2.* from ("
                + "	select friend_user_id, updated_date "
                + "	from sub_friendlist "
                + "	where user_id = ? "
                + "	and status = ? "
                + ") t1 inner join ("
                + "  select msisdn, user_id, intro_path, status "
                + "  from sub_profile_friend "
                + "  where status != ? "
                + ") t2 on t1.friend_user_id = t2.user_id "
                + " order by t1.updated_date desc";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userId);
            stmt1.setInt(2, Constants.PROFILE_STATUS_ACTIVE);
            stmt1.setInt(3, Constants.PROFILE_STATUS_REMOVE);

            rs = stmt1.executeQuery();
            while (rs.next()) {
                SubProfileInfo friend = new SubProfileInfo();
                friend.setMsisdn(rs.getString("msisdn"));
                friend.setUserId(rs.getInt("user_id"));
                friend.setStatus(rs.getInt("status"));
                if (friend.getStatus() == Constants.PROFILE_STATUS_ACTIVE || friend.getStatus() == Constants.PROFILE_STATUS_GOOD) {
                    friend.setIntroPath(rs.getString("intro_path"));
                }

                list.add(friend);
            }

        } catch (Exception e) {
            logger.error("Error in getFriendList", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }

    public String addToFriendList(int userId, int friendUserId) {
        String result = Constants.SYSTEM_ERROR;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt_up = null;
        ResultSet rs = null;
        String sql = "select status "
                + " from sub_friendlist "
                + " where user_id = ? "
                + " and friend_user_id = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userId);
            stmt1.setInt(2, friendUserId);

            rs = stmt1.executeQuery();
            if (rs.next()) {
                int status = rs.getInt("status");
                if (status == Constants.PROFILE_STATUS_ACTIVE) {
                    /*
                     * Da add truoc do
                     */
                    result = Constants.DATA_EXIST;
                } else {
                    /*
                     * Update lai trang thai ACTIVE
                     */
                    sql = "update sub_friendlist set status = ?, updated_date = now() where user_id = ? and friend_user_id = ?";
                    stmt_up = cnn.getConnection().prepareStatement(sql);
                    stmt_up.setInt(1, Constants.PROFILE_STATUS_ACTIVE);
                    stmt_up.setInt(2, userId);
                    stmt_up.setInt(3, friendUserId);

                    stmt_up.executeUpdate();
                    cnn.getConnection().commit();
                    result = Constants.SUCCESS;
                }
            } else {
                /*
                 * Them moi
                 */
                sql = "insert into sub_friendlist(user_id, friend_user_id) values(?, ?)";
                stmt_up = cnn.getConnection().prepareStatement(sql);
                stmt_up.setInt(1, userId);
                stmt_up.setInt(2, friendUserId);

                stmt_up.executeUpdate();
                cnn.getConnection().commit();
                result = Constants.SUCCESS;
            }

        } catch (Exception e) {
            logger.error("Error in addToFriendList", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return result;
    }

    public boolean checkRemovedFromFrienelist(int userId, int friendUserId) {
        boolean result = false;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select a.`status` from sub_friendlist a where user_id = ? and friend_user_id = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userId);
            stmt1.setInt(2, friendUserId);
            logger.info(stmt1.toString());
            rs = stmt1.executeQuery();
            if (rs.next()) {
                logger.info("AAAAAAA: " + rs.getInt("status"));
                if (rs.getInt("status") == -1) {
                    result = true;
                }
            }
        } catch (Exception e) {
            logger.error("Error in removeFromFriendList", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return result;
    }

    public String removeFromFriendList(int userId, int friendUserId) {
        String result = Constants.SYSTEM_ERROR;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "update sub_friendlist set status = ?, updated_date = now() where user_id = ? and friend_user_id = ? and status <> -1";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, Constants.PROFILE_STATUS_REMOVE);
            stmt1.setInt(2, userId);
            stmt1.setInt(3, friendUserId);
            stmt1.executeUpdate();

            cnn.getConnection().commit();
            result = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("Error in removeFromFriendList", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return result;
    }

    public int countFriendList(int userId) {
        int count = 0;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select count(1) as total from ("
                + "	select friend_user_id "
                + "	from sub_friendlist "
                + "	where user_id = ? "
                + "	and status = ? "
                + ") t1 inner join ("
                + "  select user_id "
                + "  from sub_profile_friend "
                + "  where status != ? "
                + ") t2 on t1.friend_user_id = t2.user_id ";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userId);
            stmt1.setInt(2, Constants.PROFILE_STATUS_ACTIVE);
            stmt1.setInt(3, Constants.PROFILE_STATUS_REMOVE);

            rs = stmt1.executeQuery();
            if (rs.next()) {
                count = rs.getInt("total");
            }

        } catch (Exception e) {
            logger.error("Error in countFriendList", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return count;
    }

    public ArrayList<SubMessageInfo> getVoiceMsgList(int userId, int limit) {
        ArrayList<SubMessageInfo> list = new ArrayList<SubMessageInfo>();

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select * from (  "
                + "	select id_sender, voice_path, message_id, read_status, from_telco "
                + " from sub_message   	"
                + "where message_type = ?   "
                + "and approve_status = ?  "
                + " and id_receiver = ? "
                + " and delete_status = 0 "
                + "order by read_status, created_date desc limit ?) t1";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, Constants.MSG_VOICE_TYPE);
            stmt1.setInt(2, Constants.MSG_APPROVED_STATUS);
            stmt1.setInt(3, userId);
            stmt1.setInt(4, limit);
            logger.info("SQL: " + stmt1.toString());
            rs = stmt1.executeQuery();
            while (rs.next()) {
                SubMessageInfo msg = new SubMessageInfo();
                msg.setIdSender(rs.getInt("id_sender"));
                msg.setVoicePath(rs.getString("voice_path"));
                msg.setMessageId(rs.getInt("message_id"));
                msg.setReaded(rs.getInt("read_status"));
                msg.setFromTelco(rs.getInt("from_telco"));

                list.add(msg);
            }

        } catch (Exception e) {
            logger.error("Error in getVoiceMsgList", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }

    public ArrayList<SubMessageInfo> getVoiceMsgListFromUser(int userId, int friendUserId, int limit) {
        ArrayList<SubMessageInfo> list = new ArrayList<SubMessageInfo>();

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select * from ("
                + "  select id_sender, voice_path, message_id, read_status , from_telco"
                + "  from sub_message "
                + "  where message_type = ? "
                + "  and approve_status = ?"
                + " and delete_status = 0 "
                + "  and ((id_receiver = ? and id_sender = ?) or (id_receiver = ? and id_sender = ?))"
                + "order by read_status, created_date desc limit ?) t1";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, Constants.MSG_VOICE_TYPE);
            stmt1.setInt(2, Constants.MSG_APPROVED_STATUS);
            stmt1.setInt(3, userId);
            stmt1.setInt(4, friendUserId);
            stmt1.setInt(5, friendUserId);
            stmt1.setInt(6, userId);
            stmt1.setInt(7, limit);

            rs = stmt1.executeQuery();
            while (rs.next()) {
                SubMessageInfo msg = new SubMessageInfo();
                msg.setVoicePath(rs.getString("voice_path"));
                msg.setMessageId(rs.getInt("message_id"));
                msg.setReaded(rs.getInt("read_status"));
                msg.setFromTelco(rs.getInt("from_telco"));

                list.add(msg);
            }

        } catch (Exception e) {
            logger.error("Error in getVoiceMsgListFromUser", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<ProvinceInfo> getListProvince() {
        ArrayList<ProvinceInfo> list = new ArrayList<ProvinceInfo>();
        String key = "getListProvince";
        Element data = cache.get(key);
        if (data != null) {
            logger.debug("getListProvince from cache");
            return (ArrayList<ProvinceInfo>) data.getObjectValue();
        }

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select province_id, name, identify_sign, file_name, region from province "
                + " where status = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, Constants.PROFILE_STATUS_ACTIVE);
            logger.info("AAAAAA:" + stmt1.toString());
            rs = stmt1.executeQuery();
            while (rs.next()) {
                ProvinceInfo p = new ProvinceInfo();
                p.setName(rs.getString("name"));
                p.setProvinceId(rs.getInt("province_id"));
                p.setIdentifySign(rs.getString("identify_sign"));
                p.setFileName(rs.getString("file_name"));
                p.setRegion(rs.getInt("region"));

                list.add(p);
            }

        } catch (Exception e) {
            logger.error("Error in getListProvince", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }

    public boolean insertVoiceMessage(SubMessageInfo msg, int status) {
        boolean updateSuccess = false;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "insert into sub_message( id_sender, id_receiver, voice_path, message_type, approve_status, sender, receiver, push_status,from_telco, to_telco) "
                + " values( ?, ?, ?, ?, ?, ?, ?, ?)";
        if (status == Constants.MSG_APPROVED_STATUS) {
            sql = "insert into sub_message( id_sender, id_receiver, voice_path, message_type, approve_status, sender, receiver, push_status, approve_date,from_telco, to_telco) "
                    + "values( ?, ?, ?, ?, ?, ?, ?, ?, now(),?,?)";
        }

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, msg.getIdSender());
            stmt1.setInt(2, msg.getIdReceiver());
            stmt1.setString(3, msg.getVoicePath());
            stmt1.setInt(4, Constants.MSG_VOICE_TYPE);
            stmt1.setInt(5, status);
            stmt1.setString(6, msg.getSender());
            stmt1.setString(7, msg.getReceiver());
            stmt1.setInt(8, msg.getPushStatus());
            stmt1.setInt(9, msg.getFromTelco());
            stmt1.setInt(10, msg.getToTelco());

            stmt1.executeUpdate();

            cnn.getConnection().commit();
            updateSuccess = true;
        } catch (Exception e) {
            logger.error("Error in insertVoiceMessage", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            updateSuccess = false;
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public boolean updateReadedVoiceMessage(int messageId, String fromTelco) {
        boolean updateSuccess = false;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "update sub_message set read_status = ?, readed_date = now() where message_id = ? and read_status != ? and from_telco = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, Constants.MSG_READED_STATUS);
            stmt1.setInt(2, messageId);
            stmt1.setInt(3, Constants.MSG_READED_STATUS);
            stmt1.setInt(4, Integer.parseInt(fromTelco));
            stmt1.executeUpdate();

            cnn.getConnection().commit();
            updateSuccess = true;
        } catch (Exception e) {
            logger.error("Error in markReadedVoiceMessage", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            updateSuccess = false;
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public boolean updateViewedUserIds(String msisdn, String viewedUserIds) {
        boolean updateSuccess = false;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "update sub_interact_his set viewed_user_ids = ?, updated_date = now() where msisdn = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, viewedUserIds);
            stmt1.setString(2, msisdn);
            stmt1.executeUpdate();

            cnn.getConnection().commit();
            updateSuccess = true;
        } catch (Exception e) {
            logger.error("Error in updateViewedUserIds", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            updateSuccess = false;
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public boolean insertViewedUserIds(String msisdn, String viewedUserIds) {
        boolean updateSuccess = false;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "insert into sub_interact_his(msisdn, viewed_user_ids) values(?, ?)";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, viewedUserIds);
            stmt1.executeUpdate();

            cnn.getConnection().commit();
            updateSuccess = true;
        } catch (Exception e) {
            logger.error("Error in insertViewedUserIds", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            updateSuccess = false;
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public String getViewedUserIds(String msisdn) {
        String userIds = "";

        ResultSet rs = null;
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "select viewed_user_ids from sub_interact_his where msisdn = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            rs = stmt1.executeQuery();

            if (rs.next()) {
                userIds = rs.getString("viewed_user_ids");
                userIds = userIds == null ? "" : userIds;
            }

        } catch (Exception e) {
            logger.error("Error in getViewedUserIds", e);
            userIds = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return userIds;
    }

    public String getBlackListOfUser(int userId) {
        String blacklist = "";

        ResultSet rs = null;
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "select blacklist_ids from blacklist where user_id = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userId);
            rs = stmt1.executeQuery();

            if (rs.next()) {
                blacklist = rs.getString("blacklist_ids");
                blacklist = blacklist == null ? "" : blacklist;
            }

        } catch (Exception e) {
            logger.error("Error in getBlackListOfUser", e);
            blacklist = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return blacklist;
    }

    public String updateBlacklistOfUser(int userId, String blacklistIds) {
        String result = Constants.SYSTEM_ERROR;

        ResultSet rs = null;
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        String sql = "select count(1) cnt from blacklist where user_id = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userId);
            rs = stmt1.executeQuery();

            int count = 0;
            if (rs.next()) {
                count = rs.getInt("cnt");
            }

            if (count == 0) {
                sql = "insert into blacklist(blacklist_ids, telco, user_id) values(?, ?, ?)";
            } else {
                sql = "update blacklist set blacklist_ids = ?, updated_date = now(), telco = ? where user_id = ?";
            }
            stmt2 = cnn.getConnection().prepareStatement(sql);
            stmt2.setString(1, blacklistIds);
            stmt2.setInt(2, Constants.TELCO_VINA);
            stmt2.setInt(3, userId);
            stmt2.executeUpdate();

            cnn.getConnection().commit();
            result = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("Error in updateBlacklistOfUser", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }

        return result;
    }

    public String updateBlacklistSMSOfUser(int userId, String blacklistIds, int telco) {
        String result = Constants.SYSTEM_ERROR;

        ResultSet rs = null;
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        String sql = "select count(1) cnt from blacklist where user_id = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userId);
            rs = stmt1.executeQuery();

            int count = 0;
            if (rs.next()) {
                count = rs.getInt("cnt");
            }

            if (count == 0) {
                sql = "insert into blacklist(sms_blacklist_ids, user_id, telco) values(?, ?, ?)";
            } else {
                sql = "update blacklist set sms_blacklist_ids = ?, updated_date = now() where user_id = ?";
            }
            stmt2 = cnn.getConnection().prepareStatement(sql);
            stmt2.setString(1, blacklistIds);
            stmt2.setInt(2, userId);
            if (count == 0) {
                stmt2.setInt(3, telco);
            }
            stmt2.executeUpdate();

            cnn.getConnection().commit();
            result = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("Error in updateBlacklistOfUser", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }

        return result;
    }

    public ArrayList<SubProfileInfo> getListSubProfile(int status, int userId, int sex, int fromYear, int toYear, int provinceId, int region, int limit, int telcoFilter) {
        ArrayList<SubProfileInfo> list = new ArrayList<SubProfileInfo>();
        logger.info(">>> getListSubProfileDBStack :");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "";

        if (region <= 0) {
            sql = "select * from ("
                    + " select msisdn, user_id, intro_path "
                    + " from sub_profile_friend "
                    + " where user_id != ? ";
            if (status <= 0) {
                sql += " and (status = ? or status = ?) ";
            } else {
                sql += " and status = ? ";
            }

            if (sex > 0) {
                sql += " and sex = ? ";
            }
            if (telcoFilter >= 0) {
                sql += " and telco = ? ";
            }
            if (fromYear > 0 && toYear > 0) {
                sql += " and birth_year >= ? and birth_year <= ? ";
            }
            if (provinceId > 0) {
                sql += " and province_id = ? ";
            }
            sql += " order by rand()  ";
            sql += ") a limit ?";
        } else {
            sql = "select * from ("
                    + " select t1.* from ("
                    + " select msisdn, user_id, intro_path, province_id "
                    + " from sub_profile_friend "
                    + " where user_id != ? ";
            if (status <= 0) {
                sql += " and (status = ? or status = ?) ";
            } else {
                sql += " and status = ? ";
            }
            if (sex > 0) {
                sql += " and sex = ? ";
            }
            if (telcoFilter >= 0) {
                sql += " and telco = ? ";
            }
            if (fromYear > 0 && toYear > 0) {
                sql += " and birth_year >= ? and birth_year <= ? ";
            }
            sql += ") t1 inner join ("
                    + " select province_id "
                    + " from province"
                    + " where region = ? "
                    + " and status = ? "
                    + ") t2 on t1.province_id = t2.province_id "
                    + " order by rand() ";
            sql += ") a  limit ?";
        }

        try {
            cnn = getConnection();
            int i = 0;
            stmt1 = cnn.getConnection().prepareStatement(sql);
            if (region <= 0) {
                stmt1.setInt(++i, userId);
                if (status <= 0) {
                    stmt1.setInt(++i, Constants.PROFILE_STATUS_ACTIVE);
                    stmt1.setInt(++i, Constants.PROFILE_STATUS_GOOD);
                } else {
                    stmt1.setInt(++i, status);
                }
                if (sex > 0) {
                    stmt1.setInt(++i, sex);
                }
                if (telcoFilter >= 0) {
                    stmt1.setInt(++i, telcoFilter);
                }
                if (fromYear > 0 && toYear > 0) {
                    stmt1.setInt(++i, fromYear);
                    stmt1.setInt(++i, toYear);
                }
                if (provinceId > 0) {
                    stmt1.setInt(++i, provinceId);
                }
                stmt1.setInt(++i, limit);
            } else {
                stmt1.setInt(++i, userId);
                if (status <= 0) {
                    stmt1.setInt(++i, Constants.PROFILE_STATUS_ACTIVE);
                    stmt1.setInt(++i, Constants.PROFILE_STATUS_GOOD);
                } else {
                    stmt1.setInt(++i, status);
                }
                if (sex > 0) {
                    stmt1.setInt(++i, sex);
                }
                if (telcoFilter >= 0) {
                    stmt1.setInt(++i, telcoFilter);
                }
                if (fromYear > 0 && toYear > 0) {
                    stmt1.setInt(++i, fromYear);
                    stmt1.setInt(++i, toYear);
                }
                stmt1.setInt(++i, region);
                stmt1.setInt(++i, Constants.STATUS_PUBLIC);
                stmt1.setInt(++i, limit);
            }
            logger.info("SQL show new : " + stmt1.toString());
            rs = stmt1.executeQuery();
            while (rs.next()) {
                SubProfileInfo p = new SubProfileInfo();
                p.setUserId(rs.getInt("user_id"));
                p.setIntroPath(rs.getString("intro_path"));

                list.add(p);
            }

        } catch (Exception e) {
            logger.error("Error in getListSubProfile", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }

    public String checkSubInvited(String msisdn, String receiver) {
        String result = "";

        ResultSet rs = null;
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "select count(1) total from sub_invitation where receiver = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, receiver);
            //stmt1.setString(2, msisdn);
            rs = stmt1.executeQuery();

            if (rs.next()) {
                result = rs.getInt("total") > 0 ? Constants.DATA_EXIST : Constants.SUCCESS;
            } else {
                result = Constants.SUCCESS;
            }

        } catch (Exception e) {
            logger.error("Error in checkSubInvited", e);
            result = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return result;
    }

    public String insertSubInvitation(String sender, String receiver, int point, String source) {
        String result = Constants.SYSTEM_ERROR;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "insert into sub_invitation(msisdn, receiver, point, source) values(?, ?, ?, ?)";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, sender);
            stmt1.setString(2, receiver);
            stmt1.setInt(3, point);
            stmt1.setString(4, source);
            logger.info("SQL: " + stmt1.toString());
            stmt1.executeUpdate();

            cnn.getConnection().commit();
            result = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("Error in insertSubInvitation", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            result = Constants.SYSTEM_ERROR;
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return result;
    }

    public ArrayList<SubProfileInfo> getListSubProfileSMS(String msisdn, int sex, int birthYear, int provinceId, int limit) {
        ArrayList<SubProfileInfo> list = new ArrayList<SubProfileInfo>();

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select t2.*, t3.province_name from ( select * from ("
                + " select msisdn, user_id, sex, name, birth_year, province_id "
                + " from sub_profile_friend "
                + " where (status = ? or status = ?) "
                + " and msisdn != ? ";

        if (sex > 0) {
            sql += " and sex = ? ";
        }
        if (birthYear > 0) {
            sql += " and birth_year = ? ";
        }
        if (provinceId > 0) {
            sql += " and province_id = ? ";
        }
        sql += " order by rand() ";
        sql += ") t1 limit ?) t2 left join ("
                + " select name province_name, province_id from province where status = 1) t3 "
                + " on t2.province_id = t3.province_id ";

        try {
            cnn = getConnection();
            int i = 0;
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(++i, Constants.PROFILE_STATUS_ACTIVE);
            stmt1.setInt(++i, Constants.PROFILE_STATUS_GOOD);
            stmt1.setString(++i, msisdn);
            if (sex > 0) {
                stmt1.setInt(++i, sex);
            }
            if (birthYear > 0) {
                stmt1.setInt(++i, birthYear);
            }
            if (provinceId > 0) {
                stmt1.setInt(++i, provinceId);
            }
            stmt1.setInt(++i, limit);
            logger.info("AAAA SQL: " + stmt1.toString());
            rs = stmt1.executeQuery();
            while (rs.next()) {
                SubProfileInfo p = new SubProfileInfo();
                p.setUserId(rs.getInt("user_id"));
                p.setMsisdn(rs.getString("msisdn"));
                p.setSex(rs.getInt("sex"));
                p.setProvinceName(rs.getString("province_name"));
                p.setBirthYear(rs.getInt("birth_year"));
                p.setName(rs.getString("name"));

                list.add(p);
            }

        } catch (Exception e) {
            logger.error("Error in getListSubProfile", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }

    public boolean updateSubProfile(SubProfileInfo profile) {
        boolean updateSuccess = false;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt_point = null;
        PreparedStatement stmt_log = null;
        String sql = "update sub_profile_friend set sex = ?, province_id = ?, name = ?, birth_year = ?, source = ?, updated_date = now(), `status` = ? where msisdn = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, profile.getSex());
            stmt1.setInt(2, profile.getProvinceId());
            stmt1.setString(3, profile.getName());
            stmt1.setInt(4, profile.getBirthYear());
            stmt1.setString(5, profile.getSource());
            stmt1.setInt(6, profile.getStatus());
            stmt1.setString(7, profile.getMsisdn());

            logger.info("SQL updateSubProfile : " + stmt1.toString());
            updateSuccess = stmt1.executeUpdate() > 0;

//            if (updateSuccess && pointBonus > 0) {
//                /*
//                 * Cong diem vao tai khoan
//                 */
//                sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, updated_date = now() where msisdn = ?";
//                stmt_point = cnn.getConnection().prepareStatement(sql);
//                stmt_point.setInt(1, pointBonus);
//                stmt_point.setInt(2, pointBonus);
//                stmt_point.setString(3, profile.getMsisdn());
//                stmt_point.executeUpdate();
//
//                sql = "insert into sub_point_history(history_id, msisdn, package_id, sub_package_id, point, action) "
//                        + " values(seq_sub_point_history.nextval, ?, ?, ?, ?, ?)";
//                stmt_log = cnn.getConnection().prepareStatement(sql);
//                stmt_log.setString(1, profile.getMsisdn());
//                stmt_log.setInt(2, profile.getPackageId());
//                stmt_log.setInt(3, profile.getSubPackageId());
//                stmt_log.setInt(4, pointBonus);
//                stmt_log.setInt(5, Constants.POINT_ACTION_DECLARE_PROFILE);
//                stmt_log.execute();
//            }
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in updateSubProfile", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            updateSuccess = false;
        } finally {
            closeStatement(stmt1);
            closeStatement(stmt_point);
            closeStatement(stmt_log);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public ArrayList<SubProfileInfo> getFriendList(int userId, int limit) {
        ArrayList<SubProfileInfo> list = new ArrayList<SubProfileInfo>();

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
//        String sql = "select * from ("
//                + " select t2.*, t1.updated_date desc, t2.updated_date  from ("
//                + "	select friend_user_id, updated_date "
//                + "	from sub_friendlist "
//                + "	where user_id = ? "
//                + "	and status = ? "
//                + ") t1 inner join ("
//                + "  select msisdn, user_id, sex, name, province_id, birth_year, updated_date "
//                + "  from sub_profile "
//                + "  where status != ? "
//                + ") t2 on t1.friend_user_id = t2.user_id"
//                + ")  order by t1.updated_date desc, t2.updated_date desc limit  <= ? ";
        String sql = " 	select t2.*, t1.updated_date  "
                + "	from (	"
                + "		select friend_user_id, updated_date 	"
                + "		from sub_friendlist"
                + " 	where user_id = ? 	"
                + "		and status = ? ) t1 "
                + "	inner join ( "
                + "		select msisdn, user_id, sex, name, province_id, birth_year, updated_date   "
                + "		from sub_profile_friend   "
                + "		where status != ? ) t2 "
                + "	on t1.friend_user_id = t2.user_id order by t1.updated_date desc, t2.updated_date desc limit  ? ";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userId);
            stmt1.setInt(2, Constants.PROFILE_STATUS_ACTIVE);
            stmt1.setInt(3, Constants.PROFILE_STATUS_REMOVE);
            stmt1.setInt(4, limit);
            logger.info("SQL: " + stmt1.toString());
            rs = stmt1.executeQuery();
            while (rs.next()) {
                SubProfileInfo friend = new SubProfileInfo();
                friend.setMsisdn(rs.getString("msisdn"));
                friend.setUserId(rs.getInt("user_id"));
                friend.setBirthYear(rs.getInt("birth_year"));
                friend.setSex(rs.getInt("sex"));
                friend.setProvinceId(rs.getInt("province_id"));
                friend.setName(rs.getString("name"));

                list.add(friend);
            }

        } catch (Exception e) {
            logger.error("Error in getFriendList", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }

    public ArrayList<SubProfileInfo> getListSubProfile(String msisdn, int sex, int birthYear, int provinceId, int limit) {
        ArrayList<SubProfileInfo> list = new ArrayList<SubProfileInfo>();

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select t2.*, t3.province_name from ( select * from ("
                + " select msisdn, user_id, sex, name, birth_year, province_id "
                + " from sub_profile_friend "
                + " where (status = ? or status = ?) "
                + " and msisdn != ? ";

        if (sex > 0) {
            sql += " and sex = ? ";
        }
        if (birthYear > 0) {
            sql += " and birth_year = ? ";
        }
        if (provinceId > 0) {
            sql += " and province_id = ? ";
        }
        sql += " order by rand() ";
        sql += ") t1 limit ?) t2 left join ("
                + " select name province_name, province_id from province where status = 1) t3 "
                + " on t2.province_id = t3.province_id ";

        try {
            cnn = getConnection();
            int i = 0;
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(++i, Constants.PROFILE_STATUS_ACTIVE);
            stmt1.setInt(++i, Constants.PROFILE_STATUS_GOOD);
            stmt1.setString(++i, msisdn);
            if (sex > 0) {
                stmt1.setInt(++i, sex);
            }
            if (birthYear > 0) {
                stmt1.setInt(++i, birthYear);
            }
            if (provinceId > 0) {
                stmt1.setInt(++i, provinceId);
            }
            stmt1.setInt(++i, limit);

            rs = stmt1.executeQuery();
            while (rs.next()) {
                SubProfileInfo p = new SubProfileInfo();
                p.setUserId(rs.getInt("user_id"));
                p.setMsisdn(rs.getString("msisdn"));
                p.setSex(rs.getInt("sex"));
                p.setProvinceName(rs.getString("province_name"));
                p.setBirthYear(rs.getInt("birth_year"));
                p.setName(rs.getString("name"));

                list.add(p);
            }

        } catch (Exception e) {
            logger.error("Error in getListSubProfile", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }

    public HashMap<String, String> getBlackListOfUserSMS(int userId) {
        HashMap<String, String> blacklist = new HashMap<String, String>();

        ResultSet rs = null;
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "select blacklist_ids, sms_blacklist_ids from blacklist where user_id = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userId);
            logger.info("SQL : " + stmt1.toString());
            rs = stmt1.executeQuery();

            if (rs.next()) {
                String list = rs.getString("blacklist_ids");
                list = list == null ? "" : list;
                blacklist.put("ivr", list);

                list = rs.getString("sms_blacklist_ids");
                list = list == null ? "" : list;
                blacklist.put("sms", list);
            }

        } catch (Exception e) {
            logger.error("Error in getBlackListOfUser", e);
            blacklist = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return blacklist;
    }

    public int countChatSMSHistoryInCurrentDay(String msisdn) {
        int count = 0;

        ResultSet rs = null;
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "select sms_count from sub_interact_his where msisdn = ? and DATE(last_sms_date) = DATE(now())";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            logger.info("SQL: " + stmt1.toString());
            rs = stmt1.executeQuery();

            if (rs.next()) {
                count = rs.getInt("sms_count");
            }

        } catch (Exception e) {
            logger.error("Error in getChatSMSCountInCurrentDay", e);
            count = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return count;
    }

    public int countInFriendList(int userId, int friendUserId) {
        int count = 0;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select count(1) total from ("
                + "	select friend_user_id "
                + "	from sub_friendlist "
                + "	where user_id = ? "
                + "	and friend_user_id = ? "
                + "	and status = ? "
                + ") t1 inner join ("
                + "	select user_id "
                + "	from sub_profile_friend "
                + "	where user_id = ? "
                + "	and status != ? "
                + ") t2 on t1.friend_user_id = t2.user_id";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userId);
            stmt1.setInt(2, friendUserId);
            stmt1.setInt(3, Constants.PROFILE_STATUS_ACTIVE);
            stmt1.setInt(4, friendUserId);
            stmt1.setInt(5, Constants.PROFILE_STATUS_REMOVE);
            logger.info(stmt1.toString());
            rs = stmt1.executeQuery();
            if (rs.next()) {
                count = rs.getInt("total");
            }

        } catch (Exception e) {
            logger.error("Error in countInFriendList", e);
            count = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return count;
    }

    public boolean insertChatSMS(SubMessageInfo msg, int status) {
        boolean updateSuccess = false;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "insert into sub_message( id_sender, id_receiver, sms_content, message_type, approve_status, sender, receiver, push_status, from_telco, to_telco) "
                + " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        if (status == Constants.MSG_APPROVED_STATUS) {
            sql = "insert into sub_message( id_sender, id_receiver, sms_content, message_type, approve_status, sender, receiver, push_status, approve_date, from_telco, to_telco) "
                    + "values( ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, ?)";
        }

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, msg.getIdSender());
            stmt1.setInt(2, msg.getIdReceiver());
            stmt1.setString(3, msg.getSmsContent());
            stmt1.setInt(4, Constants.MSG_SMS_TYPE);
            stmt1.setInt(5, status);
            stmt1.setString(6, msg.getSender());
            stmt1.setString(7, msg.getReceiver());
            stmt1.setInt(8, msg.getPushStatus());
            stmt1.setInt(9, msg.getFromTelco());
            stmt1.setInt(10, msg.getToTelco());
            stmt1.executeUpdate();

            cnn.getConnection().commit();
            updateSuccess = true;
        } catch (Exception e) {
            logger.error("Error in insertChatSMS", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            updateSuccess = false;
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public boolean updateChatSMSHistory(String msisdn, int smsCount) {
        boolean updateSuccess = false;

        DbConnection cnn = null;
        ResultSet rs = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmtGet = null;
        String sql = "select count(1) total from sub_interact_his where msisdn = ?";

        try {
            cnn = getConnection();
            stmtGet = cnn.getConnection().prepareStatement(sql);
            stmtGet.setString(1, msisdn);
            rs = stmtGet.executeQuery();
            int count = 0;
            if (rs.next()) {
                count = rs.getInt("total");
            }
            if (count > 0) {
                sql = "update sub_interact_his set last_sms_date = DATE(now()), updated_date = now(), sms_count = ? where msisdn = ?";
            } else {
                sql = "insert into sub_interact_his(sms_count, msisdn, last_sms_date) values(?, ?, DATE(now()))";
            }
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, smsCount);
            stmt1.setString(2, msisdn);
            stmt1.executeUpdate();

            cnn.getConnection().commit();
            updateSuccess = true;
        } catch (Exception e) {
            logger.error("Error in updateChatSMS", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            updateSuccess = false;
        } finally {
            closeResultSet(rs);
            closeStatement(stmtGet);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public String getMsisdnFromID(int userID) {

        String msisdn = "";

        ResultSet rs = null;
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "select msisdn from sub_profile_friend where user_id = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userID);
            logger.info("SQL: " + stmt1.toString());
            rs = stmt1.executeQuery();

            if (rs.next()) {
                msisdn = rs.getString("msisdn");
            }

        } catch (Exception e) {
            logger.error("Error in getChatSMSCountInCurrentDay", e);
            msisdn = "";
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return msisdn;
    }

    public int getTelcoFromID(int userID) {

        int telco = 0;

        ResultSet rs = null;
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "select telco from sub_profile_friend where user_id = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, userID);
            logger.info("SQL: " + stmt1.toString());
            rs = stmt1.executeQuery();

            if (rs.next()) {
                telco = rs.getInt("telco");
            }

        } catch (Exception e) {
            logger.error("Error in getChatSMSCountInCurrentDay", e);
            telco = 0;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return telco;
    }

    public String getLastInteractDate(String msisdn, int action) {
        String interactDate = null;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select DATE_FORMAT(last_interact_date, '%Y-%m-%d') last_interact_date "
                + " from sub_interact "
                + " where msisdn = ? "
                + " and action = ? ";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setInt(2, action);
            logger.info(stmt1.toString());
            rs = stmt1.executeQuery();
            if (rs.next()) {
                interactDate = rs.getString("last_interact_date");
                if (interactDate == null) {
                    interactDate = "";
                }
            } else {
                interactDate = "";
            }
        } catch (Exception e) {
            logger.error("Error in getLastInteractDate", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return interactDate;
    }

    public ArrayList<Content> getNewsMultiPartByChannel(int channelOrd,
            int topicOrd, int topicType) {
        ArrayList<Content> news = new ArrayList<Content>();
        String key = "getNewsMultiPartByChannel_" + channelOrd + "_"
                + topicOrd;
        Element data = cache.get(key);
        if (data != null) {
            logger.debug(key + " get from cache");
            news = (ArrayList<Content>) data.getObjectValue();
            if (news != null && news.size() > 0) {
                return (ArrayList<Content>) news.clone();
            }
        }
        logger.debug(key + " get from database");

        PreparedStatement stmtGetChannel = null;
        PreparedStatement stmtGetContent = null;
        DbConnection cnn = null;
        ResultSet re = null;
        ResultSet re2 = null;

        try {
            cnn = getConnection();
            String sql = "SELECT a.channel_id, a.play_limit "
                    + "FROM content_channel a inner join content_topic b on a.topic_id = b.topic_id "
                    + "WHERE a.channel_ord = ? "
                    + "and a.status = ? and b.topic_ord = ? and b.topic_type = ?";
            stmtGetChannel = cnn.getConnection().prepareStatement(sql);
            stmtGetChannel.setInt(1, channelOrd);
            stmtGetChannel.setInt(2, Constants.NEWS_CHANNEL_STATUS_PUBLIC);
            stmtGetChannel.setInt(3, topicOrd);
            stmtGetChannel.setInt(4, topicType);
            logger.info("AAAAAAAAAAAAAAA stmtGetChannel: " + stmtGetChannel.toString());
            re = stmtGetChannel.executeQuery();
            if (re.next()) {
                int channelId = re.getInt("channel_id");
                int limit = re.getInt("play_limit");

                sql = "select * from ("
                        + "	select b.news_content_id, b.channel_id,b.content_ord "
                        + " from news_content_channel b "
                        + " where b.channel_id = ? and b.`status` = ?) t1 "
                        + "inner join ( "
                        + " select a.content_path,a.name_path, a.news_content_id, a.total_part "
                        + " from news_content a "
                        + "  where a.publish_date < now() and a.`status` = ?) t2 "
                        + " where t1.news_content_id = t2.news_content_id ";
                if (topicOrd == 1 && channelOrd == 10) {
                    sql += " order by t1.news_content_id desc ";
                }
                if (limit > 0) {
                    sql += "limit ?";
                }

                stmtGetContent = cnn.getConnection().prepareStatement(sql);
                stmtGetContent.setInt(1, channelId);
                stmtGetContent.setInt(2, Constants.NEWS_STATUS_PUBLIC);
                stmtGetContent.setInt(3, Constants.NEWS_STATUS_PUBLIC);
                if (limit > 0) {
                    stmtGetContent.setInt(4, limit);
                }

                logger.info("stmtGetContent: " + stmtGetContent.toString());
                re2 = stmtGetContent.executeQuery();
                while (re2.next()) {
                    Content item = new Content();
                    item.setChannelId(String.valueOf(channelId));
                    item.setContentId(re2.getString("news_content_id"));
                    item.setNamePath(re2.getString("name_path"));
                    item.setTotalPart(re2.getString("total_part"));
                    item.setChannelId(re2.getString("channel_id"));
                    news.add(item);
                }

                if (news.size() > 0) {
                    cache.put(new Element(key, news));
                }
            }

        } catch (Exception ex) {
            logger.error("error in getNewsMultiPartByChannel in database", ex);
        } finally {
            closeResultSet(re);
            closeResultSet(re2);
            closeStatement(stmtGetContent);
            closeStatement(stmtGetChannel);
            freeConnection(cnn);
        }

        return news;
    }

    public ArrayList<Content> getListPartOfNews(int contentId) {
        ArrayList<Content> news = new ArrayList<Content>();
        String key = "getListPartOfNews_" + contentId;
        Element data = cache.get(key);
        if (data != null) {
            logger.debug(key + " get from cache");
            news = (ArrayList<Content>) data.getObjectValue();
            if (news != null && news.size() > 0) {
                return (ArrayList<Content>) news.clone();
            }
        }
        logger.debug(key + " get from database");

        PreparedStatement stmtGetContent = null;
        DbConnection cnn = null;
        ResultSet re = null;
        String sql = "select t1.news_content_id, t1.part_number, t1.path, t2.total_part, t2.name_path "
                + " from news_content_part t1 inner join news_content t2 on t1.news_content_id = t2.news_content_id "
                + " where t1.status = ? and t2.status = ? "
                + " and t2.publish_date <= now() and t1.publish_date <= now() "
                + " and t1.news_content_id = ? "
                + " order by t1.part_number";

        try {
            cnn = getConnection();

            stmtGetContent = cnn.getConnection().prepareStatement(sql);
            stmtGetContent.setInt(1, Constants.NEWS_STATUS_PUBLIC);
            stmtGetContent.setInt(2, Constants.NEWS_STATUS_PUBLIC);
            stmtGetContent.setInt(3, contentId);
            logger.debug(">>>>> SQL stmtGetContent: " + stmtGetContent.toString());
            re = stmtGetContent.executeQuery();
            while (re.next()) {
                Content item = new Content();
                item.setID(re.getString("news_content_id"));
                item.setPath(re.getString("path"));
                item.setPartID(String.valueOf(re.getInt("part_number")));
                if (news.size() == 0) {
                    item.setNamePath(re.getString("name_path"));
                    item.setTotalPart(String.valueOf(re.getInt("total_part")));
                }

                news.add(item);
            }

            if (news.size() > 0) {
                cache.put(new Element(key, news));
            }

        } catch (Exception ex) {
            logger.error("error in getListPartOfNews in database", ex);
            news = null;
        } finally {
            closeResultSet(re);
            closeStatement(stmtGetContent);
            freeConnection(cnn);
        }

        return news;
    }

    public int checkExitpackage(String msisdn) {
        int count = -1;
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select count(a.msisdn)  Total "
                    + " from ( select DISTINCT(msisdn) from sub_ctkm c where c.msisdn = ? and c.status_reg <> 1) a"
                    + " LEFT join sub_package b "
                    + " on a.msisdn = b.msisdn "
                    + " where b.msisdn is null ";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            rs = stmt.executeQuery();
            logger.info("sQL :" + stmt.toString());
            if (rs.next()) {
                count = rs.getInt("Total");
                logger.info("Total :" + count);
            }
        } catch (Exception e) {
            logger.error("Exception in checkExitpackage for ", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return count;
    }

    public int insertSubCTKM(String msisdn) {
        int result = -1;
        Integer subPackageId = 0;
        DbConnection cnn = null;
        PreparedStatement stmt_up = null;
        try {
            cnn = getConnection();
            String sql_up = "update sub_ctkm set status_reg = 1 where msisdn = ?";

            stmt_up = cnn.getConnection().prepareStatement(sql_up);
            stmt_up.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt_up.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            result = stmt_up.executeUpdate();

        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("SQLException in insertSubCTKM: " + msisdn, e);
        } finally {
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return result;
    }

    public boolean deleteVoiceMessage(int messageId, int fromTelco) {
        boolean updateSuccess = false;
        logger.info("AAAAAAA");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "update sub_message set delete_status = 1 where message_id = ? and from_telco = ? ";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, messageId);
            stmt1.setInt(2, fromTelco);
            stmt1.executeUpdate();

            cnn.getConnection().commit();
            updateSuccess = true;
        } catch (Exception e) {
            logger.error("Error in markReadedVoiceMessage", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
            updateSuccess = false;
        } finally {
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public void updateStatusNoUpdateProfile(String msisdn) {

        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "UPDATE `sub_profile_friend` SET `status`=5 WHERE  msisdn = ?";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.executeUpdate();
            cnn.getConnection().commit();
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("SQLException in updateStatusNoUpdateProfile", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
    }

    public void updateStatusWeakProfile(String msisdn) {

        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "UPDATE `sub_profile_friend` SET `status`=3 WHERE  msisdn = ?";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.executeUpdate();
            cnn.getConnection().commit();
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("SQLException in updateStatusNoUpdateProfile", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
    }

    public int checkStausProfile(String msisdn) {
        int result = -1;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select a.`status` from sub_profile_friend a where a.msisdn = ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            logger.info(stmt1.toString());
            rs = stmt1.executeQuery();
            if (rs.next()) {
                logger.info("AAAAAAA: " + rs.getInt("status"));
                result = rs.getInt("status");
            }
        } catch (Exception e) {
            logger.error("Error in checkStausProfile", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getMessage());
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return result;
    }

    public HashMap<String, Object> getListSubMessageSendMt(int status) {
        HashMap<String, Object> hm = new HashMap<String, Object>();
        SubMessageInfo messageInfo = new SubMessageInfo();
        ArrayList<SubMessageInfo> data = new ArrayList<SubMessageInfo>();
        logger.debug("getSubMessageSendMt from DB");
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet re = null;
        String sql = "select message_id,id_sender,id_receiver,sender,receiver,to_telco,sms_content,message_type from sub_message where push_status = ? and to_telco =? and read_status = 0";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, status);
            stmt.setInt(2, 2);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            re = stmt.executeQuery();
            while (re.next()) {
                messageInfo = new SubMessageInfo();
                messageInfo.setMessageId(re.getInt("message_id"));
                messageInfo.setIdSender(re.getInt("id_sender"));
                messageInfo.setIdReceiver(re.getInt("id_receiver"));
                messageInfo.setSender(re.getString("sender"));
                messageInfo.setReceiver(re.getString("receiver"));
                messageInfo.setToTelco(re.getInt("to_telco"));
                messageInfo.setSmsContent(re.getString("sms_content"));
                messageInfo.setMessageType(re.getInt("message_type"));
                data.add(messageInfo);
            }
            if (data.size() > 0) {
                logger.info("DAo getListSubMessageSendMt :" + data.size());
                hm.put("data", data);
                hm.put(Constants.ERROR_CODE, Constants.SUCCESS);

            } else {
                hm.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("error in getListSubMessageSendMt in database", e);
        } finally {
            closeResultSet(re);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return hm;
    }

    public void updateSubMessageSendMt(SubMessageInfo subMesssage) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "";
        if (subMesssage.getMessageType() == 2) {
            sql = "UPDATE `sub_message` SET `push_status`=1 ,read_status = 1 ,readed_date = NOW() WHERE  message_id = ? and push_status = 0 ";
        } else {
            sql = "UPDATE `sub_message` SET `push_status`=1 WHERE  message_id = ? and push_status = 0 ";
        }

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, subMesssage.getMessageId());
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.executeUpdate();
            cnn.getConnection().commit();
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("SQLException in updateSubMessageSendMt", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
    }
// mini game 

    /**
     *
     * @param key
     * @return
     */
    public int deleteConfigKeyValue(String key) {
        DbConnection conn = null;
        int result = -1;
        PreparedStatement stmt_del = null;
        String sql = "delete from config_key_value where key = ?";

        try {
            conn = getConnection();
            stmt_del = conn.getConnection().prepareStatement(sql);
            stmt_del.setString(1, key);
            stmt_del.executeUpdate();

            conn.getConnection().commit();
            result = 0;
        } catch (SQLException e) {
            logger.trace(e);
            if (conn != null) {
                try {
                    conn.getConnection().rollback();
                } catch (SQLException e1) {
                }
            }
        } catch (Exception e) {
            logger.trace(e);
        } finally {
            closeStatement(stmt_del);
            freeConnection(conn);
        }

        return result;
    }

    public int insertConfigKeyValueWithTimeout(String key, String value, int minuteTimeout) {
        DbConnection cnn = null;
        int result = -1;
        PreparedStatement stmt = null;
        PreparedStatement stmt_del = null;
        String sql = "delete from config_key_value where key = ? and CEILING(TIMESTAMPDIFF(SECOND,created_at, NOW())/60) > ? or value = ?)";

        try {
            cnn = getConnection();
            stmt_del = cnn.getConnection().prepareStatement(sql);
            stmt_del.setString(1, key);
            stmt_del.setInt(2, minuteTimeout);
            stmt_del.setString(3, value);
            stmt_del.executeUpdate();

            sql = "insert into config_key_value(key, value, created_at) values(?, ?, now())";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.execute();
            cnn.getConnection().commit();

            result = 0;
        } catch (Exception e) {
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (Exception e1) {
                }
            }
        } finally {
            closeStatement(stmt_del);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return result;
    }
}
