package com.vega.service.api.db;

import com.vega.service.api.common.Constants;
import com.vega.service.api.object.IdolAwardInfo;
import com.vega.service.api.object.IdolCompetitionInfo;
import com.vega.service.api.object.IdolListenInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import net.sf.ehcache.Element;


import com.vega.service.api.object.IdolRecordInfo;
import com.vega.service.api.object.IdolSubjectInfo;
import com.vega.service.api.object.IdolVoteInfo;
import com.vega.vcs.service.database.pool.DbConnection;
import javax.naming.NamingException;

public class IdolDAO extends DBConnections {

    public void init() throws Exception {
        super.start();
    }

    public synchronized IdolCompetitionInfo getActiveIdolCompetition() {
        IdolCompetitionInfo compet = new IdolCompetitionInfo();

        String keyName = "ACTIVE_IDOL_COMPET";
        Element element = cache.get(keyName);
        if (element != null) {
            logger.debug(keyName + " from cache");
            compet = ((IdolCompetitionInfo) element.getObjectValue());
            try {
                compet = (IdolCompetitionInfo) compet.clone();
                if (compet != null) {
                    return compet;
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from idol_competition where status = ? and first_round_start <= now() "
                + " and second_round_end > now() ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, IdolCompetitionInfo.STATUS_ON);

            rs = stmt.executeQuery();
            if (rs.next()) {
                compet.setCompetitionId(rs.getInt("competition_id"));
                compet.setFirstRoundStart(rs.getTimestamp("first_round_start"));
                compet.setFirstRoundEnd(rs.getTimestamp("first_round_end"));
                compet.setSecondRoundStart(rs.getTimestamp("second_round_start"));
                compet.setSecondRoundEnd(rs.getTimestamp("second_round_end"));
                compet.setMaxRecordFirstRound(rs.getInt("max_record_first_round"));
                compet.setMaxRecordSecondRound(rs.getInt("max_record_second_round"));
                compet.setPointVoteFirstRound(rs.getInt("point_vote_first_round"));
                compet.setPointVoteSecondRound(rs.getInt("point_vote_second_round"));
                compet.setPromtPause(rs.getString("promt_pause"));
                compet.setRuleKey1FirstRound(rs.getInt("rule_key1_first_round"));
                compet.setRuleKey2FirstRound(rs.getInt("rule_key2_first_round"));
                compet.setRuleKey3FirstRound(rs.getInt("rule_key3_first_round"));
                compet.setRuleKey1SecondRound(rs.getInt("rule_key1_second_round"));
                compet.setRuleKey2SecondRound(rs.getInt("rule_key2_second_round"));
            }
        } catch (Exception e) {
            logger.error("error in getActiveIdolCompetition from database", e);
            compet = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        if (compet.getCompetitionId() > 0) {
            // get thong tin chu de thi idol
            IdolSubjectInfo idolSubjectInfo = getIdolSubject(compet.getCompetitionId());
            if (idolSubjectInfo != null) {
                compet.setPromtSubject(idolSubjectInfo.getPathPromt());
                compet.setPromptRecording(idolSubjectInfo.getPromtRecording());
            }
            cache.put(new Element(keyName, compet));

        }
        return compet;
    }

    public IdolCompetitionInfo getIdolCompetitionById(int competId) {
        IdolCompetitionInfo compet = new IdolCompetitionInfo();

        String keyName = "getIdolCompetitionById_" + competId;
        Element element = cache.get(keyName);
        if (element != null) {
            logger.debug(keyName + " from cache");
            compet = ((IdolCompetitionInfo) element.getObjectValue());
            try {
                compet = (IdolCompetitionInfo) compet.clone();
                if (compet != null) {
                    return compet;
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from idol_competition where competition_id = ? and status = ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, competId);
            stmt.setInt(++i, IdolCompetitionInfo.STATUS_ON);

            rs = stmt.executeQuery();
            if (rs.next()) {
                compet.setCompetitionId(rs.getInt("competition_id"));
                compet.setFirstRoundStart(rs.getTimestamp("first_round_start"));
                compet.setFirstRoundEnd(rs.getTimestamp("first_round_end"));
                compet.setSecondRoundStart(rs.getTimestamp("second_round_start"));
                compet.setSecondRoundEnd(rs.getTimestamp("second_round_end"));
                compet.setMaxRecordFirstRound(rs.getInt("max_record_first_round"));
                compet.setMaxRecordSecondRound(rs.getInt("max_record_second_round"));
                compet.setPointVoteFirstRound(rs.getInt("point_vote_first_round"));
                compet.setPointVoteSecondRound(rs.getInt("point_vote_second_round"));
                compet.setPromtPause(rs.getString("promt_pause"));
                compet.setRuleKey1FirstRound(rs.getInt("rule_key1_first_round"));
                compet.setRuleKey2FirstRound(rs.getInt("rule_key2_first_round"));
                compet.setRuleKey3FirstRound(rs.getInt("rule_key3_first_round"));
                compet.setRuleKey1SecondRound(rs.getInt("rule_key1_second_round"));
                compet.setRuleKey2SecondRound(rs.getInt("rule_key2_second_round"));

                cache.put(new Element(keyName, compet));
            }
        } catch (Exception e) {
            logger.error("error in getIdolCompetitionById from database", e);
            compet = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return compet;
    }

    public ArrayList<IdolRecordInfo> getListApprovedRecord(int competitionId, String msisdn, int roundNo) {
        ArrayList<IdolRecordInfo> list = new ArrayList<IdolRecordInfo>();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from idol_record where competition_id = ? and msisdn = ? "
                + " and approve_status > ? and delete_status = 0 ";

        if (roundNo > 1) {
            sql += " and second_round_status = ? ";
        }
        sql += " order by created_date desc ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, competitionId);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
            if (roundNo > 1) {
                stmt.setInt(++i, IdolRecordInfo.SECOND_ROUND_ON);
            }

            rs = stmt.executeQuery();
            while (rs.next()) {
                IdolRecordInfo item = new IdolRecordInfo();
                item.setRecordId(rs.getInt("record_id"));
                item.setCompetitionId(rs.getInt("competition_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setFirstTopStatus(rs.getInt("first_top_status"));
                item.setSecondTopStatus(rs.getInt("second_top_status"));
                item.setSecondRoundStatus(rs.getInt("second_round_status"));
                item.setQuantityTop(rs.getInt("quantity_top"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setFirstVoteCount(rs.getInt("first_vote_count"));
                item.setSecondVoteCount(rs.getInt("second_vote_count"));
                item.setRecordCode(rs.getString("record_code"));
                item.setQuantityTop1(rs.getInt("quantity_top_1"));
                item.setQuantityTop2(rs.getInt("quantity_top_2"));

                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getListApprovedRecord from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public ArrayList<IdolRecordInfo> getListRecordIdol(int competitionId, String msisdn, int roundNo) {
        ArrayList<IdolRecordInfo> list = new ArrayList<IdolRecordInfo>();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;

        IdolSubjectInfo idolSubject = getIdolSubject(competitionId);
        String sqlWhereSubject = "";
        if (idolSubject != null) {
            sqlWhereSubject = "and created_date between ? and ?";
        }

        String sql = "select * from idol_record where competition_id = ? and msisdn = ? "
                + // " and approve_status > ? " +
                " and delete_status = 0 ";
        if (roundNo > 1) {
            sql += " and second_round_status = ?  ";
            sql += sqlWhereSubject;
        }
        sql += "  order by created_date desc  ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, competitionId);
            stmt.setString(++i, msisdn);
            // stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
            if (roundNo > 1) {
                stmt.setInt(++i, IdolRecordInfo.SECOND_ROUND_ON);
                if (idolSubject != null) {
                    stmt.setTimestamp(++i, new Timestamp(idolSubject.getBegin_at().getTime()));
                    stmt.setTimestamp(++i, new Timestamp(idolSubject.getEnd_at().getTime()));
                }
            }
            logger.info("Dao getListRecordIdol :" + stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                IdolRecordInfo item = new IdolRecordInfo();
                item.setRecordId(rs.getInt("record_id"));
                item.setCompetitionId(rs.getInt("competition_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setFirstTopStatus(rs.getInt("first_top_status"));
                item.setSecondTopStatus(rs.getInt("second_top_status"));
                item.setSecondRoundStatus(rs.getInt("second_round_status"));
                item.setQuantityTop(rs.getInt("quantity_top"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setFirstVoteCount(rs.getInt("first_vote_count"));
                item.setSecondVoteCount(rs.getInt("second_vote_count"));
                item.setRecordCode(rs.getString("record_code"));
                item.setQuantityTop1(rs.getInt("quantity_top_1"));
                item.setQuantityTop2(rs.getInt("quantity_top_2"));

                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getListApprovedRecord from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public boolean checkStatusRoundStatus(int competitionId, String msisdn) {
        boolean result = false;

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select count(*) total from idol_record where competition_id = ? "
                + "and msisdn = ? and approve_status > ? and second_round_status = ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, competitionId);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
            stmt.setInt(++i, IdolRecordInfo.SECOND_ROUND_ON);

            rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("total") > 0) {
                result = true;
            }

        } catch (Exception e) {
            logger.error("error in checkStatusRoundStatus from database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return result;
    }

    public String insertIdolRecord(IdolRecordInfo item) {
        String resp = Constants.SYSTEM_ERROR;

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "insert into idol_record(competition_id, user_id, msisdn, record_path,second_round_status) "
                + "values(?, ?, ?, ?, ?)";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, item.getCompetitionId());
            stmt.setInt(++i, item.getUserId());
            stmt.setString(++i, item.getMsisdn());
            stmt.setString(++i, item.getRecordPath());
            stmt.setInt(++i, item.getSecondRoundStatus());
            stmt.executeUpdate();

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in insertIdolRecord into database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return resp;
    }

    public String removeIdolRecord(int recordId, String msisdn) {
        String resp = Constants.SYSTEM_ERROR;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "update idol_record set delete_status = 1 where record_id = ? and msisdn = ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, recordId);
            stmt.setString(++i, msisdn);
            stmt.executeUpdate();

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in removeIdolRecord from database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return resp;
    }

    public String addRecordToIdolCollection(IdolRecordInfo item) {
        String resp = Constants.SYSTEM_ERROR;
        ResultSet rs = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt_up = null;
        DbConnection cnn = null;
        String sql = "select status from idol_collection where record_id = ? and msisdn = ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, item.getRecordId());
            stmt.setString(++i, item.getMsisdn());

            rs = stmt.executeQuery();
            if (rs.next()) {
                int status = rs.getInt("status");
                if (status != 1) {
                    i = 0;
                    sql = "update idol_collection set status = 1, updated_date = now() where record_id = ? and msisdn = ?";
                    stmt_up = cnn.getConnection().prepareStatement(sql);
                    stmt_up.setInt(++i, item.getRecordId());
                    stmt_up.setString(++i, item.getMsisdn());
                    stmt_up.executeUpdate();
                }
            } else {
                i = 0;
                sql = "insert into idol_collection(record_id, msisdn, user_id, competition_id) values(?, ?, ?, ?)";
                stmt_up = cnn.getConnection().prepareStatement(sql);
                stmt_up.setInt(++i, item.getRecordId());
                stmt_up.setString(++i, item.getMsisdn());
                stmt_up.setInt(++i, item.getUserId());
                stmt_up.setInt(++i, item.getCompetitionId());
                stmt_up.executeUpdate();
            }

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in addRecordToIdolCollection ", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return resp;
    }

    public String removeRecordFromIdolCollection(int recordId, String msisdn) {
        String resp = Constants.SYSTEM_ERROR;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "update idol_collection set status = -1 where record_id = ? and msisdn = ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, recordId);
            stmt.setString(++i, msisdn);
            stmt.executeUpdate();

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in removeRecordFromIdolCollection ", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return resp;
    }

    public ArrayList<IdolRecordInfo> getListTopRecordByRound(int competitionId, String msisdn, int roundNo) {
        ArrayList<IdolRecordInfo> list = new ArrayList<IdolRecordInfo>();
        // lay thong tin ve chu de
        IdolSubjectInfo currentIdolSubject = getIdolSubject(competitionId);
        String sqlWhereSubject = "";
        if (currentIdolSubject != null) {
            sqlWhereSubject = " and created_date between ? and ? ";
        }

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;

        String sql = "select t1.*, t2.record_id_listened from ( select * from idol_record where competition_id = ? "
                + "and approve_status > ? and delete_status = 0 ";
        if (roundNo <= 1) {
            sql += " and first_top_status = ? ";
        } else {
            sql += " and second_top_status = ? and second_round_status = ? ";
            sql += sqlWhereSubject;
        }
        sql += ") t1 left join (select record_id record_id_listened from idol_listened where msisdn = ? "
                + ") t2 on t1.record_id = t2.record_id_listened ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, competitionId);
            stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
            stmt.setInt(++i, IdolRecordInfo.TOP_STATUS_ON);
            if (roundNo > 1) {
                stmt.setInt(++i, IdolRecordInfo.SECOND_ROUND_ON);
            }
            if (currentIdolSubject != null) {
                stmt.setTimestamp(++i, new Timestamp(currentIdolSubject.getBegin_at().getTime()));
                stmt.setTimestamp(++i, new Timestamp(currentIdolSubject.getEnd_at().getTime()));
            }
            stmt.setString(++i, msisdn);

            rs = stmt.executeQuery();
            while (rs.next()) {
                IdolRecordInfo item = new IdolRecordInfo();
                item.setRecordId(rs.getInt("record_id"));
                item.setCompetitionId(rs.getInt("competition_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setFirstTopStatus(rs.getInt("first_top_status"));
                item.setSecondTopStatus(rs.getInt("second_top_status"));
                item.setSecondRoundStatus(rs.getInt("second_round_status"));
                item.setQuantityTop(rs.getInt("quantity_top"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setFirstVoteCount(rs.getInt("first_vote_count"));
                item.setSecondVoteCount(rs.getInt("second_vote_count"));
                item.setRecordCode(rs.getString("record_code"));
                item.setQuantityTop1(rs.getInt("quantity_top_1"));
                item.setQuantityTop2(rs.getInt("quantity_top_2"));
                if (rs.getInt("record_id_listened") > 0) {
                    item.setListened(1);
                }

                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getListTopRecordByRound from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public IdolSubjectInfo getIdolSubject(int competitionId) {

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        IdolSubjectInfo idolSubject = null;
        String sql = "select * from idol_subject where competition_id = ? and DATE_FORMAT(now(),'%Y-%m-%d') >= begin_at and DATE_FORMAT(now(),'%Y-%m-%d') <= end_at";
        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, competitionId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                idolSubject = new IdolSubjectInfo();
                idolSubject.setCompetitionId(competitionId);
                idolSubject.setSubjectName(rs.getString("subject_name"));
                idolSubject.setBegin_at(new java.sql.Date(rs.getTimestamp("begin_at").getTime()));
                idolSubject.setEnd_at(new java.sql.Date(rs.getTimestamp("end_at").getTime()));
                String promtSubject = rs.getString("promt");
                if (promtSubject == null) {
                    promtSubject = "";
                }
                idolSubject.setPathPromt(promtSubject);
                String promtRecording = rs.getString("promt_recording");
                idolSubject.setPromtRecording(promtRecording == null ? "" : promtRecording);
                break;
            }
        } catch (Exception e) {
            logger.error("error in getIdolSubject from database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return idolSubject;
    }

    public ArrayList<IdolRecordInfo> getListNewsestRecordByRound(int competitionId, String msisdn, int roundNo, int limit) {
        ArrayList<IdolRecordInfo> list = new ArrayList<IdolRecordInfo>();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select t1.*, t2.record_id_listened from "
                + "(select * from (select idol_record.* "
                + "from idol_record where competition_id = ? and approve_status > ? ";
        if (roundNo > 1) {
            sql += " and second_round_status = ? ";
        }
        sql += " and delete_status = 0) a limit 0,? ";
        sql += ") t1 left join (select record_id record_id_listened from idol_listened where msisdn = ?) t2 on t1.record_id = t2.record_id_listened ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, competitionId);
            stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
            if (roundNo > 1) {
                stmt.setInt(++i, IdolRecordInfo.SECOND_ROUND_ON);
            }
            stmt.setInt(++i, limit);
            stmt.setString(++i, msisdn);

            rs = stmt.executeQuery();
            while (rs.next()) {
                IdolRecordInfo item = new IdolRecordInfo();
                item.setRecordId(rs.getInt("record_id"));
                item.setCompetitionId(rs.getInt("competition_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setFirstTopStatus(rs.getInt("first_top_status"));
                item.setSecondTopStatus(rs.getInt("second_top_status"));
                item.setSecondRoundStatus(rs.getInt("second_round_status"));
                item.setQuantityTop(rs.getInt("quantity_top"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setFirstVoteCount(rs.getInt("first_vote_count"));
                item.setSecondVoteCount(rs.getInt("second_vote_count"));
                item.setRecordCode(rs.getString("record_code"));
                item.setQuantityTop1(rs.getInt("quantity_top_1"));
                item.setQuantityTop2(rs.getInt("quantity_top_2"));
                if (rs.getInt("record_id_listened") > 0) {
                    item.setListened(1);
                }
                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getListNewsestRecordByRound from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public ArrayList<IdolRecordInfo> getListAllRecordByRound(int competitionId, String msisdn, int roundNo) {

        // lay thong tin ve chu de
        IdolSubjectInfo idolSubject = getIdolSubject(competitionId);

        String sqlWhereSubject = "";
        if (idolSubject != null) {
            sqlWhereSubject = " and created_date between ? and ? ";
        }

        ArrayList<IdolRecordInfo> list = new ArrayList<IdolRecordInfo>();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select t1.*, t2.record_id_listened from ( "
                + " select * from idol_record "
                + " where competition_id = ?  and approve_status > ?"
                + " and delete_status = 0 ";
        if (roundNo > 1) {
            sql += " and second_round_status = ? ";
            sql += sqlWhereSubject;
        }

        sql += " limit 0,200) t1 left join ("
                + " select record_id as record_id_listened "
                + " from idol_listened where msisdn = ? "
                + ") t2 on t1.record_id = t2.record_id_listened ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, competitionId);
            stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
            if (roundNo > 1) {
                stmt.setInt(++i, IdolRecordInfo.SECOND_ROUND_ON);
            }

            if (idolSubject != null) {
                stmt.setTimestamp(++i, new Timestamp(idolSubject.getBegin_at().getTime()));
                stmt.setTimestamp(++i, new Timestamp(idolSubject.getEnd_at().getTime()));
            }
            stmt.setString(++i, msisdn);

            rs = stmt.executeQuery();
            while (rs.next()) {
                IdolRecordInfo item = new IdolRecordInfo();
                item.setRecordId(rs.getInt("record_id"));
                item.setCompetitionId(rs.getInt("competition_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setFirstTopStatus(rs.getInt("first_top_status"));
                item.setSecondTopStatus(rs.getInt("second_top_status"));
                item.setSecondRoundStatus(rs.getInt("second_round_status"));
                item.setQuantityTop(rs.getInt("quantity_top"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setFirstVoteCount(rs.getInt("first_vote_count"));
                item.setSecondVoteCount(rs.getInt("second_vote_count"));
                item.setRecordCode(rs.getString("record_code"));
                item.setQuantityTop1(rs.getInt("quantity_top_1"));
                item.setQuantityTop2(rs.getInt("quantity_top_2"));
                if (rs.getInt("record_id_listened") > 0) {
                    item.setListened(1);
                }
                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getListAllRecordByRound from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public String addVoteForRecord(IdolVoteInfo item) {
        logger.info(">>> addVoteForRecord Dao : "+ item.getRoundNo());
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
        String sql = "select count(*) total from idol_vote where record_id = ? and msisdn = ?";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, item.getRecordId());
            stmt.setString(++i, item.getMsisdn());
              logger.info(">>> addVoteForRecord stmt : "+ stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("total") > 0) {
                if (item.getRoundNo() <= 1) {
                    sql = "select first_vote_count as total from idol_record where record_id = ? ";
                } else {
                    sql = "select second_vote_count as total from idol_record where record_id = ? ";
                }
                i = 0;
                stmt_total = cnn.getConnection().prepareStatement(sql);
                stmt_total.setInt(++i, item.getRecordId());
                 logger.info(">>> addVoteForRecord stmt_total : "+ stmt_total.toString());
                rs_total = stmt_total.executeQuery();
                if (rs_total.next()) {
                    item.setVoteCount(rs_total.getInt("total"));
                }

                resp = Constants.DATA_EXIST;
            } else {
                i = 0;
                sql = "insert into idol_vote(competition_id, record_id, received_user_id, receiver, voted_user_id, msisdn, point) "
                        + "values(?, ?, ?, ?, ?, ?, ?)";
                stmt_up = cnn.getConnection().prepareStatement(sql);
                stmt_up.setInt(++i, item.getCompetitionId());
                stmt_up.setInt(++i, item.getRecordId());
                stmt_up.setInt(++i, item.getReceivedUserId());
                stmt_up.setString(++i, item.getReceiver());
                stmt_up.setInt(++i, item.getVotedUserId());
                stmt_up.setString(++i, item.getMsisdn());
                stmt_up.setInt(++i, item.getPoint());
                logger.info(">>> addVoteForRecord stmt_up : "+ stmt_up.toString());
                stmt_up.executeUpdate();

                i = 0;
                if (item.getRoundNo() <= 1) {
                    sql = "update idol_record set first_vote_count = first_vote_count + 1 where record_id = ?";
                } else {
                    sql = "update idol_record set second_vote_count = second_vote_count + 1 where record_id = ?";
                }
                stmt_record = cnn.getConnection().prepareStatement(sql);
                stmt_record.setInt(++i, item.getRecordId());
                logger.info(">>> addVoteForRecord stmt_record : "+ stmt_record.toString());
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
                     logger.info(">>> addVoteForRecord stmt_up_point : "+ stmt_up_point.toString());
                    stmt_up_point.executeUpdate();

                    /*
                     * Luu lich su cong diem 
                     */
                    sql = "insert into sub_point_history(msisdn, point, action, package_id, sub_package_id) values(?, ?, ?, 0, 0)";
                    stmt_log = cnn.getConnection().prepareStatement(sql);
                    stmt_log.setString(1, item.getReceiver());
                    stmt_log.setInt(2, item.getPoint());
                    stmt_log.setInt(3, Constants.POINT_ACTION_IDOL);
                      logger.info(">>> addVoteForRecord stmt_log : "+ stmt_log.toString());
                    stmt_log.executeUpdate();
                }

                cnn.getConnection().commit();

                if (item.getRoundNo() <= 1) {
                    sql = "select first_vote_count total from idol_record where record_id = ? ";
                } else {
                    sql = "select second_vote_count total from idol_record where record_id = ? ";
                }
                i = 0;
                stmt_total = cnn.getConnection().prepareStatement(sql);
                stmt_total.setInt(++i, item.getRecordId());
                 logger.info(">>> addVoteForRecord stmt_total : "+ stmt_total.toString());
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

    public IdolRecordInfo getRecordInfo(int recordId) {
        IdolRecordInfo item = new IdolRecordInfo();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from idol_record where record_id = ? and delete_status = 0";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, recordId);

            rs = stmt.executeQuery();
            if (rs.next()) {
                item.setRecordId(rs.getInt("record_id"));
                item.setCompetitionId(rs.getInt("competition_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setFirstTopStatus(rs.getInt("first_top_status"));
                item.setSecondTopStatus(rs.getInt("second_top_status"));
                item.setSecondRoundStatus(rs.getInt("second_round_status"));
                item.setQuantityTop(rs.getInt("quantity_top"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setFirstVoteCount(rs.getInt("first_vote_count"));
                item.setSecondVoteCount(rs.getInt("second_vote_count"));
                item.setRecordCode(rs.getString("record_code"));
                item.setQuantityTop1(rs.getInt("quantity_top_1"));
                item.setQuantityTop2(rs.getInt("quantity_top_2"));
                item.setCreatedDate(rs.getTimestamp("created_date"));
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

    public IdolRecordInfo getRecordInfoByCode(String code, int competitionId, int roundNo) {
        logger.info("Dao getRecordInfoByCode code :" + code + ",competitionId :" + competitionId + ",roundNo :" + roundNo + "\n");
        IdolRecordInfo item = new IdolRecordInfo();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from idol_record where record_code = ? and competition_id = ? and delete_status = 0 and approve_status > ? ";
        if (roundNo > 1) {
            sql += " and second_round_status = ? ";
        }

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, code);
            stmt.setInt(++i, competitionId);
            stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
            if (roundNo > 1) {
                stmt.setInt(++i, IdolRecordInfo.SECOND_ROUND_ON);
            }

            rs = stmt.executeQuery();
            if (rs.next()) {
                item.setRecordId(rs.getInt("record_id"));
                item.setCompetitionId(rs.getInt("competition_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setFirstTopStatus(rs.getInt("first_top_status"));
                item.setSecondTopStatus(rs.getInt("second_top_status"));
                item.setSecondRoundStatus(rs.getInt("second_round_status"));
                item.setQuantityTop(rs.getInt("quantity_top"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setFirstVoteCount(rs.getInt("first_vote_count"));
                item.setSecondVoteCount(rs.getInt("second_vote_count"));
                item.setRecordCode(rs.getString("record_code"));
                item.setQuantityTop1(rs.getInt("quantity_top_1"));
                item.setQuantityTop2(rs.getInt("quantity_top_2"));
                item.setCreatedDate(rs.getTimestamp("created_date"));
            }

        } catch (Exception e) {
            logger.error("error in getRecordInfoByCode from database", e);
            item = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return item;
    }

    public String processApprovedForIdolRecord(IdolRecordInfo item) {
        String resp = Constants.SYSTEM_ERROR;
        ResultSet rs = null;
        PreparedStatement stmt_get = null;
        PreparedStatement stmt_up = null;
        PreparedStatement stmt_code = null;
        DbConnection cnn = null;
        String sql_get = "select * from idol_record_code where msisdn = ?";
        String sql = "update idol_record set approve_status = ?, approved_date = now() where record_id = ? ";
        if (item.getApproveStatus() > IdolRecordInfo.STATUS_BAD) {
            sql = "update idol_record set approve_status = ?, approved_date = now(), record_code = ? where record_id = ? ";
        }

        int i = 0;
        boolean insertLastCode = false;
        String newRecordCode = "";
        int lastCode = 0;
        String userId = String.valueOf(item.getUserId());
        while (userId.length() < 6) {
            userId = "0" + userId;
        }

        try {
            cnn = getConnection();
            if (item.getApproveStatus() > IdolRecordInfo.STATUS_BAD) {
                /*
                 * Generate new code for record
                 */
                i = 0;
                stmt_get = cnn.getConnection().prepareStatement(sql_get);
                stmt_get.setString(++i, item.getMsisdn());
                rs = stmt_get.executeQuery();
                if (rs.next()) {
                    lastCode = rs.getInt("last_code");
                } else {
                    insertLastCode = true;
                }
                if (lastCode < 0) {
                    lastCode = 0;
                }
                lastCode++;
                if (lastCode < 10) {
                    newRecordCode = userId + "0" + String.valueOf(lastCode);
                } else {
                    newRecordCode = userId + String.valueOf(lastCode);
                }
                if (newRecordCode.length() < 8) {
                    newRecordCode = "0" + newRecordCode;
                }
            }

            i = 0;
            stmt_up = cnn.getConnection().prepareStatement(sql);
            stmt_up.setInt(++i, item.getApproveStatus());
            if (item.getApproveStatus() > IdolRecordInfo.STATUS_BAD) {
                stmt_up.setString(++i, newRecordCode);
            }
            stmt_up.setInt(++i, item.getRecordId());
            stmt_up.executeUpdate();

            if (item.getApproveStatus() > IdolRecordInfo.STATUS_BAD) {
                i = 0;
                if (insertLastCode) {
                    sql = "insert into idol_record_code(last_code, msisdn, user_id) values(?, ?, ?)";
                } else {
                    sql = "update idol_record_code set last_code = ? where msisdn = ? and user_id = ?";
                }
                stmt_code = cnn.getConnection().prepareStatement(sql);
                stmt_code.setInt(++i, lastCode);
                stmt_code.setString(++i, item.getMsisdn());
                stmt_code.setInt(++i, item.getUserId());
                stmt_code.executeUpdate();
            }

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
            item.setRecordCode(newRecordCode);
        } catch (Exception e) {
            logger.error("error in processApprovedForIdolRecord ", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt_get);
            closeStatement(stmt_up);
            closeStatement(stmt_code);
            freeConnection(cnn);
        }

        return resp;
    }

    public String processAssignFirstTopOfIdolRecord(IdolRecordInfo item) {
        String resp = Constants.SYSTEM_ERROR;
        PreparedStatement stmt_up = null;
        DbConnection cnn = null;
        String sql = "update idol_record set first_top_status = ?, quantity_top_1 = quantity_top_1 + 1, quantity_top = quantity_top + 1 where record_id = ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt_up = cnn.getConnection().prepareStatement(sql);
            stmt_up.setInt(++i, item.getFirstTopStatus());
            stmt_up.setInt(++i, item.getRecordId());
            stmt_up.executeUpdate();

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in processAssignFirstTopOfIdolRecord ", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return resp;
    }

    public String processAssignSecondTopOfIdolRecord(IdolRecordInfo item) {
        String resp = Constants.SYSTEM_ERROR;
        PreparedStatement stmt_up = null;
        DbConnection cnn = null;
        String sql = "update idol_record set second_top_status = ?, quantity_top_2 = quantity_top_2 + 1, quantity_top = quantity_top + 1 where record_id = ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt_up = cnn.getConnection().prepareStatement(sql);
            stmt_up.setInt(++i, item.getSecondTopStatus());
            stmt_up.setInt(++i, item.getRecordId());
            stmt_up.executeUpdate();

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in processAssignSecondTopOfIdolRecord ", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return resp;
    }

    public String updateListenHistory(IdolListenInfo item) {
        logger.info(">>>>> updateListenHistory ");
        String resp = Constants.SYSTEM_ERROR;
        ResultSet rs = null;
        PreparedStatement stmt_get = null;
        PreparedStatement stmt_up = null;
        PreparedStatement stmt_ins = null;
        PreparedStatement stmt_his = null;
        DbConnection cnn = null;
        String sql = "update idol_record set listen_count = listen_count + 1, listen_duration = listen_duration + ? where record_id = ? ";
        String sql_get = "select * from idol_listened where msisdn = ? and record_id = ?";
        String sql_his = "insert into idol_listen_his(record_id, user_id, msisdn, duration, competition_id, channel, package_id, sub_package_id) values(?, ?, ?, ?, ?, ?, ?, ?)";

        int i = 0;
        try {
            cnn = getConnection();
            if (item.getUpdateCounter() == 1) {
                stmt_up = cnn.getConnection().prepareStatement(sql);
                stmt_up.setInt(++i, item.getDuration());
                stmt_up.setInt(++i, item.getRecordId());
                stmt_up.executeUpdate();
                logger.info(">>>>> update updateListenHistory stmt_up: " + stmt_up.toString());
            }

            i = 0;
            stmt_get = cnn.getConnection().prepareStatement(sql_get);
            stmt_get.setString(++i, item.getMsisdn());
            stmt_get.setInt(++i, item.getRecordId());
            logger.info(">>>>> update updateListenHistory stmt_get: " + stmt_get.toString());
            rs = stmt_get.executeQuery();
            if (!rs.next()) {

                i = 0;
                sql = "insert into idol_listened(msisdn, user_id, record_id, competition_id) values(?, ?, ?, ?)";
                stmt_ins = cnn.getConnection().prepareStatement(sql);
                stmt_ins.setString(++i, item.getMsisdn());
                stmt_ins.setInt(++i, item.getUserId());
                stmt_ins.setInt(++i, item.getRecordId());
                stmt_ins.setInt(++i, item.getCompetitionId());
                logger.info(">>>>> insert updateListenHistory stmt_ins :" + stmt_ins.toString());
                stmt_ins.executeUpdate();
            }

            i = 0;
            stmt_his = cnn.getConnection().prepareStatement(sql_his);
            stmt_his.setInt(++i, item.getRecordId());
            stmt_his.setInt(++i, item.getUserId());
            stmt_his.setString(++i, item.getMsisdn());
            stmt_his.setInt(++i, item.getDuration());
            stmt_his.setInt(++i, item.getCompetitionId());
            stmt_his.setInt(++i, item.getChannel());
            stmt_his.setInt(++i, item.getPackageId());
            stmt_his.setInt(++i, item.getSubPackageId());
            logger.info(">>>>> insert updateListenHistory sql_his :" + stmt_his.toString());
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

    public ArrayList<IdolRecordInfo> getListRecordFromCollection(int competitionId, int roundNo, String msisdn, int mode, int limit) {
        ArrayList<IdolRecordInfo> list = new ArrayList<IdolRecordInfo>();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "";
        if (mode == 1 || mode == 2) {
            sql = "select * from idol_record where delete_status = 0 and msisdn = ? ";
            if (mode == 1) {
                sql += " and approve_status > ? and competition_id = ? ";
                if (roundNo == 2) {
                    sql += " and second_round_status = ? ";
                } else {
                    sql += " and second_round_status != ? ";
                }
            }
        } else {
            sql = "select t1.updated_date, t2.* from (select record_id, updated_date from idol_collection where msisdn = ? and status = 1 ) t1 "
                    + "inner join (select * from idol_record where delete_status = 0 and approve_status > ?"
                    + " and msisdn != ?) t2 on t1.record_id = t2.record_id ";
        }

        if (mode == 1) {
            sql = "select * from (select t3.* from (" + sql + ") t3) t4 order by created_date desc limit 0,? ";
        } else {
            sql = "select b.record_id_listened, a.* from (" + sql + ") a left join (select record_id record_id_listened from idol_listened where msisdn = ?) b on a.record_id = b.record_id_listened ";
            sql = "select * from (select t3.* from (" + sql + ") t3) t4 order by created_date desc limit 0,? ";
        }

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, msisdn);
            if (mode == 1) {
                stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
                stmt.setInt(++i, competitionId);
                stmt.setInt(++i, IdolRecordInfo.SECOND_ROUND_ON);
            } else if (mode == 3) {
                stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
                stmt.setString(++i, msisdn);
            }
            if (mode != 1) {
                stmt.setString(++i, msisdn);
            }
            stmt.setInt(++i, limit);

            rs = stmt.executeQuery();
            while (rs.next()) {
                IdolRecordInfo item = new IdolRecordInfo();
                item.setRecordId(rs.getInt("record_id"));
                item.setCompetitionId(rs.getInt("competition_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setFirstTopStatus(rs.getInt("first_top_status"));
                item.setSecondTopStatus(rs.getInt("second_top_status"));
                item.setSecondRoundStatus(rs.getInt("second_round_status"));
                item.setQuantityTop(rs.getInt("quantity_top"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setFirstVoteCount(rs.getInt("first_vote_count"));
                item.setSecondVoteCount(rs.getInt("second_vote_count"));
                item.setRecordCode(rs.getString("record_code"));
                item.setQuantityTop1(rs.getInt("quantity_top_1"));
                item.setQuantityTop2(rs.getInt("quantity_top_2"));
                if (mode != 1 && rs.getInt("record_id_listened") > 0) {
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

    public String checkUserId(int userId) {
        logger.info(">> Dao  checkUserId :" + userId);
        String resp = Constants.SYSTEM_ERROR;
        ResultSet rs = null;
        PreparedStatement stmt_up = null;
        DbConnection cnn = null;
        String sql = "select * from sub_profile_friend where user_id = ? and telco = ?";

        int i = 0;
        try {
            cnn = getConnection();
            stmt_up = cnn.getConnection().prepareStatement(sql);
            stmt_up.setInt(++i, userId);
            stmt_up.setInt(++i, Constants.Telco.VINAPHONE.getValue());
            rs = stmt_up.executeQuery();
            if (rs.next()) {
                resp = Constants.SUCCESS;
            } else {
                resp = Constants.NO_DATA_FOUND;
            }
            logger.info(">> Dao  checkUserId :" + resp);
        } catch (Exception e) {
            logger.error("error in checkUserId ", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return resp;
    }

    public ArrayList<IdolRecordInfo> getListApprovedRecordByUser(int userId, int competitionId, int roundNo) {
        ArrayList<IdolRecordInfo> list = new ArrayList<IdolRecordInfo>();
        logger.info(" >>>> DAO getListApprovedRecordByUser :" + userId + ", competitionId :" + competitionId + ",roundNo: " + roundNo + "\n");
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from idol_record where competition_id = ? and user_id = ? and approve_status > ? and delete_status = 0 ";
        if (roundNo > 1) {
            sql += " and second_round_status = ? ";
        }
        sql += " order by approved_date desc ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, competitionId);
            stmt.setInt(++i, userId);
            stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
            if (roundNo > 1) {
                stmt.setInt(++i, IdolRecordInfo.SECOND_ROUND_ON);
            }

            rs = stmt.executeQuery();
            while (rs.next()) {
                logger.info(" >>> Dao getListApprovedRecordByUser rs \n");
                IdolRecordInfo item = new IdolRecordInfo();
                item.setRecordId(rs.getInt("record_id"));
                item.setCompetitionId(rs.getInt("competition_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setMsisdn(rs.getString("msisdn"));
                item.setRecordPath(rs.getString("record_path"));
                item.setApproveStatus(rs.getInt("approve_status"));
                item.setFirstTopStatus(rs.getInt("first_top_status"));
                item.setSecondTopStatus(rs.getInt("second_top_status"));
                item.setSecondRoundStatus(rs.getInt("second_round_status"));
                item.setQuantityTop(rs.getInt("quantity_top"));
                item.setListenCount(rs.getInt("listen_count"));
                item.setListenDuration(rs.getInt("listen_duration"));
                item.setFirstVoteCount(rs.getInt("first_vote_count"));
                item.setSecondVoteCount(rs.getInt("second_vote_count"));
                item.setRecordCode(rs.getString("record_code"));
                item.setQuantityTop1(rs.getInt("quantity_top_1"));
                item.setQuantityTop2(rs.getInt("quantity_top_2"));

                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getListApprovedRecordByUser from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public int getTotalApprovedRecordByUser(String msisdn, int competitionId, int roundNo) {
        int total = 0;

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select count(*) total from idol_record where competition_id = ? and msisdn = ? and approve_status > ? and delete_status = 0 ";
        if (roundNo > 1) {
            sql += " and second_round_status = ? ";
        }

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, competitionId);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, IdolRecordInfo.STATUS_BAD);
            if (roundNo > 1) {
                stmt.setInt(++i, IdolRecordInfo.SECOND_ROUND_ON);
            }

            rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getInt("total");
            }

        } catch (Exception e) {
            logger.error("error in getTotalApprovedRecordByUser from database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return total;
    }

    public String insertIdolAwardFromVoting(IdolAwardInfo item) {
        String result = Constants.SYSTEM_ERROR;

        DbConnection cnn = null;
        PreparedStatement stmt_up_point = null;
        PreparedStatement stmt_log = null;
        PreparedStatement stmt_award = null;
        PreparedStatement stmt_get_award = null;
        PreparedStatement stmt_topup = null;

        ResultSet rs_get_award = null;
        String sql = "";

        try {
            cnn = getConnection();

            if (item.getType() == IdolAwardInfo.AWARD_TYPE_POINT) {
                /*
                 * Cong diem cho thue bao
                 */
                int point = item.getVal();

                /*
                 * Cong diem vao tai khoan
                 */
                sql = "update sub_point set before_total_point = total_point, total_point = total_point + ?, original_point = original_point + ?, updated_date = now() where msisdn = ?";
                stmt_up_point = cnn.getConnection().prepareStatement(sql);
                stmt_up_point.setInt(1, point);
                stmt_up_point.setInt(2, point);
                stmt_up_point.setString(3, item.getMsisdn());
                stmt_up_point.executeUpdate();

                /*
                 * Luu lich su cong diem 
                 */
                sql = "insert into sub_point_history(msisdn, point, action, package_id, sub_package_id) values(?, ?, ?, 0, 0)";
                stmt_log = cnn.getConnection().prepareStatement(sql);
                stmt_log.setString(1, item.getMsisdn());
                stmt_log.setInt(2, point);
                stmt_log.setInt(3, Constants.POINT_ACTION_IDOL);
                stmt_log.executeUpdate();

                cnn.getConnection().commit();
                result = Constants.SUCCESS;

            } else if (item.getType() == IdolAwardInfo.AWARD_TYPE_CARD) {
                sql = "select * FROM award WHERE status = ? and award_key = ?";
                stmt_get_award = cnn.getConnection().prepareStatement(sql);
                stmt_get_award.setInt(1, Constants.STATUS_PUBLIC);
                stmt_get_award.setInt(2, item.getKey());

                rs_get_award = stmt_get_award.executeQuery();
                if (rs_get_award.next()) {
                    int awardId = rs_get_award.getInt("award_id");
                    int topupPrice = rs_get_award.getInt("topup_price");
                    String awardName = rs_get_award.getString("award_name");

                    /*
                     * Luu lai giao dich nhan the cao
                     */
                    sql = "insert into sub_award(msisdn, award_id, status, point_exchange, package_id, sub_package_id) values(?, ?, 1, 0, 0, 0)";
                    stmt_award = cnn.getConnection().prepareStatement(sql);
                    stmt_award.setString(1, item.getMsisdn());
                    stmt_award.setInt(2, awardId);
                    stmt_award.executeUpdate();

                    /*
                     * Nap tien qua topup
                     */
                    sql = "insert into topup(msisdn, price, program_name) values(?, ?, ?)";
                    stmt_topup = cnn.getConnection().prepareStatement(sql);
                    stmt_topup.setString(1, item.getMsisdn());
                    stmt_topup.setInt(2, topupPrice);
                    stmt_topup.setString(3, item.getProgramName());
                    stmt_topup.executeUpdate();

                    cnn.getConnection().commit();
                    result = Constants.SUCCESS;
                    item.setAwardName(awardName);
                }
            } else {
                result = Constants.ERROR_REJECT;
            }
        } catch (Exception ex) {
            logger.error("error in updateLuckyAwardForSub into database", ex);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e) {
                    logger.error(e.getMessage());
                }
            }
        } finally {
            closeResultSet(rs_get_award);

            closeStatement(stmt_up_point);
            closeStatement(stmt_award);
            closeStatement(stmt_get_award);
            closeStatement(stmt_log);
            closeStatement(stmt_topup);
            freeConnection(cnn);
        }

        return result;
    }

    public IdolRecordInfo getIdolRecordByCode(String code) {
        logger.info(" >>>> getIdolRecordByCode :" + code + "\n");
        IdolRecordInfo item = new IdolRecordInfo();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from idol_record where RECORD_CODE = ? and DELETE_STATUS = 0 ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, code);
            logger.info("SQL getIdolRecordByCode : " + stmt.toString() + "\n");
            rs = stmt.executeQuery();
            if (rs.next()) {
                logger.info(" >>>> getIdolRecordByCode record_id:" + rs.getInt("record_id") + "\n");
                item.setRecordId(rs.getInt("RECORD_ID"));
                item.setCompetitionId(rs.getInt("COMPETITION_ID"));
                item.setUserId(rs.getInt("USER_ID"));
                item.setMsisdn(rs.getString("MSISDN"));
                item.setRecordPath(rs.getString("RECORD_PATH"));
                item.setApproveStatus(rs.getInt("APPROVE_STATUS"));
                item.setFirstTopStatus(rs.getInt("FIRST_TOP_STATUS"));
                item.setSecondTopStatus(rs.getInt("SECOND_TOP_STATUS"));
                item.setSecondRoundStatus(rs.getInt("SECOND_ROUND_STATUS"));
                item.setQuantityTop(rs.getInt("QUANTITY_TOP"));
                item.setListenCount(rs.getInt("LISTEN_COUNT"));
                item.setListenDuration(rs.getInt("LISTEN_DURATION"));
                item.setFirstVoteCount(rs.getInt("FIRST_VOTE_COUNT"));
                item.setSecondVoteCount(rs.getInt("SECOND_VOTE_COUNT"));
                item.setRecordCode(rs.getString("RECORD_CODE"));
                item.setQuantityTop1(rs.getInt("QUANTITY_TOP_1"));
                item.setQuantityTop2(rs.getInt("QUANTITY_TOP_2"));
                item.setCreatedDate(rs.getTimestamp("CREATED_DATE"));
            }

        } catch (Exception e) {
            logger.error("error in getIdolRecordByCode from database", e);
            item = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return item;
    }

    public ArrayList<IdolRecordInfo> getListSummaryVote(Date fromDate, Date toDate) {
        ArrayList<IdolRecordInfo> list = new ArrayList<IdolRecordInfo>();
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select t1.msisdn, t1.total_vote, t2.total_point from ( select receiver msisdn, count(*) total_vote "
                + " from idol_vote  where created_date >= ? and created_date <= ? and receiver != msisdn "
                + " group by receiver) t1 inner join ( select msisdn, sum(point) total_point "
                + "  from sub_point_history where created_date >= ? and created_date <= ? and action = ? group by msisdn "
                + ") t2 on t1.msisdn = t2.msisdn";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setTimestamp(++i, new Timestamp(fromDate.getTime()));
            stmt.setTimestamp(++i, new Timestamp(toDate.getTime()));
            stmt.setTimestamp(++i, new Timestamp(fromDate.getTime()));
            stmt.setTimestamp(++i, new Timestamp(toDate.getTime()));
            stmt.setInt(++i, Constants.POINT_ACTION_IDOL);
            rs = stmt.executeQuery();

            while (rs.next()) {
                IdolRecordInfo item = new IdolRecordInfo();
                item.setMsisdn(rs.getString("msisdn"));
                item.setTotalVote(rs.getInt("total_vote"));
                item.setTotalPoint(rs.getInt("total_point"));
                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getListSummaryVote ", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }
}
