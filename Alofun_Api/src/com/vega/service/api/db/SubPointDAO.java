package com.vega.service.api.db;

import com.vega.service.api.common.DateUtil;
import com.vega.service.api.common.Helper;
import com.vega.service.api.common.SubPointAction;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.object.BillingErrorCode;
import com.vega.service.api.object.SubPackageInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.vega.vcs.service.database.pool.DbConnection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import javax.naming.NamingException;

/**
 * @author HaoNM
 *
 */
public class SubPointDAO extends DBConnections {

    public SubPointDAO() throws NamingException {
        super.start();
    }

    public boolean checkSubPoint(String msisdn) {
        String sql = null;
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            cnn = getConnection();
            sql = "select * where msisdn = ? ";
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            rs = stmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            logger.error(e);
        } finally {
            freeConnection(cnn);
        }
        return false;
    }

    public void addPointForFirstCallInDay(SubPackageInfo sub) {
        logger.info("Cong diem: addPointForFirstCallInDay: " + sub.getMsisdn());
        String sql = null;
        DbConnection cnn = null;
        PreparedStatement stmt_update_call = null;

        try {
            cnn = getConnection();
            if (sub.getStatus() == SubPackageInfo.SubPackageStatus.Active) {
                int point = Helper.getInt(ConfigStack.getConfig("game", "pointFirstCallInDay", "50"), 50);
                if (point > 0 && sub.isIsFirstCallInDay()) {
                    /*
                     * Xu ly cong diem cuoc goi lan dau trong ngay
                     */
                    processAddPointForFirstCallInDay(sub, point, SubPointAction.FirstCallInDay);
                } else if (!sub.isIsFirstCallInDay()) {
                    /*
                     * Update thoi gian goi gan nhat
                     */
                    sql = "update sub_point set last_call_date = now() where msisdn = ?";
                    stmt_update_call = cnn.getConnection().prepareStatement(sql);
                    stmt_update_call.setString(1, sub.getMsisdn());
                    stmt_update_call.executeUpdate();
                    cnn.getConnection().commit();
                }
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            closeStatement(stmt_update_call);
            freeConnection(cnn);
        }
    }

    public boolean processAddPointForListAction(SubPackageInfo sub, HashMap<SubPointAction, Integer> addPointWithActions) {
        logger.info("Cong diem: processAddPointForListAction: " + sub.getMsisdn());
        DbConnection cnn = null;
        PreparedStatement stmt_point = null;
        PreparedStatement stmt_log = null;
        PreparedStatement stmt_get = null;
        ResultSet rs = null;

        boolean addPoint = false;
        boolean hasPointForFirstCall = false;
        boolean hasPointBalance = false;
        int totalPoint = 0;
        int currentPoint = 0;
        String sql = null;

        try {
            cnn = getConnection();
            for (SubPointAction action : addPointWithActions.keySet()) {
                int point = addPointWithActions.get(action);
                if (point > 0) {
                    totalPoint += point;

                    if (action == SubPointAction.FirstCallInDay) {
                        hasPointForFirstCall = true;
                    }
                }
            }

            sql = "select total_point,last_promt_ord from sub_point where msisdn = ?";
            stmt_get = cnn.getConnection().prepareStatement(sql);
            stmt_get.setString(1, sub.getMsisdn());
            rs = stmt_get.executeQuery();
            if (rs.next()) {
                currentPoint = rs.getInt("total_point");
                sub.setPromtCustomerCareOrd(rs.getInt("last_promt_ord"));
                hasPointBalance = true;
            }

            if (totalPoint > 0) {
                int nextPromtOrd = sub.getPromtCustomerCareOrd() + 1;
                int maxPromtOrd = Helper.getInt(ConfigStack.getConfig("game", "maxPromtCustomerCare", "7"), 7);

                if (hasPointForFirstCall) {
                    if (nextPromtOrd > maxPromtOrd) {
                        float maxPromtPeriodDay = Helper.getInt(ConfigStack.getConfig("game", "maxPromtPeriodDay", "60"), 60);
                        Calendar gameStartingDate = Calendar.getInstance();
                        Calendar currentDate = Calendar.getInstance();
                        Calendar lastCallDate = sub.getLastCallDate() == null ? Calendar.getInstance() : sub.getLastCallDate();
                        String startingDate = ConfigStack.getConfig("game", "startingDate", "");
                        java.util.Date d = null;

                        try {
                            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            d = f.parse(startingDate);
                        } catch (Exception e) {
                        }

                        if (d != null) {
                            gameStartingDate.setTime(d);
                        }

                        int daysByLastCall = DateUtil.daysBetween(new Timestamp(gameStartingDate.getTimeInMillis()),new Timestamp(lastCallDate.getTimeInMillis()));
                        int daysByCurrent = DateUtil.daysBetween(new Timestamp(gameStartingDate.getTimeInMillis()),new Timestamp(currentDate.getTimeInMillis()));

                        if (Math.floor(daysByLastCall / maxPromtPeriodDay) < Math.floor(daysByCurrent / maxPromtPeriodDay)) {
                            /*
                             * Restart on new period
                             */
                            nextPromtOrd = 1;
                        }
                    }

                    if (hasPointBalance) {
                        /*
                         * Cong don diem vao tai khoan
                         */
                        sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, last_promt_ord = ?, updated_date = now(), last_call_date = now() where msisdn = ?";
                        stmt_point = cnn.getConnection().prepareStatement(sql);
                        stmt_point.setInt(1, totalPoint);
                        stmt_point.setInt(2, totalPoint);
                        stmt_point.setInt(3, nextPromtOrd);
                        stmt_point.setString(4, sub.getMsisdn());
                        stmt_point.executeUpdate();
                    } else {
                        /*
                         * Tao tai khoan diem
                         */
                        sql = "insert into sub_point(total_point, last_promt_ord, msisdn, last_call_date, original_point) values(?, ?, ?, now(), ?)";
                        stmt_point = cnn.getConnection().prepareStatement(sql);
                        stmt_point.setInt(1, totalPoint);
                        stmt_point.setInt(2, nextPromtOrd);
                        stmt_point.setString(3, sub.getMsisdn());
                        stmt_point.setInt(4, totalPoint);
                        stmt_point.executeUpdate();
                    }

                } else {
                    if (hasPointBalance) {
                        /*
                         * Cong don diem vao tai khoan
                         */
                        sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, updated_date = now() where msisdn = ?";
                        stmt_point = cnn.getConnection().prepareStatement(sql);
                        stmt_point.setInt(1, totalPoint);
                        stmt_point.setInt(2, totalPoint);
                        stmt_point.setString(3, sub.getMsisdn());
                        stmt_point.executeUpdate();
                    } else {
                        /*
                         * Tao tai khoan diem
                         */
                        sql = "insert into sub_point(total_point, msisdn, original_point) values(?, ?, ?)";
                        stmt_point = cnn.getConnection().prepareStatement(sql);
                        stmt_point.setInt(1, totalPoint);
                        stmt_point.setString(2, sub.getMsisdn());
                        stmt_point.setInt(3, totalPoint);
                        stmt_point.executeUpdate();
                    }
                }

                /*
                 * Luu log cong diem
                 */
                sql = "insert into sub_point_history(msisdn, package_id, sub_package_id, point, action) "
                        + " values(?, ?, ?, ?, ?)";
                stmt_log = cnn.getConnection().prepareStatement(sql);

                for (SubPointAction action : addPointWithActions.keySet()) {
                    int point = addPointWithActions.get(action);
                    if (point > 0) {
                        stmt_log.setString(1, sub.getMsisdn());
                        stmt_log.setInt(2, sub.getPackageId());
                        stmt_log.setInt(3, sub.getSubPackageId());
                        stmt_log.setInt(4, point);
                        stmt_log.setInt(5, action.getValue());
                        stmt_log.addBatch();
                    }
                }

                stmt_log.executeBatch();

                if (hasPointForFirstCall) {
                    sub.setIsBonusFirstCallInDay(true);
                    if (nextPromtOrd > maxPromtOrd) {
                        nextPromtOrd = 0;
                    }
                    sub.setPromtCustomerCareOrd(nextPromtOrd);
                }

                cnn.getConnection().commit();
                addPoint = true;
            }

            sub.setTotalPoint(totalPoint + currentPoint);
            sub.setBeforeTotalPoint(currentPoint);

        } catch (Exception ex) {
            logger.error("error in processAddPointForListAction into database", ex);
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
            closeStatement(stmt_log);
            closeStatement(stmt_point);
            freeConnection(cnn);
        }

        return addPoint;
    }

    public boolean processAddPointForFirstCallInDay(SubPackageInfo sub, int point, SubPointAction action) {
        logger.info(">>>> Cong diem: processAddPointForFirstCallInDay: " + sub.getMsisdn());
        DbConnection cnn = null;
        PreparedStatement stmt_point = null;
        PreparedStatement stmt_log = null;
        PreparedStatement stmt_get = null;
        ResultSet rs = null;

        int currentPoint = 0;
        boolean addPoint = false;
        boolean hasPointBalance = false;
        String sql = null;

        try {
            cnn = getConnection();
            int maxPromtOrd = Helper.getInt(ConfigStack.getConfig("game", "maxPromtCustomerCare", "7"), 7);
            int nextPromtOrd = sub.getPromtCustomerCareOrd() + 1;

            if (nextPromtOrd > maxPromtOrd) {
                float maxPromtPeriodDay = Helper.getInt(ConfigStack.getConfig("game", "maxPromtPeriodDay", "60"), 60);
                if (nextPromtOrd > maxPromtPeriodDay) {
                    /*
                     * Restart on new period
                     */
                    nextPromtOrd = 1;
                }
            }

            sql = "select total_point from sub_point where msisdn = ?";
            stmt_get = cnn.getConnection().prepareStatement(sql);
            stmt_get.setString(1, sub.getMsisdn());
            rs = stmt_get.executeQuery();
            if (rs.next()) {
                currentPoint = rs.getInt("total_point");
                hasPointBalance = true;
            }

            if (hasPointBalance) {
                /*
                 * Cong don diem vao tai khoan
                 */
                sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, last_promt_ord = ?, updated_date = now(), last_call_date = now() where msisdn = ?";
                stmt_point = cnn.getConnection().prepareStatement(sql);
                stmt_point.setInt(1, point);
                stmt_point.setInt(2, point);
                stmt_point.setInt(3, nextPromtOrd);
                stmt_point.setString(4, sub.getMsisdn());
                stmt_point.executeUpdate();
            } else {
                /*
                 * Tao tai khoan diem
                 */
                action = SubPointAction.FirstRegisterFromGamingStarted;
                sql = "insert into sub_point(total_point, last_promt_ord, msisdn, last_call_date, original_point) values(?, ?, ?, now(), ?)";
                stmt_point = cnn.getConnection().prepareStatement(sql);
                stmt_point.setInt(1, point);
                stmt_point.setInt(2, nextPromtOrd);
                stmt_point.setString(3, sub.getMsisdn());
                stmt_point.setInt(4, point);
                stmt_point.executeUpdate();
            }

            sql = "insert into sub_point_history(msisdn, package_id, sub_package_id, point, action) "
                    + " values(?, ?, ?, ?, ?)";
            stmt_log = cnn.getConnection().prepareStatement(sql);
            stmt_log.setString(1, sub.getMsisdn());
            stmt_log.setInt(2, sub.getPackageId());
            stmt_log.setInt(3, sub.getSubPackageId());
            stmt_log.setInt(4, point);
            stmt_log.setInt(5, action.getValue());
            stmt_log.execute();
            cnn.getConnection().commit();
            addPoint = true;

            sub.setTotalPoint(currentPoint + point);
            sub.setBeforeTotalPoint(currentPoint);
            sub.setIsBonusFirstCallInDay(addPoint);
            if (nextPromtOrd > maxPromtOrd) {
                /*
                 * Khong phat tiep file promt CSKH thong bao the le
                 */
                nextPromtOrd = 0;
            }
            sub.setPromtCustomerCareOrd(nextPromtOrd);
        } catch (Exception ex) {
            logger.error("error in processAddPointForFirstCallInDay into database", ex);
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
            closeStatement(stmt_log);
            closeStatement(stmt_point);
            freeConnection(cnn);
        }

        return addPoint;
    }

    public BillingErrorCode processBeforeRemainUsingDays(SubPackageInfo oldSub, SubPackageInfo sub) {
        logger.info("Bao luu diem + ngay su dung khi chua quy doi: processBeforeRemainUsingDays: " + sub.getMsisdn());
        BillingErrorCode result = BillingErrorCode.SystemError;
        DbConnection cnn = null;
        PreparedStatement stmt_get = null;
        PreparedStatement stmt_checked = null;
        ResultSet rs = null;
        String sql = "select remain_using_days, DATE_FORMAT(checked_using_date, '%Y-%m-%d %H:%i:%s') checked_using_date from last_check_point where msisdn = ? ";

        int remainUsingDays = 0;
        Calendar checkingUsingDate = null;

        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar currentDate = Calendar.getInstance();
        Calendar gameStartingDate = Calendar.getInstance();

        try {
            /*
             * ngay bat dau chay gamification
             */
            String startingDate = ConfigStack.getConfig("game", "startingDate", "");
            java.util.Date d = null;
            try {
                d = f.parse(startingDate);
            } catch (Exception e) {
                d = currentDate.getTime();
            }
            gameStartingDate.setTime(d);

            cnn = getConnection();
            stmt_get = cnn.getConnection().prepareStatement(sql);
            stmt_get.setString(1, sub.getMsisdn());
            rs = stmt_get.executeQuery();
            if (rs.next()) {
                remainUsingDays = rs.getInt("remain_using_days");
                if (rs.getTimestamp("checked_using_date") != null) {
                    /*
                     * Ngay chot diem thoi gian su dung truoc do
                     */
                    checkingUsingDate = DateUtil.string2Calendar(rs.getString("checked_using_date"));
                }
            }

            if (checkingUsingDate == null) {
                /*
                 * Chua chot lan nao, mac dinh la ngay dau chay gamification 
                 */
                checkingUsingDate = (Calendar) gameStartingDate.clone();
                if (DateUtil.string2Calendar(oldSub.getRegAt()).after(checkingUsingDate)) {
                    /*
                     * Tinh tu ngay dang ky, neu dang ky sau ngay ra mat gamification
                     */
                    checkingUsingDate = DateUtil.string2Calendar(oldSub.getRegAt());
                }
            }

            /*
             * Tinh so ngay su dung con du cua goi cuoc cu (chua quy doi ra diem)
             * Xac dinh bang cach: Ngay huy goi cuoc cu - ngay chot quy doi diem truoc do
             */
            int days = DateUtil.daysBetween(new Timestamp(checkingUsingDate.getTimeInMillis()), new Timestamp(DateUtil.string2Calendar(oldSub.getUpdatedAt()).getTimeInMillis()));
            days = days < 0 ? 0 : days;
            logger.debug("remain using days for " + sub.getMsisdn() + ": " + days);
            /*
             * Cong them so ngay su dung bao luu da tinh truoc do
             */
            days = days + remainUsingDays;
            logger.debug("using days for " + sub.getMsisdn() + " after add remain: " + days);

            if (days > 0 && days != remainUsingDays) {
                /*
                 * Bao luu lai so ngay su dung du truoc do
                 * Ngay chot quy doi diem tinh tu ngay dang ky goi cuoc moi
                 */
                sql = "update last_check_point set remain_using_days = ?, checked_using_date = now(), updated_date = now() where msisdn = ?";
                stmt_checked = cnn.getConnection().prepareStatement(sql);
                stmt_checked.setInt(1, days);
                stmt_checked.setString(2, sub.getMsisdn());
                int row = stmt_checked.executeUpdate();

                if (row <= 0) {
                    closeStatement(stmt_checked);

                    sql = "insert into last_check_point(msisdn, remain_call_minutes, remain_using_days, sub_package_id, checked_using_date, checked_call_date) values(?, ?, ?, ?, now(), now())";
                    stmt_checked = cnn.getConnection().prepareStatement(sql);
                    stmt_checked.setString(1, sub.getMsisdn());
                    stmt_checked.setInt(2, 0);
                    stmt_checked.setInt(3, days);
                    stmt_checked.setInt(4, sub.getSubPackageId());
                    stmt_checked.executeUpdate();
                }

                cnn.getConnection().commit();
            }

            result = BillingErrorCode.Success;
        } catch (Exception ex) {
            logger.error("error in processBeforeRemainUsingDays into database", ex);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e) {
                    logger.error(e.getMessage());
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(stmt_get);
            closeStatement(stmt_checked);
            freeConnection(cnn);
        }

        return result;
    }
//
//    public ArrayList<SubPointCheckingInfo> getListSubPointChecking() {
//        DbConnection cnn = null;
//        PreparedStatement stmt = null;
//        ResultSet rs = null;
//
//        ArrayList<SubPointCheckingInfo> list = new ArrayList<SubPointCheckingInfo>();
//
//        try {
//            cnn = getConnection();
//            String sql = "select t3.*, t4.total_point, t4.last_call_date, t4.before_total_point from ("
//                    + "  select t1.msisdn, t1.sub_package_id, t1.package_id, t2.sub_package_id old_sub_package_id, t1.reg_at, "
//                    + "    t2.action, t2.remain_call_minutes, t2.checked_call_date, t2.checked_using_date, t2.remain_using_days from ("
//                    + "    select msisdn, sub_package_id, package_id, reg_at  from vp_sub_package "
//                    + "    where status = ? "
//                    + "  ) t1 left join("
//                    + "    select msisdn, sub_package_id, action, remain_call_minutes, checked_call_date, checked_using_date, remain_using_days "
//                    + "    from last_check_point "
//                    + "  ) t2 on t1.msisdn = t2.msisdn "
//                    + ") t3 left join sub_point t4 on t3.msisdn = t4.msisdn "
//                    + " order by last_call_date desc, total_point desc";
//
//            stmt = cnn.getConnection().prepareStatement(sql);
//            stmt.setInt(1, PackageStatus.Active.getValue());
//
//            rs = stmt.executeQuery();
//            while (rs.next()) {
//                SubPointCheckingInfo s = new SubPointCheckingInfo();
//                s.setMsisdn(rs.getString("msisdn"));
//                s.setPackageId(rs.getInt("package_id"));
//                s.setSubPackageid(rs.getInt("sub_package_id"));
//                s.setOldSubPackageId(rs.getInt("old_sub_package_id"));
//                s.setRemainCallMinutes(rs.getInt("remain_call_minutes"));
//                s.setRemainUsingDays(rs.getInt("remain_using_days"));
//                s.setTotalPoint(rs.getInt("total_point"));
//                s.setBeforeTotalPoint(rs.getInt("before_total_point"));
//                s.setRegAt(BillingUtility.getCalendar(rs.getTimestamp("reg_at"), false));
//                s.setCheckedCallDate(BillingUtility.getCalendar(rs.getTimestamp("checked_call_date"), true));
//                s.setCheckedUsingDate(BillingUtility.getCalendar(rs.getTimestamp("checked_using_date"), true));
//                s.setLastCallDate(BillingUtility.getCalendar(rs.getTimestamp("last_call_date"), true));
//
//                list.add(s);
//            }
//
//        } catch (Exception ex) {
//            logger.error("error in getListSubPointChecking", ex);
//        } finally {
//            closeResultSet(rs);
//            closeStatement(stmt);
//            freeConnection(cnn);
//        }
//
//        return list;
//    }
//
//    public BillingErrorCode addPointByCheckingPoint(SubPointCheckingInfo checkingInfo, HashMap<SubPointAction, Integer> addPointWithActions) {
//        BillingErrorCode result = BillingErrorCode.SystemError;
//
//        DbConnection cnn = null;
//        PreparedStatement stmt_point = null;
//        PreparedStatement stmt_get = null;
//        PreparedStatement stmt_log = null;
//        PreparedStatement stmt_checked = null;
//        ResultSet rs = null;
//
//        boolean hasPointBalance = false;
//        int totalPoint = 0;
//        int currentPoint = 0;
//        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//        try {
//            cnn = getConnection();
//            for (SubPointAction action : addPointWithActions.keySet()) {
//                int point = addPointWithActions.get(action);
//                if (point > 0) {
//                    totalPoint += point;
//                }
//            }
//
//            logger.debug("totalPoint for " + checkingInfo.getMsisdn() + ": " + totalPoint + "; checkedCallDate: " + f.format(checkingInfo.getCheckedCallDate().getTime()));
//
//            String sql = "select total_point from sub_point where msisdn = ?";
//            stmt_get = cnn.getConnection().prepareStatement(sql);
//            stmt_get.setString(1, checkingInfo.getMsisdn());
//            rs = stmt_get.executeQuery();
//            if (rs.next()) {
//                currentPoint = rs.getInt("total_point");
//                hasPointBalance = true;
//            }
//
//            if (totalPoint > 0) {
//                if (hasPointBalance) {
//                    /*
//                     * Cong don diem vao tai khoan
//                     */
//                    sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, updated_date = now() where msisdn = ?";
//                    stmt_point = cnn.getConnection().prepareStatement(sql);
//                    stmt_point.setInt(1, totalPoint);
//                    stmt_point.setInt(2, totalPoint);
//                    stmt_point.setString(3, checkingInfo.getMsisdn());
//                    stmt_point.executeUpdate();
//                } else {
//                    /*
//                     * Tao tai khoan diem
//                     */
//                    sql = "insert into sub_point(sub_point_ord, total_point, msisdn, original_point) values(seq_sub_point.nextval, ?, ?, ?)";
//                    stmt_point = cnn.getConnection().prepareStatement(sql);
//                    stmt_point.setInt(1, totalPoint);
//                    stmt_point.setString(2, checkingInfo.getMsisdn());
//                    stmt_point.setInt(3, totalPoint);
//                    stmt_point.executeUpdate();
//                }
//
//                /*
//                 * Luu log cong diem
//                 */
//                sql = "insert into sub_point_history(history_id, msisdn, package_id, sub_package_id, point, action) "
//                        + " values(seq_sub_point_history.nextval, ?, ?, ?, ?, ?)";
//                stmt_log = cnn.getConnection().prepareStatement(sql);
//
//                for (SubPointAction action : addPointWithActions.keySet()) {
//                    int point = addPointWithActions.get(action);
//                    stmt_log.setString(1, checkingInfo.getMsisdn());
//                    stmt_log.setInt(2, checkingInfo.getPackageId());
//                    stmt_log.setInt(3, checkingInfo.getSubPackageid());
//                    stmt_log.setInt(4, point);
//                    stmt_log.setInt(5, action.getValue());
//                    stmt_log.addBatch();
//                }
//                stmt_log.executeBatch();
//            }
//
//            /*
//             * Luu lich su check point 
//             */
//            sql = "update last_check_point set remain_using_days = ?, remain_call_minutes = ?, checked_using_date = to_date(?, 'yyyy-mm-dd hh24:mi:ss'), checked_call_date = to_date(?, 'yyyy-mm-dd hh24:mi:ss'), sub_package_id = ?, updated_date = now() where msisdn = ?";
//            stmt_checked = cnn.getConnection().prepareStatement(sql);
//            stmt_checked.setInt(1, checkingInfo.getRemainUsingDays());
//            stmt_checked.setInt(2, checkingInfo.getRemainCallMinutes());
//            stmt_checked.setString(3, f.format(checkingInfo.getCheckedUsingDate().getTime()));
//            stmt_checked.setString(4, f.format(checkingInfo.getCheckedCallDate().getTime()));
//            stmt_checked.setInt(5, checkingInfo.getSubPackageid());
//            stmt_checked.setString(6, checkingInfo.getMsisdn());
//            int row = stmt_checked.executeUpdate();
//            if (row <= 0) {
//                closeStatement(stmt_checked);
//
//                sql = "insert into last_check_point(msisdn, remain_call_minutes, checked_using_date, checked_call_date, sub_package_id, remain_using_days) values(?, ?, to_date(?, 'yyyy-mm-dd hh24:mi:ss'), to_date(?, 'yyyy-mm-dd hh24:mi:ss'), ?, ?)";
//                stmt_checked = cnn.getConnection().prepareStatement(sql);
//                stmt_checked.setString(1, checkingInfo.getMsisdn());
//                stmt_checked.setInt(2, checkingInfo.getRemainCallMinutes());
//                stmt_checked.setString(3, f.format(checkingInfo.getCheckedUsingDate().getTime()));
//                stmt_checked.setString(4, f.format(checkingInfo.getCheckedCallDate().getTime()));
//                stmt_checked.setInt(5, checkingInfo.getSubPackageid());
//                stmt_checked.setInt(6, checkingInfo.getRemainUsingDays());
//                stmt_checked.executeUpdate();
//            }
//
//            cnn.getConnection().commit();
//
//            totalPoint = totalPoint + currentPoint;
//            checkingInfo.setTotalPoint(totalPoint);
//            checkingInfo.setBeforeTotalPoint(currentPoint);
//
//            result = BillingErrorCode.Success;
//        } catch (Exception ex) {
//            logger.error("error in addPointByCheckingPoint into database", ex);
//            if (cnn != null) {
//                try {
//                    cnn.getConnection().rollback();
//                } catch (Exception e) {
//                    logger.error(e.getMessage());
//                }
//            }
//        } finally {
//            closeResultSet(rs);
//            closeStatement(stmt_get);
//            closeStatement(stmt_point);
//            closeStatement(stmt_log);
//            closeStatement(stmt_checked);
//            freeConnection(cnn);
//        }
//
//        return result;
//    }
//
//    public ArrayList<SubPointCheckingInfo> getListSubPointNotCalling(int limit, int days, int lastSubPointOrd) {
//        DbConnection cnn = null;
//        PreparedStatement stmt = null;
//        ResultSet rs = null;
//
//        ArrayList<SubPointCheckingInfo> list = new ArrayList<SubPointCheckingInfo>();
//
//        try {
//            cnn = getConnection();
//            String sql = "select * from ("
//                    + "  select t1.*, row_number() over (order by sub_point_ord) as rnum from ("
//                    + "    select msisdn, total_point, sub_point_ord "
//                    + "    from sub_point "
//                    + "    where sub_point_ord > ? "
//                    + "		and ((last_call_date is null and created_date + ? >= now()) "
//                    + "      or (last_call_date is not null and last_call_date + ? >= now()))"
//                    + "  ) t1 inner join ("
//                    + "      select msisdn "
//                    + "      from vp_sub_package "
//                    + "      where status = ? "
//                    + "  ) t2 on t1.msisdn = t2.msisdn "
//                    + ") where rnum <= ?";
//
//            stmt = cnn.getConnection().prepareStatement(sql);
//            stmt.setInt(1, lastSubPointOrd);
//            stmt.setInt(2, days);
//            stmt.setInt(3, days);
//            stmt.setInt(4, PackageStatus.Active.getValue());
//            stmt.setInt(5, limit);
//
//            rs = stmt.executeQuery();
//            while (rs.next()) {
//                SubPointCheckingInfo s = new SubPointCheckingInfo();
//                s.setMsisdn(rs.getString("msisdn"));
//                s.setTotalPoint(rs.getInt("total_point"));
//                s.setSubPointOrd(rs.getInt("sub_point_ord"));
//
//                list.add(s);
//            }
//
//        } catch (Exception ex) {
//            logger.error("error in getListSubPointNotCalling", ex);
//        } finally {
//            closeResultSet(rs);
//            closeStatement(stmt);
//            freeConnection(cnn);
//        }
//
//        return list;
//    }
//
//    public ArrayList<SubPointCheckingInfo> getListSubPointEnoughPointToAward(int limit, int point, int lastSubPointOrd) {
//        DbConnection cnn = null;
//        PreparedStatement stmt = null;
//        ResultSet rs = null;
//
//        ArrayList<SubPointCheckingInfo> list = new ArrayList<SubPointCheckingInfo>();
//
//        try {
//            cnn = getConnection();
//            String sql = "select * from ("
//                    + "  select t1.*, row_number() over (order by sub_point_ord) as rnum from ("
//                    + "    select msisdn, total_point, before_total_point, sub_point_ord "
//                    + "    from sub_point "
//                    + "    where sub_point_ord > ? "
//                    + "    and total_point >= ? "
//                    + "    and before_total_point < total_point "
//                    + "  ) t1 inner join ("
//                    + "      select msisdn "
//                    + "      from vp_sub_package "
//                    + "      where status = ? "
//                    + "  ) t2 on t1.msisdn = t2.msisdn "
//                    + ") where rnum <= ?";
//
//            stmt = cnn.getConnection().prepareStatement(sql);
//            stmt.setInt(1, lastSubPointOrd);
//            stmt.setInt(2, point);
//            stmt.setInt(3, PackageStatus.Active.getValue());
//            stmt.setInt(4, limit);
//
//            rs = stmt.executeQuery();
//            while (rs.next()) {
//                SubPointCheckingInfo s = new SubPointCheckingInfo();
//                s.setMsisdn(rs.getString("msisdn"));
//                s.setTotalPoint(rs.getInt("total_point"));
//                s.setBeforeTotalPoint(rs.getInt("before_total_point"));
//                s.setSubPointOrd(rs.getInt("sub_point_ord"));
//
//                list.add(s);
//            }
//
//        } catch (Exception ex) {
//            logger.error("error in getListSubPointEnoughPointToAward", ex);
//        } finally {
//            closeResultSet(rs);
//            closeStatement(stmt);
//            freeConnection(cnn);
//        }
//
//        return list;
//    }
}
