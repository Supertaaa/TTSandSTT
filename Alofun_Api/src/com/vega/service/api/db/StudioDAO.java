package com.vega.service.api.db;

import com.vega.service.api.common.Constants;
import com.vega.service.api.object.StudioListenInfo;
import com.vega.service.api.object.StudioRecordInfo;
import com.vega.service.api.object.StudioVoteInfo;
import com.vega.vcs.service.database.pool.DbConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.naming.NamingException;

public class StudioDAO extends DBConnections {

     public boolean start() throws NamingException {
        return super.start();
    }


    public String insertStudioRecord(StudioRecordInfo item) {
        String resp = Constants.SYSTEM_ERROR;

        PreparedStatement stmt = null;
        PreparedStatement stmt_up_point = null;
        PreparedStatement stmt_log = null;
        DbConnection cnn = null;
        String sql = "insert into studio_record(user_id,msisdn,record_path,exchange_point,competition_id)"
                + " values(?, ?, ?, ? ,?)";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, item.getUserId());
            stmt.setString(++i, item.getMsisdn());
            stmt.setString(++i, item.getRecordPath());
            stmt.setInt(++i, item.getExchangePoint());
            stmt.setInt(++i, item.getCompetitionId());
            stmt.executeUpdate();

            if (item.getExchangePoint() > 0) {
                /*
                 * Tru diem tu tai khoan
                 */
                sql = "update sub_point set before_total_point = total_point, total_point = total_point - ?, updated_date = now() where msisdn = ?";
                stmt_up_point = cnn.getConnection().prepareStatement(sql);
                stmt_up_point.setInt(1, item.getExchangePoint());
                stmt_up_point.setString(2, item.getMsisdn());
                stmt_up_point.executeUpdate();

                /*
                 * Luu lich su tru diem 
                 */
                sql = "insert into sub_point_history(msisdn, point, action, package_id, sub_package_id) "
                        + " values(?, ?, ?, 0, 0)";
                stmt_log = cnn.getConnection().prepareStatement(sql);
                stmt_log.setString(1, item.getMsisdn());
                stmt_log.setInt(2, 0 - item.getExchangePoint());
                stmt_log.setInt(3, Constants.POINT_ACTION_STUDIO);
                stmt_log.executeUpdate();
            }

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in insertStudioRecord into database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            closeStatement(stmt_up_point);
            closeStatement(stmt_log);
            freeConnection(cnn);
        }

        return resp;
    }

    public String addRecordToStudioCollection(StudioRecordInfo item) {
        String resp = Constants.SYSTEM_ERROR;
        ResultSet rs = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt_up = null;
        PreparedStatement stmt_record = null;
        DbConnection cnn = null;
        String sql = "select status from studio_collection where record_id = ? and msisdn = ? ";
        String sql_record = "update studio_record set delete_collection = 0 where record_id = ? and msisdn = ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt_record = cnn.getConnection().prepareStatement(sql_record);
            stmt_record.setInt(++i, item.getRecordId());
            stmt_record.setString(++i, item.getMsisdn());
            stmt_record.executeUpdate();

            if (item.getIsOwner() != 1) {
                i = 0;
                stmt = cnn.getConnection().prepareStatement(sql);
                stmt.setInt(++i, item.getRecordId());
                stmt.setString(++i, item.getMsisdn());

                rs = stmt.executeQuery();
                if (rs.next()) {
                    int status = rs.getInt("status");
                    if (status != 1) {
                        i = 0;
                        sql = "update studio_collection set status = 1, updated_date = now() where record_id = ? and msisdn = ?";
                        stmt_up = cnn.getConnection().prepareStatement(sql);
                        stmt_up.setInt(++i, item.getRecordId());
                        stmt_up.setString(++i, item.getMsisdn());
                        stmt_up.executeUpdate();
                    }
                } else {
                    i = 0;
                    sql = "insert into studio_collection(record_id, msisdn, user_id) values(?, ?, ?)";
                    stmt_up = cnn.getConnection().prepareStatement(sql);
                    stmt_up.setInt(++i, item.getRecordId());
                    stmt_up.setString(++i, item.getMsisdn());
                    stmt_up.setInt(++i, item.getUserId());
                    stmt_up.executeUpdate();
                }
            }

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in addRecordToStudioCollection ", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeStatement(stmt_up);
            closeStatement(stmt_record);
            freeConnection(cnn);
        }

        return resp;
    }

    public String removeRecordFromStudioCollection(int recordId, String msisdn) {
        String resp = Constants.SYSTEM_ERROR;
        PreparedStatement stmt = null;
        PreparedStatement stmt_record = null;
        DbConnection cnn = null;
        String sql = "update studio_collection set status = -1 where record_id = ? and msisdn = ? ";
        String sql_record = "update studio_record set delete_collection = 1 where record_id = ? and msisdn = ?";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, recordId);
            stmt.setString(++i, msisdn);
            stmt.executeUpdate();

            i = 0;
            stmt_record = cnn.getConnection().prepareStatement(sql_record);
            stmt_record.setInt(++i, recordId);
            stmt_record.setString(++i, msisdn);
            stmt_record.executeUpdate();

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in removeRecordFromStudioCollection ", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            closeStatement(stmt_record);
            freeConnection(cnn);
        }

        return resp;
    }

    public ArrayList<StudioRecordInfo> getListTopStudioRecord(String msisdn, int limit) {
        ArrayList<StudioRecordInfo> list = new ArrayList<StudioRecordInfo>();
        logger.info(" >>>>>>> StudioDao getListTopStudioRecord");
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select t1.*, t2.record_id_listened from ( " +
                    "    (select studio_record.*  " +
                    "     from studio_record  " +
                    "     where approve_status = ?  " +
                    "     order by vote_count desc, created_date desc limit 0,?  " +
                    "    ) union all  " +
                    "    ( select * from ( " +
                    "    select studio_record.*  " +
                    "    from studio_record  " +
                    "     where approve_status = ?  " +
                    "    and record_id not in ( " +
                    "      select record_id from ( " +
                    "     select record_id " +
                    "     from studio_record  " +
                    "     where approve_status = ? " +
                    "     order by vote_count desc, created_date desc limit 0,? " +
                    "      ) b  " +
                    "    ) " +
                    "   ) a1 order by listen_duration desc, created_date desc limit 0,? " +
                    "    ) " +
                    " ) t1 left join ( " +
                    "   select record_id record_id_listened  " +
                    "   from studio_listened  " +
                    "   where msisdn = ?  " +
                    " ) t2 on t1.record_id = t2.record_id_listened";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, StudioRecordInfo.STATUS_APPROVED);
            stmt.setInt(++i, limit / 2);
            stmt.setInt(++i, StudioRecordInfo.STATUS_APPROVED);
            stmt.setInt(++i, StudioRecordInfo.STATUS_APPROVED);
            stmt.setInt(++i, limit / 2);
            stmt.setInt(++i, limit / 2);
            stmt.setString(++i, msisdn);
            logger.info(" >>>>>>> StudioDao sql :"+stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                StudioRecordInfo item = new StudioRecordInfo();
                item.setRecordId(rs.getInt("record_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setVoteCount(rs.getInt("vote_count"));
                if (rs.getInt("record_id_listened") > 0) {
                    item.setListened(1);
                }

                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getListTopStudioRecord from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public ArrayList<StudioRecordInfo> getListAllRecord(String msisdn, int limit) {
        ArrayList<StudioRecordInfo> list = new ArrayList<StudioRecordInfo>();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select t.* from ("
                + "  select t1.*, t2.record_id_listened from ("
                + "    select *"
                + "    from studio_record"
                + "    where approve_status = ? "
                + "  ) t1 left join ("
                + "    select record_id record_id_listened"
                + "    from studio_listened"
                + "    where msisdn = ? "
                + "  ) t2 on t1.record_id = t2.record_id_listened"
                + ")t order by t.record_id_listened is null desc, t.record_id_listened asc limit 0,?";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, StudioRecordInfo.STATUS_APPROVED);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, limit);

            rs = stmt.executeQuery();
            while (rs.next()) {
                StudioRecordInfo item = new StudioRecordInfo();
                item.setRecordId(rs.getInt("record_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setVoteCount(rs.getInt("vote_count"));
                if (rs.getInt("record_id_listened") > 0) {
                    item.setListened(1);
                }
                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getListAllRecord from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public String addVoteForRecord(StudioVoteInfo item) {
        String resp = Constants.SYSTEM_ERROR;
        ResultSet rs = null;
        ResultSet rs_total = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt_up = null;
        PreparedStatement stmt_record = null;
        PreparedStatement stmt_total = null;
        PreparedStatement stmt_up_point = null;
        PreparedStatement stmt_log = null;
        DbConnection cnn = null;
        String sql = "select count(*) total from studio_vote where record_id = ? and msisdn = ?";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, item.getRecordId());
            stmt.setString(++i, item.getMsisdn());

            rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("total") > 0) {
                resp = Constants.DATA_EXIST;
                i = 0;
                sql = "select vote_count total from studio_record where record_id = ?";
                stmt_total = cnn.getConnection().prepareStatement(sql);
                stmt_total.setInt(++i, item.getRecordId());
                rs_total = stmt_total.executeQuery();
                if (rs_total.next()) {
                    item.setVoteCount(rs_total.getInt("total"));
                }
            } else {
                i = 0;
                sql = "insert into studio_vote(record_id, receiver_user_id, receiver, voted_user_id, msisdn, point) values(?, ?, ?, ?, ?, ?)";
                stmt_up = cnn.getConnection().prepareStatement(sql);
                stmt_up.setInt(++i, item.getRecordId());
                stmt_up.setInt(++i, item.getReceivedUserId());
                stmt_up.setString(++i, item.getReceiver());
                stmt_up.setInt(++i, item.getVotedUserId());
                stmt_up.setString(++i, item.getMsisdn());
                stmt_up.setInt(++i, item.getPoint());
                stmt_up.executeUpdate();

                i = 0;
                sql = "update studio_record set vote_count = vote_count + 1 where record_id = ?";
                stmt_record = cnn.getConnection().prepareStatement(sql);
                stmt_record.setInt(++i, item.getRecordId());
                stmt_record.executeUpdate();

                /*
                 * Cong diem ca nhan tac gia
                 */
                if (item.getPoint() > 0) {
                    /*
                     * Cong diem vao tai khoan
                     */
                    sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, updated_date = now() where msisdn = ?";
                    stmt_up_point = cnn.getConnection().prepareStatement(sql);
                    stmt_up_point.setInt(1, item.getPoint());
                    stmt_up_point.setInt(2, item.getPoint());
                    stmt_up_point.setString(3, item.getReceiver());
                    stmt_up_point.executeUpdate();

                    /*
                     * Luu lich su cong diem 
                     */
                    sql = "insert into sub_point_history(msisdn, point, action, package_id, sub_package_id) "
                            + " values(?, ?, ?, 0, 0)";
                    stmt_log = cnn.getConnection().prepareStatement(sql);
                    stmt_log.setString(1, item.getReceiver());
                    stmt_log.setInt(2, item.getPoint());
                    stmt_log.setInt(3, Constants.POINT_ACTION_STUDIO);
                    stmt_log.executeUpdate();
                }

                cnn.getConnection().commit();

                i = 0;
                sql = "select vote_count total from studio_record where record_id = ?";
                stmt_total = cnn.getConnection().prepareStatement(sql);
                stmt_total.setInt(++i, item.getRecordId());
                rs_total = stmt_total.executeQuery();
                if (rs_total.next()) {
                    item.setVoteCount(rs_total.getInt("total"));
                }

                resp = Constants.SUCCESS;
            }

        } catch (Exception e) {
            logger.error("error in addVoteForRecord ", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeResultSet(rs_total);

            closeStatement(stmt);
            closeStatement(stmt_up);
            closeStatement(stmt_record);
            closeStatement(stmt_total);
            closeStatement(stmt_up_point);
            closeStatement(stmt_log);
            freeConnection(cnn);
        }

        return resp;
    }

    public StudioRecordInfo getRecordInfo(int recordId, boolean includeDraft) {
        StudioRecordInfo item = new StudioRecordInfo();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from studio_record where record_id = ? ";
        if (!includeDraft) {
            sql += "and approve_status = ?";
        }

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, recordId);
            if (!includeDraft) {
                stmt.setInt(++i, StudioRecordInfo.STATUS_APPROVED);
            }

            rs = stmt.executeQuery();
            if (rs.next()) {
                item.setRecordId(rs.getInt("record_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setVoteCount(rs.getInt("vote_count"));
                item.setCreatedDate(rs.getTimestamp("created_date"));
                item.setRefundPoint(rs.getInt("refund_point"));
                item.setExchangePoint(rs.getInt("exchange_point"));
            }

        } catch (Exception e) {
            logger.error("error in getRecordInfo from database", e);
            item = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return item;
    }

    public String processApproveRecord(StudioRecordInfo item) {
        String resp = Constants.SYSTEM_ERROR;

        PreparedStatement stmt_up = null;
        PreparedStatement stmt_up_point = null;
        PreparedStatement stmt_log = null;
        DbConnection cnn = null;

        String sql = "update studio_record set approve_status = ?, approved_date = now() where record_id = ? ";

        int i = 0;
        int exchangePoint = item.getExchangePoint();
        int refundStatus = item.getRefundPoint();
        String msisdn = item.getMsisdn();

        try {
            cnn = getConnection();
            /*
             * Refund point if not approve
             */
            if (item.getApproveStatus() == StudioRecordInfo.STATUS_NOT_APPROVE
                    && exchangePoint > 0
                    && refundStatus == 0) {
                /*
                 * Cong diem vao tai khoan
                 */
                sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, updated_date = now() where msisdn = ?";
                stmt_up_point = cnn.getConnection().prepareStatement(sql);
                stmt_up_point.setInt(1, exchangePoint);
                stmt_up_point.setInt(2, exchangePoint);
                stmt_up_point.setString(3, msisdn);
                stmt_up_point.executeUpdate();

                /*
                 * Luu lich su cong diem 
                 */
                sql = "insert into sub_point_history(msisdn, point, action, package_id, sub_package_id) "
                        + " values(?, ?, ?, 0, 0)";
                stmt_log = cnn.getConnection().prepareStatement(sql);
                stmt_log.setString(1, msisdn);
                stmt_log.setInt(2, exchangePoint);
                stmt_log.setInt(3, Constants.POINT_ACTION_STUDIO);
                stmt_log.executeUpdate();

                /*
                 * cap nhat trang thai refund
                 */
                sql = "update studio_record set approve_status = ?, approved_date = now(), refund_point = 1 where record_id = ? ";
            }

            stmt_up = cnn.getConnection().prepareStatement(sql);
            stmt_up.setInt(++i, item.getApproveStatus());
            stmt_up.setInt(++i, item.getRecordId());
            stmt_up.executeUpdate();

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;

        } catch (Exception e) {
            logger.error("error in processApproveRecord ", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt_up);
            closeStatement(stmt_up_point);
            closeStatement(stmt_log);
            freeConnection(cnn);
        }

        return resp;
    }

    public String updateListenHistory(StudioListenInfo item) {
        String resp = Constants.SYSTEM_ERROR;
        ResultSet rs = null;
        PreparedStatement stmt_get = null;
        PreparedStatement stmt_up = null;
        PreparedStatement stmt_ins = null;
        PreparedStatement stmt_his = null;
        DbConnection cnn = null;
        String sql = "update studio_record set listen_count = listen_count + 1, listen_duration = listen_duration + ? where record_id = ? ";
        String sql_get = "select * from studio_listened where msisdn = ? and record_id = ?";
        String sql_his = "insert into studio_listen_his(record_id, user_id, msisdn, duration, channel, package_id, sub_package_id) values(?, ?, ?, ?, ?, ?, ?)";

        int i = 0;
        try {
            cnn = getConnection();
            stmt_up = cnn.getConnection().prepareStatement(sql);
            stmt_up.setInt(++i, item.getDuration());
            stmt_up.setInt(++i, item.getRecordId());
            stmt_up.executeUpdate();

            i = 0;
            stmt_get = cnn.getConnection().prepareStatement(sql_get);
            stmt_get.setString(++i, item.getMsisdn());
            stmt_get.setInt(++i, item.getRecordId());
            rs = stmt_get.executeQuery();
            if (!rs.next()) {
                i = 0;
                sql = "insert into studio_listened(msisdn, user_id, record_id) values(?, ?, ?)";
                stmt_ins = cnn.getConnection().prepareStatement(sql);
                stmt_ins.setString(++i, item.getMsisdn());
                stmt_ins.setInt(++i, item.getUserId());
                stmt_ins.setInt(++i, item.getRecordId());
                stmt_ins.executeUpdate();
            }

            i = 0;
            stmt_his = cnn.getConnection().prepareStatement(sql_his);
            stmt_his.setInt(++i, item.getRecordId());
            stmt_his.setInt(++i, item.getUserId());
            stmt_his.setString(++i, item.getMsisdn());
            stmt_his.setInt(++i, item.getDuration());
            stmt_his.setInt(++i, item.getChannel());
            stmt_his.setInt(++i, item.getPackageId());
            stmt_his.setInt(++i, item.getSubPackageId());
            stmt_his.executeUpdate();

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in updateListenHistory ", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt_up);
            closeStatement(stmt_get);
            closeStatement(stmt_ins);
            closeStatement(stmt_his);
            freeConnection(cnn);
        }

        return resp;
    }

    public ArrayList<StudioRecordInfo> getListRecordFromCollection(String msisdn, int limit) {
        ArrayList<StudioRecordInfo> list = new ArrayList<StudioRecordInfo>();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select t2.* from ("
                + " select record_id "
                + " from studio_collection "
                + " where msisdn = ? "
                + " and status = 1 "
                + ") t1 inner join ("
                + " select * "
                + " from studio_record "
                + " where approve_status = ?"
                + " and msisdn != ? "
                + ") t2 on t1.record_id = t2.record_id "
                + " union all "
                + " select * "
                + " from studio_record"
                + " where approve_status = ? "
                + " and msisdn = ? "
                + " and delete_collection = 0 ";

        sql = "select b.record_id_listened, a.* from (" + sql + ") a left join (select record_id record_id_listened from studio_listened where msisdn = ?) b on a.record_id = b.record_id_listened ";
        sql = "select * from (select row_number() over(order by record_id_listened nulls first, created_date desc) rnum, t3.* from (" 
                + sql + ") t3) t4 order by record_id_listened is null desc, record_id_listened asc, created_date desc limit 0,?";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, StudioRecordInfo.STATUS_APPROVED);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, StudioRecordInfo.STATUS_APPROVED);
            stmt.setString(++i, msisdn);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, limit);

            rs = stmt.executeQuery();
            while (rs.next()) {
                StudioRecordInfo item = new StudioRecordInfo();
                item.setRecordId(rs.getInt("record_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setVoteCount(rs.getInt("vote_count"));
                if (rs.getInt("record_id_listened") > 0) {
                    item.setListened(1);
                }

                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getListRecordFromCollection from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public int getTotalPointOfSub(String msisdn) {
        int totalPoint = 0;

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from sub_point where msisdn = ?";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, msisdn);

            rs = stmt.executeQuery();
            if (rs.next()) {
                totalPoint = rs.getInt("total_point");
            }

        } catch (Exception e) {
            logger.error("error in getTotalPointOfSub from database", e);
            totalPoint = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return totalPoint;
    }
     public int getCompetitionId() {
        int id = 0;

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select id from studio_competition where status = 1 and end_time >= DATE_FORMAT(now(),\"%Y-%m-%d\")";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);

            rs = stmt.executeQuery();
            if (rs.next()) {
                id = rs.getInt("id");
            }
        } catch (Exception e) {
            logger.error("error in getCompetitionId from database", e);
            id = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return id;
    }

    public int getPointExchange() {
        int totalPoint = 0;

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select point from studio_competition where status = 1 and end_time >= DATE_FORMAT(now(),'%Y-%m-%d')";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);

            rs = stmt.executeQuery();
            if (rs.next()) {
                totalPoint = rs.getInt("point");
            }

        } catch (Exception e) {
            logger.error("error in getPointExchange from database", e);
            totalPoint = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return totalPoint;
    }
}
