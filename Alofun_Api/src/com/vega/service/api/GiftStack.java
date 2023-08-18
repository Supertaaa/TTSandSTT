package com.vega.service.api;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.google.gson.Gson;
import com.vega.service.api.billing.BillingStack;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.GiftAccountInfo;
import com.vega.service.api.common.GiftContentInfo;
import com.vega.service.api.common.Helper;
import com.vega.service.api.common.HttpUtility;
import com.vega.service.api.common.Song;
import com.vega.service.api.common.StoreResult;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.GiftDAO;
import com.vega.service.api.db.IdolDAO;
import com.vega.service.api.db.StudioDAO;
import com.vega.service.api.object.BillingErrorCode;
import com.vega.service.api.object.IdolRecordInfo;
import com.vega.service.api.object.SMS;
import com.vega.service.api.object.StudioRecordInfo;
import com.vega.service.api.object.SubPackageInfo;
import com.vega.service.api.response.Result;

public class GiftStack {

    private static Logger logger = Logger.getLogger(GiftStack.class);
    private GiftDAO dao;
    private IdolDAO idolDao;
    private StudioDAO studioDao;
    private BillingStack billingStack;
    private RestfulStack restfulStack;

    public void start(RestfulStack restfulstack) throws Exception {
        dao = new GiftDAO();
        dao.init();
        idolDao = new IdolDAO();
        idolDao.start();
        studioDao = new StudioDAO();
        studioDao.start();
        this.restfulStack = restfulstack;
        this.billingStack = restfulstack.getBilling();
    }

    public Object validateGiftReceiver(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");
        String receiver = params.get("receiver");
        int topicType = Helper.getInt(params.get("topicType"), GiftContentInfo.TOPIC_MUSIC);

        if (msisdn != null && receiver != null) {
            /*
             * Check format mobile number
             */
            receiver = Helper.processMobile(receiver);
            String validatePattern = ConfigStack.getConfig("general", "giftMobilePattern", "");
            if (topicType == GiftContentInfo.TOPIC_IDOL || topicType == GiftContentInfo.TOPIC_STUDIO) {
                validatePattern = ConfigStack.getConfig("general", "idolGiftMobilePattern", "");
            }

            if (Helper.isMobileNumber(receiver, validatePattern)) {

                String vinaPattern = ConfigStack.getConfig("general", "vinaphonePattern", "");
                boolean isVinaReceiver = Helper.isMobileNumber(receiver, vinaPattern);
                if (isVinaReceiver) {
                    /*
                     * Gui noi mang
                     */
                    logger.debug("validateGiftReceiver Gui noi mang ");
                    boolean createNewIfNotExist = false;
                    int maxFreeCount = Helper.getInt(ConfigStack.getConfig("gift", "max_free_count", "6"));

                    GiftAccountInfo acc = dao.getGiftAccount(receiver, createNewIfNotExist, maxFreeCount, 0, "");
                    if (acc == null) {
                        logger.debug("acc Data nulll");
                        r.setErrorCode(Constants.SYSTEM_ERROR);
                    } else {
                        String item = GiftAccountInfo.SEPERATOR + msisdn;

                        if (acc.getStatus() == GiftAccountInfo.STATUS_REJECT) {
                            /*
                             * Check in whitelist
                             */
                            String whiteList = Helper.isNull(acc.getWhiteList()) ? ""
                                    : acc.getWhiteList();
                            if (whiteList.contains(item)) {
                                logger.debug("So dien thoai nguoi gui trong whitelist cua: " + receiver);
                                r.setErrorCode(Constants.SUCCESS);
                            } else {
                                logger.debug("So dien thoai nguoi nhan da tu choi: " + receiver);
                                r.setErrorCode(Constants.WRONG_PARAM);
                            }
                        } else {
                            /*
                             * Check in blacklist
                             */
                            String blackList = Helper.isNull(acc.getBlackList()) ? ""
                                    : acc.getBlackList();
                            if (blackList.contains(item)) {
                                logger.debug("So dien thoai nguoi gui trong blacklist cua: " + receiver);
                                r.setErrorCode(Constants.WRONG_PARAM);
                            } else {
                                r.setErrorCode(Constants.SUCCESS);
                            }
                        }
                    }
                } else {
                    /*
                     * Gui ngoai mang => kiem tra dieu kien TB ngoai mang qua Kho tap trung
                     */
                    logger.debug("Vao gui ngoai mang ");
                    ConfigStack.getConfig("ivr_store", "api_username", "");
                    String username = ConfigStack.getConfig("ivr_store", "api_username", "");
                    String password = ConfigStack.getConfig("ivr_store", "api_password", "");
                    String apiUrl = ConfigStack.getConfig("ivr_store", "api_url", "");
                    int timeout = Helper.getInt(ConfigStack.getConfig("ivr_store", "api_timeout", "15000"));

                    HashMap<String, String> requestParams = new HashMap<String, String>();
                    requestParams.put("username", username);
                    requestParams.put("password", password);
                    requestParams.put("sender", msisdn);
                    requestParams.put("receiver", receiver);

                    String fullUrl = apiUrl + "/canSendGiftContent" + HttpUtility.buildParams(requestParams);
                    String dataResp = null;
                    StoreResult fullData = null;
                    try {
                        logger.error(msisdn + " => Link full call apiUrl: " + fullUrl);
                        dataResp = HttpUtility.get(fullUrl, timeout);
                    } catch (IOException ex) {
                        logger.trace(ex);
                    }
                    if (Helper.isNull(dataResp)) {
                        logger.error(msisdn + " => Cannot get data from API: " + apiUrl);
                    } else {
                        Gson gson = new Gson();
                        try {
                            fullData = gson.fromJson(dataResp, StoreResult.class);
                        } catch (Exception e) {
                            logger.trace(e);
                        }
                        if (fullData == null) {
                            logger.error(msisdn + " => Cannot parse data from API: " + apiUrl + "; data: " + dataResp);
                        }
                    }

                    if (fullData != null) {
                        if (Constants.SUCCESS.equalsIgnoreCase(String.valueOf(fullData.getErrorCode()))) {
                            /*
                             * Cho phep gui TB ngoai mang
                             */
                            r.setErrorCode(Constants.SUCCESS);
                        } else if (Constants.ERROR_REJECT.equalsIgnoreCase(String.valueOf(fullData.getErrorCode()))) {
                            /*
                             * Khong gui duoc toi TB ngoai mang
                             */
                            logger.debug("So dien thoai nguoi gui trong blacklist cua: " + receiver);
                            r.setErrorCode(Constants.WRONG_PARAM);
                        } else {
                            logger.debug("Loi he thong gui qua FullData");
                            r.setErrorCode(Constants.SYSTEM_ERROR);
                        }
                    } else {
                        logger.debug("fullData Null");
                        r.setErrorCode(Constants.SYSTEM_ERROR);
                    }
                }

            } else {
                logger.debug("So dien thoai nguoi nhan khong dung: " + receiver);
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } else {
            logger.debug("thieu tham so sender hoac receiver");
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object validateCallDate(HashMap<String, String> params) {
        Result r = new Result();
        String strCallDate = params.get("callDate");

        if (strCallDate != null && strCallDate.length() == 4) {
            Calendar callDate = Calendar.getInstance();
            Calendar currentDate = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmmss");
            SimpleDateFormat sdf_year = new SimpleDateFormat("yyyy");
            sdf.setLenient(false);

            try {
                Date d = sdf.parse(strCallDate
                        + sdf_year.format(currentDate.getTime()) + "235959");
                callDate.setTime(d);

                if (callDate.before(currentDate)
                        && !callDate.equals(currentDate)) {
                    // Publish in next year
                    callDate.add(Calendar.YEAR, 1);
                }

                r.setDesc(sdf_year.format(callDate.getTime()));
                r.setErrorCode(Constants.SUCCESS);
            } catch (Exception e) {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object validateCallHour(HashMap<String, String> params) {
        Result r = new Result();
        String strCallHour = params.get("callHour");

        int fromHour = Helper.getInt(ConfigStack.getConfig("gift", "validFromHour", "8"));
        int toHour = Helper.getInt(ConfigStack.getConfig("gift", "validToHour", "21"));

        if (strCallHour != null && strCallHour.length() == 4) {
            Calendar callDate = Calendar.getInstance();
            Calendar currentDate = Calendar.getInstance();

            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmm");
            SimpleDateFormat sdf_date = new SimpleDateFormat("ddMMyyyy");
            sdf.setLenient(false);

            try {
                Date d = sdf.parse(sdf_date.format(currentDate.getTime()) + strCallHour);
                callDate.setTime(d);
                callDate.set(Calendar.SECOND, 0);
                callDate.set(Calendar.MILLISECOND, 0);
                currentDate.set(Calendar.SECOND, 0);
                currentDate.set(Calendar.MILLISECOND, 0);

                logger.debug("callHour after parsed: " + sdf.format(callDate.getTime()) + "; hourOfDay: " + callDate.get(Calendar.HOUR_OF_DAY));

                if (callDate.get(Calendar.HOUR_OF_DAY) < fromHour
                        || callDate.get(Calendar.HOUR_OF_DAY) > toHour) {
                    r.setErrorCode(Constants.WRONG_PARAM);
                } else {
                    r.setErrorCode(Constants.SUCCESS);
                }

                r.setName(callDate.before(currentDate) ? "before" : "after");

            } catch (Exception e) {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        r.setDesc(String.valueOf(fromHour) + "-" + String.valueOf(toHour));

        return r;
    }

    public Object getValidRangeHourForGift(HashMap<String, String> params) {
        Result r = new Result();

        int fromHour = Helper.getInt(ConfigStack.getConfig("gift", "validFromHour", "8"));
        int toHour = Helper.getInt(ConfigStack.getConfig("gift", "validToHour", "21"));

        r.setErrorCode(Constants.SUCCESS);
        r.setDesc(String.valueOf(fromHour) + "-" + String.valueOf(toHour));

        return r;
    }

    public Object getListNewGiftContent(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");
        if (msisdn != null) {
            boolean getOnlyNotReceive = true;
            int limit = Helper.getInt(ConfigStack.getConfig("API", "max_gift", "100"));

            ArrayList<GiftContentInfo> list = dao.getListGiftContent(msisdn, getOnlyNotReceive, limit);

            if (list == null) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            } else {
                if (list.isEmpty()) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                } else {
                    GiftContentInfo[] data = new GiftContentInfo[list.size()];
                    Helper.copyListToArray(list, data);
                    r.setGiftData(data);
                    r.setTotal(String.valueOf(data.length));
                    r.setErrorCode(Constants.SUCCESS);

                    /*
                     * Get current package (if exist)
                     */
                    int overFeePending = Helper.getInt(ConfigStack.getConfig("sipcm", "over_fee_pending", "-1"));
                    String regPackageIdFromGift = ConfigStack.getConfig("gift", "regPackageIdFromGift", "1");
                    if (Helper.isNull(regPackageIdFromGift)) {
                        regPackageIdFromGift = "1";
                    }
                    r.setDesc(regPackageIdFromGift);
                    HashMap<String, String> paramsCheck = new HashMap<>();
                    paramsCheck.put("msisdn", msisdn);

                    com.vega.alome.sbb.billing.bundletype.SubPackageInfo sub = billingStack.getSubPackage(paramsCheck);
                    if (sub.getErrorCode().getValue() == BillingErrorCode.Success.getValue()
                            && sub.getStatus().getValue() == SubPackageInfo.SubPackageStatus.Active.getValue()) {
                        /*
                         * Registered service
                         */
                        r.setSubPackageId(String.valueOf(sub.getSubPackageId()));
                    } else if ((sub.getErrorCode().getValue() == BillingErrorCode.Success.getValue() && sub.getStatus().getValue() == SubPackageInfo.SubPackageStatus.Cancel.getValue())
                            || sub.getErrorCode().getValue() == BillingErrorCode.NotFoundData.getValue()
                            || sub.getErrorCode().getValue() == BillingErrorCode.CancelPackage.getValue()) {
                        /*
                         * Not register service
                         */
                        r.setSubPackageId("0");
                    } else {
                        r.setErrorCode(Constants.SYSTEM_ERROR);
                    }
                }
            }

        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object sendSMSSearchMusicCode(HashMap<String, String> params) {
        Result r = new Result();
        String msisdn = params.get("msisdn");

        if (msisdn != null) {

            String mt = ConfigStack.getConfig("mt", "GIFT_SMS_SEARCH_CODE", "");
            SMS sms = new SMS();
            sms.setMsisdn(msisdn);
            sms.setMtContent(mt);
            sms.setType(1);
            sms.setAction("SEND_MT");
            sms.setPackageId(0);
            logger.info("Contetn :" + mt);
            restfulStack.sendMT(sms);
            r.setErrorCode(Constants.SUCCESS);
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object updateListenedGiftContent(HashMap<String, String> params) {
        Result r = new Result();
        String msisdn = params.get("msisdn");
        int giftContentId = Helper.getInt(params.get("giftContentId"), 0);
        int listenStatus = Helper.getInt(params.get("listenStatus"), 0);
        int duration = Helper.getInt(params.get("duration"), 0);
        int channel = Helper.getInt(params.get("channel"), 0);
        int telco = Helper.getInt(params.get("telco"), -1);

        if (msisdn != null
                && giftContentId > 0
                && listenStatus > 0
                && channel > 0
                && Constants.Telco.isValid(telco)) {
            r.setErrorCode(dao.updateListenedGiftContent(giftContentId, listenStatus, duration, channel, telco));
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object getListGiftContentFromAlbum(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");
        if (msisdn != null) {
            boolean getOnlyNotReceive = false;
            int limit = Helper.getInt(ConfigStack.getConfig("API", "max_gift", "100"));

            ArrayList<GiftContentInfo> list = dao.getListGiftContent(
                    msisdn, getOnlyNotReceive, limit);

            if (list == null) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            } else {
                if (list.isEmpty()) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                } else {
                    GiftContentInfo[] data = new GiftContentInfo[list.size()];
                    Helper.copyListToArray(list, data);

                    r.setGiftData(data);
                    r.setTotal(String.valueOf(data.length));
                    r.setErrorCode(Constants.SUCCESS);

                    /*
                     * Promt option
                     */
                    String keyCache = "getListGiftContentFromAlbum." + msisdn;
                    String showPromtStatus = RestfulStack.getFromCache(keyCache);
                    if (showPromtStatus == null) {
                        showPromtStatus = "1";
                        int day = Helper.getInt(ConfigStack.getConfig("API", "gift_day_show_promt", "7"));

                        RestfulStack.pushToCacheWithExpiredTime(keyCache,
                                showPromtStatus, day * 24 * 60 * 60);

                        r.setDesc(showPromtStatus);
                    } else {
                        r.setDesc("0");
                    }
                }
            }

        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object removeGiftContentFromAlbum(HashMap<String, String> params) {
        Result r = new Result();
        String msisdn = params.get("msisdn");
        int giftContentId = Helper.getInt(params.get("giftContentId"), 0);
        int telco = Helper.getInt(params.get("telco"), -1);

        if (msisdn != null && giftContentId > 0 && Constants.Telco.isValid(telco)) {
            r.setErrorCode(dao.removeGiftContent(giftContentId, msisdn, telco));
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object donateGift(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");
        String receiver = params.get("receiver");
        int subPackageId = Helper.getInt(params.get("subPackageId"), 0);
        int packageId = Helper.getInt(params.get("packageId"), 0);
        int contentId = Helper.getInt(params.get("contentId"), -1);
        String contentCode = params.get("contentCode");
        int topicType = Helper.getInt(params.get("topicType"), GiftContentInfo.TOPIC_MUSIC);
        String callTime = params.get("callTime");
        String source = Helper.isNull(params.get("source")) ? "IVR" : params.get("source").toUpperCase();
        String messagePath = params.get("messagePath");
        String regSource = params.get("regSource");
        String regCommand = params.get("regCommand");
        String expireAt = params.get("expireAt");
        int musicMultilPart = 0;
        
        if (params.get("partNumber") != null && !params.get("partNumber").equals("")) {
            logger.info("VAO DAY CHUA PART NUMBER : "+ params.get("partNumber"));
            musicMultilPart = Helper.getInt(params.get("partNumber"));
        }

        if (msisdn == null || subPackageId <= 0 || (contentId <= 0 && Helper.isNull(contentCode)) || topicType <= 0) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }
        /*
         * Check time to call
         */
        int delayMinuteToCall = Helper.getInt(ConfigStack.getConfig("gift", "delay_minute_to_call", "5"));
        Calendar[] timePart = Helper.formatTimeGift(callTime, "ddMMyyyy-HHmm", delayMinuteToCall);
        if (timePart == null) {
            logger.debug("Thoi gian gui qua khong dung dinh dang: " + callTime);
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }
        Calendar timeForSMS = timePart[0];
        Calendar timeForCall = timePart[1];

        /*
         * Validate mobile number of receiver
         */
        Result validateResult = (Result) validateGiftReceiver(params);
        logger.info("validateGiftReceiver :" + validateResult.getErrorCode());
        if (!validateResult.getErrorCode().equals(Constants.SUCCESS)) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }
        receiver = Helper.processMobile(receiver);

        /*
         * Check content info
         */
        String contentName = "";
        String contentPath = "";
        if (topicType == GiftContentInfo.TOPIC_MUSIC) {
            int iContentCode = Helper.getInt(contentCode, 0);
            Song s = dao.getSongInfoByIdOrCode(contentId, iContentCode);
            if (s == null || Helper.isNull(s.getSongId())) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }

            contentId = Helper.getInt(s.getSongId(), 0);
            contentCode = s.getBeat();
            contentName = s.getName();
        } else if (topicType == GiftContentInfo.TOPIC_IDOL) {
            IdolRecordInfo idolItem = idolDao.getIdolRecordByCode(contentCode);
            if (idolItem == null) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }
            if (idolItem.getRecordId() == 0) {
                logger.debug("khong tim thay ban thu Idol: " + contentCode + "; receiver: " + receiver + "; msisdn:" + msisdn);
                r.setErrorCode(Constants.WRONG_PARAM);
                return r;
            }

            contentId = idolItem.getRecordId();
            contentName = idolItem.getRecordCode();
            contentPath = idolItem.getRecordPath();
        } else if (topicType == GiftContentInfo.TOPIC_STUDIO) {
            boolean includeDraftRecord = false;
            int iContentCode = Helper.getInt(contentCode, 0);
            StudioRecordInfo studioItem = studioDao.getRecordInfo(iContentCode, includeDraftRecord);
            if (studioItem == null) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }
            if (studioItem.getRecordId() == 0) {
                logger.debug("khong tim thay ban thu Studio: " + contentCode + "; receiver: " + receiver + "; msisdn:" + msisdn);
                r.setErrorCode(Constants.WRONG_PARAM);
                return r;
            }

            contentId = studioItem.getRecordId();
            contentName = String.valueOf(studioItem.getRecordId());
            contentPath = studioItem.getRecordPath();
        } else if (topicType == GiftContentInfo.TOPIC_MUSIC_PART && musicMultilPart > 0) {
            int iContentCode = Helper.getInt(contentCode, 0);
            Song s = dao.getSongInfoMultilPart(contentId, iContentCode);
            if (s == null || Helper.isNull(s.getSongId())) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }
            contentId = Helper.getInt(s.getSongId(), 0);
            contentCode = s.getBeat();
            contentName = s.getName();
        } else {
            contentId = 0;
        }
        if (contentId <= 0) {
            r.setErrorCode(Constants.WRONG_PARAM);
            logger.debug("Khong tim thay ma noi dung: " + contentCode + "; topicType: " + topicType);
            return r;
        }

        int telco = Constants.Telco.VINAPHONE.getValue();
        int toTelco = -1;
        String viettelPattern = ConfigStack.getConfig("general", "viettelPattern", "");
        String mobifonePattern = ConfigStack.getConfig("general", "mobifonePattern", "");
        String vinaphonePattern = ConfigStack.getConfig("general", "vinaphonePattern", "");
        if (Helper.isMobileNumber(receiver, viettelPattern)) {
            toTelco = Constants.Telco.VIETTEL.getValue();
        } else if (Helper.isMobileNumber(receiver, mobifonePattern)) {
            toTelco = Constants.Telco.MOBIFONE.getValue();
        } else if (Helper.isMobileNumber(receiver, vinaphonePattern)) {
            toTelco = Constants.Telco.VINAPHONE.getValue();
        }

        /*
         * Check account of sender
         */
        boolean createNewIfNotExist = true;
        int maxFreeCount = Helper.getInt(ConfigStack.getConfig("gift", "free_count", "5"));

        GiftAccountInfo senderAcc = dao.getGiftAccount(msisdn, createNewIfNotExist, maxFreeCount, subPackageId, expireAt);
        if (senderAcc == null) {
            r.setErrorCode(Constants.SYSTEM_ERROR);
            return r;
        }
        try {
            // check them co update so qua mien phi theo chu ki goi cuoc hay k
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            cal1.setTime(sdf.parse(expireAt));
            cal2.setTime(sdf.parse(senderAcc.getSubExpireAt()));
            logger.info("SubpackageId :" + subPackageId + "---senderAcc.getSubPackageId : " + senderAcc.getSubPackageId());
            if (subPackageId != senderAcc.getSubPackageId()) {
                senderAcc = dao.updateFreeCount(msisdn, subPackageId, expireAt, maxFreeCount);
                logger.info("SubpackageId  Khac senderAcc.getSubPackageId : ");
            } else {
                if (cal1.after(cal2)) {
                    // ngay het han trong gift_account nho hon ngay het han trong sub_package thi thuc hien update qua cho kh
                    senderAcc = dao.updateFreeCount(msisdn, subPackageId, expireAt, maxFreeCount);
                    logger.info("SubpackageId  DEO KHAC  senderAcc.getSubPackageId : ");
                }
            }
        } catch (Exception ex) {
            logger.error("donateGift :" + ex);
        }

        int fee = 0;
        int countGift = senderAcc.getFreeCount();
        if (senderAcc.getFreeCount() <= 0) {

            fee = Helper.getInt(ConfigStack.getConfig("gift", "charge_gift_fee", "2000"));

            /*
             * chargeGift(String msisdn, int subPackageId, int packageId, String source, String regSource, String regCommand);
             */
            HashMap<String, String> chargePrams = new HashMap<>();
            chargePrams.put("msisdn", msisdn);
            chargePrams.put("subPackageId", String.valueOf(subPackageId));
            chargePrams.put("packageId", String.valueOf(packageId));
            chargePrams.put("source", source);
            chargePrams.put("regSource", regSource);
            chargePrams.put("regCommand", regCommand);
            chargePrams.put("amount", String.valueOf(fee));

            Result chargeResult = billingStack.chargeGift(chargePrams);
            if (Integer.valueOf(chargeResult.getErrorCode()) == BillingErrorCode.NotEnoughBalance.getValue()) {
                r.setErrorCode(Constants.BALANCE_NOT_ENOUGH);
                /*
                 * Notify via SMS
                 */

                String mt = ConfigStack.getConfig("mt", "GIFT_DONATE_NOT_ENOUGH_MONEY", "");
                mt = mt.replaceAll("\\{so_tien\\}", String.valueOf(fee));
                SMS sms = new SMS();
                sms.setMsisdn(msisdn);
                sms.setMtContent(mt);
                sms.setType(1);
                sms.setAction("SEND_MT");
                sms.setPackageId(0);
                logger.info("Contetn :" + mt);
                restfulStack.sendMT(sms);
                return r;
            } else if (Integer.valueOf(chargeResult.getErrorCode()) != BillingErrorCode.Success.getValue()) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }
        }

        GiftContentInfo g = new GiftContentInfo();
        g.setSender(msisdn);
        g.setReceiver(receiver);
        g.setContentId(contentId);
        g.setContentCode(contentCode);
        g.setGiftContentName(contentName);
        g.setSendMTDate(timeForSMS);
        g.setCallDate(timeForCall);
        g.setMessagePath(messagePath);
        g.setSource(source);
        g.setTopicType(topicType);
        g.setFee(fee);
        g.setTelco(telco);
        g.setToTelco(toTelco);
        g.setAudioPath(contentPath);
        
        String updateResult = dao.insertGiftContent(g);
        r.setErrorCode(updateResult);

        if (Constants.SUCCESS.equals(updateResult)) {
            /*
             * Notify via SMS
             */
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat sdfHour = new SimpleDateFormat("HH:mm");

            String mt = "";
            SMS sms = new SMS();
            if (topicType == GiftContentInfo.TOPIC_MUSIC || topicType == GiftContentInfo.TOPIC_MUSIC_PART) {
                mt = ConfigStack.getConfig("mt", "GIFT_DONATE_OK", "");
                mt = mt.replaceAll("\\{ten_bai_hat\\}", contentName);
                mt = mt.replaceAll("\\{so_dien_thoai\\}", receiver);
                mt = mt.replaceAll("\\{gio\\}", sdfHour.format(timeForCall.getTime()));
                mt = mt.replaceAll("\\{ngay\\}", sdfDate.format(timeForCall.getTime()));
                mt = mt.replaceAll("\\{qua_free\\}", String.valueOf(countGift));
            } else if (topicType == GiftContentInfo.TOPIC_IDOL) {
                mt = mt.replaceAll("\\{ma_ban_thu\\}", contentCode);
                mt = mt.replaceAll("\\{so_dien_thoai\\}", receiver);
                mt = mt.replaceAll("\\{gio\\}", sdfHour.format(timeForCall.getTime()));
                mt = mt.replaceAll("\\{ngay\\}", sdfDate.format(timeForCall.getTime()));
                mt = mt.replaceAll("\\{qua_free\\}", String.valueOf(countGift));
            } else if (topicType == GiftContentInfo.TOPIC_STUDIO) {
                mt = ConfigStack.getConfig("mt", "GIFT_DONATE_STUDIO_OK", "");
                mt = mt.replaceAll("\\{id_ban_thu\\}", contentCode);
                mt = mt.replaceAll("\\{so_dien_thoai\\}", receiver);
                mt = mt.replaceAll("\\{gio\\}", sdfHour.format(timeForCall.getTime()));
                mt = mt.replaceAll("\\{ngay\\}", sdfDate.format(timeForCall.getTime()));
            } 
            if (!Helper.isNull(mt)) {
                mt = Helper.convert(mt);
                sms.setMsisdn(msisdn);
                sms.setMtContent(mt);
                sms.setType(1);
                sms.setAction("SEND_MT");
                sms.setPackageId(0);
                logger.info("Contetn :" + mt);
                restfulStack.sendMT(sms);
            }
        }

        return r;
    }

    

    /**
     * @return the billingStack
     */
    public BillingStack getBillingStack() {
        return billingStack;
    }

    /**
     * @param billingStack the billingStack to set
     */
    public void setBillingStack(BillingStack billingStack) {
        this.billingStack = billingStack;
    }
}
