/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api;

import com.vega.alome.sbb.billing.bundletype.BillingSBBInterface;
import static com.vega.service.api.RestfulStack.logger;
import com.vega.service.api.common.Constants;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.GameIntellectualDao;
import com.vega.service.api.object.GameIntellectualContentInfo;
import com.vega.service.api.object.GameIntellectualHistoryInfo;
import com.vega.service.api.object.SMS;
import com.vega.service.api.object.SMSType;
import com.vega.service.api.response.ResultGame;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.naming.NamingException;
import org.apache.log4j.Logger;

/**
 *
 * @author Nora
 */
public class GameIntellectualStack {

    static Logger logger = Logger.getLogger(WeatherStack.class);
    GameIntellectualDao db;

    private RestfulStack resfulStack;

    public GameIntellectualStack() throws NamingException {
        this.db = new GameIntellectualDao();
        db.start();
    }

    public Object checkCondition(HashMap<String, String> param) {
        ResultGame rs = new ResultGame();

        if (param.get("msisdn") == null || "".equals(param.get("msisdn"))) {
            rs.setDesc("Sai hoặc thiếu tham số");
            rs.setErrorCode(Constants.WRONG_PARAM);
            return rs;
        }
        String msisdn = param.get("msisdn");

        // lay ngay hien tai
        long millis = System.currentTimeMillis();
        Date date = new Date(millis);

        int isCondition = db.checkCondition(msisdn, date.toString() + " 00:00:00", date.toString() + " 23:59:59");

        if (isCondition < 0) {
            rs.setDesc("Lỗi hệ thống");
            rs.setErrorCode(Constants.SYSTEM_ERROR);
        } else if (isCondition >= Constants.MAX_QUESTION_BY_USER) {
            rs.setDesc("Hết quyền chơi");
            rs.setErrorCode(Constants.NO_DATA_FOUND);

        } else {
            rs.setDesc("Còn điều kiện chơi game");
            rs.setErrorCode(Constants.SUCCESS);
        }

        return rs;
    }

    public Object getPointUser(HashMap<String, String> param) {
        ResultGame result = new ResultGame();

        if (param.get("msisdn") == null || "".equals(param.get("msisdn"))) {
            result.setDesc("Sai hoặc thiếu tham số");
            result.setErrorCode(Constants.WRONG_PARAM);
            return result;
        }
        try {
            String msisdn = param.get("msisdn");

            int point = db.getPointUser(msisdn, Constants.GAME_TYPE_INTELLECTUAL);

            result.setErrorCode(Constants.SUCCESS);
            result.setPoint(point);

        } catch (Exception ex) {
            ex.printStackTrace();
            result.setErrorCode(Constants.SYSTEM_ERROR);
            return result;
        }

        return result;
    }

    public Object getQuestion(HashMap<String, String> param) {
        ResultGame result = new ResultGame();

        if (param.get("msisdn") == null || "".equals(param.get("msisdn"))) {
            result.setDesc("Sai hoặc thiếu tham số");
            result.setErrorCode(Constants.WRONG_PARAM);
            return result;
        }
        String msisdn = param.get("msisdn");

        long millis = System.currentTimeMillis();
        Date date = new Date(millis);

        // Lay lich su tra loi cua hoi cua khach hang trong ngay hien tại
        String listIdHistoryDay = db.getHistoryGame(msisdn, date.toString() + " 00:00:00", date.toString() + " 23:59:59", -1);

        int limitQuestion = Constants.LIMIT_QUESTION;
        int lengtHistoryDay = 0;
        if (listIdHistoryDay.length() > 0) {
            lengtHistoryDay = listIdHistoryDay.split(",").length;
        }

        if (lengtHistoryDay > 0) {
            // Nếu người dùng đã chơi trong ngày thì lấy limit theo số câu còn lại
            limitQuestion = Constants.LIMIT_QUESTION - lengtHistoryDay;
        }

        // Lay lich sử khách hàng đã trả lời những lần trước
        String listIdHistory = db.getHistoryGame(msisdn, "", "", -1);

        logger.info("listIdHistory......" + listIdHistory);

        // lay danh sach cau hoi
        List<GameIntellectualContentInfo> listQuestion = db.getListQuestion(listIdHistory, limitQuestion, "");
        if (listQuestion.size() <= 0) {
            listQuestion = db.getListQuestion(listIdHistoryDay, limitQuestion, "");
        }

        logger.info("limitQuestion......." + limitQuestion + " and listQuestion size...." + listQuestion.size());

        if (listQuestion.size() < limitQuestion) {
            // Nhung truong hop da tra loi het khong du cau hoi moi thi lay lai cau hoi cu
            int limitMore = 0;
            if (lengtHistoryDay > 0) {
                logger.info("listIdHistoryDay..." + listIdHistoryDay + " and lengtHistoryDay..." + lengtHistoryDay);
                limitMore = Constants.LIMIT_QUESTION - (lengtHistoryDay + listQuestion.size());
            } else {
                limitMore = Constants.LIMIT_QUESTION - listQuestion.size();
            }
            // Lay lich sử khách hàng đã trả lời những lần trước
            String listIdHistoryLimit = db.getHistoryGame(msisdn, "", "", limitMore + 1);
            if(!(listIdHistoryLimit != null && listIdHistoryLimit.length() > 0)){
                listIdHistoryLimit = "";
            }
            List<GameIntellectualContentInfo> listQuestionMore = db.getListQuestion("", limitMore, listIdHistoryLimit);
            int index = listQuestion.size() + 1;
            for (GameIntellectualContentInfo item : listQuestionMore) {
                item.setIndex(index);
                item.setId_game(index);
                listQuestion.add(item);
                index++;
            }

        }

        if (lengtHistoryDay > 0) {
            logger.info(" lengtHistoryDay vao đây không..." + lengtHistoryDay);
            // Tiến hành format lại index câu hỏi về vị trí tiếp sau câu hỏi đã tl
            for (GameIntellectualContentInfo item : listQuestion) {
                lengtHistoryDay = lengtHistoryDay + 1;
                item.setIndex(lengtHistoryDay);
            }
        }

        if (listQuestion.size() <= 0) {
            result.setDesc("Không có dữ liệu");
            result.setErrorCode(Constants.NO_DATA_FOUND);
            return result;
        }

        result.setErrorCode(Constants.SUCCESS);
        result.setData(listQuestion);
        return result;
    }

    public Object saveHistory(HashMap<String, String> param) {
        ResultGame result = new ResultGame();

        if (param.get("msisdn") == null || "".equals(param.get("msisdn"))
                || param.get("action") == null || "".equals(param.get("action"))) {
            result.setDesc("Sai hoặc thiếu tham số");
            result.setErrorCode(Constants.WRONG_PARAM);
            return result;
        }
        String msisdn = param.get("msisdn");
        int action = Integer.parseInt(param.get("action"));

        GameIntellectualHistoryInfo history = null;
        if (action == Constants.ACTION_ANSWER || action == Constants.ACTION_CHOOSE_START) {

            if (param.get("question_id") == null || "".equals(param.get("question_id"))
                    || param.get("result_correct") == null || "".equals(param.get("result_correct"))
                    || param.get("order_number_answer") == null || "".equals(param.get("order_number_answer"))) {
                result.setDesc("Sai hoặc thiếu tham số");
                result.setErrorCode(Constants.WRONG_PARAM);
                // key_answer=D&action=1&order_number_answer=1&result_correct=A&question_id=26&iscorrect=false?msisdn=84973006457
                return result;
            }

            int questionId = Integer.parseInt(param.get("question_id"));
            String keyAnswer = param.get("key_answer");
            String resultCorrect = param.get("result_correct");
            int iscorrect = 0;
            if (keyAnswer.trim().equals(resultCorrect.trim())) {
                iscorrect = 1;
            }

            int orderNumberAnswer = Integer.parseInt(param.get("order_number_answer"));

            logger.info("orderNumberAnswer............." + orderNumberAnswer);

            history = new GameIntellectualHistoryInfo(questionId, msisdn, keyAnswer, resultCorrect, iscorrect, orderNumberAnswer, action);

        } else {
            history = new GameIntellectualHistoryInfo(msisdn, action);
        }

        int rsInsert = db.insertHistory(history, action);

        if (rsInsert < 0) {
            result.setDesc("Lỗi khi insert lịch sử");
            result.setErrorCode(Constants.SYSTEM_ERROR);
            return result;
        }

        result.setDesc("Lưu lịch sử thành công");
        result.setErrorCode(Constants.SUCCESS);

        return result;
    }

    public Object checkConditionResetPoint(HashMap<String, String> param) {
        ResultGame result = new ResultGame();

        if (param.get("msisdn") == null || "".equals(param.get("msisdn"))) {
            result.setDesc("Sai hoặc thiếu tham số");
            result.setErrorCode(Constants.WRONG_PARAM);
            return result;
        }
        String msisdn = param.get("msisdn");

        long millis = System.currentTimeMillis();
        Date date = new Date(millis);

        // kiem tra xem thue bao co dang ki trong ngay hay khong
        boolean checkTime = db.checkTimeRegisterPackage(msisdn, date.toString() + " 00:00:00", date.toString() + " 23:59:59");

        // lay lich sử trả lời game
        String history = db.getHistoryGame(msisdn, date.toString() + " 00:00:00", date.toString() + " 23:59:59", -1);

        // lay điểm của thuê bao
        int point = db.getPointUser(msisdn, Constants.GAME_TYPE_INTELLECTUAL);

        if (checkTime && history.length() <= 0 && point > 0) {
            // Thue bao mới đăng ki lại nên reset số điểm đã có
            int rsUpdate = db.updatePoint(msisdn, 0);
            if (rsUpdate > 0) {
                // lưu lịch sử trừ điểm
                db.insertHistoryPoint(msisdn, point, Constants.RESET_POINT);
                result.setDesc("Tiến hành trừ điểm do không duy trì dịch vụ");
                result.setErrorCode(Constants.DATA_EXIST);
                return result;
            }
        }
        result.setDesc("Thuê bao không năm trong danh sách bị trừ điểm");
        result.setErrorCode(Constants.SUCCESS);
        return result;
    }

    public Object getNumAnswerCorrect(HashMap<String, String> param) {
        ResultGame result = new ResultGame();

        if (param.get("msisdn") == null || "".equals(param.get("msisdn"))) {
            result.setDesc("Sai hoặc thiếu tham số");
            result.setErrorCode(Constants.WRONG_PARAM);
            return result;
        }
        String msisdn = param.get("msisdn");

        long millis = System.currentTimeMillis();
        Date date = new Date(millis);

        List<Integer> listAnswerCorrect = nextSequence(db.getNumberAnswerCorrect(msisdn, date.toString() + " 00:00:00", date.toString() + " 23:59:59", Constants.CORRECT_ANSWER));

        List<Integer> listAnswerFailed = db.getNumberAnswerCorrect(msisdn, date.toString() + " 00:00:00", date.toString() + " 23:59:59", Constants.FAILED_ANSWER);

        if (listAnswerCorrect.size() <= 0 && listAnswerFailed.size() <= 0) {
            result.setDesc("Không có dữ liệu");
            result.setErrorCode(Constants.NO_DATA_FOUND);
            return result;
        }

        result.setErrorCode(Constants.SUCCESS);
        result.setDataAnswerCorrect(listAnswerCorrect);
        result.setDataAnswerFailed(listAnswerFailed);

        return result;
    }

    public Object chooseStar(HashMap<String, String> param) {

        ResultGame result = new ResultGame();

        if (param.get("msisdn") == null || "".equals(param.get("msisdn"))) {
            result.setDesc("Sai hoặc thiếu tham số");
            result.setErrorCode(Constants.WRONG_PARAM);
            return result;
        }

        String msisdn = param.get("msisdn");

        long millis = System.currentTimeMillis();
        Date date = new Date(millis);

        try {
            int rs = db.chooseStar(msisdn, date.toString() + " 00:00:00", date.toString() + " 23:59:59");

            logger.info(" start choose :" + rs);
            result.setActionStar(rs);
            result.setErrorCode(Constants.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            result.setDesc("Exception");
            result.setErrorCode(Constants.SYSTEM_ERROR);
            return result;
        }

        return result;
    }

    public Object insertOrUpdatePoint(HashMap<String, String> param) {
        ResultGame result = new ResultGame();

        if (param.get("msisdn") == null || "".equals(param.get("msisdn"))
                || param.get("point") == null || "".equals(param.get("point"))
                || param.get("action") == null || "".equals(param.get("action"))) {
            result.setDesc("Sai hoặc thiếu tham số");
            result.setErrorCode(Constants.WRONG_PARAM);
            return result;

        }

        try {

            long millis = System.currentTimeMillis();
            Date date = new Date(millis);
            // Tiến hành lưu thông tin vào topup
            String msisdn = param.get("msisdn");
            int point = Integer.parseInt(param.get("point"));
            int action = Integer.parseInt(param.get("action"));

            boolean isTopup = db.checkExitsTopup(msisdn, date.toString() + " 00:00:00", date.toString() + " 23:59:59");
            if (point >= 10 && !isTopup) {
                int price = 10000;
                String programName = "GAME_ALOFUN";

                db.insertTopup(msisdn, price, programName);

                String timeCurrent[] = date.toString().split("-");

                SMS sms = new SMS();
                int mtType = SMSType.Nofity.getValue();
                String mt = ConfigStack.getConfig("mt", "mt_notify_game_alofun", "");

                mt = mt.replaceAll("\\{dd\\}", timeCurrent[2]);
                mt = mt.replaceAll("\\{mm\\}", timeCurrent[1]);
                mt = mt.replaceAll("\\{yyyy\\}", timeCurrent[0]);
                logger.info(" MT trung thuong :" + mt);
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction(programName);
                sms.setSource("SMS");
                sms.setPackageId(0);
                sms.setBrandName(true);
                resfulStack.sendMT(sms);


            }

            // tiến hành kiểm tra có tài khoản điểm chưa
            int isAccount = db.checkExitsAccoutPoint(msisdn);
            int rs = -1;
            if (isAccount > 0) {
                // Tiến hành update
                rs = db.updatePoint(msisdn, point);
            } else {
                // Tiến hành insert
                rs = db.insertPoint(msisdn, point);
            }

            if (rs > 0) {
                db.insertHistoryPoint(msisdn, point, action);
                result.setErrorCode(Constants.SUCCESS);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.setDesc("Exception");
            result.setErrorCode(Constants.SYSTEM_ERROR);
            return result;
        }

        return result;
    }

    public Object insertTopup(HashMap<String, String> param) {
        ResultGame result = new ResultGame();

        if (param.get("msisdn") == null || "".equals(param.get("msisdn"))
                || param.get("price") == null || "".equals(param.get("price"))) {
            result.setDesc("Sai hoặc thiếu tham số");
            result.setErrorCode(Constants.WRONG_PARAM);
            return result;
        }

        String msisdn = param.get("msisdn");
        int price = Integer.parseInt(param.get("price"));
        String programName = "GAME_ALOFUN";

        int rs = db.insertTopup(msisdn, price, programName);

        if (rs <= 0) {
            result.setDesc("Không thành công");
            result.setErrorCode(Constants.NO_DATA_FOUND);
            return result;
        }
        result.setDesc("Thành công");
        result.setErrorCode(Constants.SUCCESS);
        return result;
    }

    public Object updateStar(HashMap<String, String> param) {
        ResultGame result = new ResultGame();

        if (param.get("msisdn") == null || "".equals(param.get("msisdn"))) {
            result.setDesc("Sai hoac thieu tham so");
            result.setErrorCode(Constants.WRONG_PARAM);
            return result;
        }

        String msisdn = param.get("msisdn");

        long millis = System.currentTimeMillis();
        Date date = new Date(millis);

        int rs = db.updateStar(msisdn, date.toString() + " 00:00:00", date.toString() + " 23:59:59");

        if (rs <= 0) {
            result.setDesc("Không thành công");
            result.setErrorCode(Constants.NO_DATA_FOUND);
            return result;
        }

        result.setDesc("Thành công");
        result.setErrorCode(Constants.SUCCESS);
        return result;
    }

    private List<Integer> nextSequence(List<Integer> listValue) {

        List<Integer> listNewValue = new ArrayList<Integer>();

        if (listValue.size() <= 0) {
            return listValue;
        }

        listNewValue.add(listValue.get(0));

        for (int i = 1; i < listValue.size(); i++) {

            if (listValue.get(i - 1) == listValue.get(i) - 1 && listNewValue.get(listNewValue.size() - 1) == listValue.get(i) - 1) {
                listNewValue.add(listValue.get(i));
            } else {
                if (listNewValue.size() < 7) {
                    listNewValue.clear();
                    listNewValue.add(listValue.get(i));
                }
            }
        }

        return listNewValue;
    }

    public RestfulStack getResfulStack() {
        return resfulStack;
    }

    public void setResfulStack(RestfulStack resfulStack) {
        this.resfulStack = resfulStack;
    }
}
