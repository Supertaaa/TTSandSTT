package com.vega.service.api;

import com.vega.service.api.billing.BillingStack;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.Helper;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.DBStack;
import com.vega.service.api.db.StudioDAO;
import com.vega.service.api.object.SMS;
import com.vega.service.api.object.SMSType;
import com.vega.service.api.object.StudioListenInfo;
import com.vega.service.api.object.StudioRecordInfo;
import com.vega.service.api.object.StudioVoteInfo;
import com.vega.service.api.response.Result;
import com.vega.service.api.sms.SMSStack;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

public class StudioStack {

    static Logger logger = Logger.getLogger(StudioStack.class);
    private StudioDAO studioDao;
    private RestfulStack restfulStack;
    private DBStack dbStack;
    private SMSStack smsStack;
    private BillingStack billing;

    public StudioStack() throws NamingException {
        studioDao = new StudioDAO();
        studioDao.start();
    }

    public RestfulStack getRestfulStack() {
        return restfulStack;
    }

    public void setRestfulStack(RestfulStack restfulStack) {
        this.restfulStack = restfulStack;
    }

    public DBStack getDbStack() {
        return dbStack;
    }

    public void setDbStack(DBStack dbStack) {
        this.dbStack = dbStack;
    }

    public SMSStack getSmsStack() {
        return smsStack;
    }

    public void setSmsStack(SMSStack smsStack) {
        this.smsStack = smsStack;
    }

    public BillingStack getBilling() {
        return billing;
    }

    public void setBilling(BillingStack billing) {
        this.billing = billing;
    }

    public Result getListStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol getListStudioRecord");
        Result r = new Result();

        String msisdn = params.get("msisdn");

        if (!Helper.isNull(msisdn)) {
            int limit = Helper.getInt(ConfigStack.getConfig("studio", "limit_list_all", "200"));
            ArrayList<StudioRecordInfo> listRecord = studioDao.getListAllRecord(msisdn, limit);

            if (listRecord != null) {
                r.setTotal(String.valueOf(listRecord.size()));
                if (listRecord.isEmpty()) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                } else {
                    /*
                     * Chia 2 list: da nghe, chua nghe
                     */
                    ArrayList<StudioRecordInfo> listListened = new ArrayList<StudioRecordInfo>();
                    ArrayList<StudioRecordInfo> listNotListen = new ArrayList<StudioRecordInfo>();

                    for (StudioRecordInfo item : listRecord) {
                        if (item.getListened() == 1) {
                            listListened.add(item);
                        } else {
                            listNotListen.add(item);
                        }
                    }

                    /*
                     * Random 2 list
                     */
                    StudioRecordInfo[] arrListened = new StudioRecordInfo[listListened.size()];
                    StudioRecordInfo[] arrNotListen = new StudioRecordInfo[listNotListen.size()];
                    Helper.copyListToArray(listListened, arrListened);
                    Helper.copyListToArray(listNotListen, arrNotListen);

                    Helper.shuffleList(arrListened);
                    Helper.shuffleList(arrNotListen);

                    /*
                     * Sap xep danh sach:
                     * 1. Chua nghe
                     * 2. Da nghe
                     */
                    listRecord.clear();
                    listRecord.addAll(Arrays.asList(arrNotListen));
                    listRecord.addAll(Arrays.asList(arrListened));

                    StudioRecordInfo[] arrData = new StudioRecordInfo[listRecord.size()];
                    Helper.copyListToArray(listRecord, arrData);

                    r.setStudioRecords(arrData);
                    r.setErrorCode(Constants.SUCCESS);
                }
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result getTopStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack getTopStudioRecord :" + params.get("msisdn"));
        Result r = new Result();

        String msisdn = params.get("msisdn");
        if (!Helper.isNull(msisdn)) {
            int limit = Helper.getInt(ConfigStack.getConfig("studio", "limit_top", "20"));
            ArrayList<StudioRecordInfo> listRecord = studioDao.getListTopStudioRecord(msisdn, limit);

            if (listRecord != null) {
                r.setTotal(String.valueOf(listRecord.size()));
                if (listRecord.isEmpty()) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                } else {
                    /*
                     * Chia 2 list: da nghe, chua nghe
                     */
                    ArrayList<StudioRecordInfo> listListened = new ArrayList<StudioRecordInfo>();
                    ArrayList<StudioRecordInfo> listNotListen = new ArrayList<StudioRecordInfo>();

                    for (StudioRecordInfo item : listRecord) {
                        if (item.getListened() == 1) {
                            listListened.add(item);
                        } else {
                            listNotListen.add(item);
                        }
                    }

                    /*
                     * Random 2 list
                     */
                    StudioRecordInfo[] arrListened = new StudioRecordInfo[listListened.size()];
                    StudioRecordInfo[] arrNotListen = new StudioRecordInfo[listNotListen.size()];
                    Helper.copyListToArray(listListened, arrListened);
                    Helper.copyListToArray(listNotListen, arrNotListen);

                    Helper.shuffleList(arrListened);
                    Helper.shuffleList(arrNotListen);

                    /*
                     * Sap xep danh sach:
                     * 1. Chua nghe
                     * 2. Da nghe
                     */
                    listRecord.clear();
                    listRecord.addAll(Arrays.asList(arrNotListen));
                    listRecord.addAll(Arrays.asList(arrListened));

                    StudioRecordInfo[] arrData = new StudioRecordInfo[listRecord.size()];
                    Helper.copyListToArray(listRecord, arrData);

                    r.setStudioRecords(arrData);
                    r.setErrorCode(Constants.SUCCESS);
                }
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result sendStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack sendStudioRecord :" + params.get("msisdn"));
        Result r = new Result();
        r.setErrorCode(Constants.SYSTEM_ERROR);

        int packageId = Helper.getInt(params.get("packageId"), 0);
        String msisdn = params.get("msisdn");
        int userId = Helper.getInt(params.get("userId"), 0);
        String recordPath = params.get("recordPath");

        if (!Helper.isNull(msisdn) && userId > 0 && !Helper.isNull(recordPath)) {
            //int exchangePoint = Helper.getInt(ConfigStack.getConfig("studio", "exchange_point", "20000"));
            int exchangePoint = studioDao.getPointExchange();
            if (exchangePoint > 0) {
                int totalPoint = studioDao.getTotalPointOfSub(msisdn);
                if (totalPoint < exchangePoint) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                    return r;
                }
            }
            int competitionId = studioDao.getCompetitionId();
            StudioRecordInfo item = new StudioRecordInfo();
            item.setMsisdn(msisdn);
            item.setUserId(userId);
            item.setRecordPath(recordPath);
            item.setExchangePoint(exchangePoint);
            item.setCompetitionId(competitionId);

            String ret = studioDao.insertStudioRecord(item);
            r.setErrorCode(ret);

            if (Constants.SUCCESS.equals(ret)) {
                /*
                 * Send MT
                 */
                String mt = ConfigStack.getConfig("mt", "studio_send_record_ok", "");
                //Gui MT
                int mtType = SMSType.Nofity.getValue();
                SMS sms = new SMS();
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction("sendStudioRecord");
                sms.setSource("IVR");
                sms.setPackageId(packageId);
                sms.setBrandName(true);
                restfulStack.sendMT(sms);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result addStudioRecordToMyCollection(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack addStudioRecordToMyCollection :" + params.get("msisdn"));
        Result r = new Result();

        int packageId = Helper.getInt(params.get("packageId"), 0);
        String msisdn = params.get("msisdn");
        int recordId = Helper.getInt(params.get("recordId"), 0);
        int userId = Helper.getInt(params.get("userId"), 0);

        if (!Helper.isNull(msisdn) && recordId > 0) {
            String ret = "";
            boolean includeDraftRecord = false;
            StudioRecordInfo record = studioDao.getRecordInfo(recordId, includeDraftRecord);
            if (record == null || record.getRecordId() <= 0) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }

            StudioRecordInfo item = new StudioRecordInfo();
            item.setMsisdn(msisdn);
            item.setUserId(userId);
            item.setRecordId(recordId);
            if (record.getMsisdn().equalsIgnoreCase(msisdn)) {
                item.setIsOwner(1);
            }

            ret = studioDao.addRecordToStudioCollection(item);
            r.setErrorCode(ret);

            if (Constants.SUCCESS.equals(ret)) {
                String mt = ConfigStack.getConfig("mt", "studio_add_collection_ok", "");
                mt = mt.replaceAll("\\{id_ban_thu\\}", String.valueOf(recordId));
                //Gui MT
                int mtType = SMSType.Nofity.getValue();
                SMS sms = new SMS();
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction("addStudioRecordToMyCollection");
                sms.setSource("IVR");
                sms.setPackageId(packageId);
                sms.setBrandName(true);
                restfulStack.sendMT(sms);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result removeStudioRecordFromMyCollection(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack removeStudioRecordFromMyCollection :" + params.get("msisdn"));
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int recordId = Helper.getInt(params.get("recordId"), 0);

        if (!Helper.isNull(msisdn) && recordId > 0) {
            boolean includeDraftRecord = true;
            StudioRecordInfo record = studioDao.getRecordInfo(recordId, includeDraftRecord);
            if (record == null || record.getRecordId() <= 0) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }

            String ret = studioDao.removeRecordFromStudioCollection(recordId, msisdn);
            r.setErrorCode(ret);
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result voteForStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack voteForStudioRecord :" + params.get("msisdn"));
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int userId = Helper.getInt(params.get("userId"), 0);
        int recordId = Helper.getInt(params.get("recordId"), 0);
        int receivedUserId = Helper.getInt(params.get("receivedUserId"), 0);
        String receiver = params.get("receiver");

        if (!Helper.isNull(msisdn)
                && recordId > 0
                && userId > 0
                && receivedUserId > 0
                && !Helper.isNull(receiver)) {
            receiver = Helper.processMobile(receiver);

            StudioVoteInfo item = new StudioVoteInfo();
            item.setMsisdn(msisdn);
            item.setVotedUserId(userId);
            item.setReceivedUserId(receivedUserId);
            item.setReceiver(receiver);
            item.setRecordId(recordId);

            if (userId != receivedUserId) {
                item.setPoint(Helper.getInt(ConfigStack.getConfig("studio", "point_vote", "10")));
            }

            String ret = studioDao.addVoteForRecord(item);
            r.setErrorCode(ret);
            r.setTotal(String.valueOf(item.getVoteCount()));
//			if(Constants.SUCCESS.equalsIgnoreCase(ret)){
//				r.setTotal(String.valueOf(item.getVoteCount()));
//			}
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result processApproveStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack processApproveStudioRecord :");
        Result r = new Result();
        r.setErrorCode(Constants.SYSTEM_ERROR);

        int packageId = Helper.getInt(params.get("packageId"), 0);
        int recordId = Helper.getInt(params.get("recordId"), 0);
        int approveStatus = Helper.getInt(params.get("approveStatus"), -1);

        if (recordId > 0 && approveStatus > -1) {
            boolean includeDraftRecord = true;
            StudioRecordInfo item = studioDao.getRecordInfo(recordId, includeDraftRecord);
            if (item == null || item.getRecordId() == 0 || item.getApproveStatus() == approveStatus) {
                return r;
            }
            item.setApproveStatus(approveStatus);

            String ret = studioDao.processApproveRecord(item);
            r.setErrorCode(ret);
            /*
             * Send sms
             */
            if (Constants.SUCCESS.equalsIgnoreCase(ret)) {
                String mt = null;
                if (approveStatus == StudioRecordInfo.STATUS_APPROVED) {
                    mt = ConfigStack.getConfig("mt", "studio_approved_record", "");
                    mt = mt.replaceAll("\\{id_ban_thu\\}", String.valueOf(item.getRecordId()));
                } else if (approveStatus == StudioRecordInfo.STATUS_NOT_APPROVE) {
                    mt = ConfigStack.getConfig("mt", "studio_not_approve_record", "");
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                    String recordTime = sdf.format(item.getCreatedDate());
                    mt = mt.replaceAll("\\{thoi_gian\\}", recordTime);
                    mt = mt.replaceAll("\\{diem\\}", String.valueOf(item.getExchangePoint()));
                }

                if (!Helper.isNull(mt)) {
                    if (!Helper.isOutOfRangeValidHour(ConfigStack.getConfig("validRangeHourToPushSMS", "general", ""), "-")) {
                        //Gui MT
                        int mtType = SMSType.Nofity.getValue();
                        SMS sms = new SMS();
                        sms.setMtContent(mt);
                        sms.setMsisdn(item.getMsisdn());
                        sms.setType(mtType);
                        sms.setHaveMO(false);
                        sms.setAction("processApproveStudioRecord");
                        sms.setSource("IVR");
                        sms.setPackageId(packageId);
                        sms.setBrandName(true);
                        restfulStack.sendMT(sms);
                    } else {
                        restfulStack.sendMTLow(item.getMsisdn(), packageId, mt, SMSType.Genral.getValue(), true);
                    }
                }
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result updateListenHisOfStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack updateListenHisOfStudioRecord :" + params.get("msisdn"));
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int userId = Helper.getInt(params.get("userId"), 0);
        int recordId = Helper.getInt(params.get("recordId"), 0);
        int duration = Helper.getInt(params.get("duration"), 0);
        int channel = Helper.getInt(params.get("channelNumber"), -1);
        int packageId = Helper.getInt(params.get("packageId"), 0);
        int subPackageId = Helper.getInt(params.get("subPackageId"), 0);

        if (!Helper.isNull(msisdn)
                && recordId > 0
                && userId > 0
                && duration >= 0
                && channel >= 0) {
            StudioListenInfo item = new StudioListenInfo();
            item.setMsisdn(msisdn);
            item.setRecordId(recordId);
            item.setDuration(duration);
            item.setChannel(channel);
            item.setPackageId(packageId);
            item.setSubPackageId(subPackageId);
            item.setUserId(userId);

            String ret = studioDao.updateListenHistory(item);
            r.setErrorCode(ret);
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result getMyStudioCollection(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack getMyStudioCollection :" + params.get("msisdn"));
        Result r = new Result();

        String msisdn = params.get("msisdn");

        if (!Helper.isNull(msisdn)) {
            int limit = Helper.getInt(ConfigStack.getConfig("studio", "limit_collection", "200"));
            ArrayList<StudioRecordInfo> listRecord = studioDao.getListRecordFromCollection(msisdn, limit);
            if (listRecord != null) {
                r.setTotal(String.valueOf(listRecord.size()));
                if (listRecord.isEmpty()) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                } else {
                    /*
                     * Chia 2 list: da nghe, chua nghe
                     */
                    ArrayList<StudioRecordInfo> listListened = new ArrayList<StudioRecordInfo>();
                    ArrayList<StudioRecordInfo> listNotListen = new ArrayList<StudioRecordInfo>();

                    for (StudioRecordInfo item : listRecord) {
                        if (item.getListened() == 1) {
                            listListened.add(item);
                        } else {
                            listNotListen.add(item);
                        }
                    }

                    /*
                     * Random 2 list
                     */
                    StudioRecordInfo[] arrListened = new StudioRecordInfo[listListened.size()];
                    StudioRecordInfo[] arrNotListen = new StudioRecordInfo[listNotListen.size()];
                    Helper.copyListToArray(listListened, arrListened);
                    Helper.copyListToArray(listNotListen, arrNotListen);

                    Helper.shuffleList(arrListened);
                    Helper.shuffleList(arrNotListen);

                    /*
                     * Sap xep danh sach:
                     * 1. Chua nghe
                     * 2. Da nghe
                     */
                    listRecord.clear();
                    listRecord.addAll(Arrays.asList(arrNotListen));
                    listRecord.addAll(Arrays.asList(arrListened));

                    StudioRecordInfo[] arrData = new StudioRecordInfo[listRecord.size()];
                    Helper.copyListToArray(listRecord, arrData);

                    r.setStudioRecords(arrData);
                    r.setErrorCode(Constants.SUCCESS);
                }
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result getInfoOfStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack getInfoOfStudioRecord :" + params.get("msisdn"));
        Result r = new Result();

        int packageId = Helper.getInt(params.get("packageId"), 0);
        String msisdn = params.get("msisdn");
        int recordId = Helper.getInt(params.get("recordId"), 0);
        int userId = Helper.getInt(params.get("userId"), 0);

        if (!Helper.isNull(msisdn) && recordId > 0 && userId > 0) {
            boolean includeDraftRecord = false;
            StudioRecordInfo item = studioDao.getRecordInfo(recordId, includeDraftRecord);
            if (item != null) {
                if (item.getRecordId() > 0) {
                    /*
                     * Send MT
                     */
                    String mt = ConfigStack.getConfig("mt", "studio_get_info_record", "");
                    mt = mt.replaceAll("\\{id_thue_bao\\}", String.valueOf(item.getUserId()));
                    mt = mt.replaceAll("\\{id_ban_thu\\}", String.valueOf(item.getRecordId()));
                    //Gui MT
                    int mtType = SMSType.Nofity.getValue();
                    SMS sms = new SMS();
                    sms.setMtContent(mt);
                    sms.setMsisdn(msisdn);
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setAction("getInfoOfStudioRecord");
                    sms.setSource("IVR");
                    sms.setPackageId(packageId);
                    sms.setBrandName(true);
                    restfulStack.sendMT(sms);

                    r.setErrorCode(Constants.SUCCESS);
                } else {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                }
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result getStudioRecordByID(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack getStudioRecordByID :" + params.get("msisdn"));
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int searchId = Helper.getInt(params.get("searchId"), 0);

        if (!Helper.isNull(msisdn)) {
            boolean includeDraftRecord = false;
            StudioRecordInfo item = studioDao.getRecordInfo(searchId, includeDraftRecord);
            if (item != null) {
                if (item.getRecordId() > 0) {
                    StudioRecordInfo[] arrData = {item};
                    r.setStudioRecords(arrData);
                    r.setTotal("1");
                    r.setErrorCode(Constants.SUCCESS);
                } else {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                }
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result checkAbilityRecordStudio(HashMap<String, String> params) {
        logger.info(" >>>>>>> StudioStack checkAbilityRecordStudio :" + params.get("msisdn"));
        Result r = new Result();

        String msisdn = params.get("msisdn");

        if (!Helper.isNull(msisdn)) {
            //int exchangePoint = Helper.getInt(ConfigStack.getConfig("studio", "exchange_point", "20000"));
            int exchangePoint = studioDao.getPointExchange();
              logger.info(" >>>>>>> StudioStack checkAbilityRecordStudio exchangePoint:" + exchangePoint+"\n");

            if (exchangePoint > 0) {
                int totalPoint = studioDao.getTotalPointOfSub(msisdn);
                logger.info(" >>>>>>> StudioStack checkAbilityRecordStudio exchangePoint:" + exchangePoint+"\n");
                if (totalPoint < exchangePoint) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                } else {
                    r.setErrorCode(Constants.SUCCESS);
                }
            } else {
                r.setErrorCode(Constants.NO_DATA_FOUND);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }
}
