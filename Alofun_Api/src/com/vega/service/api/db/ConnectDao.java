/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.db;

import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.object.ConnectInfo;
import com.vega.service.api.object.ListenHistory;
import com.vega.service.api.response.Result;
import com.vega.vcs.service.database.pool.DbConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.naming.NamingException;
import org.apache.log4j.Logger;

/**
 *
 * @author N.Tuyen
 */
public class ConnectDao extends DBConnections {

    static transient Logger logger = Logger.getLogger(ConnectDao.class);
    // DAO xo so

    public boolean start() throws NamingException {
        return super.start();
    }

    /**
     * lay danh sach noi dung trong bang connect_content
     *
     * @param msisdn
     * @return
     */
    public ArrayList<ConnectInfo> getListContentConnect() {
        logger.info(" >> getListContentConnect Dao");
        ArrayList<ConnectInfo> list = new ArrayList<>();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select connect_content_id,content_path,content_question,list_key,key_right,"
                + " start_date_question,end_date_question,connect_status "
                + " from connect_content where connect_status =1"
                + " order by connect_content_id desc";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            logger.info("getListContentConnect : " + stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                logger.info("DAO start_date_question  :" + rs.getTimestamp("start_date_question") + "\n");
                ConnectInfo item = new ConnectInfo();
                item.setConnect_content_id(rs.getInt("connect_content_id"));
                item.setContent_path(rs.getString("content_path"));
                item.setContent_question(rs.getString("content_question"));
                item.setList_key(rs.getString("list_key"));
                item.setKey_right(rs.getInt("key_right"));
                item.setStart_date_question(rs.getTimestamp("start_date_question"));
                item.setEnd_date_question(rs.getTimestamp("end_date_question"));
                list.add(item);
            }
        } catch (Exception e) {
            logger.error("error in getListContentConnect from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    /**
     * getListenHistoryConnect lay lich su nghe cua nguoi dung
     *
     * @param msisdn
     * @return
     */
    public ListenHistory getListenHistoryConnect(String msisdn) {

        logger.debug("getListenHistoryConnect from Database");
        ListenHistory listenHistory = null;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select content_list,last_content_id,duration from connect_content_history where msisdn = ?";
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
                listenHistory = new ListenHistory();
                listenHistory.setMsisdn(msisdn);
                listenHistory.setContentListened(rs.getString("content_list"));
                listenHistory.setContentId(rs.getInt("last_content_id"));
                listenHistory.setDuration(rs.getInt("duration"));
                return listenHistory;
            }
        } catch (Exception e) {
            logger.error("error in getListenHistoryConnect in database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return listenHistory;
    }

    /**
     * lay cau hoi dang active
     *
     * @return
     */
    public ConnectInfo getQuestionActive() {
        logger.debug("getQuestionActive from Database");
        ConnectInfo connectInfo = new ConnectInfo();
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select connect_content_id from connect_content where "
                + " connect_status =1 and start_date_question <= now() and end_date_question >= now()"
                + " order by connect_content_id desc limit 1";
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
                connectInfo.setConnect_content_id(rs.getInt("connect_content_id"));
            }
        } catch (Exception e) {
            logger.error("error in getQuestionActive in database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return connectInfo;
    }

    /**
     * lay cau hoi nguoi dung da trl cuoi cung
     *
     * @param msisdn
     * @return
     */
    public ConnectInfo getLastQuestionAnswerByMsisdn(String msisdn) {
        logger.info("getLastQuestionAnswerByMsisdn from Database :" + msisdn);
        ConnectInfo connectInfo = new ConnectInfo();
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select connect_content_id from connect_game_his where "
                + " msisdn = ?"
                + " order by connect_game_his_id desc limit 1";
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
                connectInfo.setConnect_content_id(rs.getInt("connect_content_id"));
            }
        } catch (Exception e) {
            logger.error("error in getLastQuestionAnswerByMsisdn in database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return connectInfo;
    }

    /**
     *
     * Kiem tra xem thue bao co tu choi trl cau hoi k
     *
     * @param msisdn
     * @return
     */
    public boolean checkSubRejectByMsisdn(String msisdn) {
        logger.info("checkSubRejectByMsisdn Dao : " + msisdn);
        boolean result = false;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from sub_reject_conntent where msisdn = ? and status = 1 and type_reject = 1";
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
                result = true;
            }
        } catch (Exception e) {
            logger.error("error in checkSubRejectByMsisdn in database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return result;
    }

    /**
     * insert or update cau tra loi cua nguoi dung
     *
     * @param msisdn
     * @param connect_content_id
     * @param answerKey
     * @param numberSameAnswer
     * @return
     */
    public Result answerConnectQuestion(String msisdn, int connect_content_id, int answerKey, int numberSameAnswer) {
        DbConnection cnn = null;
        Result result = new Result();
        PreparedStatement stmt = null;
        PreparedStatement stmt_ins_up = null;
        ResultSet rs = null;
        String sql_select = "select connect_game_his_id from connect_game_his where msisdn = ? and connect_content_id=?";
        String sql_up = "update connect_game_his set answer_key=?,number_same_answer=?,update_at=now() "
                + " where connect_game_his_id =? and msisdn = ? and connect_content_id =?";
        String sql_ins = "insert into connect_game_his"
                + " (msisdn,connect_content_id,answer_key,number_same_answer,created_at,update_at) "
                + " values (?,?,?,?,now(),now())";

        int connect_game_his_id = 0;
        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql_select);
            stmt.setString(1, msisdn);
            stmt.setInt(2, connect_content_id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                // update ban ghi
                connect_game_his_id = rs.getInt("connect_game_his_id");
            }
            if (connect_game_his_id > 0) {

                stmt_ins_up = cnn.getConnection().prepareStatement(sql_up);
                stmt_ins_up.setInt(++i, answerKey);
                stmt_ins_up.setInt(++i, numberSameAnswer);
                stmt_ins_up.setInt(++i, connect_game_his_id);
                stmt_ins_up.setString(++i, msisdn);
                stmt_ins_up.setInt(++i, connect_content_id);

            } else {
                stmt_ins_up = cnn.getConnection().prepareStatement(sql_ins);
                stmt_ins_up.setString(++i, msisdn);
                stmt_ins_up.setInt(++i, connect_content_id);
                stmt_ins_up.setInt(++i, answerKey);
                stmt_ins_up.setInt(++i, numberSameAnswer);

            }
            stmt_ins_up.executeUpdate();
            result.setErrorCode("0");
            cnn.getConnection().commit();

        } catch (Exception ex) {
            logger.info("Error answerConnectQuestion ex :" + ex);
            result.setErrorCode("-1");
            rollbackTransaction(cnn);
        }finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeStatement(stmt_ins_up);
            freeConnection(cnn);
        } 
        return result;
    }

    /**
     * update lich su nghe
     *
     * @param listenHis
     * @return
     */
    public int updateListenHistory(ListenHistory listenHis) {
        logger.info(">>>. updateListenHistory ");
        int result = -1;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "update connect_content_history set content_list = ?,last_content_id=?,duration=?,update_at=now() where msisdn = ?";
        /*
         * Limit length
         */
        if ("".equals(listenHis.getContentListened())) {
            logger.info("Not insert connect_content_history cause of content_id is blank! ");
            return result;
        }

        int max_length_in_char = Integer.parseInt(ConfigStack.getConfig("api_general", "listen_content_max_length", "1000"));
        int content_length = listenHis.getContentListened().length();
        if (content_length > max_length_in_char) {
            listenHis.setContentListened(listenHis.getContentListened()
                    .substring(content_length - max_length_in_char - 1, content_length));
            int first_index_sep = listenHis.getContentListened().indexOf(ListenHistory.SEPERATOR);
            listenHis.setContentListened(
                    listenHis.getContentListened().substring(first_index_sep, listenHis.getContentListened().length()));
        }

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, listenHis.getContentListened());
            stmt.setInt(2, listenHis.getContentId());
            stmt.setInt(3, listenHis.getDuration());
            stmt.setString(4, listenHis.getMsisdn());
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.executeUpdate();
            result = 0;
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("error in updateListenHistory in database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return result;
    }

    /**
     * insert lich su nghe
     *
     * @param listenHis
     * @return
     */
    public int insertListenHistory(ListenHistory listenHis) {
        logger.info(">>>. insertListenHistory :" + listenHis.getContentId());
        int result = -1;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "insert into connect_content_history(msisdn,content_list,last_content_id,duration,created_at,update_at) values(?,?,?,?,now(),now())";
        /*
         * Limit length
         */
        // --id1-id2-id3-id4-
        if ("".equals(listenHis.getContentListened())) {
            logger.info("Not insert connect_content_history cause of content_id is blank! ");
            return result;
        }

        int max_length_in_char = Integer.parseInt(ConfigStack.getConfig("api_general", "listen_content_max_length", "1000"));
        int content_length = listenHis.getContentListened().length();
        if (content_length > max_length_in_char) {
            listenHis.setContentListened(listenHis.getContentListened()
                    .substring(content_length - max_length_in_char - 1, content_length));
            int first_index_sep = listenHis.getContentListened().indexOf(ListenHistory.SEPERATOR);
            listenHis.setContentListened(
                    listenHis.getContentListened().substring(first_index_sep, listenHis.getContentListened().length()));
        }
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, listenHis.getMsisdn());
            stmt.setString(2, listenHis.getContentListened());
            stmt.setInt(3, listenHis.getContentId());
            stmt.setInt(4, listenHis.getDuration());
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            stmt.executeUpdate();
            result = 0;
            cnn.getConnection().commit();
        } catch (Exception e) {
            logger.error("error in insertListenHistory in database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return result;
    }

    /**
     * kiem tra so lan khach hang tu choi
     *
     * @param msisdn
     * @return
     */
    public Result checkCountReject(String msisdn) {
        logger.info(">>>. checkCountReject :" + msisdn);
        Result result = new Result();
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from  sub_reject_conntent where msisdn = ?";

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
                result.setErrorCode("0");
                result.setCount_reject(rs.getInt("count_reject"));
                result.setMt_notify_at(rs.getTimestamp("mt_notify_at"));
                result.setStatus(rs.getInt("status"));
                logger.info(" result checkCountReject :" + rs.getInt("count_reject") + "\n");
            } else {
                result.setErrorCode("1");
            }
        } catch (Exception e) {
            logger.error("error in checkCountReject in database", e);
            result.setErrorCode("-1");
        } finally {
            closeStatement(stmt);
            closeResultSet(rs);
            freeConnection(cnn);
        }
        return result;
    }

    public int insertRejectConnect(String msisdn, int countReject) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        int result = -1;
        String sql = "insert into sub_reject_conntent(msisdn,count_reject,created_at,update_at) values(?,?,now(),now())";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            stmt.setInt(2, countReject);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            result = stmt.executeUpdate();
            logger.info("Result insertRejectConnect : " + result + "\n");
            result = 0;
            cnn.getConnection().commit();
        } catch (Exception e) {
            e.printStackTrace();
            rollbackTransaction(cnn);
            logger.error("Exception in insertRejectConnect", e);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return result;
    }

    public int updateRejectConnect(String msisdn, int countReject, int status) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        int result = -1;
        String sql = "";
        try {
            if (countReject % 3 == 0) {
                sql = "update sub_reject_conntent set count_reject = ?,status=?,mt_notify_at=now(),update_at=now() where msisdn =?";
            } else {
                sql = "update sub_reject_conntent set count_reject = ?,status=?,update_at=now() where msisdn =?";
            }
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, countReject);
            stmt.setInt(2, status);
            stmt.setString(3, msisdn);
            if (logger.isDebugEnabled()) {
                String sql_log = stmt.toString();
                sql_log = sql_log.substring(sql_log.indexOf(":") + 1).trim();
                logger.debug(sql_log);
            }
            result = stmt.executeUpdate();
            result = 0;
            cnn.getConnection().commit();
        } catch (Exception e) {
            e.printStackTrace();
            rollbackTransaction(cnn);
            logger.error("Exception in updateRejectConnect", e);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return result;
    }
}
