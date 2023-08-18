package com.vega.service.api.db;

import com.google.gson.Gson;
import com.vega.service.api.MTRemindInfo;
import com.vega.service.api.RestfulStack;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.DateUtil;
import com.vega.service.api.common.SMSHelper;
import com.vega.service.api.config.Config;
import com.vega.service.api.logfile.LogFileStack;
import com.vega.service.api.object.*;
import com.vega.service.api.object.SubPackageInfo.PackageFunPromotionStatus;
import com.vega.service.api.object.SubPackageInfo.SubPackageStatus;
import com.vega.vcs.service.cache.CacheService;
import com.vega.vcs.service.database.DBPool;
import com.vega.vcs.service.database.pool.DbConnection;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;

public class DBService {

    static transient Logger logger = Logger.getLogger(DBService.class);
    private DBPool dbPool;
    private SimpleDateFormat dateFormat;
    private ArrayList<PackageInfo> packages = new ArrayList<PackageInfo>();
    private Hashtable<Integer, String> listTopics = new Hashtable<Integer, String>();
    private Hashtable<Integer, String> listChannels = new Hashtable<Integer, String>();
    private Hashtable<Integer, String> listContents = new Hashtable<Integer, String>();

    private Object lock = new Object();
    Gson gson = new Gson();
    Ehcache cache;

    public boolean start() throws NamingException {
        Context ctx = new InitialContext();
        dbPool = (DBPool) ctx.lookup("service/dbpool");
        CacheService cacheService = (CacheService) ctx.lookup("service/cache");
        cache = cacheService.getCache("alofun");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
     * @throws Exception
     */
    private DbConnection getConnection() throws Exception {
        DbConnection conn = null;
        try {
            conn = dbPool.getConnectionPool().getConnection();
            while (!conn.getConnection().isValid(5)) {
                conn = dbPool.getConnectionPool().getConnection();
            }
        } catch (Exception ex) {
            logger.error(ex);
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
            packages.clear();
            getListPackage();
        }
    }

    public ArrayList<PackageInfo> getListPackage() {
        if (packages.size() == 0) {
            synchronized (lock) {
                if (packages.size() == 0) {
                    DbConnection cnn = null;
                    PreparedStatement stmt = null;
                    ResultSet rs = null;
                    try {
                        cnn = getConnection();
                        String sql = "select package_id, package_name, sub_fee, time_life, free_minutes, pending_life, allow_renew, over_fee, debit_life, time_life_promotion, free_minutes_promotion, over_fee_pending from package";
                        stmt = cnn.getConnection().prepareStatement(sql);
                        rs = stmt.executeQuery();
                        while (rs.next()) {
                            PackageInfo p = new PackageInfo();
                            p.setPackageId(rs.getInt("package_id"));
                            p.setPackageName(rs.getString("package_name"));
                            p.setFreeMinutes(rs.getInt("free_minutes"));
                            p.setSubFee(rs.getInt("sub_fee"));
                            p.setAllowRenew(rs.getInt("allow_renew"));
                            p.setPendingLife(rs.getInt("pending_life"));
                            p.setTimeLife(rs.getInt("time_life"));
                            p.setOverFee(rs.getInt("over_fee"));
                            p.setDebitLife(rs.getInt("debit_life"));
                            p.setTimeLifePromotion(rs.getInt("time_life_promotion"));
                            p.setFreeMinutesPromotion(rs.getInt("free_minutes_promotion"));
                            p.setOverFeePending(rs.getInt("over_fee_pending"));
                            packages.add(p);
                        }
                        cnn.getConnection().commit();
                    } catch (Exception e) {
                        logger.error("Error in getListPackage", e);
                    } finally {
                        closeResultSet(rs);
                        closeStatement(stmt);
                        freeConnection(cnn);
                    }
                }
            }
        }

        return packages;
    }

    /**
     * Lay thong tin chu ki goi cuoc moi nhat cua thue bao
     *
     * @param msisdn
     * @param checkPromotionWhenActive : Kiem tra dieu kien khuyen mai khi dang
     * active 1 goi cuoc ?
     * @param checkChargingSubLocking : Kiem tra giao dich charging sub truoc do
     * ?
     * @return
     */
    public SubPackageInfo getLastSubPackage(String msisdn, int regPackageId,
            boolean checkPromotionWhenActive, boolean checkChargingSubLocking, int timeReSetPromoton) {
        logger.info("getLastSubPackage  funtion ");
        SubPackageInfo subInfo = new SubPackageInfo();
        subInfo.setErrorCode(BillingErrorCode.SystemError);
        subInfo.setFunPromotion(PackageFunPromotionStatus.NO_PROMOTION);
        subInfo.setMsisdn(msisdn);

        DbConnection cnn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt_check = null;
        PreparedStatement stmt_cnt = null;
        ResultSet rs_check = null;
        ResultSet rs_cnt = null;
        ResultSet rs = null;

        String sql = "SELECT sub_package_id, msisdn, package_id, free_minutes, status, reg_at, "
                + " updated_at, expire_at, source, charge_success, DATEDIFF(NOW(), expire_at) AS expire_day, renewed "
                + " FROM sub_package "
                + " WHERE msisdn = ? "
                + " AND (? = 0 OR package_id = ?)"
                + " ORDER BY updated_at DESC, sub_package_id DESC LIMIT 1";

        String sql_check = "SELECT distinct(package_id) as package_id "
                + "  FROM sub_package a"
                + "  WHERE msisdn = ? and (DATEDIFF(now(),a.updated_at )> ?)";

        String sql_count = "select count(*) as cnt from charge_locking where msisdn = ? and charge_type = ? and TIMESTAMPDIFF(MINUTE, created_at, NOW()) <= ?";
        boolean chargeSubLocked = false;

        try {
            cnn = getConnection();
            if (checkChargingSubLocking) {
                stmt_cnt = cnn.getConnection().prepareStatement(sql_count);
                stmt_cnt.setString(1, msisdn);
                stmt_cnt.setInt(2, Constants.CHARGE_TYPE_SUB);
                stmt_cnt.setInt(3,
                        Config.billChargeLockingExpireInMinute);

                rs_cnt = stmt_cnt.executeQuery();
                if (rs_cnt.next() && rs_cnt.getInt("cnt") > 0) {
                    subInfo.setErrorCode(BillingErrorCode.ChargingSubProcessing);
                    chargeSubLocked = true;
                }
            }

            if (!chargeSubLocked) {
                stmt = cnn.getConnection().prepareStatement(sql);
                stmt.setString(1, msisdn);
                stmt.setInt(2, regPackageId);
                stmt.setInt(3, regPackageId);

                rs = stmt.executeQuery();
                if (rs.next()) {
                    subInfo.setSubPackageId(rs.getInt("sub_package_id"));
                    subInfo.setMsisdn(rs.getString("msisdn"));
                    subInfo.setPackageId(rs.getInt("package_id"));
                    subInfo.setUpdatedAt(dateFormat.format(rs.getTimestamp("updated_at")));
                    subInfo.setRegAt(dateFormat.format(rs.getTimestamp("reg_at")));
                    subInfo.setExpireAt(dateFormat.format(rs.getTimestamp("expire_at")));
                    subInfo.setRenewed(rs.getInt("renewed"));
//                    SubPackageStatus stt = rs.getInt("status") == SubPackageStatus.Active.getValue() ? SubPackageStatus.Active
//                            : SubPackageStatus.Cancel;
                    SubPackageStatus stt = SubPackageStatus.newsub;
                    if (rs.getInt("status") == 1) {
                        stt = SubPackageStatus.Active;
                    };
                    if (rs.getInt("status") == 0) {
                        stt = SubPackageStatus.Cancel;
                    };
                    subInfo.setStatus(stt);
                    subInfo.setFreeMinutes(rs.getInt("free_minutes"));
                    subInfo.setExpireDay(rs.getInt("expire_day"));
                    subInfo.setExpired(subInfo.getExpireDay() <= 0 ? false : true);
                    subInfo.setSourceReg(rs.getString("source"));
                    subInfo.setRenewSuccess(rs.getInt("charge_success"));

                    boolean checkPromotion = false;
                    if (Config.proEnable) {
                        checkPromotion = true;

                        if (subInfo.getStatus() == SubPackageStatus.Active) {
                            checkPromotion = checkPromotionWhenActive;
                        }
                    }

                    if (checkPromotion) {
                        boolean dailyPromotion = (subInfo.getPackageId() == Config.billDailyPackageId) ? false
                                : true;
                        boolean weeklyPromotion = (subInfo.getPackageId() == Config.billWeeklyPackageId) ? false
                                : true;
                        boolean daily2000Promotion = (subInfo.getPackageId() == Config.billDaily2000PackageId) ? false
                                : true;
                        boolean weekly7000Promotion = (subInfo.getPackageId() == Config.billWeekly7000PackageId) ? false
                                : true;
                        boolean monthyPromotion = (subInfo.getPackageId() == Config.billMonthyPackageId) ? false
                                : true;

                        stmt_check = cnn.getConnection().prepareStatement(
                                sql_check);
                        stmt_check.setString(1, msisdn);
                        stmt_check.setInt(2, timeReSetPromoton);

                        rs_check = stmt_check.executeQuery();
                        while (rs_check.next()
                                && (dailyPromotion || weeklyPromotion || monthyPromotion)) {
                            /*
                             * Kiem tra cac goi cuoc gan day da hoac dang su
                             * dung
                             */
                            int packageId = rs_check.getInt("package_id");
                            if (packageId == Config.billDailyPackageId && (subInfo.getStatus().getValue() != 1)) {
                                dailyPromotion = false;
                            } else if (packageId == Config.billWeeklyPackageId && (subInfo.getStatus().getValue() != 1)) {
                                weeklyPromotion = false;
                            } else if (packageId == Config.billDaily2000PackageId && (subInfo.getStatus().getValue() != 1)) {
                                dailyPromotion = false;
                            } else if (packageId == Config.billWeekly7000PackageId && (subInfo.getStatus().getValue() != 1)) {
                                weeklyPromotion = false;
                            } else if (packageId == Config.billMonthyPackageId && (subInfo.getStatus().getValue() != 1)) {
                                monthyPromotion = false;
                            }
                        }

                        if (dailyPromotion && weeklyPromotion && monthyPromotion) {
                            subInfo.setFunPromotion(PackageFunPromotionStatus.PROMOTION_ALL);
                        } else if (dailyPromotion) {
                            subInfo.setFunPromotion(PackageFunPromotionStatus.PROMOTION_DAILY);
                        } else if (weeklyPromotion) {
                            subInfo.setFunPromotion(PackageFunPromotionStatus.PROMOTION_WEEKLY);
                        } else if (monthyPromotion) {
                            subInfo.setFunPromotion(PackageFunPromotionStatus.PROMOTION_MONTHY);
                        } else if (daily2000Promotion) {
                            subInfo.setFunPromotion(PackageFunPromotionStatus.PROMOTION_DAILY2000);
                        } else if (weekly7000Promotion) {
                            subInfo.setFunPromotion(PackageFunPromotionStatus.PROMOTION_WEEKLY7000);
                        } else {
                            subInfo.setFunPromotion(PackageFunPromotionStatus.NO_PROMOTION);
                        }

                        logger.debug("setPromotion: "
                                + subInfo.getPromotion().getValue());
                    }

                    subInfo.setErrorCode(BillingErrorCode.Success);
                } else {
                    subInfo.setErrorCode(BillingErrorCode.NotFoundData);

                    /*
                     * Chua dang ky dich vu => du dieu kien KM tat ca goi cuoc
                     */
                    if (Config.proEnable) {
                        subInfo.setFunPromotion(PackageFunPromotionStatus.PROMOTION_ALL);
                    }
                }
            }
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Exception in getLastSubPackage for " + msisdn, e);

            subInfo.setErrorCode(BillingErrorCode.SystemError);
        } finally {
            closeResultSet(rs);
            closeResultSet(rs_check);
            closeResultSet(rs_cnt);

            closeStatement(stmt);
            closeStatement(stmt_check);
            closeStatement(stmt_cnt);

            freeConnection(cnn);
        }

        return subInfo;
    }

    /**
     * Tao chu ki cuoc moi cho thue bao Return: Id chu ki cuoc hoac -1 neu bi
     * loi
     */
    public BillingErrorCode insertSubPackage(SubPackageInfo sub) {
        BillingErrorCode result = BillingErrorCode.SystemError;
        Integer subPackageId = 0;
        DbConnection cnn = null;
        PreparedStatement stmt_ins = null;
        PreparedStatement stmt_up = null;
        try {
            cnn = getConnection();
            String sql_up = "update sub_package set status = ?, updated_at = NOW() where msisdn = ? and status = ? ";

            String sql = "";
            if (sub.getRegAt() == null || sub.getRegAt().trim().equalsIgnoreCase("")) {
                sql = "insert into sub_package(msisdn, package_id, free_minutes, source, expire_at, "
                        + " status, charge_success, updated_at, reg_at, command, reg_type)  "
                        + " values(?, ?, ?, ?, ?, ?, ?, NOW(), NOW(),?,?)";
            } else {
                sql = "insert into sub_package(msisdn, package_id, free_minutes, source, reg_at, expire_at, "
                        + " status, charge_success, updated_at, command, reg_type)  "
                        + " values(?, ?, ?, ?, ?, ?, ?, ?, NOW(),?,?)";
            }

            stmt_up = cnn.getConnection().prepareStatement(sql_up);
            stmt_up.setInt(1, SubPackageStatus.Cancel.getValue());
            stmt_up.setString(2, sub.getMsisdn());
            stmt_up.setInt(3, SubPackageStatus.Active.getValue());
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
                stmt_ins.setString(8, sub.getCommand());
                stmt_ins.setInt(9, sub.getRegisterType());
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
            logger.error("Exception in insertSubPackage: " + sub.getMsisdn(), e);
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
        PreparedStatement stmt_acc = null;
        PreparedStatement stmt_get = null;
        ResultSet rs = null;
        String sql_up_sub = "update sub_package set status = ?, updated_at = NOW() where sub_package_id = ?";
        String sql_get_acc = "SELECT sub_package_id, msisdn, package_id, free_minutes, status, reg_at, "
                + " updated_at, expire_at, source, charge_success, expire_at < NOW() AS expired "
                + " FROM sub_package "
                + " WHERE msisdn = ? "
                + " ORDER BY updated_at DESC, sub_package_id DESC LIMIT 1";
        try {
            cnn = getConnection();
            stmt_get = cnn.getConnection().prepareStatement(sql_get_acc);
            stmt_get.setString(1, msisdn);
            rs = stmt_get.executeQuery();
            if (rs.next()) {
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

                if (subInfo.getStatus() == SubPackageStatus.Active) {
                    /*
                     * Huy goi cuoc dang active
                     */
                    stmt_sub = cnn.getConnection().prepareStatement(sql_up_sub);
                    stmt_sub.setInt(1, SubPackageStatus.Cancel.getValue());
                    stmt_sub.setInt(2, subInfo.getSubPackageId());
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
            logger.error("Exception in cancelSubPackage", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt_get);
            closeStatement(stmt_acc);
            closeStatement(stmt_sub);
            freeConnection(cnn);
        }

        return subInfo;
    }

    /**
     * Gia han chu ki goi cuoc
     *
     * @param subInfo
     * @return
     */
    public BillingErrorCode renewSubPackage(SubPackageInfo subInfo) {
        BillingErrorCode result = BillingErrorCode.SystemError;

        DbConnection cnn = null;
        PreparedStatement stmt_pre_up = null;
        PreparedStatement stmt_up = null;
        String sql_pre_up_sub = "UPDATE sub_package SET last_expire_day = DATEDIFF(NOW(),expire_at) WHERE sub_package_id = ? AND STATUS = ?";
        String sql_up_sub = "update sub_package set expire_at = ?, charge_success = ?, free_minutes = ? , updated_at = NOW(), renewed = 0 where sub_package_id = ? and status = ?";

        try {
            cnn = getConnection();
            // Pre
            stmt_pre_up = cnn.getConnection().prepareStatement(sql_pre_up_sub);
            stmt_pre_up.setInt(1, subInfo.getSubPackageId());
            stmt_pre_up.setInt(2, SubPackageStatus.Active.getValue());
            stmt_pre_up.executeUpdate();
            // 
            stmt_up = cnn.getConnection().prepareStatement(sql_up_sub);
            stmt_up.setString(1, subInfo.getExpireAt());
            stmt_up.setInt(2, subInfo.getRenewSuccess());
            stmt_up.setInt(3, subInfo.getFreeMinutes());
            stmt_up.setInt(4, subInfo.getSubPackageId());
            stmt_up.setInt(5, SubPackageStatus.Active.getValue());
            stmt_up.executeUpdate();
            cnn.getConnection().commit();
            result = BillingErrorCode.Success;
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error(
                    "Exception in renewSubPackage for "
                    + subInfo.getMsisdn(), e);
        } finally {
            closeStatement(stmt_pre_up);
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return result;
    }

    /**
     * update trang thai da quet gia han nhung ko thanh cong
     *
     * @param subInfo
     * @return
     */
    public BillingErrorCode updateRenewedInSubPackage(SubPackageInfo subInfo) {
        BillingErrorCode result = BillingErrorCode.SystemError;

        DbConnection cnn = null;
        PreparedStatement stmt_up = null;
        String sql_up_sub = "UPDATE `sub_package` SET `renewed`= 1 WHERE  `sub_package_id`=? ";

        try {
            cnn = getConnection();
            // 
            stmt_up = cnn.getConnection().prepareStatement(sql_up_sub);
            stmt_up.setInt(1, subInfo.getSubPackageId());
            stmt_up.executeUpdate();
            cnn.getConnection().commit();
            result = BillingErrorCode.Success;
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error(
                    "Exception in updateRenewedInSubPackage for "
                    + subInfo.getMsisdn(), e);
        } finally {
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return result;
    }

    /**
     * Gia han chu ki goi cuoc
     *
     * @param subInfo
     * @return
     */
    public BillingErrorCode reActiveSubPackage(SubPackageInfo subInfo) {
        BillingErrorCode result = BillingErrorCode.SystemError;

        DbConnection cnn = null;
        PreparedStatement stmt_up = null;
        PreparedStatement stmt = null;
        String sql = "update sub_package set status = ? where sub_package_id <> ? and status = ? and msisdn = ?";
        String sql_up_sub = "update sub_package set status = ?, source = ?, updated_at = NOW() where sub_package_id = ? and status = ?";

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, SubPackageStatus.Cancel.getValue());
            stmt.setInt(2, subInfo.getSubPackageId());
            stmt.setInt(3, SubPackageStatus.Active.getValue());
            stmt.setString(4, subInfo.getMsisdn());

            stmt.execute();
            stmt_up = cnn.getConnection().prepareStatement(sql_up_sub);
            stmt_up.setInt(1, SubPackageStatus.Active.getValue());
            stmt_up.setString(2, subInfo.getSourceReg());
            stmt_up.setInt(3, subInfo.getSubPackageId());
            stmt_up.setInt(4, SubPackageStatus.Cancel.getValue());
            stmt_up.executeUpdate();
            cnn.getConnection().commit();
            result = BillingErrorCode.Success;
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error(
                    "Exception in reActiveSubPackage for "
                    + subInfo.getMsisdn(), e);
        } finally {
            closeStatement(stmt_up);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return result;
    }

    /**
     * Insert log giao dich billing
     *
     * @param msisdn
     * @param requestId
     * @param packageId
     * @param subPackageId
     * @param billingType : 1 - Dang ky goi cuoc; 2 - Huy goi cuoc; 3 - Gia han
     * goi cuoc; 4 - Tin MO
     * @param amount
     * @param chargeResult
     * @param source : IVR, SMS, CLIENT, WAP
     * @param desc : Mo ta them hoac chua tin MO
     * @param promotion : 1 - Dang ky khuyen mai; 0: khong khuyen mai
     * @return
     */
    public BillingErrorCode insertBillingActivity(BillingActivityInfo actInfo) {
        BillingErrorCode result = BillingErrorCode.SystemError;
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        String sql = "insert into billing_activity(request_id, msisdn, package_id, sub_package_id, "
                + " billing_type, amount, result, source, description, promotion, billing_at) "
                + " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, actInfo.getRequestId());
            stmt.setString(2, SMSHelper.formatMobileNumber(actInfo.getMsisdn()));
            stmt.setInt(3, actInfo.getPackageId());
            stmt.setInt(4, actInfo.getSubPackageId());
            stmt.setInt(5, actInfo.getBillingType());
            stmt.setInt(6, actInfo.getAmount());
            stmt.setInt(7, NumberUtils.toInt(actInfo.getResult()));
            stmt.setString(8, actInfo.getSource());
            stmt.setString(9, actInfo.getDescription());
            stmt.setInt(10, actInfo.getPromotion());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                actInfo.setBillingActivityId(rs.getInt(1));
                cnn.getConnection().commit();
                result = BillingErrorCode.Success;
            } else {
                rollbackTransaction(cnn);
            }

        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("Exception in insertBillingActivity", e);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return result;
    }

    public ArrayList<SubPackageInfo> getListSubExpired(int limit,
            int lastSubPackageId) {
        ArrayList<SubPackageInfo> subList = new ArrayList<SubPackageInfo>();
        DbConnection cnn = null;
        String sql = "SELECT s.sub_package_id, s.msisdn, s.package_id, s.free_minutes, s.status, s.reg_at, "
                + " s.updated_at, s.expire_at, s.source, s.charge_success"
                + " FROM sub_package s, package p "
                + " WHERE s.package_id = p.package_id "
                + "	AND p.allow_renew = ? "
                + "	AND s.expire_at < NOW() "
                + "	AND s.status = ? "
                + "	AND (? = 0 OR s.sub_package_id < ?) "
                + " ORDER BY s.sub_package_id DESC " + " LIMIT ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, PackageInfo.RENEW_ALLOW);
            stmt.setInt(2, SubPackageStatus.Active.getValue());
            stmt.setInt(3, lastSubPackageId);
            stmt.setInt(4, lastSubPackageId);
            stmt.setInt(5, limit);
            rs = stmt.executeQuery();

            while (rs.next()) {
                SubPackageInfo subInfo = new SubPackageInfo();
                subInfo.setSubPackageId(rs.getInt("sub_package_id"));
                subInfo.setMsisdn(rs.getString("msisdn"));
                subInfo.setPackageId(rs.getInt("package_id"));
                subInfo.setUpdatedAt(dateFormat.format(rs.getTimestamp("updated_at")));
                subInfo.setRegAt(dateFormat.format(rs.getTimestamp("reg_at")));
                subInfo.setExpireAt(dateFormat.format(rs.getTimestamp("expire_at")));
                subInfo.setStatus(SubPackageStatus.Active);
                subInfo.setFreeMinutes(rs.getInt("free_minutes"));
                subInfo.setExpired(true);
                subInfo.setSourceReg(rs.getString("source"));
                subInfo.setRenewSuccess(rs.getInt("charge_success"));

                subList.add(subInfo);
            }
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in getListSubExpired", e);

        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return subList;
    }

    public int lockConfig(String configName, String configVal, String groupName) {
        int result = 0;
        DbConnection cnn = null;
        String sql = "update configuration set val = ?, status = ? where config_name = ? and group_name = ? and status = ?";
        PreparedStatement stmt_up = null;
        PreparedStatement stmt_sel = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            stmt_up = cnn.getConnection().prepareStatement(sql);
            stmt_up.setString(1, configVal);
            stmt_up.setInt(2, Constants.STATUS_ACTIVE);
            stmt_up.setString(3, configName);
            stmt_up.setString(4, groupName);
            stmt_up.setInt(5, Constants.STATUS_LOCK);

            result = stmt_up.executeUpdate();
            if (result <= 0) {
                sql = "select count(configuration_id) as cnt from configuration where config_name = ? and val = ? and group_name = ? and status = ?";
                stmt_sel = cnn.getConnection().prepareStatement(sql);
                stmt_sel.setString(1, configName);
                stmt_sel.setString(2, configVal);
                stmt_sel.setString(3, groupName);
                stmt_sel.setInt(4, Constants.STATUS_ACTIVE);
                rs = stmt_sel.executeQuery();

                if (rs.next()) {
                    result = rs.getInt("cnt");
                }
            }

            cnn.getConnection().commit();
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("Exception in lockConfig", e);
            result = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt_up);
            closeStatement(stmt_sel);
            freeConnection(cnn);
        }

        return result;
    }

    public int unlockConfig(String configName, String configVal,
            String groupName) {
        int result = 0;
        DbConnection cnn = null;
        String sql = "update configuration set val = ?, status = ? where config_name = ? and group_name = ? and status = ?";
        PreparedStatement stmt = null;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, configVal);
            stmt.setInt(2, Constants.STATUS_LOCK);
            stmt.setString(3, configName);
            stmt.setString(4, groupName);
            stmt.setInt(5, Constants.STATUS_ACTIVE);

            result = stmt.executeUpdate();
            cnn.getConnection().commit();
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("Exception in unlockConfig", e);
            result = -1;
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return result;
    }

    /**
     * Luu cac loai chi so CCU
     *
     * @param ccuList
     * @return
     */
    public BillingErrorCode insertCCU(ArrayList<CCUInfo> ccuList) {
        BillingErrorCode result = BillingErrorCode.SystemError;
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        try {
            boolean logFile = false;
            if (Config.logccu < 2) {
                cnn = getConnection();
                String sql = "insert into ccu(total_ccu, ccu_type, package_id, `key`, created_at) values(?, ?, ?, ?, NOW())";
                stmt = cnn.getConnection().prepareStatement(sql);
                for (int i = 0; i < ccuList.size(); i++) {
                    stmt.setInt(1, ccuList.get(i).getCcu_total());
                    stmt.setInt(2, ccuList.get(i).getCcu_type());
                    stmt.setInt(3, ccuList.get(i).getPackage_id());
                    stmt.setInt(4, ccuList.get(i).getKey());
                    stmt.executeUpdate();
                }
                cnn.getConnection().commit();
                if (Config.logccu == 0) {
                    logFile = true;
                }
            }
            if (Config.logccu == 2 || logFile) {
                for (int i = 0; i < ccuList.size(); i++) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date now = new Date();
                    LogFileStack.logCCU(sdf.format(now), ccuList.get(i).getCcu_total(), ccuList.get(i).getCcu_type(), ccuList.get(i).getPackage_id(), ccuList.get(i).getKey());
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

    /**
     * Lock giao dich charging khi dang xu ly, ngan chan tru lap
     *
     * @param msisdn
     * @param chargeType
     * @param expireMinutes
     * @return
     */
    public BillingErrorCode insertChargeLocking(String msisdn,
            Integer chargeType, Integer expireMinutes) {
        BillingErrorCode result = BillingErrorCode.SystemError;
        msisdn = SMSHelper.formatMobileNumber(msisdn);
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
            stmt_del.setInt(3, expireMinutes);
            stmt_del.executeUpdate();

            stmt_ins = cnn.getConnection().prepareStatement(sql_ins);
            stmt_ins.setString(1, msisdn);
            stmt_ins.setInt(2, chargeType);
            int count = stmt_ins.executeUpdate();
            cnn.getConnection().commit();

            if (count > 0) {
                result = BillingErrorCode.Success;
            } else {
                result = BillingErrorCode.ChargingSubProcessing;
            }
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("Exception in insertChargeLocking for " + msisdn, e);
        } finally {
            closeStatement(stmt_ins);
            closeStatement(stmt_del);
            freeConnection(cnn);
        }

        return result;
    }

    /**
     * Locking giao dich gia han cua nhieu thue bao
     *
     * @param subs
     * @param chargeType
     * @param expireMinutes
     */
    public void insertMultiChargeLocking(ArrayList<SubPackageInfo> subs,
            Integer chargeType, Integer expireMinutes) {
        DbConnection cnn = null;
        PreparedStatement stmt_del = null;
        PreparedStatement stmt_ins = null;
        String sql_del = "delete from charge_locking where msisdn = ? and charge_type = ? and TIMESTAMPDIFF(MINUTE, created_at, NOW()) >= ?";
        String sql_ins = "insert ignore into charge_locking(msisdn, charge_type, created_at) values(?, ?, NOW())";

        try {
            cnn = getConnection();
            stmt_del = cnn.getConnection().prepareStatement(sql_del);
            stmt_ins = cnn.getConnection().prepareStatement(sql_ins);

            int i = 0;
            while (i < subs.size()) {
                final SubPackageInfo sub = subs.get(i);
                try {
                    stmt_del.setString(1, SMSHelper.formatMobileNumber(sub.getMsisdn()));
                    stmt_del.setInt(2, chargeType);
                    stmt_del.setInt(3, expireMinutes);
                    stmt_del.executeUpdate();

                    stmt_ins.setString(1, sub.getMsisdn());
                    stmt_ins.setInt(2, chargeType);
                    int count = stmt_ins.executeUpdate();
                    cnn.getConnection().commit();

                    if (count > 0) {
                        i++;
                    } else {
                        rollbackTransaction(cnn);
                        subs.remove(i);
                        continue;
                    }
                } catch (Exception e) {
                    rollbackTransaction(cnn);
                    subs.remove(i);
                    continue;
                }
            }

        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("Exception in insertMultiChargeLocking", e);
        } finally {
            closeStatement(stmt_ins);
            closeStatement(stmt_del);
            freeConnection(cnn);
        }
    }

    /**
     * Xoa lock giao dich charging
     *
     * @param msisdn
     * @param chargeType
     * @return
     */
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
            stmt_del.executeUpdate();
            cnn.getConnection().commit();
            result = BillingErrorCode.Success;
        } catch (Exception e) {
            rollbackTransaction(cnn);
            logger.error("Exception in deleteChargeLocking for " + msisdn, e);
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
            if (Config.logivr < 2) {
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
                stmt.executeUpdate();
                cnn.getConnection().commit();
                if (Config.logivr == 0) {
                    logFile = true;
                }
            }
            if (Config.logivr == 2 || logFile) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                LogFileStack.logDTMF(sdf.format(begin_at), sdf.format(end_at), msisdn, dtmf);
            }
            resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
        } catch (Exception e) {

            logger.error("Error in insert Log Dtmf", e.getCause());
            e.printStackTrace();
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> insertAllSubListen(int sub_package_id, int package_id, String msisdn, String contentListen, int daily_call_id) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        try {
            cnn = getConnection();
            String[] content = contentListen.split("#");
            if (content.length > 0) {
                boolean logFile = false;
                if (Config.loglisten < 2) {
                    String sql = "INSERT INTO sub_listen (sub_package_id,package_id,msisdn,channel_id,content_id,duration,channel_ord,topic_ord,daily_call_id,created_at)"
                            + " VALUES (?,?,?,?,?,?,?,?,?,now())";
                    stmt = cnn.getConnection().prepareStatement(sql);
                    for (int j = 0; j < content.length; j++) {
                        String[] info = content[j].split("-");
                        if (info.length >= 6) {
                            int topic_ord = Integer.parseInt(info[0]);
                            int channel_ord = Integer.parseInt(info[1]);
                            int channel_id = Integer.parseInt(info[2]);
                            int content_id = Integer.parseInt(info[3]);
                            long start = Long.parseLong(info[4]);
                            long end = Long.parseLong(info[5]);
                            int duration = (int) (end - start);
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
                            stmt.addBatch();
                        } else {
                            logger.debug("Tham so contentListen k du du lieu de insert");
                        }
                    }
                    if (stmt != null) {
                        stmt.executeBatch();
                    }
                    cnn.getConnection().commit();
                    if (Config.loglisten == 0) {
                        logFile = true;
                    }
                }
                if (Config.loglisten == 2 || logFile) {
                    for (int j = 0; j < content.length; j++) {
                        String[] info = content[j].split("-");
                        if (info.length >= 6) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date now = new Date();
                            String created_at = sdf.format(now);
                            int topic_ord = Integer.parseInt(info[0]);
                            String topic_name = getListTopics().get(topic_ord);
                            int channel_ord = Integer.parseInt(info[1]);
                            int channel_id = Integer.parseInt(info[2]);
                            String channel_name = getListChannels().get(channel_id);
                            int content_id = Integer.parseInt(info[3]);
                            String content_name = getListContents().get(content_id);
                            long start = Long.parseLong(info[4]);
                            long end = Long.parseLong(info[5]);
                            int duration = (int) (end - start);
                            LogFileStack.logSubListen(created_at, daily_call_id, sub_package_id, package_id, msisdn, topic_name, channel_name, content_id, content_name, duration, topic_ord, channel_ord, channel_id);
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

    // Get list topics
    public Hashtable<Integer, String> getListTopics() {
        String key = "alofun_getListTopics";
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
            String sql = "select topic_ord ord, topic_name name from content_topic order by topic_ord";
            stmt = cnn.getConnection().prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("ord");
                String name = rs.getString("name");
                listTopics.put(ord, name);
            }
            if (!listTopics.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "listTopics", Config.redis_cache_timeout_log);
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
        String key = "alofun_getListChannels";
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
            String sql = "select channel_id ord, channel_name name from content_channel order by channel_ord";
            stmt = cnn.getConnection().prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("ord");
                String name = rs.getString("name");
                listChannels.put(ord, name);
            }
            if (!listChannels.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "listChannels", Config.redis_cache_timeout_log);
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

    // Get list content
    public Hashtable<Integer, String> getListContents() {
        String key = "alofun_getListContents";
        String data = RestfulStack.getFromCache(key);
        boolean isCache = false;
        if (data != null) {
            isCache = true;
            if (!listContents.isEmpty()) {
                logger.debug("getListContents from cache");
                return listContents;
            }
        }
        logger.debug("getListContents from Database");
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            String sql = "select content_id ord, content_name name from content "
                    + "where status = 1 order by content_id";
            stmt = cnn.getConnection().prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                int ord = rs.getInt("ord");
                String name = rs.getString("name");
                listContents.put(ord, name);
            }
            if (!listContents.isEmpty() && !isCache) {
                RestfulStack.pushToCacheWithExpiredTime(key,
                        "listContents", Config.redis_cache_timeout_log);
            }
        } catch (Exception e) {
            logger.error("Error in getListContents", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listContents;
    }

    public HashMap<String, Object> getListContent(String msisdn,
            int topic_ord, int channel_ord) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        logger.debug("getListContent from Database");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        ResultSet re2 = null;
        PreparedStatement stmt3 = null;
        ResultSet re3 = null;
        try {
            cnn = getConnection();
            String sql = "Select topic_id from content_topic where topic_ord = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, topic_ord);
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                int topic_id = re1.getInt("topic_id");
                sql = "Select channel_id, type from content_channel "
                        + "where topic_id = ? "
                        + "and channel_ord = ? "
                        + "and status = 1";
                stmt2 = cnn.getConnection().prepareStatement(sql);
                stmt2.setInt(1, topic_id);
                stmt2.setInt(2, channel_ord);
                re2 = stmt2.executeQuery();
                if (re2.next()) {
                    int channel_id = re2.getInt("channel_id");
                    int type = re2.getInt("type");
                    resp.put("channel_id", channel_id);
                    resp.put("type", type);
                    sql = "Select a.content_id,a.content_path from content a, content_channel b "
                            + "where "
                            + "(a.channel_id = ? and a.channel_id = b.channel_id and a.status = 1 and b.day_limit <> 0 and a.publish_at <= now() and (DATE_ADD(a.publish_at,INTERVAL b.day_limit DAY) >= now())) "
                            + "or "
                            + "(a.channel_id = ? and a.channel_id = b.channel_id and a.status = 1 and b.day_limit = 0 and a.publish_at <= now() )"
                            + "order by content_ord desc, publish_at desc limit 0," + Config.contentMax;
                    stmt3 = cnn.getConnection().prepareStatement(sql);
                    stmt3.setInt(1, channel_id);
                    stmt3.setInt(2, channel_id);
                    re3 = stmt3.executeQuery();
                    ArrayList<Content> data = new ArrayList<Content>();
                    while (re3.next()) {
                        String contentId = re3.getString("content_id");
                        String contentPath = re3.getString("content_path");
                        Content a = new Content();
                        a.setContentId(contentId);
                        a.setContentPath(contentPath);
                        data.add(a);
                    }
                    if (data.size() > 0) {
                        resp.put("data", data);
                        resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                    } else {
                        resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                    }
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in getListContent", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeResultSet(re2);
            closeResultSet(re3);
            closeStatement(stmt1);
            closeStatement(stmt2);
            closeStatement(stmt3);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getPlaylist(String msisdn) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet re = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id, b.content_id,b.content_path from sub_playlist a, content b "
                    + "where a.content_id = b.content_id "
                    + "and b.status = 1 "
                    + "and a.msisdn = ? "
                    + "order by a.created_at desc";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            re = stmt.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            int total = 0;
            while (re.next()) {
                total++;
                if (total == 1) {
                    int channel_id = re.getInt("channel_id");
                    resp.put("channel_id", channel_id);
                }
                String contentId = re.getString("content_id");
                String contentPath = re.getString("content_path");
                Content a = new Content();
                a.setContentId(contentId);
                a.setContentPath(contentPath);
                data.add(a);
            }
            if (total > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in getListContent", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> checkLiveShow() {
        logger.debug("checkliveshow from database");
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        try {
            cnn = getConnection();
            String sql = "select a.id liveshow_id, a.link_channel link_channel,a.path_intro path_intro, TIME_TO_SEC(TIMEDIFF(a.end_time, now())) time_stream from channel_live a where a.`status` =1 and (a.start_time < now()) and (now() < a.end_time )";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            re1 = stmt1.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            int total = 0;
            while (re1.next()) {
                total++;
                String link_channel = re1.getString("link_channel");
                String liveshow_id = re1.getString("liveshow_id");
                String time_stream = re1.getString("time_stream");
                String path_intro = re1.getString("path_intro");
                Content a = new Content();
                a.setLink_channel(link_channel);
                a.setLiveshow_id(liveshow_id);
                a.setTime_stream(time_stream);
                a.setPath_intro(path_intro);
                data.add(a);
            }
            logger.info("total liveshow: " + total);
            if (total > 0) {
                resp.put("total", total);
                resp.put("data", data);
                if (total == 1) {
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS_ONCE_DATA);
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS_MORE_DATA);
                }
            }
            if (total == 0) {
                sql = "select a.id calendar_id, a.path path_calendar from channel_calendar a where a.`status` =1 and (a.start_time < now()) and (now() < a.end_time) order by a.ord, a.end_time";
                stmt2 = cnn.getConnection().prepareStatement(sql);
                re1 = stmt2.executeQuery();
                total = 0;
                while (re1.next()) {
                    total++;
                    String path_calendar = re1.getString("path_calendar");
                    String calendar_id = re1.getString("calendar_id");
                    Content a = new Content();
                    a.setPath_calendar(path_calendar);
                    a.setCalendar_id(calendar_id);
                    data.add(a);
                }
                logger.info("total calendar: " + total);
                if (total > 0) {
                    resp.put("total", total);
                    resp.put("data", data);
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS_MORE_CALENDAR);
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
                }
            }
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in updatePlaylist", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> updatePlaylist(String msisdn, String contentId, String action) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        PreparedStatement stmt2 = null;
        try {
            cnn = getConnection();
            String sql = "select content_id from sub_playlist where msisdn = ? and content_id = ?";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, contentId);
            re1 = stmt1.executeQuery();
            if (re1.next()) {
                if (action.equals("add")) {
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                } else if (action.equals("delete")) {
                    sql = "delete from sub_playlist where msisdn = ? and content_id = ?";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, msisdn);
                    stmt2.setString(2, contentId);
                    stmt2.execute();
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.WRONG_PARAM);
                }
            } else {
                if (action.equals("add")) {
                    sql = "insert into sub_playlist(msisdn,content_id,created_at) values(?,?,now())";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setString(1, msisdn);
                    stmt2.setString(2, contentId);
                    stmt2.execute();
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                } else if (action.equals("delete")) {
                    resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
                } else {
                    resp.put(Constants.ERROR_CODE, Constants.WRONG_PARAM);
                }
            }
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in updatePlaylist", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            closeStatement(stmt2);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> insertRecordRequirement(String msisdn, String fileName) {
        int result = 0;
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re = null;
        try {
            cnn = getConnection();
            String sql = "INSERT INTO `question` ( `msisdn`, `question_path`, `status`, `created_at`, `update_at`) VALUES ( ?, ?, 0, now() , now());";
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setString(2, fileName);
            stmt1.execute();
            cnn.getConnection().commit();
            resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
        } catch (Exception e) {
            logger.error("Error in updatePlaylist", e);
            resp.put(Constants.ERROR_CODE, Constants.SYSTEM_ERROR);
        } finally {
            closeResultSet(re);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        return resp;
    }

    public HashMap<String, Object> getListAnswer(String msisdn) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet re = null;
        try {
            cnn = getConnection();
            String sql = "select a.answerID, a.path answerPath from answer a where a.msisdn = ? and a.`status` = 1";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            re = stmt.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            while (re.next()) {
                Content a = new Content();
                String answerID = re.getString("answerID");
                String answerPath = re.getString("answerPath");
                a.setAnswerID(answerID);
                a.setAnswerPath(answerPath);
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
            cnn.getConnection().commit();
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

    public HashMap<String, Object> getListChannel(int topic_ord) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet re = null;
        try {
            cnn = getConnection();
            String sql = "Select b.channel_id, b.channel_ord, b.name_path from content_topic a, content_channel b "
                    + "where "
                    + "a.topic_id = b.topic_id and b.status = 1 and a.topic_ord = ? "
                    + "order by b.channel_ord";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, topic_ord);
            re = stmt.executeQuery();
            ArrayList<Content> data = new ArrayList<Content>();
            while (re.next()) {
                Content a = new Content();
                String channelId = re.getString("channel_id");
                String channelOrd = re.getString("channel_ord");
                String namePath = re.getString("name_path");
                a.setChannelId(channelId);
                a.setChannelOrd(channelOrd);
                a.setNamePath(namePath);
                data.add(a);
            }
            if (data.size() > 0) {
                resp.put("data", data);
                resp.put(Constants.ERROR_CODE, Constants.SUCCESS);
            } else {
                resp.put(Constants.ERROR_CODE, Constants.NO_DATA_FOUND);
            }
            cnn.getConnection().commit();
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

    public ListenHistory getListenHistory(String msisdn, int channelId, int channel_type) {
        ListenHistory listenHistory = null;
        String key = "getListenHistory_" + msisdn + "_" + channelId + "_" + channel_type;
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
                + "and channel_id = ? and channel_type = ?";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            stmt.setInt(2, channelId);
            stmt.setInt(3, channel_type);
            listenHistory = new ListenHistory();
            listenHistory.setMsisdn(msisdn);
            listenHistory.setChannelId(channelId);
            listenHistory.setChannelType(channel_type);
            rs = stmt.executeQuery();
            if (rs.next()) {
                listenHistory.setContentListened(rs.getString("content_list"));
            }
            cnn.getConnection().commit();
            RestfulStack.pushToCacheWithExpiredTime(key,
                    gson.toJson(listenHistory), Config.redis_cache_timeout);
        } catch (Exception e) {
            logger.error("error in getListenHistory in database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listenHistory;
    }

    public int insertListenHistory(ListenHistory listenHis, int channel_type) {
        int result = 0;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "insert into access_history(msisdn,channel_id,channel_type,content_list,created_at) values(?,?,?,?,now())";
        /*
         * Limit length
         */
        while (listenHis.getContentListened() != null
                && listenHis.getContentListened().length() >= Config.listenHistoryMaxLen) {
            int lastIndexOfSep = listenHis.getContentListened().lastIndexOf(
                    ListenHistory.SEPERATOR,
                    listenHis.getContentListened().length() - 2);
            listenHis.setContentListened(listenHis.getContentListened().substring(0, lastIndexOfSep + 1));
        }
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, SMSHelper.formatMobileNumber(listenHis.getMsisdn()));
            stmt.setInt(2, listenHis.getChannelId());
            stmt.setInt(3, channel_type);
            stmt.setString(4, listenHis.getContentListened());
            result = stmt.executeUpdate();
            cnn.getConnection().commit();
            updateCacheListenHistory(listenHis, channel_type);
        } catch (Exception e) {
            logger.error("error in insertListenHistory in database", e);
            result = -1;
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return result;
    }

    private void updateCacheListenHistory(ListenHistory listenHis,
            int channelType) {
        String key = "getListenHistory_" + listenHis.getMsisdn() + "_"
                + listenHis.getChannelId() + "_" + channelType;
        RestfulStack.pushToCacheWithExpiredTime(key, gson.toJson(listenHis),
                Config.redis_cache_timeout);
    }

    public int updateListenHistory(ListenHistory listenHis, int channel_type) {
        int result = 0;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "update access_history set content_list = ? "
                + "where msisdn = ? and channel_id = ? "
                + "and channel_type = ?";
        /*
         * Limit length
         */
        while (listenHis.getContentListened() != null
                && listenHis.getContentListened().length() >= Config.listenHistoryMaxLen) {
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
            result = stmt.executeUpdate();
            cnn.getConnection().commit();
            updateCacheListenHistory(listenHis, channel_type);
        } catch (Exception e) {
            logger.error("error in updateListenHistory in database", e);
            result = -1;
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return result;
    }

    public ArrayList<PromtInTimeRange> getListPromtInTimeRange() {
        ArrayList<PromtInTimeRange> promts = new ArrayList<PromtInTimeRange>();
        String key = "ListPromtInRange";
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
                cnn.getConnection().commit();
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
    // SMS
    private Hashtable<String, String> smsRules = new Hashtable<String, String>();

    public void refreshSMSRules() {
        synchronized (lock) {
            smsRules.clear();
            getSMSRules();
        }
    }

    public Hashtable<String, String> getSMSRules() {
        if (smsRules.size() == 0) {
            synchronized (lock) {
                if (smsRules.size() == 0) {
                    DbConnection cnn = null;
                    String sql = "select config_name, val from configuration where status = 1 and lower(group_name) = 'sms' ";
                    logger.debug(sql);
                    PreparedStatement stmt = null;
                    ResultSet rs = null;
                    try {
                        cnn = getConnection();
                        stmt = cnn.getConnection().prepareStatement(sql);
                        rs = stmt.executeQuery();
                        while (rs.next()) {
                            String name = rs.getString("config_name");
                            String val = rs.getString("val");
                            logger.info("Load: " + name + "|" + val);
                            smsRules.put(name, val);
                        }
                        cnn.getConnection().commit();
                    } catch (Exception e) {
                        logger.error("Error in getSMSRules", e);
                    } finally {
                        closeResultSet(rs);
                        closeStatement(stmt);
                        freeConnection(cnn);
                    }
                }
            }
        }
        return smsRules;
    }

    public int insertMO(String msisdn, String service_number, String content, int status) {
        DbConnection cnn = null;
        msisdn = SMSHelper.formatMobileNumber(msisdn);
        int resp = 0;
        String sql = "insert into mo(msisdn, content, service_number, status, received_at) values (?, ?, ?, ?, NOW())";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, msisdn);
            stmt.setString(2, content);
            stmt.setString(3, service_number);
            stmt.setInt(4, status);
            stmt.execute();

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                resp = rs.getInt(1);
            }
            rs.close();
            stmt.close();

            if (resp > 0) {
                cnn.getConnection().commit();
            } else {
                rollbackTransaction(cnn);
            }
        } catch (Exception ex) {
            logger.error("error in insertMO in database", ex);
            rollbackTransaction(cnn);
            resp = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return resp;
    }

    public int insertMT(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMtContent();
        String service_number = sms.getServiceNumber();
        int mo_id = sms.getMoId();
        int status = sms.getStatus();
        int type = sms.getType();
        sms.setMtSentTime(new Timestamp(System.currentTimeMillis()));
        DbConnection cnn = null;
        int resp = 0;
        logger.info("co inset tin nhan vao db:" + content);
        msisdn = SMSHelper.formatMobileNumber(msisdn);
        String sql = "insert into mt(msisdn, content, service_number, mo_id, status, type, sent_at) "
                + "values (?, ?, ?, ?, ?, ?, NOW())";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            boolean logFile = false;
            if (Config.logmomt < 2) {
                cnn = getConnection();
                stmt = cnn.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, msisdn);
                stmt.setString(2, content);
                stmt.setString(3, service_number);
                stmt.setInt(4, mo_id);
                stmt.setInt(5, status);
                stmt.setInt(6, type);
                stmt.execute();

                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    resp = rs.getInt(1);
                }
                rs.close();
                stmt.close();

                if (resp > 0) {
                    cnn.getConnection().commit();
                } else {
                    rollbackTransaction(cnn);
                }
                if (Config.logmomt == 0) {
                    logFile = true;
                }
            }

            if (Config.logmomt == 2 || logFile) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (sms.isHaveMO()) {
                    LogFileStack.logSMS(sdf.format(sms.getMoReceivedTime()), sdf.format(sms.getMtSentTime()), sms.getMsisdn(), sms.getMoContent(), sms.getMtContent(), sms.getServiceNumber());
                } else {
                    LogFileStack.logSMS("", sdf.format(sms.getMtSentTime()), sms.getMsisdn(), "", sms.getMtContent(), sms.getServiceNumber());
                }
            }
        } catch (Exception ex) {
            logger.error("error in insertMT in database", ex);
            rollbackTransaction(cnn);
            resp = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return resp;
    }

    public ArrayList<MTRemindInfo> getListMTRemindWaiting(int limit, int lastRemindKey) {
        ArrayList<MTRemindInfo> list = new ArrayList<MTRemindInfo>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet re = null;
        try {
            cnn = getConnection();
            String sql = "select * from mt_remind where status = ? and (? = 0 or mt_remind_key > ?) order by mt_remind_key limit ?";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, MTRemindInfo.STATUS_NOT_SEND);
            stmt.setInt(2, lastRemindKey);
            stmt.setInt(3, lastRemindKey);
            stmt.setInt(4, limit);
            re = stmt.executeQuery();

            while (re.next()) {
                MTRemindInfo mt = new MTRemindInfo();
                mt.setMtRemindKey(re.getInt("mt_remind_key"));
                mt.setMsisdn(re.getString("msisdn"));
                mt.setMtContent(re.getString("mt_content"));
                mt.setSubPackageId(re.getInt("sub_package_id"));
                list.add(mt);
            }

        } catch (Exception e) {
            logger.error("Error in getListMTRemindWaiting", e);
        } finally {
            closeResultSet(re);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public int updateStatusSendMTRemind(MTRemindInfo mt) {
        int result = 0;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "update mt_remind set status = ?, sent_at = now() where mt_remind_key = ?";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, mt.getStatus());
            stmt.setInt(2, mt.getMtRemindKey());

            result = stmt.executeUpdate();
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("error in updateStatusSendMTRemind in database", e);
            result = -1;
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return result;
    }

    public SubPackageInfo getSubPackage(String msisdn) {
        logger.info("getSubPackage  funtion ");
        SubPackageInfo subInfo = new SubPackageInfo();
        subInfo.setErrorCode(BillingErrorCode.SystemError);
        subInfo.setMsisdn(msisdn);

        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String sql = "SELECT sub_package_id, msisdn, package_id, free_minutes, status, reg_at, "
                + " updated_at, expire_at, source, charge_success, DATEDIFF(NOW(), expire_at) AS expire_day "
                + " FROM sub_package "
                + " WHERE msisdn = ? "
                + " AND status = 1"
                + " ORDER BY updated_at DESC, sub_package_id DESC LIMIT 1";

        try {
            cnn = getConnection();

            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            logger.info(sql);
            rs = stmt.executeQuery();
            if (rs.next()) {
                subInfo.setSubPackageId(rs.getInt("sub_package_id"));
                subInfo.setMsisdn(rs.getString("msisdn"));
                subInfo.setPackageId(rs.getInt("package_id"));
                subInfo.setUpdatedAt(dateFormat.format(rs.getTimestamp("updated_at")));
                subInfo.setRegAt(dateFormat.format(rs.getTimestamp("reg_at")));
                subInfo.setExpireAt(dateFormat.format(rs.getTimestamp("expire_at")));
//                    SubPackageStatus stt = rs.getInt("status") == SubPackageStatus.Active.getValue() ? SubPackageStatus.Active
//                            : SubPackageStatus.Cancel;
                SubPackageStatus stt = SubPackageStatus.newsub;
                if (rs.getInt("status") == 1) {
                    stt = SubPackageStatus.Active;
                };
                if (rs.getInt("status") == 0) {
                    stt = SubPackageStatus.Cancel;
                };
                subInfo.setStatus(stt);
                subInfo.setFreeMinutes(rs.getInt("free_minutes"));
                subInfo.setExpireDay(rs.getInt("expire_day"));
                subInfo.setExpired(subInfo.getExpireDay() <= 0 ? false : true);
                subInfo.setSourceReg(rs.getString("source"));
                subInfo.setRenewSuccess(rs.getInt("charge_success"));
                subInfo.setErrorCode(BillingErrorCode.Success);
            } else {
                subInfo.setErrorCode(BillingErrorCode.NotFoundData);
            }

            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Exception in getLastSubPackage for " + msisdn, e);

            subInfo.setErrorCode(BillingErrorCode.SystemError);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return subInfo;
    }

    public int getPackageIdForVoiceBroadCast() {
        String key = "alofun.getPackageIdForVoiceBroadCast";
        logger.info("getPackageIdForVoiceBroadCast with key = " + key);
        Element data = cache.get(key);
        if (data != null) {
            logger.info("cache.get(key)= " + data);
            logger.debug("getListPromtInTimeRange from cache");
            return Integer.parseInt(String.valueOf(data.getObjectValue()));
        }
        int packageId = 0;
        synchronized (lock) {
            DbConnection cnn = null;
            String sql = "select val from configuration where status = 1 and lower(group_name) = 'voicebroadcast' and lower(config_name) = 'package_id'";
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
                if (rs.next()) {
                    packageId = rs.getInt("val");
                }
                if (packageId > 0) {
                    cache.put(new Element(key, packageId));
                }
                cnn.getConnection().commit();
            } catch (Exception e) {
                logger.error("Error in getPackageIdForVoiceBroadCast", e);
            } finally {
                closeResultSet(rs);
                closeStatement(stmt);
                freeConnection(cnn);
            }
        }
        return packageId;
    }
}
