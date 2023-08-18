/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api;

import com.vega.service.api.common.Constants;
import com.vega.service.api.common.Helper;
import com.vega.service.api.config.ConfigStack;
import javax.naming.NamingException;
import org.apache.log4j.Logger;
import com.vega.service.api.db.GameDao;
import com.vega.service.api.object.SMS;
import com.vega.service.api.response.Result;
import java.util.HashMap;

/**
 *
 * @author N.Tuyen
 */
public class GameStack {

    static Logger logger = Logger.getLogger(GameStack.class);
    GameDao dao;
    // cac api xo so
    private RestfulStack restfulStack;

    public GameStack() throws NamingException {
        this.dao = new GameDao();
        dao.start();
    }
    /*
     * Minigame: Tra loi cau hoi game Suc khoe
     */

    public Object answerHealthGame(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int contentId = Helper.getInt(params.get("contentId"), 0);
        int subPackageId = Helper.getInt(params.get("subPackageId"), 0);
        int packageId = Helper.getInt(params.get("packageId"), 0);
        boolean rightAnswer = Helper.getInt(params.get("rightAnswer"), 0) == 1;
        String key = params.get("key");

        int gameKey = Constants.GAME_KEY_HEALTH;
        int point = Helper.getInt(ConfigStack.getConfig("game_health", "point_question", "50"), 50);
        int pointAction = Constants.POINT_ACTION_HEALTH_GAME;

        if (msisdn != null && contentId > 0) {
            int result = dao.answerQuestionOfGame(msisdn, subPackageId, packageId, contentId, rightAnswer, point, gameKey, pointAction, key);
            if (result >= 0) {
                if (rightAnswer) {
                    /*
                     * Gui MT thong bao diem thuong
                     */
                    String mt = ConfigStack.getConfig("mt", "GAME_HEALTH_POINT", "");
                    mt = mt.replaceAll("\\{diem\\}", String.valueOf(point));
                    mt = mt.replaceAll("\\{tong_diem\\}", String.valueOf(result));
                    SMS sms = new SMS();
                    sms.setMsisdn(msisdn);
                    sms.setMtContent(mt);
                    sms.setType(1);
                    sms.setAction("SEND_MT");
                    sms.setPackageId(0);
                    logger.info("Contetn :" + mt);
                    restfulStack.sendMT(sms);
                }

                r.setErrorCode(Constants.SUCCESS);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }

        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    /*
     * Minigame: Tu choi game Suc khoe
     */
    public Object rejectHealthGame(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int scriptNo = Helper.getInt(params.get("scriptNo"), 0);
        int contentId = Helper.getInt(params.get("contentId"), 0);

        int rejectCountConfirm = Helper.getInt(ConfigStack.getConfig("game_health", "reject_count_confirm", "50"), 3);

        int gameKey = Constants.GAME_KEY_HEALTH;

        if (msisdn != null) {
            int rejectCount = dao.rejectPlayGame(msisdn, gameKey, scriptNo, contentId, rejectCountConfirm);
            if (rejectCount > 0) {
                if (rejectCount % rejectCountConfirm == 0) {
                    /*
                     * Gui SMS thong bao xac nhan tu choi tham gia
                     */

                    String mt = ConfigStack.getConfig("mt", "HEALTH_GAME_REJECT_CONFIRM", "");
                    SMS sms = new SMS();
                    sms.setMsisdn(msisdn);
                    sms.setMtContent(mt);
                    sms.setType(1);
                    sms.setAction("SEND_MT");
                    sms.setPackageId(0);
                    logger.info("Contetn :" + mt);
                    restfulStack.sendMT(sms);
                }

                r.setErrorCode(Constants.SUCCESS);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }

        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }
    public GameDao getGameDAO(){
        return dao;
    }

    /**
     * @return the restfulStack
     */
    public RestfulStack getRestfulStack() {
        return restfulStack;
    }

    /**
     * @param restfulStack the restfulStack to set
     */
    public void setRestfulStack(RestfulStack restfulStack) {
        this.restfulStack = restfulStack;
    }
}
