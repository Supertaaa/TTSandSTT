/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.db;

import com.vega.service.api.common.Constants;
import com.vega.service.api.object.GameIntellectualContentInfo;
import com.vega.service.api.object.GameIntellectualHistoryInfo;
import com.vega.vcs.service.database.pool.DbConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.naming.NamingException;
import org.apache.log4j.Logger;

/**
 *
 * @author Nora
 */
public class GameIntellectualDao extends DBConnections {

    static transient Logger logger = Logger.getLogger(GameIntellectualDao.class);

    public boolean start() throws NamingException {
        return super.start();
    }

    public String getHistoryGame(String msisdn, String startTime, String endTime, int limitMore) {
        StringBuilder listHistory = new StringBuilder();

        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;

        String sqlCommand = "select DISTINCT(question_id), MAX(created_at) as created_at from game_history "
                + " where msisdn = ? and action = 1 and game_type = 1 ";

        if (!"".equals(startTime) && !"".equals(endTime)) {
            sqlCommand += " and time_answer >= ? and time_answer <= ? ";
        }
        
        sqlCommand += " GROUP BY question_id "
                + " ORDER BY created_at ASC ";
       
        if(limitMore > 0){
            sqlCommand += " limit " + limitMore;
        }
        
        logger.info("getHistoryGame sql: " + sqlCommand);
        
        try {
            int i = 0;
            getConnection().clearCachedStatements();
            conn = getConnection();

            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(++i, msisdn);

            if (!"".equals(startTime) && !"".equals(endTime)) {
                pstm.setString(++i, startTime);
                pstm.setString(++i, endTime);
            }

            rs = pstm.executeQuery();
     
            while (rs.next()) {
                listHistory.append(rs.getString("question_id")).append(",");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exeption in function getHistoryGame dao :", ex.getCause());

        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return listHistory.substring(0, (listHistory.length() > 0 ? (listHistory.length() - 1) : 0));
    }

    public int chooseStar(String msisdn, String startTime, String endTime) {
        int value = 0;

        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;

        String sqlCommand = "select action from game_history where msisdn = ? and created_at >= ? and created_at <= ? and action in(2,4) and game_type = 1";

        try {
            int i = 0;
            getConnection().clearCachedStatements();
            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(++i, msisdn);
            pstm.setString(++i, startTime);
            pstm.setString(++i, endTime);

            rs = pstm.executeQuery();


            if (rs.next()) {

                value = rs.getInt("action");
                logger.info("result chooseStar :" + value);
            }

        } catch (Exception ex) {
            logger.error("Exeption in function chooseStar dao :", ex.getCause());
            ex.printStackTrace();

        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return value;
    }

    public List<GameIntellectualContentInfo> getListQuestion(String listIdHistory, int limit, String listIdIn) {
        logger.info("get list question dao :" + listIdHistory + " and limit ..." + limit + " and listIdIn: " + listIdIn);
        List<GameIntellectualContentInfo> listQuestion = new ArrayList<>();

        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;

        String condition = "";
        if (listIdHistory.length() > 0) {
            condition = " and id_game not in(#value) ";
        }
        
        if (listIdIn.length() > 0) {
            condition = " and id_game in(#value2) ";
        }

        String sqlCommand = " select id_game, question_name, path_question, answer_a, answer_b, answer_c, answer_d, answer_correct"
                + " from game_intellectual_content "
                + " where status = 1 " + condition
                + " ORDER BY created_date DESC "
                + " limit ?";

        try {

            int i = 0;
            getConnection().clearCachedStatements();
            conn = getConnection();

            if (listIdHistory.length() > 0) {
                sqlCommand = sqlCommand.replaceAll("#value", listIdHistory);

            }
            
            if (listIdIn.length() > 0) {
                sqlCommand = sqlCommand.replaceAll("#value2", listIdIn);

            }

            pstm = conn.getConnection().prepareStatement(sqlCommand);

            logger.info("sqlCommand....." + sqlCommand);
            pstm.setInt(++i, limit);

            rs = pstm.executeQuery();
            conn.getConnection().commit();

            List<String> listAnswer = null;
            int index = 0;
            while (rs.next()) {
                index = index + 1;
                listAnswer = new ArrayList<>();
                int idGame = rs.getInt("id_game");
                String questionName = rs.getString("question_name");
                String pathQuestion = rs.getString("path_question");
                String answerA = rs.getString("answer_a");
                String answerB = rs.getString("answer_b");
                String answerC = rs.getString("answer_c");
                String answerD = rs.getString("answer_d");

                listAnswer.add("A+" + answerA);
                listAnswer.add("B+" + answerB);
                listAnswer.add("C+" + answerC);
                listAnswer.add("D+" + answerD);

                String answerCorrect = rs.getString("answer_correct");

                Collections.shuffle(listAnswer);

                listQuestion.add(new GameIntellectualContentInfo(index, idGame, questionName, pathQuestion, listAnswer, answerCorrect));
                logger.info("index db..." + index);
            }

            for (GameIntellectualContentInfo item : listQuestion) {
                logger.info(" id_game...." + item.getId_game());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception in function getListQuestion :", ex.getCause());
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return listQuestion;
    }

    public int checkCondition(String msisdn, String startTime, String endTime) {
        int result = 0;

        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;

        String sqlCommand = "select count(*) val from game_history where msisdn = ? and time_answer >= ? and time_answer <= ?";

        try {
            int i = 0;
            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(++i, msisdn);
            pstm.setString(++i, startTime);
            pstm.setString(++i, endTime);

            rs = pstm.executeQuery();
            conn.getConnection().commit();

            if (rs.next()) {
                result = rs.getInt("val");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exeption in function checkCondition dao :", ex.getCause());
            result = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return result;
    }

    public int getPointUser(String msisdn, int gameType) {
        int point = 0;

        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;

        String sqlCommand = " select point from game_point where msisdn = ? and game_type = ? ";

        try {
            int i = 0;
            getConnection().clearCachedStatements();
            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(++i, msisdn);
            pstm.setInt(++i, gameType);

            rs = pstm.executeQuery();
            conn.getConnection().commit();

            if (rs.next()) {
                point = rs.getInt("point");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception in function getPointUser....", ex.getCause());
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return point;
    }

    public int insertHistory(GameIntellectualHistoryInfo historyInfo, int action) {
        int result = -1;

        PreparedStatement pstm = null;
        ResultSet rs = null;
        DbConnection conn = null;

        String sqlComand = "";

        if (action == Constants.ACTION_ANSWER || action == Constants.ACTION_CHOOSE_START) {
            sqlComand = " insert into game_history (question_id, msisdn, key_answer, time_answer, result_correct, iscorrect, order_number_answer, action)"
                    + " values (?, ?, ?, now(), ?, ?, ?, ?)";
        } else {
            sqlComand = " insert into game_history (msisdn, action) "
                    + " values (?, ?)";
        }

        try {
            int i = 0;
            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlComand);
            if (action == Constants.ACTION_ANSWER) {
                pstm.setInt(++i, historyInfo.getQuestion_id());
                pstm.setString(++i, historyInfo.getMsisdn());
                pstm.setString(++i, historyInfo.getKey_answer());
                pstm.setString(++i, historyInfo.getResult_correct());
                pstm.setInt(++i, historyInfo.getIscorrect());
                pstm.setInt(++i, historyInfo.getOrder_number_answer());
                pstm.setInt(++i, action);
            } else {
                pstm.setString(++i, historyInfo.getMsisdn());
                pstm.setInt(++i, action);
            }

            result = pstm.executeUpdate();
            conn.getConnection().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception in function insertHistory dao", ex.getCause());
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return result;
    }

    public int updateStar(String msisdn, String startTime, String endTime) {
        int result = -1;

        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;

        String sqlCommand = " update game_history set action= 3 where msisdn = ? and action = 2 and game_type = 1 and created_at >= ? and created_at <= ? ";

        try {
            int i = 0;
            getConnection().clearCachedStatements();
            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(++i, msisdn);
            pstm.setString(++i, startTime);
            pstm.setString(++i, endTime);

            result = pstm.executeUpdate();

            conn.getConnection().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception in function updateStar dao...", ex.getCause());
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return result;
    }

    public boolean checkTimeRegisterPackage(String msisdn, String startTime, String endTime) {
        boolean result = false;

        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;

        String sqlCommand = "select count(*) val from sub_package where msisdn = ? and status = 1 and reg_at >= ? and reg_at <= ? ";

        try {
            int i = 0;
            getConnection().clearCachedStatements();
            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(++i, msisdn);
            pstm.setString(++i, startTime);
            pstm.setString(++i, endTime);

            rs = pstm.executeQuery();


            int value = 0;
            if (rs.next()) {
                value = rs.getInt("val");

            }
            if (value > 0) {
                result = true;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(" Exception in function checkTimeRegisterPackage dao", ex.getCause());
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return result;
    }

    public int insertPoint(String msisdn, int point) {
        int result = -1;

        PreparedStatement pstm = null;
        DbConnection conn = null;

        String sqlCommand = " insert into game_point (msisdn, point, created_at, updated_at) values (?, ?, now(), now()) ";

        try {
            int i = 0;

            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(++i, msisdn);
            pstm.setInt(++i, point);

            result = pstm.executeUpdate();

            conn.getConnection().commit();

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception in function insertPoint....", ex.getCause());
        } finally {
            closeStatement(pstm);
            freeConnection(conn);
        }

        return result;
    }

    public int updatePoint(String msisdn, int point) {
        int result = -1;

        PreparedStatement pstm = null;
        DbConnection conn = null;

        String sqlCommand = " update game_point set point= ?, updated_at = now() where  msisdn = ? and game_type= 1";

        try {
            int i = 0;

            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setInt(++i, point);
            pstm.setString(++i, msisdn);

            result = pstm.executeUpdate();

            conn.getConnection().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception in function updatePoint dao...", ex.getCause());
        } finally {
            closeStatement(pstm);
            freeConnection(conn);
        }

        return result;
    }

    public int checkExitsAccoutPoint(String msisdn) {
        int result = 0;
        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;

        String sqlCommand = "select count(*) val from game_point where msisdn = ? and game_type = 1";

        try {
  
            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(1, msisdn);

            rs = pstm.executeQuery();
            conn.getConnection().commit();

            if (rs.next()) {
                result = rs.getInt("val");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception in function checkExitsAccoutPoint dao..", ex.getCause());
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return result;
    }

    public int insertHistoryPoint(String msisdn, int point, int action) {

        int result = 0;
        PreparedStatement pstm = null;
        DbConnection conn = null;

        String sqlCommand = " insert into sub_point_history(msisdn, point, action, package_id, sub_package_id) values(?, ?, ?, 0, 0)";

        try {

            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            int i = 0;
            pstm.setString(++i, msisdn);
            pstm.setInt(++i, point);
            pstm.setInt(++i, action);

            result = pstm.executeUpdate();
            conn.getConnection().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(" Exception in function insertHistoryPoint dao ", ex.getCause());
        } finally {

            closeStatement(pstm);
            freeConnection(conn);
        }
        return result;
    }

    public List<Integer> getNumberAnswerCorrect(String msisdn, String startTime, String endTime, int isCorect) {
        List<Integer> listAnswer = new ArrayList<>();

        logger.info("startTime...." + startTime + " endTime...." + endTime);

        PreparedStatement pstm = null;
        ResultSet rs = null;
        DbConnection conn = null;

        String sqlCommand = " select order_number_answer from game_history where msisdn = ? "
                + " and time_answer >= ? and time_answer <= ? "
                + " and iscorrect = ? "
                + " order by order_number_answer ";

        try {
            int i = 0;
            getConnection().clearCachedStatements();
            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(++i, msisdn);
            pstm.setString(++i, startTime);
            pstm.setString(++i, endTime);
            pstm.setInt(++i, isCorect);

            rs = pstm.executeQuery();


            while (rs.next()) {
                listAnswer.add(rs.getInt("order_number_answer"));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(" Exception in function getNumberAnswerCorrect dao ", ex.getCause());
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return listAnswer;
    }

    public int insertTopup(String msisdn, int price, String programName) {
        int result = -1;

        PreparedStatement pstm = null;
        DbConnection conn = null;

        String sqlCommand = " insert into topup (msisdn, price, program_name) "
                + " values (?, ?, ?)";

        try {
            int i = 0;

            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(++i, msisdn);
            pstm.setInt(++i, price);
            pstm.setString(++i, programName);

            result = pstm.executeUpdate();

            conn.getConnection().commit();

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(" Exception in function insertTopup dao ", ex.getCause());
        } finally {
            closeStatement(pstm);
            freeConnection(conn);
        }

        return result;
    }
        public boolean checkExitsTopup(String msisdn, String startTime, String endTime) {
        boolean result = false;

        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;

        String sqlCommand = "select count(*) val from topup where msisdn = ? and created_date >= ? and created_date <= ? and program_name = 'GAME_ALOFUN' ";

        try {
            int i = 0;

            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setString(++i, msisdn);
            pstm.setString(++i, startTime);
            pstm.setString(++i, endTime);

            rs = pstm.executeQuery();
            conn.getConnection().commit();

            int value = 0;
            if (rs.next()) {
                value = rs.getInt("val");

            }
            if (value > 0) {
                result = true;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(" Exception in function checkExitsTopup dao", ex.getCause());
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return result;
    }
}
