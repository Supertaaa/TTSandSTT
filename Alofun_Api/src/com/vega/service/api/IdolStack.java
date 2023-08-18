package com.vega.service.api;

import com.vega.service.api.billing.BillingStack;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;

import com.vega.service.api.common.Constants;
import com.vega.service.api.common.Helper;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.DBStack;
import com.vega.service.api.db.IdolDAO;
import com.vega.service.api.object.IdolAwardInfo;
import com.vega.service.api.object.IdolCompetitionInfo;
import com.vega.service.api.object.IdolListenInfo;
import com.vega.service.api.object.IdolRecordInfo;
import com.vega.service.api.object.IdolVoteInfo;
import com.vega.service.api.object.SMS;
import com.vega.service.api.object.SMSType;
import com.vega.service.api.response.Result;
import com.vega.service.api.sms.SMSStack;
import java.lang.reflect.Method;
import javax.naming.NamingException;

public class IdolStack {

    private final static Logger logger = Logger.getLogger(IdolStack.class);
    private RestfulStack restfulStack;
    private DBStack dbStack;
    private IdolDAO idolDao;
    private SMSStack smsStack;
    private BillingStack billing;

    public IdolStack() throws NamingException {
        if (idolDao == null) {
            idolDao = new IdolDAO();
            idolDao.start();
        }
    }

    public DBStack getDbStack() {
        return dbStack;
    }

    public void setDbStack(DBStack dbStack) {
        this.dbStack = dbStack;
    }

    public IdolDAO getIdolDao() {
        return idolDao;
    }

    public void setIdolDao(IdolDAO idolDao) {
        this.idolDao = idolDao;
    }

    public RestfulStack getRestfulStack() {
        return restfulStack;
    }

    public void setRestfulStack(RestfulStack restfulStack) {
        this.restfulStack = restfulStack;
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

    public Result getActiveIdolCompetition(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idolStack getActiveIdolCompetition");
        Result r = new Result();

        String msisdn = params.get("msisdn");

        if (!Helper.isNull(msisdn)) {
            IdolCompetitionInfo compet = idolDao.getActiveIdolCompetition();
            if (compet != null) {
                if (compet.getCompetitionId() > 0 && !compet.isExpired()) {
                    compet.setRoundNo(compet.getCurrentRoundNo());
                    if (compet.getRoundNo() == 1) {
                        compet.setMaxRecord(compet.getMaxRecordFirstRound());
                    } else {
                        compet.setMaxRecord(compet.getMaxRecordSecondRound());
                    }

                    if (compet.isPause()) {
                        //Tam dung cuoc thi
                        r.setErrorCode(Constants.ERROR_REJECT);
                        compet.setRoundNo(0);
                    } else {
                        ArrayList<IdolRecordInfo> listRecord = idolDao.getListApprovedRecord(compet.getCompetitionId(), msisdn, compet.getRoundNo());
                        if (listRecord != null) {
                            r.setTotal(String.valueOf(listRecord.size()));

                            if (compet.getRoundNo() == 2) {
                                int secondRoundStatus = IdolRecordInfo.SECOND_ROUND_OFF;
                                for (int i = 0; i < listRecord.size(); i++) {
                                    if (listRecord.get(i).getSecondRoundStatus() == IdolRecordInfo.SECOND_ROUND_ON) {
                                        secondRoundStatus = IdolRecordInfo.SECOND_ROUND_ON;
                                        break;
                                    }
                                }

                                if (secondRoundStatus == IdolRecordInfo.SECOND_ROUND_OFF && idolDao.checkStatusRoundStatus(compet.getCompetitionId(), msisdn)) {
                                    secondRoundStatus = IdolRecordInfo.SECOND_ROUND_ON;
                                }

                                r.setDesc(String.valueOf(secondRoundStatus));
                            }
                            r.setErrorCode(Constants.SUCCESS);
                        } else {
                            r.setErrorCode(Constants.SYSTEM_ERROR);
                        }
                    }

                    r.setCompetInfo(compet);
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

    public Result getListRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol getListRecordIdol");
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int competitionId = Helper.getInt(params.get("competitionId"), 0);
        int roundNo = Helper.getInt(params.get("roundNo"), 0);

        if (!Helper.isNull(msisdn) && competitionId > 0 && roundNo > 0) {

            ArrayList<IdolRecordInfo> listRecord = idolDao.getListRecordIdol(competitionId, msisdn, roundNo);
            if (listRecord != null) {
                r.setTotal(String.valueOf(listRecord.size()));
                if (listRecord.isEmpty()) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                } else {
                    IdolRecordInfo[] arrData = new IdolRecordInfo[listRecord.size()];
                    Helper.copyListToArray(listRecord, arrData);

                    r.setIdolRecords(arrData);
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

    public Result sendRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol sendRecordIdol");
        Result r = new Result();
        r.setErrorCode(Constants.SYSTEM_ERROR);

        String msisdn = params.get("msisdn");
        int competitionId = Helper.getInt(params.get("competitionId"), 0);
        int userId = Helper.getInt(params.get("userId"), 0);
        String recordPath = params.get("recordPath");

        if (!Helper.isNull(msisdn) && competitionId > 0 && userId > 0 && !Helper.isNull(recordPath)) {
            IdolCompetitionInfo compet = idolDao.getActiveIdolCompetition();
            if (compet == null || compet.getCompetitionId() != competitionId) {
                return r;
            }

            IdolRecordInfo item = new IdolRecordInfo();
            item.setMsisdn(msisdn);
            item.setCompetitionId(competitionId);
            item.setUserId(userId);
            item.setRecordPath(recordPath);

            if (compet.getCurrentRoundNo() == 2 && idolDao.checkStatusRoundStatus(compet.getCompetitionId(), msisdn)) {
                /*
                 * Tu dong vao xet chung ket neu da duoc chon truoc do
                 */
                item.setSecondRoundStatus(IdolRecordInfo.SECOND_ROUND_ON);
            }

            String ret = idolDao.insertIdolRecord(item);
            r.setErrorCode(ret);

            if (Constants.SUCCESS.equals(ret)) {
                /*
                 * Send MT
                 */
                String mt = ConfigStack.getConfig("mt", "idol_send_record_ok", "");
                //Gui MT
                int mtType = SMSType.Nofity.getValue();
                SMS sms = new SMS();
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction("sendRecordIdol");
                sms.setSource("IVR");
//                    sms.setPackageId(sub.getPackageId());
                sms.setBrandName(true);
                restfulStack.sendMT(sms);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result removeRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol removeRecordIdol");
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int recordId = Helper.getInt(params.get("recordId"), 0);

        if (!Helper.isNull(msisdn) && recordId > 0) {
            String ret = idolDao.removeIdolRecord(recordId, msisdn);
            r.setErrorCode(ret);
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result getListRecordForVoteIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol getListRecordForVoteIdol");
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int competitionId = Helper.getInt(params.get("competitionId"), 0);
        int roundNo = Helper.getInt(params.get("roundNo"), 0);
        int userId = Helper.getInt(params.get("userId"), 0);
        int keyNumber = Helper.getInt(params.get("keyNumber"), 0);

        if (!Helper.isNull(msisdn) && competitionId > 0 && roundNo > 0 && userId > 0 && keyNumber > 0) {
            IdolCompetitionInfo compet = idolDao.getActiveIdolCompetition();
            if (compet == null || compet.getCompetitionId() != competitionId) {
                r.setErrorCode(Constants.NO_DATA_FOUND);
                return r;
            }

            ArrayList<IdolRecordInfo> listRecord = null;
            if (keyNumber == 1) {
                /*
                 * Top ban thu
                 */
                listRecord = idolDao.getListTopRecordByRound(competitionId, msisdn, roundNo);
            } else if (keyNumber == 2) {
                /*
                 * Tong hop ban thu
                 */
                listRecord = idolDao.getListAllRecordByRound(competitionId, msisdn, roundNo);
            } else if (keyNumber == 3) {
                /*
                 * Ban thu moi nhat
                 */
                int limit = Helper.getInt(ConfigStack.getConfig("idol", "limit_top_newest", ""), 100);
                listRecord = idolDao.getListNewsestRecordByRound(competitionId, msisdn, roundNo, limit);
            }

            if (listRecord != null) {
                if (listRecord.isEmpty()) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                } else {
                    int ruleSorting = 0;
                    if (roundNo == 1) {
                        if (keyNumber == 1) {
                            ruleSorting = compet.getRuleKey1FirstRound();
                        } else if (keyNumber == 2) {
                            ruleSorting = compet.getRuleKey2FirstRound();
                        } else if (keyNumber == 3) {
                            ruleSorting = compet.getRuleKey3FirstRound();
                        }
                    } else if (roundNo == 2) {
                        if (keyNumber == 4) {
                            ruleSorting = compet.getRuleKey1SecondRound();
                        } else if (keyNumber == 5) {
                            ruleSorting = compet.getRuleKey2SecondRound();
                        }else if (keyNumber == 2) {
                            ruleSorting = compet.getRuleKey1SecondRound();
                        }
                    }

                    try {
                        if (ruleSorting == IdolCompetitionInfo.RULE_SORT_1) {
                            /*
                             * Chia 2 list: da nghe, chua nghe
                             */
                            ArrayList<IdolRecordInfo> listListened = new ArrayList<IdolRecordInfo>();
                            ArrayList<IdolRecordInfo> listNotListen = new ArrayList<IdolRecordInfo>();

                            for (IdolRecordInfo item : listRecord) {
                                if (item.getListened() == 1) {
                                    listListened.add(item);
                                } else {
                                    listNotListen.add(item);
                                }
                            }

                            /*
                             * Random 2 list
                             */
                            IdolRecordInfo[] arrListened = new IdolRecordInfo[listListened.size()];
                            IdolRecordInfo[] arrNotListen = new IdolRecordInfo[listNotListen.size()];
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

                        } else if (ruleSorting == IdolCompetitionInfo.RULE_SORT_2) {
                            /*
                             * Chia 4 list: 
                             * ban thu Tot + chua nghe
                             * ban thu Tot + da nghe
                             * ban thu Binh thuong + chua nghe
                             * ban thu Binh thuong + da nghe
                             */
                            ArrayList<IdolRecordInfo> goodListListened = new ArrayList<IdolRecordInfo>();
                            ArrayList<IdolRecordInfo> goodListNotListen = new ArrayList<IdolRecordInfo>();
                            ArrayList<IdolRecordInfo> normalListListened = new ArrayList<IdolRecordInfo>();
                            ArrayList<IdolRecordInfo> normalListNotListen = new ArrayList<IdolRecordInfo>();

                            for (IdolRecordInfo item : listRecord) {
                                if (item.getListened() == 1) {
                                    if (item.getApproveStatus() == IdolRecordInfo.STATUS_GOOD) {
                                        goodListListened.add(item);
                                    } else {
                                        normalListListened.add(item);
                                    }

                                } else {
                                    if (item.getApproveStatus() == IdolRecordInfo.STATUS_GOOD) {
                                        goodListNotListen.add(item);
                                    } else {
                                        normalListNotListen.add(item);
                                    }
                                }
                            }

                            /*
                             * Sort random
                             */
                            IdolRecordInfo[] arrGoodListened = new IdolRecordInfo[goodListListened.size()];
                            IdolRecordInfo[] arrGoodNotListen = new IdolRecordInfo[goodListNotListen.size()];
                            IdolRecordInfo[] arrNormalListened = new IdolRecordInfo[normalListListened.size()];
                            IdolRecordInfo[] arrNormalNotListen = new IdolRecordInfo[normalListNotListen.size()];
                            Helper.copyListToArray(goodListListened, arrGoodListened);
                            Helper.copyListToArray(goodListNotListen, arrGoodNotListen);
                            Helper.copyListToArray(normalListListened, arrNormalListened);
                            Helper.copyListToArray(normalListNotListen, arrNormalNotListen);

                            Helper.shuffleList(arrGoodListened);
                            Helper.shuffleList(arrGoodNotListen);
                            Helper.shuffleList(arrNormalListened);
                            Helper.shuffleList(arrNormalNotListen);

                            /*
                             * Sap xep danh sach:
                             * Phim le: Tot + chua nghe => Tot + da nghe
                             * Phim chan: Binh thuong + chua nghe => Binh thuong + da nghe
                             */
                            goodListNotListen.clear();
                            goodListNotListen.addAll(Arrays.asList(arrGoodNotListen));
                            goodListNotListen.addAll(Arrays.asList(arrGoodListened));

                            normalListNotListen.clear();
                            normalListNotListen.addAll(Arrays.asList(arrNormalNotListen));
                            normalListNotListen.addAll(Arrays.asList(arrNormalListened));

                            listRecord.clear();
                            do {
                                if (goodListNotListen.size() > 0) {
                                    listRecord.add(goodListNotListen.remove(0));
                                }
                                if (normalListNotListen.size() > 0) {
                                    listRecord.add(normalListNotListen.remove(0));
                                }
                            } while (goodListNotListen.size() > 0 || normalListNotListen.size() > 0);
                        }
                    } catch (Exception e) {
                        logger.debug(e);
                        e.printStackTrace();
                    }

                    IdolRecordInfo[] arrData = new IdolRecordInfo[listRecord.size()];
                    Helper.copyListToArray(listRecord, arrData);

                    r.setIdolRecords(arrData);
                    r.setTotal(String.valueOf(listRecord.size()));
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

    public Result addRecordIdolToMyCollection(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol addRecordIdolToMyCollection");
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int recordId = Helper.getInt(params.get("recordId"), 0);
        int userId = Helper.getInt(params.get("userId"), 0);
        int competitionId = Helper.getInt(params.get("competitionId"), 0);
        String recordCode = params.get("recordCode");

        if (!Helper.isNull(msisdn) && recordId > 0 && !Helper.isNull(recordCode)) {
            String ret = "";
            IdolRecordInfo record = idolDao.getRecordInfo(recordId);
            if (record == null || record.getRecordId() <= 0) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }

            if (record.getMsisdn().equalsIgnoreCase(msisdn)) {
                /*
                 * Add file cua chinh minh 
                 */
                ret = Constants.SUCCESS;
            } else {
                IdolRecordInfo item = new IdolRecordInfo();
                item.setMsisdn(msisdn);
                item.setUserId(userId);
                item.setCompetitionId(competitionId);
                item.setRecordId(recordId);

                ret = idolDao.addRecordToIdolCollection(item);
            }
            r.setErrorCode(ret);

            if (Constants.SUCCESS.equals(ret)) {
                String mt = ConfigStack.getConfig("mt", "idol_add_collection_ok", "");
                mt = mt.replaceAll("\\{ma_ban_thu\\}", recordCode);
                //Gui MT
                int mtType = SMSType.Nofity.getValue();
                SMS sms = new SMS();
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction("addRecordIdolToMyCollection");
                sms.setSource("IVR");
//                    sms.setPackageId(sub.getPackageId());
                sms.setBrandName(true);
                restfulStack.sendMT(sms);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result removeRecordIdolFromMyCollection(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol removeRecordIdolFromMyCollection");
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int recordId = Helper.getInt(params.get("recordId"), 0);

        if (!Helper.isNull(msisdn) && recordId > 0) {
            IdolRecordInfo record = idolDao.getRecordInfo(recordId);
            if (record == null || record.getRecordId() <= 0) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }

            String ret = "";
            if (record.getMsisdn().equalsIgnoreCase(msisdn)) {
                ret = idolDao.removeIdolRecord(recordId, msisdn);
            } else {
                ret = idolDao.removeRecordFromIdolCollection(recordId, msisdn);
            }

            r.setErrorCode(ret);
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result voteForRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol voteForRecordIdol");
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int packageId = Helper.getInt(params.get("packageId"), 0);
        int userId = Helper.getInt(params.get("userId"), 0);
        int recordId = Helper.getInt(params.get("recordId"), 0);
        int competitionId = Helper.getInt(params.get("competitionId"), 0);
        int receivedUserId = Helper.getInt(params.get("receivedUserId"), 0);
        String receiver = params.get("receiver");
        String recordCode = params.get("recordCode");

        if (!Helper.isNull(msisdn)
                && recordId > 0
                && userId > 0
                && competitionId > 0
                && receivedUserId > 0
                && !Helper.isNull(receiver)
                && !Helper.isNull(recordCode)) {
            IdolCompetitionInfo compet = idolDao.getActiveIdolCompetition();
            if (compet == null || compet.getCompetitionId() != competitionId) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }

            receiver = Helper.processMobile(receiver);

            IdolVoteInfo item = new IdolVoteInfo();
            item.setMsisdn(msisdn);
            item.setVotedUserId(userId);
            item.setReceivedUserId(receivedUserId);
            item.setReceiver(receiver);
            item.setCompetitionId(competitionId);
            item.setRecordId(recordId);
            item.setRoundNo(compet.getCurrentRoundNo());
            logger.info(">>> voteForRecordIdol RoundNo : "+ item.getRoundNo());
            if (userId != receivedUserId) {
                if (item.getRoundNo() == 2) {
                    item.setPoint(Helper.getInt(ConfigStack.getConfig("idol_award", "point_vote_second_round", ""), 0));
                } else {
                    item.setPoint(Helper.getInt(ConfigStack.getConfig("idol_award", "point_vote_first_round", ""), 0));
                }
            }
            logger.info(">>> voteForRecordIdol getVoteCount : "+ item.getVoteCount());
            String ret = idolDao.addVoteForRecord(item);
            r.setErrorCode(ret);
            r.setTotal(String.valueOf(item.getVoteCount()));
            logger.info(">>> voteForRecordIdol getVoteCount : "+  r.getTotal());
            if (Constants.SUCCESS.equalsIgnoreCase(ret)) {

                /*
                 * Cong diem theo moc
                 */
                IdolAwardInfo awardLevel1 = new IdolAwardInfo(ConfigStack.getConfig("idol_award", "award_vote_level_1", ""));
                logger.debug(awardLevel1.getLevel() + " => " + awardLevel1.getType() + ":" + awardLevel1.getVal() + ":" + awardLevel1.getKey());
                IdolAwardInfo awardLevel2 = new IdolAwardInfo(ConfigStack.getConfig("idol_award", "award_vote_level_2", ""));
                logger.debug(awardLevel2.getLevel() + " => " + awardLevel2.getType() + ":" + awardLevel2.getVal() + ":" + awardLevel2.getKey());
                IdolAwardInfo awardLevel3 = new IdolAwardInfo(ConfigStack.getConfig("idol_award", "award_vote_level_3", ""));
                logger.debug(awardLevel3.getLevel() + " => " + awardLevel3.getType() + ":" + awardLevel3.getVal() + ":" + awardLevel3.getKey());
                IdolAwardInfo awardLevel4 = new IdolAwardInfo(ConfigStack.getConfig("idol_award", "award_vote_level_4", ""));
                logger.debug(awardLevel4.getLevel() + " => " + awardLevel4.getType() + ":" + awardLevel4.getVal() + ":" + awardLevel4.getKey());
                IdolAwardInfo awardLevel5 = new IdolAwardInfo(ConfigStack.getConfig("idol_award", "award_vote_level_5", ""));
                logger.debug(awardLevel5.getLevel() + " => " + awardLevel5.getType() + ":" + awardLevel5.getVal() + ":" + awardLevel5.getKey());
                IdolAwardInfo awardForSub = null;

                if (awardLevel1.getLevel() == item.getVoteCount()) {
                    logger.debug(receiver + " has vote count: " + item.getVoteCount() + "; level 1");
                    awardForSub = awardLevel1;
                } else if (awardLevel2.getLevel() == item.getVoteCount()) {
                    logger.debug(receiver + " has vote count: " + item.getVoteCount() + "; level 2");
                    awardForSub = awardLevel2;
                } else if (awardLevel3.getLevel() == item.getVoteCount()) {
                    logger.debug(receiver + " has vote count: " + item.getVoteCount() + "; level 3");
                    awardForSub = awardLevel3;
                } else if (awardLevel4.getLevel() == item.getVoteCount()) {
                    logger.debug(receiver + " has vote count: " + item.getVoteCount() + "; level 4");
                    awardForSub = awardLevel4;
                } else if (awardLevel5.getLevel() == item.getVoteCount()) {
                    logger.debug(receiver + " has vote count: " + item.getVoteCount() + "; level 5");
                    awardForSub = awardLevel5;
                }

                if (awardForSub != null) {
                    awardForSub.setMsisdn(receiver);
                    awardForSub.setProgramName("IDOL");

                    String insertResult = idolDao.insertIdolAwardFromVoting(awardForSub);
                    if (Constants.SUCCESS.equalsIgnoreCase(insertResult)) {
                        String mt = null;

                        if (awardForSub.getType() == IdolAwardInfo.AWARD_TYPE_POINT) {
                            mt = ConfigStack.getConfig("mt", "idol_award_point_voting", "");
                            mt = mt.replaceAll("\\{ma_ban_thu\\}", recordCode);
                            mt = mt.replaceAll("\\{luot_vote\\}", String.valueOf(item.getVoteCount()));
                            mt = mt.replaceAll("\\{diem\\}", String.valueOf(awardForSub.getVal()));
                        } else if (awardForSub.getType() == IdolAwardInfo.AWARD_TYPE_CARD) {
                            mt = ConfigStack.getConfig("mt", "idol_award_card_voting", "");
                            mt = mt.replaceAll("\\{ma_ban_thu\\}", recordCode);
                            mt = mt.replaceAll("\\{luot_vote\\}", String.valueOf(item.getVoteCount()));
                            mt = mt.replaceAll("\\{qua_tang\\}", Helper.getUnsignedString(awardForSub.getAwardName()));
                        }

                        logger.debug("send mt to " + receiver + ": " + mt);
                        if (!Helper.isNull(mt)) {
                            if (!Helper.isOutOfRangeValidHour(ConfigStack.getConfig("general", "validRangeHourToPushSMS", ""), "-")) {
                                //Gui MT
                                int mtType = SMSType.Nofity.getValue();
                                SMS sms = new SMS();
                                sms.setMtContent(mt);
                                sms.setMsisdn(msisdn);
                                sms.setType(mtType);
                                sms.setHaveMO(false);
                                sms.setAction("voteForRecordIdol");
                                sms.setSource("IVR");
                                sms.setPackageId(packageId);
                                sms.setBrandName(true);
                                restfulStack.sendMT(sms);
                            } else {
                                restfulStack.sendMTLow(receiver, packageId, mt, SMSType.Genral.getValue(), true);
                            }
                        }
                    }
                }
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result processApproveRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol processApproveRecordIdol");
        Result r = new Result();
        r.setErrorCode(Constants.SYSTEM_ERROR);

        int packageId = Helper.getInt(params.get("packageId"), 0);
        int recordId = Helper.getInt(params.get("recordId"), 0);
        int approveStatus = Helper.getInt(params.get("approveStatus"), -1);

        if (recordId > 0 && approveStatus >= IdolRecordInfo.STATUS_BAD && approveStatus <= IdolRecordInfo.STATUS_GOOD) {
            IdolRecordInfo item = idolDao.getRecordInfo(recordId);
            if (item == null || item.getRecordId() == 0) {
                return r;
            }

            if (item.getApproveStatus() >= IdolRecordInfo.STATUS_BAD) {
                r.setErrorCode(Constants.WRONG_PARAM);
                return r;
            }

            if (approveStatus > IdolRecordInfo.STATUS_BAD) {
                /*
                 * Check max approved record
                 */
                IdolCompetitionInfo compet = idolDao.getIdolCompetitionById(item.getCompetitionId());
                if (compet == null || compet.getCompetitionId() == 0) {
                    r.setErrorCode(Constants.WRONG_PARAM);
                    return r;
                }

                if (compet.getCurrentRoundNo() == 1) {
                    compet.setMaxRecord(compet.getMaxRecordFirstRound());
                } else {
                    compet.setMaxRecord(compet.getMaxRecordSecondRound());
                }

                int totalApprovedRecord = idolDao.getTotalApprovedRecordByUser(item.getMsisdn(), item.getCompetitionId(), compet.getCurrentRoundNo());
                if (totalApprovedRecord >= compet.getMaxRecord()) {
                    /*
                     * Full record
                     */
                    r.setErrorCode(Constants.DATA_EXIST);
                    return r;
                }
            }

            item.setApproveStatus(approveStatus);
            String ret = idolDao.processApprovedForIdolRecord(item);
            r.setErrorCode(ret);
            /*
             * Send sms
             */
            if (Constants.SUCCESS.equalsIgnoreCase(ret)) {
                String mt = null;
                if (approveStatus == IdolRecordInfo.STATUS_BAD) {
                    mt = ConfigStack.getConfig("mt", "idol_approved_bad_record", "");
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                    String recordTime = sdf.format(item.getCreatedDate());
                    mt = mt.replaceAll("\\{thoi_gian\\}", recordTime);
                } else if (approveStatus == IdolRecordInfo.STATUS_NORMAL) {
                    mt = ConfigStack.getConfig("mt", "idol_approved_normal_record", "");
                    mt = mt.replaceAll("\\{ma_ban_thu\\}", item.getRecordCode());
                } else if (approveStatus == IdolRecordInfo.STATUS_GOOD) {
                    mt = ConfigStack.getConfig("mt", "idol_approved_good_record", "");
                    mt = mt.replaceAll("\\{ma_ban_thu\\}", item.getRecordCode());
                }

                if (!Helper.isNull(mt)) {
                    if (!Helper.isOutOfRangeValidHour(ConfigStack.getConfig("general", "validRangeHourToPushSMS", ""), "-")) {
                        //Gui MT
                        int mtType = SMSType.Nofity.getValue();
                        SMS sms = new SMS();
                        sms.setMtContent(mt);
                        sms.setMsisdn(item.getMsisdn());
                        sms.setType(mtType);
                        sms.setHaveMO(false);
                        sms.setAction("processApproveRecordIdol");
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

    public Result assignTopFisrtRoundRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol assignTopFisrtRoundRecordIdol");
        Result r = new Result();
        r.setErrorCode(Constants.SYSTEM_ERROR);

        int packageId = Helper.getInt(params.get("packageId"), 0);
        int recordId = Helper.getInt(params.get("recordId"), 0);

        if (recordId > 0) {
            IdolRecordInfo item = idolDao.getRecordInfo(recordId);
            if (item == null || item.getRecordId() == 0) {
                return r;
            }

            if (item.getFirstTopStatus() == IdolRecordInfo.TOP_STATUS_ON) {
                r.setErrorCode(Constants.DATA_EXIST);
                return r;
            }

            item.setFirstTopStatus(IdolRecordInfo.TOP_STATUS_ON);
            String ret = idolDao.processAssignFirstTopOfIdolRecord(item);

            if (Constants.SUCCESS.equalsIgnoreCase(ret)) {
                String mt = null;
                if (item.getQuantityTop1() == 0) {
                    /*
                     * Cong diem lan dau vao top
                     */
                    int point = Helper.getInt(ConfigStack.getConfig("idol_award", "point_first_top", ""), 1000);
                    IdolAwardInfo award = new IdolAwardInfo();
                    award.setMsisdn(item.getMsisdn());
                    award.setType(IdolAwardInfo.AWARD_TYPE_POINT);
                    award.setVal(point);

                    String insertResult = idolDao.insertIdolAwardFromVoting(award);
                    if (Constants.SUCCESS.equalsIgnoreCase(insertResult)) {
                        mt = ConfigStack.getConfig("mt", "idol_first_top_first_round", "");
                        mt = mt.replaceAll("\\{ma_ban_thu\\}", item.getRecordCode());
                        mt = mt.replaceAll("\\{diem\\}", String.valueOf(point));
                    }
                } else {
                    mt = ConfigStack.getConfig("mt", "idol_top_first_round", "");
                    mt = mt.replaceAll("\\{ma_ban_thu\\}", item.getRecordCode());
                }

                if (!Helper.isNull(mt)) {
                    if (!Helper.isOutOfRangeValidHour(ConfigStack.getConfig("general", "validRangeHourToPushSMS", ""), "-")) {
                        //Gui MT
                        int mtType = SMSType.Nofity.getValue();
                        SMS sms = new SMS();
                        sms.setMtContent(mt);
                        sms.setMsisdn(item.getMsisdn());
                        sms.setType(mtType);
                        sms.setHaveMO(false);
                        sms.setAction("assignTopFisrtRoundRecordIdol");
                        sms.setSource("IVR");
                        sms.setPackageId(packageId);
                        sms.setBrandName(true);
                        restfulStack.sendMT(sms);
                    } else {
                        restfulStack.sendMTLow(item.getMsisdn(), packageId, mt, SMSType.Genral.getValue(), true);
                    }
                }
            }

            r.setErrorCode(ret);
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result assignTopSecondRoundRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol assignTopSecondRoundRecordIdol");
        Result r = new Result();
        r.setErrorCode(Constants.SYSTEM_ERROR);

        int packageId = Helper.getInt(params.get("packageId"), 0);
        int recordId = Helper.getInt(params.get("recordId"), 0);

        if (recordId > 0) {
            IdolRecordInfo item = idolDao.getRecordInfo(recordId);
            if (item == null || item.getRecordId() == 0) {
                return r;
            }

            if (item.getSecondTopStatus() == IdolRecordInfo.TOP_STATUS_ON) {
                r.setErrorCode(Constants.DATA_EXIST);
                return r;
            }

            if (item.getSecondRoundStatus() != IdolRecordInfo.SECOND_ROUND_ON) {
                logger.debug("Ban thu chua vao chung ket: " + recordId);
                r.setErrorCode(Constants.WRONG_PARAM);
                return r;
            }

            item.setSecondTopStatus(IdolRecordInfo.TOP_STATUS_ON);
            String ret = idolDao.processAssignSecondTopOfIdolRecord(item);

            if (Constants.SUCCESS.equalsIgnoreCase(ret)) {
                String mt = null;
                if (item.getQuantityTop2() == 0) {
                    /*
                     * Cong diem lan dau vao top
                     */
                    int point = Helper.getInt(ConfigStack.getConfig("idol_award", "point_second_top", ""), 5000);
                    IdolAwardInfo award = new IdolAwardInfo();
                    award.setMsisdn(item.getMsisdn());
                    award.setType(IdolAwardInfo.AWARD_TYPE_POINT);
                    award.setVal(point);

                    String insertResult = idolDao.insertIdolAwardFromVoting(award);
                    if (Constants.SUCCESS.equalsIgnoreCase(insertResult)) {
                        mt = ConfigStack.getConfig("mt", "idol_first_top_second_round", "");
                        mt = mt.replaceAll("\\{ma_ban_thu\\}", item.getRecordCode());
                        mt = mt.replaceAll("\\{diem\\}", String.valueOf(point));
                    }
                } else {
                    mt = ConfigStack.getConfig("mt", "idol_top_second_round", "");
                    mt = mt.replaceAll("\\{ma_ban_thu\\}", item.getRecordCode());
                }
                logger.info("Duyet top ban thu idol A \n");
                if (!Helper.isNull(mt)) {
                    if (!Helper.isOutOfRangeValidHour(ConfigStack.getConfig("general", "validRangeHourToPushSMS", ""), "-")) {
                        //Gui MT
                        logger.info("Duyet top ban thu idol B send mt \n");
                        int mtType = SMSType.Nofity.getValue();
                        SMS sms = new SMS();
                        sms.setMtContent(mt);
                        sms.setMsisdn(item.getMsisdn());
                        sms.setType(mtType);
                        sms.setHaveMO(false);
                        sms.setAction("assignTopSecondRoundRecordIdol");
                        sms.setSource("IVR");
                        sms.setPackageId(packageId);
                        sms.setBrandName(true);
                        restfulStack.sendMT(sms);
                    } else {
                        restfulStack.sendMTLow(item.getMsisdn(), packageId, mt, SMSType.Genral.getValue(), true);
                    }
                }
                logger.info("Duyet top ban thu idol C send mt \n");
            }

            r.setErrorCode(ret);
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result updateListenHisOfRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol updateListenHisOfRecordIdol");
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int userId = Helper.getInt(params.get("userId"), 0);
        int recordId = Helper.getInt(params.get("recordId"), 0);
        int competitionId = Helper.getInt(params.get("competitionId"), 0);
        int duration = Helper.getInt(params.get("duration"), 0);
        int channel = Helper.getInt(params.get("channelNumber"), -1);
        int packageId = Helper.getInt(params.get("packageId"), 0);
        int subPackageId = Helper.getInt(params.get("subPackageId"), 0);

        if (!Helper.isNull(msisdn)
                && recordId > 0
                && userId > 0
                && competitionId > 0
                && duration >= 0
                && channel >= 0) {
            IdolListenInfo item = new IdolListenInfo();
            item.setMsisdn(msisdn);
            item.setCompetitionId(competitionId);
            item.setRecordId(recordId);
            item.setDuration(duration);
            item.setChannel(channel);
            item.setPackageId(packageId);
            item.setSubPackageId(subPackageId);
            item.setUserId(userId);

            IdolCompetitionInfo currentCompet = idolDao.getActiveIdolCompetition();
            if (currentCompet != null
                    && currentCompet.getCompetitionId() == competitionId
                    && !currentCompet.isExpired()) {
                item.setUpdateCounter(1);
            }

            String ret = idolDao.updateListenHistory(item);
            r.setErrorCode(ret);
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result getMyCollectionIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol getMyCollectionIdol");
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int mode = Helper.getInt(params.get("mode"), 0);
        int competitionId = Helper.getInt(params.get("competitionId"), 0);
        int roundNo = Helper.getInt(params.get("roundNo"), 0);

        if (!Helper.isNull(msisdn) && mode > 0 && mode <= 3) {
            if (mode == 1 && (competitionId <= 0 || roundNo <= 0)) {
                /*
                 * Lay ban thu dang du thi, nhung khong co cuoc thi dien ra
                 */
                r.setErrorCode(Constants.NO_DATA_FOUND);
            } else {
                int limit = Helper.getInt(ConfigStack.getConfig("idol", "limit_collection", ""), 100);
                ArrayList<IdolRecordInfo> listRecord = idolDao.getListRecordFromCollection(competitionId, roundNo, msisdn, mode, limit);
                if (listRecord != null) {
                    r.setTotal(String.valueOf(listRecord.size()));
                    if (listRecord.isEmpty()) {
                        r.setErrorCode(Constants.NO_DATA_FOUND);
                    } else {

                        if (mode > 1) {
                            /*
                             * Chia 2 list: da nghe, chua nghe
                             */
                            ArrayList<IdolRecordInfo> listListened = new ArrayList<IdolRecordInfo>();
                            ArrayList<IdolRecordInfo> listNotListen = new ArrayList<IdolRecordInfo>();

                            for (IdolRecordInfo item : listRecord) {
                                if (item.getListened() == 1) {
                                    listListened.add(item);
                                } else {
                                    listNotListen.add(item);
                                }
                            }

                            /*
                             * Random 2 list
                             */
                            IdolRecordInfo[] arrListened = new IdolRecordInfo[listListened.size()];
                            IdolRecordInfo[] arrNotListen = new IdolRecordInfo[listNotListen.size()];
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
                        }

                        IdolRecordInfo[] arrData = new IdolRecordInfo[listRecord.size()];
                        Helper.copyListToArray(listRecord, arrData);

                        r.setIdolRecords(arrData);
                        r.setErrorCode(Constants.SUCCESS);
                    }
                } else {
                    r.setErrorCode(Constants.SYSTEM_ERROR);
                }
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Result getInfoOfRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol getInfoOfRecordIdol");
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int recordId = Helper.getInt(params.get("recordId"), 0);
        int userId = Helper.getInt(params.get("userId"), 0);

        if (!Helper.isNull(msisdn) && recordId > 0 && userId > 0) {
            IdolRecordInfo item = idolDao.getRecordInfo(recordId);
            if (item != null) {
                if (item.getRecordId() > 0) {
                    /*
                     * Send MT
                     */
                    String mt = ConfigStack.getConfig("mt", "idol_get_info_record", "");
                    mt = mt.replaceAll("\\{ma_thue_bao\\}", String.valueOf(item.getUserId()));
                    mt = mt.replaceAll("\\{ma_ban_thu\\}", item.getRecordCode());
                    //Gui MT
                    int mtType = SMSType.Nofity.getValue();
                    SMS sms = new SMS();
                    sms.setMtContent(mt);
                    sms.setMsisdn(msisdn);
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setAction("getInfoOfRecordIdol");
                    sms.setSource("IVR");
//                    sms.setPackageId(sub.getPackageId());
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

    public Result getListRecordByID(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> idol getListRecordByID");
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int competitionId = Helper.getInt(params.get("competitionId"), 0);
        int roundNo = Helper.getInt(params.get("roundNo"), 0);
        String searchId = params.get("searchId");
        String userId = params.get("userId");
        logger.info(" >>>> idolStack  getListRecordByID :" + userId + ",msisdn:" + msisdn + ",searchId :" + searchId + ", competitionId :" + competitionId + ",roundNo: " + roundNo + "\n");
        if (!Helper.isNull(msisdn) && competitionId > 0 && roundNo > 0 && !Helper.isNull(searchId)) {
            int type = 0;
            ArrayList<IdolRecordInfo> listRecord = null;
            //if (searchId.length() > 6) {
            if (searchId != null && !"".equals(searchId)) {
                logger.info(" >>>> idolStack  getListRecordByID : Search ma ban thu" + "\n");
                /*
                 * Search ma ban thu
                 */
                type = 2;

                IdolRecordInfo item = idolDao.getRecordInfoByCode(searchId, competitionId, roundNo);
                if (item != null) {
                    listRecord = new ArrayList<IdolRecordInfo>();

                    if (item.getRecordId() > 0) {
                        listRecord.add(item);
                    }
                }
            } else {
                  r.setErrorCode(Constants.WRONG_PARAM);
            }

            //}
//            else {
//                /*
//                 * Search theo ID thue bao
//                 */
//                type = 1;
//                logger.info(" >>>> idolStack  getListRecordByID trc check :" + userId + ", competitionId :" + competitionId + ",roundNo: " + roundNo + "\n");
//                int searchUserId = Helper.getInt(userId, 0);
//                String ret = idolDao.checkUserId(searchUserId);
//                logger.info(" >>>> idolStack  getListRecordByID sau check :" + ret+ "\n");
//                if (Constants.SYSTEM_ERROR.equals(ret) || Constants.NO_DATA_FOUND.equals(ret)) {
//                    r.setErrorCode(ret);
//                    return r;
//                }
//                logger.info(" >>>> idolStack  getListRecordByID :" + userId + ", competitionId :" + competitionId + ",roundNo: " + roundNo + "\n");
//                listRecord = idolDao.getListApprovedRecordByUser(searchUserId, competitionId, roundNo);
//            }

            if (listRecord != null) {
                r.setTotal(String.valueOf(listRecord.size()));
                if (listRecord.isEmpty()) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                } else {
                    IdolRecordInfo[] arrData = new IdolRecordInfo[listRecord.size()];
                    Helper.copyListToArray(listRecord, arrData);

                    r.setIdolRecords(arrData);
                    r.setType(String.valueOf(type));
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
}
