package com.vega.service.api.db;

import com.vega.service.api.common.AwardExchangeResult;
import com.vega.service.api.common.AwardInfo;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.Helper;
import com.vega.service.api.common.SubPointAction;
import com.vega.service.api.common.SubPointCheckingInfo;
import com.vega.service.api.common.SubProfileInfo;
import com.vega.service.api.object.BillingErrorCode;
import com.vega.service.api.object.SubPackageInfo;
import com.vega.vcs.service.database.pool.DbConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.NamingException;
import net.sf.ehcache.Element;

/**
 * @author SP1/ManND
 *
 */
public class GamificationDAO extends DBConnections {

    Object lock = new Object();

    public GamificationDAO() throws NamingException {
        super.start();
    }

    @SuppressWarnings("unchecked")
    public ConcurrentHashMap<String, AwardInfo> getListAward() {
        logger.info(">>>>> Game Dao  getListAward");
        ConcurrentHashMap<String, AwardInfo> val = null;
        String key = "GetListAward";
        Element data = cache.get(key);
        if (data != null) {
            logger.debug("get from cache");
            val = (ConcurrentHashMap<String, AwardInfo>) data.getObjectValue();
            if (val != null && val.size() > 0) {
                return val;
            }
        }

        synchronized (lock) {
            data = cache.get(key);
            if (data != null) {
                logger.debug("get from cache");
                val = (ConcurrentHashMap<String, AwardInfo>) data.getObjectValue();
                if (val != null && val.size() > 0) {
                    return val;
                }
            }

            PreparedStatement stmt = null;
            DbConnection cnn = null;
            ResultSet result = null;

            ConcurrentHashMap<String, AwardInfo> awards = new ConcurrentHashMap<String, AwardInfo>();

            try {
                cnn = getConnection();
                String sql = "SELECT * FROM award  WHERE status = ? ";
                stmt = cnn.getConnection().prepareStatement(sql);
                stmt.setInt(1, AwardInfo.STATUS_PUBLIC);
                logger.info("getListAward Dao : " + stmt.toString());
                result = stmt.executeQuery();

                while (result.next()) {
                    logger.info(">>>>> Game Dao  getListAward");
                    AwardInfo a = new AwardInfo();
                    a.setAwardId(result.getInt("award_id"));
                    a.setAwardName(result.getString("award_name"));
                    a.setAwardNameSlug(Helper.getUnsignedString(a.getAwardName()));
                    a.setAwardKey(result.getString("award_key"));
                    a.setPointExchange(result.getInt("point_exchange"));
                    a.setFreeMinutes(result.getInt("free_minutes"));
                    a.setDayUsing(result.getInt("day_using"));
                    a.setTopupPrice(result.getInt("topup_price"));
                    awards.put(a.getAwardKey().toLowerCase(), a);
                }

                if (awards.size() > 0) {
                    cache.put(new Element(key, awards));
                }
            } catch (Exception e) {
                logger.error("getListAward, SQLException", e);
            } finally {
                closeResultSet(result);
                closeStatement(stmt);
                freeConnection(cnn);
            }

            val = awards;
        }
        return val;
    }

    public AwardInfo getAwardInfo(String awardKey) {
        logger.info(">>>>> Game Dao  getAwardInfo");
        awardKey = awardKey == null ? "" : awardKey.toLowerCase();
        AwardInfo a = null;

        ConcurrentHashMap<String, AwardInfo> awardList = getListAward();
        if (awardList != null) {
            a = awardList.get(awardKey);
        }

        return a;
    }

    public AwardExchangeResult exchangePointToAward(String msisdn, int subPackageId, int packageId, AwardInfo awardInfo, int quantity, SubPointAction action, String programName) {
        DbConnection cnn = null;
        PreparedStatement stmt_up_point = null;
        PreparedStatement stmt_get_point = null;
        PreparedStatement stmt_log = null;
        PreparedStatement stmt_award = null;
        PreparedStatement stmt_up_sub = null;
        PreparedStatement stmt_topup = null;
        ResultSet rs_get_point = null;
        int totalPoint = 0;

        AwardExchangeResult exchangeResult = new AwardExchangeResult();
        exchangeResult.setErrorCode(BillingErrorCode.SystemError.getValue());

        try {
            cnn = getConnection();
            /*
             * Kiem tra tai khoan
             */
            String sql = "select total_point from sub_point where msisdn = ?";
            stmt_get_point = cnn.getConnection().prepareStatement(sql);
            stmt_get_point.setString(1, msisdn);
            rs_get_point = stmt_get_point.executeQuery();

            if (rs_get_point.next()) {
                totalPoint = rs_get_point.getInt("total_point");
            }

            int point = awardInfo.getPointExchange() * quantity;
            if (point > 0) {
                if (totalPoint >= point) {
                    if (awardInfo.getFreeMinutes() > 0 || awardInfo.getDayUsing() > 0) {
                        /*
                         * Cong phut su dung
                         */
                        int dayUsing = awardInfo.getDayUsing() > 0 ? awardInfo.getDayUsing() * quantity : 0;
                        int freeMinutes = awardInfo.getFreeMinutes() > 0 ? awardInfo.getFreeMinutes() * quantity : 0;

                        sql = "update sub_package set free_minutes = free_minutes + ?, expire_at = DATE_ADD(expire_at, INTERVAL ? DAY) where sub_package_id = ? and msisdn = ?";
                        stmt_up_sub = cnn.getConnection().prepareStatement(sql);
                        stmt_up_sub.setInt(1, freeMinutes);
                        stmt_up_sub.setInt(2, dayUsing);
                        stmt_up_sub.setInt(3, subPackageId);
                        stmt_up_sub.setString(4, msisdn);
                        stmt_up_sub.executeUpdate();
                    }

                    /*
                     * Nap tien tai khoan qua topup
                     */
                    if (awardInfo.getTopupPrice() > 0) {
                        int topupPrice = awardInfo.getTopupPrice() * quantity;
                        sql = "insert into topup(msisdn, price, program_name) values(?, ?, ?)";
                        stmt_topup = cnn.getConnection().prepareStatement(sql);
                        stmt_topup.setString(1, msisdn);
                        stmt_topup.setInt(2, topupPrice);
                        stmt_topup.setString(3, programName);
                        stmt_topup.executeUpdate();
                    }

                    /*
                     * Tru diem vao tai khoan
                     */
                    sql = "update sub_point set before_total_point = total_point, total_point = total_point - ?, updated_date = now() where msisdn = ?";
                    stmt_up_point = cnn.getConnection().prepareStatement(sql);
                    stmt_up_point.setInt(1, point);
                    stmt_up_point.setString(2, msisdn);
                    stmt_up_point.executeUpdate();

                    /*
                     * Luu giao dich doi qua
                     */
                    sql = "insert into sub_award(msisdn, sub_package_id, package_id, point_exchange, award_id, point_before, point_after, quantity, status) values(?, ?, ?, ?, ?, ?, ?, ?, 1)";
                    stmt_award = cnn.getConnection().prepareStatement(sql);
                    stmt_award.setString(1, msisdn);
                    stmt_award.setInt(2, subPackageId);
                    stmt_award.setInt(3, packageId);
                    stmt_award.setInt(4, point);
                    stmt_award.setInt(5, awardInfo.getAwardId());
                    stmt_award.setInt(6, totalPoint);
                    stmt_award.setInt(7, totalPoint - point);
                    stmt_award.setInt(8, quantity);
                    stmt_award.executeUpdate();

                    /*
                     * Luu log tru diem doi qua
                     */
                    sql = "insert into sub_point_history(msisdn, package_id, sub_package_id, point, action) "
                            + " values(?, ?, ?, ?, ?)";
                    stmt_log = cnn.getConnection().prepareStatement(sql);
                    stmt_log.setString(1, msisdn);
                    stmt_log.setInt(2, packageId);
                    stmt_log.setInt(3, subPackageId);
                    stmt_log.setInt(4, 0 - point);
                    stmt_log.setInt(5, action.getValue());
                    stmt_log.executeUpdate();

                    cnn.getConnection().commit();
                    exchangeResult.setErrorCode(BillingErrorCode.Success.getValue());
                    totalPoint = totalPoint - point;

                } else {
                    exchangeResult.setErrorCode(BillingErrorCode.NotEnoughBalance.getValue());
                }
            }

            exchangeResult.setTotalPoint(totalPoint);

        } catch (Exception ex) {
            logger.error("error in exchangePointToAward into database", ex);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        } finally {
            closeResultSet(rs_get_point);
            closeStatement(stmt_get_point);
            closeStatement(stmt_up_point);
            closeStatement(stmt_award);
            closeStatement(stmt_log);
            closeStatement(stmt_up_sub);
            closeStatement(stmt_topup);
            freeConnection(cnn);
        }
        return exchangeResult;
    }

    public boolean updateBonusBirthDay(SubProfileInfo profile, int pointBonus) {
        boolean updateSuccess = false;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt_get = null;
        PreparedStatement stmt_point = null;
        PreparedStatement stmt_log = null;
        ResultSet rs = null;
        String sql = "update sub_profile_friend set bonus_point_year = DATE_FORMAT(now(), '%Y'), updated_date = now() where msisdn = ? and bonus_point_year != DATE_FORMAT(now(), '%Y')";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, profile.getMsisdn());
            updateSuccess = stmt1.executeUpdate() > 0;

            if (updateSuccess && pointBonus > 0) {
                /*
                 * Cong diem vao tai khoan
                 */
                sql = "select total_point from sub_point where msisdn = ?";
                stmt_get = cnn.getConnection().prepareStatement(sql);
                stmt_get.setString(1, profile.getMsisdn());
                rs = stmt_get.executeQuery();
                if (rs.next()) {
                    sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, updated_date = now() where msisdn = ?";
                    stmt_point = cnn.getConnection().prepareStatement(sql);
                    stmt_point.setInt(1, pointBonus);
                    stmt_point.setInt(2, pointBonus);
                    stmt_point.setString(3, profile.getMsisdn());
                    stmt_point.executeUpdate();
                } else {
                    sql = "insert into sub_point(msisdn, total_point, last_promt_ord, original_point) values(?, ?, 0, ?)";
                    stmt_point = cnn.getConnection().prepareStatement(sql);
                    stmt_point.setString(1, profile.getMsisdn());
                    stmt_point.setInt(2, pointBonus);
                    stmt_point.setInt(3, pointBonus);
                    stmt_point.executeUpdate();
                }

                sql = "insert into sub_point_history(msisdn, package_id, sub_package_id, point, action) "
                        + " values(?, ?, ?, ?, ?)";
                stmt_log = cnn.getConnection().prepareStatement(sql);
                stmt_log.setString(1, profile.getMsisdn());
                stmt_log.setInt(2, profile.getPackageId());
                stmt_log.setInt(3, profile.getSubPackageId());
                stmt_log.setInt(4, pointBonus);
                stmt_log.setInt(5, SubPointAction.BirthDay.getValue());
                stmt_log.execute();
            }

            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("Error in updateBonusBirthDay", e);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (Exception e1) {
                    logger.error(e1.getMessage());
                }
            }
            updateSuccess = false;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            closeStatement(stmt_get);
            closeStatement(stmt_point);
            closeStatement(stmt_log);
            freeConnection(cnn);
        }

        return updateSuccess;
    }

    public ArrayList<SubProfileInfo> getSubProfileBirthDay() {
        ArrayList<SubProfileInfo> list = new ArrayList<>();

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select * from sub_profile_friend "
                + " where birth_day = DATE_FORMAT(now(), '%d%m') "
                + " and bonus_point_year != DATE_FORMAT(now(), '%Y') "
                + " and status != ?";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, Constants.PROFILE_STATUS_REMOVE);

            rs = stmt1.executeQuery();
            while (rs.next()) {
                SubProfileInfo p = new SubProfileInfo();
                p.setUserId(rs.getInt("user_id"));
                p.setMsisdn(rs.getString("msisdn"));
                p.setSex(rs.getInt("sex"));
                p.setProvinceId(rs.getInt("province_id"));
                p.setBirthYear(rs.getInt("birth_year"));
                p.setName(rs.getString("name"));
                p.setBirthDay(rs.getString("birth_day"));
                p.setJob(rs.getInt("job"));
                p.setStatus(rs.getInt("status"));

                list.add(p);
            }

        } catch (Exception e) {
            logger.error("Error in getSubProfileBirthDay", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }

    public ArrayList<SubPointCheckingInfo> getListSubPointChecking() {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        ArrayList<SubPointCheckingInfo> list = new ArrayList<>();

        try {
            cnn = getConnection();
            String sql = "SELECT t3.*,t4.total_point,t4.last_call_date,t4.before_total_point "
                    + " FROM (SELECT t1.msisdn,t1.sub_package_id,t1.package_id,t2.sub_package_id old_sub_package_id, "
                    + " t1.reg_at,t2.action,t2.remain_call_minutes,t2.checked_call_date,t2.checked_using_date,t2.remain_using_days "
                    + " FROM sub_package t1 "
                    + " LEFT JOIN  last_check_point t2 "
                    + " ON t1.msisdn = t2.msisdn "
                    + " AND t1.status = ?) t3 "
                    + " LEFT JOIN sub_point t4 "
                    + " ON t3.msisdn = t4.msisdn";

            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, Constants.STATUS_ACTIVE);
            logger.info("DAO SQL getListSubPointChecking : " + stmt + "\n");
            rs = stmt.executeQuery();
            while (rs.next()) {
                SubPointCheckingInfo s = new SubPointCheckingInfo();
                s.setMsisdn(rs.getString("msisdn"));
                s.setPackageId(rs.getInt("package_id"));
                s.setSubPackageid(rs.getInt("sub_package_id"));
                s.setOldSubPackageId(rs.getInt("old_sub_package_id"));
                s.setRemainCallMinutes(rs.getInt("remain_call_minutes"));
                s.setRemainUsingDays(rs.getInt("remain_using_days"));
                s.setTotalPoint(rs.getInt("total_point"));
                s.setBeforeTotalPoint(rs.getInt("before_total_point"));
                s.setRegAt(rs.getTimestamp("reg_at"));
                s.setCheckedCallDate(rs.getTimestamp("checked_call_date"));
                s.setCheckedUsingDate(rs.getTimestamp("checked_using_date"));
                s.setLastCallDate(rs.getTimestamp("last_call_date"));

                list.add(s);
            }

        } catch (Exception ex) {
            logger.error("error in getListSubPointChecking", ex);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public int getTotalDurationCallToMinutes(String msisdn, Timestamp fromDate, Timestamp toDate) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int total = 0;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        logger.info(">>> DAo getTotalDurationCallToMinutes fromDate: " + f.format(fromDate.getTime()) + "; toDate: " + f.format(toDate.getTime()));
        try {
            cnn = getConnection();
            String sql = "select sum(CEILING(duration_time/60)) total_minutes "
                    + " from call_activity "
                    + " where msisdn = ? "
                    + " and end_at >= ?"
                    + " and end_at <= ?"
                    + " and voice_fee > 0 ";

            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            stmt.setTimestamp(2, fromDate);
            stmt.setTimestamp(3, toDate);

            rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getInt("total_minutes");
            }

        } catch (Exception ex) {
            logger.error("error in getTotalDurationCallToMinutes: " + msisdn, ex);
            total = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return total;
    }

    public BillingErrorCode addPointByCheckingPoint(SubPointCheckingInfo checkingInfo, HashMap<SubPointAction, Integer> addPointWithActions) {
        logger.info(">>> Dao addPointByCheckingPoint :"+addPointWithActions+"\n");
        BillingErrorCode result = BillingErrorCode.SystemError;

        DbConnection cnn = null;
        PreparedStatement stmt_point = null;
        PreparedStatement stmt_get = null;
        PreparedStatement stmt_log = null;
        PreparedStatement stmt_checked = null;
        ResultSet rs = null;

        boolean hasPointBalance = false;
        int totalPoint = 0;
        int currentPoint = 0;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            cnn = getConnection();
            for (SubPointAction action : addPointWithActions.keySet()) {
                int point = addPointWithActions.get(action);
                if (point > 0) {
                    totalPoint += point;
                }
            }

            logger.debug("totalPoint for " + checkingInfo.getMsisdn() + ": " + totalPoint + "; checkedCallDate: " + f.format(checkingInfo.getCheckedCallDate().getTime()));

            String sql = "select total_point from sub_point where msisdn = ?";
            stmt_get = cnn.getConnection().prepareStatement(sql);
            stmt_get.setString(1, checkingInfo.getMsisdn());
            rs = stmt_get.executeQuery();
            if (rs.next()) {
                currentPoint = rs.getInt("total_point");
                hasPointBalance = true;
            }

            if (totalPoint > 0) {
                if (hasPointBalance) {
                    /*
                     * Cong don diem vao tai khoan
                     */
                    sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, updated_date = now() where msisdn = ?";
                    stmt_point = cnn.getConnection().prepareStatement(sql);
                    stmt_point.setInt(1, totalPoint);
                    stmt_point.setInt(2, totalPoint);
                    stmt_point.setString(3, checkingInfo.getMsisdn());
                    stmt_point.executeUpdate();
                } else {
                    /*
                     * Tao tai khoan diem
                     */
                    sql = "insert into sub_point(total_point, msisdn, original_point) values(?, ?, ?)";
                    stmt_point = cnn.getConnection().prepareStatement(sql);
                    stmt_point.setInt(1, totalPoint);
                    stmt_point.setString(2, checkingInfo.getMsisdn());
                    stmt_point.setInt(3, totalPoint);
                    stmt_point.executeUpdate();
                }

                /*
                 * Luu log cong diem
                 */
                sql = "insert into sub_point_history(msisdn, package_id, sub_package_id, point, action) "
                        + " values(?, ?, ?, ?, ?)";
                stmt_log = cnn.getConnection().prepareStatement(sql);

                for (SubPointAction action : addPointWithActions.keySet()) {
                    int point = addPointWithActions.get(action);
                    stmt_log.setString(1, checkingInfo.getMsisdn());
                    stmt_log.setInt(2, checkingInfo.getPackageId());
                    stmt_log.setInt(3, checkingInfo.getSubPackageid());
                    stmt_log.setInt(4, point);
                    stmt_log.setInt(5, action.getValue());
                    stmt_log.addBatch();
                }
                stmt_log.executeBatch();
            }

            /*
             * Luu lich su check point 
             */
            sql = "update last_check_point set remain_using_days = ?, remain_call_minutes = ?, checked_using_date = ?, checked_call_date = ?, sub_package_id = ?, updated_date = now() where msisdn = ?";
            stmt_checked = cnn.getConnection().prepareStatement(sql);
            stmt_checked.setInt(1, checkingInfo.getRemainUsingDays());
            stmt_checked.setInt(2, checkingInfo.getRemainCallMinutes());
            stmt_checked.setTimestamp(3, checkingInfo.getCheckedUsingDate());
            stmt_checked.setTimestamp(4, checkingInfo.getCheckedCallDate());
            stmt_checked.setInt(5, checkingInfo.getSubPackageid());
            stmt_checked.setString(6, checkingInfo.getMsisdn());
            int row = stmt_checked.executeUpdate();
            if (row <= 0) {
                closeStatement(stmt_checked);

                sql = "insert into last_check_point(msisdn, remain_call_minutes, checked_using_date, checked_call_date, sub_package_id, remain_using_days) values(?, ?, ?, ?, ?, ?)";
                stmt_checked = cnn.getConnection().prepareStatement(sql);
                stmt_checked.setString(1, checkingInfo.getMsisdn());
                stmt_checked.setInt(2, checkingInfo.getRemainCallMinutes());
                stmt_checked.setTimestamp(3, checkingInfo.getCheckedUsingDate());
                stmt_checked.setTimestamp(4, checkingInfo.getCheckedCallDate());
                stmt_checked.setInt(5, checkingInfo.getSubPackageid());
                stmt_checked.setInt(6, checkingInfo.getRemainUsingDays());
                stmt_checked.executeUpdate();
            }

            cnn.getConnection().commit();

            totalPoint = totalPoint + currentPoint;
            checkingInfo.setTotalPoint(totalPoint);
            checkingInfo.setBeforeTotalPoint(currentPoint);

            result = BillingErrorCode.Success;
        } catch (Exception ex) {
            logger.error("error in addPointByCheckingPoint into database", ex);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(stmt_get);
            closeStatement(stmt_point);
            closeStatement(stmt_log);
            closeStatement(stmt_checked);
            freeConnection(cnn);
        }

        return result;
    }

    public BillingErrorCode insertUpdateSubPoint(SubPackageInfo sub, int action, int point_add, String desc) {
        BillingErrorCode result = BillingErrorCode.SystemError;
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt_his = null;
        ResultSet rs = null;
        ResultSet rs_his = null;

        String sql = "INSERT INTO sub_point (msisdn,total_point,updated_date,before_total_point,original_point) VALUES (?,?,now(),?,?) "
                + " ON DUPLICATE KEY UPDATE "
                + " updated_date = now() ,"
                + " before_total_point = total_point ,"
                + " total_point = total_point + ? ,"
                + "original_point = original_point + ?";

        String sql_point_history = "INSERT INTO sub_point_history (msisdn,package_id,sub_package_id,point,action,description,created_date) VALUES (?,?,?,?,?,?,now())";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, sub.getMsisdn());
            stmt.setInt(2, point_add);
            stmt.setInt(3, point_add);
            stmt.setInt(4, point_add);
            stmt.setInt(5, point_add);
            stmt.setInt(6, point_add);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                stmt_his = cnn.getConnection().prepareStatement(sql_point_history);
                stmt_his.setString(1, sub.getMsisdn());
                stmt_his.setInt(2, sub.getPackageId());
                stmt_his.setInt(3, sub.getSubPackageId());
                stmt_his.setInt(4, point_add);
                stmt_his.setInt(5, action);
                stmt_his.setString(6, desc);

                if (logger.isDebugEnabled()) {
                    String sql_log = stmt.toString();
                    sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                    logger.debug(sql_log);
                }
                rs_his = stmt_his.executeQuery();
                cnn.getConnection().commit();
                result = BillingErrorCode.Success;
            } else {
                rollbackTransaction(cnn);
            }
        } catch (Exception e) {
            e.printStackTrace();
            rollbackTransaction(cnn);
            logger.error("Exception in insertUpdateSubPoint", e);
        } finally {
            closeResultSet(rs);
            closeResultSet(rs_his);
            closeStatement(stmt);
            closeStatement(stmt_his);
            freeConnection(cnn);
        }

        return result;
    }
}
