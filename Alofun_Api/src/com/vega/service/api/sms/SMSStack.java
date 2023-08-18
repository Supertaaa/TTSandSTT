/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.sms;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.vega.service.api.RestfulStack;
import static com.vega.service.api.RestfulStack.getFromCache;
import static com.vega.service.api.RestfulStack.pushToCacheWithExpiredTime;
import static com.vega.service.api.RestfulStack.redisManager;
import com.vega.service.api.billing.BillingStack;
import com.vega.service.api.charging.ChargingStack;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.ConvertUnsignedString;
import com.vega.service.api.common.GiftAccountInfo;
import com.vega.service.api.common.GiftContentInfo;
import com.vega.service.api.common.Helper;
import com.vega.service.api.common.MD5_Hash;
import com.vega.service.api.common.ProvinceInfo;
import com.vega.service.api.common.RandomStringUtils;
import com.vega.service.api.common.SubMessageInfo;
import com.vega.service.api.common.SubProfileInfo;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.ConnectDao;
import com.vega.service.api.db.DBStack;
import com.vega.service.api.db.GameDao;
import com.vega.service.api.db.GiftDAO;
import com.vega.service.api.db.LotteryDao;
import com.vega.service.api.db.WeatherDao;
import com.vega.service.api.object.BillingErrorCode;
import com.vega.service.api.object.Content;
import com.vega.service.api.object.LotteryHisDTO;
import com.vega.service.api.object.SMS;
import com.vega.service.api.object.SMSType;
import com.vega.service.api.object.SubPackageInfo;
import com.vega.service.api.object.SubPackageInfo.SubPackageStatus;
import com.vega.service.api.object.User;
import com.vega.service.api.response.Result;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import javax.naming.NamingException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author PhongTom
 */
public class SMSStack {

    static transient Logger logger = Logger.getLogger(SMSStack.class);
    private BillingStack billing;
    private DBStack db;
    private LotteryDao lotteryDao;
    private WeatherDao weatherDao;
    private GameDao gameDao;
    private GiftDAO giftDao;
    private ConnectDao connectDao;
    RestfulStack restfulStack;
    Gson gson = new Gson();
    Client client;
    ChargingStack chargingStack;

    public SMSStack() throws NamingException, Exception {
        lotteryDao = new LotteryDao();
        lotteryDao.start();
        gameDao = new GameDao();
        giftDao = new GiftDAO();
        gameDao.start();
        giftDao.init();
        weatherDao = new WeatherDao();
        weatherDao.start();
        connectDao = new ConnectDao();
        connectDao.start();

    }

    public BillingStack getBilling() {
        return billing;
    }

    public void setBilling(BillingStack billing) {
        this.billing = billing;
    }

    public ChargingStack getChargingStack() {
        return chargingStack;
    }

    public void setChargingStack(ChargingStack chargingStack) {
        this.chargingStack = chargingStack;
    }

    public DBStack getDb() {
        return db;
    }

    public void setDb(DBStack db) {
        this.db = db;
    }

    public RestfulStack getRestfulStack() {
        return restfulStack;
    }

    public void setRestfulStack(RestfulStack restfulStack) {
        this.restfulStack = restfulStack;
    }

    public SMS receiveSMS(SMS sms) {
        String mt = "";
        SMS smsResponeMPS = new SMS();
        String content = sms.getMoContent().trim();
        content = ConvertUnsignedString.convert(ConvertUnsignedString.compositeToPrecomposed(content));
        if (Helper.isNull(content) || Helper.isNull(sms.getMsisdn())) {
            logger.error("Params of MOEvent is invalid");
            mt = ConfigStack.getConfig("mt", "mt_wrong_syntax", "");
            smsResponeMPS.setMoContent(content);
            smsResponeMPS.setMtContent(mt);
            return smsResponeMPS;
        }

        logger.info("MO => msisdn: " + sms.getMsisdn() + "; content: "
                + content);
        content = Helper.convert(content);
        content = Helper.removeDoubleSpace(content);
        content = content.toUpperCase();
        String keyword = content.split(" ")[0].trim();
        sms.setMoKeyword(keyword);
        boolean isFound = false;
//        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_send_gift", "QUA").toUpperCase().contains(keyword)) {
//            String[] info = ConfigStack.getConfig("api_sms", "keyword_send_gift", "QUA").toUpperCase().split(",");
//            for (int i = 0; i < info.length; i++) {
//                if (info[i].equals(keyword)) {
//                    isFound = true;
//                    gift(sms);
//                    break;
//                }
//            }
//        }
//        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_register_gift", "NQ").toUpperCase().contains(keyword)) {
//            String[] info = ConfigStack.getConfig("api_sms", "keyword_register_gift", "NQ").toUpperCase().split(",");
//            for (int i = 0; i < info.length; i++) {
//                if (info[i].equals(keyword)) {
//                    isFound = true;
//                    registerGift(sms);
//                    break;
//                }
//            }
//        }
//        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_unregister_gift", "TCQ").toUpperCase().contains(keyword)) {
//            String[] info = ConfigStack.getConfig("api_sms", "keyword_unregister_gift", "TCQ").toUpperCase().split(",");
//            for (int i = 0; i < info.length; i++) {
//                if (info[i].equals(keyword)) {
//                    isFound = true;
//                    unRegisterGift(sms);
//                    break;
//                }
//            }
//        }
        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_mk", "MK").toUpperCase().equals(keyword)) {
            String[] info = ConfigStack.getConfig("api_sms", "keyword_mk", "MK").toUpperCase().split(",");
            for (int i = 0; i < info.length; i++) {
                if (info[i].equals(keyword)) {
                    isFound = true;
                    mk(sms);
                    break;
                }
            }
        }
        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_kt", "KT").toUpperCase().equals(keyword)) {
            String[] info = ConfigStack.getConfig("api_sms", "keyword_kt", "KT").toUpperCase().split(",");
            for (int i = 0; i < info.length; i++) {
                if (info[i].equals(keyword)) {
                    isFound = true;
                    mt = kt(sms);
                    smsResponeMPS.setMoContent(content);
                    smsResponeMPS.setMtContent(mt);
                    break;
                }
            }
            return smsResponeMPS;
        }
//        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_register_sms", "DK SMS").toUpperCase().contains(content)) {
//            String[] info = ConfigStack.getConfig("api_sms", "keyword_register_sms", "DK SMS").toUpperCase().split(",");
//            for (int i = 0; i < info.length; i++) {
//                if (info[i].equals(content)) {
//                    isFound = true;
//                    CallRegisModuleVasApi(sms);
//                    break;
//                }
//            }
//        }
//        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_unregister_sms", "TC SMS").toUpperCase().contains(content)) {
//            String[] info = ConfigStack.getConfig("api_sms", "keyword_unregister_sms", "TC SMS").toUpperCase().split(",");
//            for (int i = 0; i < info.length; i++) {
//                if (info[i].equals(content)) {
//                    isFound = true;
//                    unRegisterSMS(sms);
//                    break;
//                }
//            }
//        }
//        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_guide", "HD").toUpperCase().contains(keyword)) {
//            String[] info = ConfigStack.getConfig("api_sms", "keyword_guide", "HD").toUpperCase().split(",");
//            for (int i = 0; i < info.length; i++) {
//                if (info[i].equals(keyword)) {
//                    isFound = true;
//                    mt = guide(sms);
//                    smsResponeMPS.setMoContent(content);
//                    smsResponeMPS.setMtContent(mt);
//                    return smsResponeMPS;
//                }
//            }
//        }
//        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_guide_ams", "HD1").toUpperCase().contains(keyword)) {
//            String[] info = ConfigStack.getConfig("api_sms", "keyword_guide_ams", "HD1").toUpperCase().split(",");
//            for (int i = 0; i < info.length; i++) {
//                if (info[i].equals(keyword)) {
//                    isFound = true;
//                    guideAMS(sms);
//                    break;
//                }
//            }
//        }
        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_find_music", "MA").toUpperCase().equals(keyword)) {
            // Tim ma bai hat
            String[] info = ConfigStack.getConfig("api_sms", "keyword_find_music", "MA").toUpperCase().split(",");
            for (int i = 0; i < info.length; i++) {
                if (info[i].equals(keyword)) {
                    isFound = true;
                    mt = findMusic(sms);
                    smsResponeMPS.setMoContent(content);
                    smsResponeMPS.setMtContent(mt);
                    break;
                }
            }
            return smsResponeMPS;
        }
        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_find_story", "T").toUpperCase().equals(keyword)) {
            // Tim ma truyen
            String[] info = ConfigStack.getConfig("api_sms", "keyword_find_story", "T").toUpperCase().split(",");
            for (int i = 0; i < info.length; i++) {
                if (info[i].equals(keyword)) {
                    isFound = true;
                    mt = findStory(sms);
                    smsResponeMPS.setMoContent(content);
                    smsResponeMPS.setMtContent(mt);
                    break;
                }
            }
            return smsResponeMPS;
        }
        
        if (!isFound && ConfigStack.getConfig("api_sms", "keyword_check_minutes", "TraPhut").replaceAll("\\s+","").toUpperCase().contains(keyword)) {
            // Tra phut
            String[] info = ConfigStack.getConfig("api_sms", "keyword_check_minutes", "TraPhut").replaceAll("\\s+","").toUpperCase().split(",");
            for (int i = 0; i < info.length; i++) {
                if (info[i].equals(keyword)) {
                    isFound = true;
                    mt = checkMinutes(sms);
                    smsResponeMPS.setMoContent(content);
                    smsResponeMPS.setMtContent(mt);
                    break;
                }
            }
            return smsResponeMPS;
        }
//
//        String upContent = this.formatMessage(content).toUpperCase();
//        logger.info("AAAAAAAAAAAAAA content: " + content);
//        logger.info("AAAAAAAAAAAAAA keyword: " + keyword);
//        logger.info("AAAAAAAAAAAAAA config: " + ConfigStack.getConfig("mo", "WEATHER_REJECT", "tc_thoitiet").toUpperCase());
//        if (!isFound && ConfigStack.getConfig("mo", "WEATHER_REJECT", "tc thoitiet").toUpperCase().contains(content.toUpperCase())) {
//            logger.info(">>>>>>>>>>>>>>>>> WEATHER_REJECT: " + content);
//            logger.info(">>>>>>>>>>>>>>>>> config: " + ConfigStack.getConfig("mo", "WEATHER_REJECT ", "tc thoitiet"));
//            rejectWeather(sms);
//            isFound = true;
//        }
//        if (!isFound && ConfigStack.getConfig("mo", "FRIEND_GUIDE", "KB").toUpperCase().contains(upContent)) {
//            /*
//             * Huong dan tinh nang Ket ban
//             */
//            logger.info("AAAAAAAAAAAAAA content: " + content);
//            logger.info("AAAAAAAAAAAAAA config: " + ConfigStack.getConfig("mo", "FRIEND_GUIDE", "KB"));
//            guideMakeFriend(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_DECLARE_PROFILE", "TK").toUpperCase().contains(keyword.toUpperCase())) {
//            /*
//             * Khai bao thong tin ca nhan
//             */
//            declareProfile(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_CHAT", "").toUpperCase().contains(upContent)) {
//            /*
//             * Lay ban be ngau nhien
//             */
//            processChat(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_LIST", "").toUpperCase().contains(upContent)) {
//            /*
//             * Lay danh sach ban be trong friendlist
//             */
//            processFriendList(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_SEARCH", "").toUpperCase().contains(keyword.toUpperCase())) {
//            /*
//             * Tim kiem ban be
//             */
//            processSearchFriend(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_VIEW_ID", "").toUpperCase().contains(keyword.toUpperCase())) {
//            /*
//             * Xem thong tin cua thanh vien
//             */
//            viewFriendProfile(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_ADD", "").toUpperCase().contains(keyword.toUpperCase())) {
//            /*
//             * Them ban vao friendlist
//             */
//            addToFriendlist(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_REMOVE", "").toUpperCase().contains(keyword.toUpperCase())) {
//            /*
//             * Xoa ban ra khoi friendlist
//             */
//            removeFromFriendlist(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_VIEW_PROFILE", "HS").toUpperCase().contains(upContent)) {
//            /*
//             * Xem thong tin ca nhan
//             */
//            logger.info(">>>>> Goi ham ViewProfile with : " + upContent);
//            viewProfile(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_ADD_BLACKLIST", "").toUpperCase().contains(keyword.toUpperCase())) {
//            /*
//             * Them ID vao blacklist ca nhan
//             */
//            addToBlacklistOfUser(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_REMOVE_BLACKLIST", "").toUpperCase().contains(keyword.toUpperCase())) {
//            /*
//             * Xoa ID khoi blacklist ca nhan
//             */
//            removeFromBlacklistOfUser(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_VIEW_BLACKLIST", "").toUpperCase().contains(upContent)) {
//            /*
//             * Xem blacklist ca nhan
//             */
//            viewBlacklistOfUser(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "FRIEND_INVITE", "").toUpperCase().contains(keyword.toUpperCase())) {
//            /*
//             * Gioi thieu ban be
//             */
//            inviteFriend(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "TC", "TC").toUpperCase().contains(keyword.toUpperCase())) {
//            /*
//             * Tu choi nhan tin
//             */
//            addSubBlackList(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "LOTTERY_GUIDE", "lottery").toUpperCase().contains(keyword.toUpperCase())) {
//            /*
//             * Huong dan tinh nang Ket ban
//             */
//            logger.info("AAAAAAAAAAAAAA content: " + content);
//            logger.info("AAAAAAAAAAAAAA config: " + ConfigStack.getConfig("mo", "LOTTERY_GUIDE", "lottery"));
//            /*
//             * Huong dan xo so
//             */
//            guideLottery(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "LOTTERY_REG_CALLOUT", "lottery").toUpperCase().contains(keyword.toUpperCase())) {
//            logger.info("AAAAAAAAAAAAAA content: " + content);
//            logger.info("AAAAAAAAAAAAAA config: " + ConfigStack.getConfig("mo", "LOTTERY_REG_CALLOUT", "lottery"));
//            /*
//             * Dang ky nhan callout xo so
//             */
//            registerLotteryCallout(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "LOTTERY_CANCEL_CALLOUT", "abc").toUpperCase().contains(keyword.toUpperCase())) {
//            logger.info("AAAAAAAAAAAAAA xo so: " + content);
//            logger.info("AAAAAAAAAAAAAA config: " + ConfigStack.getConfig("mo", "LOTTERY_CANCEL_CALLOUT", "abc"));
//            cancelLotteryCallout(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "GAME_HEALTH_REG", "SK").toUpperCase().contains(keyword.toUpperCase())) {
//            logger.info("AAAAAAAAAAAAAA GAME_HEALTH: " + content);
//            logger.info("AAAAAAAAAAAAAA config regHealthGame: " + ConfigStack.getConfig("mo", "GAME_HEALTH_REG", "SK"));
//            regHealthGame(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "GAME_HEALTH_REJECT ", "YES3").toUpperCase().contains(keyword.toUpperCase())) {
//            logger.info("AAAAAAAAAAAAAA GAME_HEALTH: " + content);
//            logger.info("AAAAAAAAAAAAAA config: " + ConfigStack.getConfig("mo", "GAME_HEALTH_REJECT ", "YES3"));
//            rejectHealthGame(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "GAME_CONNECT_REJECT ", "YesKN").toUpperCase().contains(keyword.toUpperCase())) {
//            logger.info("AAAAAAAAAAAAAA GAME_HEALTH: " + content);
//            logger.info("AAAAAAAAAAAAAA config: " + ConfigStack.getConfig("mo", "GAME_CONNECT_REJECT ", "YesKN"));
//            rejectConnectGame(sms);
//            isFound = true;
//        } else if (!isFound && ConfigStack.getConfig("mo", "GAME_CONNECT_ACCEPT ", "KETNOI").toUpperCase().contains(keyword.toUpperCase())) {
//            logger.info("AAAAAAAAAAAAAA GAME_HEALTH: " + content);
//            logger.info("AAAAAAAAAAAAAA config: " + ConfigStack.getConfig("mo", "GAME_CONNECT_ACCEPT ", "KETNOI"));
//            acceptConnectGame(sms);
//            isFound = true;
//        }

        if (!isFound) {
            if (Helper.isNumber(keyword)) {
                mt = sendMessage(sms);
                smsResponeMPS.setMoContent(content);
                smsResponeMPS.setMtContent(mt);
                return smsResponeMPS;
            } else {
                mt = wrongSyntax(sms);
                smsResponeMPS.setMoContent(content);
                smsResponeMPS.setMtContent(mt);
                return smsResponeMPS;
            }
        }
        return smsResponeMPS;
    }

    public String formatMessage(String sms) {
        if (sms != null) {
            while (sms.contains("  ")) {
                sms = sms.replaceAll("  ", " ");
            }

            sms = sms.trim();
        }

        return sms;
    }

    private String findMusic(SMS sms) {
        String content = sms.getMoContent().toLowerCase();
        String keyword = sms.getMoKeyword().toLowerCase();
        String text = content.replaceFirst(keyword, "").trim();
        String mt = "";
        if (text.length() == 0) {
            mt = wrongSyntax(sms);
        } else {
            ArrayList<Content> array = db.getMusicByName(text);
            int mtType = SMSType.FindContent.getValue();
            if (!array.isEmpty()) {
                String b = "";
                for (int i = 0; i < array.size(); i++) {
                    String c = ConfigStack.getConfig("mt", "search_music", "");
                    Content a = array.get(i);
                    c = Helper.prepaidContent(c, "", "", "", a.getContentNameSlug(), a.getCode(), "", "", "", a.getSingerSlug(), "", "", "", "");
                    b = b + c;
                }
                mt = ConfigStack.getConfig("mt", "mt_find_music_success", "");
                mt = Helper.prepaidContent(mt, "", "", "", b, "", "", "", "", "", "", "", "", "");
                sms.setMtContent(mt);
                sms.setType(mtType);
            } else {
                mt = ConfigStack.getConfig("mt", "mt_find_music_not_success", "");
                mt = Helper.prepaidContent(mt, "", "", "", text, "", "", "", "", "", "", "", "", "");
                sms.setMtContent(mt);
                sms.setType(mtType);
            }
        }
        sms.setAction("FIND_CONTENT");
        // restfulStack.sendMT(sms);
        return mt;
    }

    private String findStory(SMS sms) {
        String content = sms.getMoContent().toLowerCase();
        String keyword = sms.getMoKeyword().toLowerCase();
        String text = content.replaceFirst(keyword, "").trim();
        String mt = "";
        if (text.length() == 0) {
            mt = wrongSyntax(sms);
        } else {
            ArrayList<Content> array = db.getStoryByName(text);
            int mtType = SMSType.FindContent.getValue();
            if (!array.isEmpty()) {
                String b = "";
                for (int i = 0; i < array.size(); i++) {
                    String c = ConfigStack.getConfig("mt", "search_story", "");
                    Content a = array.get(i);
                    c = Helper.prepaidContent(c, "", "", "", a.getContentNameSlug(), a.getCode(), "", "", "", "", "", "", "", "");
                    b = b + c;
                }
                mt = ConfigStack.getConfig("mt", "mt_find_story_success", "");
                mt = Helper.prepaidContent(mt, "", "", "", b, "", "", "", "", "", "", "", "", "");
                sms.setMtContent(mt);
                sms.setType(mtType);
            } else {
                mt = ConfigStack.getConfig("mt", "mt_find_story_not_success", "");
                mt = Helper.prepaidContent(mt, "", "", "", text, "", "", "", "", "", "", "", "", "");
                sms.setMtContent(mt);
                sms.setType(mtType);
            }
        }
        sms.setAction("FIND_CONTENT");
        //restfulStack.sendMT(sms);
        return mt;
    }
    
    private String checkMinutes(SMS sms) {
        // Tra tin MT
        String typePackage = "";
        String content = sms.getMoContent().toLowerCase();
        String keyword = sms.getMoKeyword().toLowerCase();
        String text = content.replaceFirst(keyword, "").trim();
        String mt = "";
        if (text.length() > 0) {
            mt = wrongSyntax(sms);
        } else {
            SubPackageInfo subInfo = db.checkSubPackage(sms.getMsisdn());
            logger.info("checkMinutes : " + subInfo.getErrorCode().getValue());
            int mtType = SMSType.Genral.getValue();
            if (subInfo.getPackageId() == 1) {
                typePackage = "ngay";
            } else if (subInfo.getPackageId() == 2) {
                typePackage = "tuan";
            }
            if (subInfo.getErrorCode().getValue() == BillingErrorCode.Success.getValue()) {
                mt = ConfigStack.getConfig("mt", "mt_check_minutes_success", "");
                mt = Helper.prepaidContent(mt, subInfo.getExpireAt(), String.valueOf(subInfo.getFreeMinutes()), subInfo.getPackageName(), typePackage, "", "", "", "", "", String.valueOf(subInfo.getSubFee()), "", "", "");
            } else {
                mt = ConfigStack.getConfig("mt", "mt_kt_not_found", "");
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            }
            sms.setMtContent(mt);
            sms.setType(mtType);
            sms.setAction("CHECK");
            //restfulStack.sendMT(sms);
        }
        return mt;
    }
    
    private String guide(SMS sms) {
        // Tra tin MT
        String content = sms.getMoContent().toLowerCase();
        String keyword = sms.getMoKeyword().toLowerCase();
        String text = content.replaceFirst(keyword, "").trim();
        String mt = "";
        if (text.length() > 0) {
            mt = wrongSyntax(sms);
        } else {

            int mtType = SMSType.Genral.getValue();
            mt = ConfigStack.getConfig("mt", "mt_guide", "");
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setType(mtType);
            sms.setAction("GUIDE");
            // restfulStack.sendMT(sms);
        }
        return mt;
    }

    private void guideAMS(SMS sms) {
        // Tra tin MT
        String content = sms.getMoContent().toLowerCase();
        String keyword = sms.getMoKeyword().toLowerCase();
        String text = content.replaceFirst(keyword, "").trim();
        if (text.length() > 0) {
            wrongSyntax(sms);
        } else {
            String index = keyword.substring(keyword.length() - 1, keyword.length());
            String mt = "";
            int mtType = SMSType.Genral.getValue();
            mt = ConfigStack.getConfig("mt", "mt_guide_ams_" + index, "");
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setType(mtType);
            sms.setAction("GUIDE");
            restfulStack.sendMT(sms);
        }
    }

    private void registerSMS(SMS sms) {
        // Tra tin MT
        db.insertRegisterSMS(sms.getMsisdn());
        String mt = "";
        int mtType = SMSType.Genral.getValue();
        mt = ConfigStack.getConfig("mt", "mt_register_sms", "");
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setType(mtType);
        sms.setAction("DK SMS");
        restfulStack.sendMT(sms);
    }

    /**
     * Ham de call dk sang module vas_api
     *
     * @param sms
     */
    private void CallRegisModuleVasApi(SMS sms) {
        // goi ham dang ky module vas_api
        logger.info(">> CallModuleVasApi : " + " msisdn :" + sms.getMsisdn() + ", content :" + sms.getMoKeyword());
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String channel = "UDV";
        String resultCharging = chargingStack.registerForwardFormUngDichVu(msisdn, channel, content);
        logger.info(">> CallModuleVasApi : " + " resultCharging :" + resultCharging);

    }

    private void unRegisterSMS(SMS sms) {
        // Tra tin MT
        db.removeRegisterSMS(sms.getMsisdn());
        String mt = "";
        int mtType = SMSType.Genral.getValue();
        mt = ConfigStack.getConfig("mt", "mt_unregister_sms", "");
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setType(mtType);
        sms.setAction("DK SMS");
        restfulStack.sendMT(sms);
    }

    private void gift(SMS sms) {
        // Tra tin MT
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String mt = "";
        int mtType = SMSType.Genral.getValue();
        String content = sms.getMoContent().toLowerCase();
        String keyword = sms.getMoKeyword().toLowerCase();
        String text = content.replaceFirst(keyword, "").trim();
        if (text.length() == 0) {
            wrongSyntax(sms);
            return;
        } else {
            String msisdn = sms.getMsisdn();
            SubPackageInfo subInfo = db.checkSubPackage(msisdn);
            if (subInfo.getErrorCode().getValue() == BillingErrorCode.Success.getValue()) {
                String packageId = subInfo.getPackageId() + "";
                String subPackageId = subInfo.getSubPackageId() + "";
                String expireAt = subInfo.getExpireAt() + " 23:59:59";
                try {
                    expireAt = sdf1.format(sdf2.parse(expireAt));
                } catch (Exception ex) {
                    logger.error(ex);
                }
                String[] a = text.split(" ");
                String receiver = Helper.formatMobileNumber(a[a.length - 1]);
                String gift = text.replaceAll(a[a.length - 1], "").replaceAll(" ", "");
                String pattern = ConfigStack.getConfig("general", "giftMobilePattern", "");
                boolean checkMobi = false;
                boolean isVina = false;
                if (Helper.isCheckMobi(receiver, pattern)) {
                    logger.debug("sdt hop le : " + receiver);
                    if (Helper.isMobileNumber(receiver)) {
                        isVina = true;
                    }
                    checkMobi = true;
                }
                if (checkMobi) {
                    int topic_type = 1;
                    HashMap<String, Object> resp = db.getMusicByCodeOrName(gift);
                    String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                    if (errorCode.equals(Constants.SUCCESS)) {
                        String time = "";
                        SimpleDateFormat gio = new SimpleDateFormat("HH");
                        Calendar c = Calendar.getInstance();
                        if (Integer.parseInt(gio.format(c.getTime())) < 8 || Integer.parseInt(gio.format(c.getTime())) > 21) {
                            if (Integer.parseInt(gio.format(c.getTime())) > 21) {
                                c.add(Calendar.DAY_OF_MONTH, 1);
                            }
                            SimpleDateFormat sdf_ngay = new SimpleDateFormat("ddMMyyyy");
                            time = sdf_ngay.format(c.getTime()) + "-0805";
                        }
                        int delayMinuteToCall = Helper.getInt(ConfigStack.getConfig("gift", "delay_minute_to_call", "5"));
                        Calendar[] info = Helper.formatTimeGift(time, "ddMMyyyy-HHmm", delayMinuteToCall);
                        Calendar timeSendMT = info[0];
                        Calendar timeSendGift = info[1];
                        /*
                         * Check account of sender
                         */
                        boolean createNewIfNotExist = true;
                        int maxFreeCount = Helper.getInt(ConfigStack.getConfig("gift", "free_count", "5"));

                        GiftAccountInfo senderAcc = giftDao.getGiftAccount(msisdn, createNewIfNotExist, maxFreeCount, Integer.valueOf(subPackageId), expireAt);
                        if (senderAcc == null) {
                            mt = ConfigStack.getConfig("mt", "mt_system_error", "");
                            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                        }
                        try {
                            // check them co update so qua mien phi theo chu ki goi cuoc hay k
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                            Calendar cal1 = Calendar.getInstance();
                            Calendar cal2 = Calendar.getInstance();
                            cal1.setTime(sdf.parse(expireAt));
                            cal2.setTime(sdf.parse(senderAcc.getSubExpireAt()));
                            logger.info("SubpackageId :" + subPackageId + "---senderAcc.getSubPackageId : " + senderAcc.getSubPackageId());
                            if (Integer.valueOf(subPackageId) != senderAcc.getSubPackageId()) {
                                senderAcc = giftDao.updateFreeCount(msisdn, Integer.valueOf(subPackageId), expireAt, maxFreeCount);
                                logger.info("SubpackageId  Khac senderAcc.getSubPackageId : ");
                            } else {
                                if (cal1.after(cal2)) {
                                    // ngay het han trong gift_account nho hon ngay het han trong sub_package thi thuc hien update qua cho kh
                                    senderAcc = giftDao.updateFreeCount(msisdn, Integer.valueOf(subPackageId), expireAt, maxFreeCount);
                                    logger.info("SubpackageId  DEO KHAC  senderAcc.getSubPackageId : ");
                                }
                            }
                        } catch (Exception ex) {
                            logger.error("donateGift :" + ex);
                        }

                        int fee = 0;
                        boolean charge = false;
                        int countGift = senderAcc.getFreeCount();
                        if (senderAcc.getFreeCount() <= 0) {
                            charge = true;
                        }

                        if (isVina) {
                            // tang qua noi mang 
                            String contentName = String.valueOf(resp.get("content_name"));
                            String contentId = String.valueOf(resp.get("content_id"));
                            String code = String.valueOf(resp.get("code"));
                            // Charge tien qua tang
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("msisdn", msisdn);
                            params.put("receiver", receiver);
                            params.put("source", "SMS");
                            params.put("packageId", packageId);
                            params.put("subPackageId", subPackageId);
                            params.put("amount", ConfigStack.getConfig("gift", "fee_gift_music", "2000"));
                            Result resp_billing = new Result();
                            if (charge) {
                                resp_billing = billing.chargeGift(params);
                            } else {
                                resp_billing.setErrorCode(Constants.SUCCESS);
                            }
                            if (resp_billing.getErrorCode().equals(Constants.SUCCESS)) {
                                // Tao qua                               
                                GiftContentInfo g = new GiftContentInfo();
                                g.setSender(msisdn);
                                g.setReceiver(receiver);
                                g.setContentId(Integer.valueOf(contentId));
                                g.setContentCode(code);
                                g.setGiftContentName(contentName);
                                g.setSendMTDate(timeSendMT);
                                g.setCallDate(timeSendGift);
                                g.setMessagePath(null);
                                g.setSource("SMS");
                                g.setTopicType(topic_type);
                                if (resp_billing.getChargeFree() == 1) {
                                    g.setFee(0);
                                } else {
                                    g.setFee(Integer.parseInt(params.get("amount")));
                                }

                                g.setTelco(Constants.TELCO_VINA);
                                g.setToTelco(Constants.TELCO_VINA);
                                g.setAudioPath(null);
                                errorCode = giftDao.insertGiftContent(g);

                                if (errorCode.equals(Constants.SUCCESS)) {
                                    SimpleDateFormat sdf_sms_ngay = new SimpleDateFormat("dd/MM/yyyy");
                                    SimpleDateFormat sdf_sms_phut = new SimpleDateFormat("HH:mm");
                                    try {
                                        String phut = sdf_sms_phut.format(timeSendGift.getTime());
                                        String ngay = sdf_sms_ngay.format(timeSendGift.getTime());
                                        receiver = "0" + Helper.formatMobileNumberWithoutPrefix(receiver);
                                        if (resp_billing.getChargeFree() == 1) {
                                            mt = ConfigStack.getConfig("mt", "mt_send_gift_success_free", "");
                                            mt = Helper.prepaidContent(mt, ngay, phut, "", contentName, "", "", "", "" + resp_billing.getGiftFree(), "", "", "", "", receiver);
                                            mt = mt.replaceAll("\\{qua_free\\}", String.valueOf(countGift));
                                        } else {
                                            mt = ConfigStack.getConfig("mt", "mt_send_gift_success", "");
                                            mt = Helper.prepaidContent(mt, ngay, phut, "", contentName, "", "", "", "", "", "", "", "", receiver);
                                            mt = mt.replaceAll("\\{qua_free\\}", String.valueOf(countGift));
                                        }
                                    } catch (Exception e) {
                                        logger.error(e);
                                        mt = ConfigStack.getConfig("mt", "mt_system_error", "");
                                        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                                    }
                                } else {
                                    mt = ConfigStack.getConfig("mt", "mt_system_error", "");
                                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                                }
                            } else {
                                if (resp_billing.getErrorCode().equals(Constants.BLACK_LIST)) {
                                    mt = ConfigStack.getConfig("mt", "mt_send_gift_receiver_reject", "");
                                    receiver = "0" + Helper.formatMobileNumberWithoutPrefix(receiver);
                                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", receiver);
                                } else {
                                    mt = ConfigStack.getConfig("mt", "mt_send_gift_not_money", "");
                                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                                }
                            }
                        } else {
                            // tang qua lien mang
                            String checkCanSendGift = "";
                            logger.info("vao case xu ly so ngoai vina");
                            logger.info("du dieu kien paten sau do goi len kho");
                            try {
                                String url = ConfigStack.getConfig("ivr_store", "api_url", "");
                                String user = ConfigStack.getConfig("ivr_store", "api_username", "");
                                String pass = ConfigStack.getConfig("ivr_store", "api_password", "");
                                if (url.length() > 0) {
                                    url = url + "/canSendGiftContent?sender=" + msisdn + "&receiver=" + receiver + "&username=" + user + "&password=" + pass;
                                    logger.info("canSendGiftContent: " + url);
                                    String respApi = readUrl(url);
                                    logger.info("canSendGiftContent: " + respApi);
                                    JSONObject obj = new JSONObject(respApi);
                                    checkCanSendGift = obj.getString("errorCode");
                                    logger.info("checkCanSendGift: " + checkCanSendGift);
                                }
                            } catch (Exception e) {
                                logger.error(e);
                            }
                            if (checkCanSendGift.equals(Constants.SUCCESS)) {
                                logger.info("checkCanSendGift: " + checkCanSendGift);
                                // check xem qua song da tom tai chua neu chua co thi dowload ve

                                HashMap<String, Object> resp1 = db.getMusicByCodeOrName(gift);
                                errorCode = String.valueOf(resp1.get(Constants.ERROR_CODE));
                                if (errorCode.equals(Constants.SUCCESS)) {
                                    String contentName = String.valueOf(resp.get("content_name"));
                                    String contentId = String.valueOf(resp.get("content_id"));
                                    String code = String.valueOf(resp.get("code"));
                                    // Charge tien qua tang
                                    HashMap<String, String> params = new HashMap<String, String>();
                                    params.put("msisdn", msisdn);
                                    params.put("receiver", receiver);
                                    params.put("source", "SMS");
                                    params.put("packageId", packageId);
                                    params.put("subPackageId", subPackageId);
                                    params.put("amount", ConfigStack.getConfig("gift", "fee_gift_music", "2000"));
                                    Result resp_billing = new Result();
                                    if (charge) {
                                        resp_billing = billing.chargeGift(params);
                                    } else {
                                        resp_billing.setErrorCode(Constants.SUCCESS);
                                    }
                                    if (resp_billing.getErrorCode().equals(Constants.SUCCESS)) {
                                        // Tao qua
                                        GiftContentInfo g = new GiftContentInfo();
                                        g.setSender(msisdn);
                                        g.setReceiver(receiver);
                                        g.setContentId(Integer.valueOf(contentId));
                                        g.setContentCode(code);
                                        g.setGiftContentName(contentName);
                                        g.setSendMTDate(timeSendMT);
                                        g.setCallDate(timeSendGift);
                                        g.setMessagePath(null);
                                        g.setSource("SMS");
                                        g.setTopicType(topic_type);
                                        if (resp_billing.getChargeFree() == 1) {
                                            g.setFee(0);
                                        } else {
                                            g.setFee(Integer.parseInt(params.get("amount")));
                                        }

                                        g.setTelco(Constants.TELCO_VINA);
                                        int toTelCo = 2;
                                        String viettelPattern = ConfigStack.getConfig("general", "viettelPattern", "");
                                        String mobifonePattern = ConfigStack.getConfig("general", "mobifonePattern", "");
                                        if (Helper.isCheckMobi(receiver, viettelPattern)) {
                                            toTelCo = Constants.Telco.VIETTEL.getValue();
                                        } else if (Helper.isCheckMobi(receiver, mobifonePattern)) {
                                            toTelCo = Constants.Telco.MOBIFONE.getValue();
                                        }
                                        g.setToTelco(toTelCo);
                                        g.setAudioPath(null);
                                        errorCode = giftDao.insertGiftContent(g);
                                        logger.info("errocode sau khiinsert gift vao db: " + errorCode);
                                        if (errorCode.equals(Constants.SUCCESS)) {
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                            SimpleDateFormat sdf_sms_ngay = new SimpleDateFormat("dd/MM/yyyy");
                                            SimpleDateFormat sdf_sms_phut = new SimpleDateFormat("HH:mm");
                                            try {
                                                String phut = sdf_sms_phut.format(timeSendGift.getTime());
                                                String ngay = sdf_sms_ngay.format(timeSendGift.getTime());
                                                receiver = "0" + Helper.formatMobileNumberWithoutPrefix(receiver);
                                                if (resp_billing.getChargeFree() == 1) {
                                                    mt = ConfigStack.getConfig("mt", "mt_send_gift_success_free", "");
                                                    mt = Helper.prepaidContent(mt, ngay, phut, "", contentName, "", "", "", "" + resp_billing.getGiftFree(), "", "", "", "", receiver);
                                                    mt = mt.replaceAll("\\{qua_free\\}", String.valueOf(countGift));
                                                } else {
                                                    mt = ConfigStack.getConfig("mt", "mt_send_gift_success", "");
                                                    mt = Helper.prepaidContent(mt, ngay, phut, "", contentName, "", "", "", "", "", "", "", "", receiver);
                                                    mt = mt.replaceAll("\\{qua_free\\}", String.valueOf(countGift));
                                                }
                                            } catch (Exception e) {
                                                logger.error(e);
                                                mt = ConfigStack.getConfig("mt", "mt_system_error", "");
                                                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                                            }
                                        } else {
                                            mt = ConfigStack.getConfig("mt", "mt_system_error", "");
                                            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                                        }
                                    } else {
                                        if (resp_billing.getErrorCode().equals(Constants.BLACK_LIST)) {
                                            mt = ConfigStack.getConfig("mt", "mt_send_gift_receiver_reject", "");
                                            receiver = "0" + Helper.formatMobileNumberWithoutPrefix(receiver);
                                            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", receiver);
                                        } else {
                                            mt = ConfigStack.getConfig("mt", "mt_send_gift_not_money", "");
                                            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                                        }
                                    }
                                } else {
                                    mt = ConfigStack.getConfig("mt", "mt_send_gift_wrong", "");
                                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                                }

                            } else {
                                if (checkCanSendGift.equals(Constants.ERROR_REJECT)) {
                                    // tu choi ngoai mang
                                    mt = ConfigStack.getConfig("mt", "mt_send_gift_receiver_reject", "");
                                    receiver = "0" + Helper.formatMobileNumberWithoutPrefix(receiver);
                                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", receiver);
                                } else {
                                    mt = ConfigStack.getConfig("mt", "mt_send_gift_wrong", "");
                                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                                }
                            }
                        }
                    } else {
                        mt = ConfigStack.getConfig("mt", "mt_send_gift_wrong", "");
                        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                    }
                } else {
                    mt = ConfigStack.getConfig("mt", "mt_send_gift_wrong", "");
                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                }
            } else {
                mt = ConfigStack.getConfig("mt", "mt_unregister_not_existed", "");
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            }
        }
        sms.setMtContent(mt);
        sms.setType(mtType);
        sms.setAction("GIFT");
        restfulStack.sendMT(sms);
    }

    private void registerGift(SMS sms) {
        // Tra tin MT
        String mt = "";
        int mtType = SMSType.Genral.getValue();
        String content = sms.getMoContent().toLowerCase();
        String keyword = sms.getMoKeyword().toLowerCase();
        String text = content.replaceFirst(keyword, "").trim();
        if (text.length() == 0) {
            db.insertRegisterGift(sms.getMsisdn(), "");
            mt = ConfigStack.getConfig("mt", "mt_register_gift", "");
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        } else {
            String sender = Helper.formatMobileNumber(text);
            String checkPattern = ConfigStack.getConfig("general", "giftMobilePattern", "");
            if (Helper.isCheckMobi(sender, checkPattern)) {
                db.insertRegisterGift(sms.getMsisdn(), sender);
                mt = ConfigStack.getConfig("mt", "mt_register_gift_with_sender", "");
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", sender, "");
            } else {
                mt = ConfigStack.getConfig("mt", "mt_register_gift_with_sender_wrong", "");
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            }
        }
        sms.setMtContent(mt);
        sms.setType(mtType);
        sms.setAction("GIFT");
        restfulStack.sendMT(sms);
    }

    private void unRegisterGift(SMS sms) {
        // Tra tin MT
        String mt = "";
        int mtType = SMSType.Genral.getValue();
        String content = sms.getMoContent().toLowerCase();
        String keyword = sms.getMoKeyword().toLowerCase();
        String text = content.replaceFirst(keyword, "").trim();
        if (text.length() == 0) {
            int r = db.removeRegisterGift(sms.getMsisdn(), "");
            if (r == 0) {
                mt = ConfigStack.getConfig("mt", "mt_unregister_gift", "");
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            } else {
                mt = ConfigStack.getConfig("mt", "mt_unregister_gift_unsuccess", "");
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            }
        } else {
            String sender = Helper.formatMobileNumber(text);
            String checkPattern = ConfigStack.getConfig("general", "giftMobilePattern", "");
            if (Helper.isCheckMobi(sender, checkPattern)) {
                int r = db.removeRegisterGift(sms.getMsisdn(), sender);
                if (r == 0) {
                    mt = ConfigStack.getConfig("mt", "mt_unregister_gift_with_sender", "");
                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", sender, "");
                } else if (r == 1) {
                    mt = ConfigStack.getConfig("mt", "mt_unregister_gift_unsuccess", "");
                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                } else if (r == 2) {
                    mt = ConfigStack.getConfig("mt", "mt_unregister_gift_with_sender_unsuccess", "");
                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", sender, "");
                }
            } else {
                mt = ConfigStack.getConfig("mt", "mt_unregister_gift_with_sender_wrong", "");
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            }
        }
        sms.setMtContent(mt);
        sms.setType(mtType);
        sms.setAction("GIFT");
        restfulStack.sendMT(sms);
    }

    private String kt(SMS sms) {
        // Tra tin MT
        String typePackage = "";
        String content = sms.getMoContent().toLowerCase();
        String keyword = sms.getMoKeyword().toLowerCase();
        String text = content.replaceFirst(keyword, "").trim();
        String mt = "";
        if (text.length() > 0) {
            mt = wrongSyntax(sms);
        } else {
            SubPackageInfo subInfo = db.checkSubPackage(sms.getMsisdn());
            logger.info("KT : " + subInfo.getErrorCode().getValue());
            int mtType = SMSType.Genral.getValue();
            if (subInfo.getPackageId() == 1) {
                typePackage = "ngay";
            } else if (subInfo.getPackageId() == 2) {
                typePackage = "tuan";
            }
            if (subInfo.getFreeMinutes() == 0 && subInfo.getErrorCode().getValue() == BillingErrorCode.Success.getValue()) {
                mt = ConfigStack.getConfig("mt", "mt_kt_expired_free_minures", "");
                mt = Helper.prepaidContent(mt, "", "", subInfo.getPackageName().toLowerCase(), typePackage, "", "", "", "", "", String.valueOf(subInfo.getSubFee()), "", "", "");
            } else if (subInfo.getErrorCode().getValue() == BillingErrorCode.Success.getValue()) {
                mt = ConfigStack.getConfig("mt", "mt_kt_success", "");
                mt = Helper.prepaidContent(mt, subInfo.getExpireAt(), String.valueOf(subInfo.getFreeMinutes()), subInfo.getPackageName().toLowerCase(), typePackage, "", "", "", "", "", String.valueOf(subInfo.getSubFee()), "", "", "");
                if(subInfo.getPackageId() == 1){
                    subInfo.setSubFee(2000);
                }else if (subInfo.getPackageId() == 2) {
                    subInfo.setSubFee(7000);
                }else if (subInfo.getPackageId() == 3) {
                    subInfo.setSubFee(15000);
                }
            } else {
                mt = ConfigStack.getConfig("mt", "mt_kt_not_found", "");
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            }
            sms.setMtContent(mt);
            sms.setType(mtType);
            sms.setAction("CHECK");
            //restfulStack.sendMT(sms);
        }
        return mt;
    }

    private void mk(SMS sms) {
        // Tra tin MT
        String content = sms.getMoContent().toLowerCase();
        String keyword = sms.getMoKeyword().toLowerCase();
        String text = content.replaceFirst(keyword, "").trim();
        if (text.length() > 0) {
            wrongSyntax(sms);
        } else {
            //SubPackageInfo subInfo = db.checkSubPackage(sms.getMsisdn());
            User currentUser = db.checkExistUser(sms.getMsisdn());
            String mt = "";
            int mtType = SMSType.Genral.getValue();
            if (currentUser.getCode() == BillingErrorCode.Success.getValue()) {
                // Tao mat khau
                String salt = MD5_Hash.generateSalt(Integer.parseInt(ConfigStack.getConfig("api_user_profile", "length_key", "6")));
                String password = RandomStringUtils.randomNumeric(Integer.parseInt(ConfigStack.getConfig("api_user_profile", "length_pass", "8")));
                String md5_pass = MD5_Hash.md5(MD5_Hash.md5(password) + salt);
                User user = db.resetPassword(sms.getMsisdn(), md5_pass, salt);
                if (user.getCode() == BillingErrorCode.Success.getValue()) {
                    mt = ConfigStack.getConfig("mt", "mt_mk_success", "");
                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", password, "", "");
                }
            } else {
                mt = ConfigStack.getConfig("mt", "mt_mk_not_found", "");
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            }
            sms.setMtContent(mt);
            sms.setType(mtType);
            sms.setAction("RESET PASSWORD");
            restfulStack.sendMT(sms);
        }
    }

    private String wrongSyntax(SMS sms) {
        // Tra tin MT
        String mt = "";
        int mtType = SMSType.Genral.getValue();
        mt = ConfigStack.getConfig("mt", "mt_wrong_syntax", "");
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setAction("WRONG_SYNTAX");
        sms.setType(mtType);
        //restfulStack.sendMT(sms);
        return mt;
    }

    //friend
    private void guideMakeFriend(SMS sms) {
        if (logger.isDebugEnabled()) {
            logger.debug("guideMakeFriend called: " + sms.getMsisdn());
        }

        // String mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_EMPTY", "")("FRIEND_GUIDE");
        // this.sendSMS(sms, mt);
        String mt = "";
        mt = ConfigStack.getConfig("mt", "FRIEND_GUIDE", "");
        logger.info("AAAAAAAAAAAAA: mt => " + mt);
        int mtType = SMSType.Genral.getValue();
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setAction("SEND_FRIENDLY");
        sms.setType(mtType);
        restfulStack.sendMT(sms);
    }

    private void declareProfile(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("declare Profile called: " + msisdn + "; sms: " + content + " KeyWord: " + keyWord);
        }

        /*
         * Kiem tra goi cuoc
         */
        if (!checkRegister(sms)) {
            return;
        }

        String contentPart = content.substring(keyWord.length()).trim();
        if (logger.isDebugEnabled()) {
            logger.debug("contentPart: " + contentPart + " ContentPart.length(): " + contentPart.length());
        }
        if (contentPart.length() == 0) {
            wrongSyntax(sms);
            return;
        }

        String mt = "";
        String[] parts = contentPart.split(ConfigStack.getConfig("mo", "separator", "_"));
        String name = "";
        int year = 0;
        String provinceName = "";
        int provinceId = 0;
        int sex = 0;
        int i = -1;
        while (++i < parts.length) {
            if (Helper.isNumber(parts[i]) && year == 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                sdf.setLenient(false);
                try {
                    sdf.parse(parts[i]);
                    year = Integer.parseInt(parts[i]);
                    if (year > 1000 && year < 2016) {
                        logger.debug("year: " + year);
                    } else {
                        year = 0;
                        logger.debug("year: = 0 vi dieu kien nhap nam chua dung ");
                    }
                } catch (Exception e) {
                    continue;
                }
            } else {
                if (year > 0) {
                    provinceName = provinceName + ConfigStack.getConfig("mo", "separator", "_") + parts[i];
                    logger.info("provinceName: " + provinceName);
                    while (provinceName.indexOf(" ") != -1) {
                        provinceName = provinceName.replaceAll(" ", "");
                    }
                } else {
                    name = name + ConfigStack.getConfig("mo", "separator", "_") + parts[i];
                }
            }
        }

        String[] namePart = name.split(ConfigStack.getConfig("mo", "separator", "_"));
        String lastPart = parts[1];
        if (lastPart.length() > 1) {
            logger.info("lastPart: " + lastPart);
            if ("NU".equalsIgnoreCase(lastPart)) {
                sex = 1;
                name = name.substring(0, name.lastIndexOf(ConfigStack.getConfig("mo", "separator", "_")));
            } else if ("NAM".equalsIgnoreCase(lastPart)) {
                sex = 2;
                name = name.substring(0, name.lastIndexOf(ConfigStack.getConfig("mo", "separator", "_")));
            }
            logger.info("name: " + name);
        }

        /*
         * Kiem tra thong tin nhap
         */
        String invalidInput = "";
        if (name.length() == 0) {
            invalidInput = "<Ten>";
        }
        if (sex == 0) {
            invalidInput += invalidInput.length() > 0 ? "/<Gioi tinh>" : "<Gioi tinh>";
        }
        if (year == 0) {
            invalidInput += invalidInput.length() > 0 ? "/<Nam sinh>" : "<Nam sinh>";
        }
        ProvinceInfo p = null;
        if (provinceName.length() > 0) {
            p = getProvinceFromSign(provinceName, db.getListProvince());
            if (p == null) {
                invalidInput += invalidInput.length() > 0 ? "/<Tinh thanh>" : "<Tinh thanh>";
            } else {
                provinceId = p.getProvinceId();
            }
            logger.info("invalidInput: " + invalidInput);
        } else {
            invalidInput += invalidInput.length() > 0 ? "/<Tinh thanh>" : "<Tinh thanh>";
        }
        logger.info("AAAAAAAAAAAAAAAAAAAAAAAA");
        if (invalidInput.length() > 0) {
            mt = ConfigStack.getConfig("mt", "FRIEND_DECLARE_INVALID", "");
            logger.info("mt FRIEND_DECLARE_INVALID: " + mt);
            if (mt != null) {
                mt = mt.replaceAll("\\{gia_tri_sai\\}", invalidInput);
                int mtType = SMSType.Genral.getValue();
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                sms.setMtContent(mt);
                sms.setAction("SEND_FRIENDLY");
                sms.setType(mtType);
                restfulStack.sendMT(sms);
            }
            return;
        }

        /*
         * Kiem tra thong tin hien tai va tinh diem cong
         */
        logger.info("AAAAAAAAAAAAAAAAAAAAAAAA");
        int totalBonusPoint = 0;
        int point = Helper.getInt(ConfigStack.getConfig("game", "pointDeclareProfile", ""), 100);
        SubProfileInfo profile = db.getSubProfile(msisdn, 0, -1);
        if (profile == null) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            int mtType = SMSType.Genral.getValue();
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setAction("SEND_FRIENDLY");
            sms.setType(mtType);
            restfulStack.sendMT(sms);
            return;
        } else if (profile.getUserId() == 0) {
            // profile = db.generateNewUserId(msisdn);

            // goi sang API cua kho tap trung
//            String url = ConfigStack.getConfig("auth_api", "url", "");
//            String user = ConfigStack.getConfig("auth_api", "username", "");
//            String pass = ConfigStack.getConfig("auth_api", "password", "");
//            String telcoId = ConfigStack.getConfig("ivr_store", "telco_id", "2");
//
//            if (url.length() > 0) {
//                url = url + "/getSubProfile?msisdn=" + msisdn + "&telco="+telcoId+"&source=SMS&username=" + user + "&password=" + pass;
//                logger.info("request to global Storage url: " + url);
//                try {
//                    // khoi tao client
//                    String resp = readUrl(url);
//                    logger.info("return JSON: " + resp);
//                    JSONObject obj = new JSONObject(resp);
//                    String errorCode = obj.getString("errorCode");
//                    JSONObject subProfile = obj.getJSONObject("subProfile");
//                    String userIdStr = subProfile.getString("userId");
//                    String telco = subProfile.getString("telco");
//                    String updatedDate = subProfile.getString("updatedDate");
//                    logger.info("BBBBBBBBBBB@@@@@: " + errorCode + userIdStr + telco);
//                    profile.setUpdatedDate(updatedDate);
//                    profile.setErrorCode(Integer.parseInt(errorCode));
//                    profile.setUserId(Integer.parseInt(userIdStr));
//                    profile.setTelco(Integer.parseInt(telco));
//                } catch (Exception e) {
//                    logger.error("Error while request to global Storage: " + url + "; Exception: " + e);
//                }
//                /*
//                 * insert user vao db local
//                 */
//                if (profile.getErrorCode() == 0) {
//                    // ket noi dc kho tap trung va tra lai ket qua dung
//                    profile = db.generateNewUserId(msisdn, profile.getUserId(), profile.getUpdatedDate(), profile.getCreatedDate());
//
//                }
//            }
            profile = db.generateNewUserId(msisdn, -1, "", "");
            if (profile == null) {
                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                int mtType = SMSType.Genral.getValue();
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                sms.setMtContent(mt);
                sms.setAction("SEND_FRIENDLY");
                sms.setType(mtType);
                restfulStack.sendMT(sms);
                return;
            }
        }
        logger.info("AAAAAAAAAAAAAAAAAAAAAAAA");
        if (Helper.isEmpty(profile.getName())) {
            totalBonusPoint += point;
        }
        profile.setName(name);
        profile.setSex(sex);
        profile.setBirthYear(year);
        profile.setProvinceId(provinceId);
        profile.setSource("SMS");
        profile.setMsisdn(msisdn);
        logger.info("AAAAAAAAAAAAAAAAAAAAAAAA");
        boolean notifyBadStatus = false;
        if (profile.getStatus() != Constants.PROFILE_STATUS_BAD
                && Helper.isEmpty(profile.getIntroPath())) {
            profile.setStatus(Constants.PROFILE_STATUS_BAD);
            notifyBadStatus = true;
        }
        if (db.updateSubProfile(profile)) {

            mt = ConfigStack.getConfig("mt", "FRIEND_DECLARE_SUCCESS", "");
            logger.info("mt: " + mt);
            if (mt != null) {
                mt = mt.replaceAll("\\{diem\\}", String.valueOf(totalBonusPoint));
                mt = mt.replaceAll("\\{ho_ten\\}", ConvertUnsignedString.getUnsignedString(name).toUpperCase());
                mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(profile.getUserId()));
                mt = mt.replaceAll("\\{gioi_tinh\\}", getSexName(sex));
                mt = mt.replaceAll("\\{nam_sinh\\}", String.valueOf(profile.getBirthYear()));
                mt = mt.replaceAll("\\{tinh_thanh\\}", ConvertUnsignedString.getUnsignedString(p.getName()).toUpperCase());
                int mtType = SMSType.Genral.getValue();
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                sms.setMtContent(mt);
                sms.setAction("SEND_FRIENDLY");
                sms.setType(mtType);
                restfulStack.sendMT(sms);
                if (notifyBadStatus) {
                    mt = ConfigStack.getConfig("mt", "APPROVE_PROFILE_WEAK", "");
                    logger.info("mt: " + mt);
                    if (mt != null) {
                        int smsChatDelay = Helper.getInt(ConfigStack.getConfig("friend", "SMS_CHAT_DELAY_IN_SECOND", "3"), 3) * 1000;
                        if (smsChatDelay > 0) {
                            try {
                                Thread.sleep(smsChatDelay);
                            } catch (Exception e) {
                            }
                        }
                        sms.setMtContent(mt);
                        restfulStack.sendMT(sms);
                    }
                }
            }
            logger.info("AAAAAAAAAAAAAAAAAAAAAAAA");
        } else {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            int mtType = SMSType.Genral.getValue();
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setAction("SEND_FRIENDLY");
            sms.setType(mtType);
            restfulStack.sendMT(sms);
        }
    }

    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1) {
                buffer.append(chars, 0, read);
            }

            return buffer.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void processChat(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("processChat called: " + msisdn);
        }

        String mt = "";
        /*
         * Kiem tra goi cuoc
         */
        if (!checkRegister(sms)) {
            return;
        }

        ArrayList<SubProfileInfo> profiles1 = db.getListSubProfileSMS(msisdn, 0, 0, 0, 100);
        ArrayList<SubProfileInfo> profiles = new ArrayList<SubProfileInfo>();
        if (profiles1 == null) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");

            sendSMS(sms, mt);
            return;
        }

        /*
         * Kiem tra thong tin
         */
        SubProfileInfo profile = checkProfile(sms);
        if (profile == null) {
            return;
        }
        String key = "listMsisdnSendChat_" + String.valueOf(msisdn);
        String listMsisdnSendChat = getFromCache(key);

        for (int i = 0; i < profiles1.size(); i++) {
            String profileItemID = SubProfileInfo.SEPERATOR + String.valueOf(profiles1.get(i).getUserId()) + SubProfileInfo.SEPERATOR;
            logger.info("profileItemID: " + profileItemID);
            logger.info("listMsisdnSendChat: " + listMsisdnSendChat);
            if (listMsisdnSendChat == null || listMsisdnSendChat == "") {
                profiles.add(profiles1.get(i));
                listMsisdnSendChat += profileItemID;
                int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
                pushToCacheWithExpiredTime(key, listMsisdnSendChat, timeOut);
                break;
            }
            if (listMsisdnSendChat.toLowerCase().contains(profileItemID.toLowerCase()) == false) {
                listMsisdnSendChat += profileItemID;
                int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
                pushToCacheWithExpiredTime(key, listMsisdnSendChat, timeOut);
                logger.info("listMsisdnSendChat: " + listMsisdnSendChat);
                profiles.add(profiles1.get(i));
                break;
            }
        }
        if (profiles.size() == 0) {
            profiles.add(profiles1.get(0));
        }
        logger.info("profileItemID: " + listMsisdnSendChat);
        mt = ConfigStack.getConfig("mt", "FRIEND_CHAT_OK", "");
        String name = "";
        if (!Helper.isEmpty(profiles.get(0).getName())) {
            name = ConvertUnsignedString.getUnsignedString(profiles.get(0).getName()).toUpperCase() + ";";
        }
        mt = mt.replaceAll("\\{ho_ten\\}", name);
        mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(profiles.get(0).getUserId()));
        mt = mt.replaceAll("\\{gioi_tinh\\}", getSexName(profiles.get(0).getSex()) + "; ");
        String year = "";
        if (profiles.get(0).getBirthYear() > 0) {
            year = String.valueOf(profiles.get(0).getBirthYear());
        }
        mt = mt.replaceAll("\\{nam_sinh\\}", year + "; ");
        String provinceName = "";
        if (!Helper.isEmpty(profiles.get(0).getProvinceName())) {
            provinceName = ConvertUnsignedString.getUnsignedString(profiles.get(0).getProvinceName());
            logger.info("provinceName: " + provinceName);
            while (provinceName.indexOf(" ") != -1) {
                provinceName = provinceName.replaceAll(" ", "");
            }
        }
        mt = mt.replaceAll("\\{tinh_thanh\\}", provinceName.toUpperCase());
        logger.info("mt: " + mt);
        sendSMS(sms, mt);
    }

    private void processFriendList(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("processFriendList called: " + msisdn);
        }

        String mt = "";
        /*
         * Kiem tra thong tin ca nhan
         */
        SubProfileInfo profile = checkProfile(sms);
        if (profile == null) {
            return;
        }

        int limit = Integer.parseInt(ConfigStack.getConfig("api_sms", "limitFriendList", "8"));
        ArrayList<SubProfileInfo> friends = db.getFriendList(profile.getUserId(), limit);
        if (friends != null) {
            if (friends.size() > 0) {
                mt = ConfigStack.getConfig("mt", "FRIEND_LIST_OK", "");
                String list = "";
                String templateItem = ConfigStack.getConfig("mt", "FRIEND_LIST_TEMPLATE_ITEM", "");

                for (int i = 0; i < friends.size(); i++) {
                    String item = templateItem.replaceAll("\\{ho_ten\\}", ConvertUnsignedString.getUnsignedString(friends.get(i).getName()).toUpperCase());
                    item = item.replaceAll("\\{ma_so\\}", String.valueOf(friends.get(i).getUserId()));
                    item = item.replaceAll("\\{nam_sinh\\}", friends.get(i).getBirthYear() > 0 ? ", " + String.valueOf(friends.get(i).getBirthYear()) : "");
                    item = item.replaceAll("\\{gioi_tinh\\}", friends.get(i).getSex() > 0 ? ", " + getSexName(friends.get(i).getSex()) : "");
                    item = item.replaceAll("\\{tinh_thanh\\}", friends.get(i).getProvinceId() > 0 ? ", " + getProvinceName(friends.get(i).getProvinceId(), db.getListProvince()).toUpperCase() : "");

                    list += list.length() > 0 ? "; " + item : item;
                }

                mt = mt.replaceAll("\\{danh_sach\\}", list);
            } else {
                /*
                 * Khong co ID trong friendlist
                 */
                mt = ConfigStack.getConfig("mt", "FRIEND_LIST_EMPTY", "");
            }
        } else {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");

        }

        sendSMS(sms, mt);
    }

    private void processSearchFriend(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("processSearchFriend called: " + msisdn + "; sms: " + content);
        }

        String mt = "";
        int birthYear = 0;
        int provinceId = 0;
        String provinceName = "";
        int sex = "NU".equalsIgnoreCase(keyWord) ? 1 : 2;
        /*
         * Lay thong tin dau vao
         */
        String contentPart = content.substring(keyWord.length()).trim();
        if (contentPart.length() > 0) {
            String[] parts = contentPart.split(ConfigStack.getConfig("mo", "separator", "_"));
            for (int i = 0; i < parts.length; i++) {
                if (Helper.isNumber(parts[i])) {
                    birthYear = Integer.parseInt(parts[i]);
                } else {
                    provinceName += provinceName.length() > 0 ? ConfigStack.getConfig("mo", "separator", "_") + parts[i] : parts[i];
                    logger.info("provinceName: " + provinceName);
                    while (provinceName.indexOf(" ") != -1) {
                        provinceName = provinceName.replaceAll(" ", "");
                    }
                }
            }

            if (provinceName.length() > 0) {
                ProvinceInfo p = getProvinceFromSign(provinceName, db.getListProvince());
                if (p != null) {
                    provinceId = p.getProvinceId();
                }
            }
        }

        int limit = Helper.getInt(ConfigStack.getConfig("api_sms", "limitSearchFriend", "8"), 8);
        ArrayList<SubProfileInfo> friends = db.getListSubProfile(msisdn, sex, birthYear, provinceId, limit);
        if (friends != null) {
            if (friends.size() > 0) {
                mt = ConfigStack.getConfig("mt", "FRIEND_SEARCH_OK", "");
                String list = "";
                String templateItem = ConfigStack.getConfig("mt", "FRIEND_LIST_TEMPLATE_ITEM", "");

                for (int i = 0; i < friends.size(); i++) {
                    String item = templateItem.replaceAll("\\{ho_ten\\}", ConvertUnsignedString.getUnsignedString(friends.get(i).getName()).toUpperCase());
                    item = item.replaceAll("\\{ma_so\\}", String.valueOf(friends.get(i).getUserId()));
                    item = item.replaceAll("\\{nam_sinh\\}", "");
                    item = item.replaceAll("\\{gioi_tinh\\}", "");
                    item = item.replaceAll("\\{tinh_thanh\\}", !Helper.isEmpty(friends.get(i).getProvinceName()) ? ", " + ConvertUnsignedString.getUnsignedString(friends.get(i).getProvinceName()).toUpperCase() : "");

                    list += list.length() > 0 ? "; " + item : item;
                }

                mt = mt.replaceAll("\\{danh_sach\\}", list);
            } else {
                /*
                 * Khong co ID trong ket qua
                 */
                mt = ConfigStack.getConfig("mt", "FRIEND_SEARCH_EMPTY", "");
            }
        } else {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
        }

        sendSMS(sms, mt);
    }

    private void addToFriendlist(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("addToFriendlist called: " + msisdn + "; sms: " + content);
        }

        String contentPart = content.substring(keyWord.length()).trim();
        if (contentPart.length() == 0 || !Helper.isNumber(contentPart)) {
            wrongSyntax(sms);
            return;
        }

        String mt = "";
        int friendUserId = Helper.getInt(contentPart, 0);

        /*
         * Kiem tra goi cuoc
         */
        if (!checkRegister(sms)) {
            return;
        }

        /*
         * Kiem tra thong tin ca nhan
         */
        SubProfileInfo profile = checkProfile(sms);
        if (profile == null) {
            return;
        }

        SubProfileInfo friendProfile = null;
        if (friendUserId > 0) {
            friendProfile = db.getSubProfile("", friendUserId, -1);
            if (friendProfile == null) {
                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                sendSMS(sms, mt);
                return;
            }
        }
        if (friendUserId <= 0 || friendProfile.getUserId() == 0) {
            mt = ConfigStack.getConfig("mt", "FRIEND_ID_INVALID_OR_EXIST", "");
            sendSMS(sms, mt);
            return;
        }

        /*
         * Them vao friendlist
         */
        String result = db.addToFriendList(profile.getUserId(), friendUserId);
        if (result.equalsIgnoreCase(Constants.SUCCESS)) {
            mt = ConfigStack.getConfig("mt", "FRIEND_LIST_ADD_SUCCESS", "");
            mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(friendUserId));
        } else if (result.equalsIgnoreCase(Constants.DATA_EXIST)) {
            mt = ConfigStack.getConfig("mt", "FRIEND_ID_INVALID_OR_EXIST", "");
        } else {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
        }
        sendSMS(sms, mt);
    }

    private void removeFromFriendlist(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("removeFromFriendlist called: " + msisdn + "; sms: " + content);
        }

        String contentPart = content.substring(keyWord.length()).trim();
        if (contentPart.length() == 0 || !Helper.isNumber(contentPart)) {
            wrongSyntax(sms);
            return;
        }

        String mt = "";
        int friendUserId = Helper.getInt(contentPart, 0);

        /*
         * Kiem tra goi cuoc
         */
        if (!checkRegister(sms)) {
            return;
        }

        /*
         * Kiem tra thong tin ca nhan
         */
        SubProfileInfo profile = checkProfile(sms);
        if (profile == null) {
            return;
        }

        SubProfileInfo friendProfile = null;
        if (friendUserId > 0) {
            friendProfile = db.getSubProfile("", friendUserId);
            if (friendProfile == null) {
                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                sendSMS(sms, mt);
                return;
            }
        }
        boolean checkRemovedFromFrienelist = db.checkRemovedFromFrienelist(profile.getUserId(), friendUserId);
        logger.info("checkRemovedFromFrienelist: " + checkRemovedFromFrienelist);
        if (checkRemovedFromFrienelist) {
            mt = ConfigStack.getConfig("mt", "FRIEND_ID_INVALID_OR_NOT_EXIST", "");
            sendSMS(sms, mt);
            return;
        }
        if (friendUserId <= 0 || friendProfile.getUserId() == 0) {
            mt = ConfigStack.getConfig("mt", "FRIEND_ID_INVALID_OR_NOT_EXIST", "");
            sendSMS(sms, mt);
            return;
        }

        /*
         * Xoa khoi friendlist
         */
        String result = db.removeFromFriendList(profile.getUserId(), friendUserId);
        if (result.equalsIgnoreCase(Constants.SUCCESS)) {
            mt = ConfigStack.getConfig("mt", "FRIEND_LIST_REMOVE_SUCCESS", "");
            mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(friendUserId));
        } else if (result.equalsIgnoreCase(Constants.NO_DATA_FOUND)) {
            mt = ConfigStack.getConfig("mt", "FRIEND_ID_INVALID_OR_NOT_EXIST", "");
        } else {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
        }
        sendSMS(sms, mt);
    }

    private void viewProfile(SMS sms) {
        logger.debug("viewProfile >>>>>>>>>>: ");
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("viewProfile called: " + msisdn);
        }

        /*
         * Kiem tra goi cuoc
         */
        if (!checkRegister(sms)) {
            return;
        }

        String mt = "";
        /*
         * Kiem tra ho so
         */
        SubProfileInfo profile = db.getSubProfile(msisdn, 0);
        if (profile == null) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            sendSMS(sms, mt);
            return;
        }
        if (profile.getUserId() == 0) {
            mt = ConfigStack.getConfig("mt", "FRIEND_CHECK_PROFILE_NOT_ID", "");
            sendSMS(sms, mt);
            return;
        }

        mt = ConfigStack.getConfig("mt", "FRIEND_CHECK_PROFILE_OK", "");
        mt = mt.replaceAll("\\{ho_ten\\}", Helper.isEmpty(profile.getName()) ? "" : "Ho ten: " + ConvertUnsignedString.getUnsignedString(profile.getName()) + ";");
        mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(profile.getUserId()));
        mt = mt.replaceAll("\\{gioi_tinh\\}", profile.getSex() > 0 ? "; " + getSexName(profile.getSex()) : "");
        mt = mt.replaceAll("\\{nam_sinh\\}", profile.getBirthYear() > 0 ? "; " + String.valueOf(profile.getBirthYear()) : "");
        mt = mt.replaceAll("\\{tinh_thanh\\}", profile.getProvinceId() > 0 ? "; " + getProvinceName(profile.getProvinceId(), db.getListProvince()).toUpperCase() : "");
        sendSMS(sms, mt);
    }

    private void addToBlacklistOfUser(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("addBlacklistOfUser called: " + msisdn + "; sms: " + content);
        }

        String contentPart = content.substring(keyWord.length()).trim();
        if (contentPart.length() == 0 || !Helper.isNumber(contentPart)) {
            wrongSyntax(sms);
            return;
        }

        int friendUserId = Helper.getInt(contentPart, 0);
        SubProfileInfo profile = checkProfile(sms);
        if (profile == null) {
            return;
        }

        String mt = "";
        // String key = "blacklist_sms_" + String.valueOf(profile.getUserId());
        String blacklistIds = "";// = getFromCache(key);
        logger.info("AAAAAAAAA blacklistIds: " + blacklistIds);
        if (blacklistIds == null || blacklistIds.equals("")) {
            HashMap<String, String> blacklist = db.getBlackListOfUserSMS(profile.getUserId());
//            if (blacklist == null) {
//                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
//                sendSMS(sms, mt);
//                return;
//            }
            logger.info("sms_blacklist_ids: " + blacklist.get("sms_blacklist_ids"));
            logger.info("sms_blacklist_ids: " + blacklist.get("sms"));
            blacklistIds = blacklist.get("sms") == null ? "" : blacklist.get("sms");
            //pushToCache(key, blacklistIds);
        }

        String seperator = "-";
        logger.info("AAAAAAAAA blacklistIds: " + blacklistIds);
        boolean inBlacklist = isBlacklist(friendUserId, blacklistIds, seperator);
        logger.info("AAAAAAAAA inBlacklist: " + inBlacklist);
        if (inBlacklist) {
            mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_INVALID_OR_EXIST", "");
        } else {
            SubProfileInfo friendProfile = db.getSubProfile("", friendUserId);
            if (friendProfile == null) {
                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                sendSMS(sms, mt);
                return;
            }

            if (friendProfile.getUserId() == 0) {
                mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_INVALID_OR_EXIST", "");
            } else {
                blacklistIds += blacklistIds.length() > 0 ? String.valueOf(friendUserId) + seperator : seperator + String.valueOf(friendUserId) + seperator;
                if (Constants.SUCCESS.equalsIgnoreCase(db.updateBlacklistSMSOfUser(profile.getUserId(), blacklistIds, friendProfile.getTelco()))) {
                    //   pushToCache(key, blacklistIds);
                    /*
                     * Cap nhat thanh cong
                     */
                    mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_ADD_SUCCESS", "");
                    mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(friendUserId));
                } else {
                    mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                }
            }
        }

        sendSMS(sms, mt);
    }

    private void removeFromBlacklistOfUser(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("removeFromBlacklistOfUser called: " + msisdn + "; sms: " + content);
        }

        String contentPart = content.substring(keyWord.length()).trim();
        if (contentPart.length() == 0 || !Helper.isNumber(contentPart)) {
            wrongSyntax(sms);
            return;
        }

        /*
         * Kiem tra dang ky
         */
        if (!checkRegister(sms)) {
            return;
        }

        /*
         * Kiem tra tai khoan
         */
        SubProfileInfo profile = checkProfile(sms);
        if (profile == null) {
            return;
        }

        int friendUserId = Helper.getInt(contentPart, 0);
        String mt = "";
        //   String key = "blacklist_sms_" + String.valueOf(profile.getUserId());
        //  String blacklistIds = getFromCache(key);
        String blacklistIds = null;
        if (blacklistIds == null) {
            HashMap<String, String> blacklist = db.getBlackListOfUserSMS(profile.getUserId());
            if (blacklist == null) {
                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                sendSMS(sms, mt);
                return;
            }
            blacklistIds = blacklist.get("sms") == null ? "" : blacklist.get("sms");
            //          blacklistIds = blacklist.get("sms") == null ? "" : blacklist.get("sms");
            //         pushToCache(key, blacklistIds);
        }

        String seperator = "-";
        boolean inBlacklist = isBlacklist(friendUserId, blacklistIds, seperator);
        if (!inBlacklist) {
            mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_REMOVE_INVALID_OR_EXIST", "");
        } else {
            SubProfileInfo friendProfile = db.getSubProfile("", friendUserId);
            if (friendProfile == null) {
                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                sendSMS(sms, mt);
                return;
            }

            if (friendProfile.getUserId() == 0) {
                mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_REMOVE_INVALID_OR_EXIST", "");
            } else {
                String item = seperator + String.valueOf(friendUserId) + seperator;
                blacklistIds = blacklistIds.replaceAll(item, "-");
                if (seperator.equals(blacklistIds)) {
                    blacklistIds = "";
                }

                if (Constants.SUCCESS.equalsIgnoreCase(db.updateBlacklistSMSOfUser(profile.getUserId(), blacklistIds, friendProfile.getTelco()))) {
                    //               pushToCache(key, blacklistIds);
                    /*
                     * Cap nhat thanh cong
                     */
                    mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_REMOVE_SUCCESS", "");
                    mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(friendUserId));
                } else {
                    mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                }
            }
        }

        sendSMS(sms, mt);
    }

    private void viewBlacklistOfUser(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("viewBlacklistOfUser called: " + msisdn);
        }

        String mt = "";
        /*
         * Kiem tra ho so
         */
        SubProfileInfo profile = checkProfile(sms);
        if (profile == null) {
            return;
        }

        HashMap<String, String> blacklist = null;
        String seperator = "-";
        // String key = "blacklist_sms_" + String.valueOf(profile.getUserId());
        String blacklistSMSIds = null;//= getFromCache(key);
        if (blacklistSMSIds == null) {
            blacklist = db.getBlackListOfUserSMS(profile.getUserId());
            if (blacklist == null) {
                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                sendSMS(sms, mt);
                return;
            }

            blacklistSMSIds = blacklist.get("sms") == null ? "" : blacklist.get("sms");
            //    pushToCache(key, blacklistIds);
        }

        //   key = "blacklist_" + String.valueOf(profile.getUserId());
        String ivrBlacklistIds = null;//getFromCache(key);
        if (ivrBlacklistIds == null) {
            if (blacklist == null) {
                blacklist = db.getBlackListOfUserSMS(profile.getUserId());
                if (blacklist == null) {
                    mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                    sendSMS(sms, mt);
                    return;
                }
            }

            ivrBlacklistIds = blacklist.get("ivr") == null ? "" : blacklist.get("ivr");
            //       pushToCache(key, ivrBlacklistIds);
        }

        String listBlacklistIVR = "";
        String listBlacklistSMS = "";
        if (ivrBlacklistIds.length() == 0 && blacklistSMSIds.length() == 0) {
            mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_EMPTY", "");
        } else {
            int limit = Helper.getInt(ConfigStack.getConfig("api_sms", "limitBlacklist", "8"), 8);
            if (ivrBlacklistIds.length() > 0) {
                String[] parts = ivrBlacklistIds.split(seperator);
                int i = -1;
                while (++i <= limit && i < parts.length) {
                    int userId = Helper.getInt(parts[i], 0);
                    if (userId > 0) {
                        listBlacklistIVR += listBlacklistIVR.length() > 0 ? "; " + String.valueOf(userId) : String.valueOf(userId);
                    }
                }
            }

            if (blacklistSMSIds.length() > 0) {
                String[] parts = blacklistSMSIds.split(seperator);
                int i = -1;
                while (++i <= limit && i < parts.length) {
                    int userId = Helper.getInt(parts[i], 0);
                    if (userId > 0) {
                        listBlacklistSMS += listBlacklistSMS.length() > 0 ? "; " + String.valueOf(userId) : String.valueOf(userId);
                    }
                }
            }

            if (listBlacklistIVR.length() > 0 && listBlacklistSMS.length() == 0) {
                mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_ONLY_IVR", "");
                mt = mt.replaceAll("\\{danh_sach\\}", listBlacklistIVR);
            } else if (listBlacklistIVR.length() == 0 && listBlacklistSMS.length() > 0) {
                mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_ONLY_SMS", "");
                mt = mt.replaceAll("\\{danh_sach\\}", listBlacklistSMS);
            } else {
                mt = ConfigStack.getConfig("mt", "FRIEND_BLACK_OK", "");
                mt = mt.replaceAll("\\{danh_sach_ivr\\}", listBlacklistIVR);
                mt = mt.replaceAll("\\{danh_sach_sms\\}", listBlacklistSMS);
            }
        }

        sendSMS(sms, mt);
    }

    private String sendMessage(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        String mt = "";
        if (logger.isDebugEnabled()) {
            logger.debug("sendMessage called: " + msisdn + "; sms: " + content);
        }

        String msgContent = content.substring(keyWord.length()).trim();
        msgContent = Helper.convert(msgContent);
        if (msgContent.length() == 0) {
            mt = wrongSyntax(sms);
            return mt;
        }

        /*
         * Kiem tra dang ky
         */
        if (!checkRegister(sms)) {
            return mt;
        }

        /*
         * Kiem tra tai khoan
         */
        SubProfileInfo profile = checkProfile(sms);
        if (profile == null) {
            return mt;
        }

        /*
         * Kiem tra ID nguoi nhan
         */
        int friendUserId = Helper.getInt(keyWord, 0);
        SubProfileInfo friendProfile = db.getSubProfile("", friendUserId);
        if (friendProfile == null) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            //sendSMS(sms, mt);
            return mt;
        } else if (friendProfile.getUserId() == 0) {
            mt = wrongSyntax(sms);
            return mt;
        }

        /*
         * Kiem tra blacklist
         */
        String key = "blacklist_sms_" + String.valueOf(friendUserId);
        //   String blacklistUserIds = getFromCache(key);
        //  if (blacklistUserIds == null) {
        HashMap<String, String> blacklist = db.getBlackListOfUserSMS(friendUserId);
        if (blacklist == null) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            //sendSMS(sms, mt);
            return mt;
        }

        String blacklistUserIds = blacklist.get("sms") == null ? "" : blacklist.get("sms");
        //   pushToCache(key, blacklistUserIds);
        // }

        String seperator = "-";
        boolean inBlacklist = isBlacklist(profile.getUserId(), blacklistUserIds, seperator);
        if (inBlacklist) {
            mt = ConfigStack.getConfig("mt", "FRIEND_CHAT_BLACK_LIST", "");
            logger.info("@@@@@@@@@@@@AAAAAAAAAAA: " + mt);
            logger.info("@@@@@@@@@@@@AAAAAAAAAAA: " + friendUserId);
            mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(friendUserId));
            logger.info("@@@@@@@@@@@@AAAAAAAAAAA: " + friendUserId);
            //sendSMS(sms, mt);
            return mt;
        }

        /*
         * Kiem tra gioi han gui tin 1 ngay
         */
        int limitSMSInDay = Helper.getInt(ConfigStack.getConfig("api_sms", "limitChatSMS", "10"), 10);
        int smsCount = db.countChatSMSHistoryInCurrentDay(msisdn);
        logger.info("AAAAAAAAAAAA smsCount: " + smsCount);
        if (smsCount < 0) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            //sendSMS(sms, mt);
            return mt;
        } else if (smsCount >= limitSMSInDay) {
            mt = ConfigStack.getConfig("mt", "FRIEND_CHAT_LIMITED", "");
            mt = mt.replaceAll("\\{so_luong\\}", String.valueOf(limitSMSInDay));
            // sendSMS(sms, mt);
            return mt;
        }

        /*
         * Kiem tra ID nguoi gui ton tai trong friendlist nguoi nhan
         */
        boolean inFriendListOfReceiver = db.countInFriendList(friendUserId, profile.getUserId()) > 0;

        /*
         * Tu dong duyet hay khong ? 
         */
        int autoApproveSMS = Helper.getInt(ConfigStack.getConfig("friend", "AUTO_APPROVE_SMS_MSG", ""), 1);
        int approveStatus = autoApproveSMS == 1 ? Constants.MSG_APPROVED_STATUS : Constants.MSG_NOT_APPROVE_STATUS;
        int fromTelco = db.getTelcoFromID(profile.getUserId());
        int toTelco = db.getTelcoFromID(friendUserId);
        SubMessageInfo msg = new SubMessageInfo();
        msg.setIdSender(profile.getUserId());
        msg.setIdReceiver(friendUserId);
        msg.setSender(msisdn);
        msg.setReceiver(friendProfile.getMsisdn());
        msg.setSmsContent(msgContent);
        msg.setFromTelco(fromTelco);
        msg.setToTelco(toTelco);
        if (toTelco == 0) {
            msg.setPushStatus(approveStatus == Constants.MSG_APPROVED_STATUS ? 1 : 0);
        } else {
            msg.setPushStatus(0);
        }
        /*
         * Lich su tuong tac sms chat cua nguoi nhan
         */
        int actionKey = Constants.INTERACT_SMS_CHAT;
        key = "interact_" + profile.getUserId() + "_" + friendUserId + "_" + String.valueOf(actionKey);
        String lastInteractDate = getFromCache(key);
        logger.info("lastInteractDate: " + lastInteractDate);
//        if (lastInteractDate == null) {
//            lastInteractDate = db.getLastInteractDate(msisdn, actionKey);
//            int timeOut = Integer.parseInt(ConfigStack.getConfig("FRIEND", "GUIDE_VOICE_MAIL_DAYS", "7")) * 24 * 60 * 60;
//            pushToCacheWithExpiredTime(key, lastInteractDate, timeOut);
//            //pushToCache(key, lastInteractDate);
//        }

        boolean guideSMSChat = true;
        String dateFormat = "yyyy-MM-dd";
        if (!Helper.isEmpty(lastInteractDate)) {
            int dayRange = Helper.getInt(ConfigStack.getConfig("friend", "GUIDE_SMS_CHAT_DAYS", ""), 30);
            logger.info("dayRange: " + dayRange);
            if (!Helper.checkDateOutOfRange(lastInteractDate, dayRange, dateFormat)) {
                guideSMSChat = false;
            }
            logger.info("guideSMSChat: " + guideSMSChat);
        }

        if (db.insertChatSMS(msg, approveStatus)) {
            /*
             * Gui MT1
             */
            if (toTelco == Constants.TELCO_VINA) {
                mt = ConfigStack.getConfig("mt", "FRIEND_CHAT_SEND_TO_RECEIVER", "");
                mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(profile.getUserId()));
                mt = mt.replaceAll("\\{tin_nhan\\}", msgContent);
                int mtType = SMSType.Genral.getValue();
                sms.setMsisdn(friendProfile.getMsisdn());
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                sms.setMtContent(mt);
                sms.setAction("SEND_FRIENDLY");
                sms.setType(mtType);
                // restfulStack.sendMT(sms);
            }
            //sendSMS(friendProfile.getMsisdn(), mt, smsId);

            /*
             * Tang so tin nhan chat da gui
             */
            smsCount++;
            db.updateChatSMSHistory(msisdn, smsCount);

            int smsChatDelay = Helper.getInt(ConfigStack.getConfig("friend", "SMS_CHAT_DELAY_IN_SECOND", "3"), 3) * 1000;
            if (guideSMSChat && !inFriendListOfReceiver) {
                /*
                 * Gui them MT2
                 */

                if (toTelco == Constants.TELCO_VINA) {
                    if (smsChatDelay > 0) {
                        try {
                            Thread.sleep(smsChatDelay);
                        } catch (Exception e) {
                        }
                    }
                    mt = ConfigStack.getConfig("mt", "FRIEND_CHAT_GUIDE_NOT_FRIENDLIST", "");
                    mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(profile.getUserId()));
                    int mtType = SMSType.Genral.getValue();
                    sms.setMsisdn(friendProfile.getMsisdn());
                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                    sms.setMtContent(mt);
                    sms.setAction("SEND_FRIENDLY");
                    sms.setType(mtType);
                    //restfulStack.sendMT(sms);
                }
                //  sendSMS(friendProfile.getMsisdn(), mt, smsId);
            } else if (guideSMSChat && inFriendListOfReceiver) {
                /*
                 * Gui them MT3
                 */

                if (toTelco == Constants.TELCO_VINA) {
                    if (smsChatDelay > 0) {
                        try {
                            Thread.sleep(smsChatDelay);
                        } catch (Exception e) {
                        }
                    }
                    mt = ConfigStack.getConfig("mt", "FRIEND_CHAT_GUIDE_HAS_FRIENDLIST", "");
                    mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(profile.getUserId()));
                    int mtType = SMSType.Genral.getValue();
                    sms.setMsisdn(friendProfile.getMsisdn());
                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                    sms.setMtContent(mt);
                    sms.setAction("SEND_FRIENDLY");
                    sms.setType(mtType);
                    //restfulStack.sendMT(sms);
                    //sendSMS(friendProfile.getMsisdn(), mt, smsId);
                }
            }

            /*
             * Cap nhat lich su tuong tac cua nguoi gui
             */
            key = "interact_" + String.valueOf(profile.getUserId()) + "_" + String.valueOf(actionKey);
            String currentDate = new SimpleDateFormat(dateFormat).format(Calendar.getInstance().getTime());
            lastInteractDate = getFromCache(key);
//            if (lastInteractDate == null) {
//                lastInteractDate = db.getLastInteractDate(msisdn, actionKey);
//            }
            if (Helper.isEmpty(lastInteractDate) || !currentDate.equalsIgnoreCase(lastInteractDate)) {
                lastInteractDate = currentDate;

                if (db.updateLastInteractDate(msisdn, actionKey)) {
                    // int timeOut = Integer.parseInt(ConfigStack.getConfig("FRIEND", "GUIDE_VOICE_MAIL_DAYS", "7")) * 24 * 60 * 60;
                    //pushToCacheWithExpiredTime(key, msisdn, timeOut);
                    pushToCache(key, lastInteractDate);
                }
            }

        } else {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            //sendSMS(sms, mt);
        }
        key = "interact_" + profile.getUserId() + "_" + friendUserId + "_" + String.valueOf(actionKey);
        int timeOut = Integer.parseInt(ConfigStack.getConfig("FRIEND", "GUIDE_VOICE_MAIL_DAYS", "7")) * 24 * 60 * 60;
        pushToCacheWithExpiredTime(key, lastInteractDate, timeOut);
        logger.debug("key: " + key + "; lastInteractDate: " + lastInteractDate);
        return mt;
    }

    private void viewFriendProfile(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        if (logger.isDebugEnabled()) {
            logger.debug("viewFriendProfile called: " + msisdn + "; sms: " + content);
        }

        String contentPart = content.substring(keyWord.length()).trim();
        if (contentPart.length() == 0 || !Helper.isNumber(contentPart)) {
            wrongSyntax(sms);
            return;
        }
        int friendUserId = Helper.getInt(contentPart, 0);

        String mt = "";

        /*
         * Kiem tra tai khoan
         */
        SubProfileInfo friendProfile = db.getSubProfile("", friendUserId);
        if (friendProfile == null) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            sendSMS(sms, mt);
            return;
        }
        if (friendProfile.getUserId() == 0) {
            mt = ConfigStack.getConfig("mt", "FRIEND_PROFILE_FRIEND_NOT_ID", "");
            sendSMS(sms, mt);
            return;
        }

        if (friendProfile.getStatus() >= Constants.PROFILE_STATUS_ACTIVE) {
            boolean hasMinimumInfo = !Helper.isEmpty(friendProfile.getName())
                    || friendProfile.getSex() > 0
                    || friendProfile.getBirthYear() > 0
                    || friendProfile.getProvinceId() > 0;

            if (hasMinimumInfo) {
                mt = ConfigStack.getConfig("mt", "FRIEND_PROFILE_FRIEND_OK", "");
                mt = mt.replaceAll("\\{ho_ten\\}", Helper.isEmpty(friendProfile.getName()) ? "" : "Ho ten: " + ConvertUnsignedString.getUnsignedString(friendProfile.getName()).toUpperCase() + ";");
                mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(friendProfile.getUserId()));
                mt = mt.replaceAll("\\{gioi_tinh\\}", friendProfile.getSex() > 0 ? "; " + getSexName(friendProfile.getSex()) : "");
                mt = mt.replaceAll("\\{nam_sinh\\}", friendProfile.getBirthYear() > 0 ? "; " + String.valueOf(friendProfile.getBirthYear()) : "");
                mt = mt.replaceAll("\\{tinh_thanh\\}", friendProfile.getProvinceId() > 0 ? "; " + getProvinceName(friendProfile.getProvinceId(), db.getListProvince()).toUpperCase() : "");
            } else {
                mt = ConfigStack.getConfig("mt", "FRIEND_PROFILE_FRIEND_EMPTY", "");
                mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(friendUserId));
            }
        } else {
            mt = ConfigStack.getConfig("mt", "FRIEND_PROFILE_FRIEND_EMPTY", "");
            mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(friendUserId));
        }

        sendSMS(sms, mt);
    }

    private void inviteFriend(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        String mt = "";
        String friendMobile = content.substring(keyWord.length()).trim();
        friendMobile = Helper.processMobile(friendMobile);
        if (friendMobile.length() == 0) {
            wrongSyntax(sms);
            return;
        }
        if (!Helper.isMobileNumber(friendMobile)) {
            sms.setMsisdn(friendMobile);
            mt = ConfigStack.getConfig("mt", "NUMBER_NOT_INVITE", "");
            sendSMS(sms, mt);
            return;
        }
        logger.info("friendMobile: " + friendMobile);
        String source = "SMS";


        /*
         * Kiem tra dang ky nguoi gui
         */
        if (!checkRegister(sms)) {
            return;
        }

        /*
         * Kiem tra trang thai dang ky nguoi nhan
         */
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("msisdn", friendMobile);
        com.vega.alome.sbb.billing.bundletype.SubPackageInfo subPackage = billing.getSubPackage(param);
        if (subPackage.getErrorCode().getValue() == BillingErrorCode.SystemError.getValue()
                || subPackage.getErrorCode().getValue() == BillingErrorCode.ChargingSubProcessing.getValue()) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            sendSMS(sms, mt);
            return;
        }

        if (subPackage.getErrorCode().getValue() == BillingErrorCode.Success.getValue()
                && subPackage.getStatus().getValue() == SubPackageInfo.SubPackageStatus.Active.getValue()) {
            logger.info("Kiem tra ho so nguoi nhan: " + friendMobile);
            SubProfileInfo friendProfile = db.getSubProfile(friendMobile, 0);
            if (friendProfile == null) {
                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                sendSMS(sms, mt);
                return;
            }

            String key = "NotSendSmsInSecond_" + msisdn + "_" + friendMobile;
            logger.info("AAAAAAAAAAAAAAAAAAAAAA key: " + key);
            String secondSendsms = getFromCache(key);
            logger.info("secondSendsms: " + secondSendsms);
            /*
             * Kiem tra ho so nguoi nhan
             */
            if (secondSendsms != null && !secondSendsms.equals("")) {
                /*
                 * Gui MT cho nguoi gioi thieu lan 2
                 */
                logger.info("Gui MT cho nguoi gioi thieu: " + msisdn);
                if (friendProfile.getUserId() > 0) {
                    mt = ConfigStack.getConfig("mt", "INVITE_SENDER_EXISTED", "");
                } else {
                    mt = ConfigStack.getConfig("mt", "INVITE_SENDER_REGED_NOT_ID", "");
                }
                if (mt != null) {
                    mt = mt.replaceAll("\\{so_dien_thoai\\}", friendMobile);
                    sendSMS(sms, mt);
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                    }
                }
            } else {
                /*
                 * Gui MT cho nguoi gioi thieu lan 1
                 */
                logger.info("Gui MT cho nguoi gioi thieu: " + msisdn);
                if (friendProfile.getUserId() > 0) {
                    mt = ConfigStack.getConfig("mt", "INVITE_SENDER_REGED_HAS_ID", "");
                } else {
                    mt = ConfigStack.getConfig("mt", "INVITE_SENDER_REGED_NOT_ID", "");
                }
                if (mt != null) {
                    mt = mt.replaceAll("\\{so_dien_thoai\\}", friendMobile);
                    sendSMS(sms, mt);
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                    }
                }
            }
            /*
             * Gui MT cho nguoi duoc gioi thieu
             Gioi thieu lan 2 thi ko tra ve sms nay nuwax
             */
            if (secondSendsms != null && !secondSendsms.equals("")) {
                logger.info("secondSendsms: " + secondSendsms);
                int timeOut = Integer.parseInt(ConfigStack.getConfig("FRIEND", "GUIDE_VOICE_MAIL_DAYS", "7")) * 24 * 60 * 60;
                secondSendsms = msisdn + "_" + friendMobile;
                pushToCacheWithExpiredTime(key, secondSendsms, timeOut);
                logger.info("Khong gui tin nhan cho nguoi dc GT");
            } else {

                logger.info("Gui MT cho nguoi duoc gioi thieu: " + friendMobile);
                mt = ConfigStack.getConfig("mt", "INVITE_RECEIVER_REGED", "");
                if (mt != null) {
                    mt = mt.replaceAll("\\{so_dien_thoai\\}", msisdn);

                    if (!Helper.isOutOfRangeValidHour(ConfigStack.getConfig("general", "validRangeHourToPushSMS", "-"), "-")) {
                        // sendSMS(friendMobile, mt, smsId);
                        int mtType = SMSType.Genral.getValue();
                        sms.setMsisdn(friendMobile);
                        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                        sms.setMtContent(mt);
                        sms.setAction("SEND_FRIENDLY");
                        sms.setType(mtType);
                        restfulStack.sendMT(sms);
//                        try {
//                            Thread.sleep(5000);
//                        } catch (Exception e) {
//                        }
                    } else {
                        restfulStack.sendMTLow(friendMobile, 0, mt, SMSType.Genral.getValue(), true);
                        // db.insertSMSDelay(friendMobile, mt);
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("FRIEND", "GUIDE_VOICE_MAIL_DAYS", "7")) * 24 * 60 * 60;
                    secondSendsms = msisdn + "_" + friendMobile;
                    pushToCacheWithExpiredTime(key, secondSendsms, timeOut);
                    logger.info("Gui MT cho nguoi dc gioi thieu vao ghi vao bo nho GT");
                }
            }

            return;
        }

        /*
         * Kiem tra trang thai gioi thieu
         */
        logger.info("Kiem tra trang thai gioi thieu: " + friendMobile);
        String inviteResult = db.checkSubInvited(msisdn, friendMobile);
        logger.info("inviteResult: " + inviteResult);
        if (inviteResult == null) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            sendSMS(sms, mt);
            return;
        }

        if (inviteResult.equals(Constants.SUCCESS)) {
            /*
             * Tao loi gioi thieu
             */
            int point = Helper.getInt(ConfigStack.getConfig("game", "pointInviteFriend", "0"));

            if (db.insertSubInvitation(msisdn, friendMobile, point, source).equalsIgnoreCase(Constants.SUCCESS)) {
                /*
                 * Gui MT cho nguoi gioi thieu
                 */
                logger.info("AAAAAAAAAAAAAAAA");
                mt = ConfigStack.getConfig("mt", "INVITE_SENDER_SUCCESS", "");
                logger.info("mt" + mt);
                if (mt != null) {
                    mt = mt.replaceAll("\\{so_dien_thoai\\}", friendMobile);
                    mt = mt.replaceAll("\\{diem\\}", String.valueOf(point));
                    sms.setMtContent(mt);
                    sendSMS(sms, mt);
                    logger.info("mt" + sms.getMtContent());
                    restfulStack.sendMT(sms);
                    try {
                        Thread.sleep(5000);                 //1000 milliseconds is one second.
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

                /*
                 * Gui MT cho nguoi duoc gioi thieu
                 */
                mt = ConfigStack.getConfig("mt", "INVITE_RECEIVER_SUCCESS", "");
                mt = mt.replaceAll("\\{so_dien_thoai\\}", msisdn);
                logger.info("AAAAAAAAAAAAAAAA");
                if (mt != null) {
                    if (!Helper.isOutOfRangeValidHour(ConfigStack.getConfig("general", "validRangeHourToPushSMS", "-"), "-")) {
                        int mtType = SMSType.Genral.getValue();
                        sms.setMsisdn(friendMobile);
                        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                        sms.setMtContent(mt);
                        sms.setAction("SEND_FRIENDLY");
                        sms.setType(mtType);
                        restfulStack.sendMT(sms);
                        try {
                            Thread.sleep(5000);
                        } catch (Exception e) {
                        }
                    } else {
                        restfulStack.sendMTLow(friendMobile, 0, mt, SMSType.Genral.getValue(), true);
                        // db.insertSMSDelay(friendMobile, mt);
                    }
                }
            } else {
                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                sendSMS(sms, mt);
            }

        } else {
            /*
             * Gui MT thong bao da duoc gioi thieu truoc do
             */
            mt = ConfigStack.getConfig("mt", "NUMBER_NOT_INVITE", "");
            //  mt = mt.replaceAll("\\{so_dien_thoai\\}", friendMobile);
            logger.info("friendMobile: " + friendMobile);
            logger.info("mt: " + mt);
            sendSMS(sms, mt);
        }
    }

    private boolean checkRegister(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        boolean registered = true;
        String mt = "";
        /*
         * Kiem tra goi cuoc
         */
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("msisdn", msisdn);
        com.vega.alome.sbb.billing.bundletype.SubPackageInfo sub = billing.getSubPackage(param);
        if (sub.getErrorCode().getValue() == BillingErrorCode.SystemError.getValue() || sub.getErrorCode().getValue() == BillingErrorCode.ChargingSubProcessing.getValue()) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            sendSMS(sms, mt);
            registered = false;
        } else if (sub.getErrorCode().getValue() == BillingErrorCode.NotFoundData.getValue()
                || (sub.getErrorCode().getValue() == BillingErrorCode.Success.getValue() && sub.getStatus().getValue() == SubPackageInfo.SubPackageStatus.Cancel.getValue())) {
            mt = ConfigStack.getConfig("mt", "FRIEND_NOT_REG", "");
            sendSMS(sms, mt);
            registered = false;
        }

        return registered;
    }

    private SubProfileInfo checkProfile(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String smsId = sms.getSmsId();
        String keyWord = sms.getMoKeyword();
        /*
         * Kiem tra thong tin ca nhan
         */
        String mt = "";
        SubProfileInfo profile = db.getSubProfile(msisdn, 0);
        if (profile == null) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            sendSMS(sms, mt);
            profile = null;
        }
        if (profile.getUserId() == 0) {
            mt = ConfigStack.getConfig("mt", "FRIEND_NOT_ID", "");
            sendSMS(sms, mt);
            profile = null;
        }

        return profile;
    }

    private ProvinceInfo getProvinceFromSign(String sign, ArrayList<ProvinceInfo> provinces) {
        ProvinceInfo p = null;

        if (provinces != null && sign != null) {
            sign = "," + sign.trim() + ",";
            for (int i = 0; i < provinces.size(); i++) {
                String fullIdentifySign = provinces.get(i).getIdentifySign();

                int a = fullIdentifySign.toLowerCase().indexOf(sign.toLowerCase());
                logger.info("AAAAA: " + a + " sign " + sign + " fullIdentifySign " + fullIdentifySign);
                if (a >= 0) {
                    return provinces.get(i);
                }
//		String fullIdentifySign = SEPERATOR + this.identifySign + SEPERATOR;
//		
//		return fullIdentifySign.toLowerCase().indexOf(sign.toLowerCase()) >= 0;
//                if (provinces.get(i).findProvince(sign)) {
//                    return provinces.get(i);
//                }
            }
        }

        return p;
    }

    private void registerLotteryCallout(SMS sms) {
        String msisdn = sms.getMsisdn();
        String keyword = sms.getMoKeyword();
        String content = sms.getMoContent();
        String mt = "";
        String provinceNameInput = content.toUpperCase().replaceFirst(keyword.toUpperCase(), "").replaceAll(" ", "").trim();
        String provinceName = provinceNameInput;
        if ("MIENBAC".equalsIgnoreCase(provinceName)) {
            provinceName = "HANOI";
        }
        ProvinceInfo p = getProvinceFromSign(provinceName, db.getListProvince());
        if (p != null) {
            int region = 0;
            List<String> listProvinceId1 = Arrays.asList(ConfigStack.getConfig("lottery", "province_ids_region_1", "").split("-"));
            List<String> listProvinceId2 = Arrays.asList(ConfigStack.getConfig("lottery", "province_ids_region_2", "").split("-"));
            List<String> listProvinceId3 = Arrays.asList(ConfigStack.getConfig("lottery", "province_ids_region_3", "").split("-"));
            if (listProvinceId1.contains(String.valueOf(p.getProvinceId()))) {
                region = 1;
            } else if (listProvinceId2.contains(String.valueOf(p.getProvinceId()))) {
                region = 2;
            } else if (listProvinceId3.contains(String.valueOf(p.getProvinceId()))) {
                region = 3;
            }
            if (region > 0) {
                SubPackageInfo subInfo = db.checkSubPackage(sms.getMsisdn());
                logger.debug("subInfo:" + subInfo.getPackageId());
                boolean checkRegister = false;
                if (subInfo.getPackageId() > 0) {
                    checkRegister = true;
                }
                if (checkRegister) {
                    int status = 1;
                    if (lotteryDao.updateLotteryCalloutStatusOfSub(msisdn, region, p.getProvinceId(), status, "", "SMS") == 0) {
                        mt = ConfigStack.getConfig("mt", "LOTTERY_REG_CALLOUT_SUCCESS", "");
                        mt = mt.replaceAll("\\{tinh\\}", provinceNameInput);

                        LotteryHisDTO his = new LotteryHisDTO();
                        his.setMsisdn(msisdn);
                        his.setChannel("SMS");
                        his.setAction(LotteryHisDTO.ACT_SETUP_RECV_EVERY_DAY);
                        his.setProvinceId(p.getProvinceId());
                        his.setRegion(region);
                        his.setResult(LotteryHisDTO.RES_SUCCESS);
                        lotteryDao.insertLotteryHis(his);
                    } else {
                        mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                    }
                } else {
                    mt = ConfigStack.getConfig("mt", "LOTTERY_REG_CALLOUT_NOT_SUB", "");
                    mt = mt.replaceAll("\\{tinh\\}", provinceNameInput);
                }
            } else {
                mt = ConfigStack.getConfig("mt", "SCP", "");
            }
        } else {
            mt = ConfigStack.getConfig("mt", "SCP", "");
        }
        logger.info("AAAAAAAAAAAAA: mt => " + mt);
        int mtType = SMSType.Genral.getValue();
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setAction("LOTTERY");
        sms.setType(mtType);
        restfulStack.sendMT(sms);
    }

    private void guideLottery(SMS sms) {
        String mt = ConfigStack.getConfig("mt", "LOTTERY_GUIDE", "");
        logger.info("AAAAAAAAAAAAA: mt => " + mt);
        int mtType = SMSType.Genral.getValue();
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setAction("LOTTERY");
        sms.setType(mtType);
        restfulStack.sendMT(sms);
    }

    private void cancelLotteryCallout(SMS sms) {
        String msisdn = sms.getMsisdn();
        String keyword = sms.getMoKeyword();
        String content = sms.getMoContent();
        String mt = "";

        String provinceNameInput = content.toUpperCase().replaceFirst(keyword.toUpperCase(), "").replaceAll(" ", "").trim();
        String provinceName = provinceNameInput;
        if ("MIENBAC".equalsIgnoreCase(provinceName)) {
            provinceName = "HANOI";
        }
        ProvinceInfo p = getProvinceFromSign(provinceName, db.getListProvince());
        if (p != null) {
            int region = 0;
            List<String> listProvinceId1 = Arrays.asList(ConfigStack.getConfig("lottery", "province_ids_region_1", "").split("-"));
            List<String> listProvinceId2 = Arrays.asList(ConfigStack.getConfig("lottery", "province_ids_region_2", "").split("-"));
            List<String> listProvinceId3 = Arrays.asList(ConfigStack.getConfig("lottery", "province_ids_region_3", "").split("-"));
            if (listProvinceId1.contains(String.valueOf(p.getProvinceId()))) {
                region = 1;
            } else if (listProvinceId2.contains(String.valueOf(p.getProvinceId()))) {
                region = 2;
            } else if (listProvinceId3.contains(String.valueOf(p.getProvinceId()))) {
                region = 3;
            }
            if (region > 0) {
                SubPackageInfo subInfo = db.checkSubPackage(sms.getMsisdn());
                boolean checkRegister = false;
                if (subInfo.getPackageId() > 0) {
                    checkRegister = true;
                }
                if (checkRegister) {
                    /*
                     * Da dang ky callout truoc do ?
                     */
                    int status = lotteryDao.getStatusCalloutOfSub(msisdn, p.getProvinceId());
                    if (status > 0) {
                        status = 0;
                        if (lotteryDao.updateLotteryCalloutStatusOfSub(msisdn, region, p.getProvinceId(), status, "", "SMS") == 0) {
                            mt = ConfigStack.getConfig("mt", "LOTTERY_CANCEL_CALLOUT_SUCCESS", "");
                            logger.info("AAAAAAAAAAAAA: mt => " + mt);
                            mt = mt.replaceAll("\\{tinh\\}", provinceNameInput);

                            LotteryHisDTO his = new LotteryHisDTO();
                            his.setMsisdn(msisdn);
                            his.setChannel("SMS");
                            his.setAction(LotteryHisDTO.ACT_CANCEL_RECV_RESULT);
                            his.setProvinceId(p.getProvinceId());
                            his.setRegion(region);
                            his.setResult(LotteryHisDTO.RES_SUCCESS);
                            lotteryDao.insertLotteryHis(his);
                        } else {
                            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
                            logger.info("AAAAAAAAAAAAA: mt => " + mt);
                        }
                    } else {
                        mt = ConfigStack.getConfig("mt", "LOTTERY_CANCEL_CALLOUT_NOT_REG", "");
                        mt = mt.replaceAll("\\{tinh\\}", provinceNameInput);
                        logger.info("AAAAAAAAAAAAA: mt => " + mt);
                    }
                } else {
                    mt = ConfigStack.getConfig("mt", "LOTTERY_CANEL_CALLOUT_NOT_SUB", "");
                    mt = mt.replaceAll("\\{tinh\\}", provinceNameInput);
                    logger.info("AAAAAAAAAAAAA: mt => " + mt);
                }
            } else {
                mt = ConfigStack.getConfig("mt", "SCP", "");
                logger.info("AAAAAAAAAAAAA: mt => " + mt);

            }
        } else {
            mt = ConfigStack.getConfig("mt", "SCP", "");
        }
        // sendSMS(msisdn, mt);
        logger.info("AAAAAAAAAAAAA: mt => " + mt);
        int mtType = SMSType.Genral.getValue();
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setAction("LOTTERY");
        sms.setType(mtType);
        restfulStack.sendMT(sms);
    }
//

    private void rejectHealthGame(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String mt = "";
        int gameKey = Constants.GAME_KEY_HEALTH;
        boolean updateStatus = gameDao.updateStatusRejectGame(msisdn, gameKey);
        if (updateStatus) {
            mt = ConfigStack.getConfig("mt", "GAME_HEALTH_REJECT_OK", "");
        } else {
            mt = ConfigStack.getConfig("mt", "GAME_HEALTH_REJECT_FAILED", "");
        }
        int mtType = SMSType.Genral.getValue();
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setAction("GAME_HEALTH");
        sms.setType(mtType);
        restfulStack.sendMT(sms);
    }

    private void regHealthGame(SMS sms) {
        String mt = "";
        /*
         * Kiem tra dang ky dich vu
         */
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        SubPackageInfo sub = db.getLastSubPackage(msisdn, false, 0);
        if (sub.getErrorCode().getValue() == BillingErrorCode.SystemError.getValue() || sub.getErrorCode().getValue() == BillingErrorCode.ChargingSubProcessing.getValue()) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            int mtType = SMSType.Genral.getValue();
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setAction("GAME_HEALTH");
            sms.setType(mtType);
            restfulStack.sendMT(sms);
            return;
        } else if (sub.getErrorCode().getValue() == BillingErrorCode.NotFoundData.getValue()
                || (sub.getErrorCode().getValue() == BillingErrorCode.Success.getValue() && sub.getStatus().getValue() == SubPackageStatus.Cancel.getValue())) {
            mt = ConfigStack.getConfig("mt", "GAME_HEALTH_NOT_REG", "");
            int mtType = SMSType.Genral.getValue();
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setAction("GAME_HEALTH");
            sms.setType(mtType);
            restfulStack.sendMT(sms);
            return;
        }
        int gameKey = Constants.GAME_KEY_HEALTH;
        boolean updateStatus = gameDao.updateStatusPlayGame(msisdn, gameKey);
        if (updateStatus) {
            mt = ConfigStack.getConfig("mt", "GAME_HEALTH_REG_OK", "");
        } else {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
        }
        int mtType = SMSType.Genral.getValue();
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setAction("GAME_HEALTH");
        sms.setType(mtType);
        restfulStack.sendMT(sms);
    }
    //  game connect

    private void rejectConnectGame(SMS sms) {
        String msisdn = sms.getMsisdn();
        SubPackageInfo sub = db.getLastSubPackage(msisdn, false, 0);
        String mt = "";
        if (sub.getErrorCode().getValue() == BillingErrorCode.SystemError.getValue()
                || sub.getErrorCode().getValue() == BillingErrorCode.ChargingSubProcessing.getValue()) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            int mtType = SMSType.Genral.getValue();
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setAction("GAME_CONNECT");
            sms.setType(mtType);
            restfulStack.sendMT(sms);
            logger.info("MT SYSTEM_ERROR");
            return;
        } else if (sub.getErrorCode().getValue() == BillingErrorCode.NotFoundData.getValue()
                || (sub.getErrorCode().getValue() == BillingErrorCode.Success.getValue()
                && sub.getStatus().getValue() == SubPackageStatus.Cancel.getValue())) {
            mt = ConfigStack.getConfig("mt", "GAME_HEALTH_NOT_REG", "");
            int mtType = SMSType.Genral.getValue();
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setAction("GAME_CONNECT");
            sms.setType(mtType);
            restfulStack.sendMT(sms);
            logger.info("MT GAME_HEALTH_NOT_REG");
            return;
        }
        Result rsCheck = new Result();
        rsCheck = connectDao.checkCountReject(msisdn);
        String errorCodeCheck = rsCheck.getErrorCode();
        int status = rsCheck.getStatus();
        int countReject = rsCheck.getCount_reject();
        logger.info("STATUS :" + status);
        logger.info("Count Reject SMS :" + countReject);
        if ("0".equals(errorCodeCheck) && status == 1) {
            // ban da tu choi
            mt = ConfigStack.getConfig("mt", "mt_game_connect_reject_success", "");
            int mtType = SMSType.Genral.getValue();
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setAction("GAME_CONNECT");
            sms.setType(mtType);
            restfulStack.sendMT(sms);
            return;
        } else if ("0".equals(errorCodeCheck) && status == 0) {
            if (countReject % 3 == 0) {
                logger.info("Du dieu kien");
                // gui tin du dieu kien tu choi va up date
                int result = connectDao.updateRejectConnect(msisdn, 0, 1);
                if (result == 0) {
                    mt = ConfigStack.getConfig("mt", "mt_game_connect_reject_success", "");
                    int mtType = SMSType.Genral.getValue();
                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                    sms.setMtContent(mt);
                    sms.setAction("GAME_CONNECT");
                    sms.setType(mtType);
                    restfulStack.sendMT(sms);
                }
            } else {
                // k du dieu kien 
                logger.info("KHONG DU DIEU KIEN");
                mt = ConfigStack.getConfig("mt", "mt_game_connect_reject_fail", "");
                int mtType = SMSType.Genral.getValue();
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                sms.setMtContent(mt);
                sms.setAction("GAME_CONNECT");
                sms.setType(mtType);
                restfulStack.sendMT(sms);
                return;
            }
        }
        return;
    }

    private void acceptConnectGame(SMS sms) {
        String msisdn = sms.getMsisdn();
        SubPackageInfo sub = db.getLastSubPackage(msisdn, false, 0);
        String mt = "";
        if (sub.getErrorCode().getValue() == BillingErrorCode.SystemError.getValue()
                || sub.getErrorCode().getValue() == BillingErrorCode.ChargingSubProcessing.getValue()) {
            mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            int mtType = SMSType.Genral.getValue();
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setAction("GAME_CONNECT");
            sms.setType(mtType);
            restfulStack.sendMT(sms);
            logger.info("MT SYSTEM_ERROR");
            return;
        } else if (sub.getErrorCode().getValue() == BillingErrorCode.NotFoundData.getValue()
                || (sub.getErrorCode().getValue() == BillingErrorCode.Success.getValue()
                && sub.getStatus().getValue() == SubPackageStatus.Cancel.getValue())) {
            mt = ConfigStack.getConfig("mt", "GAME_HEALTH_NOT_REG", "");
            int mtType = SMSType.Genral.getValue();
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setAction("GAME_CONNECT");
            sms.setType(mtType);
            restfulStack.sendMT(sms);
            logger.info("MT GAME_HEALTH_NOT_REG");
            return;
        }
        int result = connectDao.updateRejectConnect(msisdn, 0, 0);
        if (result == 0) {
            mt = ConfigStack.getConfig("mt", "mt_game_connect_reg_success", "");
            int mtType = SMSType.Genral.getValue();
            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            sms.setMtContent(mt);
            sms.setAction("GAME_CONNECT");
            sms.setType(mtType);
            restfulStack.sendMT(sms);
            return;
        }
        return;
    }
//

    private void addSubBlackList(SMS sms) {
        String msisdn = sms.getMsisdn();
        int result = db.insertSubBlackList(msisdn);
        if (result == 0) {
            String mt = ConfigStack.getConfig("mt", "TC_SUCCESS", "");
            sendSMS(sms, mt);
        }
    }

    private void rejectWeather(SMS sms) {
        String msisdn = sms.getMsisdn();
        String content = sms.getMoContent();
        String mt = "";
        SubPackageInfo subInfo = db.checkSubPackage(msisdn);
        logger.info("rejectWeather subInfo :" + subInfo.getErrorCode());
        if (subInfo.getErrorCode().getValue() == BillingErrorCode.Success.getValue()) {
            logger.info("rejectWeather Insert vao bang reject ");
            int status = 1;
            int updateStatus = weatherDao.insertRejectWeather(msisdn, status);
            logger.info("rejectWeather updateStatus:" + updateStatus);
            if (updateStatus == 0) {
                mt = ConfigStack.getConfig("mt", "WEATHER_REJECT_OK", "");
            } else if (updateStatus == 2) {
                mt = ConfigStack.getConfig("mt", "WEATHER_REJECT_FAILED", "");
            } else {
                mt = ConfigStack.getConfig("mt", "SYSTEM_ERROR", "");
            }
        } else {
            mt = ConfigStack.getConfig("mt", "WEATHER_NOT_REGIS", "");
        }
        logger.info("rejectWeather mt:" + mt);
        int mtType = SMSType.Genral.getValue();
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setAction("WEATHER");
        sms.setType(mtType);
        restfulStack.sendMT(sms);
    }

    private String getSexName(int sex) {
        String name = "";
        if (sex == 1) {
            name = "Nu";
        } else if (sex == 2) {
            name = "Nam";
        } else if (sex == 3) {
            name = "Gioi tinh thu 3";
        }
        return name;
    }

    private void sendSMS(SMS sms, String mt) {
        int mtType = SMSType.Genral.getValue();
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
        sms.setMtContent(mt);
        sms.setAction("SEND_FRIENDLY");
        sms.setType(mtType);
        restfulStack.sendMT(sms);
    }

    private String getProvinceName(int id, ArrayList<ProvinceInfo> provinces) {
        String name = "";

        if (provinces != null && id > 0) {
            for (int i = 0; i < provinces.size(); i++) {
                if (provinces.get(i).getProvinceId() == id) {
                    name = ConvertUnsignedString.getUnsignedString(provinces.get(i).getName());
                    break;
                }
            }
        }

        return name;
    }

    public static String getFromCache(String key) {
        if (redisManager != null) {
            return redisManager.get(key);
        }
        return null;
    }

    public static String pushToCache(String key, String value) {
        logger.debug("pushToCache, key = " + key + ", value = "
                + value);
        if (redisManager != null) {
            String rs = redisManager.set(key, value);
            return rs;
        }
        return null;
    }

    private boolean isBlacklist(int userId, String blacklistUserIds, String separator) {
        boolean isBlacklist = false;

        if (userId > 0 && blacklistUserIds != null) {
            isBlacklist = blacklistUserIds.indexOf(separator + String.valueOf(userId) + separator) >= 0;
        }

        return isBlacklist;
    }

    /**
     * @return the dao
     */
    public LotteryDao getDao() {
        return lotteryDao;
    }

    /**
     * @param dao the dao to set
     */
    public void setDao(LotteryDao dao) {
        this.lotteryDao = dao;
    }
}
