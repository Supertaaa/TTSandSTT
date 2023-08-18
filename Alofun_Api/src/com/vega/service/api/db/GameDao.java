/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.db;

import com.vega.service.api.common.Constants;
import com.vega.vcs.service.database.pool.DbConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import javax.naming.NamingException;
import org.apache.log4j.Logger;

/**
 *
 * @author N.Tuyen
 */
public class GameDao extends DBConnections {

    static transient Logger logger = Logger.getLogger(GameDao.class);

    public boolean start() throws NamingException {
        return super.start();
    }

    public int checkAccessGame(String msisdn, int gameKey) {
        int questionCountCurrentDate = 0;

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        String sql = "select question_count, status, answered_date, DATE_FORMAT(now(), '%Y-%m-%d') c_date "
                + "from game_account "
                + "where msisdn = ? "
                + "and game_key = ? ";

        int statusGameOfUser = Constants.STATUS_PUBLIC;

        try {
            cnn = getConnection();
            /*
             * Kiem tra so cau hoi da tra loi trong ngay va trang thai choi game
             */
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setInt(2, gameKey);
            re1 = stmt1.executeQuery();

            if (re1.next()) {
                String answeredDate = re1.getString("answered_date");
                String currentDate = re1.getString("c_date");
                questionCountCurrentDate = re1.getInt("question_count");
                statusGameOfUser = re1.getInt("status");

                if (answeredDate != null && !answeredDate.equalsIgnoreCase(currentDate)) {
                    /*
                     * Reset so cau hoi neu chuyen ngay khac
                     */
                    questionCountCurrentDate = 0;
                }
            }

            /*
             * Cho phep tham gia game:
             * - trang thai dang ky choi game
             */
            if (statusGameOfUser != Constants.STATUS_PUBLIC) {
                questionCountCurrentDate = -1;
            }

        } catch (Exception e) {
            logger.error("Error in checkAccessGame", e);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return questionCountCurrentDate;
    }

    public int rejectPlayGame(String msisdn, int gameKey, int scriptNo, int contentId, int rejectCountConfirm) {
        int rejectCount = 0;

        DbConnection cnn = null;
        PreparedStatement stmt_game_acc = null;
        PreparedStatement stmt_up_game_acc = null;
        PreparedStatement stmt_game_his = null;

        ResultSet rs_get_game_acc = null;
        String sql = "";
        String questionList = null;
        logger.info("rejectPlayGame :" + msisdn + "-" + gameKey);
        try {
            cnn = getConnection();
            /*
             * Danh dau so lan tuong tac trong ngay
             */
            sql = "select answered_date, question_count, reject_count, DATE_FORMAT(now(), '%Y-%m-%d') as c_date, question_list from game_account where msisdn = ? and game_key = ?";
            stmt_game_acc = cnn.getConnection().prepareStatement(sql);
            stmt_game_acc.setString(1, msisdn);
            stmt_game_acc.setInt(2, gameKey);

            rs_get_game_acc = stmt_game_acc.executeQuery();
            if (rs_get_game_acc.next()) {
                String currentDate = rs_get_game_acc.getString("c_date");
                String answeredDate = rs_get_game_acc.getString("answered_date");
                int questionCount = rs_get_game_acc.getInt("question_count");
                rejectCount = rs_get_game_acc.getInt("reject_count");
                rejectCount++;

                if (answeredDate != null && !answeredDate.equalsIgnoreCase(currentDate)) {
                    /*
                     * Reset counter
                     */
                    questionCount = 1;
                } else {
                    questionCount++;
                }

                questionList = rs_get_game_acc.getString("question_list");
                if (contentId > 0) {
                    String keyQuestion = "-" + String.valueOf(contentId) + "-";
                    if (questionList == null || questionList.equals("")) {
                        questionList = keyQuestion;
                    } else {
                        if (!questionList.contains(keyQuestion)) {
                            questionList = questionList + String.valueOf(contentId) + "-";
                        }
                    }
                }
                logger.info("rejectPlayGame : update into game_account");
                if (rejectCount % rejectCountConfirm == 0) {
                    sql = "update game_account set question_count = ?, reject_count = ?, answered_date = DATE_FORMAT(now(), '%Y-%m-%d'), updated_date = NOW(), confirm_expiry = DATE_ADD(NOW() , INTERVAL 1 DAY), question_list = ?, script_no = ? where msisdn = ? and game_key = ?";
                } else {
                    sql = "update game_account set question_count = ?, reject_count = ?, answered_date = DATE_FORMAT(now(), '%Y-%m-%d'), updated_date = NOW(), question_list = ?, script_no = ? where msisdn = ? and game_key = ?";
                }
                stmt_up_game_acc = cnn.getConnection().prepareStatement(sql);
                stmt_up_game_acc.setInt(1, questionCount);
                stmt_up_game_acc.setInt(2, rejectCount);
                stmt_up_game_acc.setString(3, questionList);
                stmt_up_game_acc.setInt(4, scriptNo);
                stmt_up_game_acc.setString(5, msisdn);
                stmt_up_game_acc.setInt(6, gameKey);
                stmt_up_game_acc.executeUpdate();
            } else {
                rejectCount = 1;
                if (contentId > 0) {
                    questionList = "-" + String.valueOf(contentId) + "-";
                }
                logger.info("rejectPlayGame : insert into game_account");
                sql = "insert into game_account(msisdn, game_key, question_count, reject_count, answered_date, question_list, script_no) "
                        + "values(?, ?, 1, 1, DATE_FORMAT(now(), '%Y-%m-%d'), ?, ?)";
                stmt_up_game_acc = cnn.getConnection().prepareStatement(sql);
                stmt_up_game_acc.setString(1, msisdn);
                stmt_up_game_acc.setInt(2, gameKey);
                stmt_up_game_acc.setString(3, questionList);
                stmt_up_game_acc.setInt(4, scriptNo);
                stmt_up_game_acc.executeUpdate();
            }

            /*
             * Luu lai lich su tuong tac game
             */
            logger.info("rejectPlayGame : insert into game_interact_his");
            sql = "insert into game_interact_his( game_key, msisdn, result, script_no, content_id) values(?, ?, ?, ?, ?)";
            stmt_game_his = cnn.getConnection().prepareStatement(sql);
            stmt_game_his.setInt(1, gameKey);
            stmt_game_his.setString(2, msisdn);
            stmt_game_his.setInt(3, Constants.GAME_HIS_RESULT_REJECT);
            stmt_game_his.setInt(4, scriptNo);
            stmt_game_his.setInt(5, contentId);
            stmt_game_his.executeUpdate();

            cnn.getConnection().commit();

        } catch (Exception ex) {
            logger.error("error in rejectPlayGame into database", ex);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e) {
                    logger.error(e.getMessage());
                }
            }
            rejectCount = -1;
        } finally {
            closeResultSet(rs_get_game_acc);

            closeStatement(stmt_game_acc);
            closeStatement(stmt_up_game_acc);
            closeStatement(stmt_game_his);
            freeConnection(cnn);
        }

        return rejectCount;
    }

    public String getAnsweredQuestionList(String msisdn, int gameKey) {
        String questionList = "";

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet re1 = null;
        String sql = "select question_list "
                + "from game_account "
                + "where msisdn = ? "
                + "and game_key = ? ";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setInt(2, gameKey);
            re1 = stmt1.executeQuery();

            if (re1.next()) {
                questionList = re1.getString("question_list");
                if (questionList == null) {
                    questionList = "";
                }
            }

        } catch (Exception e) {
            logger.error("Error in getAnsweredQuestionList", e);
        } finally {
            closeResultSet(re1);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return questionList;
    }

    public String updateAnsweredQuestionList(String msisdn, int contentId, int gameKey) {
        String resp = Constants.SYSTEM_ERROR;

        DbConnection cnn = null;
        PreparedStatement stmt_game_acc = null;
        PreparedStatement stmt_up_game_acc = null;

        ResultSet rs_get_game_acc = null;
        String sql = "select question_list from game_account where msisdn = ? and game_key = ?";
        String questionList = "";
        String keyQuestion = "-" + String.valueOf(contentId) + "-";

        try {
            cnn = getConnection();

            stmt_game_acc = cnn.getConnection().prepareStatement(sql);
            stmt_game_acc.setString(1, msisdn);
            stmt_game_acc.setInt(2, gameKey);

            rs_get_game_acc = stmt_game_acc.executeQuery();
            if (rs_get_game_acc.next()) {
                questionList = rs_get_game_acc.getString("question_list");
                if (questionList == null || questionList.equals("")) {
                    questionList = keyQuestion;
                } else {
                    if (!questionList.contains(keyQuestion)) {
                        questionList = questionList + String.valueOf(contentId) + "-";
                    }
                }

                sql = "update game_account set question_list = ?, answered_date = DATE_FORMAT(now(), '%Y-%m-%d'), updated_date = NOW where msisdn = ? and game_key = ?";
                stmt_up_game_acc = cnn.getConnection().prepareStatement(sql);
                stmt_up_game_acc.setString(1, questionList);
                stmt_up_game_acc.setString(2, msisdn);
                stmt_up_game_acc.setInt(3, gameKey);
                stmt_up_game_acc.executeUpdate();
            } else {
                questionList = keyQuestion;

                sql = "insert into game_account(msisdn, game_key, question_list, answered_date) values(?, ?, ?, DATE_FORMAT(now(), '%Y-%m-%d'))";
                stmt_up_game_acc = cnn.getConnection().prepareStatement(sql);
                stmt_up_game_acc.setString(1, msisdn);
                stmt_up_game_acc.setInt(2, gameKey);
                stmt_up_game_acc.setString(3, questionList);
                stmt_up_game_acc.executeUpdate();
            }

            cnn.getConnection().commit();
            resp = questionList;
        } catch (Exception ex) {
            logger.error("error in updateAnsweredQuestionList into database", ex);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e) {
                    logger.error(e.getMessage());
                }
            }
        } finally {
            closeResultSet(rs_get_game_acc);
            closeStatement(stmt_game_acc);
            closeStatement(stmt_up_game_acc);

            freeConnection(cnn);
        }

        return resp;
    }

    public int answerQuestionOfGame(String msisdn, int subPackageId, int packageId, int contentId, boolean rightAnswer, int point, int gameKey, int pointAction, String desc) {
        int result = 0;

        DbConnection cnn = null;
        PreparedStatement stmt_game_acc = null;
        PreparedStatement stmt_up_game_acc = null;
        PreparedStatement stmt_game_his = null;

        ResultSet rs_get_game_acc = null;
        String sql = "";
        int total = 0;
        String questionList = "";

        try {
            cnn = getConnection();

            /*
             * Danh dau so lan tuong tac trong ngay
             */
            String keyQuestion = "-" + String.valueOf(contentId) + "-";
            sql = "select answered_date, question_count, DATE_FORMAT(now(), '%Y-%m-%d') as c_date, question_list "
                    + " from game_account where msisdn = ? and game_key = ?";
            stmt_game_acc = cnn.getConnection().prepareStatement(sql);
            stmt_game_acc.setString(1, msisdn);
            stmt_game_acc.setInt(2, gameKey);

            rs_get_game_acc = stmt_game_acc.executeQuery();
            if (rs_get_game_acc.next()) {
                questionList = rs_get_game_acc.getString("question_list");
                if (questionList == null || questionList.equals("")) {
                    questionList = keyQuestion;
                } else {
                    if (!questionList.contains(keyQuestion)) {
                        questionList = questionList + String.valueOf(contentId) + "-";
                    }
                }

                String currentDate = rs_get_game_acc.getString("c_date");
                String answeredDate = rs_get_game_acc.getString("answered_date");
                int questionCount = rs_get_game_acc.getInt("question_count");
                if (answeredDate != null && !answeredDate.equalsIgnoreCase(currentDate)) {
                    /*
                     * Reset counter
                     */
                    questionCount = 1;
                } else {
                    questionCount++;
                }

                sql = "update game_account set question_count = ?, question_list = ?, answered_date = DATE_FORMAT(now(), '%Y-%m-%d'), updated_date = NOW() where msisdn = ? and game_key = ?";
                stmt_up_game_acc = cnn.getConnection().prepareStatement(sql);
                stmt_up_game_acc.setInt(1, questionCount);
                stmt_up_game_acc.setString(2, questionList);
                stmt_up_game_acc.setString(3, msisdn);
                stmt_up_game_acc.setInt(4, gameKey);
                stmt_up_game_acc.executeUpdate();
            } else {
                questionList = keyQuestion;

                sql = "insert into game_account(msisdn, game_key, question_count, question_list, answered_date) values(?, ?, 1, ?, DATE_FORMAT(now(), '%Y-%m-%d'))";
                stmt_up_game_acc = cnn.getConnection().prepareStatement(sql);
                stmt_up_game_acc.setString(1, msisdn);
                stmt_up_game_acc.setInt(2, gameKey);
                stmt_up_game_acc.setString(3, questionList);
                stmt_up_game_acc.executeUpdate();
            }

            /*
             * Luu lai lich su tuong tac game
             */
            sql = "insert into game_interact_his(game_key, msisdn, award, award_type, result, description, content_id) values(?, ?, ?, ?, ?, ?, ?)";
            stmt_game_his = cnn.getConnection().prepareStatement(sql);
            stmt_game_his.setInt(1, gameKey);
            stmt_game_his.setString(2, msisdn);
            stmt_game_his.setInt(3, point);
            stmt_game_his.setInt(4, Constants.AWARD_TYPE_POINT);
            stmt_game_his.setInt(5, rightAnswer ? Constants.GAME_HIS_RESULT_OK : Constants.GAME_HIS_RESULT_FAILED);
            stmt_game_his.setString(6, desc);
            stmt_game_his.setInt(7, contentId);
            stmt_game_his.executeUpdate();

            cnn.getConnection().commit();
        } catch (Exception ex) {
            logger.error("error in answerQuestionOfGame into database", ex);
            if (cnn != null) {
                try {
                    cnn.getConnection().rollback();
                } catch (SQLException e) {
                    logger.error(e.getMessage());
                }
            }
            result = -1;
        } finally {
            closeResultSet(rs_get_game_acc);

            closeStatement(stmt_game_acc);
            closeStatement(stmt_up_game_acc);
            closeStatement(stmt_game_his);
            freeConnection(cnn);
        }

        return result;
    }
    /*
     * Minigame: Dang ky choi game
     */

    public boolean updateStatusPlayGame(String msisdn, int gameKey) {
        boolean result = false;
        logger.info("updateStatusPlayGame >>>>>>");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        String sql = "update game_account "
                + " set status = ?, updated_date = now(), reject_count = 0, confirm_expiry = null, return_date = NOW() "
                + " where msisdn = ? "
                + " and game_key = ? "
                + " and status <> ? ";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, Constants.STATUS_PUBLIC);
            stmt1.setString(2, msisdn);
            stmt1.setInt(3, gameKey);
            stmt1.setInt(4, Constants.STATUS_PUBLIC);
            stmt1.executeUpdate();

            cnn.getConnection().commit();
            result = true;
        } catch (Exception e) {
            logger.error("Error in updateStatusPlayGame", e);
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


    /*
     * ============== Minigame ====================
     * ============================================
     * - Xac nhan tu choi choi game
     */
    public boolean updateStatusRejectGame(String msisdn, int gameKey) {
        boolean result = false;

        int status = Constants.STATUS_PUBLIC;
        Calendar currentDate = null;
        Calendar confirmExpiry = null;

        ResultSet rs = null;
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        String sql = "select status, confirm_expiry, NOW() as currentDate "
                + "from game_account "
                + "where msisdn = ? "
                + "and game_key = ? ";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            stmt1.setInt(2, gameKey);
            rs = stmt1.executeQuery();

            if (rs.next()) {
                status = rs.getInt("status");
                Timestamp time1 = rs.getTimestamp("confirm_expiry");
                if (time1 != null) {
                    confirmExpiry = Calendar.getInstance();
                    confirmExpiry.setTime(time1);

                    currentDate = Calendar.getInstance();
                    Timestamp time2 = rs.getTimestamp("currentDate");
                    currentDate.setTime(time2);
                }

                if (status == Constants.STATUS_PUBLIC
                        && (confirmExpiry != null && confirmExpiry.after(currentDate))) {
                    /*
                     * Cap nhat trang thai tu choi
                     */
                    sql = "update game_account set status = ?, reject_count = 0, updated_date = NOW(), confirm_expiry = null where msisdn = ? and game_key = ?";
                    stmt2 = cnn.getConnection().prepareStatement(sql);
                    stmt2.setInt(1, Constants.STATUS_LOCK);
                    stmt2.setString(2, msisdn);
                    stmt2.setInt(3, gameKey);
                    stmt2.executeUpdate();

                    cnn.getConnection().commit();
                    result = true;
                }
            }

        } catch (Exception e) {
            logger.error("Error in updateStatusRejectGame", e);
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
}
